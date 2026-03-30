package com.guerre.backend.controller;

import com.guerre.backend.config.AdminSecurity;
import com.guerre.backend.config.DatabaseConnection;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticleTagController {
    public void handle(HttpExchange exchange, Gson gson) throws IOException {
        try {
            if (!AdminSecurity.requireAdmin(exchange)) {
                return;
            }

            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if ("GET".equalsIgnoreCase(method)) {
                String resp = gson.toJson(all());
                writeJson(exchange, 200, resp);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, Object> body = readBody(exchange, gson);
                Long articleId = parseLong(body.get("articleId"));
                Long tagId = parseLong(body.get("tagId"));
                if (articleId == null || tagId == null) {
                    writeText(exchange, 400, "articleId and tagId are required");
                    return;
                }
                create(articleId, tagId);
                writeText(exchange, 201, "created");
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                Map<String, Object> body = readBody(exchange, gson);
                Long oldArticleId = parseLong(body.get("oldArticleId"));
                Long oldTagId = parseLong(body.get("oldTagId"));
                Long articleId = parseLong(body.get("articleId"));
                Long tagId = parseLong(body.get("tagId"));
                if (oldArticleId == null || oldTagId == null || articleId == null || tagId == null) {
                    writeText(exchange, 400, "oldArticleId, oldTagId, articleId, tagId are required");
                    return;
                }
                update(oldArticleId, oldTagId, articleId, tagId);
                writeText(exchange, 200, "updated");
                return;
            }

            if ("DELETE".equalsIgnoreCase(method)) {
                Map<String, String> q = queryParams(exchange);
                Long articleId = parseLong(q.get("articleId"));
                Long tagId = parseLong(q.get("tagId"));
                if (articleId == null || tagId == null) {
                    writeText(exchange, 400, "articleId and tagId are required");
                    return;
                }
                delete(articleId, tagId);
                writeText(exchange, 200, "deleted");
                return;
            }

            exchange.sendResponseHeaders(405, -1);
        } catch (Exception ex) {
            ex.printStackTrace();
            writeText(exchange, 500, "Error: " + ex.getMessage());
        }
    }

    private Map<String, Object> readBody(HttpExchange exchange, Gson gson) throws Exception {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(isr, mapType);
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }

    private List<Map<String, Object>> all() throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT at.article_id, a.titre, a.slug AS article_slug, at.tag_id, t.nom AS tag_nom, t.slug AS tag_slug " +
                     "FROM article_tags at " +
                     "JOIN articles a ON a.id = at.article_id " +
                     "JOIN tags t ON t.id = at.tag_id " +
                     "ORDER BY at.article_id, at.tag_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("articleId", rs.getLong("article_id"));
                m.put("articleTitre", rs.getString("titre"));
                m.put("articleSlug", rs.getString("article_slug"));
                m.put("tagId", rs.getLong("tag_id"));
                m.put("tagNom", rs.getString("tag_nom"));
                m.put("tagSlug", rs.getString("tag_slug"));
                out.add(m);
            }
        }
        return out;
    }

    private void create(long articleId, long tagId) throws Exception {
        String sql = "INSERT INTO article_tags(article_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, articleId);
            ps.setLong(2, tagId);
            ps.executeUpdate();
        }
    }

    private void update(long oldArticleId, long oldTagId, long articleId, long tagId) throws Exception {
        String del = "DELETE FROM article_tags WHERE article_id = ? AND tag_id = ?";
        String ins = "INSERT INTO article_tags(article_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psDel = conn.prepareStatement(del);
             PreparedStatement psIns = conn.prepareStatement(ins)) {
            psDel.setLong(1, oldArticleId);
            psDel.setLong(2, oldTagId);
            psDel.executeUpdate();

            psIns.setLong(1, articleId);
            psIns.setLong(2, tagId);
            psIns.executeUpdate();
        }
    }

    private void delete(long articleId, long tagId) throws Exception {
        String sql = "DELETE FROM article_tags WHERE article_id = ? AND tag_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, articleId);
            ps.setLong(2, tagId);
            ps.executeUpdate();
        }
    }

    private Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> out = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) return out;
        String[] parts = query.split("&");
        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) out.put(kv[0], kv[1]);
        }
        return out;
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, b.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
    }

    private void writeText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, b.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
    }
}
