package com.guerre.backend.config;

import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public class AdminSecurity {
    private AdminSecurity() {}

    private static String[] extractCredentials(HttpExchange exchange) {
        try {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String b64 = null;
            if (auth != null && auth.startsWith("Basic ")) {
                b64 = auth.substring("Basic ".length());
            }

            if (b64 == null || b64.isBlank()) {
                String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
                if (cookieHeader != null) {
                    String[] cookies = cookieHeader.split(";");
                    for (String c : cookies) {
                        String[] kv = c.trim().split("=", 2);
                        if (kv.length == 2 && "admin_auth".equals(kv[0])) {
                            b64 = kv[1];
                            break;
                        }
                    }
                }
            }

            if (b64 == null || b64.isBlank()) {
                return null;
            }

            String decoded = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
            int idx = decoded.indexOf(':');
            if (idx <= 0) {
                return null;
            }
            String email = decoded.substring(0, idx);
            String password = decoded.substring(idx + 1);
            return new String[] { email, password };
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAdmin(HttpExchange exchange) {
        try {
            String[] creds = extractCredentials(exchange);
            if (creds == null) {
                return false;
            }
            String email = creds[0];
            String password = creds[1];

            String sql = "SELECT id FROM users WHERE email = ? AND mot_de_passe = ? AND role = 'admin'";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean requireAdmin(HttpExchange exchange) {
        if (isAdmin(exchange)) {
            return true;
        }
        try {
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"Admin Zone\"");
            exchange.sendResponseHeaders(401, -1);
        } catch (Exception ignored) {
        }
        return false;
    }
}
