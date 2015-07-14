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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.UnsupportedContentModelException;

public class DocumentIndexingPipelineTest extends Assert {
	private DocumentIndexingPackageFactory factory;
	@Mock
	private DocumentIndexingPackageDataLoader loader;
	
	@Before
	public void setup() throws Exception {
		initMocks(this);
		
		factory = new DocumentIndexingPackageFactory();
		factory.setDataLoader(loader);
	}
	
	@Test(expected=UnsupportedContentModelException.class)
	public void depositReceipt() throws Exception {
		DocumentIndexingPackage dip = factory.createDip("info:fedora/uuid:test");

		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/depositReceipt.xml")));
		dip.setFoxml(foxml);

		DocumentIndexingPipeline pipeline = new DocumentIndexingPipeline();
		pipeline.process(dip);
	}
}
