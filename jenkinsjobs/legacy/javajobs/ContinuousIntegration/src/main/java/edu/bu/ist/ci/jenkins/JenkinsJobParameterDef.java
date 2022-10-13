package edu.bu.ist.ci.jenkins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.bu.ist.ci.JobDef;
import edu.bu.ist.ci.JobParameterDef;
import edu.bu.ist.ci.Utils;
import edu.bu.ist.ci.Utils.Equivalency;

/**
 * Basic implementation of JobParameterDef. 
 * Instances are equal if their names are equal, ignoring case.
 * 
 * @author wrh
 *
 */
public class JenkinsJobParameterDef implements JobParameterDef {
	
	private JobDef jobDef;
	private String name;
	private boolean required;
	private String viewPathName;
	private String html;
	private String defaultClasspathResourceDirectory;
	
	public static final String INVALID_NULL_DEF_PARM = "JenkinsJobDef: Constructor jobDef arg cannot be null";
	public static final String INVALID_EMPTY_NAME_PARM = "JenkinsJobDef: Constructor name arg cannot be empty or null";
	public static final String INVALID_UNKNOWN_VIEW = "JenkinsJobDef: Constructor view arg does not indicate a classpath resource or file system path";
	public static final String CLASSPATH_HTML_RESOURCE_DIR = "edu/bu/ist/ci/jenkins/job/parameter/html";
	
	public JenkinsJobParameterDef(){}
	
	public JenkinsJobParameterDef(JobDef jobDef, String name, String viewPathName, boolean required) throws Exception {
		
		if(jobDef == null) {
			throw new IllegalArgumentException(INVALID_NULL_DEF_PARM);
		}
		if(Utils.isEmpty(name)) {
			throw new IllegalArgumentException(INVALID_EMPTY_NAME_PARM);
		}
		else if(name.contains(":")) {
			// You can squeeze the required property in with the name if you separate with a colon.
			this.name = name.substring(0, name.indexOf(":")).trim();
			String reqstr = name.substring(name.indexOf(":") + 1).trim();
			this.required = reqstr.matches("(?i)(yes)|(true)|(required)");
			if(Utils.isEmpty(this.name)) {
				throw new IllegalArgumentException(INVALID_EMPTY_NAME_PARM);
			}
		}
		else {
			this.name = name.trim();
			this.required = required;
		}
		
		this.jobDef = jobDef;
		this.viewPathName = viewPathName;
		
		if(!jobDef.getParameterDefs().contains(this)) {
			jobDef.addParameterDef(this);
		}
	}
	
	/**
	 * Set the html code that will constitute the UI for displaying the parameter field.
	 * 
	 * @throws Exception
	 */
	private void setHtml() throws Exception {
		if(Utils.isEmpty(viewPathName))
			return;
		
		/**
		 * Implements matching functionality where one string is compared to another.
		 * The names do not have to be equal, but can be "equivalent" if they pass the implemention boolean test.
		 */
		Equivalency<String> equivalency = new Equivalency<String>(){
			@Override public boolean areEquivalent(String str1, String str2) {
				if(str1 == null || str2 == null) 
					return false;
				String s1 = str1.toLowerCase().trim();
				String s2 = str2.toLowerCase().trim();
				
				if(s1.equals(s2))
					return true;
				
				if(s1.contains("/"))
					s1 = Utils.getLastSegment(s1, "/");
				
				if(s2.contains("/"))
					s2 = Utils.getLastSegment(s2, "/");
				
				if(s1.equals(s2))
					return true;
				
				if(s1.endsWith(".htm") || s1.endsWith(".html") || s1.endsWith(".txt") || s1.endsWith(".cfg")) 
					s1 = Utils.trimLastSegment(s1, ".");
				
				if(s2.endsWith(".htm") || s2.endsWith(".html") || s2.endsWith(".txt") || s2.endsWith(".cfg")) 
					s2 =  Utils.trimLastSegment(s2, ".");
				
				return s1.equals(s2);
			}};

			// 1) Search the classpath at the specified location.
			String path = Utils.trimLastSegment(viewPathName, "/");
			if(path.equals(viewPathName)) {
				path = getDefaultClasspathResourceDirectory();
			}
			html = Utils.getClassPathResourceContent(
					Utils.getLastSegment(viewPathName, "/"), 
					path, 
					equivalency);

			if(html != null) {
				html = html.trim();
				return;
			}
			
			// 2) Search the classpath at the default location using a default name. 
			html = Utils.getClassPathResourceContent(
					getDefaultClasspathResourceDirectory() + "/" + this.name, 
					getDefaultClasspathResourceDirectory(), 
					equivalency);
			
			if(html != null) {
				html = html.trim();
				return;
			}
			
			// 3) Assume viewPathName is a file or directory and search accordingly.
			File htmlFile = new File(viewPathName);
			if(htmlFile.isFile()) {
				html = Utils.getStringFromInputStream(new FileInputStream(htmlFile));
			}
			else if(htmlFile.isDirectory()) {
				for(File f : htmlFile.listFiles()) {
					if(equivalency.areEquivalent(this.name, f.getName())) {
						html = Utils.getStringFromInputStream(new FileInputStream(f));
					}
				}
			}
			
			if(html != null)
				html = html.trim();
	}
	
	public JenkinsJobParameterDef(JobDef jobDef, String name, String viewPathName) throws Exception {
		this(jobDef, name, viewPathName, false);
	}
	
	public JenkinsJobParameterDef(JobDef jobDef, String name) throws Exception {
		this(jobDef, name, null, false);
	}

	@Override
	@JsonSerialize(using=JobDefSerializer.class)
	@JsonDeserialize(using=JobDefDeserializer.class, as=JenkinsJobDef.class)
	public JobDef getJobDef() {
		return jobDef;
	}
	public void setJobDef(JobDef jobDef) {
		this.jobDef = jobDef;
	}
	@Override
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public boolean isRequired() {
		return required;
	}
	public void setRequired(boolean required) {
		this.required = required;
	}
	@Override
	public String getViewPathName() {
		return viewPathName;
	}
	public void setViewPathName(String viewPathName) throws Exception {
		this.viewPathName = viewPathName;
	}
	@Override
	public String getView(String...parms) {
		if(Utils.isEmpty(html)) {
			try {
				setHtml();
			} 
			catch (Exception e) {
				e.printStackTrace(System.out);
				return "<textarea cols=20 rows=50>" + Utils.stackTraceToString(e) + "</textarea>";
			}
		}
		return html;
	}	
	public void setView(String view) {
		this.html = view;
	}
	@JsonIgnore
	public String getDefaultClasspathResourceDirectory() {
		return defaultClasspathResourceDirectory == null ? CLASSPATH_HTML_RESOURCE_DIR : defaultClasspathResourceDirectory;
	}

	public void setDefaultClasspathResourceDirectory(String dir) throws Exception {	
		String oldValue = null;
		if(defaultClasspathResourceDirectory != null) {
			oldValue = new String(defaultClasspathResourceDirectory);
		}
		String s = null;
		if(dir != null) {
			s = new String(dir);
		}
		if(!Utils.isEmpty(s)) {
			s = s.trim();
			if(s.endsWith("/")) {
				s = s.substring(0, s.length() - 1);
			}
		}
		defaultClasspathResourceDirectory = s;
		if(s != null && !s.equalsIgnoreCase(oldValue)) {
			setHtml();
		}
	}
	@Override
	public String toJSON() {
		return Utils.getJSON(this);
	}	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JenkinsJobParameterDef other = (JenkinsJobParameterDef) obj;
		if (jobDef == null || jobDef.getClassName() == null || other.jobDef == null || other.jobDef.getClassName() == null) {
			return false;
		} else if (!jobDef.getClassName().trim().equalsIgnoreCase(other.jobDef.getClassName().trim())) 
			return false;
		if (name == null || other.name == null) {
			return false;
		} else if (!name.trim().equalsIgnoreCase(other.getName().trim()))
			return false;
		return true;
	}
		
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String viewAbbr = null;
		if(!Utils.isEmpty(html)) {
			viewAbbr = new String(html.trim().replaceAll("[\\r\\n\\f]+", ""));
			if(viewAbbr.length() > 20) {
				viewAbbr = viewAbbr.substring(0, 17) + "...";
			}
		}
		builder.append("JenkinsJobParameterDef [jobDef=").append(jobDef).append(", name=").append(name)
				.append(", required=").append(required).append(", viewPathName=").append(viewPathName)
				.append(", getView()=").append(viewAbbr).append("]");
		return builder.toString();
	}
	
	
	
	/**
	 * Without custom serialization, the standard serialization would throw a stack overflow error
	 * due to the reflexive reference between JobDef and JobParameterDef.
	 * 
	 * @author wrh
	 *
	 */
	public static class JobDefSerializer extends JsonSerializer<JobDef> {
		@Override public void serialize(
				JobDef def, 
				JsonGenerator generator, 
				SerializerProvider provider) throws IOException, JsonProcessingException {
			
			generator.writeStartObject();
			generator.writeStringField("className", def.getClassName());
			generator.writeArrayFieldStart("parameterDefs");
			for(JobParameterDef jpdef : def.getParameterDefs()) {
				generator.writeString(jpdef.getName());
			}
			generator.writeEndArray();
			generator.writeEndObject();
		}
	}
	
	public static class JobDefDeserializer extends JsonDeserializer<JobDef> {
		@Override public JobDef deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			JsonNode node = jp.getCodec().readTree(jp);
			String name = node.get("className").asText();
			List<JsonNode> jpdefs = node.findValues("parameterDefs");
			JobDef jobdef = new JenkinsJobDef(name);
			for(JsonNode jpdefname : jpdefs) {
				try {
					@SuppressWarnings("unused") // jpdef is added to a collection within jobdef implicitly.
					JobParameterDef jpdef = new JenkinsJobParameterDef(jobdef, jpdefname.get(0).textValue());
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			return jobdef;
		}
	}

	/**
	 * Get a printout of the json that will represent a single JobParameterDef or the entire JobDef
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		JobDef jobdef = new JenkinsJobDef("MyJob");
		JobParameterDef jpdef = new JenkinsJobParameterDef(
				jobdef, 
				"MyParmDef1", 
				//JenkinsJobParameterDef.CLASSPATH_HTML_RESOURCE_DIR + "/myparm1", 
				"myparm1", 
				true);
		//System.out.println(jpdef.toJSON());
		
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jobdef);
		System.out.println(json);
	}
}
