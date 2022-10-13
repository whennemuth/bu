select
n.neg_id                     as "Negotiation ID",
n.neg_agree_desc             as "Negotiation Agreement Type",
n.neg_status_desc            as "Negotiation Status",
n.neg_full_name              as "Negotiator Name",
n.neg_start_date             as "Notification Date",
n.neg_end_date               as "Completion Date",
n.sponsor_name               as "Sponsor Name",
n.prime_sponsor_name         as "Prime Sponsor Name", 
n.pi_full_name               as "PI Name",
n.title                      as "Title",
a.activity_type_desc         as "Activity Type Desc",
a.description                as "Activity Desc",
a.location_desc              as "Location Desc",
a.start_date                 as "Activity Start Date",
a.end_date                   as "Activity End Date",
a.activity_age_days          as "Activity Age Days",
a.followup_date              as "Follow Up Date",
a.create_date                as "Create Date",
a.last_update_user           as "Last Update By",
a.last_update_date           as "Last Update Date",
n.grant_num                  as "Grant Num",
dept.unit_name               as "Funds Center Name",
n.lead_unit_num              as "Funds Center Num",
sch.unit_name                as "School",
n.pi_email_addr              as "PI Email",
a.restricted                 as "Restricted Ind"


from SAPBWKCRM.NEGOTIATION_COMPOSITE n

-- Get all Activities for negotiation. Only create report line for Activities in finalized Negotiations.
inner join SAPBWKCRM.NEGOTIATION_ACTIVITY a
  on a.neg_id = n.neg_id

-- Get Department Name
left join kcoeus.unit dept
  on dept.unit_number = n.lead_unit_num

-- Get School Name
left join kcoeus.unit sch
  on sch.unit_number = SUBSTR(n.lead_unit_num,1,3) || '0000000'

order by n.neg_id, a.activity_id;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;