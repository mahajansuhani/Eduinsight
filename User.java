package model;

public class User {
    private int id;
    private String name;
    private String email;
    private String role;
    private String stream;
    private String subject;
    private String gender;
    private int age;
    private String phone;
    private double attendance;
    private double studyHours;
    private double assignmentScore;
    private boolean isActive;
    
    public User() {}
    
    public User(String name, String email, String role, String stream) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.stream = stream;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public double getAttendance() { return attendance; }
    public void setAttendance(double attendance) { this.attendance = attendance; }
    
    public double getStudyHours() { return studyHours; }
    public void setStudyHours(double studyHours) { this.studyHours = studyHours; }
    
    public double getAssignmentScore() { return assignmentScore; }
    public void setAssignmentScore(double assignmentScore) { this.assignmentScore = assignmentScore; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    // Calculate GPA (attendance 40% + study hours 40% + assignments 20%)
    public double calculateGPA() {
        double gpa = (attendance / 100) * 4.0 * 0.4 +
                     (studyHours / 8) * 4.0 * 0.4 +
                     (assignmentScore / 100) * 4.0 * 0.2;
        return Math.min(Math.round(gpa * 100) / 100.0, 4.0);
    }
    
    // Calculate Risk Level
    public String getRiskLevel() {
        double gpa = calculateGPA();
        if (gpa < 2.0) return "CRITICAL";
        if (gpa < 2.5) return "HIGH RISK";
        if (attendance < 60) return "ATTENTION NEEDED";
        if (gpa < 3.0) return "MEDIUM RISK";
        return "SAFE";
    }
    
    public String getRiskColor() {
        String risk = getRiskLevel();
        return switch (risk) {
            case "CRITICAL" -> "#ef4444";
            case "HIGH RISK" -> "#f97316";
            case "ATTENTION NEEDED" -> "#f59e0b";
            case "MEDIUM RISK" -> "#eab308";
            default -> "#10b981";
        };
    }
}
