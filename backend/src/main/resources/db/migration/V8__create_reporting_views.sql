-- Reporting layer for Power BI. The report login only ever got SELECT on these views, never on the tables.
CREATE TABLE policy_segments (
    policy_id    BIGINT       PRIMARY KEY REFERENCES policies (id) ON DELETE CASCADE,
    segment      VARCHAR(20)  NOT NULL,
    computed_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE VIEW rpt_claims_daily AS
SELECT (c.created_at AT TIME ZONE 'UTC')::date AS day,
       e.category AS equipment_category,
       p.state_code,
       count(*) AS claims_filed,
       count(*) FILTER (WHERE c.triage_lane = 'FAST_TRACK') AS fast_tracked,
       count(*) FILTER (WHERE c.triage_lane = 'SPECIAL_INVESTIGATION') AS investigated,
       count(*) FILTER (WHERE c.status IN ('APPROVED', 'PAID')) AS approved,
       count(*) FILTER (WHERE c.status = 'DENIED') AS denied,
       sum(c.estimated_loss) AS estimated_loss,
       coalesce(sum(c.approved_amount), 0) AS approved_amount
FROM claims c
JOIN policies p ON p.id = c.policy_id
JOIN equipment e ON e.id = c.equipment_id
GROUP BY 1, 2, 3;

-- Premium was allocated to each machine by its share of the policy's insured value.
CREATE VIEW rpt_loss_ratio_by_segment AS
WITH fleet AS (
    SELECT policy_id, sum(insured_value) AS total FROM equipment GROUP BY policy_id
), premium AS (
    SELECT e.id AS equipment_id, e.category, p.state_code,
           coalesce(p.annual_premium, 0) * e.insured_value / NULLIF(f.total, 0) AS allocated
    FROM equipment e JOIN policies p ON p.id = e.policy_id JOIN fleet f ON f.policy_id = p.id
), losses AS (
    SELECT equipment_id, count(*) AS claims,
           sum(approved_amount) FILTER (WHERE status IN ('APPROVED', 'PAID')) AS incurred
    FROM claims GROUP BY equipment_id
)
SELECT pr.category AS equipment_category, pr.state_code,
       round(sum(pr.allocated), 2) AS earned_premium,
       coalesce(sum(l.incurred), 0) AS incurred_losses,
       coalesce(sum(l.claims), 0) AS claims,
       round(coalesce(sum(l.incurred), 0) / NULLIF(sum(pr.allocated), 0), 4) AS loss_ratio
FROM premium pr LEFT JOIN losses l ON l.equipment_id = pr.equipment_id
GROUP BY 1, 2;

CREATE VIEW rpt_adjuster_cycle_time AS
WITH decisions AS (
    SELECT DISTINCT ON (ev.claim_id) ev.claim_id, ev.actor_id, ev.created_at AS decided_at
    FROM claim_events ev
    WHERE ev.event_type = 'STATUS_CHANGED' AND ev.to_status IN ('APPROVED', 'DENIED')
    ORDER BY ev.claim_id, ev.created_at
)
SELECT u.full_name AS adjuster,
       count(*) AS decisions,
       round((percentile_cont(0.5) WITHIN GROUP (
           ORDER BY extract(EPOCH FROM d.decided_at - c.created_at) / 86400))::numeric, 2) AS median_days_to_decision,
       round(avg(extract(EPOCH FROM d.decided_at - c.created_at) / 86400)::numeric, 2) AS average_days_to_decision
FROM decisions d
JOIN claims c ON c.id = d.claim_id
JOIN app_users u ON u.id = d.actor_id
GROUP BY u.full_name;

CREATE VIEW rpt_fraud_funnel AS
SELECT 1 AS stage_order, 'Filed' AS stage, count(*) AS claims FROM claims
UNION ALL SELECT 2, 'Scored', count(*) FROM claims WHERE fraud_score IS NOT NULL
UNION ALL SELECT 3, 'Fast tracked', count(*) FROM claims WHERE triage_lane = 'FAST_TRACK'
UNION ALL SELECT 4, 'Investigated', count(*) FROM claims WHERE triage_lane = 'SPECIAL_INVESTIGATION'
UNION ALL SELECT 5, 'Confirmed fraud', count(*) FROM claims WHERE confirmed_fraud
UNION ALL SELECT 6, 'Paid', count(*) FROM claims WHERE status = 'PAID';

CREATE VIEW rpt_fraud_by_segment AS
SELECT s.segment,
       count(DISTINCT s.policy_id) AS policies,
       count(c.id) AS claims,
       count(c.id) FILTER (WHERE c.confirmed_fraud OR c.triage_lane = 'SPECIAL_INVESTIGATION') AS flagged_claims,
       round(count(c.id) FILTER (WHERE c.confirmed_fraud OR c.triage_lane = 'SPECIAL_INVESTIGATION')::numeric
             / NULLIF(count(c.id), 0), 4) AS fraud_rate
FROM policy_segments s
LEFT JOIN claims c ON c.policy_id = s.policy_id
GROUP BY s.segment;

-- How often adjusters agreed with the agent, which is the number a manager actually asks about.
CREATE VIEW rpt_agent_proposals AS
SELECT action,
       count(*) AS proposals,
       count(*) FILTER (WHERE status = 'APPROVED') AS approved,
       count(*) FILTER (WHERE status = 'REJECTED') AS overridden,
       count(*) FILTER (WHERE status = 'PENDING') AS pending,
       round(count(*) FILTER (WHERE status = 'REJECTED')::numeric
             / NULLIF(count(*) FILTER (WHERE status <> 'PENDING'), 0), 4) AS override_rate
FROM agent_proposals
GROUP BY action;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'report_reader') THEN
        CREATE ROLE report_reader NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO report_reader;
GRANT SELECT ON rpt_claims_daily, rpt_loss_ratio_by_segment, rpt_adjuster_cycle_time, rpt_fraud_funnel,
    rpt_fraud_by_segment, rpt_agent_proposals TO report_reader;
