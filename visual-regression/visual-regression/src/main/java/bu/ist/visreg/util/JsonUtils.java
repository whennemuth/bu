package bu.ist.visreg.util;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtils {
	
	public static String toJson(Object obj) throws JsonProcessingException {
		return getObjectWriter().writeValueAsString(obj);
	}

	public static void toJsonFile(Object obj, File f) throws Exception, JsonMappingException, IOException {
		getObjectWriter().writeValue(f, obj);
	}
	
	public static void printJson(Object obj) throws JsonGenerationException, JsonMappingException, IOException {
		System.out.println(toJson(obj));
		// Cannot use this approach because the writer closes the printStream.
		// getObjectWriter().writeValue(System.out, obj);
	}
	
	private static ObjectWriter getObjectWriter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
		return mapper.writer(prettyPrinter);		
	}
}
