SELECT
    p.proposal_id,
    p.proposal_number,
    p.sequence_number,
    p.prime_sponsor_code,
    p.sponsor_code,
    pp.person_id AS pi,
    u.person_id AS da,
    p.lead_unit_number AS da_lead_unit,
    nvl(p.requested_start_date_total,p.requested_start_date_initial) AS requested_start_date,
    nvl(p.requested_end_date_total,p.requested_end_date_initial) AS requested_end_date,
    p.total_direct_cost_total,
    TO_DATE(c.value,'MM/dd/YYYY') AS submit_date,
    p.title
FROM
    kcoeus.proposal p,
    kcoeus.proposal_persons pp,
    kcoeus.proposal_custom_data c,
    kcoeus.unit_administrator u,
    huron_proposal_migration m
WHERE
    p.proposal_id = c.proposal_id
    AND   p.proposal_number = c.proposal_number
    AND   p.sequence_number = c.sequence_number
    AND   c.custom_attribute_id = 480 -- submit_date
    AND   c.value IS NOT NULL
    AND   TO_DATE(c.value,'MM/dd/YYYY') > m.last_run
    AND   p.proposal_id = pp.proposal_id
    AND   p.proposal_number = pp.proposal_number
    AND   p.sequence_number = pp.sequence_number
    AND   p.total_direct_cost_total IS NOT NULL
    AND   p.total_direct_cost_total > 0
    AND   p.status_code = 1 -- pending
    AND   p.lead_unit_number = u.unit_number (+)
    AND   p.proposal_sequence_status = 'ACTIVE'
    AND   pp.contact_role_code = 'PI';