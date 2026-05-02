const API_BASE = 'http://localhost:8080/api';

function getCurrentUser() {
    return {
        isLoggedIn: localStorage.getItem('isLoggedIn') === 'true',
        id: parseInt(localStorage.getItem('userId') || '0', 10) || 0,
        email: localStorage.getItem('userEmail') || '',
        name: localStorage.getItem('userName') || '',
        role: localStorage.getItem('userRole') || 'student'
    };
}

function requireLogin(expectedRole) {
    const user = getCurrentUser();
    if (!user.isLoggedIn) {
        window.location.href = 'login.html';
        return false;
    }

    if (expectedRole && user.role !== expectedRole) {
        window.location.href = user.role === 'teacher' ? 'teacher-dashboard.html' : 'student-dashboard.html';
        return false;
    }

    return true;
}

function logoutToHome() {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userName');
    window.location.href = 'index.html';
}

async function apiGet(path) {
    const response = await fetch(`${API_BASE}${path}`);
    const data = await response.json();
    if (!response.ok || !data.success) {
        throw new Error(data.message || 'Request failed');
    }
    return data;
}

async function apiPostForm(path, formDataObj) {
    const formData = new URLSearchParams();
    Object.keys(formDataObj).forEach((key) => {
        formData.append(key, formDataObj[key] == null ? '' : String(formDataObj[key]));
    });

    const response = await fetch(`${API_BASE}${path}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: formData.toString()
    });

    const data = await response.json();
    if (!response.ok || !data.success) {
        throw new Error(data.message || 'Request failed');
    }

    return data;
}

async function apiDelete(path) {
    const response = await fetch(`${API_BASE}${path}`, { method: 'DELETE' });
    const data = await response.json();
    if (!response.ok || !data.success) {
        throw new Error(data.message || 'Request failed');
    }
    return data;
}

function riskClassFromLevel(riskLevel) {
    const value = (riskLevel || '').toUpperCase();
    if (value.includes('CRITICAL') || value.includes('HIGH')) return 'risk-high';
    if (value.includes('MEDIUM') || value.includes('ATTENTION')) return 'risk-medium';
    return 'risk-low';
}
