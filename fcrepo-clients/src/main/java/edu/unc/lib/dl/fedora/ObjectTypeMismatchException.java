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
 * Exception which indicates that a repository object did not match the expected RDF types
 *
 * @author bbpennel
 *
 */
public class ObjectTypeMismatchException extends FedoraException {

    private static final long serialVersionUID = -1660852243627715050L;

    public ObjectTypeMismatchException(Exception e) {
        super(e);
    }

    public ObjectTypeMismatchException(String value) {
        super(value);
    }
}
