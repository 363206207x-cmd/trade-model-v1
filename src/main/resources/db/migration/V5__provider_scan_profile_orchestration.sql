ALTER TABLE tm_user_config ADD COLUMN IF NOT EXISTS scan_base_profile VARCHAR(16);
ALTER TABLE tm_user_config ADD COLUMN IF NOT EXISTS scan_position_profile VARCHAR(16);
ALTER TABLE tm_user_config ADD COLUMN IF NOT EXISTS scan_pool_profile VARCHAR(16);
ALTER TABLE tm_user_config ADD COLUMN IF NOT EXISTS scan_auto_escalation_enabled BOOLEAN;
ALTER TABLE tm_user_config ADD COLUMN IF NOT EXISTS scan_manual_override_until TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE tm_user_config ADD COLUMN IF NOT EXISTS scan_update_reason VARCHAR(512);
ALTER TABLE tm_user_config ADD COLUMN IF NOT EXISTS scan_updated_at TIMESTAMP WITHOUT TIME ZONE;

INSERT INTO tm_rule_config(rule_id, rule_type, rule_key, rule_value, description, version, enabled) VALUES
('cfg-provider-scan-emergency-price', 'provider_scan_profile_config', 'provider.scan.emergency_price_movement_1m', '0.05', 'Emergency 1m price movement threshold', 'v1.0', TRUE),
('cfg-provider-scan-emergency-liquidation', 'provider_scan_profile_config', 'provider.scan.emergency_liquidation_spike', '90', 'Emergency liquidation spike score threshold', 'v1.0', TRUE),
('cfg-provider-scan-emergency-confused', 'provider_scan_profile_config', 'provider.scan.emergency_confused_score', '85', 'Emergency confused score threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-price', 'provider_scan_profile_config', 'provider.scan.high_price_movement_1m', '0.02', 'High 1m price movement threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-atr', 'provider_scan_profile_config', 'provider.scan.high_atr_multiple_5m', '2.0', 'High 5m ATR multiple threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-volume', 'provider_scan_profile_config', 'provider.scan.high_volume_spike', '2.5', 'High volume spike threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-spread', 'provider_scan_profile_config', 'provider.scan.high_spread_spike', '2.0', 'High spread spike threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-oi', 'provider_scan_profile_config', 'provider.scan.high_open_interest_change', '0.10', 'High open interest change threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-funding', 'provider_scan_profile_config', 'provider.scan.high_funding_extremity', '80', 'High funding extremity threshold', 'v1.0', TRUE),
('cfg-provider-scan-near-boundary', 'provider_scan_profile_config', 'provider.scan.near_boundary_distance', '0.01', 'Near stop or target distance threshold', 'v1.0', TRUE),
('cfg-provider-scan-data-quality', 'provider_scan_profile_config', 'provider.scan.data_quality_deterioration_score', '60', 'Data quality deterioration threshold', 'v1.0', TRUE),
('cfg-provider-scan-standard-confused', 'provider_scan_profile_config', 'provider.scan.standard_confused_score', '55', 'Standard profile confused score threshold', 'v1.0', TRUE),
('cfg-provider-scan-high-hold', 'provider_scan_profile_config', 'provider.scan.high_min_hold_seconds', '300', 'High profile minimum hold seconds', 'v1.0', TRUE),
('cfg-provider-scan-emergency-hold', 'provider_scan_profile_config', 'provider.scan.emergency_min_hold_seconds', '120', 'Emergency profile minimum hold seconds', 'v1.0', TRUE),
('cfg-provider-scan-recovery-cycles', 'provider_scan_profile_config', 'provider.scan.recovery_confirm_cycles', '2', 'Recovery cycles before downgrade', 'v1.0', TRUE),
('cfg-provider-scan-downgrade-cooldown', 'provider_scan_profile_config', 'provider.scan.downgrade_cooldown_seconds', '300', 'Profile downgrade cooldown seconds', 'v1.0', TRUE)
ON CONFLICT (rule_key) DO UPDATE SET
rule_type = EXCLUDED.rule_type,
rule_value = EXCLUDED.rule_value,
description = EXCLUDED.description,
version = EXCLUDED.version,
enabled = EXCLUDED.enabled;
