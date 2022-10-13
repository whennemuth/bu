SELECT 
  s.update_timestamp,
  t.description        as "SPONSOR_TYPE",
  s.sponsor_code,
  s.sponsor_name,
  s.acronym,
  r.address_line_1 || ' ' || r.address_line_2 || ' ' || r.address_line_3 AS "ADDRESS",
  r.city,
  r.state,
  r.country_code,
  c.postal_cntry_nm,
  r.postal_code,
  r.comments

from kcoeus.sponsor s

left join kcoeus.sponsor_type t
 on t.sponsor_type_code = s.sponsor_type_code

left join kcoeus.rolodex r 
  on r.rolodex_id = s.sponsor_code

left join kcoeus.krlc_cntry_t c
  on c.alt_postal_cntry_cd = r.country_code

-- For current week, return 1st day of week (Monday) then get prior 7 days of data
where s.update_timestamp >= trunc(sysdate,'IW') - 7  
  and s.update_timestamp <= trunc(sysdate,'IW') - 1

order by s.update_timestamp
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;