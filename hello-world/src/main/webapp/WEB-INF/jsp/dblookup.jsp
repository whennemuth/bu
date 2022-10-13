<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ include file="header.jsp" %>
<c:out value="${db.comment}"></c:out>
<br>
<form action="hello.htm?task=lookup" method="post">
<textarea name="sql" cols=80 rows=10><c:out value="${db.defaultSql}" /></textarea>
</form>
<br>
<input type="button" value="submit" onclick="document.forms[0].submit();">
<br><br>
<c:if test="${!empty db.sql}">
	<div id="dbrows">
		<table cellpadding=3>
			<tr>
				<c:forEach var="col" items="${db.rows[0]}">
					<th><c:out value="${col.key}"/></th>
				</c:forEach>
			</tr>
			<c:forEach var="row" items="${db.rows}">
				<tr>
					<c:forEach var="entry" items="${row}">
						<td><c:out value="${entry.value}"/></td>
					</c:forEach>
				</tr>
			</c:forEach>
		</table>
	</div>
</c:if>
<%@ include file="footer.jsp" %>