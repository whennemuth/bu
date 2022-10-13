-- Override default login.sql to force scripts to return number of records from SELECT. 
SET FEEDBACK ON;

-- Look for Award records with multiple ACTIVE versions.
select 
a.award_number,
a.sequence_number,
a.award_id,
a.document_number, 
a.award_sequence_status, 
a.title, 
a.status_code, 
to_char(a.update_timestamp,'MM/DD/YYYY hh:mi:ssam')  as "Award Dt Time",
a.update_user as "Award Update User",
v.version_status as "Version History Status", 
to_char(v.update_timestamp,'MM/DD/YYYY hh:mi:ssam')  as "Version History Dt Time",
v.update_user as "Version History Update User"

from kcoeus.award a
left join kcoeus.version_history v
  on v.seq_owner_version_name_value = a.award_number 
  and v.seq_owner_seq_number = a.sequence_number 

-- identify Awards with multiple 'ACTIVE' entries 
inner join 
	(
	select
	    award_number,
	    count(award_number) 
	from kcoeus.award
	where award_sequence_status = 'ACTIVE'
    group by award_number 
	having count(award_number) > 1 
	) dup
  on dup.award_number = a.award_number 

order by a.award_number, a.sequence_number, a.award_id
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;