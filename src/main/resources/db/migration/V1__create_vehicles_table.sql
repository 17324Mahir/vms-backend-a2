CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vehicle_number VARCHAR(64) NOT NULL,
    company VARCHAR(120) NOT NULL,
    type VARCHAR(60) NOT NULL,
    brand VARCHAR(80) NOT NULL,
    model VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uk_vehicles_vehicle_number UNIQUE (vehicle_number),
    CONSTRAINT chk_vehicles_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);
