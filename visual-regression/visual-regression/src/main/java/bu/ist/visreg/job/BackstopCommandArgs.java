package bu.ist.visreg.job;

import java.util.ArrayList;
import java.util.List;

import bu.ist.visreg.backstop.BackstopJson;

/**
 * This class take a job definition and determines all the arguments that would need to be supplied
 * to a node command in order to execute the job. Depending on what a job definition holds, there 
 * can be significant variation on what arguments to provide to node, significant enough to warrant 
 * breaking out the logic into a separate class (this class).
 * 
 * @author wrh
 *
 */
public class BackstopCommandArgs {
	
	private BackstopJson backstopJson;
	private List<String> createReferenceImagesArgs = new ArrayList<String>();
	private List<String> createTestImagesArgs = new ArrayList<String>();
	
	public BackstopCommandArgs(BackstopJson backstopJson) {
		this.backstopJson = backstopJson;
	}

	public BackstopJson getBackstopJson() {
		return backstopJson;
	}

	public String[] getCreateReferenceImagesArgs() {
		return (String[]) createReferenceImagesArgs.toArray();
	}

	public String[] getCreateTestImagesArgs() {
		return (String[]) createTestImagesArgs.toArray();
	}
	
}
