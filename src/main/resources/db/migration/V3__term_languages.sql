ALTER TABLE terms ADD COLUMN source_language VARCHAR(32);
ALTER TABLE terms ADD COLUMN target_language VARCHAR(32);

-- backfill existing rows (adjust if your old data wasn't English -> Arabic)
UPDATE terms SET source_language = 'ENGLISH', target_language = 'ARABIC';

ALTER TABLE terms ALTER COLUMN source_language SET NOT NULL;
ALTER TABLE terms ALTER COLUMN target_language SET NOT NULL;

ALTER TABLE terms DROP CONSTRAINT uc_user_term;
ALTER TABLE terms ADD CONSTRAINT uc_user_term
    UNIQUE (user_id, term_word, source_language, target_language);