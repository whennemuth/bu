-- Override default login.sql to force scripts to return number of records from SELECT. 
SET FEEDBACK ON;

-- PENDING IP list

select	
	p.proposal_number     as "Proposal Num",
	p.sequence_number     as "Seq Num",
	p.proposal_id         as "Proposal ID",
	p.document_number     as "Document Num" ,
	p.proposal_sequence_status as "Seq Status", 
	d.doc_hdr_stat_cd     as "Doc Hdr Status",
	d.crte_dt             as "Doc Create Dt", 
	d.stat_mdfn_dt        as "Doc Stat Mod Date", 
	p.status_code         as "Proposal Status Cd", 
	ps.description        as "Proposal Status Desc", 
	p.title               as "Title",
	p.create_timestamp    as "IP Create Dt", 
	p.update_timestamp    as "Update Timestamp",
	p.update_user         as "Update User" 

from KCOEUS.PROPOSAL p

-- get Document Header 
left join KCOEUS.KREW_DOC_HDR_T d
  on d.doc_hdr_id = p.document_number
    
-- get Proposal Status Desc 
left join kcoeus.proposal_status ps
  on ps.proposal_status_code = p.status_code 
  
where p.status_code = '1' 
  and p.update_timestamp < add_months(sysdate, -18) -- last update is more than 18 months ago 
  and p.proposal_sequence_status = 'ACTIVE'

order by to_number(p.proposal_number), p.sequence_number, p.proposal_id 
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;