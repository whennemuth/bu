package edu.bu.ist.apps.kualiautomation.rest.resource;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.bu.ist.apps.kualiautomation.util.Utils;

public class ServiceResponse {

	private Object data;
	private String json;
	private String message;
	
	public  ServiceResponse() { } 
	
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public String getJson() {
		return json;
	}
	public void setJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
		} 
		catch (Exception e) {
			json = Utils.stackTraceToString(e);
		}
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public static ServiceResponse getInstance(Object data, String message) {
		ServiceResponse sr = new ServiceResponse();
		sr.data = data;
		sr.message = message;
		return sr;
	}
	public static ServiceResponse getInstance(Object data, Status status) {
		return getInstance(data, status.getReasonPhrase());
	}
	public static Response getResponse(Object data, String message, Status status) {
		ServiceResponse sr = getInstance(data, message);
		return Response.status(status).entity(sr).build();
	}
	public static Response getSuccessResponse(Object data) {
		return getResponse(data, Status.OK);
	}
	public static Response getSuccessResponse(Object data, String message) {
		return getResponse(data, message, Status.OK);
	}
	public static Response getResponse(Object data, Status status) {
		return getResponse(data, status.getReasonPhrase(), status);
	}
	public static Response getErrorResponse(String message) {
		return getResponse(null, message, Status.INTERNAL_SERVER_ERROR);
	}
	public static Response getInvalidResponse(String message, Status status) {
		return getResponse(null, message, status);
	}
	public static Response getExceptionResponse(Throwable e) {
		return getResponse(Utils.stackTraceToString(e), e.getMessage(), Status.INTERNAL_SERVER_ERROR);
	}
}
