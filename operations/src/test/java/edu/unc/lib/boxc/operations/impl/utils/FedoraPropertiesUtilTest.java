package edu.unc.lib.boxc.operations.impl.utils;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class FedoraPropertiesUtilTest {
    @Mock
    private RepositoryObject repositoryObject;
    @Mock
    private Resource resource;
    @Mock
    private Property property;
    @Mock
    private Statement statement;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        when(repositoryObject.getResource()).thenReturn(resource);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void getValueOfNullPropertyTest() {
        assertNull(FedoraPropertiesUtil.getValue(repositoryObject, property));
    }

    @Test
    public void getValueOfPropertyTest() {
        when(resource.getProperty(eq(property))).thenReturn(statement);
        when(statement.getString()).thenReturn("best value ever");

        assertEquals("best value ever", FedoraPropertiesUtil.getValue(repositoryObject, property));
    }
}
