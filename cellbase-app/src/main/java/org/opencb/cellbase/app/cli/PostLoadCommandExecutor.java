package org.opencb.cellbase.app.cli;

import com.beust.jcommander.ParameterException;
import org.opencb.biodata.formats.annotation.io.VepFormatReader;
import org.opencb.biodata.models.variant.annotation.VariantAnnotation;
import org.opencb.cellbase.core.CellBaseConfiguration;
import org.opencb.cellbase.core.client.CellBaseClient;
import org.opencb.cellbase.core.lib.DBAdaptorFactory;
import org.opencb.cellbase.core.lib.api.variation.ClinicalDBAdaptor;
import org.opencb.cellbase.core.lib.api.variation.VariantAnnotationDBAdaptor;
import org.opencb.cellbase.core.variant_annotation.VariantAnnotator;
import org.opencb.cellbase.core.variant_annotation.VariantAnnotatorRunner;
import org.opencb.cellbase.mongodb.db.MongoDBAdaptorFactory;
import org.opencb.datastore.core.QueryOptions;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by fjlopez on 14/04/15.
 */
public class PostLoadCommandExecutor extends CommandExecutor{

    private CliOptionsParser.PostLoadCommandOptions postLoadCommandOptions;

    private Path clinicalAnnotationFilename = null;
    private String assembly = null;
    private static final int CLINICAL_ANNOTATION_BATCH_SIZE=1000;

    // TODO: remove constructor, just for debugging purposes
    public PostLoadCommandExecutor() {}

    public PostLoadCommandExecutor(CliOptionsParser.PostLoadCommandOptions postLoadCommandOptions) {
        super(postLoadCommandOptions.commonOptions.logLevel, postLoadCommandOptions.commonOptions.verbose,
                postLoadCommandOptions.commonOptions.conf);

        this.postLoadCommandOptions = postLoadCommandOptions;
    }

    @Override
    public void execute() {
        checkParameters();
        if(clinicalAnnotationFilename!=null) {
            loadClinicalAnnotation();
        } else {
            throw new ParameterException("Only post-load of clinical annotations is available right now.");
        }
    }

    private void checkParameters() {
        // input file
        if (postLoadCommandOptions.clinicalAnnotationFilename != null) {
            clinicalAnnotationFilename = Paths.get(postLoadCommandOptions.clinicalAnnotationFilename);
            if (!clinicalAnnotationFilename.toFile().exists()) {
                throw new ParameterException("Input file " + clinicalAnnotationFilename + " doesn't exist");
            } else if (clinicalAnnotationFilename.toFile().isDirectory()) {
                throw new ParameterException("Input file cannot be a directory: " + clinicalAnnotationFilename);
            }

            if(postLoadCommandOptions.assembly != null) {
                assembly = postLoadCommandOptions.assembly;
                if(!assembly.equals("GRCh37") && !assembly.equals("GRCh38")) {
                    throw  new ParameterException("Please, provide a valid human assembly. Available assemblies: GRCh37, GRCh38");
                }
            } else {
                throw  new ParameterException("Providing human assembly is mandatory if loading clinical annotations. Available assemblies: GRCh37, GRCh38");
            }

        } else {
            throw  new ParameterException("Please check command line syntax. Provide a valid input file name.");
        }
    }

    // TODO: change to private - just for debugging purposes
    public void loadClinicalAnnotation() {

        /**
         * Initialize VEP reader
          */
        VepFormatReader vepFormatReader = new VepFormatReader(clinicalAnnotationFilename.toString());
        vepFormatReader.open();
        vepFormatReader.pre();

        /**
         * Prepare clinical adaptor
         */
        org.opencb.cellbase.core.common.core.CellbaseConfiguration adaptorCellbaseConfiguration =
                new org.opencb.cellbase.core.common.core.CellbaseConfiguration();
        adaptorCellbaseConfiguration.addSpeciesAlias("hsapiens", "hsapiens");
        adaptorCellbaseConfiguration.addSpeciesConnection("hsapiens", assembly,
                configuration.getDatabase().getHost(), "cellbase_hsapiens_"+assembly.toLowerCase()+"_"+
                        configuration.getVersion(), Integer.valueOf(configuration.getDatabase().getPort()), "mongo",
                configuration.getDatabase().getUser(), configuration.getDatabase().getPassword(), 10, 10);

        DBAdaptorFactory dbAdaptorFactory = new MongoDBAdaptorFactory(adaptorCellbaseConfiguration);
        ClinicalDBAdaptor clinicalDBAdaptor = dbAdaptorFactory.getClinicalDBAdaptor("hsapiens", assembly);

        /**
         * Load annotations
         */
        int nVepAnnotatedVariants = 0;
        List<VariantAnnotation> variantAnnotationList = vepFormatReader.read(CLINICAL_ANNOTATION_BATCH_SIZE);
        while(!variantAnnotationList.isEmpty()) {
            nVepAnnotatedVariants += variantAnnotationList.size();
            clinicalDBAdaptor.updateAnnotations(variantAnnotationList, new QueryOptions());
            logger.info(Integer.valueOf(nVepAnnotatedVariants)+" read variants with vep annotations");
            variantAnnotationList = vepFormatReader.read(CLINICAL_ANNOTATION_BATCH_SIZE);
        }

        vepFormatReader.post();
        vepFormatReader.close();
        logger.info(nVepAnnotatedVariants+" VEP annotated variants were read from "+clinicalAnnotationFilename.toString());
        logger.info("Finished");
    }


}
