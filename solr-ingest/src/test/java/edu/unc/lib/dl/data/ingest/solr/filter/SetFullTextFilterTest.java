package edu.unc.lib.dl.data.ingest.solr.filter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 *
 * @author bbpennel
 * @date Mar 12, 2014
 */
public class SetFullTextFilterTest extends Assert {

	@Mock
	private AccessClient accessClient;
	@Mock
	private TripleStoreQueryService tsqs;
	@Mock
	private DocumentIndexingPackage dip;
	@Mock
	private IndexDocumentBean idb;
	@Mock
	private MIMETypedStream stream;
	@Mock
	Map<String, List<String>> triples;
	@Mock
	List<String> tripleValues;

	private SetFullTextFilter filter;

	@Before
	public void setup() throws Exception {

		initMocks(this);

		filter = new SetFullTextFilter();
		filter.setAccessClient(accessClient);
		filter.setTripleStoreQueryService(tsqs);

		when(stream.getStream()).thenReturn("Full text value".getBytes());

		when(dip.getDocument()).thenReturn(idb);
		when(dip.getTriples()).thenReturn(null);

		when(accessClient.getDatastreamDissemination(any(PID.class), eq(Datastream.MD_FULL_TEXT.name()), anyString()))
				.thenReturn(stream);

		when(tsqs.fetchBySubjectAndPredicate(any(PID.class), eq(ContentModelHelper.CDRProperty.fullText.toString())))
				.thenReturn(tripleValues);

		when(tripleValues.size()).thenReturn(0);
		when(triples.get(eq(ContentModelHelper.CDRProperty.fullText.toString()))).thenReturn(tripleValues);
	}

	@Test
	public void testNoTriplesNoFullText() {

		when(tripleValues.size()).thenReturn(0);

		filter.filter(dip);

		verify(idb, never()).setFullText(anyString());

	}

	@Test
	public void testNoTriplesFullText() throws Exception {

		when(tripleValues.size()).thenReturn(1);
		when(tripleValues.get(eq(0))).thenReturn("pid/MD_FULL_TEXT");

		filter.filter(dip);

		verify(stream).getStream();
		verify(idb).setFullText(anyString());

	}

	@Test
	public void testTriplesFullText() throws Exception {

		when(tripleValues.size()).thenReturn(1);
		when(tripleValues.get(eq(0))).thenReturn("pid/MD_FULL_TEXT");

		when(dip.getTriples()).thenReturn(triples);

		filter.filter(dip);

		verify(stream).getStream();
		verify(idb).setFullText(anyString());

	}

	@Test
	public void testTriplesNoFullText() {

		when(tsqs.fetchBySubjectAndPredicate(any(PID.class), eq(ContentModelHelper.CDRProperty.fullText.toString())))
				.thenReturn(null);

		filter.filter(dip);

		verify(idb, never()).setFullText(anyString());

	}

	@Test
	public void testTriplesFalseFullText() throws Exception {

		when(tripleValues.size()).thenReturn(1);
		when(tripleValues.get(eq(0))).thenReturn("false");

		when(dip.getTriples()).thenReturn(triples);

		filter.filter(dip);

		verify(stream, never()).getStream();
		verify(idb, never()).setFullText(anyString());

		verify(tripleValues).get(eq(0));

	}

}
