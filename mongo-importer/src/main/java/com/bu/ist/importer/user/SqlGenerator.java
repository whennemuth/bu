package com.bu.ist.importer.user;

public class SqlGenerator {
	
	private static final String SQL_SELECT_BASE = 
			"SELECT  \n" + 
			"  p.PRNCPL_ID AS \"schoolId\" \n" + 
			"  ,p.PRNCPL_NM AS \"username\" \n" + 
			"  ,p.ACTV_IND AS \"active\" \n" + 
			"  ,n.FIRST_NM AS \"firstName\" \n" + 
			"  ,n.LAST_NM AS \"lastName\" \n" + 
			"  ,n.LAST_NM || ', ' || n.FIRST_NM AS \"name\" \n" + 
			"  ,e.EMAIL_ADDR AS \"email\" \n" + 
			"  ,nvl(admins.role,'user') AS \"role\" \n";
	
	private static final String SQL_FROM_BASE =
			"FROM KRIM_PRNCPL_T p \n" + 
			"  ,KRIM_ENTITY_NM_T n \n" + 
			"  ,KRIM_ENTITY_EMAIL_T e \n" + 
			"  ,(SELECT p.PRNCPL_ID, p.PRNCPL_NM AS userid, 'admin' AS role \n" + 
			"      FROM KRIM_PERM_T perm \n" + 
			"          ,KRIM_ROLE_PERM_T rp \n" + 
			"          ,KRIM_ROLE_T r \n" + 
			"          ,KRIM_ROLE_MBR_T m \n" + 
			"          ,KRIM_PRNCPL_T p \n" + 
			"     WHERE perm.NM = 'Modify Entity' \n" + 
			"       AND perm.PERM_ID = rp.PERM_ID \n" + 
			"       AND r.ROLE_ID = rp.ROLE_ID \n" + 
			"       AND m.ROLE_ID = r.ROLE_ID \n" + 
			"       AND m.MBR_TYP_CD = 'P' \n" + 
			"       AND m.ACTV_TO_DT is null \n" + 
			"       AND m.MBR_ID = p.PRNCPL_ID) admins \n";
	
	private static final String SQL_WHERE_BASE = 
			"WHERE  \n" + 
			"  p.PRNCPL_ID = n.ENTITY_ID \n" + 
			"  AND n.DFLT_IND = 'Y' -- get default name \n" + 
			"  AND n.ACTV_IND = 'Y' -- active name \n" + 
			"  AND p.PRNCPL_ID = e.ENTITY_ID \n" + 
			"  AND e.DFLT_IND = 'Y' -- get default email \n" + 
			"  AND e.ACTV_IND = 'Y' -- active email \n" + 
			"  AND p.PRNCPL_ID = admins.PRNCPL_ID(+) \n";
	
	private static final String SELECT_ROLE_IDS_SQL = 
			"SELECT ROLE_ID \n" + 
			"  FROM KRIM_ROLE_PERM_T \n" + 
			"  WHERE PERM_ID in ( \n" + 
			"    SELECT PERM_ID \n" + 
			"    FROM KRIM_PERM_T \n" + 
			"    WHERE NM = 'Modify Entity' \n" + 
			"      AND NMSPC_CD = 'KR-IDM' \n" + 
			"  )";
	
	private static final String SELECT_PERMISSION_ASSIGNEES = 
			"SELECT * \n" + 
			"      FROM KRIM_ROLE_MBR_T \n" + 
			"      WHERE (MBR_TYP_CD = 'P' OR MBR_TYP_CD = 'G') \n" + 
			"        AND (ACTV_FRM_DT is null OR ACTV_FRM_DT <= TRUNC(SYSDATE)) \n" + 
			"        AND (ACTV_TO_DT is null OR ACTV_TO_DT >= TRUNC(SYSDATE)) \n" + 
			"        AND ROLE_ID in ( \n" + 
			"          #{SELECT_ROLE_IDS} \n" + 
			"        )";
	
	private static final String SELECT_GROUPS = 
			"SELECT GRP_NM AS \"name\" \n" + 
			"          ,GRP_DESC AS \"description\" \n" + 
			"          ,ACTV_IND AS \"active\" \n" + 
			"      FROM KRIM_GRP_T \n" + 
			"      WHERE ACTV_IND = 'Y'";
	
	private StringBuilder sql = new StringBuilder();
	private String[] groups = new String[] {};
	private String[] roles = new String[] {};
	private String principalName;
	private String principalId;
	private String email;
	
	public String[] getGroups() {
		return groups;
	}

	public void setGroups(String... groups) {
		this.groups = groups;
	}

	public String[] getRoles() {
		return roles;
	}

	public void setRoles(String... roles) {
		this.roles = roles;
	}
	
	

	public String getSql() {
		// 1) Form the field list
		sql = new StringBuilder(SQL_SELECT_BASE);
		if(groups.length > 0) {
			sql.append("KRIM_GRP_MBR_T gm")
			.append(" \n")
			.append("KRIM_GRP_T g").append(" \n");			
		}
		
		// 2) Form the table list
		
		// 3) Form the where clause
		if(groups.length > 0) {
			sql.append("  AND gm.GRP_ID = g.GRP_ID \n")
			.append("  AND gm.ACTV_TO_DT is null \n")
			.append("  AND gm.MBR_ID = p.PRNCPL_ID \n")
			.append("  AND g.GRP_NM in ( \n ");
			for(int i=0; i<groups.length; i++) {
				sql.append("    '" + groups[i] + "' \n");
			}
			sql.append("  ) \n");
		}

		return sql.toString();
	}
}
/*

  def select_kim_users_sql(groups = nil, &block)
    if groups == 'all'
      all_users_sql(&block)
    else
      group_members_sql(groups, &block)
      #group_active_members_sql(groups, &block)
    end
  end

  def all_users_sql(&block)
    generate_sql(&block)
  end

  # Returns users that belong to a group
  def group_members_sql(groups = [], &block)
    opt = {
      tbls: [
        'KRIM_GRP_MBR_T gm',
        'KRIM_GRP_T g',
      ],
      conds: [
        'gm.GRP_ID = g.GRP_ID',
        'gm.ACTV_TO_DT is null',
        'gm.MBR_ID = p.PRNCPL_ID',
      ]
    }
    unless groups.nil? || groups.empty?
      opt[:conds] << "g.GRP_NM in ('" + groups.join("', '") + "')"
    end
    generate_sql(opt, &block)
  end

  def select_kim_user_by_name_sql(name, groups = [])
    select_kim_users_sql(groups) do |opt|
      opt[:conds] << "p.PRNCPL_NM = '#{name}'"
    end
  end

  def select_kim_user_by_email_sql(email, groups = [])
    select_kim_users_sql(groups) do |opt|
      opt[:conds] << "e.EMAIL_ADDR = '#{email}'"
    end
  end

  def select_kim_user_by_id_sql(school_id, groups = [])
    select_kim_users_sql(groups) do |opt|
      opt[:conds] << "p.PRNCPL_ID = '#{school_id}'"
    end
  end

  def select_new_group_members_sql(groups, days = 1)
    group_members_sql(groups) do |opt|
      opt[:conds] << "gm.LAST_UPDT_DT > sysdate-#{days}"
    end
  end

  def select_permission_assignees_sql
    return SELECT_PERMISSION_ASSIGNEES
  end

  def select_groups_sql
    return SELECT_GROUPS
  end

  def select_role_ids_sql
    return SELECT_ROLE_IDS
  end

  private

  # opt: { selects, tbls, conds }
  def generate_sql(opt = {}, &block)
    adjust_sql_options(opt)
    yield opt if block_given?
    generate_select_phrase(opt[:selects]) +
    generate_from_phrase(opt[:tbls]) +
    generate_where_phrase(opt[:conds])
  end

  def adjust_sql_options(opt = {})
    opt[:selects] = [] unless opt[:selects]
    opt[:tbls] = [] unless opt[:tbls]
    opt[:conds] = [] unless opt[:conds]
    opt[:selects] = [ opt[:selects] ] if opt[:selects].is_a? String
    opt[:tbls] = [ opt[:tbls] ] if opt[:tbls].is_a? String
    opt[:conds] = [ opt[:conds] ] if opt[:conds].is_a? String
  end

  def generate_select_phrase(selects = nil)
    return SQL_SELECT_BASE
  end

  def generate_from_phrase(tbls = nil)
    sql = SQL_FROM_BASE
    padx = ' ' * 10
    if tbls && !tbls.empty?
      tbls.each do |t|
        sql += padx + ',' + t + "\n"
      end
    end
    sql
  end

  def generate_where_phrase(conds = nil)
    sql = SQL_WHERE_BASE
    pad7 = ' ' * 7
    if conds && !conds.empty?
      conds.each do |c|
        sql += pad7 + 'AND ' + c + "\n"
      end
    end
    sql
  end
end
*/