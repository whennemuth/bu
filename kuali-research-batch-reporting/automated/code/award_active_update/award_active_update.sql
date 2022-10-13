-- Override default login.sql to force scripts to return number of records from SELECT. 
SET FEEDBACK ON;

-- Look for Award records with multiple ACTIVE versions and ARCHIVE the older version. There should be only one ACTIVE version.

update kcoeus.award
set award_sequence_status = 'ARCHIVED'
where award_id in 
    (
    select 
        min_award_id 
    from 
        (
        select
            award_number,
	        min(award_id) as min_award_id, 
	        count(award_number) 
        from kcoeus.award
        where award_sequence_status = 'ACTIVE'
        group by award_number 
        having count(award_number) > 1
        ) 
    )
;

commit;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;