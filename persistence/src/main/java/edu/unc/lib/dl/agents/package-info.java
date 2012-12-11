/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Provides interfaces and classes necessary to manage and describe agents that
 * interact with the repository, such as people, groups, and software. These 
 * agents serve as references in PREMIS event logs but may provide authorization
 * information for access controls, etc.
 * 
 * <p>The agents package provides three classes of agents:
 * <ul>
 * <li>PersonAgent describes a human user of the repository system.</li>
 * <li>GroupAgent describes a group of users of the repository system.</li>
 * <li>SoftwareAgent describes a software component.</li>
 * </ul>
 */
package edu.unc.lib.dl.agents;

