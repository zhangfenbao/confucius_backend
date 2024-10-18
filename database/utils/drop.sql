-- ========================
-- Disable triggers (optional)
-- ========================
SET session_replication_role = 'replica';

-- ========================
-- Drop foreign key constraints
-- ========================
DO $$ DECLARE
    r RECORD;
BEGIN
    -- Loop through each constraint in the current schema
    FOR r IN (SELECT constraint_name, table_name
              FROM information_schema.table_constraints
              WHERE constraint_type = 'FOREIGN KEY' 
              AND table_schema = 'public') LOOP
        EXECUTE 'ALTER TABLE ' || r.table_name || ' DROP CONSTRAINT ' || r.constraint_name || ' CASCADE';
    END LOOP;
END $$;

-- ========================
-- Drop all triggers
-- ========================
DO $$ DECLARE
    r RECORD;
BEGIN
    -- Loop through each trigger in the current schema
    FOR r IN (SELECT trigger_name, event_object_table
              FROM information_schema.triggers
              WHERE trigger_schema = 'public') LOOP
        EXECUTE 'DROP TRIGGER IF EXISTS ' || r.trigger_name || ' ON ' || r.event_object_table || ' CASCADE';
    END LOOP;
END $$;

-- ========================
-- Drop all views
-- ========================
DO $$ DECLARE
    r RECORD;
BEGIN
    -- Loop through each view in the current schema
    FOR r IN (SELECT table_name FROM information_schema.views WHERE table_schema = 'public') LOOP
        EXECUTE 'DROP VIEW IF EXISTS ' || r.table_name || ' CASCADE';
    END LOOP;
END $$;

-- ========================
-- Drop all tables
-- ========================
DO $$ DECLARE
    r RECORD;
BEGIN
    -- Loop through each table in the current schema
    FOR r IN (SELECT table_name FROM information_schema.tables 
              WHERE table_schema = 'public' AND table_type = 'BASE TABLE') LOOP
        EXECUTE 'DROP TABLE IF EXISTS ' || r.table_name || ' CASCADE';
    END LOOP;
END $$;

-- ========================
-- Drop all sequences
-- ========================
DO $$ DECLARE
    r RECORD;
BEGIN
    -- Loop through each sequence in the current schema
    FOR r IN (SELECT sequence_name FROM information_schema.sequences 
              WHERE sequence_schema = 'public') LOOP
        EXECUTE 'DROP SEQUENCE IF EXISTS ' || r.sequence_name || ' CASCADE';
    END LOOP;
END $$;

-- ========================
-- Re-enable triggers (optional)
-- ========================
SET session_replication_role = 'origin';