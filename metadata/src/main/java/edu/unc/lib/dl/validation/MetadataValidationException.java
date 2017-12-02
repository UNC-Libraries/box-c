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
package edu.unc.lib.dl.validation;

/**
 * Exception indicating that metadata was determined to be invalid
 *
 * @author bbpennel
 *
 */
public class MetadataValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String details;

    public MetadataValidationException() {
    }

    public MetadataValidationException(String message) {
        super(message);
    }

    public MetadataValidationException(String message, String details) {
        super(message);
        this.details = details;
    }

    public MetadataValidationException(Throwable cause) {
        super(cause);
    }

    public MetadataValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetadataValidationException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    /**
     * @return the details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Returns the message and details of this exception if available.
     *
     * @return
     */
    public String getDetailedMessage() {
        if (details == null) {
            return getMessage();
        } else {
            return getMessage() + "\n" + details;
        }
    }
}
