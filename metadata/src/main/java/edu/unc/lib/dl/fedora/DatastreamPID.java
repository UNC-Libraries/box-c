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
package edu.unc.lib.dl.fedora;

/**
 * Datastream aware PID object
 * 
 * @author bbpennel
 *
 */
public class DatastreamPID extends PID {
    private static final long serialVersionUID = -668408790779214575L;
    private String datastream;

    public DatastreamPID(String pid) {
        super(pid);

        datastream = null;

        int index = this.pid.indexOf('/');
        if (index != -1) {
            if (index != this.pid.length() - 1) {
                this.datastream = this.pid.substring(index + 1);
            }

            this.pid = this.pid.substring(0, index);
        }
    }

    public String getDatastream() {
        return datastream;
    }

    public String getDatastreamURI() {
        if (datastream == null) {
            return getURI();
        }

        return getURI() + "/" + datastream;
    }
}
