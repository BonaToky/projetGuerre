package com.guerre.backend;

import com.guerre.backend.config.DatabaseConnection;
import com.guerre.backend.config.AdminSecurity;
import com.guerre.backend.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;

public class Main {
	public static void main(String[] args) throws Exception {
		ensureUploadsDirectoryReady();
		seedDefaultUserIfMissing();

		// Serialize LocalDateTime safely on Java 17 to avoid reflective access failures.
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class,
						(JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
				.create();

		// create controllers and wire them to handlers
		com.guerre.backend.controller.AuthController authController = new com.guerre.backend.controller.AuthController();
		com.guerre.backend.controller.UserController userController = new com.guerre.backend.controller.UserController();
		com.guerre.backend.controller.ArticleController articleController = new com.guerre.backend.controller.ArticleController();
		com.guerre.backend.controller.ArticleTagController articleTagController = new com.guerre.backend.controller.ArticleTagController();
		com.guerre.backend.controller.AdminController adminController = new com.guerre.backend.controller.AdminController();

		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		server.createContext("/api/auth/login", exchange -> authController.handle(exchange, gson));
		server.createContext("/api/users", exchange -> userController.handle(exchange, gson));
		server.createContext("/api/articles", exchange -> articleController.handle(exchange, gson));
		server.createContext("/api/article-tags", exchange -> articleTagController.handle(exchange, gson));
		server.createContext("/api/admin", exchange -> adminController.handle(exchange, gson));

		// static file handler: serve files from backend/static
		server.createContext("/", exchange -> {
			String reqPath = exchange.getRequestURI().getPath();
			if (reqPath.equals("/")) reqPath = "/index.html";
			// normalized routes
			if (reqPath.equals("/login")) reqPath = "/index.html";
			if (reqPath.equals("/articles")) reqPath = "/articles.html";
			if (reqPath.equals("/admin")) reqPath = "/admin.html";
			if (reqPath.equals("/curdarticle")) reqPath = "/curdarticle.html";
			if (reqPath.equals("/crudarticle_tags")) reqPath = "/crudarticle_tags.html";
			if (reqPath.startsWith("/articles/") && reqPath.length() > "/articles/".length()) {
				reqPath = "/article.html";
			}

			boolean isAdminPage = reqPath.equals("/admin.html") || reqPath.equals("/curdarticle.html") || reqPath.equals("/crudarticle_tags.html");
			if (isAdminPage && !AdminSecurity.requireAdmin(exchange)) {
				return;
			}

			File f = new File("static" + reqPath);
			if (!f.exists() || f.isDirectory()) {
				exchange.sendResponseHeaders(404, -1);
				return;
			}
			Path p = f.toPath();
			String contentType = Files.probeContentType(p);
			if (contentType == null) contentType = "application/octet-stream";
			byte[] bytes = Files.readAllBytes(p);
			exchange.getResponseHeaders().add("Content-Type", contentType);
			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
		});

		server.setExecutor(null);
		server.start();
		System.out.println("Server started on http://localhost:8080");
	}

	private static void ensureUploadsDirectoryReady() throws IOException {
		Path uploadsDir = Paths.get("static", "uploads");
		if (Files.exists(uploadsDir) && !Files.isDirectory(uploadsDir)) {
			throw new IOException("Upload path exists but is not a directory: " + uploadsDir);
		}
		Files.createDirectories(uploadsDir);
	}

	private static void seedDefaultUserIfMissing() {
		String checkSql = "SELECT id FROM users WHERE email = ?";
		String insertSql = "INSERT INTO users(nom, email, mot_de_passe, role) VALUES (?, ?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
			 java.sql.PreparedStatement check = conn.prepareStatement(checkSql)) {
			check.setString(1, "test@example.com");
			try (java.sql.ResultSet rs = check.executeQuery()) {
				if (rs.next()) {
					return;
				}
			}
			try (java.sql.PreparedStatement insert = conn.prepareStatement(insertSql)) {
				insert.setString(1, "Test User");
				insert.setString(2, "test@example.com");
				insert.setString(3, "password");
				insert.setString(4, "lecteur");
				insert.executeUpdate();
			}
		} catch (Exception ignored) {
			// Keep server start even if DB is not ready yet.
		}
	}
}
