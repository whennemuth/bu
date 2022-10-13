package edu.bu.ist.apps.kualiautomation.services.automate.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Calls to SearchContext.find(By) and subsequent processing of WebElement instances is expensive in
 * terms of time. There is no need to repeat a search if an identical search has already been made.
 * To avoid this, search results are stored in this cache which is always checked first if another search
 * is about to be run via xpath. A match is found in the cache if the SearchContext, xpath and frame 
 * presense are all equal.
 * 
 * @author wrh
 *
 */
public class XpathElementCache {
	
	public static final boolean CACHING_ENABLED = false;
	
	private static final Map<XpathElementCache.XpathAndContext, List<WebElement>> cache = 
			new HashMap<XpathElementCache.XpathAndContext, List<WebElement>>();
	
	public static void put(SearchContext searchContext, String xpath, boolean frame, List<WebElement> webElements) {
		XpathAndContext key = new XpathAndContext(searchContext, xpath, frame);
		cache.put(key, new ArrayList<WebElement>(webElements));
	}
	
	public static List<WebElement> get(SearchContext searchContext, String xpath, boolean frame) {
		XpathAndContext xctx = new XpathAndContext(searchContext, xpath, frame);
		List<WebElement> cached = cache.get(xctx);
		if(cached == null)
			return new ArrayList<WebElement>();
		return cached;
	}
	
	public static void clear() {
		cache.clear();
	}
	
	public static int size() {
		return cache.size();
	}
	
	public static String cacheToString() {
		StringBuilder s = new StringBuilder();
		for(XpathAndContext xctx : cache.keySet()) {
			s.append(xctx);
		}
		return s.toString();
	}
	
	private static class XpathAndContext {
		private SearchContext searchContext;
		private String url;
		private String xpath;
		private boolean frame;
		private boolean isDriver;
		
		public XpathAndContext(SearchContext searchContext, String xpath, boolean frame) {
			this.searchContext = searchContext;
			if(searchContext != null) {
				if(searchContext instanceof WebDriver) {
					this.isDriver = true;
					this.url = ((WebDriver) searchContext).getCurrentUrl();
				}
			}
			
			this.xpath = xpath;
			this.frame = frame;
		}
		public String getXpath() {
			return xpath;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (frame ? 1231 : 1237);
			result = prime * result + ((url == null) ? 0 : url.hashCode());
			result = prime * result + ((xpath == null) ? 0 : xpath.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof XpathAndContext))
				return false;
			XpathAndContext other = (XpathAndContext) obj;
			if (frame != other.frame)
				return false;
			if (xpath == null || other.xpath == null) {
				return false;
			} else if (!xpath.equals(other.xpath))
				return false;
			return sameSearchContexts(other);
		}
		private boolean sameSearchContexts(XpathAndContext other) {
			if(this.isDriver && other.isDriver) {
				if(url == null && other.url == null)
					return true;
				else if (url == null || other.url == null) 
					return false;
				else
					return url.equals(other.url);
			}
			if(this.searchContext instanceof WebElement && other.searchContext instanceof WebElement) {
				WebElement we1 = (WebElement)this.searchContext;
				WebElement we2 = (WebElement)other.searchContext;
				return we1.equals(we2);
			}
			return false;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("XpathAndContext [\n\tsearchContext=")
			.append(searchContext)
			.append(", \n\turl=").append(url)
			.append(", \n\txpath=").append(xpath)
			.append(", \n\tisDriver=").append(isDriver)
			.append(", \n\tframe=").append(frame).append("]\n");
			return builder.toString();
		}
	}
}
