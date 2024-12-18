-- ========================
-- Extensions
-- ========================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";    -- For gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";     -- For full-text search
CREATE EXTENSION IF NOT EXISTS "unaccent";    -- For text normalization in search

--- Drop all policies

DO $$
DECLARE
    policy RECORD;
BEGIN
    FOR policy IN
        SELECT schemaname, tablename, policyname
        FROM pg_policies
        WHERE schemaname = 'public' -- Change if your policies are in a different schema
    LOOP
        EXECUTE format('DROP POLICY %I ON %I.%I', policy.policyname, policy.schemaname, policy.tablename);
    END LOOP;
END $$;

-- ========================
-- RLS Functions
-- ========================
-- Function to set the current user_id in the application context
CREATE OR REPLACE FUNCTION public.set_current_user_id(user_id text)
RETURNS void AS $$
BEGIN
    PERFORM set_config('app.current_user_id', user_id, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

-- Function to retrieve the current user_id from the application context
CREATE OR REPLACE FUNCTION public.get_current_user_id()
RETURNS text AS $$
    SELECT current_setting('app.current_user_id', true);
$$ LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp;

-- ========================
-- Users Table
-- ========================
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable row-level security on the users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: Allow users to update only their own account details
CREATE POLICY user_can_update_their_account
ON users
FOR UPDATE
USING (user_id = get_current_user_id());

CREATE TABLE IF NOT EXISTS login_attempts (
    email VARCHAR(255),
    attempt_time TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (email, attempt_time)
);

-- ========================
-- Tokens Table
-- ========================
CREATE TABLE IF NOT EXISTS tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_tokens_user_id ON tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_tokens_user_token ON tokens(token, user_id);
CREATE INDEX IF NOT EXISTS idx_tokens_token_revoked_expires_at ON tokens(token, revoked, expires_at);

-- Enable row-level security on the token table
ALTER TABLE tokens ENABLE ROW LEVEL SECURITY;

-- Policy: Allow users to select (retrieve) a token only if it is not revoked and has not expired
CREATE POLICY user_can_access_tokens
ON tokens
FOR SELECT
USING (
    (
        (get_current_user_id() IS NULL OR get_current_user_id() = '') 
        AND token IS NOT NULL 
        AND revoked = false 
        AND expires_at > NOW()
    )
    OR
    (
        (user_id)::text = get_current_user_id()
    )
);

-- Policy: Allow users to insert new tokens associated with their user account
CREATE POLICY user_can_insert_token
ON tokens
FOR INSERT
WITH CHECK (user_id = get_current_user_id());

-- Policy: Allow users to revoke their own tokens
CREATE POLICY user_can_revoke_token
ON tokens
FOR UPDATE
USING (user_id = get_current_user_id());

-- ========================
-- Workspaces Table
-- ========================
CREATE TABLE IF NOT EXISTS workspaces (
    workspace_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    config JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workspaces_title ON workspaces(title);
CREATE INDEX IF NOT EXISTS idx_workspaces_user_id ON workspaces(user_id);

-- Enable row-level security on the workspaces table
ALTER TABLE workspaces ENABLE ROW LEVEL SECURITY;

-- Policy: Allow users to select (retrieve) only their own workspaces
CREATE POLICY user_can_access_their_workspaces
ON workspaces
USING (user_id = get_current_user_id());

-- Policy: Allow users to insert only workspaces associated with their user_id
CREATE POLICY user_can_insert_workspace
ON workspaces
FOR INSERT
WITH CHECK (user_id = get_current_user_id());

-- Policy: Allow users to update only workspaces they own
CREATE POLICY user_can_update_their_workspace
ON workspaces
FOR UPDATE
USING (user_id = get_current_user_id());

-- Policy: Allow users to delete only workspaces they own
CREATE POLICY user_can_delete_their_workspace
ON workspaces
USING (user_id = get_current_user_id());

-- ========================
-- Conversations Table
-- ========================
CREATE TABLE IF NOT EXISTS conversations (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
    title VARCHAR(255),
    archived BOOLEAN DEFAULT FALSE,
    language_code VARCHAR(20) DEFAULT 'english',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversations_workspace_id_active
ON conversations(workspace_id)
WHERE archived = FALSE;

CREATE INDEX IF NOT EXISTS idx_conversations_language_code_active
ON conversations(language_code)
WHERE archived = FALSE;

CREATE INDEX IF NOT EXISTS idx_conversations_archived 
ON conversations(archived) 
WHERE archived = true;

-- Enable row-level security on the conversations table
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;

-- Policy: Allow users to select (retrieve) only conversations in their own workspaces
CREATE POLICY user_can_access_their_conversations
ON conversations
USING (
    workspace_id IN (
        SELECT workspace_id FROM workspaces WHERE user_id = get_current_user_id()
    )
);

-- ========================
-- Messages Table
-- ========================
CREATE TABLE IF NOT EXISTS messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    message_number SERIAL,
    content JSONB NOT NULL,
    content_tsv tsvector,
    language_code VARCHAR(20) DEFAULT 'english',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    token_count INTEGER DEFAULT 0,
    extra_metadata JSONB,
    UNIQUE (conversation_id, message_number),
    CONSTRAINT non_negative_token_count CHECK (token_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_content_tsv ON messages USING gin (content_tsv);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_number ON messages(conversation_id, message_number);
CREATE INDEX IF NOT EXISTS idx_messages_language_code ON messages(language_code);

-- Enable row-level security on the messages table
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

-- Policy: Allow users to select (retrieve) only messages in their own workspaces
CREATE POLICY user_can_access_their_messages
ON messages
USING (
    conversation_id IN (
        SELECT conversation_id FROM conversations 
        WHERE workspace_id IN (
            SELECT workspace_id FROM workspaces WHERE user_id = get_current_user_id()
        )
    )
);

-- ========================
-- Attachments Table
-- ========================
CREATE TABLE IF NOT EXISTS attachments (
    attachment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    message_id UUID REFERENCES messages(message_id) ON DELETE SET NULL,
    content JSONB NOT NULL DEFAULT '{}',
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attachments_conversation_id ON attachments(conversation_id);
CREATE INDEX IF NOT EXISTS idx_attachments_message_id ON attachments(message_id) WHERE message_id IS NOT NULL;

-- Enable row-level security on the attachments table
-- ALTER TABLE attachments ENABLE ROW LEVEL SECURITY;

-- Policy: Allow users to select (retrieve) only attachments in their own workspaces
-- CREATE POLICY user_can_access_their_attachments
-- ON attachments
-- USING (
--     conversation_id IN (
--         SELECT conversation_id FROM conversations
--         WHERE workspace_id IN (
--             SELECT workspace_id FROM workspaces 
--             WHERE user_id = get_current_user_id()
--         )
--     )
-- );

-- Policy: Allow users to insert attachments for messages they own
-- CREATE POLICY user_can_insert_attachment
-- ON attachments
-- FOR INSERT
-- WITH CHECK (
--     conversation_id IN (
--         SELECT conversation_id FROM conversations
--         WHERE workspace_id IN (
--             SELECT workspace_id FROM workspaces 
--             WHERE user_id = get_current_user_id()
--         )
--     )
-- );

-- Policy: Allow users to delete their own attachments
-- CREATE POLICY user_can_delete_their_attachments
-- ON attachments
-- FOR DELETE
-- USING (
--     conversation_id IN (
--         SELECT conversation_id FROM conversations
--         WHERE workspace_id IN (
--             SELECT workspace_id FROM workspaces 
--             WHERE user_id = get_current_user_id()
--         )
--     )
-- );

-- ========================
-- Services Table
-- ========================
CREATE TABLE IF NOT EXISTS services (
    service_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(64) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    workspace_id UUID REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    service_type VARCHAR(255) NOT NULL,
    service_provider VARCHAR(255) NOT NULL,
    api_key TEXT NOT NULL,
    options JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    -- Prevent duplicate providers within the same workspace
    CONSTRAINT unique_provider_per_workspace UNIQUE (workspace_id, service_provider, service_type)
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_services_user_id ON services(user_id);
CREATE INDEX IF NOT EXISTS idx_services_workspace_id ON services(workspace_id);
CREATE INDEX IF NOT EXISTS idx_services_service_type ON services(service_type);

-- Create a unique partial index for user-level services
CREATE UNIQUE INDEX IF NOT EXISTS unique_provider_per_user 
ON services (user_id, service_provider, service_type) 
WHERE workspace_id IS NULL;

-- Enable row-level security
ALTER TABLE services ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only view their own services
CREATE POLICY services_select_policy
ON services
FOR SELECT
USING (user_id = get_current_user_id());

-- Policy: Users can only insert services for themselves
CREATE POLICY services_insert_policy
ON services
FOR INSERT
WITH CHECK (user_id = get_current_user_id());

-- Policy: Users can only update their own services
CREATE POLICY services_update_policy
ON services
FOR UPDATE
USING (user_id = get_current_user_id());

-- Policy: Users can only delete their own services
CREATE POLICY services_delete_policy
ON services
FOR DELETE
USING (user_id = get_current_user_id());


-- ========================
-- Functions and Triggers
-- ========================
-- Function to update 'updated_at' timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updating 'updated_at'
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS trg_workspaces_updated_at ON workspaces;
CREATE TRIGGER trg_workspaces_updated_at
BEFORE UPDATE ON workspaces
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS trg_conversations_updated_at ON conversations;
CREATE TRIGGER trg_conversations_updated_at
BEFORE UPDATE ON conversations
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS trg_messages_updated_at ON messages;
CREATE TRIGGER trg_messages_updated_at
BEFORE UPDATE OF content ON messages
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

DROP TRIGGER IF EXISTS trg_services_updated_at ON services;
CREATE TRIGGER trg_services_updated_at
BEFORE UPDATE ON services
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Function to update 'workspaces.updated_at' when a new conversation is created
CREATE OR REPLACE FUNCTION update_workspace_updated_at() 
RETURNS trigger AS $$
BEGIN
    UPDATE workspaces
    SET updated_at = NOW()
    WHERE workspace_id = NEW.workspace_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger that fires after a new conversation is created
DROP TRIGGER IF EXISTS trg_conversations_update_workspace_updated_at ON conversations;
CREATE TRIGGER trg_conversations_update_workspace_updated_at
AFTER INSERT ON conversations
FOR EACH ROW
EXECUTE PROCEDURE update_workspace_updated_at();

-- Function to update content_tsv for full-text search
CREATE OR REPLACE FUNCTION messages_tsvector_trigger() 
RETURNS trigger AS $$
DECLARE
    lang_config regconfig;
BEGIN
    IF NEW.language_code IS NOT NULL THEN
        BEGIN
            lang_config := NEW.language_code::regconfig;
        EXCEPTION WHEN undefined_object THEN
            lang_config := 'simple'::regconfig;
        END;
    ELSE
        lang_config := 'simple'::regconfig;
    END IF;
    NEW.content_tsv := to_tsvector(lang_config, unaccent(COALESCE(NEW.content->>'content', '')));
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_messages_tsvector_update ON messages;
CREATE TRIGGER trg_messages_tsvector_update
BEFORE INSERT OR UPDATE OF content ON messages
FOR EACH ROW
EXECUTE PROCEDURE messages_tsvector_trigger();

-- Function to update 'conversations.updated_at' when messages change
CREATE OR REPLACE FUNCTION update_conversation_updated_at() 
RETURNS trigger AS $$
BEGIN
    UPDATE conversations
    SET updated_at = NOW()
    WHERE conversation_id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_messages_update_conversation_updated_at ON messages;
CREATE TRIGGER trg_messages_update_conversation_updated_at
AFTER INSERT OR UPDATE ON messages
FOR EACH ROW
EXECUTE PROCEDURE update_conversation_updated_at();

-- Function to drop sequence when a conversation is deleted
CREATE OR REPLACE FUNCTION drop_message_sequence(conversation_id UUID) 
RETURNS VOID AS $$
DECLARE
    seq_name TEXT := 'message_seq_' || conversation_id;
BEGIN
    EXECUTE format('DROP SEQUENCE IF EXISTS %I', seq_name);
END;
$$ LANGUAGE plpgsql;

-- Trigger function to drop message sequence when a conversation is deleted
CREATE OR REPLACE FUNCTION cleanup_sequence_on_conversation_delete() 
RETURNS TRIGGER AS $$
BEGIN
    PERFORM drop_message_sequence(OLD.conversation_id);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cleanup_sequence_conversation_delete ON conversations;
CREATE TRIGGER trg_cleanup_sequence_conversation_delete
AFTER DELETE ON conversations
FOR EACH ROW
EXECUTE PROCEDURE cleanup_sequence_on_conversation_delete();

-- Function to increment message number for a conversation
CREATE OR REPLACE FUNCTION next_message_number(conversation_uuid UUID)
RETURNS INTEGER AS $$
DECLARE
    next_number INTEGER;
BEGIN
    PERFORM 1 FROM conversations WHERE conversation_id = conversation_uuid FOR UPDATE;
    SELECT COALESCE(MAX(message_number), 0) + 1
    INTO next_number
    FROM messages
    WHERE conversation_id = conversation_uuid;

    RETURN next_number;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_message_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.message_number := next_message_number(NEW.conversation_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_set_message_number ON messages;
CREATE TRIGGER trg_set_message_number
BEFORE INSERT ON messages
FOR EACH ROW
EXECUTE FUNCTION set_message_number();

-- Function to clean up expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS integer AS $$
DECLARE
    deleted_count integer;
BEGIN
    DELETE FROM tokens 
    WHERE expires_at < NOW() 
    RETURNING COUNT(*) INTO deleted_count;
    
    RAISE NOTICE 'Deleted % expired tokens', deleted_count;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to authenticate a user with email and password
CREATE OR REPLACE FUNCTION get_user_for_login(p_email VARCHAR(255))
RETURNS TABLE (user_id VARCHAR(64), email VARCHAR(255), password_hash TEXT) AS $$
BEGIN
    RETURN QUERY
    SELECT u.user_id::VARCHAR(64), u.email::VARCHAR(255), u.password_hash::TEXT
    FROM users u
    WHERE u.email = p_email;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

REVOKE ALL ON FUNCTION get_user_for_login(VARCHAR(255)) FROM PUBLIC;

CREATE OR REPLACE FUNCTION check_rate_limit(p_email VARCHAR(255), p_max_attempts INT, p_window_minutes INT)
RETURNS BOOLEAN AS $$
DECLARE
    attempt_count INT;
BEGIN
    SELECT COUNT(*) INTO attempt_count
    FROM login_attempts
    WHERE email = p_email
    AND attempt_time > NOW() - (p_window_minutes || ' minutes')::INTERVAL;

    IF attempt_count >= p_max_attempts THEN
        RETURN FALSE;
    END IF;

    INSERT INTO login_attempts (email, attempt_time) VALUES (p_email, NOW());
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

REVOKE ALL ON FUNCTION check_rate_limit(VARCHAR(255), INT, INT) FROM PUBLIC;

-- ========================
-- Create Public Role
-- ========================
-- Create the user with a password for database connection
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = '%%USER%%'
    ) THEN
        CREATE USER "%%USER%%" WITH PASSWORD '%%PASSWORD%%';
    END IF;
END $$;

-- Grant USAGE permission to access the public schema
GRANT USAGE ON SCHEMA public TO "%%USER%%";

-- Grant SELECT, INSERT, UPDATE, DELETE privileges on all tables in the public schema
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "%%USER%%";

-- Grant USAGE and SELECT privileges on all sequences
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public'
    LOOP
        EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE %I TO "%%USER%%";', r.sequence_name);
    END LOOP;
END $$;

-- Grant EXECUTE privileges on all functions and procedures
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO "%%USER%%";

-- Grant CREATE privileges on the public schema to allow user to create sequences
GRANT CREATE ON SCHEMA public TO "%%USER%%";

-- ========================
-- End of SQL Schema
-- ========================