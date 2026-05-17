package com.rplbo.app.rplboblessedbot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseHelper
 * --------------
 * Singleton helper untuk koneksi ke blessbot.db (SQLite).
 *
 * File DB dicari di:
 *   1. Folder yang sama dengan JAR / working directory  →  ./blessbot.db
 *   2. Fallback: folder project root (saat development di IDE)
 *
 * Cara pakai:
 *   Connection conn = DatabaseHelper.getConnection();
 */
public class DatabaseHelper {

    private static final String DB_FILENAME = "blessbot.db";
    private static Connection connection = null;

    private DatabaseHelper() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = resolveDbUrl();

            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC Driver tidak ditemukan", e);
            }

            connection = DriverManager.getConnection(url);

            // Aktifkan foreign key enforcement
            connection.createStatement().execute("PRAGMA foreign_keys = ON");

            System.out.println("✅ DB terhubung: " + url);
        }

        return connection;
    }

    private static String resolveDbUrl() {
        // Cari di working directory
        Path workDir = Paths.get(System.getProperty("user.dir"), DB_FILENAME);
        if (workDir.toFile().exists()) {
            return "jdbc:sqlite:" + workDir.toAbsolutePath();
        }

        // Fallback: cari di folder project root
        Path projectRoot = Paths.get(System.getProperty("user.dir")).getParent();
        if (projectRoot != null) {
            Path candidate = projectRoot.resolve(DB_FILENAME);
            if (candidate.toFile().exists()) {
                return "jdbc:sqlite:" + candidate.toAbsolutePath();
            }
        }

        // Default: buat di working directory kalau belum ada
        return "jdbc:sqlite:" + workDir.toAbsolutePath();
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Koneksi DB ditutup.");
            }
        } catch (SQLException e) {
            System.err.println("Gagal menutup koneksi: " + e.getMessage());
        }
    }
}
