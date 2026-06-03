package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.mimetype.CorrectMimetypesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CorrectMimetypesControllerTest {
    private static final PID PID_1 = PIDs.get("83c2d7f8-2e6b-4f0b-ab7e-7397969c0682");
    private static final PID PID_2 = PIDs.get("0e33ad0b-7a16-4bfa-b833-6126c262d889");
    private CorrectMimetypesController controller;

    @Mock
    private CorrectMimetypesService correctMimetypesService;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    public void setup() throws Exception {
        controller = new CorrectMimetypesController();
        controller.setService(correctMimetypesService);
    }

    @Test
    void correctMimetypesTest() throws Exception {
        String csv = """
                id,mimetype
                83c2d7f8-2e6b-4f0b-ab7e-7397969c0682,image/png
                """;

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(correctMimetypesService.correctMimetypes(
                any(InputStream.class),
                any(AgentPrincipals.class)))
                .thenReturn(List.of(PID_1, PID_2));

        ResponseEntity<Object> response = controller.correctMimetype(multipartFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = asMap(response.getBody());

        assertEquals("correct mimetypes", body.get("action"));
        assertEquals("Corrected mimetypes for " + List.of(PID_1, PID_2), body.get("status"));
        assertTrue(body.containsKey("timestamp"));
        assertFalse(body.containsKey("error"));

        verify(correctMimetypesService).correctMimetypes(
                any(InputStream.class),
                any(AgentPrincipals.class));
    }

    @Test
    public void emptyFileTest() {
        when(multipartFile.isEmpty()).thenReturn(true);

        ResponseEntity<Object> response = controller.correctMimetype(multipartFile);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = asMap(response.getBody());

        assertEquals("correct mimetypes", body.get("action"));
        assertEquals("CSV file is required", body.get("error"));
        assertTrue(body.containsKey("timestamp"));

        verifyNoInteractions(correctMimetypesService);
    }

    @Test
    public void nullFileTest() {
        ResponseEntity<Object> response = controller.correctMimetype(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> body = asMap(response.getBody());

        assertEquals("correct mimetypes", body.get("action"));
        assertEquals("CSV file is required", body.get("error"));
        assertTrue(body.containsKey("timestamp"));

        verifyNoInteractions(correctMimetypesService);
    }

    @Test
    public void correctMimetypesFailTest() throws Exception {
        String csv = """
                id,mimetype
                83c2d7f8-2e6b-4f0b-ab7e-7397969c0682,image/png
                """;

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        when(correctMimetypesService.correctMimetypes(
                any(InputStream.class),
                any(AgentPrincipals.class)))
                .thenThrow(new AccessRestrictionException());

        ResponseEntity<Object> response = controller.correctMimetype(multipartFile);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        Map<String, Object> body = asMap(response.getBody());

        assertEquals("correct mimetypes", body.get("action"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("timestamp"));
    }

    private Map<String, Object> asMap(Object body) {
        assertNotNull(body);
        assertTrue(body instanceof Map);
        return (Map<String, Object>) body;
    }
}
