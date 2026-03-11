package edu.unc.lib.boxc.services.camel.machineGenerated;

import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class MachineGenDescriptionProcessorTest {
    private MachineGenDescriptionProcessor processor;

    private AutoCloseable closeable;
    @Mock
    private FileObject fileObject;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender indexingMessageSender;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        when(repositoryObjectLoader.getFileObject(any())).thenReturn(fileObject);
        when(fileObject.getOriginalFile())
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
}
