-- Sample leave balances for a handful of seeded employees (2026)
INSERT INTO leave_balances (employee_id, leave_type, year, total_allocated, used) VALUES
(7, 'ANNUAL', 2026, 18, 4),
(7, 'CASUAL', 2026, 12, 2),
(7, 'MEDICAL', 2026, 10, 0),
(7, 'WORK_FROM_HOME', 2026, 24, 6),
(8, 'ANNUAL', 2026, 18, 8),
(8, 'CASUAL', 2026, 12, 5),
(9, 'ANNUAL', 2026, 18, 2),
(10, 'MEDICAL', 2026, 10, 3);

-- Sample leave requests
INSERT INTO leave_requests (employee_id, employee_name, leave_type, start_date, end_date, total_days, reason, status, approver_id, approver_name, decided_at) VALUES
(7, 'Myra Chopra', 'ANNUAL', '2026-03-10', '2026-03-13', 4, 'Family function', 'APPROVED', 4, 'Manoj Verma', now() - interval '20 days'),
(8, 'Riya Nair', 'CASUAL', '2026-04-02', '2026-04-06', 5, 'Personal work', 'APPROVED', 3, 'Nikhil Naidu', now() - interval '10 days'),
(9, 'Rekha Kumar', 'ANNUAL', '2026-07-15', '2026-07-16', 2, 'Short trip', 'APPROVED', 3, 'Nikhil Naidu', now() - interval '5 days'),
(10, 'Sai Bhatt', 'MEDICAL', '2026-02-01', '2026-02-03', 3, 'Fever', 'APPROVED', 4, 'Manoj Verma', now() - interval '40 days');

INSERT INTO leave_requests (employee_id, employee_name, leave_type, start_date, end_date, total_days, reason, status) VALUES
(7, 'Myra Chopra', 'WORK_FROM_HOME', '2026-07-08', '2026-07-13', 6, 'Home renovation', 'PENDING'),
(8, 'Riya Nair', 'ANNUAL', '2026-08-01', '2026-08-05', 5, 'Vacation', 'PENDING');
