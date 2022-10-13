select
n.neg_id                     as "Negotiation ID",
n.neg_agree_desc             as "Negotiation Agreement Type",
n.neg_status_desc            as "Negotiation Status",
n.neg_full_name              as "Negotiator Name",
n.neg_start_date             as "Notification Date",
n.neg_end_date               as "Completion Date",
n.neg_age_days               as "Negotiation Age Days",
n.sponsor_name               as "Sponsor Name",
n.prime_sponsor_name         as "Prime Sponsor Name", 
n.pi_full_name               as "PI Name",
n.title                      as "Title",
n.lead_unit_num              as "Funds Center Num",
dept.unit_name               as "Funds Center Name",
n.pi_email_addr              as "PI Email",
n.grant_num                  as "Grant Num",
n.assoc_type_desc            as "Negotiation Assoc Type",
n.assoc_doc_id               as "Negotiation Assoc ID",
n.budget_approval            as "Budget Approval",
n.addgene_number             as "Addgene Num",
n.ct_reg_num                 as "Clinical Trials Reg Num",
n.msa_start_date             as "Master Agreement Start Dt",
n.msa_end_date               as "Master Agreement End Dt",
n.mta_exp_date               as "MTA Expiration Dt",
n.ind_ide                    as "IND/IDE",
sch.unit_name                as "School",

case
  when n.neg_start_date is null
  then null
  else '0' || TO_CHAR(n.neg_start_date, 'MM/YYYY')
end                          as "Notification Month Year",

case
  when n.neg_end_date is null
  then null
  else '0' || TO_CHAR(n.neg_end_date, 'MM/YYYY')
end                          as "Completion Month Year"

from SAPBWKCRM.NEGOTIATION_COMPOSITE n

-- Get Department Name
left join kcoeus.unit dept
  on dept.unit_number = n.lead_unit_num

-- Get School Name
left join kcoeus.unit sch
  on sch.unit_number = SUBSTR(n.lead_unit_num,1,3) || '0000000'

order by n.neg_id;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;