CREATE TABLE tickets (
    id                      BIGSERIAL PRIMARY KEY,
    ticket_number           VARCHAR(20) NOT NULL UNIQUE,
    title                   VARCHAR(200) NOT NULL,
    description             VARCHAR(2000),
    category                VARCHAR(30) NOT NULL,
    priority                VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    raised_by_employee_id   BIGINT NOT NULL,
    raised_by_name          VARCHAR(200) NOT NULL,
    assigned_to_employee_id BIGINT,
    assigned_to_name        VARCHAR(200),
    sla_due_at              TIMESTAMP NOT NULL,
    resolution              VARCHAR(2000),
    resolved_at             TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE ticket_comments (
    id            BIGSERIAL PRIMARY KEY,
    ticket_id     BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id     BIGINT NOT NULL,
    author_name   VARCHAR(200) NOT NULL,
    comment       VARCHAR(1000) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_status ON tickets(status);
CREATE INDEX idx_ticket_priority ON tickets(priority);
CREATE INDEX idx_ticket_assigned_to ON tickets(assigned_to_employee_id);
CREATE INDEX idx_ticket_comments_ticket ON ticket_comments(ticket_id);
