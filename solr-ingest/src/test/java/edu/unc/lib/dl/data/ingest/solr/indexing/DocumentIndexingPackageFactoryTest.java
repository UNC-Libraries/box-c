package edu.unc.lib.dl.data.ingest.solr.indexing;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class DocumentIndexingPackageFactoryTest extends Assert {

	@Mock
	private ManagementClient managementClient;
	@Mock
	private AccessClient accessClient;

	@Mock
	private MIMETypedStream datastream;
	@Mock
	private Document mockFoxml;

	private DocumentIndexingPackageFactory dipFactory;

	@Before
	public void setup() {
		initMocks(this);

		dipFactory = new DocumentIndexingPackageFactory();
		dipFactory.setAccessClient(accessClient);
		dipFactory.setManagementClient(managementClient);
	}

	@Test
	public void testCreateDocumentIndexingPackageWithMDContents() throws Exception {

		byte[] stream = IOUtils.toByteArray(getClass().getResourceAsStream("/datastream/mdContents.xml"));

		when(datastream.getStream()).thenReturn(stream);
		when(accessClient.getDatastreamDissemination(any(PID.class), eq(Datastream.MD_CONTENTS.getName()), anyString()))
				.thenReturn(datastream);

		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackageWithMDContents(new PID("pid"));

		assertNotNull("Factory must return a dip", dip);
		assertNotNull("Factory must have assigned md contents", dip.getMdContents());

		verify(accessClient)
				.getDatastreamDissemination(any(PID.class), eq(Datastream.MD_CONTENTS.getName()), anyString());
	}

	@Test
	public void testCreateDIPWithoutMDContents() throws Exception {

		when(accessClient.getDatastreamDissemination(any(PID.class), eq(Datastream.MD_CONTENTS.getName()), anyString()))
			.thenThrow(new NotFoundException(""));

		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackageWithMDContents(new PID("pid"));

		assertNotNull("Factory must return a dip", dip);
		assertNull("No MD-CONTENTS should have been assigned", dip.getMdContents());

		verify(accessClient)
				.getDatastreamDissemination(any(PID.class), eq(Datastream.MD_CONTENTS.getName()), anyString());
	}

	@Test(expected=IndexingException.class)
	public void testCreateDIPWithMDContentsFedoraException() throws Exception {

		when(accessClient.getDatastreamDissemination(any(PID.class), eq(Datastream.MD_CONTENTS.getName()), anyString()))
			.thenThrow(new FedoraException(""));

		try {
			dipFactory.createDocumentIndexingPackageWithMDContents(new PID("pid"));
		} finally {
			verify(accessClient).getDatastreamDissemination(any(PID.class), eq(Datastream.MD_CONTENTS.getName()),
					anyString());
		}

	}

	@Test
	public void testCreateDocumentIndexingPackageWithRelsExt() throws Exception {

		byte[] stream = IOUtils.toByteArray(getClass().getResourceAsStream("/datastream/relsExt.xml"));

		when(datastream.getStream()).thenReturn(stream);
		when(accessClient.getDatastreamDissemination(any(PID.class), eq(Datastream.RELS_EXT.getName()), anyString()))
				.thenReturn(datastream);

		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackageWithRelsExt(new PID("pid"));

		assertNotNull("Factory must return a dip", dip);
		assertNotNull("Factory must have assigned md contents", dip.getRelsExt());

		verify(accessClient).getDatastreamDissemination(any(PID.class), eq(Datastream.RELS_EXT.getName()), anyString());

	}

	@Test(expected = IndexingException.class)
	public void testCreateDocumentIndexingPackageWithNoRelsExt() throws Exception {

		when(accessClient.getDatastreamDissemination(any(PID.class), eq(Datastream.RELS_EXT.getName()), anyString()))
				.thenThrow(new NotFoundException(""));

		try {
			dipFactory.createDocumentIndexingPackageWithRelsExt(new PID("pid"));
		} finally {
			verify(accessClient)
					.getDatastreamDissemination(any(PID.class), eq(Datastream.RELS_EXT.getName()), anyString());
		}

	}

	@Test
	public void testCreateDocumentIndexingPackageFOXML() throws Exception {

		Document foxml = ClientUtils.parseXML(IOUtils.toByteArray(getClass().getResourceAsStream(
				"/foxml/fileOctetStream.xml")));

		when(managementClient.getObjectXML(any(PID.class))).thenReturn(foxml);

		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(new PID("pid"));

		assertNotNull("Factory must return a dip", dip);
		assertNotNull("Factory must have assigned foxml", dip.getFoxml());

		verify(managementClient).getObjectXML(any(PID.class));

	}

	@Test
	public void testCreateDocumentIndexingPackageRetries() throws Exception {

		dipFactory.setRetryDelay(1L);
		dipFactory.setMaxRetries(3);

		Document foxml = ClientUtils.parseXML(IOUtils.toByteArray(getClass().getResourceAsStream(
				"/foxml/fileOctetStream.xml")));

		when(managementClient.getObjectXML(any(PID.class))).thenReturn(null, null, foxml);

		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(new PID("pid"));

		assertNotNull("Factory must return a dip", dip);
		assertNotNull("Factory must have assigned foxml", dip.getFoxml());

		verify(managementClient, times(3)).getObjectXML(any(PID.class));

	}

	@Test(expected = IndexingException.class)
	public void testCreateDocumentIndexingPackageNoFOXML() throws Exception {

		dipFactory.setRetryDelay(1L);
		dipFactory.setMaxRetries(3);

		when(managementClient.getObjectXML(any(PID.class))).thenReturn(null);

		try {
			dipFactory.createDocumentIndexingPackage(new PID("pid"));
		} finally {
			verify(managementClient, times(3)).getObjectXML(any(PID.class));
		}

	}

	@Test
	public void testCreateDocumentIndexingPackageServiceException() throws Exception {

		dipFactory.setRetryDelay(1L);
		dipFactory.setMaxRetries(3);

		when(managementClient.getObjectXML(any(PID.class))).thenThrow(new ServiceException("")).thenReturn(mockFoxml);

		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(new PID("pid"));

		verify(managementClient, times(2)).getObjectXML(any(PID.class));
		assertNotNull(dip);
	}

	@Test(expected = IndexingException.class)
	public void testCreateDocumentIndexingPackageMultipleServiceException() throws Exception {

		dipFactory.setRetryDelay(1L);
		dipFactory.setMaxRetries(3);

		when(managementClient.getObjectXML(any(PID.class))).thenThrow(new ServiceException(""));

		try {
			dipFactory.createDocumentIndexingPackage(new PID("pid"));
		} finally {
			verify(managementClient, times(3)).getObjectXML(any(PID.class));
		}

	}

	@Test(expected = IndexingException.class)
	public void testCreateDocumentIndexingPackageFOXMLException() throws Exception {

		when(managementClient.getObjectXML(any(PID.class))).thenThrow(new FedoraException(""));

		try {
			dipFactory.createDocumentIndexingPackage(new PID("pid"));
		} finally {
			verify(managementClient).getObjectXML(any(PID.class));
		}

	}
}
