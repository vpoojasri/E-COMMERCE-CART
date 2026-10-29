const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

async function request(endpoint, options = {}) {
  const isFormData = options.body instanceof FormData;
  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(options.headers || {}),
  };

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
    credentials: 'include',
  });

  if (response.status === 204) {
    return null;
  }

  let data;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    data = await response.json();
  } else {
    const text = await response.text();
    data = { message: text || response.statusText };
  }

  if (!response.ok) {
    const error = new Error(data?.message || `Request failed with status ${response.status}`);
    error.status = response.status;
    error.fieldErrors = data?.fieldErrors || null;
    throw error;
  }

  return data;
}

export const api = {
  // Authentication
  register: (payload) => request('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  login: (payload) => request('/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  getMe: () => request('/auth/me'),
  updateProfile: (payload) => request('/auth/profile', { method: 'PUT', body: JSON.stringify(payload) }),

  // Catalog & Categories
  getCategories: () => request('/categories'),
  getProducts: (params = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        qs.append(k, v);
      }
    });
    return request(`/products?${qs.toString()}`);
  },
  getProductById: (id) => request(`/products/${id}`),

  // Cart
  getCart: () => request('/cart'),
  addToCart: (productId, quantity = 1) =>
    request('/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity }) }),
  updateCartItem: (cartItemId, quantity) =>
    request(`/cart/items/${cartItemId}`, { method: 'PUT', body: JSON.stringify({ quantity }) }),
  removeCartItem: (cartItemId) =>
    request(`/cart/items/${cartItemId}`, { method: 'DELETE' }),

  // Orders & Tracking
  placeOrder: (payload) => request('/orders', { method: 'POST', body: JSON.stringify(payload) }),
  getMyOrders: (page = 0, size = 10) => request(`/orders/my?page=${page}&size=${size}`),
  getOrderById: (id) => request(`/orders/${id}`),
  cancelOrder: (id, reason) =>
    request(`/orders/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }),
  getOrderTracking: (id) => request(`/orders/${id}/tracking`),
  trackByTrackingNumber: (trackingNumber) =>
    request(`/tracking/${encodeURIComponent(trackingNumber.trim())}`),

  // Notifications
  getNotifications: () => request('/notifications'),
  markNotificationsRead: () => request('/notifications/read-all', { method: 'POST' }),

  // Administrator
  getAdminDashboard: () => request('/admin/dashboard'),
  getAdminUsers: () => request('/admin/users'),
  getAdminOrders: (status = '', page = 0, size = 15) => {
    const qs = new URLSearchParams({ page, size });
    if (status) qs.append('status', status);
    return request(`/admin/orders?${qs.toString()}`);
  },
  updateOrderStatus: (orderId, payload) =>
    request(`/admin/orders/${orderId}/status`, { method: 'PATCH', body: JSON.stringify(payload) }),
  toggleAutomation: (enabled) =>
    request('/admin/automation/toggle', { method: 'POST', body: JSON.stringify({ enabled }) }),
  stepAutomationNow: () =>
    request('/admin/automation/step', { method: 'POST' }),

  createCategory: (payload) =>
    request('/admin/categories', { method: 'POST', body: JSON.stringify(payload) }),
  updateCategory: (id, payload) =>
    request(`/admin/categories/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteCategory: (id) =>
    request(`/admin/categories/${id}`, { method: 'DELETE' }),

  createProduct: (payload) =>
    request('/admin/products', { method: 'POST', body: JSON.stringify(payload) }),
  updateProduct: (id, payload) =>
    request(`/admin/products/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteProduct: (id) =>
    request(`/admin/products/${id}`, { method: 'DELETE' }),
  uploadProductImage: (productId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request(`/admin/products/${productId}/image`, { method: 'POST', body: formData });
  },
  uploadStandaloneImage: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request('/admin/uploads/image', { method: 'POST', body: formData });
  },
};

