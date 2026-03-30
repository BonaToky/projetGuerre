package com.guerre.backend.controller;

import com.guerre.backend.config.DatabaseConnection;
import com.guerre.backend.models.User;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserController {
    public UserController() {}

    public List<User> all() throws Exception {
        String sql = "SELECT id, nom, email, mot_de_passe, role, created_at FROM users ORDER BY id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setNom(rs.getString("nom"));
                u.setEmail(rs.getString("email"));
                u.setMotDePasse(rs.getString("mot_de_passe"));
                u.setRole(rs.getString("role"));
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    u.setCreatedAt(created.toLocalDateTime());
                }
                users.add(u);
            }
        }
        return users;
    }

    public User create(User u) throws Exception {
        if (u.getRole() == null || u.getRole().isBlank()) {
            u.setRole("lecteur");
        }
        String sql = "INSERT INTO users(nom, email, mot_de_passe, role) VALUES (?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getMotDePasse());
            ps.setString(4, u.getRole());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    u.setId(rs.getLong("id"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        u.setCreatedAt(created.toLocalDateTime());
                    }
                }
            }
        }
        return u;
    }

    public void handle(HttpExchange exchange, Gson gson) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            System.out.println("[UserController] request from=" + exchange.getRemoteAddress() + " method=" + method + " path=" + exchange.getRequestURI());
            // handle CORS preflight
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("GET".equalsIgnoreCase(method)) {
                String resp = gson.toJson(all());
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            } else if ("POST".equalsIgnoreCase(method)) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                User newUser = gson.fromJson(isr, User.class);
                System.out.println("[UserController] create user payload=" + gson.toJson(newUser));
                User saved = create(newUser);
                String resp = gson.toJson(saved);
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            }
            exchange.sendResponseHeaders(405, -1);
        } catch (Exception ex) {
            ex.printStackTrace();
            byte[] b = ("Error: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }
    }
}
