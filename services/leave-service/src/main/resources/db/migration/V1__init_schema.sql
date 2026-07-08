CREATE TABLE leave_requests (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       BIGINT NOT NULL,
    employee_name     VARCHAR(200) NOT NULL,
    leave_type        VARCHAR(30) NOT NULL,
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    total_days        INT NOT NULL,
    reason            VARCHAR(500),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id       BIGINT,
    approver_name     VARCHAR(200),
    approver_comment  VARCHAR(500),
    decided_at        TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE leave_balances (
    id               BIGSERIAL PRIMARY KEY,
    employee_id      BIGINT NOT NULL,
    leave_type       VARCHAR(30) NOT NULL,
    year             INT NOT NULL,
    total_allocated  INT NOT NULL,
    used             INT NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0,
    UNIQUE (employee_id, leave_type, year)
);

CREATE INDEX idx_leave_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_status ON leave_requests(status);
CREATE INDEX idx_balance_employee_year ON leave_balances(employee_id, year);
