-- Parm value is month offset. use -1 for prior month. 
@new_ips_list.sql -1;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;