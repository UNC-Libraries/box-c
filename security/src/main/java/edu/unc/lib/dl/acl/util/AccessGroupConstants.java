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
package edu.unc.lib.dl.acl.util;

/**
 * Constants related to access control groups
 *
 * @author bbpennel
 *
 */
@Deprecated
public class AccessGroupConstants {
    public final static String PUBLIC_GROUP = "public";
    public final static String AUTHENTICATED_GROUP = "authenticated";
    public static String ADMIN_GROUP = null;
    public static String ADMIN_GROUP_ESCAPED = null;

    public void setADMIN_GROUP(String group) {
        if (ADMIN_GROUP != null) {
            return;
        }
        ADMIN_GROUP = group;
        ADMIN_GROUP_ESCAPED = ADMIN_GROUP.replaceAll(":", "\\\\:");
    }

    public String getPUBLIC_GROUP() {
        return PUBLIC_GROUP;
    }

    public String getADMIN_GROUP() {
        return ADMIN_GROUP;
    }

    public String getADMIN_GROUP_ESCAPED() {
        return ADMIN_GROUP_ESCAPED;
    }
}
