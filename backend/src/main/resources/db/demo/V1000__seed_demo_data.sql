-- Demo data for local and preview environments. Password for every account: Harvest#2026
INSERT INTO app_users (email, password_hash, full_name, role) VALUES
    ('agent@notsureance.dev', '$2a$12$sXUXKCgrNW5BXWB6.FdCKO0e/30VudhSTBdC7ZDkmjb2Pco8xxKRK', 'Dana Whitfield', 'AGENT'),
    ('agent2@notsureance.dev', '$2a$12$sXUXKCgrNW5BXWB6.FdCKO0e/30VudhSTBdC7ZDkmjb2Pco8xxKRK', 'Marcus Ortega', 'AGENT'),
    ('adjuster@notsureance.dev', '$2a$12$sXUXKCgrNW5BXWB6.FdCKO0e/30VudhSTBdC7ZDkmjb2Pco8xxKRK', 'Priya Raman', 'ADJUSTER'),
    ('adjuster2@notsureance.dev', '$2a$12$sXUXKCgrNW5BXWB6.FdCKO0e/30VudhSTBdC7ZDkmjb2Pco8xxKRK', 'Tom Becker', 'ADJUSTER');

INSERT INTO policies (policy_number, farm_name, holder_name, holder_email, state_code, county, acreage,
                      coverage_limit, deductible, effective_date, expiry_date, agent_id) VALUES
    ('NSF-100231', 'Prairie Bend Farms', 'Ellen Haas', 'ellen@prairiebend.farm', 'IA', 'Story', 1850, 1500000, 2500, CURRENT_DATE - 400, CURRENT_DATE + 330, 1),
    ('NSF-100232', 'Cedar Creek Grain', 'Luis Navarro', 'luis@cedarcreekgrain.com', 'NE', 'Hall', 2400, 2000000, 5000, CURRENT_DATE - 700, CURRENT_DATE + 30, 1),
    ('NSF-100233', 'Sunfield Orchards', 'Grace Kim', 'grace@sunfield.farm', 'OH', 'Wayne', 640, 600000, 1000, CURRENT_DATE - 250, CURRENT_DATE + 115, 1),
    ('NSF-100234', 'Red Barn Cattle Co', 'Jack Turner', 'jack@redbarncattle.com', 'KS', 'Riley', 3200, 1200000, 2500, CURRENT_DATE - 20, CURRENT_DATE + 345, 1),
    ('NSF-100235', 'Big Muddy Rice', 'Andre Dubois', 'andre@bigmuddyrice.com', 'AR', 'Poinsett', 1500, 1800000, 5000, CURRENT_DATE - 500, CURRENT_DATE + 230, 2),
    ('NSF-100236', 'Hilltop Dairy', 'Sara Lindqvist', 'sara@hilltopdairy.com', 'WI', 'Marathon', 900, 800000, 2500, CURRENT_DATE - 300, CURRENT_DATE + 65, 2);

INSERT INTO equipment (policy_id, category, make, model, model_year, serial_number, insured_value, engine_hours, gps_tracked) VALUES
    (1, 'TRACTOR', 'John Deere', '8R 370', 2021, 'JD8R370-21-0042', 420000, 2100, TRUE),
    (1, 'COMBINE', 'John Deere', 'S780', 2019, 'JDS780-19-1187', 610000, 3400, TRUE),
    (1, 'PLANTER', 'Kinze', '3600', 2017, 'KZ3600-17-5521', 145000, 0, FALSE),
    (2, 'TRACTOR', 'Case IH', 'Magnum 340', 2018, 'CIHM340-18-7730', 310000, 4800, TRUE),
    (2, 'COMBINE', 'Case IH', 'Axial-Flow 8250', 2022, 'CIH8250-22-0911', 720000, 1300, TRUE),
    (2, 'SPRAYER', 'Hagie', 'STS12', 2016, 'HGSTS12-16-3304', 280000, 5200, FALSE),
    (2, 'IRRIGATION_PIVOT', 'Valley', '8000 Series', 2014, 'VLY8000-14-6612', 95000, 0, FALSE),
    (3, 'TRACTOR', 'Kubota', 'M7-172', 2020, 'KBM7172-20-2290', 165000, 1900, FALSE),
    (3, 'UTILITY_VEHICLE', 'Polaris', 'Ranger XP 1000', 2023, 'PLRXP-23-8841', 24000, 400, FALSE),
    (4, 'TRACTOR', 'New Holland', 'T7.315', 2015, 'NHT7315-15-4478', 190000, 6900, FALSE),
    (4, 'BALER', 'Vermeer', '605N', 2019, 'VM605N-19-1022', 68000, 0, FALSE),
    (5, 'COMBINE', 'Claas', 'Lexion 8700', 2021, 'CLX8700-21-0067', 780000, 2200, TRUE),
    (5, 'GRAIN_DRYER', 'GSI', '2112 Tower', 2012, 'GSI2112-12-9150', 160000, 0, FALSE),
    (6, 'TRACTOR', 'Fendt', '724 Vario', 2022, 'FD724-22-3381', 295000, 1100, TRUE),
    (6, 'BALER', 'Kuhn', 'VB 7190', 2018, 'KHVB7190-18-7720', 72000, 0, FALSE);

INSERT INTO claims (claim_number, policy_id, equipment_id, filed_by, assigned_adjuster, cause, status, incident_date,
                    reported_at, description, estimated_loss, approved_amount, harvest_critical, fraud_score,
                    predicted_payout, triage_lane, priority_score, decision_reason, confirmed_fraud, created_at) VALUES
    ('FEQ-2026-H4ILR7X', 1, 2, 1, 3, 'HAIL', 'APPROVED', CURRENT_DATE - 40, now() - interval '39 days',
     'Golf ball sized hail cracked the windshield and dented the cab roof. Photos are attached for review.',
     38500, 33200, TRUE, 0.0800, 31800, 'STANDARD', 58, 'Damage matches the regional storm report', FALSE, now() - interval '39 days'),
    ('FEQ-2026-F1R3S9K', 2, 6, 1, 3, 'FIRE', 'DENIED', CURRENT_DATE - 25, now() - interval '12 days',
     'Burned out in the machine shed overnight.',
     210000, NULL, FALSE, 0.8100, 0, 'SPECIAL_INVESTIGATION', 71, 'Fire marshal found accelerant at two origin points', TRUE, now() - interval '12 days'),
    ('FEQ-2026-C0LL2DE', 3, 8, 1, 4, 'COLLISION', 'PAID', CURRENT_DATE - 60, now() - interval '59 days',
     'Struck a utility pole while turning at the field entrance. Dealer inspected the unit and gave an estimate.',
     12400, 9900, FALSE, 0.0500, 10200, 'FAST_TRACK', 30, 'Clear liability and dealer estimate on file', FALSE, now() - interval '59 days'),
    ('FEQ-2026-L1GHT55', 5, 13, 2, 3, 'LIGHTNING', 'PAID', CURRENT_DATE - 90, now() - interval '89 days',
     'Lightning hit near the dryer and burned the circuit boards. Electrical surge fried the controller.',
     48000, 41000, TRUE, 0.0700, 39500, 'STANDARD', 62, 'Utility confirmed a strike on the service line', FALSE, now() - interval '89 days'),
    ('FEQ-2026-MECH8B2', 6, 15, 2, 4, 'MECHANICAL_BREAKDOWN', 'APPROVED', CURRENT_DATE - 120, now() - interval '118 days',
     'Gearbox bearing failure with metal shavings in the oil. Unit has been moved to the shop.',
     15000, 8100, FALSE, 0.1100, 7900, 'FAST_TRACK', 33, 'Covered parts approved, wear items excluded', FALSE, now() - interval '118 days'),
    ('FEQ-2026-W1ND4QP', 2, 7, 1, 4, 'WIND', 'PAID', CURRENT_DATE - 70, now() - interval '69 days',
     'Straight line winds flipped the pivot towers. Neighbor witnessed the event.',
     52000, 46500, FALSE, 0.0600, 44100, 'STANDARD', 41, 'Wind event confirmed by county emergency office', FALSE, now() - interval '69 days'),
    ('FEQ-2026-TRN6M3A', 1, 1, 1, NULL, 'MECHANICAL_BREAKDOWN', 'SUBMITTED', CURRENT_DATE - 5, now() - interval '4 days',
     'Transmission failed under load while pulling the planter. This happened during our busiest week.',
     18500, NULL, TRUE, NULL, NULL, NULL, 0, NULL, FALSE, now() - interval '4 days'),
    ('FEQ-2026-THF7Z2W', 4, 10, 1, NULL, 'THEFT', 'SUBMITTED', CURRENT_DATE - 16, now() - interval '1 day',
     'Engine seized and the tractor will not start.',
     182000, NULL, FALSE, NULL, NULL, NULL, 0, NULL, FALSE, now() - interval '1 day'),
    ('FEQ-2026-VND3K8P', 3, 9, 1, NULL, 'VANDALISM', 'SUBMITTED', CURRENT_DATE - 3, now() - interval '2 days',
     'Windows smashed and seats slashed by trespassers near the orchard gate. Sheriff report filed.',
     4800, NULL, FALSE, NULL, NULL, NULL, 0, NULL, FALSE, now() - interval '2 days'),
    ('FEQ-2026-HL9CMB4', 2, 5, 1, 3, 'HAIL', 'UNDER_REVIEW', CURRENT_DATE - 12, now() - interval '11 days',
     'Hailstorm shattered the lights and mirrors and pitted the panels on the combine. Photos are attached for review.',
     64000, NULL, TRUE, NULL, NULL, NULL, 0, NULL, FALSE, now() - interval '11 days'),
    ('FEQ-2026-FLD2X7N', 5, 12, 2, 4, 'FLOOD', 'NEEDS_INFO', CURRENT_DATE - 15, now() - interval '14 days',
     'River overflowed and submerged the yard. Flood water covered the engine and electronics on the combine.',
     240000, NULL, TRUE, NULL, NULL, NULL, 0, NULL, FALSE, now() - interval '14 days'),
    ('FEQ-2026-RLV5T1C', 6, 14, 2, NULL, 'ROLLOVER', 'SUBMITTED', CURRENT_DATE - 8, now() - interval '7 days',
     'Tipped over on a steep terrace slope and rolled into the drainage ditch. Operator was not injured.',
     96000, NULL, FALSE, NULL, NULL, NULL, 0, NULL, FALSE, now() - interval '7 days');

INSERT INTO claim_events (claim_id, actor_id, event_type, from_status, to_status, note, created_at)
SELECT id, filed_by, 'FILED', NULL, 'SUBMITTED', NULL, created_at FROM claims;

INSERT INTO claim_events (claim_id, actor_id, event_type, from_status, to_status, note, created_at)
SELECT id, assigned_adjuster, 'STATUS_CHANGED', 'SUBMITTED', 'UNDER_REVIEW', NULL, created_at + interval '1 day'
FROM claims WHERE assigned_adjuster IS NOT NULL;

INSERT INTO claim_events (claim_id, actor_id, event_type, from_status, to_status, note, created_at)
SELECT id, assigned_adjuster, 'STATUS_CHANGED', 'UNDER_REVIEW',
       CASE WHEN status = 'DENIED' THEN 'DENIED' WHEN status = 'NEEDS_INFO' THEN 'NEEDS_INFO' ELSE 'APPROVED' END,
       COALESCE(decision_reason, 'Need the dealer repair estimate and photos of the engine bay'), created_at + interval '3 days'
FROM claims WHERE status IN ('APPROVED', 'PAID', 'DENIED', 'NEEDS_INFO');

INSERT INTO claim_events (claim_id, actor_id, event_type, from_status, to_status, note, created_at)
SELECT id, assigned_adjuster, 'STATUS_CHANGED', 'APPROVED', 'PAID', 'Payment issued', created_at + interval '6 days'
FROM claims WHERE status = 'PAID';

INSERT INTO quote_leads (full_name, email, phone, state_code, equipment_category, equipment_value, model_year,
                         deductible, annual_premium, status, assigned_agent_id) VALUES
    ('Owen Fischer', 'owen@fischerfarms.com', '515-555-0142', 'IA', 'COMBINE', 540000, 2020, 5000, 6214.18, 'NEW', 1),
    ('Maria Santos', 'maria@santosproduce.com', '559-555-0199', 'CA', 'TRACTOR', 210000, 2019, 2500, 2180.45, 'CONTACTED', 1),
    ('Ben Holloway', 'ben@hollowayranch.com', NULL, 'TX', 'UTILITY_VEHICLE', 22000, 2024, 1000, 380.60, 'NEW', 2);
