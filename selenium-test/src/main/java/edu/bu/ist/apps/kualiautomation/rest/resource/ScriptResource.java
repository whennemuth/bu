package edu.bu.ist.apps.kualiautomation.rest.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import edu.bu.ist.apps.kualiautomation.entity.Cycle;
import edu.bu.ist.apps.kualiautomation.entity.LabelAndValue;
import edu.bu.ist.apps.kualiautomation.services.automate.KerberosLoginParms;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.services.automate.locate.screenscrape.ScreenScrapeComparePattern;
import edu.bu.ist.apps.kualiautomation.services.config.ScriptService;
import edu.bu.ist.apps.kualiautomation.util.DateOffset;
import edu.bu.ist.apps.kualiautomation.util.DateOffset.DatePart;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScriptResource {

	@GET
	@Path("/cycle/empty/{userId}")
	public Response getEmptyCycle(@PathParam("userId") Integer userId) throws Exception {		
		Response response = ServiceResponse.getResponse((new ScriptService()).getEmptyCycle(userId), Status.OK);
		return response;		
	}

	@GET
	@Path("/cycle/element/types")
	public Response getElementTypes() throws Exception {		
		Response response = ServiceResponse.getResponse(ElementType.toJson(), Status.OK);
		return response;		
	}

	@GET
	@Path("/cycle/element/types/screenscrape")
	public Response getScreenScrapeTypes() throws Exception {		
		Response response = ServiceResponse.getResponse(ScreenScrapeComparePattern.toJson(), Status.OK);
		return response;		
	}
	
	@POST
	@Path("/cycle/element/date/format")
	public Response getDateFormat(LabelAndValue lv) {
		int dateUnits = Integer.valueOf(lv.getDateUnits());
		DatePart datePart = DatePart.valueOf(lv.getDatePart());
		DateOffset offset = DateOffset.valueOf(lv.getDateFormatChoice());
		String retval = null;
		if(DateOffset.CUSTOM.equals(offset)) {
			retval = offset.getOffsetDate(lv.getDateFormat(), datePart, dateUnits);
		}
		else {
			retval = offset.getOffsetDate(datePart, dateUnits);
		}
		Response response = ServiceResponse.getResponse(retval, Status.OK);
		return response;
	}
	
	@GET
	@Path("/cycle/element/date/format/choices")
	public Response getDateOffsetTypes() {
		Response response = ServiceResponse.getResponse(DateOffset.toJson(), Status.OK);
		return response;
	}
	
	@GET
	@Path("/cycle/element/date/part")
	public Response getDatePart() {
		Response response = ServiceResponse.getResponse(DatePart.toJson(), Status.OK);
		return response;
	}
	
	@GET
	@Path("/cycle/{cycleId}")
	public Response getCycle(@PathParam("cycleId") Integer cycleId) throws Exception {
		try {
			ScriptService svc = new ScriptService();
			Cycle cycle = svc.getCycle(cycleId);
			return ServiceResponse.getSuccessResponse(cycle);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return ServiceResponse.getExceptionResponse(e);
		}
	}
	
	@GET
	@Path("/cycles/{userId}")
	public Response getCycles(@PathParam("userId") Integer userId) throws Exception {
		try {
			ScriptService svc = new ScriptService();
			List<Cycle> cycles = svc.getCycles(userId);
			return ServiceResponse.getSuccessResponse(cycles);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return ServiceResponse.getExceptionResponse(e);
		}
	}

	@POST
	@Path("/cycle/save")
	public Response saveCycle(Cycle cycle) throws Exception {
		try {
			ScriptService svc = new ScriptService();
			List<Cycle> cycles = svc.saveCycle(cycle);			
			return ServiceResponse.getResponse(cycles, Status.OK);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return ServiceResponse.getExceptionResponse(e);
		}		
	}
	
	@DELETE
	@Path("/cycle/delete/{cycleId}")
	public Response deleteCycle(@PathParam("cycleId") Integer cycleId) throws Exception {
		try {
			ScriptService svc = new ScriptService();
			List<Cycle> cycles = svc.deleteCycle(cycleId);
			return ServiceResponse.getSuccessResponse(cycles);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return ServiceResponse.getExceptionResponse(e);
		}
	}
	
	@POST
	@Path("/cycle/launch/cycle")
	public Response launchCycle(
			KerberosLoginParms parms, 
			@QueryParam("cycleId") Integer cycleId, 
			@QueryParam("cfgId") Integer configId, 
			@QueryParam("terminate") boolean terminate) {
			//@DefaultValue("true") @QueryParam("terminate") boolean terminate) {
		try {
			ScriptService svc = new ScriptService();
			String message = svc.launchCycle(configId, parms, cycleId, terminate);
			return ServiceResponse.getSuccessResponse(null, message);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return ServiceResponse.getExceptionResponse(e);
		}
	}
	
}
