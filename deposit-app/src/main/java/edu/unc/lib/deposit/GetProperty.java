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
package edu.unc.lib.deposit;

/**
 * 
 * @author count0
 *
 */
public class GetProperty {
    private GetProperty() {
    }

    /**
     * Prints out system property values based on command line arguments.
     * Copied from https://github.com/rfkrocktk/commons-daemon-example
     * 
     * @param args
     *            Names of system properties to print values for.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err
                    .println("ERROR: Please pass a system property name an an argument "
                            + "to the program.");
            System.exit(1);
        }

        for (String property : args) {
            if (!System.getProperties().containsKey(property)) {
                System.err.println(String.format(
                        "ERROR: Unable to find property '%s'.", property));
                System.exit(1);
            }
        }

        for (String property : args) {
            System.out.println(System.getProperty(property));
        }
    }

}
