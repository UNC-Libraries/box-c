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
package edu.unc.lib.dl.ingest.aip;

/**
 * An ingest filter performs some piece of pre-ingest work on an ReportingIngestBundle. Filters should also log their
 * actions in the PremisEventLogger. Only one instance of a filter is created per application context, therefore the
 * doFilter method must be thread-safe.
 * 
 * @author count0
 * 
 */
public interface AIPIngestFilter {
	public ArchivalInformationPackage doFilter(ArchivalInformationPackage aip) throws AIPException;
}
