-- ────────────────────────────────────────────────────────────────────────────
-- Customer feedback table — stores ratings + free-text from the storefront.
-- Idempotent: only creates if not present.
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customer_feedback (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    customer_id   INT NOT NULL,
    rating        TINYINT NOT NULL,                                 -- 1..5
    subject       VARCHAR(200) NULL,
    message       TEXT NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_customer
        FOREIGN KEY (customer_id) REFERENCES user_customer(id)
        ON DELETE CASCADE
);
