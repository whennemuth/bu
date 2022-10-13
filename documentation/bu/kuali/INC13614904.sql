SET SERVEROUTPUT ON;
declare
    start_time date;
begin
    start_time := sysdate;
    
    execute immediate 'alter table KCOEUS.sponsor disable constraint FK_SPONSOR_ROLODEX_KRA';
    
    INSERT INTO sponsor (
        sponsor_code,
        rolodex_id,
        sponsor_name,
        dodac_number,
        acronym,
        owned_by_unit,
        sponsor_type_code,
        country_code,
        postal_code,
        state,
        ver_nbr,
        obj_id,
        create_user,
        update_user,
        update_timestamp
    )
        SELECT
            seq_sponsor_code.nextval,
            seq_rolodex_id.nextval,
            i.sponsor_name,
            i.dodac_number,
            i.acronym,
            '000001',
            t.sponsor_type_code,
            c.ALT_POSTAL_CNTRY_CD,
            i.sponsor_zip_code,
            i.sponsor_state,
            1,
            SYS_GUID(),
            'wrh',
            'wrh',
            start_time
        FROM
            sponsor_import i, sponsor_type t, KRLC_CNTRY_T c
        WHERE
            lower(rtrim(decode(i.sponsor_type_code, 'Foreign Foundation/Association/Society', 'Foundation/Association/Society', i.sponsor_type_code))) = lower(rtrim(t.description)) and
            NOT EXISTS (
                SELECT
                    NULL
                FROM
                    sponsor s
                WHERE
                    s.sponsor_name = i.sponsor_name
            ) and
            c.POSTAL_CNTRY_NM = i.sponsor_country (+);
            
        dbms_output.put_line('Sponsor rows added: ' || sql%rowcount);

        insert into rolodex (
            rolodex_id,
            organization,
            owned_by_unit,
            sponsor_address_flag,
            actv_ind,
            create_user,
            update_timestamp,
            update_user,
            ver_nbr,
            obj_id,
            address_line_1,
            address_line_2,
            city,
            state,
            postal_code,
            comments
        )
            select
                s.rolodex_id, 
                s.sponsor_name, 
                '000001', 
                'Y',
                'Y',
                'wrh', 
                start_time, 
                'wrh', 
                1, 
                SYS_GUID(),
                i.sponsor_address,
                i.sponsor_address_2,
                i.sponsor_city,
                i.sponsor_state,
                i.sponsor_zip_code,
                decode(i.comments, 'n/a', null, i.comments)
            from 
                sponsor s, sponsor_import i
            where 
                s.sponsor_name = i.sponsor_name and
                s.update_timestamp >= start_time;
                
        dbms_output.put_line('Rolodex rows added: ' || sql%rowcount);                
                
        execute immediate 'alter table KCOEUS.sponsor enable constraint FK_SPONSOR_ROLODEX_KRA';
    rollback;
--    commit;
end;
/
