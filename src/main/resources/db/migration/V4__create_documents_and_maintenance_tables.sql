CREATE TABLE IF NOT EXISTS vehicle_documents (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    document_type VARCHAR(60) NOT NULL,
    document_number VARCHAR(100),
    issue_date DATE,
    expiry_date DATE NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(100),
    file_data TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_vehicle_documents_type CHECK (document_type IN ('REGISTRATION', 'TAX_TOKEN', 'INSURANCE', 'FITNESS_CERTIFICATE', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_vehicle_documents_vehicle_id ON vehicle_documents(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_documents_expiry_date ON vehicle_documents(expiry_date);

CREATE TABLE IF NOT EXISTS vehicle_maintenance_records (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    maintenance_date DATE NOT NULL,
    maintenance_type VARCHAR(60) NOT NULL,
    cost NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    workshop_vendor VARCHAR(160) NOT NULL,
    odometer_reading INT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehicle_maintenance_vehicle_id ON vehicle_maintenance_records(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_maintenance_date ON vehicle_maintenance_records(maintenance_date);

