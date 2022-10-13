package com.bu.ist.importer.sql;

import java.util.ArrayList;
import java.util.List;

public class SelectStatement {

	private String sql;
	private List<String> cleanup = new ArrayList<String>();
	
	public SelectStatement(String sql) {
		this.sql = sql;
	}

	public static enum InsertPoints {
		INSERT_SELECT, INSERT_FROM, INSERT_WHERE;
	}

	public void addSelectField(String field) {
		insert(InsertPoints.INSERT_SELECT.toString(), field, ",");
	}

	public void addTable(String table) {
		insert(InsertPoints.INSERT_FROM.toString(), table, ",");
	}

	public void and(String condition) {
		insert(InsertPoints.INSERT_WHERE.toString(), condition, "AND");
	}
	
	public void or(String condition) {
		insert(InsertPoints.INSERT_WHERE.toString(), condition, "OR");
	}

	public void insert(String point, String replacement) {
		insert(point, replacement, null);
	}

	private void insert(String point, String replacement, String delimiter) {
		String replace = new String("${"+point+"}");
		if(delimiter == null) {
			sql = sql.replace(replace, replacement);
		}
		else {
			String d = new String(delimiter.trim());
			if(!",".equals(d)) {
				d = " " + d;
			}
			d = d + " ";
			cleanup.add(d + replace);
			sql = sql.replace(replace, (replacement + d + replace));
		}
	}
	
	public Object getSql() {
		String retval = new String(sql);
		// Delete all insertion points that were used for appending content where they appear against the delimiter used.
		for(String item : cleanup) {
			retval = retval.replace(item, "");
		}
		// Delete anything left over that matches any kind of insertion point.
		retval = retval.replaceAll("\\$\\{[^\\}]+\\}", "");
		return retval;
	}
}
