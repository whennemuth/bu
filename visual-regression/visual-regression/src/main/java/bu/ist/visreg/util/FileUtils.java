package bu.ist.visreg.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtils {

	private File f;
	public FileUtils() {
		super();
	}
	public FileUtils(File f) {
		setFile(f);
	}
	public FileUtils(String pathname) {
		setFile(pathname);
	}
	public void setFile(File f) {
		this.f = f;
	}
	public void setFile(String pathname) {
		this.f = new File(pathname);
	}	
	public File getFile() {
		return f;
	}
	
	public String readFile() {
		if(f.isFile()) {
			try {
				byte[] bytes = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
				return new String(bytes);				
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public boolean writeFile(String s) {
		Path result;
		StandardOpenOption opt = f.exists() ? 
			StandardOpenOption.TRUNCATE_EXISTING : 
			StandardOpenOption.CREATE_NEW;		
		try {
			result = Files.write(Paths.get(f.getAbsolutePath()), s.getBytes(), opt);
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return result.toFile().isFile();
	}
	
	public FileUtils newInstance(File f) {
		return new FileUtils(f);
	}

	public boolean delete() {
		return f.delete();
	}
	public boolean isFile() {
		return f.isFile();
	}
	public String getName() {
		return f.getName();
	}
	public boolean renameTo(File targetFile) {
		return f.renameTo(targetFile);
	}
	public String getAbsolutePath() {
		return f.getAbsolutePath();
	}
	public File getParentFile() {
		return f.getParentFile();
	}
	public boolean isDirectory() {
		return f.isDirectory();
	}
	public boolean mkdirs() {
		return f.mkdirs();
	}
	public File[] listFiles() {
		return f.listFiles();
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
		InputStream in = TestUtils.class.getClassLoader().getResourceAsStream(resource);
		return getStringFromInputStream(in);
	}
	
	public static File getClassPathResource(String resource) {
		URL url = TestUtils.class.getClassLoader().getResource(resource);
		if(url == null) {
			return null;
		}
		return new File(url.getFile());
	}

}
