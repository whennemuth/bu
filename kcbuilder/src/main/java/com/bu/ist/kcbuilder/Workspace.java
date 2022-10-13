package com.bu.ist.kcbuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import com.bu.ist.kcbuilder.config.Module;

/**
 * This class represents a workspace file system directory where the maven projects will be built.
 * That directory structure is created by this class.
 * 
 * @author hennemuthw
 *
 */
public class Workspace extends File {

	private static final long serialVersionUID = -8435365726643225763L;
	private PomFile pom;
	
	public Workspace(PomFile pom) {
		super(pom.getConfig().getWorkspacePath());
		this.pom = pom;
	}

	public boolean create() {
		boolean created = true;
		
		// Create the directory structure
		try {
			Set<Module> modules = pom.getSubModuleVersions().keySet();
			for (Iterator<Module> iterator = modules.iterator(); iterator.hasNext();) {
				Module module = (Module) iterator.next();
				File subdir = new File(this, module.getProjectDirectoryName());
				if(subdir.mkdirs())
					System.out.println("Creating module directory: " + subdir.getAbsolutePath());
				else
					System.out.println("Found module directory: " + subdir.getAbsolutePath());
				
				if(!iterator.hasNext()) {
					savePom(subdir);
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			created = false;
		}
		
		return created;
	}
	
	/**
	 * Save the pom file in the module directory that corresponds to the last in the build order.
	 * @param dir
	 * @throws FileNotFoundException
	 */
	private void savePom(File dir) throws FileNotFoundException {
		
		// Clear out any existing pom file.
		File file = new File(dir, "pom.xml");
		if(file.isFile()) {
			file.delete();
		}
		
		// Instantiate io objects
		FileOutputStream fs = new FileOutputStream(file);
		BufferedOutputStream bs = new BufferedOutputStream(fs);
		PrintWriter pw = null;
		
		// Write out the script file
		try {
			pw = new PrintWriter(bs);
			pw.write(pom.getContent());
			pw.flush();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(pw != null) {
				pw.close();
			}
		}
	}
	
	public static void main(String[] args) {
		Workspace workspace = null;
		try {
			PomFile pom = Util.getDefaultPomFile();
			workspace = new Workspace(pom);
			if(workspace.create()) {
				if(workspace.isDirectory()) {
					System.out.println(workspace.getAbsolutePath());
					for(File f :workspace.listFiles()) {
						System.out.print(f.isFile() ? "file: " : "directory: ");
						System.out.println(f.getAbsolutePath());
					}
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
