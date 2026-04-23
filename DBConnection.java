package util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    // Database credentials - use environment variables for security
    private static final String URL = System.getenv("DB_URL") != null 
        ? System.getenv("DB_URL")
        : "jdbc:mysql://localhost:3306/eduinsight?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
    
    private static final String USER = System.getenv("DB_USER") != null 
        ? System.getenv("DB_USER")
        : "root";
    
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null 
        ? System.getenv("DB_PASSWORD")
        : "pun1613"; // CHANGE THIS - use environment variable instead
    
    private static boolean schemaEnsured = false;
    
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            if (!schemaEnsured) {
                synchronized (DBConnection.class) {
                    if (!schemaEnsured) {
                        ensureSchema(conn);
                        schemaEnsured = true;
                    }
                }
            }
            return conn;
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Database connection failed!");
            System.err.println(e.getMessage());
            return null;
        }
    }

    private static void ensureSchema(Connection conn) throws SQLException {
        // These columns should already exist in MySQL schema
        // Just ensure they're present for migration safety
        ensureColumn(conn, "users", "subject", "ALTER TABLE users ADD COLUMN subject VARCHAR(100) NULL");
        ensureColumn(conn, "users", "gender", "ALTER TABLE users ADD COLUMN gender VARCHAR(20) NULL");
        ensureColumn(conn, "users", "age", "ALTER TABLE users ADD COLUMN age INT NULL");
        ensureColumn(conn, "users", "phone", "ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL");
        ensureColumn(conn, "users", "is_active", "ALTER TABLE users ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1");
    }

    private static void ensureColumn(Connection conn, String tableName, String columnName, String alterSql) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            if (!columns.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(alterSql);
                    System.out.println("Added column: " + columnName);
                }
            }
        }
    }
    
    public static void closeConnection() {
        // No-op: connections are managed by try-with-resources in DAO classes.
    }
    
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
