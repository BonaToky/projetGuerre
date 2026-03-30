package com.guerre.backend.controller;

import com.guerre.backend.config.DatabaseConnection;
import com.guerre.backend.models.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class AuthController {
    public AuthController() {}

    public Optional<User> login(String email, String motDePasse) {
        if (email == null || motDePasse == null) {
            return Optional.empty();
        }
        String sql = "SELECT id, nom, email, mot_de_passe, role, created_at FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String dbPassword = rs.getString("mot_de_passe");
                if (dbPassword == null || !dbPassword.equals(motDePasse)) {
                    return Optional.empty();
                }
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setNom(rs.getString("nom"));
                user.setEmail(rs.getString("email"));
                user.setMotDePasse(dbPassword);
                user.setRole(rs.getString("role"));
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    user.setCreatedAt(created.toLocalDateTime());
                }
                return Optional.of(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void handle(HttpExchange exchange, Gson gson) throws IOException {
        String method = exchange.getRequestMethod();
        // handle CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"POST".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> body = gson.fromJson(isr, mapType);
            System.out.println("[AuthController] request from=" + exchange.getRemoteAddress() + " body=" + gson.toJson(body));
            String email = body == null || body.get("email") == null ? null : body.get("email").toString();
            String motDePasse = body == null || body.get("motDePasse") == null ? null : body.get("motDePasse").toString();
            Optional<User> userOpt = login(email, motDePasse);
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                System.out.println("[AuthController] login success userId=" + u.getId() + " email=" + u.getEmail());
                String resp = gson.toJson(u);
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                String token = Base64.getEncoder().encodeToString((email + ":" + motDePasse).getBytes(StandardCharsets.UTF_8));
                exchange.getResponseHeaders().add("Set-Cookie", "admin_auth=" + token + "; Path=/; HttpOnly; SameSite=Lax");
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
            } else {
                System.out.println("[AuthController] login failed for email=" + email);
                byte[] b = "Invalid credentials".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(401, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            byte[] b = ("Error: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }
    }
}
