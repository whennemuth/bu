package com.bu.ist.importer.sql;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SelectStatementTest {

	private SelectStatement stmt;
	
	@Test
	public void test() {
		stmt = new SelectStatement(
				"select ${INSERT_SELECT} \n"
				+ "from tbl1 a inner join tbl2 b \n"
				+ "${INSERT_FROM} \n"
				+ "where \n"
				+ "  a.fld1 = b.fld1 and \n"
				+ "  a.fld2 in (\n"
				+ "    ${INSERT_SUBQUERY} \n"
				+ "  ) ${INSERT_WHERE}"
        );
		
		stmt.addSelectField("newfld1");
		assertEquals("select newfld1 \n"
				+ "from tbl1 a inner join tbl2 b \n"
				+ " \n"
				+ "where \n"
				+ "  a.fld1 = b.fld1 and \n"
				+ "  a.fld2 in (\n"
				+ "     \n"
				+ "  ) ", stmt.getSql());
		
//		stmt.addSelectField("newfld2");
//		stmt.addTable("newtbl1");
//		stmt.insert(SelectStatement.InsertPoints.INSERT_FROM.toString(), "newtbl2");
//		stmt.and("this = that");
//		stmt.or("these = those");
//		stmt.insert("INSERT_SUBQUERY", "select c.fld2 from tbl3 c where c.fld3 = b.fld3");
	}

}
