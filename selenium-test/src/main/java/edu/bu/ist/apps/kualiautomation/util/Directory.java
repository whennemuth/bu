package edu.bu.ist.apps.kualiautomation.util;

import java.io.File;

/**
 * Delete a directory and all its files and subdirectories with this class.
 * @author wrh
 *
 */
public class Directory {

	private File dir;
	
	public Directory(File dir) {
		this.dir = dir;
	}
	
	public boolean delete() {
		if(!dir.exists()) {
			System.out.println(dir.getAbsolutePath() + " does not exist - cannot delete!");
			return true;
		}
		return delete(dir);
	}
	
	private boolean delete(File file) {

	    File[] flist = null;

	    if(file == null){
	        return false;
	    }

	    if (file.isFile()) {
	    	System.out.println("deleting " + file.getAbsolutePath());
	        return file.delete();
	    }

	    if (!file.isDirectory()) {
	        return false;
	    }

	    flist = file.listFiles();
	    if (flist != null && flist.length > 0) {
	        for (File f : flist) {
		    	System.out.println("deleting " + f.getAbsolutePath());
	            if (!delete(f)) {
	            	System.out.println("failed to delete " + f.getAbsolutePath());
	                return false;
	            }
	        }
	    }

	    System.out.println("deleting " + file.getAbsolutePath());
	    return file.delete();
	}

	public static void main(String[] args) {
		File dir = new File("C:\\Users\\wrh\\Desktop\\webapp");
		new Directory(dir).delete();
	}
}
