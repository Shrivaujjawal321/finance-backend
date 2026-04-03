// API client - handles all backend communication

const API_BASE = '/api';

function getToken() {
    return localStorage.getItem('token');
}

function getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
}

function saveAuth(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify({ username: data.username, role: data.role }));
}

function clearAuth() {
    localStorage.clear();
}

function isLoggedIn() {
    return !!getToken();
}

function requireAuth() {
    if (!isLoggedIn()) {
        window.location.href = '/index.html';
        return false;
    }
    return true;
}

async function apiCall(endpoint, options = {}) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers: { ...headers, ...options.headers }
    });

    const isAuthEndpoint = endpoint.startsWith('/auth/');

    if (response.status === 401 && !isAuthEndpoint) {
        clearAuth();
        window.location.href = '/index.html';
        throw new Error('Session expired');
    }

    if (response.status === 403) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.message || 'Access denied');
    }

    if (response.status === 204) return null;

    const data = await response.json();

    if (!response.ok) {
        const msg = data.fieldErrors
            ? Object.values(data.fieldErrors).join(', ')
            : data.message || 'Something went wrong';
        throw new Error(msg);
    }

    return data;
}

// Auth
const Auth = {
    login: (body) => apiCall('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
    register: (body) => apiCall('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
};

// Records
const Records = {
    list: (params = {}) => {
        const qs = new URLSearchParams();
        Object.entries(params).forEach(([k, v]) => { if (v !== '' && v != null) qs.set(k, v); });
        return apiCall(`/records?${qs.toString()}`);
    },
    get: (id) => apiCall(`/records/${id}`),
    create: (body) => apiCall('/records', { method: 'POST', body: JSON.stringify(body) }),
    update: (id, body) => apiCall(`/records/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id) => apiCall(`/records/${id}`, { method: 'DELETE' }),
};

// Dashboard
const Dashboard = {
    summary: () => apiCall('/dashboard/summary'),
};

// Users
const Users = {
    list: () => apiCall('/users'),
    get: (id) => apiCall(`/users/${id}`),
    update: (id, body) => apiCall(`/users/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id) => apiCall(`/users/${id}`, { method: 'DELETE' }),
};
