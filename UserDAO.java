package dao;

import java.util.HashMap;
import java.util.Map;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.User;
import util.DBConnection;

public class UserDAO {
    
    // Register new student
    public boolean registerUser(User user, String password) {
        String findUserSql = "SELECT id FROM users WHERE email = ? LIMIT 1";
        String insertUserSql = "INSERT INTO users (name, email, password_hash, role, stream, subject, gender, age, phone, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateUserSql = "UPDATE users SET name = ?, password_hash = ?, role = ?, stream = ?, subject = ?, gender = ?, age = ?, phone = ?, is_active = 1 WHERE id = ?";
        String academicSql = "INSERT INTO academic_data (student_id, attendance, study_hours, assignment_score) VALUES (?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE attendance = VALUES(attendance), study_hours = VALUES(study_hours), assignment_score = VALUES(assignment_score)";
        
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            int userId = 0;

            try (PreparedStatement findStmt = conn.prepareStatement(findUserSql)) {
                findStmt.setString(1, user.getEmail());
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("id");
                    }
                }
            }

            if (userId > 0) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateUserSql)) {
                    updateStmt.setString(1, user.getName());
                    updateStmt.setString(2, password);
                    updateStmt.setString(3, user.getRole());
                    updateStmt.setString(4, user.getStream());
                    updateStmt.setString(5, user.getSubject());
                    updateStmt.setString(6, user.getGender());
                    updateStmt.setInt(7, user.getAge());
                    updateStmt.setString(8, user.getPhone());
                    updateStmt.setInt(9, userId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
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
                    
                    ResultSet generatedKeys = userStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    }
                }
            }

            if ("student".equalsIgnoreCase(user.getRole()) && userId > 0) {
                try (PreparedStatement academicStmt = conn.prepareStatement(academicSql)) {
                    academicStmt.setInt(1, userId);
                    academicStmt.setDouble(2, user.getAttendance());
                    academicStmt.setDouble(3, user.getStudyHours());
                    academicStmt.setDouble(4, user.getAssignmentScore());
                    academicStmt.executeUpdate();
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

    // Update academic data for a student
    public boolean updateStudentAcademicData(int studentId, double attendance, double studyHours, double assignmentScore) {
        String sql = "INSERT INTO academic_data (student_id, attendance, study_hours, assignment_score) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE attendance = VALUES(attendance), study_hours = VALUES(study_hours), assignment_score = VALUES(assignment_score)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            pstmt.setDouble(2, attendance);
            pstmt.setDouble(3, studyHours);
            pstmt.setDouble(4, assignmentScore);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update academic data error: " + e.getMessage());
            return false;
        }
    }
        // ==================== ATTENDANCE METHODS ====================
    
    // Save attendance for a student
    public boolean saveAttendance(int studentId, String date, String status, String notes, int markedBy) {
        String sql = "INSERT INTO attendance (student_id, attendance_date, status, notes, marked_by) VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE status = ?, notes = ?, updated_at = CURRENT_TIMESTAMP";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            pstmt.setDate(2, java.sql.Date.valueOf(date));
            pstmt.setString(3, status);
            pstmt.setString(4, notes);
            pstmt.setInt(5, markedBy);
            pstmt.setString(6, status);
            pstmt.setString(7, notes);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Save attendance error: " + e.getMessage());
            return false;
        }
    }
    
    // Get attendance for a specific date
    public ResultSet getAttendanceByDate(String date) {
        String sql = "SELECT a.*, u.name as student_name, u.stream " +
                     "FROM attendance a " +
                     "JOIN users u ON a.student_id = u.id " +
                     "WHERE a.attendance_date = ? AND u.role = 'student'";
        
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            return pstmt.executeQuery();
        } catch (SQLException e) {
            System.err.println("Get attendance error: " + e.getMessage());
            return null;
        }
    }
    
    // Get attendance summary for a student
    public Map<String, Object> getAttendanceSummary(int studentId) {
        String sql = "SELECT COUNT(*) as total, " +
                     "SUM(CASE WHEN status = 'present' THEN 1 ELSE 0 END) as present " +
                     "FROM attendance WHERE student_id = ?";
        
        Map<String, Object> summary = new HashMap<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                summary.put("total", rs.getInt("total"));
                summary.put("present", rs.getInt("present"));
                summary.put("percentage", rs.getInt("total") > 0 ? 
                           (rs.getInt("present") * 100.0 / rs.getInt("total")) : 0);
            }
        } catch (SQLException e) {
            System.err.println("Get attendance summary error: " + e.getMessage());
        }
        return summary;
    }
    
    // ==================== EXAM RESULTS METHODS ====================
    
    // Save exam result
    public boolean saveExamResult(int studentId, String examName, String subject, double marksObtained, double totalMarks, String examDate) {
        String sql = "INSERT INTO exam_results (student_id, exam_name, subject, marks_obtained, total_marks, percentage, exam_date, grade) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE marks_obtained = ?, total_marks = ?, percentage = ?, grade = ?";
        
        double percentage = totalMarks > 0 ? (marksObtained / totalMarks) * 100 : 0;
        String grade = calculateGrade(percentage);
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            pstmt.setString(2, examName);
            pstmt.setString(3, subject);
            pstmt.setDouble(4, marksObtained);
            pstmt.setDouble(5, totalMarks);
            pstmt.setDouble(6, percentage);
            pstmt.setDate(7, java.sql.Date.valueOf(examDate));
            pstmt.setString(8, grade);
            pstmt.setDouble(9, marksObtained);
            pstmt.setDouble(10, totalMarks);
            pstmt.setDouble(11, percentage);
            pstmt.setString(12, grade);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Save exam result error: " + e.getMessage());
            return false;
        }
    }
    
    // Get exam results for a student
    public List<Map<String, Object>> getExamResults(int studentId) {
        String sql = "SELECT * FROM exam_results WHERE student_id = ? ORDER BY exam_date DESC";
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", rs.getInt("id"));
                result.put("exam_name", rs.getString("exam_name"));
                result.put("subject", rs.getString("subject"));
                result.put("marks_obtained", rs.getDouble("marks_obtained"));
                result.put("total_marks", rs.getDouble("total_marks"));
                result.put("percentage", rs.getDouble("percentage"));
                result.put("grade", rs.getString("grade"));
                result.put("exam_date", rs.getDate("exam_date").toString());
                results.add(result);
            }
        } catch (SQLException e) {
            System.err.println("Get exam results error: " + e.getMessage());
        }
        return results;
    }
    
    // Get all exam results for teacher view
    public List<Map<String, Object>> getAllExamResults(String examName, String subject) {
        StringBuilder sql = new StringBuilder(
            "SELECT e.*, u.name as student_name, u.stream " +
            "FROM exam_results e " +
            "JOIN users u ON e.student_id = u.id " +
            "WHERE u.role = 'student'"
        );
        
        List<Object> params = new ArrayList<>();
        
        if (examName != null && !examName.isEmpty()) {
            sql.append(" AND e.exam_name = ?");
            params.add(examName);
        }
        if (subject != null && !subject.isEmpty()) {
            sql.append(" AND e.subject = ?");
            params.add(subject);
        }
        
        sql.append(" ORDER BY e.exam_date DESC");
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", rs.getInt("id"));
                result.put("student_id", rs.getInt("student_id"));
                result.put("student_name", rs.getString("student_name"));
                result.put("stream", rs.getString("stream"));
                result.put("exam_name", rs.getString("exam_name"));
                result.put("subject", rs.getString("subject"));
                result.put("marks_obtained", rs.getDouble("marks_obtained"));
                result.put("total_marks", rs.getDouble("total_marks"));
                result.put("percentage", rs.getDouble("percentage"));
                result.put("grade", rs.getString("grade"));
                result.put("exam_date", rs.getDate("exam_date").toString());
                results.add(result);
            }
        } catch (SQLException e) {
            System.err.println("Get all exam results error: " + e.getMessage());
        }
        return results;
    }
    
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }
}