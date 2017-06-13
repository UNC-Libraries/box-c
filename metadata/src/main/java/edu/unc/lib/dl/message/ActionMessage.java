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
package edu.unc.lib.dl.message;

import java.io.Serializable;

/**
 * Action Messages
 * @author bbpennel
 *
 */
public interface ActionMessage extends Serializable {
    /**
     * Returns the identifier for this message
     * 
     * @return
     */
    public String getMessageID();

    /**
     * Returns the target of this message.
     * 
     * @return
     */
    public String getTargetID();

    /**
     * Returns the label for the target of this message
     * 
     * @return
     */
    public String getTargetLabel();

    public void setTargetLabel(String targetLabel);

    /**
     * Returns the unqualified action to be performed on the target
     * 
     * @return
     */
    public String getAction();

    /**
     * Returns the namespace of the action to be performed on the target
     * 
     * @return
     */
    public String getNamespace();

    /**
     * Returns the action name qualified by its namespace
     * 
     * @return
     */
    public String getQualifiedAction();

    /**
     * Returns the time at which this message was created.
     * 
     * @return
     */
    public long getTimeCreated();
}
