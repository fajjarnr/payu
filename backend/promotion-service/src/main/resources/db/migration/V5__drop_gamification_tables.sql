-- SIMP-002: Remove gamification tables (feature removed)
DROP TABLE IF EXISTS xp_transactions CASCADE;
DROP TABLE IF EXISTS user_badges CASCADE;
DROP TABLE IF EXISTS badges CASCADE;
DROP TABLE IF EXISTS user_levels CASCADE;
DROP TABLE IF EXISTS level_rewards CASCADE;
DROP TABLE IF EXISTS daily_checkins CASCADE;
