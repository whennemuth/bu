package edu.bu.ist.ci;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * This class represents all environment variables in the system.
 * 
 * @author wrh
 *
 */
public class BasicEnvironment implements Environment {

	Map<String, String> env = new HashMap<String, String>();
	
	public BasicEnvironment() {
		this(System.getenv());
	}
	
	public BasicEnvironment(Map<String, String> env) {
		this.env.putAll(env);
	}

	/* (non-Javadoc)
	 * @see edu.bu.ist.ci.Environment#getVariables()
	 */
	@Override
	public Map<String, String> getVariables() {
		return env;
	}

	/* (non-Javadoc)
	 * @see edu.bu.ist.ci.Environment#getVariable(java.lang.String)
	 */
	@Override
	public String getVariable(String name) {
		return getVariable(name, getVariables());
	}
	
	public String getVariable(String name, Map<String, String> vars) {
		if(name == null)
			return null;
		for(String key : vars.keySet()) {
			if(key.trim().equalsIgnoreCase(name.trim())) {
				return vars.get(key).trim();
			}
			else if(key.trim().replaceAll("_", "").equalsIgnoreCase(name.trim())) {
				return vars.get(key).trim();
			}
		}
		return null;		
	}
	
	public void printVariables() throws IOException {
		printVariables(env, System.out, false);
	}
	
	public static void printVariables(Map<String, String> env, OutputStream out, boolean close) throws IOException {
		
		PrintWriter pw = null;
		BufferedWriter bw = null;
		
		try {
			bw = new BufferedWriter(new OutputStreamWriter(out));
			pw = new PrintWriter(bw);
			TreeSet<String> sorted = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			sorted.addAll(env.keySet());
			for(String key : sorted) {
				pw.print(key);
				pw.print(" = ");
				pw.println(env.get(key));
			}
		}
		finally {
			if(bw != null) {
				bw.flush();
			}
			if(close && out != null) {
				out.close();
			}
		}
	}
}
