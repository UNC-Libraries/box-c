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
package edu.unc.lib.dl.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;

/**
 *  Helper method for IT tests
 *
 * @author harring
 *
 */
public class TestHelper {

    public static void setContentBase(String uri) {
        try {
            Method m = RepositoryPaths.class.getDeclaredMethod("setContentBase", String.class);
            m.setAccessible(true);
            try {
                m.invoke(null, uri);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
