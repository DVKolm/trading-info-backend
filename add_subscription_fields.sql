-- Add subscription fields to lessons table
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS is_folder BOOLEAN DEFAULT FALSE;
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS subscription_required BOOLEAN DEFAULT FALSE;

-- Update existing folders (those with '(Подписка)' in the name) to require subscription
UPDATE lessons
SET subscription_required = TRUE
WHERE path LIKE '%Подписка%' AND is_folder = TRUE;

-- Mark existing folders as folders
UPDATE lessons
SET is_folder = TRUE
WHERE path IN (
    SELECT DISTINCT parent_folder
    FROM lessons
    WHERE parent_folder IS NOT NULL
);