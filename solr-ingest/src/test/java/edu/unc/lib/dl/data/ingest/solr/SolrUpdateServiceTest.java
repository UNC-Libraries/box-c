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
package edu.unc.lib.dl.data.ingest.solr;

import java.lang.ref.WeakReference;

import javax.annotation.Resource;
import static org.mockito.Mockito.*;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.dl.data.ingest.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import org.jdom.Document;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/solr-update-service-context.xml" })
public class SolrUpdateServiceTest {

	@Resource
	SolrUpdateService solrUpdateService;
	
	@Resource 
	ManagementClient managementClient;
	
	@Resource
	DocumentIndexingPackageFactory dipFactory;
	
	@Resource
	DocumentIndexingPipeline solrFullUpdatePipeline;
	
	@Resource
	SolrUpdateDriver solrUpdateDriver;
	
	@Resource
	AccessClient accessClient;
	
	//@Test
	public void stressFinishedTest() throws Exception {
		Document document = mock(Document.class);
		when(managementClient.getObjectXML(any(PID.class))).thenReturn(document);
		
		long startTime = System.currentTimeMillis();
		int documentCount = 500000;
		
		for (int i=0; i < documentCount; i++){
			solrUpdateService.offer("uuid:test"+i, SolrUpdateAction.ADD);
			if (i % 50 == 0)
				System.out.println("Walk count: " + walkCount(solrUpdateService.getRoot(), 0));
			if (i % 100 == 0) {
				System.out.println("*********************COUNTS*********************");
				System.out.println("Queued: " + solrUpdateService.getPidQueue().size());
				System.out.println("Blocked: " + solrUpdateService.getLockedPids().size());
				System.out.println("Finished: " + solrUpdateService.getFinishedMessages().size());
				System.out.println("Failed: " + solrUpdateService.getFailedMessages().size());
				System.out.println("Root Children: " + solrUpdateService.getRoot().getChildren().size());
				reset(solrFullUpdatePipeline);
				reset(solrUpdateDriver);
				reset(managementClient);
				when(managementClient.getObjectXML(any(PID.class))).thenReturn(document);
				reset(accessClient);
			}
			if (i % 500 == 0) {
				System.out.println("*********************GC TIME*********************");
				//System.gc();
			}
		}
		//Thread.sleep(5000L);
		System.out.println("Queue: " + solrUpdateService.getPidQueue());
		//System.out.println("Finished: " + solrUpdateService.getFinishedMessages());
		for (int i=0; i<10; i++)
			System.out.println(i + ":" + solrUpdateService.getFinishedMessages().get(i).getPid());
		System.out.println("Finished: " + solrUpdateService.getFinishedMessages().size());
		System.out.println(solrUpdateService.getRoot().getChildrenPending());
		System.out.println(solrUpdateService.getRoot().getChildrenProcessed());
		
		System.out.print("Completed stress test of " + documentCount + " in " + (System.currentTimeMillis() - startTime) + "ms");
	}
	
	public long walkCount(UpdateNodeRequest node, long count) {
		if (node == null)
			return count;
		count++;
		if (node.getChildren() == null)
			return count;
		for (WeakReference<UpdateNodeRequest> child: node.getChildren()) {
			if (child != null && child.get() != null)
				count = walkCount(child.get(), count);
		}
		return count;
	}
}
