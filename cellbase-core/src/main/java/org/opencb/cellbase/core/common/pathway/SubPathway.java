/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.cellbase.core.common.pathway;

import java.util.ArrayList;
import java.util.List;

public class SubPathway {
	String name;
	List<String> displayName;
	List<SubPathway> subPathways;
	
	public SubPathway(String name, List<String> displayName) {
		this.name = name;
		this.displayName = displayName;
		this.subPathways = new ArrayList<SubPathway>();
	}

	public void addSubpathways(SubPathway sp) {
		this.subPathways.add(sp);
	}
}
