package com.guerre.backend.controller;

import com.guerre.backend.config.AdminSecurity;
import com.guerre.backend.config.DatabaseConnection;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController {
    public void handle(HttpExchange exchange, Gson gson) throws IOException {
        try {
            if (!AdminSecurity.requireAdmin(exchange)) {
                return;
            }
            String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/me")) {
                String resp = gson.toJson(Map.of("isAdmin", true));
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            }

            if (path.endsWith("/meta")) {
                Map<String, Object> out = new HashMap<>();
                out.put("categories", loadCategories());
                out.put("tags", loadTags());
                String resp = gson.toJson(out);
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            }

            exchange.sendResponseHeaders(404, -1);
        } catch (Exception ex) {
            ex.printStackTrace();
            byte[] b = ("Error: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }
    }

    private List<Map<String, Object>> loadCategories() throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT id, nom, slug FROM categories ORDER BY nom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("nom", rs.getString("nom"));
                m.put("slug", rs.getString("slug"));
                out.add(m);
            }
        }
        return out;
    }

    private List<Map<String, Object>> loadTags() throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT id, nom, slug FROM tags ORDER BY nom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getLong("id"));
                m.put("nom", rs.getString("nom"));
                m.put("slug", rs.getString("slug"));
                out.add(m);
            }
        }
        return out;
    }
}
