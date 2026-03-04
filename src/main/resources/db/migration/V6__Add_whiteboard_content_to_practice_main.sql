-- Add whiteboard_content JSONB column to practice_main and practice_main_history

ALTER TABLE practice_main
    ADD COLUMN whiteboard_content JSONB;

ALTER TABLE practice_main_history
    ADD COLUMN whiteboard_content JSONB;

