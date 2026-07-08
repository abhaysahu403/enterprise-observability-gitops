CREATE TABLE employee_salary_profiles (
    employee_id     BIGINT PRIMARY KEY,
    employee_name   VARCHAR(200) NOT NULL,
    basic_salary    NUMERIC(12,2) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE payroll_records (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL,
    employee_name   VARCHAR(200) NOT NULL,
    pay_month       INT NOT NULL,
    pay_year        INT NOT NULL,
    basic_salary    NUMERIC(12,2) NOT NULL,
    bonus           NUMERIC(12,2) NOT NULL DEFAULT 0,
    gross_salary    NUMERIC(12,2) NOT NULL,
    tax_deducted    NUMERIC(12,2) NOT NULL DEFAULT 0,
    net_salary      NUMERIC(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    generated_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    UNIQUE (employee_id, pay_month, pay_year)
);

CREATE INDEX idx_payroll_employee ON payroll_records(employee_id);
CREATE INDEX idx_payroll_period ON payroll_records(pay_year, pay_month);
