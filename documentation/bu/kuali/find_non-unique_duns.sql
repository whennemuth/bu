-- Find duns codes from the sponsor table that appear more than once
select distinct s.* from SPONSOR_BKUP_220202 s, (
    select dun_and_bradstreet_number, count(sponsor_code)
    from SPONSOR_BKUP_220202
    where dun_and_bradstreet_number is not null
    group by dun_and_bradstreet_number
    having count(sponsor_code) > 1
) f,
sponsor_sam_mapping m
where s.dun_and_bradstreet_number = f.dun_and_bradstreet_number
and f.dun_and_bradstreet_number = m.ueiduns
order by s.dun_and_bradstreet_number;

-- Find duns codes from the mapping table that appear more than once with different uei codes (should be none).
select distinct m1.* from sponsor_sam_mapping m1, (
    select ueiduns, count(distinct ueisam)
    from sponsor_sam_mapping
    where ueiduns is not null
    group by ueiduns
    having count(distinct ueisam) > 1
) m2
where m1.ueiduns = m2.ueiduns
order by m1.ueiduns;

