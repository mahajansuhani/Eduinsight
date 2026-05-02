🎓 EduInsight - Smart Student Performance Analyzer
A full-stack web application that helps educational institutions track, analyze, and predict student academic performance using real-time data and risk assessment.

✨ Features
Student Dashboard: Real-time GPA calculation, interactive performance charts, personalized AI-powered recommendations, subject-wise performance breakdown, risk level alerts (Safe/Low/Medium/High/Critical)

Teacher Dashboard: Complete student management (CRUD operations), CSV import/export, daily attendance tracking with notes, exam results management, class-wide analytics, at-risk student identification, performance visualizations with Chart.js

GPA Formula: GPA = (Attendance/100 × 4 × 0.4) + (StudyHours/8 × 4 × 0.4) + (AssignmentScore/100 × 4 × 0.2)

Risk Levels: CRITICAL (<2.0) | HIGH RISK (2.0-2.5) | MEDIUM RISK (2.5-3.0) | LOW RISK (3.0-3.5) | SAFE (>3.5)

🛠️ Tech Stack
Backend: Java (JDK 25) with built-in HTTP server Database: MySQL 8.0 Frontend: HTML5, CSS3, JavaScript Charts: Chart.js API: RESTful endpoints

📁 Project Structure
java/ ├── src/ │ ├── Main.java # HTTP server & API handlers │ ├── dao/UserDAO.java # Database operations │ ├── model/User.java # User entity & GPA logic │ ├── util/DBConnection.java # MySQL connection │ └── utils/KaggleImporter.java ├── lib/ # JAR dependencies ├── css/style.css # Global styles ├── js/app-core.js # Shared utilities ├── *.html # Frontend pages ├── eduinsight.sql # Database schema └── db.properties # Database config

🚀 Installation & Setup
Prerequisites: Java JDK 25+, MySQL 8.0+, Modern web browser

Step 1 - Create Database:

mysql -u root -p < eduinsight.sql
Default password: pun1613

Step 2 - Compile Backend:

bash
javac -d bin -cp "lib/mysql-connector-j-8.4.0.jar;lib/jbcrypt-0.4.jar" src/Main.java src/dao/UserDAO.java src/model/User.java src/util/DBConnection.java src/utils/KaggleImporter.java
Step 3 - Run Backend:

bash
java -cp "bin;lib/mysql-connector-j-8.4.0.jar;lib/jbcrypt-0.4.jar" Main
Step 4 - Open Frontend: Open index.html in browser (Live Server recommended on port 5500)

📡 API Endpoints
Method	Endpoint	Description
GET	/api/health	Health check
POST	/api/register	Register new user
POST	/api/login	User login
GET	/api/students	Get all students
DELETE	/api/students?id={id}	Soft delete student
📱 Frontend Pages
index.html (Role selection) | login.html (Authentication) | register.html (Account creation) | student-dashboard.html (Student view) | teacher-dashboard.html (Teacher panel) | student-management.html (CRUD operations) | attendance.html (Daily attendance) | exam-results.html (Exam scores) | csv-import.html (Bulk import) | student-setup.html (Profile setup)

🗄️ Database Schema
users (id, name, email, password_hash, role, stream, subject, gender, age, phone, is_active) | academic_data (student_id, attendance, study_hours, assignment_score) | exam_results | performance_trends | alerts | skill_data

🔒 Demo Credentials
Student: Email: test@example.com | Password: test123 | Role: Student

Teacher: Email: mahajansuhani70@gmail.com | Password: test@example.com | Role: Teacher

🎯 Future Enhancements
JWT authentication | BCrypt password hashing | Email notifications | Machine learning predictions | Mobile app | Parent portal | Automated PDF reports

