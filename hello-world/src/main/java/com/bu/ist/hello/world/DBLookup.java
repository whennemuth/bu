package com.bu.ist.hello.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.bu.ist.hello.world.HelloConfig.DBTYPE;

public class DBLookup {

	private HelloConfig cfg;
	private String sql;
	private List<Map<String, Object>> rows;
	
	@SuppressWarnings("unused")
	private DBLookup() { /* Restrict private constructor */ }
	
	public DBLookup(HelloConfig cfg) {
		this.cfg = cfg;
	}

	public String getComment() {
		return "Type some sql and click submit";
	}
	
	public String getLookup() {
		return getLookup(null);
	}
	
	public String getLookup(String sql) {
		return Util.rowsToHTML(getRows(sql));
	}
	
	public List<Map<String, Object>> getRows() {
		return getRows(null);
	}
	
	public List<Map<String, Object>> getRows(String sql) {
		if(rows != null)
			return rows;
		setRows(sql);
		return rows;
	}
	
	private void setRows(String sql) {
		if(sql != null)
			this.sql = sql;
		
		if(this.sql == null) {
			Map<String, Object> nothing = new HashMap<String, Object>();
			nothing.put("RESULT", "NO SQL PROVIDED!");
			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			list.add(nothing);
			rows = list;
			return;
		}
		
		String password = cfg.getProperty("datasource.password");
		if(Util.isEmpty(password))
			password = "";
		
		@SuppressWarnings("deprecation")
		SingleConnectionDataSource datasrc = new SingleConnectionDataSource(
				cfg.getProperty("datasource.driver.name"), 
				cfg.getProperty("datasource.url"), 
				cfg.getProperty("datasource.username"), 
				password, 
				false);
		
		JdbcTemplate jdbc = new JdbcTemplate(datasrc);
		rows = jdbc.queryForList(this.sql);
	}
	
	public void setSql(String sql) {
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}

	public String getDefaultSql() {
		return Util.isEmpty(sql) ? "select * from proposal_state" : sql;
	}

	public static void main(String[] args) {
		HelloConfig cfg = new HelloConfig("dev", DBTYPE.ORACLE);
		DBLookup db = new DBLookup(cfg);
		String html = db.getLookup("select * from proposal_state");
		System.out.println(html);
	}
}
