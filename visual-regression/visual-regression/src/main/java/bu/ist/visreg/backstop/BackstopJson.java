package bu.ist.visreg.backstop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import bu.ist.visreg.job.JobDefinition;
import bu.ist.visreg.util.JsonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackstopJson {
	
	String id;
	@JsonMerge ViewPort[] viewports;
	String onBeforeScript;
	String onReadyScript;
	@JsonMerge Paths paths;
	String[] report;
	String engine;
	@JsonMerge EngineOptions engineOptions;
	Integer asyncCaptureLimit;
	Integer asyncCompareLimit;
	Boolean debug;
	Boolean debugWindow;
	Integer delay;
	Integer misMatchThreshold;
	@JsonMerge Scenario defaultScenario;
	@JsonMerge List<Scenario> scenarios;
	
	List<String> invalidMessages;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public ViewPort[] getViewports() {
		return viewports;
	}
	public void setViewports(ViewPort[] viewports) {
		this.viewports = viewports;
	}
	public String getOnBeforeScript() {
		return onBeforeScript;
	}
	public void setOnBeforeScript(String onBeforeScript) {
		this.onBeforeScript = onBeforeScript;
	}
	public String getOnReadyScript() {
		return onReadyScript;
	}
	public void setOnReadyScript(String onReadyScript) {
		this.onReadyScript = onReadyScript;
	}
	public Paths getPaths() {
		return paths;
	}
	public void setPaths(Paths paths) {
		this.paths = paths;
	}
	public String[] getReport() {
		return report;
	}
	public void setReport(String[] report) {
		this.report = report;
	}
	public String getEngine() {
		return engine;
	}
	public void setEngine(String engine) {
		this.engine = engine;
	}
	public EngineOptions getEngineOptions() {
		return engineOptions;
	}
	public void setEngineOptions(EngineOptions engineOptions) {
		this.engineOptions = engineOptions;
	}
	public Integer getAsyncCaptureLimit() {
		return asyncCaptureLimit;
	}
	public void setAsyncCaptureLimit(Integer asyncCaptureLimit) {
		this.asyncCaptureLimit = asyncCaptureLimit;
	}
	public Integer getAsyncCompareLimit() {
		return asyncCompareLimit;
	}
	public void setAsyncCompareLimit(Integer asyncCompareLimit) {
		this.asyncCompareLimit = asyncCompareLimit;
	}
	public Boolean isDebug() {
		return debug;
	}
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
	public Boolean isDebugWindow() {
		return debugWindow;
	}
	public void setDebugWindow(Boolean debugWindow) {
		this.debugWindow = debugWindow;
	}
	public Integer getDelay() {
		return delay;
	}
	public void setDelay(Integer delay) {
		this.delay = delay;
	}
	public Integer getMisMatchThreshold() {
		return misMatchThreshold;
	}
	public void setMisMatchThreshold(Integer misMatchThreshold) {
		this.misMatchThreshold = misMatchThreshold;
	}
	public Scenario getDefaultScenario() {
		return defaultScenario;
	}
	public void setDefaultScenario(Scenario defaultScenario) {
		this.defaultScenario = defaultScenario;
	}
	public List<Scenario> getScenarios() {
		return scenarios;
	}
	public void setScenarios(List<Scenario> scenarios) {
		this.scenarios = scenarios;
	}
	public BackstopJson clearScenarios() {
		scenarios = new ArrayList<Scenario>();
		return this;
	}
	public void addScenario(Scenario scenario) {
		if(scenario == null) return;
		if(scenarios == null) {
			scenarios = new ArrayList<Scenario>();
		}
		scenarios.add(scenario);
	}
	@JsonIgnore
	public String toJson() throws JsonProcessingException {
		return JsonUtils.toJson(this);
	}
	@JsonIgnore
	public boolean isValid() {
		if(invalidMessages == null) {
			getInvalidMessages();
			return isValid();
		}
		return invalidMessages.isEmpty();
	}
	@JsonIgnore
	public List<String> getInvalidMessages() {
		if(invalidMessages != null) {
			return invalidMessages;
		}
		invalidMessages = new ArrayList<String>();
		String id = this.id == null ? "[jobdef]" : this.id;
		if(viewports != null) {
			for(ViewPort viewport : this.viewports) {
				for(String msg : viewport.getInvalidMessages()) {
					invalidMessages.add(id + "." + msg);
				}
			}
		}
		if(this.scenarios != null) {
			for(Scenario scenario : this.scenarios) {
				for(String msg : scenario.getInvalidMessages()) {
					invalidMessages.add(id + "." + msg);
				}
			}
		}		
		return invalidMessages;
	}
	
	public static BackstopJson getInstance(String json) throws JsonMappingException, JsonProcessingException {
		BackstopJson backstopJson = null;
		if(json != null) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			backstopJson = mapper.readValue(json, JobDefinition.class);
		}
		return backstopJson;
	}
	
	public static BackstopJson getDefaultInstance() {
		BackstopJson bj = new BackstopJson();
		bj.setId("backstop_default_id_" + String.valueOf(System.currentTimeMillis()));
		bj.setViewports(new ViewPort[] {
			ViewPort.getDefaultInstance()
		});
		bj.setOnBeforeScript("onBefore.js");		
		bj.setOnReadyScript("puppet/onReady.js");
		bj.setPaths(new Paths());		
		bj.setReport(new String[] { "report" });		
		bj.setEngine("puppeteer");	
		bj.setEngineOptions(new EngineOptions());		
		bj.setAsyncCaptureLimit(3);		
		bj.setAsyncCompareLimit(1);		
		bj.setDebug(false);
		bj.setDebugWindow(false);
		bj.setDelay(0);
		bj.setMisMatchThreshold(0);
		bj.setDefaultScenario(Scenario.getDefaultInstance());
		return bj;
	}
	
	public static void main(String[] args) throws Exception, JsonMappingException, IOException {	
		JsonUtils.printJson(getDefaultInstance());
	}
}
