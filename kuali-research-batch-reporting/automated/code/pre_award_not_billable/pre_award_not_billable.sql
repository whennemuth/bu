select  
a.award_number      as "Award / Grant Num",
a.title             as "Title",
a.sequence_number   as "Seq Num",
a.award_id          as "Award ID",
a.account_number    as "SP/IO Num", 
a.account_type_code as "Account Type Cd", 
t.description       as "Acount Type Desc", 
a.status_code       as "Child Status Cd",
stat.description    as "Child Status Desc",
pb.status_code      as "Parent Status Cd", 
pstat.description   as "Parent Status Desc", 
a.update_timestamp  as "Update Timestamp"

from kcoeus.award a

-- we want only current active Award
inner join  sapbwkcrm.AWARDS_MAXSEQ_UNIQUE maxseq
        on a.award_id = maxseq.award_id

inner join
  (
 select 
    substr(p.award_number,1,6)  as "PARENTBASE",
    p.status_code
  from kcoeus.award p
  inner join sapbwkcrm.AWARDS_MAXSEQ_UNIQUE mx
          on mx.award_id = p.award_id
  where substr(p.award_number,8,5) = '00001' -- include only Parant Awards 
    and p.status_code not in ('8', '9')  -- exclude closed and canceled Parent Awards
  ) pb
  on pb.parentbase = substr(a.award_number, 1,6)  -- child matches non-closed Parent Award

left join kcoeus.account_type t 
       on t.account_type_code = a.account_type_code

left join kcoeus.award_status stat
       on a.status_code = stat.status_code
       
left join kcoeus.award_status pstat
       on pb.status_code = pstat.status_code
       
where substr(a.award_number,8,5) <> '00001' -- exclude Parant Awards 
  and a.status_code = '2' -- include only Pre-Award Not Billable

order by a.award_number, a.award_id
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;