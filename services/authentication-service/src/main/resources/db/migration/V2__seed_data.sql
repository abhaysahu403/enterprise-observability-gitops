-- All seed accounts use the password: Password@123
-- Hash generated with BCrypt strength 12.

INSERT INTO users (username, email, password, first_name, last_name, enabled) VALUES
('admin',      'admin@enterprise.demo',      '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'System',   'Administrator', TRUE),
('hr.sharma',  'hr.sharma@enterprise.demo',  '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Anita',    'Sharma',        TRUE),
('hr.iyer',    'hr.iyer@enterprise.demo',    '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Karthik',  'Iyer',          TRUE),
('mgr.rao',    'mgr.rao@enterprise.demo',    '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Suresh',   'Rao',           TRUE),
('mgr.gupta',  'mgr.gupta@enterprise.demo',  '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Priya',    'Gupta',         TRUE),
('mgr.khan',   'mgr.khan@enterprise.demo',   '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Imran',    'Khan',          TRUE),
('emp.verma',  'emp.verma@enterprise.demo',  '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Rohit',    'Verma',         TRUE),
('emp.nair',   'emp.nair@enterprise.demo',   '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Divya',    'Nair',          TRUE),
('emp.singh',  'emp.singh@enterprise.demo',  '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Aman',     'Singh',         TRUE),
('emp.reddy',  'emp.reddy@enterprise.demo',  '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Sneha',    'Reddy',         TRUE),
('emp.das',    'emp.das@enterprise.demo',    '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Arjun',    'Das',           TRUE),
('emp.mehta',  'emp.mehta@enterprise.demo',  '$2b$12$YVVP2fjcKoG3OCA.NVRsv.7a7ZBciLvBXuFbl138qCAIDR0EIiVc6', 'Kavita',   'Mehta',         TRUE);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin';

INSERT INTO user_roles (user_id, role)
SELECT id, 'HR' FROM users WHERE username IN ('hr.sharma', 'hr.iyer');

INSERT INTO user_roles (user_id, role)
SELECT id, 'MANAGER' FROM users WHERE username IN ('mgr.rao', 'mgr.gupta', 'mgr.khan');

INSERT INTO user_roles (user_id, role)
SELECT id, 'EMPLOYEE' FROM users WHERE username IN
    ('emp.verma', 'emp.nair', 'emp.singh', 'emp.reddy', 'emp.das', 'emp.mehta');
