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
package edu.unc.lib.dcr.migration.content;

import static edu.unc.lib.dcr.migration.paths.PathIndex.OBJECT_TYPE;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static java.nio.file.Files.newInputStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.RecursiveAction;

import org.jdom2.Document;
import org.jdom2.JDOMException;

import edu.unc.lib.dcr.migration.deposit.DepositModelManager;
import edu.unc.lib.dcr.migration.paths.PathIndex;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 */
public class ContentObjectTransformer extends RecursiveAction {

    private PathIndex pathIndex;
    private PID pid;
    private DepositModelManager modelManager;


    /**
     *
     */
    public ContentObjectTransformer() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void compute() {
        Map<Integer, Path> paths = pathIndex.getPaths(pid);
        Path foxmlPath = paths.get(OBJECT_TYPE);

        Document document;
        try {
            document = createSAXBuilder().build(newInputStream(foxmlPath));
        } catch (IOException | JDOMException e) {
            throw new RepositoryException("Failed to read FOXML for " + pid, e);
        }

    }

}
