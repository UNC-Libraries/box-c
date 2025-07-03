package edu.unc.lib.boxc.web.access.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.web.access.processing.CfTurnstileToken;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.util.SubnetUtils;

@Controller
public class BotChallengeController extends AbstractErrorHandlingSearchController {
    private static final Logger log = LoggerFactory.getLogger(BotChallengeController.class);

    @RequestMapping(value = "/api/challenge", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody
    String  turnstileJson(@RequestBody CfTurnstileToken token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        String ipAddress = request.getRemoteAddr();

        return turnstileVerification(ipAddress, token, session);
    }

    private String turnstileVerification(String ipAddress, CfTurnstileToken token, HttpSession session) {
        var turnstileRequstInfo = new HashMap<String, String>();
        turnstileRequstInfo.put("remoteip", ipAddress);
        turnstileRequstInfo.put("secret", "1x0000000000000000000000000000000AA");
        turnstileRequstInfo.put("response", token.getCfTurnstileToken());
        String turnstileData = SerializationUtil.objectToJSON(turnstileRequstInfo);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest turnstileRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(turnstileData))
                .build();
        try {
            HttpResponse<String> turnstileResponse = client.send(turnstileRequest, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode turnstileJson = mapper.readTree(turnstileResponse.body());
            var validationSucceeded = turnstileJson.get("validationSucceeded").asBoolean();
            if (validationSucceeded) {
                session.setAttribute("turnstileTokenExpiresIn", expiresIn());
            }

            session.setAttribute("validCfTurnstileToken", validationSucceeded);
            session.setAttribute("userIPAddress", ipAddress);

            return turnstileResponse.body();
        } catch (IOException | InterruptedException e) {
            log.warn("Unable to validate Turnstile token {}", e.getMessage());
            Map<String, Object> error_response = new HashMap<>();
            error_response.put("success", false);
            error_response.put("http_exception", e.getMessage());

            return SerializationUtil.objectToJSON(error_response);
        }
    }

    private Boolean hasUncAddress(String ipAddress, HttpSession session) {
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }

        var uncAddresses = List.of(
                "152.2.0.0/16", // Campus
                "152.19.0.0/16", // Campus
                "152.23.0.0/16", // Campus
                "152.54.0.0/20", // RENCI
                "172.17.0.0/18", // VPN
                "172.17.57.0/28", // Library-IT VPN group
                "198.85.230.0/23", // Off campus location
                "204.84.8.0/22", // Off campus location
                "204.84.252.0/22", // Off campus location
                "204.85.176.0/20", // Off campus location
                "204.85.192.0/18" // UNC Hospitals
        );

        var validUncAddress = false;

        for (var uncAddress : uncAddresses) {
            SubnetUtils utils = new SubnetUtils(uncAddress);
            if (utils.getInfo().isInRange(ipAddress)) {
                validUncAddress = true;
                break;
            };
        }

        var uncAddressSession = session.getAttribute("uncIPAddress");
        if (uncAddressSession == null) {
            session.setAttribute("uncIPAddress", validUncAddress);
        }

        return validUncAddress;
    }

    private Boolean hasValidTurnstileToken(HttpSession session) {
        var tokenExpiration = session.getAttribute("turnstileTokenExpiresIn");
        var userIPAddress = session.getAttribute("userIPAddress");

        if (tokenExpiration != null && userIPAddress != null) {
            return timeCheck((ZonedDateTime) tokenExpiration);
        }

        return false;
    }

    private ZonedDateTime getCurrentTime() {
        ZoneId timeZone = ZoneId.of("America/New_York");
        return ZonedDateTime.now(timeZone);
    }

    private ZonedDateTime expiresIn() {
        return getCurrentTime().plusHours(24L);
    }

    private boolean timeCheck(ZonedDateTime sessionTime) {
        return getCurrentTime().isBefore(sessionTime);
    }
}
