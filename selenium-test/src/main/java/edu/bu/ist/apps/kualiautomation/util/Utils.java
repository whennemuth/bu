package edu.bu.ist.apps.kualiautomation.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Utils {

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
	
	public static boolean isEmpty(String val) {
		return (val == null || val.trim().length() == 0);
	}
	
	public static boolean isEmpty(Object val) {
		try {
			return (val == null || val.toString().length() == 0);
		} catch (Exception e) {
			return String.valueOf(val).length() == 0;
		}
	}
	
	public static boolean anyEmpty(String... vals) {
		for(int i=0; i<vals.length; i++) {
			if(isEmpty(vals[i])) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isNumeric(String val) {
		if(isEmpty(val))
			return false;
		return val.matches("\\d+");
	}

	/**
	 * Turn a bean into a Map
	 * @param bean
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> beanToMap(Object bean) throws Exception {
		Map<String, Object> map  = new HashMap<String, Object>();		
		Method[] methods = bean.getClass().getMethods();
		for(Method method : methods) {
			if(method.getName().startsWith("get")) {
				if(method.getParameterTypes().length == 0) {
					String fldName = ReflectionUtils.getFieldName(method.getName());
					Object fldVal = method.invoke(bean, (Object[]) null);
					map.put(fldName, fldVal);
				}
			}
		}
		return map;
	}
	
	/**
	 * Get the directory containing the jar file whose code is currently running.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static File getRootDirectory() throws Exception {
		String path = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = URLDecoder.decode(path, "UTF-8");
		File f = new File(decodedPath);
		if(f.isFile() && f.getName().endsWith(".jar")) {
			return f.getParentFile();
		}
		return f;
    }
	
	/**
	 * Get the content of a file as a string.
	 * @param in
	 * @return
	 */
	public static String getStringFromInputStream(InputStream in) {
		BufferedReader br = null;
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
	
	public static String getClassPathResourceContent(String resource) {
		InputStream in = Utils.class.getClassLoader().getResourceAsStream(resource);
		return getStringFromInputStream(in);
	}
	
	public static File getClassPathResource(String resource) {
		URL url = Utils.class.getClassLoader().getResource(resource);
		if(url == null) {
			return null;
		}
		return new File(url.getFile());
	}
	
	public static boolean trimIgnoreCaseEqual(String s1, String s2) {
		if(s1 == null || s2 == null)
			return false;
		return s1.trim().equalsIgnoreCase(s2.trim());
	}
	
	
	public static boolean trimIgnoreCaseUnemptyEqual(String s1, String s2) {
		if(s1 == null || s2 == null)
			return false;
		if(s1.trim().isEmpty())
			return false;
		if(s2.trim().isEmpty())
			return false;
		return s1.trim().equalsIgnoreCase(s2.trim());
	}
}
