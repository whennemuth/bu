select
p.CRC_PROTOCOL_NUM         as "CRC Protocol Number",
p.PROTOCOL_NUMBER          as "Protocol Number",
c.IRB_RECV_DT              as "Date Received",
pi.PI_FULL_NAME            as "PI Name",
pi.PI_EMAIL_ADDRESS        as "PI Email Address",
pi.PI_AFFIL_TYP_DESC       as "PI Affiliation Type", 
p.TITLE                    as "Title",
p.PROTO_TYPE_DESC          as "Protocol Type",
p.PROTO_STAT_DESC          as "Protocol Status",
s.submission_type_desc     as "Submission Type",
s.submission_stat_desc     as "Submission Status",
s.event_type_desc          as "Event Type",
s.review_type_desc         as "Submission Review Type",
-- a.amend_type_desc          as "Amendment Type",
f.funding_grp              as "Funding Type, Num, Name",
u1.UNIT_NAME               as "Fund Center Name",
u2.unit_name               as "School Name",
c.IRB_ANALYST_NAME         as "Analyst Name",
c.IRB_CLAIM_DT             as "Claim Date",
c.IRB_DETERMINE_DT         as "Determination Date",
c.IRB_APPROVAL_DT          as "Approval Date",
c.IRB_NEW_EXP_DT           as "New Date of Expiration",
case
  when x.max_exp_dt is null
    then ' '
  when x.max_exp_dt < SYSDATE
    then 'Expired'
  when x.max_exp_dt - TRUNC(SYSDATE) < 30
    then to_char(x.max_exp_dt - TRUNC(SYSDATE))
  else ' '
end                        as "Expired or Expiring in 30 Days",
c.IRB_CLOSURE_DT           as "Closure Date",
nullif(d.WORKING_DAYS,0)   as "Working Days",
nullif(d.CALENDAR_DAYS,0)  as "Calendar Days",
nullif(d.IRB_DAYS,0)       as "IRB Days",
nullif(d.PI_DAYS,0)        as "PI Days",
p.ACTIVE_IND               as "Active Ind",
IRB_AUTH_DT                as "Authorization Date",
IRB_REQ_MOD_DT1            as "Req Mod Date 1",
IRB_RESPONSE_DT1           as "Response Date 1",
IRB_REQ_MOD_DT2            as "Req Mod Date 2",
IRB_RESPONSE_DT2           as "Response Date 2",
IRB_REQ_MOD_DT3            as "Req Mod Date 3",
IRB_RESPONSE_DT3           as "Response Date 3",
IRB_REQ_MOD_DT4            as "Req Mod Date 4",
IRB_RESPONSE_DT4           as "Response Date 4",
IRB_REQ_MOD_DT5            as "Req Mod Date 5",
IRB_RESPONSE_DT5           as "Response Date 5",
IRB_REQ_MOD_DT6            as "Req Mod Date 6",
IRB_RESPONSE_DT6           as "Response Date 6"

from sapbwkcrm.protocol p  -- we only want select Protocols

left join sapbwkcrm.protocol_pi pi
  on pi.protocol_id = p.protocol_id

left join kcoeus.unit u1
  on u1.unit_number = pi.FUND_CENTER_NUM

left join kcoeus.unit u2
  on u2.unit_number = pi.SCHOOL_NUM

left join sapbwkcrm.protocol_custom_data c
  on c.protocol_id = p.protocol_id

left join sapbwkcrm.protocol_days d
  on d.protocol_id = p.protocol_id

left join sapbwkcrm.protocol_expiration x
  on x.protocol_id = p.protocol_id
  
left join sapbwkcrm.protocol_funding_group f 
  on f.protocol_id = p.protocol_id

left join sapbwkcrm.protocol_submission s  -- can be multiples per Protocol ID
  on s.protocol_id = p.protocol_id

-- left join sapbwkcrm.protocol_amend a -- can be multiple per Protocol ID. 
--   on  a.protocol_id = p.protocol_id
--   and a.amend_type_cd <> '025'       -- ignore Protocol Permission amendment type. This is a generated entry.

-- order by p.protocol_base, s.submission_number, f.funding_type_cd
order by p.protocol_base, s.submission_number
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;