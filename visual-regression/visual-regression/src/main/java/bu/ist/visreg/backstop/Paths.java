package bu.ist.visreg.backstop;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Paths {
	String bitmaps_reference = "backstop_data/bitmaps_reference";
	String bitmaps_test = "backstop_data/bitmaps_test";
	String engine_scripts = "backstop_data/engine_scripts";
	String html_report = "backstop_data/html_report";
	String ci_report = "backstop_data/ci_report";
	public String getBitmaps_reference() {
		return bitmaps_reference;
	}
	public void setBitmaps_reference(String bitmaps_reference) {
		this.bitmaps_reference = bitmaps_reference;
	}
	public String getBitmaps_test() {
		return bitmaps_test;
	}
	public void setBitmaps_test(String bitmaps_test) {
		this.bitmaps_test = bitmaps_test;
	}
	public String getEngine_scripts() {
		return engine_scripts;
	}
	public void setEngine_scripts(String engine_scripts) {
		this.engine_scripts = engine_scripts;
	}
	public String getHtml_report() {
		return html_report;
	}
	public void setHtml_report(String html_report) {
		this.html_report = html_report;
	}
	public String getCi_report() {
		return ci_report;
	}
	public void setCi_report(String ci_report) {
		this.ci_report = ci_report;
	}
}