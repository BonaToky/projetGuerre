package com.guerre.backend.controller;

import com.guerre.backend.config.AdminSecurity;
import com.guerre.backend.config.DatabaseConnection;
import com.guerre.backend.models.Article;
import com.guerre.backend.models.Category;
import com.guerre.backend.models.Tag;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArticleController {
    private static final Path UPLOADS_DIR = Paths.get(System.getProperty("user.dir"), "static", "uploads");

    public ArticleController() {}

    private void logInfo(String message) {
        System.out.println("[ArticleController] " + message);
    }

    private Path ensureUploadsDirectoryReady() throws IOException {
        if (Files.exists(UPLOADS_DIR)) {
            if (!Files.isDirectory(UPLOADS_DIR)) {
                throw new IOException("Upload path exists but is not a directory: " + UPLOADS_DIR);
            }
            return UPLOADS_DIR;
        }
        Path created = Files.createDirectories(UPLOADS_DIR);
        logInfo("Created uploads directory: " + created.toAbsolutePath());
        return created;
    }

    public List<Article> all() throws Exception {
        String sql = "SELECT a.id, a.titre, a.chapeau, a.slug, a.contenu, a.image_url, a.author_id, a.category_id, a.created_at, a.updated_at, " +
                     "c.nom AS category_nom, c.slug AS category_slug " +
                     "FROM articles a LEFT JOIN categories c ON c.id = a.category_id ORDER BY a.id";
        List<Article> articles = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Article a = mapArticle(rs);
                a.setTags(loadTagsForArticle(conn, a.getId()));
                articles.add(a);
            }
        }
        return articles;
    }

    public Optional<Article> byId(long id) throws Exception {
        String sql = "SELECT a.id, a.titre, a.chapeau, a.slug, a.contenu, a.image_url, a.author_id, a.category_id, a.created_at, a.updated_at, " +
                     "c.nom AS category_nom, c.slug AS category_slug " +
                     "FROM articles a LEFT JOIN categories c ON c.id = a.category_id WHERE a.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Article a = mapArticle(rs);
                    a.setTags(loadTagsForArticle(conn, a.getId()));
                    return Optional.of(a);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Article> bySlug(String slug) throws Exception {
        String sql = "SELECT a.id, a.titre, a.chapeau, a.slug, a.contenu, a.image_url, a.author_id, a.category_id, a.created_at, a.updated_at, " +
                     "c.nom AS category_nom, c.slug AS category_slug " +
                     "FROM articles a LEFT JOIN categories c ON c.id = a.category_id WHERE a.slug = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Article a = mapArticle(rs);
                    a.setTags(loadTagsForArticle(conn, a.getId()));
                    return Optional.of(a);
                }
            }
        }
        return Optional.empty();
    }

    private Article mapArticle(ResultSet rs) throws Exception {
        Article a = new Article();
        a.setId(rs.getLong("id"));
        a.setTitre(rs.getString("titre"));
        a.setChapeau(rs.getString("chapeau"));
        a.setSlug(rs.getString("slug"));
        a.setContenu(rs.getString("contenu"));
        a.setImageUrl(rs.getString("image_url"));
        a.setAuthorId(rs.getObject("author_id") == null ? null : rs.getLong("author_id"));
        a.setCategoryId(rs.getObject("category_id") == null ? null : rs.getLong("category_id"));
        if (a.getCategoryId() != null) {
            Category c = new Category();
            c.setId(a.getCategoryId());
            c.setNom(rs.getString("category_nom"));
            c.setSlug(rs.getString("category_slug"));
            a.setCategory(c);
        }
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) a.setUpdatedAt(updated.toLocalDateTime());
        return a;
    }

    private HashSet<Tag> loadTagsForArticle(Connection conn, Long articleId) throws Exception {
        HashSet<Tag> tags = new HashSet<>();
        if (articleId == null) {
            return tags;
        }
        String sql = "SELECT t.id, t.nom, t.slug FROM article_tags at JOIN tags t ON t.id = at.tag_id WHERE at.article_id = ? ORDER BY t.nom";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tag t = new Tag();
                    t.setId(rs.getLong("id"));
                    t.setNom(rs.getString("nom"));
                    t.setSlug(rs.getString("slug"));
                    tags.add(t);
                }
            }
        }
        return tags;
    }

    private Long getIdQueryParam(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] parts = query.split("&");
        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && "id".equals(kv[0])) {
                try {
                    return Long.parseLong(kv[1]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String getSlugFromPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String prefix = "/api/articles/";
        if (path == null || !path.startsWith(prefix) || path.length() <= prefix.length()) {
            return null;
        }
        String slug = path.substring(prefix.length());
        int slashIdx = slug.indexOf('/');
        if (slashIdx >= 0) {
            slug = slug.substring(0, slashIdx);
        }
        if (slug.isBlank()) {
            return null;
        }
        return slug;
    }

    private Long parseNullableLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safeSlug(String input) {
        if (input == null) return null;
        String slug = input.trim().toLowerCase();
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        slug = slug.replaceAll("\\s+", "-");
        slug = slug.replaceAll("-+", "-");
        return slug;
    }

    private void replaceTagsBySlugs(Connection conn, long articleId, List<String> slugs) throws Exception {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM article_tags WHERE article_id = ?")) {
            del.setLong(1, articleId);
            del.executeUpdate();
        }
        if (slugs == null || slugs.isEmpty()) {
            return;
        }
        String selectTag = "SELECT id FROM tags WHERE slug = ?";
        String insertLink = "INSERT INTO article_tags(article_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement sel = conn.prepareStatement(selectTag);
             PreparedStatement ins = conn.prepareStatement(insertLink)) {
            for (String raw : slugs) {
                if (raw == null || raw.isBlank()) continue;
                String slug = raw.trim().toLowerCase();
                sel.setString(1, slug);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        long tagId = rs.getLong("id");
                        ins.setLong(1, articleId);
                        ins.setLong(2, tagId);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    private Article createArticle(Map<String, Object> body) throws Exception {
        String titre = body.get("titre") == null ? null : body.get("titre").toString();
        String chapeau = body.get("chapeau") == null ? null : body.get("chapeau").toString();
        String slug = body.get("slug") == null ? null : body.get("slug").toString();
        String contenu = body.get("contenu") == null ? null : body.get("contenu").toString();
        String imageUrl = body.get("imageUrl") == null ? null : body.get("imageUrl").toString();
        Long categoryId = parseNullableLong(body.get("categoryId"));
        Long authorId = parseNullableLong(body.get("authorId"));
        if (slug == null || slug.isBlank()) {
            slug = safeSlug(titre);
        } else {
            slug = safeSlug(slug);
        }

        @SuppressWarnings("unchecked")
        List<String> tagSlugs = (List<String>) body.get("tagSlugs");

        String sql = "INSERT INTO articles(titre, chapeau, slug, contenu, image_url, category_id, author_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) RETURNING id";
        long id;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setString(2, chapeau);
            ps.setString(3, slug);
            ps.setString(4, contenu);
            ps.setString(5, imageUrl);
            if (categoryId == null) ps.setObject(6, null); else ps.setLong(6, categoryId);
            if (authorId == null) ps.setObject(7, null); else ps.setLong(7, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong("id");
            }
            replaceTagsBySlugs(conn, id, tagSlugs);
        }
        logInfo("Article created id=" + id + " slug=" + slug + " imageUrl=" + imageUrl);
        return byId(id).orElseThrow();
    }

    private Optional<Article> updateArticle(long id, Map<String, Object> body) throws Exception {
        Optional<Article> currentOpt = byId(id);
        if (currentOpt.isEmpty()) {
            return Optional.empty();
        }
        Article current = currentOpt.get();
        String titre = body.get("titre") == null ? current.getTitre() : body.get("titre").toString();
        String chapeau = body.get("chapeau") == null ? current.getChapeau() : body.get("chapeau").toString();
        String slug = body.get("slug") == null ? current.getSlug() : safeSlug(body.get("slug").toString());
        String contenu = body.get("contenu") == null ? current.getContenu() : body.get("contenu").toString();
        String imageUrl = body.get("imageUrl") == null ? current.getImageUrl() : body.get("imageUrl").toString();
        Long categoryId = current.getCategoryId();
        if (body.containsKey("categoryId") && body.get("categoryId") != null) {
            Long parsed = parseNullableLong(body.get("categoryId"));
            if (parsed != null) {
                categoryId = parsed;
            }
        }
        Long authorId = body.containsKey("authorId") ? parseNullableLong(body.get("authorId")) : current.getAuthorId();

        @SuppressWarnings("unchecked")
        List<String> tagSlugs = body.containsKey("tagSlugs") ? (List<String>) body.get("tagSlugs") : null;

        String sql = "UPDATE articles SET titre = ?, chapeau = ?, slug = ?, contenu = ?, image_url = ?, category_id = ?, author_id = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setString(2, chapeau);
            ps.setString(3, slug);
            ps.setString(4, contenu);
            ps.setString(5, imageUrl);
            if (categoryId == null) ps.setObject(6, null); else ps.setLong(6, categoryId);
            if (authorId == null) ps.setObject(7, null); else ps.setLong(7, authorId);
            ps.setLong(8, id);
            ps.executeUpdate();
            if (tagSlugs != null) {
                replaceTagsBySlugs(conn, id, tagSlugs);
            }
        }
        logInfo("Article updated id=" + id + " slug=" + slug + " imageUrl=" + imageUrl);
        return byId(id);
    }

    private String uploadImage(Map<String, Object> body) throws Exception {
        String filename = body.get("filename") == null ? "image.jpg" : body.get("filename").toString();
        String dataBase64 = body.get("dataBase64") == null ? null : body.get("dataBase64").toString();
        if (dataBase64 == null || dataBase64.isBlank()) {
            throw new IllegalArgumentException("dataBase64 is required");
        }
        String clean = dataBase64;
        int comma = clean.indexOf(',');
        if (comma >= 0) {
            clean = clean.substring(comma + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(clean);

        String ext = ".jpg";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            ext = filename.substring(dot).toLowerCase();
            if (ext.length() > 8) ext = ".jpg";
        }
        String safeName = "img_" + System.currentTimeMillis() + ext;
        Path uploadsDir = ensureUploadsDirectoryReady();
        Path target = uploadsDir.resolve(safeName);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logInfo("Image uploaded file=" + target.toAbsolutePath() + " bytes=" + bytes.length);
        return "/uploads/" + safeName;
    }

    public void handle(HttpExchange exchange, Gson gson) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            // CORS
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("GET".equalsIgnoreCase(method)) {
                Long id = getIdQueryParam(exchange);
                String slug = getSlugFromPath(exchange);
                String resp;
                int status;
                if (slug != null) {
                    Optional<Article> articleOpt = bySlug(slug);
                    if (articleOpt.isPresent()) {
                        resp = gson.toJson(articleOpt.get());
                        status = 200;
                    } else {
                        resp = "Article not found";
                        status = 404;
                    }
                } else if (id != null) {
                    Optional<Article> articleOpt = byId(id);
                    if (articleOpt.isPresent()) {
                        resp = gson.toJson(articleOpt.get());
                        status = 200;
                    } else {
                        resp = "Article not found";
                        status = 404;
                    }
                } else {
                    resp = gson.toJson(all());
                    status = 200;
                }
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                if (!AdminSecurity.requireAdmin(exchange)) {
                    logInfo("Blocked POST " + path + " (admin required)");
                    return;
                }
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> body = gson.fromJson(isr, mapType);

                if (path != null && path.endsWith("/upload")) {
                    String url = uploadImage(body);
                    String resp = gson.toJson(Map.of("url", url));
                    byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(201, b.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                    return;
                }

                Article created = createArticle(body);
                String resp = gson.toJson(created);
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                if (!AdminSecurity.requireAdmin(exchange)) {
                    logInfo("Blocked PUT " + path + " (admin required)");
                    return;
                }
                Long id = getIdQueryParam(exchange);
                String slug = getSlugFromPath(exchange);
                if (id == null && slug != null) {
                    Optional<Article> bySlug = bySlug(slug);
                    if (bySlug.isPresent()) {
                        id = bySlug.get().getId();
                    }
                }
                if (id == null) {
                    byte[] b = "Missing id or slug for update".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, b.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                    return;
                }

                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> body = gson.fromJson(isr, mapType);
                Optional<Article> updated = updateArticle(id, body);
                if (updated.isEmpty()) {
                    byte[] b = "Article not found".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, b.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                    return;
                }
                String resp = gson.toJson(updated.get());
                byte[] b = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, b.length);
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
