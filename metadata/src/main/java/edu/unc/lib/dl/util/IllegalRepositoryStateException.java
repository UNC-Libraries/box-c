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
package edu.unc.lib.dl.util;

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.fedora.PID;

/**
 * This is a runtime exception that can be throw whenever some code encounters
 * bad state in the repository. These bad states might include duplicate guids,
 * repository paths. They might include an object having multiple parents or
 * collections.
 * 
 * @author count0
 *
 */
public class IllegalRepositoryStateException extends IllegalStateException {

    private static final long serialVersionUID = 1439263732009258704L;

    List<PID> invalidObjects = null;

    public IllegalRepositoryStateException(String message) {
        super(message);
    }

    public IllegalRepositoryStateException(String message, Throwable e) {
        super(message, e);
    }

    public IllegalRepositoryStateException(String message,
            List<PID> invalidObjects) {
        super(message);
        this.invalidObjects = invalidObjects;
    }

    public IllegalRepositoryStateException(String message,
            List<PID> invalidObjects, Throwable e) {
        super(message, e);
        this.invalidObjects = invalidObjects;
    }

    public IllegalRepositoryStateException(String message, PID invalidObject) {
        super(message);
        this.invalidObjects = new ArrayList<PID>();
        this.invalidObjects.add(invalidObject);
    }

    public IllegalRepositoryStateException(String message, PID invalidObject,
            Throwable e) {
        super(message, e);
        this.invalidObjects = new ArrayList<PID>();
        this.invalidObjects.add(invalidObject);
    }

    @Override
    public String getMessage() {
        String result = null;
        if (this.invalidObjects != null && invalidObjects.size() > 0) {
            StringBuffer sb = new StringBuffer(super.getMessage());
            sb.append("\nObjects with illegal state:\n");
            for (PID d : this.invalidObjects) {
                sb.append(d).append("\t");
            }
        } else {
            result = super.getMessage();
        }

        return result;
    }

}
