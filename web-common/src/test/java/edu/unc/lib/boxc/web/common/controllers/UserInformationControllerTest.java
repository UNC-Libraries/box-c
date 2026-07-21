package edu.unc.lib.boxc.web.common.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserInformationControllerTest {
    @InjectMocks
    private UserInformationController controller;
    private MockMvc mvc;
    private AutoCloseable closeable;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void setsUncAddressTrueForIpInConfiguredCidrRange() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(
                head("/api/userInformation")
                    .header("X-Forwarded-For", "152.2.0.0")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Length", "0"))
            .andExpect(request().sessionAttribute("userIPAddress", "152.2.0.0"))
            .andExpect(request().sessionAttribute("uncIPAddress", true))
            .andReturn()
            .getRequest()
            .getSession(false);

        assertNotNull(session);
    }

    @Test
    public void setsUncAddressFalseForIpOutsideConfiguredRanges() throws Exception {
        mvc.perform(
                head("/api/userInformation")
                    .header("X-Forwarded-For", "8.8.8.8")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Length", "0"))
            .andExpect(request().sessionAttribute("userIPAddress", "8.8.8.8"))
            .andExpect(request().sessionAttribute("uncIPAddress", false));
    }

    @Test
    public void usesFirstForwardedAddressAndEvaluatesAgainstRanges() throws Exception {
        mvc.perform(
                head("/api/userInformation")
                    .header("X-Forwarded-For", "152.2.0.0, 10.0.0.1")
            )
            .andExpect(status().isOk())
            .andExpect(request().sessionAttribute("userIPAddress", "152.2.0.0"))
            .andExpect(request().sessionAttribute("uncIPAddress", true));
    }

    @Test
    public void loopbackAddressIsTreatedAsUnc() throws Exception {
        mvc.perform(
                head("/api/userInformation")
                    .header("X-Real-IP", "152.2.0.0")
            )
            .andExpect(status().isOk())
            .andExpect(request().sessionAttribute("userIPAddress", "152.2.0.0"))
            .andExpect(request().sessionAttribute("uncIPAddress", true));
    }

    @Test
    public void nonIpv4AddressesAreNotTreatedAsUnc() throws Exception {
        mvc.perform(
                head("/api/userInformation")
                    .header("X-Forwarded-For", "::1")
            )
            .andExpect(status().isOk())
            .andExpect(request().sessionAttribute("userIPAddress", "::1"))
            .andExpect(request().sessionAttribute("uncIPAddress", false));
    }

    @Test
    public void ipv4MappedIpv6AddressIsNotTreatedAsUnc() throws Exception {
        mvc.perform(
                head("/api/userInformation")
                    .header("X-Forwarded-For", "::ffff:152.2.0.0")
            )
            .andExpect(status().isOk())
            .andExpect(request().sessionAttribute("userIPAddress", "::ffff:152.2.0.0"))
            .andExpect(request().sessionAttribute("uncIPAddress", false));
    }

    @Test
    public void fallsBackToRemoteAddrWhenNoForwardedHeadersPresent() throws Exception {
        mvc.perform(
                head("/api/userInformation")
                    .with(request -> {
                        request.setRemoteAddr("152.2.0.0");
                        return request;
                    })
            )
            .andExpect(status().isOk())
            .andExpect(request().sessionAttribute("userIPAddress", "152.2.0.0"))
            .andExpect(request().sessionAttribute("uncIPAddress", true));
    }

    @Test
    public void blankIpAddress() throws Exception {
        mvc.perform(
                head("/api/userInformation")
                    .with(request -> {
                        request.setRemoteAddr("");
                        return request;
                    })
            )
            .andExpect(status().isOk())
            .andExpect(request().sessionAttribute("userIPAddress", ""))
            .andExpect(request().sessionAttribute("uncIPAddress", false));
    }
}

