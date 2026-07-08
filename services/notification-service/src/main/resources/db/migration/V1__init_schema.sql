CREATE TABLE notifications (
    id                      BIGSERIAL PRIMARY KEY,
    recipient_employee_id   BIGINT NOT NULL,
    recipient_name          VARCHAR(200) NOT NULL,
    recipient_contact       VARCHAR(200),
    channel                 VARCHAR(20) NOT NULL,
    subject                 VARCHAR(200),
    message                 VARCHAR(1000) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count             INT NOT NULL DEFAULT 0,
    max_retries             INT NOT NULL DEFAULT 3,
    failure_reason          VARCHAR(500),
    last_attempt_at         TIMESTAMP,
    sent_at                 TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_status ON notifications(status);
CREATE INDEX idx_notification_recipient ON notifications(recipient_employee_id);
CREATE INDEX idx_notification_created ON notifications(created_at);
