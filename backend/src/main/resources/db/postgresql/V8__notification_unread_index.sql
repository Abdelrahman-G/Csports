CREATE INDEX idx_user_notification_unread_recipient
    ON user_notification (recipient_id)
    WHERE is_read = FALSE;
