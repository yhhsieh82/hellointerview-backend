ALTER TABLE practice
ADD CONSTRAINT uq_practice_main_question UNIQUE (practice_main_id, question_id);
