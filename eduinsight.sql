-- MySQL Schema for EduInsight
-- Create database
CREATE DATABASE IF NOT EXISTS eduinsight DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE eduinsight;

-- Drop existing tables
DROP TABLE IF EXISTS alerts;
DROP TABLE IF EXISTS skill_data;
DROP TABLE IF EXISTS results;
DROP TABLE IF EXISTS performance_trends;
DROP TABLE IF EXISTS exam_results;
DROP TABLE IF EXISTS academic_data;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('student', 'teacher')),
    gender VARCHAR(10) NULL,
    age INT NULL,
    stream VARCHAR(50) NULL,
    subject VARCHAR(100) NULL,
    phone VARCHAR(15) NULL,
    address TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    INDEX idx_users_role (role),
    INDEX idx_users_stream (stream),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Create academic_data table
CREATE TABLE academic_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    attendance DECIMAL(5,2) NOT NULL DEFAULT 0,
    study_hours DECIMAL(5,2) NOT NULL DEFAULT 0,
    assignment_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_academic_student (student_id),
    INDEX idx_academic_attendance (attendance),
    INDEX idx_academic_study_hours (study_hours)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Create exam_results table
CREATE TABLE exam_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    exam_name VARCHAR(100) NOT NULL,
    subject VARCHAR(50) NOT NULL,
    marks_obtained DECIMAL(6,2) NOT NULL,
    total_marks DECIMAL(6,2) NOT NULL,
    percentage DECIMAL(5,2) NOT NULL,
    exam_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_exam_student (student_id),
    INDEX idx_exam_subject (subject),
    INDEX idx_exam_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Create performance_trends table
CREATE TABLE performance_trends (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    month_date DATE NOT NULL,
    gpa DECIMAL(3,2) NOT NULL,
    attendance DECIMAL(5,2) NOT NULL,
    study_hours DECIMAL(5,2) NOT NULL,
    risk_score DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_month (student_id, month_date),
    INDEX idx_trends_month (month_date),
    INDEX idx_trends_risk_score (risk_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Create results table
CREATE TABLE results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    predicted_gpa DECIMAL(3,2) NOT NULL,
    actual_gpa DECIMAL(3,2) NULL,
    grade VARCHAR(10) NOT NULL,
    risk_score DECIMAL(5,2) NOT NULL,
    risk_level VARCHAR(10) NOT NULL CHECK (risk_level IN ('safe', 'medium', 'high', 'critical')),
    suggestions TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_results_student (student_id),
    INDEX idx_results_risk_level (risk_level),
    INDEX idx_results_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Create skill_data table
CREATE TABLE skill_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    part_time_job VARCHAR(10) NULL,
    extracurricular VARCHAR(10) NULL,
    technical_skills VARCHAR(200) NULL,
    certifications VARCHAR(200) NULL,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_skill_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Create alerts table
CREATE TABLE alerts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    teacher_id INT NULL,
    alert_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('low', 'medium', 'high', 'critical')),
    is_resolved TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_alert_student (student_id),
    INDEX idx_alert_teacher (teacher_id),
    INDEX idx_alert_severity (severity),
    INDEX idx_alert_resolved (is_resolved),
    INDEX idx_alert_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;