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
 * Provides an AIP processing pipeline consisting of AIP filter components. 
 * 
 * <p>The filter perform various standard functions on all AIPs:
 * <ul>
 * <li>validation of Fedora objects against specs and content models</li>
 * <li>metadata crosswalks</li>
 * <li>event logging (PREMIS events log)</li>
 * <li>collision detection with existing repository objects</li>
 * <li>addition filters may be added to the pipeline... </li>
 * </ul>
 */
package edu.unc.lib.dl.ingest.aip;

