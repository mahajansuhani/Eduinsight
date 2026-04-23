package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.User;
import util.DBConnection;

public class UserDAO {
    
    // Register new student
    public boolean registerUser(User user, String password) {
        String userSql = "INSERT INTO users (name, email, password_hash, role, stream, subject, gender, age, phone, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String academicSql = "INSERT INTO academic_data (student_id, attendance, study_hours, assignment_score) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, user.getName());
                userStmt.setString(2, user.getEmail());
                userStmt.setString(3, password);
                userStmt.setString(4, user.getRole());
                userStmt.setString(5, user.getStream());
                userStmt.setString(6, user.getSubject());
                userStmt.setString(7, user.getGender());
                userStmt.setInt(8, user.getAge());
                userStmt.setString(9, user.getPhone());
                userStmt.setBoolean(10, true);
                
                userStmt.executeUpdate();
                
                // Get the generated user ID
                ResultSet generatedKeys = userStmt.getGeneratedKeys();
                int userId = 0;
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                }
                
                // If student, insert academic data
                if ("student".equalsIgnoreCase(user.getRole()) && userId > 0) {
                    try (PreparedStatement academicStmt = conn.prepareStatement(academicSql)) {
                        academicStmt.setInt(1, userId);
                        academicStmt.setDouble(2, user.getAttendance());
                        academicStmt.setDouble(3, user.getStudyHours());
                        academicStmt.setDouble(4, user.getAssignmentScore());
                        academicStmt.executeUpdate();
                    }
                }
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Register error: " + e.getMessage());
            return false;
        }
    }
    
    // Login user
    public User login(String email, String password, String role) {
        String sql = "SELECT u.id, u.name, u.email, u.password_hash, u.role, u.stream, u.subject, u.gender, u.age, u.phone, u.is_active, " +
                     "COALESCE(a.attendance, 0) as attendance, COALESCE(a.study_hours, 0) as study_hours, COALESCE(a.assignment_score, 0) as assignment_score " +
                     "FROM users u " +
                     "LEFT JOIN academic_data a ON u.id = a.student_id " +
                     "WHERE (u.email = ? OR u.name = ?) AND u.password_hash = ? AND u.role = ? AND COALESCE(u.is_active, 1) = 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setStream(rs.getString("stream"));
                user.setSubject(rs.getString("subject"));
                user.setGender(rs.getString("gender"));
                user.setAge(rs.getInt("age"));
                user.setPhone(rs.getString("phone"));
                user.setAttendance(rs.getDouble("attendance"));
                user.setStudyHours(rs.getDouble("study_hours"));
                user.setAssignmentScore(rs.getDouble("assignment_score"));
                user.setActive(rs.getBoolean("is_active"));
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }
    
    // Get all students
    public ResultSet getAllStudents() {
        String sql = "SELECT u.id, u.name, u.email, u.stream, u.subject, u.gender, u.age, u.phone, " +
                     "COALESCE(a.attendance, 0) as attendance, COALESCE(a.study_hours, 0) as study_hours, COALESCE(a.assignment_score, 0) as assignment_score " +
                     "FROM users u " +
                     "LEFT JOIN academic_data a ON u.id = a.student_id " +
                     "WHERE u.role = 'student' AND COALESCE(u.is_active, 1) = 1 ORDER BY u.name";
        
        try {
            Connection conn = DBConnection.getConnection();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.err.println("Get students error: " + e.getMessage());
            return null;
        }
    }

    // Get all students as a list (API-friendly)
    public List<User> getAllStudentsList() {
        String sql = "SELECT u.id, u.name, u.email, u.role, u.stream, u.subject, u.gender, u.age, u.phone, u.is_active, " +
                     "COALESCE(a.attendance, 0) as attendance, COALESCE(a.study_hours, 0) as study_hours, COALESCE(a.assignment_score, 0) as assignment_score " +
                     "FROM users u " +
                     "LEFT JOIN academic_data a ON u.id = a.student_id " +
                     "WHERE u.role = 'student' AND COALESCE(u.is_active, 1) = 1 ORDER BY u.name";

        List<User> students = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setStream(rs.getString("stream"));
                user.setSubject(rs.getString("subject"));
                user.setGender(rs.getString("gender"));
                user.setAge(rs.getInt("age"));
                user.setPhone(rs.getString("phone"));
                user.setAttendance(rs.getDouble("attendance"));
                user.setStudyHours(rs.getDouble("study_hours"));
                user.setAssignmentScore(rs.getDouble("assignment_score"));
                user.setActive(rs.getBoolean("is_active"));
                students.add(user);
            }

        } catch (SQLException e) {
            System.err.println("Get students list error: " + e.getMessage());
        }

        return students;
    }
    
    // Delete student (soft delete)
    public boolean deleteStudent(int studentId) {
        String sql = "UPDATE users SET is_active = 0 WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Delete student error: " + e.getMessage());
            return false;
        }
    }
}

