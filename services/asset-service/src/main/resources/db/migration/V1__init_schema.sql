CREATE TABLE assets (
    id                      BIGSERIAL PRIMARY KEY,
    asset_tag               VARCHAR(30) NOT NULL UNIQUE,
    type                    VARCHAR(20) NOT NULL,
    model                   VARCHAR(150) NOT NULL,
    vendor                  VARCHAR(100) NOT NULL,
    purchase_date           DATE NOT NULL,
    warranty_expiry         DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    assigned_to_employee_id BIGINT,
    assigned_to_name        VARCHAR(200),
    assigned_at             TIMESTAMP,
    returned_at             TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_asset_status ON assets(status);
CREATE INDEX idx_asset_assigned_to ON assets(assigned_to_employee_id);
CREATE INDEX idx_asset_type ON assets(type);
