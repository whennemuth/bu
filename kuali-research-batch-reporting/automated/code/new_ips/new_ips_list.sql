SELECT
  p.proposal_number                    as "IP Number",
  c.submitted_dt                       as "Submitted Date",
  k.full_name                          as "PI Name",
  case
    when k.rolodex_id is not null then ro.email_address 
    when k.person_id is not null  then em.email_addr 
    else '' 
  end                                  as "PI Email", 
  a.description                        as "Activity Type",
  s.sponsor_name                       as "Sponsor",
  p.title                              as "Proposal Title",
  case substr(p.lead_unit_number,1,1)
    when '1' then 'CRC'
    when '2' then 'MED'
    when '3' then 'NEIDL'
    else 'other'  
  end                                  as "Campus", 
  p.lead_unit_number                   as "Proposal Funds Center", 
  c.exp_cntl_na                        as "Export Control Not Applicable",
  c.prior_appr_pub                     as "Prior Approval for Publication",
  c.restrict_foreign                   as "Restrict Access Foreign",
  c.export_restrict                    as "Export Control Restrictions",
  c.export_blank_psf                   as "Export Control Blank on PSF",
  r.special_review_grp                 as "Special Review Type", 
-- suppress printing of any aggregate groups with no values in group.
  case 
    when r.protocol_cnt = 0 
    then ' ' 
    else r.protocol_grp 
  end                                  as "Protocol Number", 
  case 
    when r.approval_cnt = 0 
    then ' ' 
    else r.approval_grp 
  end                                  as "Approval Date", 
  case 
    when r.expiration_cnt = 0 
    then ' ' 
    else r.expiration_grp 
  end                                  as "Expiration Date", 
  p.requested_start_date_total         as "Req Start Date Total Period", 
  p.requested_end_date_total           as "Req End Date Total Period", 
  p.total_direct_cost_total            as "Total Direct Cost Total Period",
  p.total_indirect_cost_total          as "F+A Cost Total Period",
  p.total_direct_cost_total + 
  p.total_indirect_cost_total          as "Total All Cost Total Period"

from kcoeus.proposal p

inner join sapbwkcrm.latest_proposal_unique u  -- Return latest Proposal Sequence 
  on u.proposal_id = p.proposal_id

left join kcoeus.activity_type a
  on a.activity_type_code = p.activity_type_code
  
left join kcoeus.sponsor s
  on s.sponsor_code = p.sponsor_code
  
left join kcoeus.proposal_persons k 
  on k.proposal_id = p.proposal_id 
  and k.contact_role_code = 'PI' 
  
left join kcoeus.rolodex ro 
  on ro.rolodex_id = k.rolodex_id 
  
left join kcoeus.krim_entity_email_t em 
  on em.entity_id = k.person_id 
  and em.dflt_ind = 'Y'
  and em.actv_ind = 'Y' 
  
-- Group all Special Review Fields for IP. Counts identify number of values in group as we don't want to print empty groups.
left join 
    (   
	select 
	  proposal_id, 
	  listagg (special_review_desc, ', ')
	    within group (order by special_review_number) as "SPECIAL_REVIEW_GRP",

      count(protocol_number)  as "PROTOCOL_CNT", 
	  listagg (nvl(protocol_number,'#'), ', ')
	    within group (order by special_review_number) as "PROTOCOL_GRP",
 
      count(approval_date)    as "APPROVAL_CNT", 
	  listagg (nvl(to_char(approval_date, 'MM/DD/YYYY'),'#'), ', ')
	    within group (order by special_review_number) as "APPROVAL_GRP",
        
      count(expiration_date)  as "EXPIRATION_CNT", 
	  listagg (nvl(to_char(expiration_date, 'MM/DD/YYYY'),'#'), ', ')
	    within group (order by special_review_number) as "EXPIRATION_GRP"
        
	from 
	  (
	  select 
		s.proposal_id, 
		s.special_review_number, 
		s.special_review_code,
		d.description            as "SPECIAL_REVIEW_DESC",
		s.protocol_number, 
		s.approval_date, 
		s.expiration_date
		 
	  from kcoeus.proposal_special_review s 
	  left join kcoeus.special_review d
		on d.special_review_code = s.special_review_code 
	  )
	group by proposal_id 
	) r 
  on r.proposal_id = p.proposal_id	

-- Get Export Control Custom Fields 
left join 
    ( 
	select 
	  PROPOSAL_ID,
	  SAPBWKCRM.KCTODATE(SUBMITTED_DT,'MM/DD/YYYY') as SUBMITTED_DT,
	  case when EXP_CNTL_NA      = 'Yes' then 'Yes' else '' end as EXP_CNTL_NA,
	  case when PRIOR_APPR_PUB   = 'Yes' then 'Yes' else '' end as PRIOR_APPR_PUB,
	  case when RESTRICT_FOREIGN = 'Yes' then 'Yes' else '' end as RESTRICT_FOREIGN,
	  case when EXPORT_RESTRICT  = 'Yes' then 'Yes' else '' end as EXPORT_RESTRICT,
	  case when EXPORT_BLANK_PSF = 'Yes' then 'Yes' else '' end as EXPORT_BLANK_PSF
	from
		(
		select proposal_id,
			   value,
			   custom_attribute_id
		from kcoeus.proposal_custom_data
		)
	  pivot
		( max(value)
		  for custom_attribute_id in (480  as "SUBMITTED_DT",
		                              1500 as "EXP_CNTL_NA",
									  1510 as "PRIOR_APPR_PUB",
									  1520 as "RESTRICT_FOREIGN",
									  1530 as "EXPORT_RESTRICT",
									  1540 as "EXPORT_BLANK_PSF"
									  )            

		)  
    ) c
  on c.proposal_id = p.proposal_id

-- Parm &1 is month offset. Use 0 for current and -1 for prior month. Parms are set in script calling this SQL. 
where c.submitted_dt between TRUNC(add_months(sysdate,&1), 'MONTH') and  LAST_DAY(ADD_MONTHS(sysdate,&1))
 
order by c.submitted_dt, p.proposal_number
;