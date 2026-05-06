package edu.unc.lib.boxc.web.common.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

/**
 * Returns headers with user information, such as username, admin access and uncIpAddress
 * @author lfarrell
 */
@Controller
public class UserInformationController {
    private static final List<String> UNC_ADDRESSES = List.of(
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

    private static final List<IpAddressMatcher> UNC_ADDRESS_MATCHERS = UNC_ADDRESSES.stream()
            .map(IpAddressMatcher::new)
            .toList();

    @RequestMapping(value = "/api/userInformation", method = RequestMethod.HEAD)
    public ResponseEntity<Object> getUserInformation(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String ipAddress = getClientIpAddress(request);
        session.setAttribute("userIPAddress", ipAddress);
        session.setAttribute("uncIPAddress", hasUncAddress(ipAddress));
        return ResponseEntity.status(OK).contentLength(0).build();
    }

    private Boolean hasUncAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        if (ipAddress.equals("127.0.0.1")) {
            return true;
        }

        // Any IPv6 address should not be treated as UNC.
        if (ipAddress.contains(":")) {
            return false;
        }

        return UNC_ADDRESS_MATCHERS.stream().anyMatch(matcher -> matcher.matches(ipAddress));
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
}
