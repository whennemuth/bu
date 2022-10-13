package bu.ist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import bu.ist.visreg.util.JsonUtils;

public class DeepMergeTest {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	static class OuterClass {
		String id;
		String name;
		@JsonMerge InnerClass inner;
		public String getId() {
			return id;
		}
		public OuterClass setId(String id) {
			this.id = id;
			return this;
		}
		public String getName() {
			return name;
		}
		public OuterClass setName(String name) {
			this.name = name;
			return this;
		}
		public InnerClass getInner() {
			return inner;
		}
		public OuterClass setInner(InnerClass inner) {
			this.inner = inner;
			return this;
		}
		public String toJson() throws JsonProcessingException {
			return JsonUtils.toJson(this);
		}
	}
	
	@JsonInclude(JsonInclude.Include.NON_NULL)
	static class InnerClass {
		String id;
		String name;
		public String getId() {
			return id;
		}
		public InnerClass setId(String id) {
			this.id = id;
			return this;
		}
		public String getName() {
			return name;
		}
		public InnerClass setName(String name) {
			this.name = name;
			return this;
		}
	}
	
	public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
		OuterClass outer1 = new OuterClass()
				.setId("1")
				.setName("Fred")
				.setInner(new InnerClass()
						.setId("2")
						.setName("Barney"));

		OuterClass outer2 = new OuterClass()
				.setId("1")
				.setName("Frank")
				.setInner(new InnerClass()
						.setId("3")
//						.setName("Butch")
						);
		
		ObjectMapper objectMapper = new ObjectMapper(); 
		ObjectReader objectReader = objectMapper.readerForUpdating(outer1);
		outer1 = objectReader.readValue(outer2.toJson());
		
		System.out.println(outer1.toJson());
	}
}
