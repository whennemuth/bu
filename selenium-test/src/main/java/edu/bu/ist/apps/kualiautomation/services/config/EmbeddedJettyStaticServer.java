package edu.bu.ist.apps.kualiautomation.services.config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.bu.ist.apps.kualiautomation.util.Utils;

/**
 * This jetty server does not work off a web context.
 * Instead, content is issued by matching the last portion of the target url with a map of preconfigured output.
 * The target value is the key to the map and the values of the map are either raw html, or a classpath indicator
 * to a resource that contains html.
 * Useful for unit testing.
 * 
 * @author wrh
 *
 */
public class EmbeddedJettyStaticServer {

	private Server server;
	private Map<String, String> handlers = new HashMap<String, String>();
	
	public void start(Map<String, String> handlers) throws Exception {
		
		loadHandlers(null, handlers);
		
        server = new Server(8080);
        try {
        	server.setStopAtShutdown(true);
            addHandler();
			server.start();
			server.dumpStdErr();
			// Don't use join as it causes this thread to wait for the completion of the 
			// server thread before it advances to the next line of code.
			// server.join();
		} 
        catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadHandlers(String rootUrl, Map<String, String> handlers) {
		for(String subUrl : handlers.keySet()) {
			
			String resource = handlers.get(subUrl);
			
			if(resource == null) {
				resource = subUrl;
			}
			else if(resource.contains("<")) {
				// resource raw html, not a reference to a classpath resource that contains html
				this.handlers.put(subUrl, resource);
				continue;
			}

			String rootResource = concatenateUrl("html/", resource);
			
			String url = concatenateUrl(rootUrl, subUrl);

			File parent = Utils.getClassPathResource(rootResource);
			
			if(parent != null) {
				if(parent.isDirectory()) {
					Map<String, String> subhandlers = new HashMap<String, String>();
					for(File child : parent.listFiles()) {
						if(child.isDirectory()) {
							String _url = concatenateUrl(resource, child.getName());
							String _resource = _url;
							subhandlers.put(_url, _resource);
						}
						else {
							String _url = concatenateUrl(url, child.getName());
							String _resource = concatenateUrl(rootResource, child.getName());
							this.handlers.put(_url, _resource);
						}
					}
					if(!subhandlers.isEmpty()) {
						loadHandlers(rootUrl, subhandlers);
					}
				}
				else {
					this.handlers.put(url, rootResource);
				}
			}
		}
	}

	private String concatenateUrl(String part1, String...remainingParts) {
		StringBuilder s = new StringBuilder();
		if(part1 != null) {
			s.append(part1);
		}
		for(String part : remainingParts) {
			if(!s.toString().isEmpty() && !s.toString().endsWith("/")) {
				s.append("/");
			}
			s.append(part);
		}
		return s.toString().replaceAll("//", "/");
	}
	
	private void addHandler() {
        server.setHandler(new AbstractHandler(){
			@Override public void handle(
					String targetUrl, 
					Request baseRequest, 
					HttpServletRequest request, 
					HttpServletResponse response) throws IOException, ServletException {
				
				response.setContentType("text/html; charset=utf-8");
				response.setStatus(HttpServletResponse.SC_OK);
				String domain = "http://localhost:8080/";
				
				PrintWriter out = response.getWriter();
				String subUrl = targetUrl.startsWith(domain) ? 
						targetUrl.substring(domain.length()) : 
						targetUrl;
				String resource = tryHandlers(subUrl);
				
				if(resource == null) {
					System.out.println("No source found for: " + targetUrl);
				}
				else if(!resource.contains("<")) {
					File f = Utils.getClassPathResource(resource);
					if(f.isDirectory()) {
						System.out.println(f.getAbsolutePath());
					}
					// resource is not raw html, but a reference to a classpath resource that contains html
					resource = Utils.getClassPathResourceContent(resource);
					if(resource == null) {
						System.out.println("could not find: " + targetUrl);
					}
					else {
						System.out.println("found: " + targetUrl);
					}
				}
				
				if(targetUrl.endsWith(".css")) {
					response.setContentType("text/css; charset=UTF-8");
				}
				else if(targetUrl.endsWith(".js")) {
					response.setContentType("text/javascript; charset=UTF-8");
				}
				else if(targetUrl.endsWith(".gif")) {
					response.setContentType("image/gif");
				}
				else if(targetUrl.endsWith(".jpeg") || targetUrl.endsWith(".jpg")) {
					response.setContentType("image/jpeg");
				}
				else if(targetUrl.endsWith(".ico")) {
					response.setContentType("image/ico");
				}
				out.print(resource == null ? targetUrl : resource);
				out.flush();
				baseRequest.setHandled(true);
			}});
	}
	
	/**
	 * Try variations of url if handlers has no value for it when used as a key.
	 * The variations come from adding and removing "/" characters from the beginning and end of url.
	 * 
	 * @param url
	 * @return
	 */
	private String tryHandlers(String url) {
		String resource = handlers.get(url);

		if(resource == null && url.startsWith("/"))
			resource = handlers.get(url.substring(1));

		if(resource == null && url.endsWith("/"))
			resource = handlers.get(url.substring(url.length()-1));
		
		if(resource == null && url.startsWith("/") && url.endsWith("/"))
			resource = handlers.get(url.substring(1, url.length()-1));
		
		if(resource == null)
			resource = handlers.get("/" + url);
		
		if(resource == null)
			resource = handlers.get(url + "/");
		
		if(resource == null)
			resource = handlers.get("/" + url + "/");
			
		return resource;
	}
	
	public void stop() throws Exception {
		server.stop();
		server.destroy();
	}
}
