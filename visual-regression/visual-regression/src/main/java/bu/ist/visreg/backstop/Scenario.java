package bu.ist.visreg.backstop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonMerge;

import bu.ist.visreg.util.JsonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Scenario {
	String label;                    // [required] Tag saved with your reference images
	String onBeforeScript;           // Used to set up browser state e.g. cookies.
	String cookiePath;               // import cookies in JSON format (available with default onBeforeScript see setting cookies below)
	String url;                      // [required] The url of your app state
	String referenceUrl;             // Specify a different state or environment when creating reference.
	String readyEvent;               // Wait until this string has been logged to the console.
	String readySelector;            // Wait until this selector exists before continuing.
	Integer delay;                   // Wait for x milliseconds
	String[] hideSelectors;          // Array of selectors set to visibility: hidden
	String[] removeSelectors;        // Array of selectors set to display: none
	String onReadyScript;            // After the above conditions are met -- use this script to modify UI state prior to screen shots e.g. hovers, clicks etc.
	String[] keyPressSelectors;      // Takes array of selector and string values -- simulates multiple sequential keypress interactions.
	String hoverSelector;            // Move the pointer over the specified DOM element prior to screen shot.
	String[] hoverSelectors;         // *Puppeteer only* takes array of selectors -- simulates multiple sequential hover interactions.
	String clickSelector;            // Click the specified DOM element prior to screen shot.
	String[] clickSelectors;         // *Puppeteer only* takes array of selectors -- simulates multiple sequential click interactions.
	Integer postInteractionWait;     // Wait for a selector after interacting with hoverSelector or clickSelector (optionally accepts wait time in ms. Idea for use with a click or hover element transition. available with default onReadyScript)
	String scrollToSelector;         // Scrolls the specified DOM element into view prior to screen shot (available with default onReadyScript)
	String[] selectors;              // Array of selectors to capture. Defaults to document if omitted. Use "viewport" to capture the viewport size. See Targeting elements in the next section for more info...
	String selectorExpansion;        // See Targeting elements in the next section for more info...
	Integer misMatchThreshold;       // Percentage of different pixels allowed to pass test
	Boolean requireSameDimensions;   // If set to true -- any change in selector size will trigger a test failure.
	@JsonMerge ViewPort[] viewports; // An array of screen size objects your DOM will be tested against. This configuration will override the viewports property assigned at the config root.
	
	String loginUrl;                 // Not a field from BackstopJs itself. Adding it here as an extension for authentication in getting tokens to access secured web pages.
	
	List<String> invalidMessages;

	public String getLabel() {
		return label;
	}
	public Scenario setLabel(String label) {
		this.label = label;
		return this;
	}
	public String getOnBeforeScript() {
		return onBeforeScript;
	}
	public Scenario setOnBeforeScript(String onBeforeScript) {
		this.onBeforeScript = onBeforeScript;
		return this;
	}
	public String getCookiePath() {
		return cookiePath;
	}
	public Scenario setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
		return this;
	}
	public String getUrl() {
		return url;
	}
	public Scenario setUrl(String url) {
		this.url = url;
		return this;
	}
	public String getReferenceUrl() {
		return referenceUrl;
	}
	public Scenario setReferenceUrl(String referenceUrl) {
		this.referenceUrl = referenceUrl;
		return this;
	}
	public String getReadyEvent() {
		return readyEvent;
	}
	public Scenario setReadyEvent(String readyEvent) {
		this.readyEvent = readyEvent;
		return this;
	}
	public String getReadySelector() {
		return readySelector;
	}
	public Scenario setReadySelector(String readySelector) {
		this.readySelector = readySelector;
		return this;
	}
	public Integer getDelay() {
		return delay;
	}
	public Scenario setDelay(int delay) {
		this.delay = delay;
		return this;
	}
	public String[] getHideSelectors() {
		return hideSelectors;
	}
	public Scenario setHideSelectors(String[] hideSelectors) {
		this.hideSelectors = hideSelectors;
		return this;
	}
	public String[] getRemoveSelectors() {
		return removeSelectors;
	}
	public Scenario setRemoveSelectors(String[] removeSelectors) {
		this.removeSelectors = removeSelectors;
		return this;
	}
	public String getOnReadyScript() {
		return onReadyScript;
	}
	public Scenario setOnReadyScript(String onReadyScript) {
		this.onReadyScript = onReadyScript;
		return this;
	}
	public String[] getKeyPressSelectors() {
		return keyPressSelectors;
	}
	public Scenario setKeyPressSelectors(String[] keyPressSelectors) {
		this.keyPressSelectors = keyPressSelectors;
		return this;
	}
	public String getHoverSelector() {
		return hoverSelector;
	}
	public Scenario setHoverSelector(String hoverSelector) {
		this.hoverSelector = hoverSelector;
		return this;
	}
	public String[] getHoverSelectors() {
		return hoverSelectors;
	}
	public Scenario setHoverSelectors(String[] hoverSelectors) {
		this.hoverSelectors = hoverSelectors;
		return this;
	}
	public String getClickSelector() {
		return clickSelector;
	}
	public Scenario setClickSelector(String clickSelector) {
		this.clickSelector = clickSelector;
		return this;
	}
	public String[] getClickSelectors() {
		return clickSelectors;
	}
	public Scenario setClickSelectors(String[] clickSelectors) {
		this.clickSelectors = clickSelectors;
		return this;
	}
	public Integer getPostInteractionWait() {
		return postInteractionWait;
	}
	public Scenario setPostInteractionWait(int postInteractionWait) {
		this.postInteractionWait = postInteractionWait;
		return this;
	}
	public String getScrollToSelector() {
		return scrollToSelector;
	}
	public Scenario setScrollToSelector(String scrollToSelector) {
		this.scrollToSelector = scrollToSelector;
		return this;
	}
	public String[] getSelectors() {
		return selectors;
	}
	public Scenario setSelectors(String[] selectors) {
		this.selectors = selectors;
		return this;
	}
	public String getSelectorExpansion() {
		return selectorExpansion;
	}
	public Scenario setSelectorExpansion(String selectorExpansion) {
		this.selectorExpansion = selectorExpansion;
		return this;
	}
	public Integer getMisMatchThreshold() {
		return misMatchThreshold;
	}
	public Scenario setMisMatchThreshold(int misMatchThreshold) {
		this.misMatchThreshold = misMatchThreshold;
		return this;
	}
	public Boolean isRequireSameDimensions() {
		return requireSameDimensions;
	}
	public Scenario setRequireSameDimensions(Boolean requireSameDimensions) {
		this.requireSameDimensions = requireSameDimensions;
		return this;
	}
	public ViewPort[] getViewports() {
		return viewports;
	}
	public Scenario setViewports(ViewPort[] viewports) {
		this.viewports = viewports;
		return this;
	}		
	public String getLoginUrl() {
		return loginUrl;
	}
	public Scenario setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
		return this;
	}
	public String toJson() throws Exception {
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
		
		if(viewports != null) {
			String id = this.label == null ? "[scenario]" : this.label;
			for(ViewPort viewport : this.viewports) {
				for(String msg : viewport.getInvalidMessages()) {
					invalidMessages.add(id + "." + msg);
				}
			}
		}
		return invalidMessages;
	}
	public static Scenario getDefaultInstance() {
		Scenario scenario = new Scenario();
		scenario.setCookiePath("backstop_data/engine_scripts/cookies.json");
		return scenario;
	}
}