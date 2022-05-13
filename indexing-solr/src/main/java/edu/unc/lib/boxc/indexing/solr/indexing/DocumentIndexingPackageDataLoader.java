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
package edu.unc.lib.boxc.indexing.solr.indexing;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;

import java.io.IOException;
import java.io.InputStream;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.exception.ObjectTombstonedException;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads data to populate fields in a DocumentIndexingPackage
 *
 * @author bbpennel
 * @date Jun 22, 2015
 */
public class DocumentIndexingPackageDataLoader {
    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingPackageDataLoader.class);
    private RepositoryObjectLoader repoObjLoader;

    private long cacheTimeToLive;
    private long cacheMaxSize;

    public Element loadMods(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject contentObj = getContentObject(dip);
        BinaryObject modsBinary = contentObj.getDescription();

        if (modsBinary == null) {
            return null;
        }

        try (InputStream modsStream = modsBinary.getBinaryStream()) {
            Document dsDoc = createSAXBuilder().build(modsStream);
            return dsDoc.detachRootElement();
        } catch (JDOMException | IOException | FedoraException e) {
            log.error("Failed to parse MODS stream for object {}", dip.getPid(), e);
            return null;
        }
    }

    public ContentObject getContentObject(DocumentIndexingPackage dip) throws IndexingException {
        RepositoryObject repoObj = repoObjLoader.getRepositoryObject(dip.getPid());
        if (repoObj instanceof Tombstone) {
            throw new ObjectTombstonedException("Object " + dip.getPid() + " is a tombstone");
        }
        if (!(repoObj instanceof ContentObject)) {
            throw new IndexingException("Object " + dip.getPid() + " is not a ContentObject");
        }
        return (ContentObject) repoObj;
    }

    public long getCacheTimeToLive() {
        return cacheTimeToLive;
    }

    public void setCacheTimeToLive(long cacheTimeToLive) {
        this.cacheTimeToLive = cacheTimeToLive;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }
}