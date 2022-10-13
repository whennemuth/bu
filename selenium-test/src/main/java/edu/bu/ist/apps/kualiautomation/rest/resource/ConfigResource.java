package edu.bu.ist.apps.kualiautomation.rest.resource;

import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JFileChooser;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import edu.bu.ist.apps.kualiautomation.entity.Config;
import edu.bu.ist.apps.kualiautomation.entity.ConfigShortcut;
import edu.bu.ist.apps.kualiautomation.entity.util.BeanPopulator;
import edu.bu.ist.apps.kualiautomation.model.Directory;
import edu.bu.ist.apps.kualiautomation.services.config.ConfigService;
import edu.bu.ist.apps.kualiautomation.util.Utils;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource {
	
	@GET
	@Path("/browse/for/directory")
	public Response getDirectory() {
		// http://localhost:8080/rest/browse/for/directory
		Directory dir = null;
		final JFileChooser fc = new JFileChooser();
		try {
			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
			    File file = fc.getSelectedFile();
			    dir = new Directory(file);
			    return ServiceResponse.getSuccessResponse(dir);
			} 
			else {
			    dir = new Directory();
			    return ServiceResponse.getResponse(dir, Status.CONFLICT);
			}
		}
		catch (HeadlessException e) {
			dir = new Directory();
			dir.setError(Utils.stackTraceToString(e));
			e.printStackTrace();
			return ServiceResponse.getResponse(dir, Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GET
	@Path("/config")
	public Response getConfig() throws Exception {
		try {
			ConfigService svc = new ConfigService();
			/**
			 * null below means omit the user because we want the config for the first - and, because we 
			 * are ostensibly running from a jar and not a war - the only user.
			 */
			Config cfg = svc.getConfig(null);
			return ServiceResponse.getSuccessResponse(cfg);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return ServiceResponse.getExceptionResponse(e);
		}
	}
	
	@GET
	@Path("/config/{configId}")
	public Response getConfigById(@PathParam("configId") Integer configId) throws Exception {
		try {
			ConfigService svc = new ConfigService();
			Config cfg = svc.getConfigById(configId);
			return ServiceResponse.getSuccessResponse(cfg);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return ServiceResponse.getExceptionResponse(e);
		}
	}
	
	@GET
	@Path("/config/shortcut/empty/{configId}")
		public Response getEmptyShortcut(@PathParam("configId") Integer configId) throws Exception {
		ConfigShortcut shortcut = (new ConfigService()).getEmptyShortcut(configId);
		return ServiceResponse.getSuccessResponse(shortcut);
	}	
	
	@POST
	@Path("/config/save")
	public Response setConfig(Config cfg) {
		try {
			BeanPopulator.count = 0;
			ConfigService svc = new ConfigService();
			cfg = svc.saveConfig(cfg);
			return ServiceResponse.getSuccessResponse(cfg);
		} 
		catch (Exception e) {
			e.printStackTrace(System.out);
			return ServiceResponse.getExceptionResponse(e);
		}
	}
}
