select 
  a.award_number           as "Award Number", 
  c.title                  as "Title", 
  a.award_id               as "Award ID", 
  a.sponsor_code           as "Sponsor Cd",
  s.sponsor_name           as "Sponsor Name", 
  a.transaction_type_code  as "Trans Type Cd", 
  d.description            as "Trans Type Desc", 
  a.basis_of_payment_code  as "Basis of Payment Cd",
  b.description            as "Basis of Payment Desc", 
  a.method_of_payment_code as "Method of Payment Cd", 
  m.description            as "Method of payment Desc", 
  t.transmission_date      as "Award Transmission Date",
  case
    when n.middle_nm is null 
    then
      n.first_nm || ' ' || n.last_nm 
    else 
      n.first_nm || ' ' || n.middle_nm || ' ' || n.last_nm 
  end                 as "Transmitter Name"

-- Start from all Award versions as SAP transmission is over time and multiple Award versions may be involved
from kcoeus.award a

-- Award transmission may have occurred across various versions of an Award
left join kcoeus.award_transmission t 
  on t.award_id = a.award_id 
  
-- Reference most current version of Award for reporting current Award Title 
left join sapbwkcrm.awards_maxseq_unique u 
  on u.award_number = a.award_number 
  
left join kcoeus.award c   -- get current Award Title
  on c.award_id = u.award_id

left join kcoeus.award_transaction_type d 
  on d.award_transaction_type_code = a.transaction_type_code
  
left join kcoeus.award_basis_of_payment b 
  on b.basis_of_payment_code = a.basis_of_payment_code
  
left join kcoeus.award_method_of_payment m 
  on m.method_of_payment_code = a.method_of_payment_code
  
left join kcoeus.sponsor s 
  on s.sponsor_code = a.sponsor_code 

-- Get name of person Transmitting 
left join kcoeus.krim_entity_nm_t n 
  on n.entity_id = t.transmitter_id
  and n.dflt_ind = 'Y'
  and n.actv_ind = 'Y' 

where substr(a.award_number,8,5) = '00001'  -- only Parent Awards 
  and a.transaction_type_code in ('2', '3')  -- include New or Renewal 
  and a.basis_of_payment_code = 1 -- Cost Reimbursement
  and a.method_of_payment_code = 28  -- Invoice 
  and trunc(t.transmission_date) BETWEEN trunc(ADD_MONTHS(LAST_DAY(SYSDATE),-2)+1) -- first day of prior month
                                     AND trunc(ADD_MONTHS(LAST_DAY(SYSDATE),-1))   -- last day of prior month
;

-- An EXIT statement must be present at end of each SQL script. 
-- It tells SQLcl subprocess launched by Python that its job is done.
-- If missing, SQLcl subprocess will wait for the next instruction, which never comes.   
EXIT;