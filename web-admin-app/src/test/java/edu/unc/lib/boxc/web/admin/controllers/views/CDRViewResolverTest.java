package edu.unc.lib.boxc.web.admin.controllers.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

public class CDRViewResolverTest {

    private CDRViewResolver viewResolver;
    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        viewResolver = new CDRViewResolver();
        // Set the default properties
        viewResolver.setBaseView("baseView");
        viewResolver.setSubViewPrefix("/panels/");
        viewResolver.setSuffix(".jsp");
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testSuccessfulViewBuild() throws Exception {
        AbstractUrlBasedView result = viewResolver.buildView("testView");

        // Then we expect the result to be our mock view
        assertNotNull(result);
        assertEquals("baseView.jsp", result.getUrl());

        // And the contentPage attribute should be set in the attributesMap
        assertEquals("/panels/testView.jsp", viewResolver.getAttributesMap().get("contentPage"));
    }

    @Test
    public void testViewBuildException() {
        // View resolver will throw an exception if view class isn't set
        viewResolver.setViewClass(null);

        // When we call buildView, we expect a RepositoryException
        assertThrows(RepositoryException.class, () -> {
            viewResolver.buildView("failView");
        });
    }

    @Test
    public void testGetterAndSetterMethods() {
        // Test getters
        assertEquals("baseView", viewResolver.getBaseView());
        assertEquals("/panels/", viewResolver.getSubViewPrefix());

        // Test setters
        viewResolver.setBaseView("newBaseView");
        viewResolver.setSubViewPrefix("/newPanels/");

        assertEquals("newBaseView", viewResolver.getBaseView());
        assertEquals("/newPanels/", viewResolver.getSubViewPrefix());
    }
}