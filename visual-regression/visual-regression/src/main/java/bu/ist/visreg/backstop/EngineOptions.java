package bu.ist.visreg.backstop;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EngineOptions {
	String[] args = new String[] { "--no-sandbox" };
	public String[] getArgs() {
		return args;
	}
	public void setArgs(String[] args) {
		this.args = args;
	}		
}