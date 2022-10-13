
with combined as (
    select distinct '1' as category, s.sponsor_code, s.sponsor_name, m.*
    from sponsor s, sponsor_sam_mapping m
    where s.DUN_AND_BRADSTREET_NUMBER = m.ueiduns
    union
    select '2' as category, m.* from sponsor_sam_mapping_by_name m
    union
    select '3' as category, m.* from sponsor_sam_mapping_by_names m
    union
    select '4' as category, m.* from sponsor_sam_mapping_punc m
),
condensed as (
    select distinct 
        c2.dups, 
        decode(c2.dups, 1, decode(c1.category, '1', 'Y', null), null) as accept, 
        c1.category, 
        c1.sponsor_code,
        c1.ueisam, 
        c1.ueiduns,
        c1.sponsor_name, 
        c1.LEGALBUSINESSNAME, 
        c1.dbaname,
        c1.entityurl,
        c1.entitydivisionname,
        c1.addressline1,
        c1.addressline2,
        c1.city,
        c1.stateorprovincecode,
        c1.zipcode,
        c1.zipcodeplus4,
        c1.countrycode,
        c1.entitystructuredesc,
        c1.entitytypedesc
    from combined c1, (
        select sponsor_code, count(ueisam) as dups from (
            select * from (
                select sponsor_code, ueisam, ueiduns, cagecode, 
                row_number() over (partition by sponsor_code, ueisam, ueiduns order by cagecode) rn
                from combined
            )
            where rn = 1
        )
        group by sponsor_code
    ) c2
    where 
        c1.sponsor_code = c2.sponsor_code
)

--    select * from condensed where dups = 1 and category = '1' order by sponsor_name;
--    select * from condensed where dups = 1 and category <> '1' order by category, sponsor_name;
    select * from condensed where dups > 1 order by dups, sponsor_code;