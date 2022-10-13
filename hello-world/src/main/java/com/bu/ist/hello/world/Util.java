package com.bu.ist.hello.world;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Util {

	/**
	 * Get the directory containing the jar file whose code is currently running.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static File getJarDirectory() throws Exception {
		String path = Util.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = URLDecoder.decode(path, "UTF-8");
		File f = new File(decodedPath);
		if(f.isFile() && f.getName().endsWith(".jar")) {
			return f.getParentFile();
		}
		return f;
    }

	/**
	 * Add content to the beginning of a file.
	 * 
	 * @param prepend
	 * @param f
	 */
	public static void prependFileContent(String prepend, File f) {
		PrintWriter pw = null;
		try {
			System.out.println(f.getAbsolutePath());
			String content = getFileContent(new FileInputStream(f));
			pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(f)));
			pw.write(prepend);
			pw.write(content);
			pw.flush();
		}
		catch(Exception e) {
			e.printStackTrace(); 
		}
		finally {
			if(pw != null) {
				pw.close();
			}
		}
	}

	public static boolean fileStartsWith(File f, String s) {
		if(f == null) {
			System.out.println("File provided is null!");
			return false;
		}
		if(s == null) {
			System.out.println("String provided is null!");
			return false;
		}		
		FileInputStream in;
		try {
			in = new FileInputStream(f);
		} 
		catch (FileNotFoundException e) {
			System.out.println("File not found: " + f.getAbsolutePath());
			return false;
		}
		String content = getFileContent(in);
		return content.startsWith(s);
	}
	
	/**
	 * Get the content of an inputstream as a string.
	 * @param in
	 * @return
	 */
	public static String getStringFromInputStream(InputStream in) {
		BufferedReader br = null;
		PrintWriter pw = null;
		try {
			br = new BufferedReader(new InputStreamReader(in));			
			String inputLine;
			StringWriter sb = new StringWriter();
			pw = new PrintWriter(new BufferedWriter(sb));
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
			if(pw != null) {
				pw.close();
			}
		}		
	}

	/**
	 * Get a byte array from an input stream.
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static byte[] inputStreamToByteArray(InputStream in) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(2^16);
		BufferedOutputStream buf = new BufferedOutputStream(bos);
		int next = in.read();
		while (next > -1) {
		    buf.write(next);
		    next = in.read();
		}
		bos.flush();
		return bos.toByteArray();
	}
	
	/**
	 * Get the content of an inputstream as a string.
	 * @param in
	 * @return
	 */
	public static List<String> getListFromInputStream(InputStream in) {
		BufferedReader br = null;
		List<String> list = new ArrayList<String>();
		try {
			br = new BufferedReader(new InputStreamReader(in));			
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				list.add(inputLine);
			}
			return list;
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
		InputStream in = Util.class.getClassLoader().getResourceAsStream(resource);
		return getStringFromInputStream(in);
	}
	
	/**
	 * Get the content of a file as a string.
	 * @param in
	 * @return
	 */
	public static String getFileContent(InputStream in) {
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
	
	public static String getFileContent(String filepath) {
		filepath = filepath.replaceAll("\\\\", "/");
		File f = new File(filepath);
		if(!f.isFile()) {
			System.out.println("ERROR! \"" + filepath + "\" does not exist!");
			return null;
		}
		return getFileContent(f);
	}
	
	public static String getFileContent(File f) {
		try {
			return getStringFromInputStream(new FileInputStream(f));
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static List<String> getFileContentAsList(File f) {
		try {
			return getListFromInputStream(new FileInputStream(f));
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
			return new ArrayList<String>();
		}
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
	
	public static String rowsToHTML(List<Map<String, Object>> rows) {
		StringBuilder s = new StringBuilder("<table cellpadding=3>\r\n");
		boolean firstLoop = true;
		for(Map<String, Object> row : rows) {
			if(firstLoop) {
				s.append("<tr>");
				for(String col : row.keySet()) {
					s.append("<th>").append(col).append("</th>");
				}
				s.append("</tr>");
				firstLoop = false;
			}
			s.append("<tr>");
			for(String col : row.keySet()) {
				String val = String.valueOf(row.get(col));
				s.append("<td>").append(val).append("</td>");
			}			
			s.append("</tr>");
		}
		s.append("</table>");
		return s.toString();
	}
	
	public static String concat(int end, String concat, String[] a) {
		return concat(0, end, concat, a);
	}
		
	public static String concat(int begin, int end, String concat, String[] a) {
		StringBuilder s = new StringBuilder();
		for(int i=begin; i<= end; i++) {
			s.append(a[i]);
			if(i < end) {
				s.append(concat);
			}
		}
		return s.toString();
	}
	
	public static boolean isEmpty(String s) {
		if(s == null)
			return true;
		if(s.isEmpty())
			return true;
		if(s.trim().isEmpty())
			return true;
		return false;
	}
	
	public static boolean hasValue(String s) {
		return !isEmpty(s);
	}
}
