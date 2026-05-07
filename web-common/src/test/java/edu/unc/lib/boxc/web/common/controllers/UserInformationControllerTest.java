package edu.unc.lib.boxc.web.common.controllers;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserInformationControllerTest {
    private final UserInformationController controller = new UserInformationController();

    @Test
    public void setsUncAddressTrueForIpInConfiguredCidrRange() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("152.2.10.12");

        var response = controller.getUserInformation(request);
        HttpSession session = request.getSession(false);

        assertNotNull(session);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("152.2.10.12", session.getAttribute("userIPAddress"));
        assertEquals(Boolean.TRUE, session.getAttribute("uncIPAddress"));
    }

    @Test
    public void setsUncAddressFalseForIpOutsideConfiguredRanges() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");

        controller.getUserInformation(request);
        HttpSession session = request.getSession(false);

        assertNotNull(session);
        assertEquals("8.8.8.8", session.getAttribute("userIPAddress"));
        assertEquals(Boolean.FALSE, session.getAttribute("uncIPAddress"));
    }

    @Test
    public void usesFirstForwardedAddressAndEvaluatesAgainstRanges() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "152.19.255.1, 10.0.0.1");
        request.setRemoteAddr("10.0.0.1");

        controller.getUserInformation(request);
        HttpSession session = request.getSession(false);

        assertNotNull(session);
        assertEquals("152.19.255.1", session.getAttribute("userIPAddress"));
        assertEquals(Boolean.TRUE, session.getAttribute("uncIPAddress"));
    }

    @Test
    public void loopbackAddressIsTreatedAsUnc() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        controller.getUserInformation(request);
        HttpSession session = request.getSession(false);

        assertNotNull(session);
        assertEquals(Boolean.TRUE, session.getAttribute("uncIPAddress"));
    }

    @Test
    public void nonIpv4AddressesAreNotTreatedAsUnc() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "::1");
        request.setRemoteAddr("10.0.0.1");

        controller.getUserInformation(request);
        HttpSession session = request.getSession(false);

        assertNotNull(session);
        assertEquals("::1", session.getAttribute("userIPAddress"));
        assertEquals(Boolean.FALSE, session.getAttribute("uncIPAddress"));
    }

    @Test
    public void ipv4MappedIpv6AddressIsNotTreatedAsUnc() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "::ffff:152.19.255.1");
        request.setRemoteAddr("10.0.0.1");

        controller.getUserInformation(request);
        HttpSession session = request.getSession(false);

        assertNotNull(session);
        assertEquals("::ffff:152.19.255.1", session.getAttribute("userIPAddress"));
        assertEquals(Boolean.FALSE, session.getAttribute("uncIPAddress"));
    }
}

