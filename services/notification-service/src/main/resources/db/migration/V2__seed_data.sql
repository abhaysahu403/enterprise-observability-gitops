INSERT INTO notifications (recipient_employee_id, recipient_name, recipient_contact, channel, subject, message, status, retry_count, max_retries, sent_at, created_at) VALUES
(7, 'Myra Chopra', 'myra.chopra@enterprise.demo', 'EMAIL', 'Leave Request Approved', 'Your ANNUAL leave request from 2026-03-10 to 2026-03-13 has been approved.', 'SENT', 0, 3, now() - interval '20 days', now() - interval '20 days'),
(8, 'Riya Nair', 'riya.nair@enterprise.demo', 'EMAIL', 'Leave Request Approved', 'Your CASUAL leave request from 2026-04-02 to 2026-04-06 has been approved.', 'SENT', 0, 3, now() - interval '10 days', now() - interval '10 days'),
(1, 'Employee One', '9812345678', 'SMS', NULL, 'Your June payslip is now available for download.', 'SENT', 0, 3, now() - interval '5 days', now() - interval '5 days'),
(12, 'Ravi Bose', 'ravi.bose@enterprise.demo', 'TEAMS', 'Ticket Assigned', 'Ticket TKT-000005 has been assigned to you with CRITICAL priority.', 'FAILED', 3, 3, NULL, now() - interval '3 hours'),
(9, 'Rekha Kumar', 'rekha.kumar@enterprise.demo', 'EMAIL', 'Asset Assigned', 'A Dell Latitude 5440 laptop has been assigned to you.', 'RETRYING', 1, 3, NULL, now() - interval '1 hours');
