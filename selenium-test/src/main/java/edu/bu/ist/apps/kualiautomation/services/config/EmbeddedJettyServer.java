package edu.bu.ist.apps.kualiautomation.services.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

import edu.bu.ist.apps.kualiautomation.util.Directory;
import edu.bu.ist.apps.kualiautomation.util.Utils;

public abstract class EmbeddedJettyServer {

	private static Server server;
	private static WebAppContext context;
	
	public EmbeddedJettyServer() {
		this(null);
	}
	
	public EmbeddedJettyServer(Integer port) {

		// Instantiate the web server and define what extra tasks it must do when it starts up.
    	server = new Server(port);
        server.addLifeCycleListener(new AbstractLifeCycleListener(){
			@Override
			public void lifeCycleStarted(LifeCycle event) {
				super.lifeCycleStarted(event);
				try {
					onStart();
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}});
        
        // Instantiate the web app context
        context = new WebAppContext();
        context.setContextPath("/");
        
        File jarfile = getJarFile();
        if(jarfile == null) {
        	String webappDirLocation = "src/main/resources/webapp/";
        	context.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
        	context.setResourceBase(webappDirLocation);
        	//
        	// or...
        	//
        	// context.setBaseResource(Resource.newClassPathResource("webapp"));
        	//
        	// However, if using this second method, javascript source files will not refresh even if you wipe out the browsers cache.
        	// This is probably because jetty is not properly changing header info to indicate the javascript source is newer and the 
        	// browser still has some remnant it can reuse (even though cache is cleared).
        }
        else {
        	File webapp = unpackWebAppDir();
        	context.setResourceBase(webapp.toURI().toString());
        }
 
        // Parent loader priority is a class loader setting that Jetty accepts.
        // By default Jetty will behave like most web containers in that it will
        // allow your application to replace non-server libraries that are part of the
        // container. Setting parent loader priority to true changes this behavior.
        // Read more here: http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
        context.setParentLoaderPriority(true);
        server.setHandler(context);
	}
	
	public File unpackWebAppDir() {
		
		File rootdir = null;
		File jarfile = null;
		FileInputStream fin = null;
		ZipInputStream zin = null;
		
		try {
			// 1) Find the jar file this process is running from and its parent directory (root directory).
			jarfile = EmbeddedJettyServer.getJarFile();
			rootdir = Utils.getRootDirectory();
			
			File webapp = new File("webapp");
			if(webapp.isDirectory()) {
				File skipbuild = new File("skipbuild");
				if(skipbuild.isFile()) {
					System.out.println("skipbuild file detected. Webapp directory rebuild cancelled.");
					return webapp;
				}
				
				// 2) Delete any existing webapp dir in the root directory
				new Directory(webapp).delete();
			}
			
			// 3) Recreate the webapp dir from extracting a copy of it from within the jar file
			fin = new FileInputStream(jarfile);
			zin = new ZipInputStream(fin);
			ZipEntry ze = null;
			
			while ((ze = zin.getNextEntry()) != null) {
				if(ze.getName().startsWith("webapp")) {
					System.out.println("Unzipping " + ze.getName());
					if(ze.isDirectory()) {
						String dirspec = rootdir.getAbsolutePath() + File.separator + ze.getName();
						new File(dirspec).mkdirs();
					}
					else {
						String filespec = rootdir.getAbsolutePath() + File.separator + ze.getName();
						FileOutputStream fout = null;
						try {
							fout = new FileOutputStream(filespec);
							for (int c = zin.read(); c != -1; c = zin.read()) {
								fout.write(c);
							}
							zin.closeEntry();
						}
						finally {
							if(fout != null)
								fout.close();
						}
					}
				}
			}
			zin.close();
		} 
		catch (Exception e) {
			if(rootdir == null)
				System.out.println("Cannot find webapp parent directory");
			else
				System.out.println("Problem creating webapp directory in: " + rootdir.getAbsolutePath());
			
			e.printStackTrace(System.out);
		}
		finally {
			try {
				if(fin != null)
					fin.close();
				if(zin != null)
					zin.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return new File("webapp");		
	}

	private void writeZipEntry(ZipEntry ze, ZipInputStream zin, File rootdir, String pathstart) throws Exception {
		if(ze.isDirectory()) {
			String dirspec = rootdir.getAbsolutePath() + File.separator + ze.getName();
			new File(dirspec).mkdirs();
		}
		else {
			String filespec = rootdir.getAbsolutePath() + File.separator + ze.getName();
			FileOutputStream fout = null;
			try {
				fout = new FileOutputStream(filespec);
				for (int c = zin.read(); c != -1; c = zin.read()) {
					fout.write(c);
				}
				zin.closeEntry();
			}
			finally {
				if(fout != null)
					fout.close();
			}
		}
	}
	
	public static File getJarFile() {
		File root = null;
		File f = null;
		try {
			root = Utils.getRootDirectory();
			File[] jars = root.listFiles(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					if(name.toLowerCase().contains("dependencies") && 
						name.toLowerCase().contains("selenium") &&
						name.endsWith(".jar")) 
							return true;
					return false;
				}
			});
			if(jars.length == 0) {
				System.out.println("Cannot find jar file at: " + root.getAbsolutePath());
			}
			else {
				f = jars[0];
			}
		} 
		catch (Exception e) {
			if(root == null)
				System.out.println("Cannot find jar directory");
			else if(f == null)
				System.out.println("Cannot find jar in directory: " + root.getAbsolutePath());
			else
				System.out.println("Problem loading jar file: " + f.getAbsolutePath());
			
			e.printStackTrace(System.out);
		}
		return f;
	}
	
	public void start() throws Exception {
        server.start();
        server.dumpStdErr();
        server.join();
  context.getServletContext().setInitParameter("cacheControl", "private, no-cache, no-store, proxy-revalidate, no-transform");
  context.getServletContext().setInitParameter("Pragma", "no-cache");
  context.getServletContext().setInitParameter("Expires", "0");
	}
	
	public void stop() throws Exception {
		server.stop();
		server.destroy();
	}
	
	public abstract void onStart() throws Exception;
	
	public static void main(String[] args) throws Exception {
		EmbeddedJettyServer jetty = new EmbeddedJettyServer(8080){
			@Override public void onStart() {
				System.out.println("Jetty starting!!!");
			}};
		jetty.start();
		//jetty.stop();
	}
}
