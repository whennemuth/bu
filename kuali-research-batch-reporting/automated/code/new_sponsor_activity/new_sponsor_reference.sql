-- Award references for Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Award - Sponsor' as varchar2(50)) as "REFERENCE_TYPE",
  cast (a.award_number as varchar2(25)) as "NUMBER", 
  cast (a.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.awards_maxseq a 
  on a.sponsor_code = s.sponsor_code 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
  

union all 


-- Award references for Prime Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Award - Prime Sponsor' as varchar2(50)) as "REFERENCE_TYPE",
  cast (a.award_number as varchar2(25)) as "NUMBER", 
  cast (a.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.awards_maxseq a 
  on a.prime_sponsor_code = s.sponsor_code 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
  

union all 


-- IP references for Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('IP - Sponsor' as varchar2(50))    as "REFERENCE_TYPE",
  cast (p.proposal_number as varchar2(25)) as "NUMBER", 
  cast (p.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.latest_proposal p
  on p.sponsor_code = s.sponsor_code 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
  

union all 


-- IP references for Prime Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('IP - Prime Sponsor' as varchar2(50))     as "REFERENCE_TYPE",
  cast (p.proposal_number as varchar2(25)) as "NUMBER", 
  cast (p.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.latest_proposal p
  on p.prime_sponsor_code = s.sponsor_code 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
  
  
union all 


-- Subaward references for Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Subaward - Subrecipient' as varchar2(50)) as "REFERENCE_TYPE",
  cast (b.subaward_code as varchar2(25)) as "NUMBER", 
  cast (b.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join kcoeus.subaward b 
  on b.organization_id = s.sponsor_code 

inner join sapbwkcrm.subaward_maxseq x 
  on x.subaward_id = b.subaward_id  

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
 
  
union all 


-- Negotiation references for Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Negotiation/Other - Sponsor' as varchar2(50)) as "REFERENCE_TYPE",
  cast (n.negotiation_id as varchar2(25)) as "NUMBER", 
  cast (n.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join kcoeus.negotiation_unassoc_detail n
  on n.sponsor_code = s.sponsor_code 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
 
  
union all 


-- Negotiation references for Prime Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Negotiation/Other - Prime Sponsor' as varchar2(50)) as "REFERENCE_TYPE",
  cast (n.negotiation_id as varchar2(25)) as "NUMBER", 
  cast (n.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join kcoeus.negotiation_unassoc_detail n
  on n.prime_sponsor_code = s.sponsor_code 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
  
  
union all 

-- Award references for Sponsors referenced by Negotiation
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Negotiation/Award - Sponsor' as varchar2(50)) as "REFERENCE_TYPE",
  cast (n.negotiation_id || ' / ' || a.award_number as varchar2(25)) as "NUMBER", 
  cast (a.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.awards_maxseq a 
  on a.sponsor_code = s.sponsor_code 
  
inner join kcoeus.negotiation n 
  on n.associated_document_id = a.award_number 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1
 

union all 


-- Award references for Prime Sponsors referenced by Negotiation
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Negotiation/Award - Prime Sponsor' as varchar2(50)) as "REFERENCE_TYPE",
  cast (n.negotiation_id || ' / ' || a.award_number as varchar2(25)) as "NUMBER", 
  cast (a.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.awards_maxseq a 
  on a.prime_sponsor_code = s.sponsor_code 

inner join kcoeus.negotiation n 
  on n.associated_document_id = a.award_number 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1 


union all 


-- IP references for Sponsors referenced by Negotiation
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Negotiation/IP - Sponsor' as varchar2(50))    as "REFERENCE_TYPE",
  cast (n.negotiation_id || ' / ' || p.proposal_number as varchar2(25)) as "NUMBER", 
  cast (p.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.latest_proposal p
  on p.sponsor_code = s.sponsor_code 
  
inner join kcoeus.negotiation n 
  on n.associated_document_id = p.proposal_number 
  and n.negotiation_assc_type_id = 3  -- IP 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1


union all 


-- IP references for Prime Sponsors 
SELECT 
  s.sponsor_code,
  s.sponsor_name,
  s.update_timestamp,
  t.description                   as "SPONSOR_TYPE",
  cast ('Negotiation/IP - Prime Sponsor' as varchar2(50))     as "REFERENCE_TYPE",
  cast (n.negotiation_id || ' / ' || p.proposal_number as varchar2(25)) as "NUMBER", 
  cast (p.title as varchar2(200)) as "TITLE"

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code
 
inner join sapbwkcrm.latest_proposal p
  on p.prime_sponsor_code = s.sponsor_code 

inner join kcoeus.negotiation n 
  on n.associated_document_id = p.proposal_number 
  and n.negotiation_assc_type_id = 3  -- IP 

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1


order by 1, 5, 6
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;