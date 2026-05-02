// ========================================
// EDUINSIGHT - MAIN JAVASCRIPT FILE
// ========================================

// Global variables
let currentUser = null;
let userRole = null;

// ========================================
// AUTHENTICATION FUNCTIONS
// ========================================

// Set user role (called from role selection)
function setUserRole(role) {
    localStorage.setItem('userRole', role);
    userRole = role;
}

// Get current user role
function getUserRole() {
    return localStorage.getItem('userRole') || 'student';
}

// Login function
async function login(email, password, role) {
    // Demo login - in production, this would call an API
    if (email && password) {
        localStorage.setItem('isLoggedIn', 'true');
        localStorage.setItem('userEmail', email);
        localStorage.setItem('userRole', role);
        
        if (role === 'student') {
            window.location.href = 'student-dashboard.html';
        } else {
            window.location.href = 'teacher-dashboard.html';
        }
        return true;
    }
    return false;
}

// Logout function
function logout() {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRole');
    window.location.href = 'index.html';
}

// Check if user is logged in
function isLoggedIn() {
    return localStorage.getItem('isLoggedIn') === 'true';
}

// Redirect if not logged in
function requireAuth() {
    if (!isLoggedIn()) {
        window.location.href = 'login.html';
    }
}

// ========================================
// GPA CALCULATION FUNCTIONS
// ========================================

// Calculate GPA from attendance, study hours, and assignment scores
function calculateGPA(attendance, studyHours, assignmentScore) {
    const gpa = (attendance / 100) * 4 * 0.4 +
                (studyHours / 8) * 4 * 0.4 +
                (assignmentScore / 100) * 4 * 0.2;
    return Math.min(gpa, 4.0).toFixed(2);
}

// Get risk level based on GPA
function getRiskLevel(gpa) {
    if (gpa < 2.0) return { level: 'High', color: '#FF4C4C', emoji: '🔴' };
    if (gpa < 2.5) return { level: 'Medium', color: '#FF9800', emoji: '🟠' };
    if (gpa < 3.0) return { level: 'Low', color: '#FFC107', emoji: '🟡' };
    return { level: 'Safe', color: '#4CAF50', emoji: '🟢' };
}

// Get grade from percentage
function getGrade(percentage) {
    if (percentage >= 90) return 'A+';
    if (percentage >= 80) return 'A';
    if (percentage >= 70) return 'B+';
    if (percentage >= 60) return 'B';
    if (percentage >= 50) return 'C';
    if (percentage >= 40) return 'D';
    return 'F';
}

// ========================================
// RECOMMENDATION ENGINE
// ========================================

// Generate recommendations based on student data
function generateRecommendations(studentData) {
    const recommendations = [];
    
    if (studentData.attendance < 75) {
        recommendations.push({
            type: 'warning',
            title: '⚠️ Low Attendance Alert',
            message: 'Your attendance is below 75%. Attend more classes to improve your GPA.',
            action: 'Set attendance goal'
        });
    }
    
    if (studentData.studyHours < 4) {
        recommendations.push({
            type: 'info',
            title: '📚 Increase Study Hours',
            message: `Top performers study 5+ hours/day. You study ${studentData.studyHours} hours.`,
            action: 'Create study schedule'
        });
    }
    
    if (studentData.assignmentScore < 70) {
        recommendations.push({
            type: 'warning',
            title: '📝 Focus on Assignments',
            message: 'Assignments contribute 20% to your GPA. Complete pending assignments.',
            action: 'View assignments'
        });
    }
    
    const gpa = calculateGPA(studentData.attendance, studentData.studyHours, studentData.assignmentScore);
    if (gpa < 2.5) {
        recommendations.push({
            type: 'danger',
            title: '🎯 Academic Intervention Needed',
            message: 'Your GPA is below average. Consider tutoring or study groups.',
            action: 'Get help'
        });
    }
    
    if (recommendations.length === 0) {
        recommendations.push({
            type: 'success',
            title: '🌟 Excellent Performance!',
            message: 'Keep up the great work! You\'re on track for success.',
            action: 'Set higher goals'
        });
    }
    
    return recommendations;
}

// ========================================
// CHART FUNCTIONS (using Chart.js)
// ========================================

// Create performance line chart
function createPerformanceChart(ctx, labels, data) {
    return new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Your GPA',
                data: data,
                borderColor: '#6C63FF',
                backgroundColor: 'rgba(108, 99, 255, 0.1)',
                tension: 0.4,
                fill: true
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { position: 'bottom' }
            }
        }
    });
}

// Create GPA distribution bar chart
function createGPAChart(ctx, data) {
    return new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['0-1.5', '1.5-2.0', '2.0-2.5', '2.5-3.0', '3.0-3.5', '3.5-4.0'],
            datasets: [{
                label: 'Number of Students',
                data: data,
                backgroundColor: '#6C63FF',
                borderRadius: 10
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}

// Create risk distribution doughnut chart
function createRiskChart(ctx, data) {
    return new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Low Risk', 'Medium Risk', 'High Risk'],
            datasets: [{
                data: data,
                backgroundColor: ['#4CAF50', '#FFC107', '#FF4C4C'],
                borderRadius: 10
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}

// ========================================
// EXPORT FUNCTIONS
// ========================================

// Export data to CSV
function exportToCSV(data, filename) {
    const headers = Object.keys(data[0]);
    const csvRows = [];
    
    csvRows.push(headers.join(','));
    
    for (const row of data) {
        const values = headers.map(header => {
            const value = row[header];
            return typeof value === 'string' ? `"${value.replace(/"/g, '""')}"` : value;
        });
        csvRows.push(values.join(','));
    }
    
    const blob = new Blob([csvRows.join('\n')], { type: 'text/csv' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${filename}_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
}

// Export to PDF (simple version)
function exportToPDF(content, filename) {
    const printWindow = window.open('', '_blank');
    printWindow.document.write(`
        <html>
            <head>
                <title>${filename}</title>
                <style>
                    body { font-family: Arial, sans-serif; padding: 20px; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background: #6C63FF; color: white; }
                </style>
            </head>
            <body>${content}</body>
        </html>
    `);
    printWindow.document.close();
    printWindow.print();
}

// ========================================
// NOTIFICATION FUNCTIONS
// ========================================

// Show toast notification
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        padding: 12px 24px;
        background: ${type === 'success' ? '#4CAF50' : type === 'error' ? '#FF4C4C' : '#6C63FF'};
        color: white;
        border-radius: 8px;
        z-index: 9999;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
    }, 3000);
}

// Show confirmation dialog
function confirmAction(message, onConfirm) {
    if (confirm(message)) {
        onConfirm();
    }
}

// ========================================
// FORM VALIDATION
// ========================================

// Validate email format
function isValidEmail(email) {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

// Validate password strength
function getPasswordStrength(password) {
    let strength = 0;
    if (password.length >= 8) strength++;
    if (password.match(/[a-z]/)) strength++;
    if (password.match(/[A-Z]/)) strength++;
    if (password.match(/[0-9]/)) strength++;
    if (password.match(/[^a-zA-Z0-9]/)) strength++;
    
    if (strength <= 2) return 'Weak';
    if (strength <= 4) return 'Medium';
    return 'Strong';
}

// Validate attendance (0-100)
function isValidAttendance(value) {
    return value >= 0 && value <= 100;
}

// Validate study hours (0-24)
function isValidStudyHours(value) {
    return value >= 0 && value <= 24;
}

// ========================================
// LOCAL STORAGE HELPERS
// ========================================

// Save data to localStorage
function saveToLocalStorage(key, data) {
    localStorage.setItem(key, JSON.stringify(data));
}

// Load data from localStorage
function loadFromLocalStorage(key) {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : null;
}

// Clear localStorage
function clearLocalStorage() {
    localStorage.clear();
}

// ========================================
// DOM HELPERS
// ========================================

// Show loading spinner
function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = '<div class="loading-spinner">Loading...</div>';
    }
}

// Hide loading spinner
function hideLoading(elementId, originalContent) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = originalContent;
    }
}

// Format date
function formatDate(date) {
    return new Date(date).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

// Format number with decimals
function formatNumber(num, decimals = 2) {
    return parseFloat(num).toFixed(decimals);
}

// ========================================
// INITIALIZATION
// ========================================

// Initialize page based on current route
if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', function() {
        // Add CSS animations
        const style = document.createElement('style');
        style.textContent = `
            @keyframes slideIn {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
            
            .loading-spinner {
                text-align: center;
                padding: 20px;
            }
            
            .fade-in {
                animation: fadeIn 0.5s ease;
            }
            
            @keyframes fadeIn {
                from { opacity: 0; transform: translateY(20px); }
                to { opacity: 1; transform: translateY(0); }
            }
        `;
        document.head.appendChild(style);
        
        // Add fade-in animation to main content
        const mainContent = document.querySelector('.container, .container-fluid');
        if (mainContent) {
            mainContent.classList.add('fade-in');
        }
    });
}

// Export all functions for use in browser and Node-safe environments
const EduInsight = {
    setUserRole,
    getUserRole,
    login,
    logout,
    isLoggedIn,
    requireAuth,
    calculateGPA,
    getRiskLevel,
    getGrade,
    generateRecommendations,
    createPerformanceChart,
    exportToCSV,
    showToast,
    isValidEmail,
    getPasswordStrength,
    saveToLocalStorage,
    loadFromLocalStorage,
    formatDate
};

if (typeof window !== 'undefined') {
    window.EduInsight = EduInsight;
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = EduInsight;
}