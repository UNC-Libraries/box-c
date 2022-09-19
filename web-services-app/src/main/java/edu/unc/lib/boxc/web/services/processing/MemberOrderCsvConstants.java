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
package edu.unc.lib.boxc.web.services.processing;

/**
 * Constants related to the CSV Member Order export
 *
 * @author bbpennel
 */
public class MemberOrderCsvConstants {
    public static final String OBJ_TYPE_HEADER = "Object Type";
    public static final String PID_HEADER = "PID";
    public static final String PARENT_PID_HEADER = "Parent PID";
    public static final String TITLE_HEADER = "Title";
    public static final String FILENAME_HEADER = "Filename";
    public static final String DELETED_HEADER = "Deleted";
    public static final String MIME_TYPE_HEADER = "MIME Type";
    public static final String ORDER_HEADER = "Member Order";

    public static final String[] CSV_HEADERS = new String[] {
            PARENT_PID_HEADER, PID_HEADER, TITLE_HEADER, OBJ_TYPE_HEADER, FILENAME_HEADER,
            MIME_TYPE_HEADER, DELETED_HEADER, ORDER_HEADER};

    private MemberOrderCsvConstants(){
    }
}
