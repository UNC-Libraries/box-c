package edu.unc.lib.boxc.services.camel;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

/**
 *
 * @author bbpennel
 *
 */
public class BinaryMetadataProcessorTest {

    private BinaryMetadataProcessor processor;
    private AutoCloseable closeable;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private static final String FEDORA_BASE = "http://example.com/rest/";

    private String binaryBase;

    private static final String RESC_ID = "de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String RESC_URI = FEDORA_BASE + "content/de/75/d8/11/" + RESC_ID;
    private static final String MIMETYPE = "text/plain";
    private static final String CHECKSUM_PREFIX = "urn:sha1:";
    private static final String CHECKSUM = "61673dacf6c6eea104e77b151584ed7215388ea3";

    private PID binaryPid;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private BinaryObject binaryObject;
    private DerivativeService derivativeService;

    @Before
    public void init() throws Exception {
        closeable = openMocks(this);

        TestHelper.setContentBase(FEDORA_BASE);

        binaryBase = tmpFolder.newFolder().getAbsolutePath();

        derivativeService = new DerivativeService();
        derivativeService.setDerivativeDir(binaryBase);

        processor = new BinaryMetadataProcessor();
        processor.setRepositoryObjectLoader(repoObjLoader);
        processor.setDerivativeService(derivativeService);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getIn().getHeader(FCREPO_URI)).thenReturn(RESC_ID);

        binaryPid = PIDs.get(RESC_ID);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void validExternalBinary() throws Exception {
        File file = new File(binaryBase + "/61/67/3d/" + CHECKSUM);
        file.getParentFile().mkdirs();
        file.createNewFile();

        when(repoObjLoader.getBinaryObject(binaryPid)).thenReturn(binaryObject);
        when(binaryObject.getContentUri()).thenReturn(file.toPath().toUri());

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource(RESC_URI);
        resc.addProperty(RDF.type, Fcrepo4Repository.Binary);
        resc.addProperty(Ebucore.hasMimeType, MIMETYPE);
        resc.addProperty(Premis.hasMessageDigest, CHECKSUM_PREFIX + CHECKSUM);

        setMessageBody(model);

        processor.process(exchange);

        verify(message).setHeader(CdrBinaryMimeType, MIMETYPE);
        verify(message).setHeader(CdrBinaryPath, file.toPath().toString());
    }

    @Test
    public void nonbinaryObject() throws Exception {
        when(repoObjLoader.getBinaryObject(binaryPid)).thenThrow(new ObjectTypeMismatchException(""));

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource(RESC_ID);
        resc.addProperty(RDF.type, createResource(Fcrepo4Repository.Resource.getURI()));

        setMessageBody(model);

        processor.process(exchange);

        verify(message, never()).setHeader(eq(CdrBinaryPath), any());
    }

    @Test
    public void internalBinary() throws Exception {
        when(repoObjLoader.getBinaryObject(binaryPid)).thenReturn(binaryObject);
        when(binaryObject.getContentUri()).thenReturn(null);

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource(RESC_URI);
        resc.addProperty(RDF.type, Fcrepo4Repository.Binary);
        resc.addProperty(Ebucore.hasMimeType, MIMETYPE);
        resc.addProperty(Premis.hasMessageDigest, CHECKSUM_PREFIX + CHECKSUM);

        setMessageBody(model);

        processor.process(exchange);

        verify(message, never()).setHeader(eq(CdrBinaryPath), any());
    }

    private void setMessageBody(Model model) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            RDFDataMgr.write(bos, model, RDFFormat.TURTLE_PRETTY);
            when(message.getBody(eq(InputStream.class)))
                    .thenReturn(new ByteArrayInputStream(bos.toByteArray()));
        }
    }
}
