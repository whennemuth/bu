select
a.subaward_code       as "Subaward Number",
a.mod_number          as "Subaward Action",
a.mod_type            as "Modification Type",
a.loopback_ind        as "Status Loopback Ind",
s.subrecipient_name   as "Subrecipient Name",
s.subaward_title      as "Subaward Title",
s.frn                 as "FRN",
req.full_name         as "Requisitioner Name",
s.requisition_number  as "Requisition ID",
pi.full_name          as "Award PI",
camp.unit_name        as "Campus",
sch.unit_name         as "School",
dept.unit_name        as "Department",
s.award_number        as "Award Number",
ra.full_name          as "Subaward RA Name",
osp1.full_name        as "OSP 1 Name",
osp2.full_name        as "OSP 2 Name",
to_char(a.received_dt,'MM/DD/YY')
                      as "Receive Date",
to_char(a.executed_dt,'MM/DD/YY')
                      as "Executed Date",
stat.description      as "Status of Current Action",
a.current_status_days as "Days in Current Status",
a.execution_days      as "Aging",
a.s1_cycle_days       as "01. RA Review",
a.s2_cycle_days       as "02. Sub RA",
a.s3_cycle_days       as "03. FRN",
a.s4_cycle_days       as "04. PI/DA",
a.s5_cycle_days       as "05. Sub",
a.s6_cycle_days       as "06. Sub-Rev",
s.comments            as "Comments"

from sapbwkcrm.subaward_aging_composite a

left join sapbwkcrm.subaward_composite_maxseq s
  on s.subaward_code = a.subaward_code

-- Get Award PI Name
left join
	(
	select entity_id,
	       (
           case
           when last_nm   is null
	           then ''
           when middle_nm is null
	           then last_nm || ', ' || first_nm
           else
	           last_nm || ', ' || first_nm || ', ' || SUBSTR(middle_nm,1,1)
           end
           ) as full_name
    from kcoeus.krim_entity_nm_t
    where actv_ind = 'Y'
      and dflt_ind = 'Y'
	) pi
  on pi.entity_id = s.award_pi_person_id

-- Get Requisitioner Name
left join
	(
	select entity_id,
	       (
           case
           when last_nm   is null
	           then ''
           when middle_nm is null
	           then last_nm || ', ' || first_nm
           else
	           last_nm || ', ' || first_nm || ', ' || SUBSTR(middle_nm,1,1)
           end
           ) as full_name
    from kcoeus.krim_entity_nm_t
    where actv_ind = 'Y'
      and dflt_ind = 'Y'
	) req
  on req.entity_id = s.requisitioner_id

-- Get Department Name
left join kcoeus.unit dept
  on dept.unit_number = s.lead_unit_number

-- Get School Name
left join kcoeus.unit sch
  on sch.unit_number = SUBSTR(s.lead_unit_number,1,3) || '0000000'

-- Get Campus Name
left join kcoeus.unit camp
  on camp.unit_number = SUBSTR(s.lead_unit_number,1,1) || '000000000'

-- Get description for current status of Subaward
left join kcoeus.subaward_status stat
  on stat.subaward_status_code = a.last_status_code

-- get Subaward RA
left join
	(
	select entity_id,
	       (
           case
           when last_nm   is null
	           then ''
           when middle_nm is null
	           then last_nm || ', ' || first_nm
           else
	           last_nm || ', ' || first_nm || ', ' || SUBSTR(middle_nm,1,1)
           end
           ) as full_name
    from kcoeus.krim_entity_nm_t
    where actv_ind = 'Y'
      and dflt_ind = 'Y'
	) ra
  on ra.entity_id = s.requisitioner_id


-- get OSP1
left join
	(
	select entity_id,
	       (
           case
           when last_nm   is null
	           then ''
           when middle_nm is null
	           then last_nm || ', ' || first_nm
           else
	           last_nm || ', ' || first_nm || ', ' || SUBSTR(middle_nm,1,1)
           end
           ) as full_name
    from kcoeus.krim_entity_nm_t
    where actv_ind = 'Y'
      and dflt_ind = 'Y'
	) osp1
  on osp1.entity_id = s.osp_admin_id1


-- get OSP2
left join
	(
	select entity_id,
	       (
           case
           when last_nm   is null
	           then ''
           when middle_nm is null
	           then last_nm || ', ' || first_nm
           else
	           last_nm || ', ' || first_nm || ', ' || SUBSTR(middle_nm,1,1)
           end
           ) as full_name
    from kcoeus.krim_entity_nm_t
    where actv_ind = 'Y'
      and dflt_ind = 'Y'
	) osp2
  on osp2.entity_id = s.osp_admin_id2

order by TO_NUMBER(a.subaward_code), a.mod_sequence;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;