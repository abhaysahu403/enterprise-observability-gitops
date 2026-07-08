CREATE TABLE employees (
    id              BIGSERIAL PRIMARY KEY,
    employee_code   VARCHAR(20) NOT NULL UNIQUE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(20),
    address         VARCHAR(255),
    department      VARCHAR(30) NOT NULL,
    designation     VARCHAR(100) NOT NULL,
    manager_id      BIGINT REFERENCES employees(id) ON DELETE SET NULL,
    joining_date    DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    salary_grade    VARCHAR(10) NOT NULL,
    auth_user_id    BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_employee_department ON employees(department);
CREATE INDEX idx_employee_manager ON employees(manager_id);
CREATE INDEX idx_employee_status ON employees(status);
CREATE INDEX idx_employee_code ON employees(employee_code);
