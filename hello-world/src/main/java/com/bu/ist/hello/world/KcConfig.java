package com.bu.ist.hello.world;

import java.util.Properties;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;

public class KcConfig extends Properties {

	public static KcConfig cfg;
	public static final String DEFAULT_XML_NAME = "kc-config.xml";
	public static final String[] PATHS = new String[] {
			"~/kuali/main/dev/",
			"~/kuali/main/config/",
			"/opt/kuali/main/config/"
	};
	
	private KcConfig() { /* Restrict default constructor */ }
	
	public static KcConfig getInstance() {
		if(renew()) {
			File xmlfile = null;
			for(int i=0; i<PATHS.length; i++) {
				String filepath = PATHS[i]+DEFAULT_XML_NAME;
				xmlfile = new File(filepath);
				if(xmlfile.isFile()) { 
					getInstance(filepath);
					break; 
				}
			}
		}
		return cfg;
	}

	public static KcConfig getInstance(String path) {
		if(renew()) {
			File xmlfile = new File(path);
			if(xmlfile.isDirectory()) {
				xmlfile = new File(xmlfile.getAbsolutePath() + File.separator + DEFAULT_XML_NAME);
			}
			if(!xmlfile.isFile() && ! path.contains(File.separator)) {
				for(int i=0; i<PATHS.length; i++) {
					xmlfile = new File(PATHS[i]+path);
					if(xmlfile.isFile()) { 
						break; 
					}
				}
			}
			getInstance(xmlfile);
		}
		return cfg;
	}

	public static KcConfig getInstance(File xmlfile) {
		if(renew()) {
			if(xmlfile != null && xmlfile.isFile()) {
				try {
					getInstance(new FileInputStream(xmlfile));
				} 
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return cfg;
	}
	
	public static KcConfig getInstanceFromClasspathResource(String resource) {
		if(renew()) {
			getInstance(KcConfig.class.getClassLoader().getResourceAsStream(resource));
		}
		return cfg;
	}
	
	
	public static KcConfig getInstance(InputStream in) {
		if(renew()) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
				ByteArrayInputStream input = null;
				if(in instanceof ByteArrayInputStream) {
					input = (ByteArrayInputStream) in;
				}
				else {
					byte[] bytes = Util.inputStreamToByteArray(in);
					input =  new ByteArrayInputStream(bytes);
				}
				DocumentBuilder builder = factory.newDocumentBuilder();				
				Document doc = builder.parse(input);
				Node config = doc.getElementsByTagName("config").item(0);
				NodeList parms = config.getChildNodes();
				//NodeList parms = config.getElementsByTagName("param");
				cfg = new KcConfig();
				for(int i=0; i<parms.getLength(); i++) {
					Node node = parms.item(i);
					if(node.getNodeName().equalsIgnoreCase("param")) {
						String key = node.getAttributes().getNamedItem("name").getNodeValue();
						if(key != null) {
							String val = node.getTextContent();
							if(val != null) {
								cfg.setProperty(key, val);
							}
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return cfg;
	}

	private void toString(PrintStream out) {
		for(Object key : this.keySet()) {
			out.print(key.toString());
			out.print(" = ");
			out.println(getProperty(key.toString()));
		}
	}
	
	private static boolean renew() {
		// TODO: Add some other criteria so it is possible to load some different xml even if cfg is not null.
		return cfg == null;
	}
	
	private static enum TEST_TYPE {
		STRING, RESOURCE, PATH
	};
	public static void main(String[] args) throws Exception {
		
		TEST_TYPE test = TEST_TYPE.STRING;
		if(args.length > 0) {
			test = TEST_TYPE.valueOf(args[0].toUpperCase());
		}
		
		switch(test) {
		case STRING:
			String xml = "<config>" + 
			"   <param name=\"application.http.scheme\">http</param>" + 
			"   <param name=\"application.host\">https://kuali-research-ci.bu.edu</param>i" + 
			"   <param name=\"app.host\">https://kuali-research-ci.bu.edu</param>" + 
			"   <param name=\"app.context.name\">kc-dev</param>" + 
			"   <param name=\"application.url\">${app.host}/${app.context.name}</param>" + 
			"   <param name=\"datasource.url\">jdbc:oracle:thin:@buaws-kuali-db-ci001.bu.edu:1521:KUALI</param>" + 
			"   <param name=\"datasource.username\" override=\"true\">bugs bunny</param>" + 
			"   <param name=\"datasource.password\" override=\"true\">my password</param>" + 
			"   <param name=\"datasource.ojb.platform\">Oracle9i</param>" + 
			"   <param name=\"kc.schemaspy.enabled\">false</param>" + 
			"   <param name=\"filtermapping.login.1\">/*</param>" + 
			"   <param name=\"filter.login.class\">org.kuali.rice.krad.web.filter.UserLoginFilter</param>" + 
			"</config>";
			
			ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			KcConfig config = KcConfig.getInstance(in);
			config.toString(System.out);
			break;
		case PATH:
			break;
		case RESOURCE:
			break;
		default:
			break;
		}
	}
}
