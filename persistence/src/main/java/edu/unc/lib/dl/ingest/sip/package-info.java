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
 * Provides interface and implementations of submission information packages
 * (SIPs) and SIP processors, which migrate SIP information into archival
 * information packages (AIPs).  All SIP processors must produce a valid AIP
 * with respect to <a href="https://intranet.lib.unc.edu:82/trac/cdl/wiki/FedoraObjectTypes">CDR specifications</a> for Fedora objects. 
 * 
 * <p>The SIP API provides a number of SIP options, including:
 * <ul>
 * <li>METS Package SIP (with extensible profile)</li>
 * <li>Single File SIP (file plus metadata)</li>
 * <li>Single Folder SIP</li>
 * <li>Agent SIP (used primarily by the AgentManager)</li>
 * </ul>
 */
package edu.unc.lib.dl.ingest.sip;

