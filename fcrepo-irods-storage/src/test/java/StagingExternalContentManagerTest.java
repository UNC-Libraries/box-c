import static org.junit.Assert.*;

import java.net.URI;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingArea;
import fedorax.server.module.storage.IrodsExternalContentManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-context.xml" })
public class StagingExternalContentManagerTest {

	@Autowired
	IrodsExternalContentManager externalContentManager;
	
	@Test
	public void testTagLocally() throws ServerException {
		Stages stages = externalContentManager.getStages();
		StagingArea shc = stages.getAllAreas().get(URI.create("tag:cdr.lib.unc.edu,2013:/storhouse_shc/"));
		assertTrue("The SHC test stage must be connected: "+shc.getStatus(), shc.isConnected());
		String testURL = "tag:joey@cdr.lib.unc.edu,2013:/storhouse_shc/my+project/_-1/test+file.txt";
		ContentManagerParams params = new ContentManagerParams(testURL);
		MIMETypedStream mts = externalContentManager.getExternalContent(params);
		assertNotNull("Stream result must not be null", mts);
	}

}
