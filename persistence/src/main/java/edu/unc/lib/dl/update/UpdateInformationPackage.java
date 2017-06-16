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
package edu.unc.lib.dl.update;

import java.io.File;
import java.util.Map;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;

/**
 * 
 * @author bbpennel
 *
 */
public interface UpdateInformationPackage {

    public PID getPID();

    public String getUser();

    public UpdateOperation getOperation();

    public Map<String,?> getIncomingData();

    public Map<String,?> getOriginalData();

    public Map<String,?> getModifiedData();

    public Map<String,File> getModifiedFiles();

    public String getMessage();

    public String getMimetype(String key);

    public PremisEventLogger getEventLogger();
}
