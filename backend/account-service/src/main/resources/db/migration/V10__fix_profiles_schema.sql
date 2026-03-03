-- V10: Align profiles table with JPA @MapsId mapping
-- Profile entity uses @MapsId (shared PK with users), so user_id column is redundant.
-- Add missing columns: additional_data (jsonb), birth_place, gender.
-- Rename date_of_birth -> birth_date to match entity field.
-- Drop user_id column and created_at/updated_at (not in entity).

-- Add new columns
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS additional_data jsonb;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS birth_place varchar(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS gender varchar(50);

-- Rename date_of_birth to birth_date if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='date_of_birth') THEN
        ALTER TABLE profiles RENAME COLUMN date_of_birth TO birth_date;
    END IF;
END $$;

-- Drop separate user_id column (FK is now profiles.id -> users.id via @MapsId)
ALTER TABLE profiles DROP CONSTRAINT IF EXISTS fk_profile_user;
DROP INDEX IF EXISTS idx_profiles_user_id;
ALTER TABLE profiles DROP COLUMN IF EXISTS user_id;

-- Drop audit columns not in entity
ALTER TABLE profiles DROP COLUMN IF EXISTS created_at;
ALTER TABLE profiles DROP COLUMN IF EXISTS updated_at;

-- Re-add FK constraint: profiles.id references users.id
ALTER TABLE profiles ADD CONSTRAINT fk_profile_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE;
