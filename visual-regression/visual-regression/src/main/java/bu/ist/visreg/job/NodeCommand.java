package bu.ist.visreg.job;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NodeCommand {
	
	private String[] commandParts;
	
	public NodeCommand(String[] commandParts) {
		if(!commandParts[0].equals("node")) {
			// Make sure "node" is the first part of the command.
			this.commandParts = new String[commandParts.length+1];
			this.commandParts[0] = "node";
			for(int i=0; i<commandParts.length; i++) {
				this.commandParts[i+1] = commandParts[i];
			}
		}
		else {
			this.commandParts = commandParts;			
		}
	}

	public void run() throws Exception {		
		ProcessBuilder builder = new ProcessBuilder();
		if(isWindows()) {
			/**
			 * Windows command differ from other systems.
			 * A command to list out the contents of the current directory would look like this:
			 *    builder.command("sh", "-c", "ls");
			 * But on windows it would be:
			 *    builder.command("cmd.exe", "/c", "dir");
			 */
			System.out.println("Windows!!!");
		} 
		
		builder.command(commandParts);
		
		// builder.directory(new File(System.getProperty("user.home")));
		Process process = builder.start();
		
		StreamHandler streamHandler = new StreamHandler(
				process.getInputStream(), 
				process.getErrorStream());
		
		Thread thread = new Thread(streamHandler);
		thread.start();
		int exitCode = process.waitFor();
		thread.join();
		if(exitCode == 0) {
			System.out.println(streamHandler.getStdOutput());
		}
		else {
			System.out.println(streamHandler.getErrOutput());
		}		
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}
	
	/**
	 * Operates in a separate thread and reads the input streams (stdin and stdErr) from the java.lang.process in
	 * which the node command runs. 2 Getters expose those outputs.
	 * 
	 * @author wrh
	 *
	 */
	private static class StreamHandler implements Runnable {
	    private InputStream stdIn;
	    private InputStream stdErr;
	    private StringBuilder in = new StringBuilder();
	    private StringBuilder err = new StringBuilder();
	 
	    public StreamHandler(InputStream stdIn, InputStream stdErr) {
	        this.stdIn = stdIn;
	        this.stdErr = stdErr;
	    }
	 
	    @Override
	    public void run() {	    
	        new BufferedReader(new InputStreamReader(stdIn)).lines()
	          .forEach(s -> in.append(s).append("\n"));
	        new BufferedReader(new InputStreamReader(stdErr)).lines()
	          .forEach(s -> err.append(s).append("\n"));
	    }
	    
	    private String pruneOutput(String s) {
	    	final String extra = "undefined\n";
	    	if(s.endsWith(extra)) {
	    		return s.substring(0, s.length()-extra.length());
	    	}
	    	return s;
	    }
	    
	    public String getStdOutput() {
	    	return pruneOutput(in.toString());
	    }
	    public String getErrOutput() {
	    	return pruneOutput(err.toString());
	    }
	}
	
	public static void test(String...args) {
		for(String s : args) {
			System.out.println(s);
		}
	}
	public static void main(String[] args) throws Exception {
		if(args == null || args.length == 0) {
			args = new String[] { "node", "-pe", "console.log(\\\"THIS IS A TEST!\\\"" };
		}
		new NodeCommand(args).run();
	}
}
