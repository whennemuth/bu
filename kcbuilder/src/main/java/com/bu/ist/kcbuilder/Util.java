package com.bu.ist.kcbuilder;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;

import com.bu.ist.kcbuilder.config.Config;

public class Util {
	public static Config getSampleConfig() {
		InputStream in = null;
		Config cfg = null;
		
		try {
			in = Util.class.getClassLoader().getResourceAsStream("sampleConfig.json");
			cfg = Config.getInstance(in);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(in != null) {
				try {
					in.close();
				} 
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return cfg;
	}
	
	public static PomFile getDefaultPomFile() {
		return new PomFile(getSampleConfig());
	}

	public static File getJarDirectory() throws Exception {
		String path = Util.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String decodedPath = URLDecoder.decode(path, "UTF-8");
		File f = new File(decodedPath);
		if(f.isFile() && f.getName().endsWith(".jar")) {
			return f.getParentFile();
		}
		return f;
    }

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
}
