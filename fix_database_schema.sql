-- Fix user_progress table schema to match entity definitions
-- This script addresses the type mismatch between user_id column and users.id

-- Step 1: Drop the foreign key constraint
ALTER TABLE user_progress DROP CONSTRAINT IF EXISTS FKrt37sneeps21829cuqetjm5ye;

-- Step 2: Convert user_id from character varying to bigint
ALTER TABLE user_progress ALTER COLUMN user_id TYPE bigint USING user_id::bigint;

-- Step 3: Recreate the foreign key constraint
ALTER TABLE user_progress ADD CONSTRAINT FKrt37sneeps21829cuqetjm5ye
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Optional: Drop the unique constraint that doesn't exist (mentioned in warning)
-- This will prevent the warning but won't cause an error if it doesn't exist
ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_dus03vmwyluiy7k2p2gcrqbms;