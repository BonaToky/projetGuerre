package com.guerre.backend.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
	private DatabaseConnection() {}

	public static Connection getConnection() throws SQLException {
		String url = envOrDefault("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/journale");
		String user = envOrDefault("SPRING_DATASOURCE_USERNAME", "postgres");
		String pass = envOrDefault("SPRING_DATASOURCE_PASSWORD", "postgres");
		return DriverManager.getConnection(url, user, pass);
	}

	private static String envOrDefault(String key, String fallback) {
		String val = System.getenv(key);
		if (val == null || val.isBlank()) {
			return fallback;
		}
		return val;
	}
}
