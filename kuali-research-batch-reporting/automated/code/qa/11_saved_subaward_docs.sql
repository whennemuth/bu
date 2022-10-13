-- Title : QA Tools #11 - Saved Subawards
-- Desc  : Report of all Subawards in a saved state
-- By    : Dean Haywood, 01/26/15


select s.document_number 				as "Document Number",
--     s.subaward_id                    as "Subaward ID (Internal Key)",
       s.subaward_code 					as "Subaward ID",
       s.sequence_number 				as "Version",
       typ.description 					as "Subaward Type",
       s.purchase_order_num				as "FRN",
	   s.organization_id				as "Subrecipient ID",
	   org.organization_name			as "Subrecipient Name",
	   stat.description       			as "Subaward Status Desc",
	   cmp.unit_name              	  	as "Campus",
       s.Title         					as "Title",
       s.update_user                    as "Update User",
       doc.crte_dt 						as "Created on Date",
       prncpl.prncpl_nm 				as "Initiator",
       doc.doc_hdr_stat_cd	            as "Doc Hdr Status Cd",
       bw.award_number                  as "Award Number",
       bw.lead_unit_number              as "Award Fund Center Number",
       lu.unit_name                     as "Award Fund Center Name",
       bw.sponsor_award_number          as "Sponsor Award Number",
       pi.full_name                     as "Award PI Name"

from kcoeus.subaward s

left join kcoeus.award_type typ
  on typ.award_type_code = s.subaward_type_code

left join kcoeus.organization org
  on org.organization_id = s.organization_id

left join kcoeus.subaward_status stat
  on stat.subaward_status_code = s.status_code

left join kcoeus.unit cmp
  on cmp.unit_number = s.requisitioner_unit

inner join kcoeus.krew_doc_hdr_t doc
  on doc.doc_hdr_id = s.document_number

inner join kcoeus.krew_doc_typ_t doctyp
  on doctyp.doc_typ_id = doc.doc_typ_id

inner join kcoeus.krim_prncpl_t prncpl
  on prncpl.prncpl_id = doc.initr_prncpl_id

-- Get one funding source, in case Subaward is linked to more than one Award
left join
	(
	select subaward_id,
	       min(award_id) as award_id
    from kcoeus.subaward_funding_source
    group by subaward_id
	) fs
  on fs.subaward_id = s.subaward_id

-- Get current version of Award from SAP BW Award Maxseq. Award version may have changed from version originally associated with Subaward.
left join kcoeus.award a
  on a.award_id = fs.award_id
left join sapbwkcrm.awards_maxseq bw
  on bw.award_number = a.award_number

-- Get Award Lead Unit name
left join kcoeus.unit lu
  on lu.unit_number = bw.lead_unit_number

-- Get PI for the linked Award
left join kcoeus.award_persons pi
  on   pi.award_id = bw.award_id
   and pi.contact_role_code = 'PI'

where doctyp.doc_typ_nm = 'SubAwardDocument'
  and doc.doc_hdr_stat_cd = 'S'
  and s.status_code <> '10'

order by to_number(s.subaward_code)
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;