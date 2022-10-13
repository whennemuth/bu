package bu.ist.visreg.job;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;

import bu.ist.visreg.backstop.BackstopJson;
import bu.ist.visreg.backstop.Scenario;
import bu.ist.visreg.util.JsonUtils;

/**
 * A JobDefinition has two functions:
 * <ol><li>
 * A JobDefinition is basically an extension to a BackstopJson file providing an additional top level "inheritance" scenario for all
 * the scenarios in the scenarios collection to inherit properties from that they don't explicitly declare themselves. The resulting
 * scenario will in turn inherit from a predefined default scenario any properties left over that are still not explicitly declared.
 * </li>
 * <li>
 * Can break itself into multiple BackstopJson configurations such that each of those configurations has only one scenario in its
 * scenarios collection. The idea behind this is to be able to implement batching of scenarios at a higher level and not at the
 * configuration level. For example, a docker container can be run once for one any given scenario that came with the original job
 * definition independent of the other scenarios that accompanied.
 * </li></ol>
 *  
 * @author wrh
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobDefinition extends BackstopJson {

	private Scenario scenarioInheritance;
	private List<BackstopJson> invalidBackstops = new ArrayList<BackstopJson>();
	
	public static JobDefinition getInstance(String json) throws JsonMappingException, JsonProcessingException {
		JobDefinition def = null;
		if(json != null) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			def = mapper.readValue(json, JobDefinition.class);
		}
		return def;
	}
	
	/**
	 * Constructor with no implementation.
	 * Avoids {@link com.fasterxml.jackson.databind.exc.MismatchedInputException MismatchedInputException} 
	 * by offering a default constructor for deserialization.
	 */
	public JobDefinition() {
		return;
	}
	
	/**
	 * Construct multiple mini BackstopJson instances out of the scenarios from a single Backstopjson instance.
	 * This process includes complementing unspecified attributes in both the BackstopJson and Scenario instances with default values
	 * by getting a combined result from "merging" them into those default instances.
	 * For each scenario found in this JobDefinition:
	 * <ol><li>
	 *   Obtain a new BackstopJson instance by Merging this instance into the default instance of BackstopJson.
	 *   The new BackstopJson instance has only one Scenario. Complement this Scenario in the next steps.
	 * </li><li>
	 *   Start with a default Scenario and merge anything it can "inherit" from the inheritance scenario if one is found.
	 * </li><li>
	 *   Finalize the new single scenario as a new "merged" instance by merging it into the result of step 2.
	 * </li></ol>
	 * @return
	 * @throws Exception
	 */
	@JsonIgnore
	public List<BackstopJson> getBackstops() throws Exception {
		ArrayList<BackstopJson> backstops = new ArrayList<BackstopJson>();
		for(Scenario unmergedScenario : getScenarios()) {
			
			// 1) Perform the merging
			BackstopJson backstop = merge(BackstopJson.getDefaultInstance(), this.toJson());			
			Scenario mergedScenario = null;
			if(getScenarioInheritance() == null) {
				mergedScenario = merge(Scenario.getDefaultInstance(), unmergedScenario.toJson());
			}
			else {
				mergedScenario = merge(
					merge(Scenario.getDefaultInstance(), getScenarioInheritance().toJson()),
					unmergedScenario.toJson()
				);
			}
			
			// 2) Remove the default scenario.
			backstop.setDefaultScenario(null);
			
			// 3) Replace the scenarios collection with a single entry collection containing the merged scenario.
			backstop.clearScenarios().addScenario(mergedScenario);
			
			// 4) Add the resulting backstop json object to the collection.
			if(backstop.isValid()) {
				backstops.add(backstop);
			}
			else {
				invalidBackstops.add(backstop);
			}
		}
		return backstops;
	}
	
	private <T> T merge(T valueToUpdate, String updatingJson) throws JsonMappingException, JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader objectReader = objectMapper.readerForUpdating(valueToUpdate);
		return objectReader.readValue(updatingJson);
	}
	
	@JsonIgnore
	public List<BackstopJson> getInvalidBackstops() {
		return invalidBackstops;
	}

	public void setScenarioInheritance(Scenario scenarioInheritance) {
		this.scenarioInheritance = scenarioInheritance;
	}

	public Scenario getScenarioInheritance() {
		return scenarioInheritance;
	}

	@JsonIgnore
	public String toJson() throws JsonProcessingException {
		return JsonUtils.toJson(this);
	}
	
	public static void main(String[] args) throws Exception {
		// 1) Object to json: Set the properties of an "empty" JobDefinition instance and print out the json it produces.
		JobDefinition def = new JobDefinition();
		def.setId("MyJob");
		def.setScenarioInheritance(Scenario.getDefaultInstance().setLabel("Default-Label"));
		def.addScenario(new Scenario()
				.setLabel("label1")
				.setReferenceUrl("https://ref-domain.bu.edu/main.htm")
				.setUrl("https://ref-domain.bu.edu/main-test.htm")
				.setLoginUrl("https://ref-domain.bu.edu/login.htm"));		
		def.addScenario(new Scenario()
				.setLabel("label2")
				.setReferenceUrl("https://ref-domain.bu.edu/some/other/page.htm")
				.setUrl("https://ref-domain.bu.edu/some/other/page-test.htm"));		
		def.addScenario(new Scenario()
				.setLabel("label3")
				.setReferenceUrl("https://ref-domain.bu.edu/another/web/page.htm")
				.setUrl("https://ref-domain.bu.edu/another/web/page-test.htm"));		
		String json = def.toJson();
		System.out.println(json);
		System.out.println();
		
		// 2) Json to object: Use the json output above to feed into the factory method that produces an instance out of json.
		JobDefinition def2 = JobDefinition.getInstance(json);
		System.out.println(def2.toJson());
		
		int counter = 1;
		for(BackstopJson bs : def2.getBackstops()) {
			System.out.println();
			System.out.println("BACKSTOPJSON " + String.valueOf(counter++) + ":");
			JsonUtils.printJson(bs);
		}
	}
	
}
