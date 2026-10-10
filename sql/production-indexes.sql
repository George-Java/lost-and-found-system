-- Applied only on a new MySQL volume after 01-schema.sql.
-- Supports the actual filtering, sorting and workflow queries used by the application.
ALTER TABLE lost_item
  ADD INDEX idx_lost_item_status_created (status, created_at DESC),
  ADD INDEX idx_lost_item_publisher_created (publisher_id, created_at DESC),
  ADD INDEX idx_lost_item_type_category_status_created (item_type, category, status, created_at DESC);

ALTER TABLE claim_record
  ADD INDEX idx_claim_item_status_created (item_id, status, created_at DESC),
  ADD INDEX idx_claim_claimant_created (claimant_id, created_at DESC),
  -- MySQL UNIQUE permits multiple NULLs: only PENDING claims are mutually exclusive.
  ADD COLUMN pending_claim_guard TINYINT GENERATED ALWAYS AS
    (CASE WHEN status = 'PENDING' THEN 1 ELSE NULL END) STORED,
  ADD UNIQUE INDEX uk_claim_one_pending_per_user (item_id, claimant_id, pending_claim_guard);

ALTER TABLE friend_request
  ADD INDEX idx_friend_request_receiver_status_created (receiver_id, status, created_at DESC),
  ADD INDEX idx_friend_request_requester_status_created (requester_id, status, created_at DESC);
