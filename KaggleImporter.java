package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KaggleImporter {

    private static final String DEFAULT_PASSWORD = "temp123";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/eduinsight";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "pun1613";
    private static final Logger LOGGER = Logger.getLogger(KaggleImporter.class.getName());

    public static void importKaggleData(String filePath) {
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();
            if (header == null || header.trim().isEmpty()) {
                System.out.println("CSV file is empty or missing a header.");
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",", -1);
                if (data.length < 20) {
                    continue;
                }

                try {
                    String name = "Student_" + (count + 1);
                    String email = "student" + (count + 1) + "@kaggle.com";

                    double hoursStudied = parseDouble(data[0]);
                    double attendance = parseDouble(data[1]);
                    parseDouble(data[5]);
                    parseDouble(data[6]);
                    parseDouble(data[9]);
                    parseDouble(data[14]);
                    double examScore = parseDouble(data[19]);

                    String stream = determineStream(examScore);
                    String passwordHash = hashPassword(DEFAULT_PASSWORD);

                    if (insertStudent(name, email, passwordHash, attendance, hoursStudied, examScore, stream)) {
                        count++;

                        if (count % 50 == 0) {
                            System.out.println("Imported " + count + " students...");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error on line: " + line, e);
                }
            }

            LOGGER.log(Level.INFO, "Successfully imported {0} students from Kaggle dataset.", count);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to import Kaggle data.", e);
        }
    }

    private static boolean insertStudent(String name,
                                         String email,
                                         String passwordHash,
                                         double attendance,
                                         double studyHours,
                                         double assignmentScore,
                                         String stream) {
        String userInsert = "INSERT INTO users (name, email, password_hash, role, stream, is_active) VALUES (?, ?, ?, 'student', ?, TRUE)";
        String academicInsert = "INSERT INTO academic_data (student_id, attendance, study_hours, assignment_score) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.out.println("Database connection failed.");
                return false;
            }

            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            int studentId;
            try (PreparedStatement userStmt = conn.prepareStatement(userInsert, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, name);
                userStmt.setString(2, email);
                userStmt.setString(3, passwordHash);
                userStmt.setString(4, stream);
                userStmt.executeUpdate();

                try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        conn.rollback();
                        return false;
                    }
                    studentId = generatedKeys.getInt(1);
                }
            }

            try (PreparedStatement academicStmt = conn.prepareStatement(academicInsert)) {
                academicStmt.setInt(1, studentId);
                academicStmt.setDouble(2, attendance);
                academicStmt.setDouble(3, studyHours);
                academicStmt.setDouble(4, assignmentScore);
                academicStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database operation failed while inserting student: " + email, e);
            return false;
        }
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to hash password.", e);
        }
    }

    private static String determineStream(double examScore) {
        if (examScore >= 70) {
            return "Science";
        }
        if (examScore >= 50) {
            return "Commerce";
        }
        return "Arts";
    }
}