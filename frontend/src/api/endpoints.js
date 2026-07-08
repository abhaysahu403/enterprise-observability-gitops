import { api } from './client';

// ---------- Authentication ----------
export const AuthAPI = {
  login: (payload) => api.post('/auth/login', payload),
  register: (payload) => api.post('/auth/register', payload),
  logout: (payload) => api.post('/auth/logout', payload),
  roles: () => api.get('/auth/roles'),
  listUsers: (params) => api.get('/auth/users', { params }),
  getUser: (id) => api.get(`/auth/users/${id}`),
  updateUser: (id, payload) => api.put(`/auth/users/${id}`, payload),
  deleteUser: (id) => api.delete(`/auth/users/${id}`)
};

// ---------- Employees ----------
export const EmployeeAPI = {
  list: (params) => api.get('/employees', { params }),
  get: (id) => api.get(`/employees/${id}`),
  create: (payload) => api.post('/employees', payload),
  update: (id, payload) => api.put(`/employees/${id}`, payload),
  remove: (id) => api.delete(`/employees/${id}`),
  byDepartment: (department, params) => api.get(`/employees/department/${department}`, { params }),
  byManager: (managerId) => api.get(`/employees/manager/${managerId}`),
  departmentSummary: () => api.get('/employees/reports/department-summary'),
  activeCount: () => api.get('/employees/reports/active-count')
};

// ---------- Leaves ----------
export const LeaveAPI = {
  apply: (payload) => api.post('/leaves/apply', payload),
  approve: (id, payload) => api.put(`/leaves/${id}/approve`, payload),
  reject: (id, payload) => api.put(`/leaves/${id}/reject`, payload),
  cancel: (id, employeeId) => api.put(`/leaves/${id}/cancel`, null, { params: { employeeId } }),
  balance: (employeeId, year) => api.get('/leaves/balance', { params: { employeeId, year } }),
  history: (params) => api.get('/leaves/history', { params })
};

// ---------- Payroll ----------
export const PayrollAPI = {
  generate: (payload) => api.post('/payroll/generate', payload),
  payslip: (params) => api.get('/payroll/payslip', { params }),
  history: (params) => api.get('/payroll/history', { params }),
  status: (params) => api.get('/payroll/status', { params }),
  markPaid: (id) => api.put(`/payroll/${id}/mark-paid`)
};

// ---------- Assets ----------
export const AssetAPI = {
  list: (params) => api.get('/assets', { params }),
  get: (id) => api.get(`/assets/${id}`),
  create: (payload) => api.post('/assets', payload),
  byEmployee: (employeeId) => api.get(`/assets/employee/${employeeId}`),
  assign: (id, payload) => api.put(`/assets/${id}/assign`, payload),
  returnAsset: (id) => api.put(`/assets/${id}/return`),
  updateStatus: (id, status) => api.put(`/assets/${id}/status`, null, { params: { status } }),
  countByStatus: (status) => api.get('/assets/reports/count-by-status', { params: { status } })
};

// ---------- Help Desk ----------
export const TicketAPI = {
  list: (params) => api.get('/tickets', { params }),
  get: (id) => api.get(`/tickets/${id}`),
  create: (payload) => api.post('/tickets', payload),
  assign: (id, payload) => api.put(`/tickets/${id}/assign`, payload),
  updateStatus: (id, status) => api.put(`/tickets/${id}/status`, null, { params: { status } }),
  resolve: (id, payload) => api.put(`/tickets/${id}/resolve`, payload),
  reopen: (id) => api.put(`/tickets/${id}/reopen`),
  addComment: (id, payload) => api.post(`/tickets/${id}/comments`, payload),
  comments: (id) => api.get(`/tickets/${id}/comments`),
  slaBreached: () => api.get('/tickets/reports/sla-breached'),
  statusSummary: () => api.get('/tickets/reports/status-summary')
};

// ---------- Notifications ----------
export const NotificationAPI = {
  send: (payload) => api.post('/notifications/send', payload),
  get: (id) => api.get(`/notifications/${id}`),
  retry: (id) => api.put(`/notifications/${id}/retry`),
  history: (params) => api.get('/notifications/history', { params }),
  statusSummary: () => api.get('/notifications/reports/status-summary')
};
