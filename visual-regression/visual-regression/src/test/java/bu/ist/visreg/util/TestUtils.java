package bu.ist.visreg.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;

public class TestUtils {

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
	 * Get the directory containing the jar file whose code is currently running.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static File getRootDirectory() throws Exception {
		String path = TestUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = URLDecoder.decode(path, "UTF-8");
		File f = new File(decodedPath);
		if(f.isFile() && f.getName().endsWith(".jar")) {
			return f.getParentFile();
		}
		return f;
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
