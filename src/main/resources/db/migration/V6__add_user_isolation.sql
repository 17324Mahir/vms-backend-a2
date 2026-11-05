-- ==============================================================================
-- Flyway Migration V6: User Fleet Data Isolation
-- Associates vehicles and drivers with the user_email of their owner.
-- Defaulting existing seed data to 'test@starkgroup.test'
-- ==============================================================================

ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS user_email VARCHAR(160) NOT NULL DEFAULT 'test@starkgroup.test';
CREATE INDEX IF NOT EXISTS idx_vehicles_user_email ON vehicles (user_email);

ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS uk_vehicles_vehicle_number;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_vehicles_user_vehicle_number'
    ) THEN
        ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_user_vehicle_number UNIQUE (user_email, vehicle_number);
    END IF;
END $$;

ALTER TABLE drivers ADD COLUMN IF NOT EXISTS user_email VARCHAR(160) NOT NULL DEFAULT 'test@starkgroup.test';
CREATE INDEX IF NOT EXISTS idx_drivers_user_email ON drivers (user_email);

ALTER TABLE drivers DROP CONSTRAINT IF EXISTS uk_drivers_license_number;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_drivers_user_license_number'
    ) THEN
        ALTER TABLE drivers ADD CONSTRAINT uk_drivers_user_license_number UNIQUE (user_email, license_number);
    END IF;
END $$;
