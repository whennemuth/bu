package bu.ist.visreg;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.BasketSystem.BasketType;
import bu.ist.visreg.util.FileUtils;

/**
 * This is an end-to-end test harness (not a unit test) for processing the contents of a file-based inbox.
 * 
 * @author wrh
 *
 */
public class ProcessorFileBased {
	
	private static final String ROOT_DIR = System.getProperty("java.io.tmpdir") + File.separator + "FileBaskets" + File.separator;
	private static final String DEFAULT_JSON_CLASSPATH = "job-definitions/JobDefinitionThreeRealScenarios.json";
	private static final String PREFIX = "test";
	private String jsonClasspath;
	private File workspace;
	private File inbox;
	
	public ProcessorFileBased() throws IOException {
		this(DEFAULT_JSON_CLASSPATH);
	}
	
	public ProcessorFileBased(String jsonClasspath) throws IOException {
		this.jsonClasspath = jsonClasspath;
		
		createWorkspace();
		
		createInbox();
		
		populateInbox();
	}
	
	private void createWorkspace() {
		File root = new File(ROOT_DIR);
		if( ! root.isDirectory()) {
			root.mkdirs();
		}
		File[] basketSystemDirs = root.listFiles(new FileFilter() {
			@Override public boolean accept(File f) {
				if( ! f.isDirectory()) return false;
				if( ! f.getName().matches("^" + PREFIX + "\\d+$")) return false;
				return true;
			}			
		});
		
		Arrays.sort(basketSystemDirs, new Comparator<File>() {
			@Override public int compare(File f1, File f2) {
				int i1 = Integer.valueOf(f1.getName().substring(PREFIX.length()));
				int i2 = Integer.valueOf(f2.getName().substring(PREFIX.length()));
				return i2 - i1;
			}			
		});
		
		int suffix = 1;
		if(basketSystemDirs.length > 0) {
			suffix = Integer.valueOf(basketSystemDirs[0].getName().substring(PREFIX.length())) + 1;
		}
		
		workspace = new File(root, PREFIX + String.valueOf(suffix));
		workspace.mkdir();
	}
	
	private void createInbox() {
		inbox = new File(workspace, Basket.BasketEnum.INBOX.getBasketRelativeLocation());
		if( ! inbox.isDirectory()) {
			inbox.mkdirs();
		}		
	}
	
	private void populateInbox() throws IOException {
		Path oldJsonPath = Paths.get(FileUtils.getClassPathResource(jsonClasspath).getAbsolutePath());
		Path newJsonPath = Paths.get(new File(inbox, oldJsonPath.toFile().getName()).getAbsolutePath());		
		Files.copy(oldJsonPath, newJsonPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
	}
	
	public void process() {
		Processor.main(new String[] {
				"--basket-type", BasketType.FILESYSTEM.name(),
				"--root", workspace.getAbsolutePath()
		});
	}

	public static void main(String[] args) throws IOException {
		/**
		 * RESUME NEXT: Complete any unfinished code above and test this out.
		 * Should invoke the actual backstop javascript snapshot comparisons.
		 * Try to get the login feature to work.
		 * Then move on with this test to an S3Bucket version of it.
		 * Then start thinking about making a cloud formation template.
		 */
		if(args == null || args.length == 0) {
			// Use the default backstopJson class path.
			new ProcessorFileBased().process();
		}
		else {
			// First argument will be a class path that locates the backstopJson to use.
			new ProcessorFileBased(args[0]).process();
		}
		
	}
}
