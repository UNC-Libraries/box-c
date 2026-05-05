package edu.unc.lib.boxc.web.access.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles Cloudflare Turnstile challenge verification and UNC network bypass rules
 * for access requests.
 * @author lfarrell
 */
@Controller
public class BotChallengeController {
    private static final Logger log = LoggerFactory.getLogger(BotChallengeController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Long TOKEN_TTL_HOURS = 24L;
    private String turnstileSecret;



    @RequestMapping(value = "/api/challenge", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody
    String  turnstileJson(@RequestBody CfTurnstileToken token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        String ipAddress = getClientIpAddress( request);
        boolean uncAddress = Boolean.TRUE.equals(session.getAttribute("uncIPAddress"));
        if (uncAddress || hasValidTurnstileToken(session)) {
            setSuccessSessionState(session, uncAddress);
            return SerializationUtil.objectToJSON(Map.of("success", true));
        }

        return turnstileVerification(ipAddress, token, session);
    }

    private String turnstileVerification(String ipAddress, CfTurnstileToken token, HttpSession session) {
        String turnstileData = setTurnstileRequestInfo(ipAddress, token);
        HttpRequest turnstileRequest = buildTurnstileRequest(turnstileData);

        try {
            HttpResponse<String> turnstileResponse = sendTurnstileRequest(turnstileRequest);
            JsonNode turnstileJson = OBJECT_MAPPER.readTree(turnstileResponse.body());
            var validationSucceeded = turnstileJson.get("success").asBoolean();
            setSuccessSessionState(session, validationSucceeded);
            return turnstileResponse.body();
        } catch (IOException | InterruptedException e) {
            log.warn("Unable to validate Turnstile token {}", e.getMessage());
            setErrorSessionState(session);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return SerializationUtil.objectToJSON(errorResponse);
        }
    }

    private String setTurnstileRequestInfo(String ipAddress, CfTurnstileToken token) {
        var turnstileRequestInfo = new HashMap<String, String>();
        turnstileRequestInfo.put("remoteip", ipAddress);
        turnstileRequestInfo.put("secret", turnstileSecret);
        turnstileRequestInfo.put("response", token.getCfTurnstileToken());
        return SerializationUtil.objectToJSON(turnstileRequestInfo);
    }

    protected HttpResponse<String> sendTurnstileRequest(HttpRequest turnstileRequest) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        return client.send(turnstileRequest, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildTurnstileRequest(String turnstileData) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(turnstileData))
                .build();
    }

    private void setSuccessSessionState(HttpSession session, boolean validationSucceeded) {
        if (validationSucceeded) {
            session.setAttribute("turnstileTokenExpiresIn", expiresIn());
        }
        session.setAttribute("validCfTurnstileToken", validationSucceeded);
    }

    private void setErrorSessionState(HttpSession session) {
        session.setAttribute("turnstileTokenExpiresIn", null);
        session.setAttribute("validCfTurnstileToken", false);
    }


    private Boolean hasValidTurnstileToken(HttpSession session) {
        var tokenExpiration = session.getAttribute("turnstileTokenExpiresIn");
        var userIPAddress = session.getAttribute("userIPAddress");

        if (tokenExpiration != null && userIPAddress != null) {
            return timeCheck((ZonedDateTime) tokenExpiration);
        }

        return false;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can be a comma-separated list: client, proxy1, proxy2
            // The leftmost value is the original client IP
            return xForwardedFor.split(",")[0].trim();
        }
        // Fallback to X-Real-IP if set by Apache
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    private ZonedDateTime getCurrentTime() {
        ZoneId timeZone = ZoneId.of("America/New_York");
        return ZonedDateTime.now(timeZone);
    }

    private ZonedDateTime expiresIn() {
        return getCurrentTime().plusHours(TOKEN_TTL_HOURS);
    }

    private boolean timeCheck(ZonedDateTime sessionTime) {
        return getCurrentTime().isBefore(sessionTime);
    }

    @Autowired
    public void setTurnstileSecret(@Qualifier("turnstileSecret") String turnstileSecret) {
        if (turnstileSecret == null || turnstileSecret.isBlank()) {
            throw new IllegalArgumentException("turnstileSecret must be configured");
        }
        this.turnstileSecret = turnstileSecret;
    }

    public static class CfTurnstileToken {
        private String cfTurnstileToken;

        public String getCfTurnstileToken() {
            return cfTurnstileToken;
        }

        public void setCfTurnstileToken(String token) {
            this.cfTurnstileToken = token;
        }
    }
}
