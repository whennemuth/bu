-- Override default login.sql to force scripts to return number of records from SELECT. 
SET FEEDBACK ON;

-- Update "Pending" to "Not Funded" status if IP over 18 months old 

update KCOEUS.PROPOSAL
  set status_code = '3'
  
where status_code = '1' 
  and update_timestamp < add_months(sysdate, -18) -- last update is more than 18 months ago 
  and proposal_sequence_status = 'ACTIVE'
;

commit;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;