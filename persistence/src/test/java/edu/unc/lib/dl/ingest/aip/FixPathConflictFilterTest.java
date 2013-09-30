package edu.unc.lib.dl.ingest.aip;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.METSPackageSIPProcessor;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class FixPathConflictFilterTest {

	@Resource
	private METSPackageSIPProcessor metsPackageSIPProcessor = null;

	@Resource
	private TripleStoreQueryService tripleStoreQueryService;
	
	@Before
	public void setUp() throws Exception {
		Mockito.reset(this.tripleStoreQueryService);
	}

	@Test
	public void testSimplePathConflict() throws Exception {
		
		when(this.tripleStoreQueryService.lookupRepositoryPath(eq(new PID("pid:collection")))).thenReturn("collection");
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection"))).thenReturn(new PID("pid:collection"));
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection/slug"))).thenReturn(new PID("pid:slug"));
		
		// Contains one top-level object which will have slug "slug"
		
		RDFAwareAIPImpl aip = getAIP("src/test/resources/slug-conflict-test.zip");
		HashMap<String, PID> inserted = getSlugMap(aip);
		
		FixPathConflictFilter filter = new FixPathConflictFilter();
		filter.setTripleStoreQueryService(tripleStoreQueryService);

		filter.doFilter(aip);
		
		assertEquals("'slug' should be renamed to 'slug_1'",
				"slug_1",
				JRDFGraphUtil.getRelatedLiteralObject(aip.getGraph(), inserted.get("slug"), ContentModelHelper.CDRProperty.slug.getURI()));
		
	}

	@Test
	public void testIncrementsTwice() throws Exception {
		
		when(this.tripleStoreQueryService.lookupRepositoryPath(eq(new PID("pid:collection")))).thenReturn("collection");
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection"))).thenReturn(new PID("pid:collection"));
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection/slug"))).thenReturn(new PID("pid:slug"));
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection/slug_1"))).thenReturn(new PID("pid:slug_1"));
		
		// Contains one top-level object which will have slug "slug"

		RDFAwareAIPImpl aip = getAIP("src/test/resources/slug-conflict-test.zip");
		HashMap<String, PID> inserted = getSlugMap(aip);
		
		FixPathConflictFilter filter = new FixPathConflictFilter();
		filter.setTripleStoreQueryService(tripleStoreQueryService);

		filter.doFilter(aip);
		
		assertEquals("'slug' should be renamed to 'slug_2'",
				"slug_2",
				JRDFGraphUtil.getRelatedLiteralObject(aip.getGraph(), inserted.get("slug"), ContentModelHelper.CDRProperty.slug.getURI()));
		
	}

	@Test
	public void testTwoAtTop() throws Exception {
		
		when(this.tripleStoreQueryService.lookupRepositoryPath(eq(new PID("pid:collection")))).thenReturn("collection");
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection"))).thenReturn(new PID("pid:collection"));
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection/slug"))).thenReturn(new PID("pid:slug"));
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection/slug_2"))).thenReturn(new PID("pid:slug_2"));
		
		// Contains two top-level objects which will have slugs "slug" and "slug_1"

		RDFAwareAIPImpl aip = getAIP("src/test/resources/slug-conflict-test-two-at-top.zip");
		HashMap<String, PID> inserted = getSlugMap(aip);
		
		FixPathConflictFilter filter = new FixPathConflictFilter();
		filter.setTripleStoreQueryService(tripleStoreQueryService);

		filter.doFilter(aip);
		
		assertEquals("'slug' should be renamed to 'slug_3'",
				"slug_3",
				JRDFGraphUtil.getRelatedLiteralObject(aip.getGraph(), inserted.get("slug"), ContentModelHelper.CDRProperty.slug.getURI()));
		
		assertEquals("'slug_1' should still be named 'slug_1'",
				"slug_1",
				JRDFGraphUtil.getRelatedLiteralObject(aip.getGraph(), inserted.get("slug_1"), ContentModelHelper.CDRProperty.slug.getURI()));
		
	}

	@Test
	public void testAggregate() throws Exception {
		
		when(this.tripleStoreQueryService.lookupRepositoryPath(eq(new PID("pid:collection")))).thenReturn("collection");
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection"))).thenReturn(new PID("pid:collection"));
		when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("collection/slug"))).thenReturn(new PID("pid:slug"));

		RDFAwareAIPImpl aip = getAIP("src/test/resources/slug-conflict-test-aggregate.zip");
		HashMap<String, PID> inserted = getSlugMap(aip);
		
		FixPathConflictFilter filter = new FixPathConflictFilter();
		filter.setTripleStoreQueryService(tripleStoreQueryService);

		filter.doFilter(aip);
		
		assertEquals("'slug' should be renamed to 'slug_1'",
				"slug_1",
				JRDFGraphUtil.getRelatedLiteralObject(aip.getGraph(), inserted.get("slug"), ContentModelHelper.CDRProperty.slug.getURI()));
		
	}
	
	private RDFAwareAIPImpl getAIP(String zipPackagePath) throws Exception {
		
		File ingestPackage = new File(zipPackagePath);
		PID containerPID = new PID("pid:collection");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);

		DepositRecord record = new DepositRecord("test", "test", DepositMethod.SWORD20);
		record.setPackagingType(PackagingType.SIMPLE_ZIP);

		RDFAwareAIPImpl aip = (RDFAwareAIPImpl) metsPackageSIPProcessor.createAIP(sip, record);
		
		return aip;
		
	}
	
	private HashMap<String, PID> getSlugMap(RDFAwareAIPImpl aip) {
		
		HashMap<String, PID> map = new HashMap<String, PID>();
	
		for (PID pid : aip.getTopPIDs()) {
			String slug = JRDFGraphUtil.getRelatedLiteralObject(aip.getGraph(), pid, ContentModelHelper.CDRProperty.slug.getURI());
			map.put(slug,  pid);
		}
		
		return map;
		
	}

}
