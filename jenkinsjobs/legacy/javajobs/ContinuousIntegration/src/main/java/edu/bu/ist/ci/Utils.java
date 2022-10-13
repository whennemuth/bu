package edu.bu.ist.ci;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {
	
	/**
	 * Get the content of a file as a string.
	 * @param in
	 * @return
	 */
	public static String getStringFromInputStream(InputStream in) {
		BufferedReader br = null;
		if(in == null) {
			return null;
		}
		try {
			br = new BufferedReader(new InputStreamReader(in));			
			String inputLine;
			StringWriter sb = new StringWriter();
			PrintWriter pw = new PrintWriter(new BufferedWriter(sb));
			while ((inputLine = br.readLine()) != null) {
				pw.println(inputLine);
			}
			pw.flush();
			return sb.toString();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		finally {
			if(br != null) {
				try {
					br.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}
	
	/**
	 * Get the directory containing the jar file whose code is currently running.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static File getRoot() throws Exception {
		String path = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = URLDecoder.decode(path, "UTF-8");
		File f = new File(decodedPath);
		if(f.isFile() && f.getName().endsWith(".jar")) {
			return f;
		}
		return f;
    }
	
	public static File getRootDirectory() throws Exception {
		File f = getRoot();
		if(f.isFile() && f.getName().endsWith(".jar")) {
			return f.getParentFile();
		}
		return f;
    }
	
	public static File getClassPathResource(String resource) {
		URL url = Utils.class.getClassLoader().getResource(resource);
		if(url == null) {
			return null;
		}
		return new File(url.getFile());
	}
	
	public static String getClassPathResourceContent(String resource) {
		InputStream in = Utils.class.getClassLoader().getResourceAsStream(resource);
		return getStringFromInputStream(in);
	}
	
	/**
	 * Simple interface for judging if two strings are "equivalent".
	 * 
	 * @author wrh
	 *
	 * @param <T>
	 */
	public static interface Equivalency<T> {
		public boolean areEquivalent(T s1, T s2);
	}
	
	/**
	 * We want to get the content of a file on the classpath. However, we want to accept a variations of the name of that file.
	 * If the variation is close enough (equivalent), then we want to return the content of the equivalently named file.
	 * 
	 * @param resourceName The ostensible name of the file
	 * @param rootPath The classpath location that contains the file.
	 * @param equivalency Implements the means to judge equivalency.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getClassPathResourceContent(String resourceName, String rootPath, Equivalency<String> equivalency) throws Exception {
	
		if(Utils.isEmpty(resourceName))
			return null;
		
		String path = (Utils.isEmpty(rootPath) ? "" : rootPath + "/") + resourceName;
		String content = getClassPathResourceContent(path);
		if(content == null) {
			List<String> items = getClassPathResourceList(rootPath);			
			for(String item : items) {
				String itemName = new String(item);
				if(itemName.contains("/")) {
					itemName = itemName.substring(itemName.lastIndexOf("/")+1);
				}
				if(equivalency.areEquivalent(resourceName, itemName)) {
					return getClassPathResourceContent(item);
				}
			}
		}
		
		return content;
	}

	/**
	 * Get a list of resources on the classpath relative to a particular classpath starting point.
	 * This means we have to access the classes dir (exploded app) or the read into the jar file
	 * since we are not naming any particular item, but want a list instead.
	 * 
	 * @param rootPath
	 * @return
	 * @throws Exception
	 */
	public static List<String> getClassPathResourceList(String rootPath) throws Exception {
		List<String> results = new ArrayList<String>();
		File root = Utils.getRoot();
		
		if(root == null) {
			System.out.println("Cannot get resources directory!");
		}
		else if(root.isFile() && root.getName().endsWith(".jar")) {
			Process p = null;
			try {
				String path = root.getAbsolutePath().replaceAll("\\\\", "/");
				System.out.println("Runtime.getRuntime().exec(\"jar tf " + path + "\", null, null);");
				p = Runtime.getRuntime().exec("jar tf " + path, null, null);
			
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            String line = "";			
				while ((line = reader.readLine())!= null) {
					if(line.startsWith(rootPath)) {
						if(!line.endsWith("/")) {
							results.add(line);
						}
					}
				}
				reader.close();
				p.destroy();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				if(p != null && p.isAlive()) {
					p.destroy();
				}
			}

		}
		else if(root.isDirectory()) {
			
			// A) Assume a target/classes folder exists per the standard maven build archtype.
			if(root.getName().equals("target")) {
				root = new File(root, "classes");
			}
			
			String path = new String(rootPath);
			if(!"/".equals(File.separator)) {
				// We're running in a windows environment
				path = rootPath.replaceAll("/", "\\\\");
			}

			if(root.isDirectory()) {
				File rcsdir = new File(root, path);
				if(rcsdir.isDirectory()) {
 					for(String f : rcsdir.list()) {
						results.add(rootPath + "/" + f);
					}
				}
			}
			// or...
			// B) Maybe this is a junit test, so assume a target/test-classes folder exists per the standard maven build archtype.
			if(results.isEmpty()) {
				root = new File(root.getParent(), "test-classes");
				File rcsdir = new File(root, path);
				if(rcsdir.isDirectory()) {
 					for(String f : rcsdir.list()) {
						results.add(rootPath + "/" + f);
					}
				}
			}
		}

		return results;		
	}
	
	public static boolean isEmpty(String value) {
		if(value == null)
			return true;
		return(value.trim().isEmpty());
	}

	public static String stackTraceToString(Throwable e) {
		if(e == null)
			return null;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.flush();
		String trace = sw.getBuffer().toString();
		return trace;
	}

	public static String trimLastSegment(String string, String delimiter) {
		if(isEmpty(string) || delimiter == null)
			return string;
		if(!string.contains(delimiter))
			return string;
		return string.substring(0, string.lastIndexOf(delimiter));
	}

	public static String getLastSegment(String string, String delimiter) {
		if(isEmpty(string) || delimiter == null)
			return string;
		if(!string.contains(delimiter))
			return string;
		return string.substring(string.lastIndexOf(delimiter) + 1);
	}
	
	public static String getJSON(Object obj) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} 
		catch (Exception e) {
			return Utils.stackTraceToString(e);
		}
	}

	public static <T> T getFromJSON(Class<T> clazz, String json) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		T obj = mapper.readValue(json, clazz);
		return obj;
	}
	
	public static String base64Decode(String encoded) {
		byte[] decoded = Base64.decodeBase64(encoded);
		return new String(decoded);
	}
	
	public static boolean isBase64(String encoded) {
//		if(encoded == null)
//			return false;
		return encoded.matches("[A-Za-z0-9+/=]+");
	}
}
