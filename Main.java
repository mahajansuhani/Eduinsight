import dao.UserDAO;
import model.User;
import util.DBConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("EduInsight backend startup...");

        if (!DBConnection.testConnection()) {
            System.err.println("Database connection failed. Check DB credentials and MySQL service.");
            return;
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            server.createContext("/api/health", new HealthHandler());
            server.createContext("/api/login", new LoginHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/students", new StudentsHandler());
            server.createContext("/api/attendance", new AttendanceHandler());
            server.createContext("/api/exam-results", new ExamResultsHandler());

            server.setExecutor(null);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(DBConnection::closeConnection));

            System.out.println("Database connected successfully.");
            System.out.println("API running at http://localhost:" + PORT);
            System.out.println("Endpoints: /api/health, /api/login, /api/register, /api/students, /api/attendance, /api/exam-results");
        } catch (IOException e) {
            System.err.println("Failed to start API server: " + e.getMessage());
        }
    }

    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) {
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            sendJson(exchange, 200, "{\"success\":true,\"message\":\"API is running\"}");
        }
    }

    private static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) {
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> form = parseFormBody(body);

            String email = valueOrEmpty(form.get("email"));
            String password = valueOrEmpty(form.get("password"));
            String role = valueOrDefault(form.get("role"), "student");

            if (email.isEmpty() || password.isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Email and password are required\"}");
                return;
            }

            UserDAO dao = new UserDAO();
            User user = dao.login(email, password, role);

            if (user == null) {
                sendJson(exchange, 401, "{\"success\":false,\"message\":\"Invalid credentials\"}");
                return;
            }

            String json = "{\"success\":true," +
                    "\"message\":\"Login successful\"," +
                    "\"user\":{" +
                    "\"id\":" + user.getId() + "," +
                    "\"name\":\"" + escapeJson(user.getName()) + "\"," +
                    "\"email\":\"" + escapeJson(user.getEmail()) + "\"," +
                    "\"role\":\"" + escapeJson(user.getRole()) + "\"}" +
                    "}";

            sendJson(exchange, 200, json);
        }
    }

    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) {
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> form = parseFormBody(body);

            User user = new User();
            user.setName(valueOrEmpty(form.get("name")));
            user.setEmail(valueOrEmpty(form.get("email")));
            user.setRole(valueOrDefault(form.get("role"), "student"));
            user.setStream(valueOrEmpty(form.get("stream")));
            user.setSubject(valueOrEmpty(form.get("subject")));
            user.setGender(valueOrEmpty(form.get("gender")));
            user.setAge(parseInt(valueOrDefault(form.get("age"), "0")));
            user.setPhone(valueOrEmpty(form.get("phone")));
            user.setAttendance(parseDouble(valueOrDefault(form.get("attendance"), "0")));
            user.setStudyHours(parseDouble(valueOrDefault(form.get("studyHours"), "0")));
            user.setAssignmentScore(parseDouble(valueOrDefault(form.get("assignmentScore"), "0")));

            String password = valueOrEmpty(form.get("password"));

            if (user.getName().isEmpty() || user.getEmail().isEmpty() || password.isEmpty()) {
                sendJson(exchange, 400, "{\"success\":false,\"message\":\"Name, email and password are required\"}");
                return;
            }

            UserDAO dao = new UserDAO();
            boolean created = dao.registerUser(user, password);

            if (!created) {
                sendJson(exchange, 500, "{\"success\":false,\"message\":\"Registration failed\"}");
                return;
            }

            sendJson(exchange, 201, "{\"success\":true,\"message\":\"Registration successful\"}");
        }
    }

    private static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) {
                return;
            }

            String method = exchange.getRequestMethod();
            UserDAO dao = new UserDAO();

            if ("GET".equalsIgnoreCase(method)) {
                List<User> students = dao.getAllStudentsList();
                sendJson(exchange, 200, studentsToJson(students));
                return;
            }

            if ("DELETE".equalsIgnoreCase(method)) {
                String query = valueOrEmpty(exchange.getRequestURI().getQuery());
                Map<String, String> params = parseFormBody(query);
                int id = parseInt(valueOrDefault(params.get("id"), "0"));

                if (id <= 0) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Valid student id is required\"}");
                    return;
                }

                boolean deleted = dao.deleteStudent(id);
                if (!deleted) {
                    sendJson(exchange, 404, "{\"success\":false,\"message\":\"Student not found\"}");
                    return;
                }

                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Student deleted\"}");
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> form = parseFormBody(body);

                int id = parseInt(valueOrDefault(form.get("id"), "0"));
                double attendance = parseDouble(valueOrDefault(form.get("attendance"), "0"));
                double studyHours = parseDouble(valueOrDefault(form.get("studyHours"), "0"));
                double assignmentScore = parseDouble(valueOrDefault(form.get("assignmentScore"), "0"));

                if (id <= 0) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Valid student id is required\"}");
                    return;
                }

                boolean updated = dao.updateStudentAcademicData(id, attendance, studyHours, assignmentScore);
                if (!updated) {
                    sendJson(exchange, 500, "{\"success\":false,\"message\":\"Failed to update student data\"}");
                    return;
                }

                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Student data updated\"}");
                return;
            }

            sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
        }
    }

    private static String studentsToJson(List<User> students) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true,\"students\":[");

        for (int i = 0; i < students.size(); i++) {
            User s = students.get(i);
            sb.append("{")
                    .append("\"id\":").append(s.getId()).append(",")
                    .append("\"name\":\"").append(escapeJson(s.getName())).append("\",")
                    .append("\"email\":\"").append(escapeJson(s.getEmail())).append("\",")
                    .append("\"role\":\"").append(escapeJson(s.getRole())).append("\",")
                    .append("\"stream\":\"").append(escapeJson(s.getStream())).append("\",")
                    .append("\"subject\":\"").append(escapeJson(s.getSubject())).append("\",")
                    .append("\"gender\":\"").append(escapeJson(s.getGender())).append("\",")
                    .append("\"age\":").append(s.getAge()).append(",")
                    .append("\"phone\":\"").append(escapeJson(s.getPhone())).append("\",")
                    .append("\"attendance\":").append(s.getAttendance()).append(",")
                    .append("\"studyHours\":").append(s.getStudyHours()).append(",")
                    .append("\"assignmentScore\":").append(s.getAssignmentScore()).append(",")
                    .append("\"gpa\":").append(s.calculateGPA()).append(",")
                    .append("\"riskLevel\":\"").append(escapeJson(s.getRiskLevel())).append("\"")
                    .append("}");

            if (i < students.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]}");
        return sb.toString();
    }

    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseFormBody(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isBlank()) {
            return params;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = decode(kv[0]);
            String value = kv.length > 1 ? decode(kv[1]) : "";
            params.put(key, value);
        }

        return params;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        addCorsHeaders(exchange);
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String valueOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Attendance Handler
    private static class AttendanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) {
                return;
            }

            String method = exchange.getRequestMethod();
            UserDAO dao = new UserDAO();

            if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> form = parseFormBody(body);

                int studentId = parseInt(form.get("studentId"));
                String date = form.get("date");
                String status = form.get("status");
                String notes = form.get("notes");
                int markedBy = parseInt(form.get("markedBy"));

                boolean saved = dao.saveAttendance(studentId, date, status, notes, markedBy);

                if (saved) {
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Attendance saved\"}");
                } else {
                    sendJson(exchange, 500, "{\"success\":false,\"message\":\"Failed to save attendance\"}");
                }
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                String query = valueOrEmpty(exchange.getRequestURI().getQuery());
                Map<String, String> params = parseFormBody(query);
                String date = params.get("date");

                if (date == null || date.isEmpty()) {
                    sendJson(exchange, 400, "{\"success\":false,\"message\":\"Date parameter required\"}");
                    return;
                }

                ResultSet rs = dao.getAttendanceByDate(date);

                StringBuilder json = new StringBuilder("{\"success\":true,\"attendance\":[");
                try {
                    boolean first = true;
                    while (rs != null && rs.next()) {
                        if (!first) json.append(",");
                        first = false;
                        json.append("{")
                            .append("\"studentId\":").append(rs.getInt("student_id")).append(",")
                            .append("\"studentName\":\"").append(escapeJson(rs.getString("student_name"))).append("\",")
                            .append("\"stream\":\"").append(escapeJson(rs.getString("stream"))).append("\",")
                            .append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\",")
                            .append("\"notes\":\"").append(escapeJson(rs.getString("notes") != null ? rs.getString("notes") : "")).append("\"")
                            .append("}");
                    }
                    if (rs != null) rs.close();
                } catch (Exception e) {
                    System.err.println("Error processing attendance results: " + e.getMessage());
                }
                json.append("]}");
                sendJson(exchange, 200, json.toString());
                return;
            }

            sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
        }
    }

    // Exam Results Handler
    private static class ExamResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) {
                return;
            }

            String method = exchange.getRequestMethod();
            UserDAO dao = new UserDAO();

            if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> form = parseFormBody(body);

                int studentId = parseInt(form.get("studentId"));
                String examName = form.get("examName");
                String subject = form.get("subject");
                double marksObtained = parseDouble(form.get("marksObtained"));
                double totalMarks = parseDouble(form.get("totalMarks"));
                String examDate = form.get("examDate");

                boolean saved = dao.saveExamResult(studentId, examName, subject, marksObtained, totalMarks, examDate);

                if (saved) {
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Exam result saved\"}");
                } else {
                    sendJson(exchange, 500, "{\"success\":false,\"message\":\"Failed to save exam result\"}");
                }
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                String query = valueOrEmpty(exchange.getRequestURI().getQuery());
                Map<String, String> params = parseFormBody(query);

                List<Map<String, Object>> results;

                if (params.containsKey("studentId")) {
                    int studentId = parseInt(params.get("studentId"));
                    results = dao.getExamResults(studentId);
                } else {
                    String examName = params.get("examName");
                    String subject = params.get("subject");
                    results = dao.getAllExamResults(examName, subject);
                }

                StringBuilder json = new StringBuilder("{\"success\":true,\"results\":[");
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> r = results.get(i);
                    json.append("{");
                    int idx = 0;
                    for (Map.Entry<String, Object> entry : r.entrySet()) {
                        if (idx > 0) json.append(",");
                        json.append("\"").append(entry.getKey()).append("\":\"")
                            .append(escapeJson(String.valueOf(entry.getValue()))).append("\"");
                        idx++;
                    }
                    json.append("}");
                    if (i < results.size() - 1) json.append(",");
                }
                json.append("]}");
                sendJson(exchange, 200, json.toString());
                return;
            }

            sendJson(exchange, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
        }
    }
}
