package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.web.access.processing.CfTurnstileToken;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BotChallengeControllerTest {
    private static final String NON_UNC_IP_ADDRESS = "8.8.8.8";
    private static final String UNC_IP_ADDRESS = "152.2.5.10";

    @Test
    public void turnstileJsonSkipsVerificationForUncAddress() throws Exception {
        BotChallengeController controller = controllerSpy();
        MockHttpServletRequest request = requestWithIp(UNC_IP_ADDRESS);

        String result = controller.turnstileJson(token(), request, new MockHttpServletResponse());

        assertTrue(result.contains("\"success\":true"));
        verify(controller, never()).sendTurnstileRequest(any(HttpRequest.class));
    }

    @Test
    public void turnstileJsonSkipsVerificationForIpv4LoopbackAddress() throws Exception {
        BotChallengeController controller = controllerSpy();
        MockHttpServletRequest request = requestWithIp("127.0.0.1");

        String result = controller.turnstileJson(token(), request, new MockHttpServletResponse());

        assertTrue(result.contains("\"success\":true"));
        verify(controller, never()).sendTurnstileRequest(any(HttpRequest.class));
    }

    @Test
    public void turnstileJsonSkipsVerificationWhenSessionHasUncIpFlag() throws Exception {
        BotChallengeController controller = controllerSpy();
        MockHttpServletRequest request = requestWithIp(NON_UNC_IP_ADDRESS);
        Objects.requireNonNull(request.getSession(true)).setAttribute("uncIPAddress", true);

        String result = controller.turnstileJson(token(), request, new MockHttpServletResponse());

        assertTrue(result.contains("\"success\":true"));
        verify(controller, never()).sendTurnstileRequest(any(HttpRequest.class));
    }

    @Test
    public void turnstileJsonSkipsVerificationForValidSessionToken() throws Exception {
        BotChallengeController controller = controllerSpy();
        MockHttpServletRequest request = requestWithIp(NON_UNC_IP_ADDRESS);
        Objects.requireNonNull(request.getSession(true)).setAttribute("turnstileTokenExpiresIn",
                ZonedDateTime.now(ZoneId.of("America/New_York")).plusMinutes(30));
        Objects.requireNonNull(request.getSession()).setAttribute("userIPAddress", NON_UNC_IP_ADDRESS);

        String result = controller.turnstileJson(token(), request, new MockHttpServletResponse());

        assertTrue(result.contains("\"success\":true"));
        verify(controller, never()).sendTurnstileRequest(any(HttpRequest.class));
    }

    @Test
    public void turnstileJsonExpiredSessionTokenFallsBackToVerification() throws Exception {
        BotChallengeController controller = controllerSpy();
        MockHttpServletRequest request = requestWithIp(NON_UNC_IP_ADDRESS);
        Objects.requireNonNull(request.getSession(true)).setAttribute("turnstileTokenExpiresIn",
                ZonedDateTime.now(ZoneId.of("America/New_York")).minusMinutes(1));
        Objects.requireNonNull(request.getSession()).setAttribute("userIPAddress", NON_UNC_IP_ADDRESS);
        doReturn(turnstileResponse("{\"success\":false}")).when(controller).sendTurnstileRequest(any(HttpRequest.class));

        controller.turnstileJson(token(), request, new MockHttpServletResponse());

        verify(controller).sendTurnstileRequest(any(HttpRequest.class));
        assertEquals(Boolean.FALSE, request.getSession().getAttribute("validCfTurnstileToken"));
        assertEquals(NON_UNC_IP_ADDRESS, request.getSession().getAttribute("userIPAddress"));
    }

    @Test
    public void turnstileJsonVerificationSuccessSetsExpiryAndTokenFlag() throws Exception {
        BotChallengeController controller = controllerSpy();
        MockHttpServletRequest request = requestWithIp(NON_UNC_IP_ADDRESS);
        doReturn(turnstileResponse("{\"success\":true}")).when(controller).sendTurnstileRequest(any(HttpRequest.class));

        String result = controller.turnstileJson(token(), request, new MockHttpServletResponse());

        verify(controller).sendTurnstileRequest(any(HttpRequest.class));
        assertTrue(result.contains("\"success\":true"));
        assertEquals(Boolean.TRUE, Objects.requireNonNull(request.getSession()).getAttribute("validCfTurnstileToken"));
        assertNotNull(request.getSession().getAttribute("turnstileTokenExpiresIn"));
        assertEquals(NON_UNC_IP_ADDRESS, request.getSession().getAttribute("userIPAddress"));
    }

    @Test
    public void turnstileJsonVerificationFailureSetsErrorState() throws Exception {
        BotChallengeController controller = controllerSpy();
        MockHttpServletRequest request = requestWithIp(NON_UNC_IP_ADDRESS);
        doThrow(new IOException("bad token")).when(controller).sendTurnstileRequest(any(HttpRequest.class));

        String result = controller.turnstileJson(token(), request, new MockHttpServletResponse());

        verify(controller).sendTurnstileRequest(any(HttpRequest.class));
        assertTrue(result.contains("bad token"));
        assertEquals(Boolean.FALSE, Objects.requireNonNull(request.getSession()).getAttribute("validCfTurnstileToken"));
        assertEquals(NON_UNC_IP_ADDRESS, request.getSession().getAttribute("userIPAddress"));
        assertNotEquals(Boolean.TRUE, request.getSession().getAttribute("uncIPAddress"));
    }

    private BotChallengeController controllerSpy() {
        BotChallengeController controller = spy(new BotChallengeController());
        controller.setTurnstileSecret("test-secret");
        return controller;
    }

    private CfTurnstileToken token() {
        CfTurnstileToken token = new CfTurnstileToken();
        token.setCfTurnstileToken("1234");
        return token;
    }

    private MockHttpServletRequest requestWithIp(String ipAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ipAddress);
        return request;
    }

    private HttpResponse<String> turnstileResponse(String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(body);
        return response;
    }
}
