CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_kyc_user_blind
    ON kyc_reviews(user_id_blind_index);
