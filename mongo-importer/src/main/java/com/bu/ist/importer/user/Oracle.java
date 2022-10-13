package com.bu.ist.importer.user;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class Oracle {

	private static final String SQL = "select * from proposal_state";
	
	public static void main(String[] args) {
		List<Map<String, Object>> rows;
		
		@SuppressWarnings("deprecation")
		SingleConnectionDataSource datasrc = new SingleConnectionDataSource(
				"oracle.jdbc.driver.OracleDriver", 
				"jdbc:oracle:thin:@("
						+ "DESCRIPTION=("
						+ "ADDRESS_LIST=("
						+ "ADDRESS=(PROTOCOL=TCP)"
						+   "(HOST=buaws-kuali-db-ci001.bu.edu)"
						+   "(PORT=1521)))"
						+ "(CONNECT_DATA=(SERVICE_NAME=Kuali)))", 
				"KCOEUS", 
				"g8r9s5#8MUgv", 
				false);
		
		JdbcTemplate jdbc = new JdbcTemplate(datasrc);
		rows = jdbc.queryForList(SQL);
	}
}
