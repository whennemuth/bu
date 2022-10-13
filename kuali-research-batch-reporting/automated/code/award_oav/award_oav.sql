select  
a.award_number     as "Award",
a.title            as "Title",
a.sequence_number  as "Seq Num",
a.award_id         as "Award ID",
a.lead_unit_number as "Lead Unit Number",
a.document_number  as "Doc Num",
a.update_timestamp as "Update Timestamp",
a.status_code      as "Status Cd",
stat.description   as "Status Desc",
uni.unit_administrator_type_code as "Unit Admin Type Cd",
uds.description    as "Unit Admin Type Desc",
case
  when substr(a.award_number,8,5) = '00001' 
    then 'P'
    else 'C'
end                as "Parent or Child?",
uni.person_id      as "Unit Contact Person ID",
uni.full_name      as "Unit Contact Name",
prn.prncpl_nm      as "User Name",
eml.email_addr     as "Email Address" 

from kcoeus.award a

-- we want only current active Award
inner join  sapbwkcrm.AWARDS_MAXSEQ_UNIQUE maxseq
        ON a.award_id = maxseq.award_id

left join kcoeus.AWARD_UNIT_CONTACTS uni
	   on uni.award_id = a.award_id
       and uni.unit_administrator_type_code = '6' -- OAV (Other Access Viewer)

left join kcoeus.unit_administrator_type uds
       on uds.unit_administrator_type_code = uni.unit_administrator_type_code

left join kcoeus.award_status stat
       on a.status_code = stat.status_code
       
left join kcoeus.krim_prncpl_t prn 
       on prn.prncpl_id = uni.person_id
       
left join kcoeus.krim_entity_email_t eml
       on eml.entity_id = uni.person_id
       and eml.email_typ_cd = 'WRK'
       and eml.dflt_ind = 'Y'
       and eml.actv_ind = 'Y' 

where a.status_code <> '9' -- exclude cancelled
  and uni.unit_administrator_type_code = '6'  -- OAV (Other Access Viewer)
  
order by a.award_number, a.award_id, uni.person_id
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;