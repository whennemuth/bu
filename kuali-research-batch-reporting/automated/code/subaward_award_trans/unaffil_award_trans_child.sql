define exclude_users = '''jsmacher'', ''antcast'', ''eljamies'', ''cjscott'', ''mlmacd'', ''soeob'', ''vmcraige'', ''akush''';

select 
  c.award_number      as "Award Child", 
  substr(c.award_number,1,6) || '-00001'  as "Award Parent", 
  c.title             as "Title", 
  p.full_name         as "PI Name",
  f.award_number      as "Subaward Award Ref",
  t.transmission_date as "Award Transmission Date",
  case
    when n.middle_nm is null 
    then
      n.first_nm || ' ' || n.last_nm 
    else 
      n.first_nm || ' ' || n.middle_nm || ' ' || n.last_nm 
  end                 as "Transmitter Name"

-- Start from all Award versions as SAP transmission is over time and multiple Award versions may be involved
from kcoeus.award a

-- Award transmission may have occurred across various versions of an Award
left join kcoeus.award_transmission_child tc 
  on tc.award_id = a.award_id 
left join kcoeus.award_transmission t 
  on t.transmission_id = tc.transmission_id
  
-- Reference most current version of Award for reporting 
left join sapbwkcrm.awards_maxseq_unique u 
  on u.award_number = a.award_number 
  
left join kcoeus.award c 
  on c.award_id = u.award_id
  
-- Get Award PI 
left join kcoeus.award_persons p
  on  p.award_id = c. award_id 
  and p.contact_role_code = 'PI' 
  

-- Include Award ID (Award version) only if Budget Detail includes Subawards Under 25k or Subawards Over 25k
inner join 
  (
  SELECT 
	abx.award_id

  from  kcoeus.award_budget_ext abx
  
  inner join -- filter Award Budgets to include only latest Budget for each Award version
	(
	select 
	  award_id, 
	  max(budget_id)  as MAX_BUDGET_ID
	from kcoeus.award_budget_ext 
	group by award_id 
	) mbx 
	on  mbx.award_id = abx.award_id 
	and mbx.max_budget_id = abx.budget_id 

  left join kcoeus.budget_details det
	on det.budget_id = abx.budget_id 
  
  where abx.award_budget_status_code = '9' -- only posted budgets 
	and det.cost_element in ('19', '49')   -- only 'Subawards Under 25k' or 'Subawards Over 25k'

  group by abx.award_id  
  ) b
  on b.award_id = a.award_id 


-- Get all Subawards referencing this Award as a Funding Source. There could be none.
left join 
  (
  select 
    a.award_number 
    
    from kcoeus.award a 
    
    inner join kcoeus.subaward_funding_source fs 
      on fs.award_id = a.award_id 
  
    group by a.award_number

  ) f
  on f.award_number = c.award_number  

-- Get name of person Transmitting 
left join kcoeus.krim_entity_nm_t n 
  on n.entity_id = t.transmitter_id
  and n.dflt_ind = 'Y'
  and n.actv_ind = 'Y' 

where substr(a.award_number,8,5) <> '00001'  -- only Child Awards
  and f.award_number is null  -- Award is not affiliated with a Subaward  
  and (substr(a.award_number,1,6) not in ('204713', '205331', '206373', '206375', '207102'))  -- exclude CARB-X Awards
  and a.transaction_type_code in ('2', '3')  -- include New (2), Renewal (3) and skip Increment (4), Continuation (5), Rebudget (7), Supplement (8).
  and trunc(t.transmission_date) between trunc(sysdate -7) and trunc(sysdate -1)  -- prior 7 days transmissions
--  and trunc(t.transmission_date) between trunc(to_date('07/01/2020','MM/DD/YYYY')) and trunc(to_date('10/01/2020','MM/DD/YYYY'))  -- special date range 
  and t.transmitter_id not in 
    (
    select entity_id 
    from kcoeus.krim_prncpl_t 
    where prncpl_nm in (&exclude_users) -- parameter value passed to script
    )

order by c.award_number, t.transmission_date
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;