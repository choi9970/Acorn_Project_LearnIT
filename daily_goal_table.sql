-- =========================================================
-- 일일 학습 목표 (Daily Goal)
-- =========================================================

-- 기존 테이블 삭제
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS daily_goal;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE daily_goal (
    goal_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    class_goal     INT NOT NULL DEFAULT 2 COMMENT '하루 목표 강의 수',
    time_goal      INT NOT NULL DEFAULT 10 COMMENT '하루 목표 학습 시간 (분)',
    interpreter_goal INT NOT NULL DEFAULT 2 COMMENT '하루 목표 인터프리터 실행 수',
    start_date     DATE NOT NULL COMMENT '목표 시작일 (주 시작일)',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_daily_goal_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,

    UNIQUE KEY uk_user_start_date (user_id, start_date),
    INDEX idx_daily_goal_user_date (user_id, start_date),
    INDEX idx_daily_goal_start_date (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일일 학습 목표';

