-- =========================================================
-- 인터프리터 실행 로그 (Interpreter Log)
-- =========================================================

-- 기존 테이블 삭제
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS interpreter_log;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE interpreter_log (
    log_id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    course_id      INT NOT NULL,
    chapter_id     INT NOT NULL,
    language_id    INT NOT NULL COMMENT '실행 언어 ID (Judge0 기준)',
    executed_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_interpreter_log_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_interpreter_log_course
        FOREIGN KEY (course_id) REFERENCES course(course_id) ON DELETE CASCADE,
    CONSTRAINT fk_interpreter_log_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapter(chapter_id) ON DELETE CASCADE,

    INDEX idx_interpreter_log_user_date (user_id, executed_at),
    INDEX idx_interpreter_log_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='인터프리터 실행 로그';

