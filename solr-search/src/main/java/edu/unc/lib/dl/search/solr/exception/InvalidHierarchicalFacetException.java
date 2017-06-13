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
package edu.unc.lib.dl.search.solr.exception;

/**
 * 
 * @author bbpennel
 *
 */
public class InvalidHierarchicalFacetException extends InvalidFacetException {
    private static final long serialVersionUID = 1L;

    public InvalidHierarchicalFacetException(String msg, Exception e) {
        super(msg);
        this.setStackTrace(e.getStackTrace());
    }

    public InvalidHierarchicalFacetException(String msg) {
        super(msg);
    }
}
