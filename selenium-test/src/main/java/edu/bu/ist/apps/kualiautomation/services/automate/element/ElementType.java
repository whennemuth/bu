package edu.bu.ist.apps.kualiautomation.services.automate.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import edu.bu.ist.apps.kualiautomation.util.Utils;

public enum ElementType {

	TEXTBOX(
		"Simple text box",
		new String[]{"input"},
		"text",
		"//input[@type='text'] | //input[not(@type)]",
		true,
		false,
		false,
		false),
	PASSWORD(
		"Password text box",
		new String[]{"input"},
		"password",
		"//input[@type='password']",
		true,
		false,
		false,
		false),
	TEXTAREA(
		"Area in which multi-line text can be input",
		new String[]{"textarea"},
		null,
		"//textarea",
		true,
		false,
		false,
		false),
	BUTTON(
		"Something to click that looks like a button",
		new String[]{"input"},
		"button",
		"//input[@type='button'] | //input[@type='submit'] | //button",
		false,
		true,
		false,
		true),
	BUTTONIMAGE(
		"Something to click that looks like an image",
		new String[]{"input"},
		"image",
		"//input[@type='image']",
		false,
		true,
		false,
		true),	
	HYPERLINK(
		"Clickable text that changes the cursor when hovered over to indicate a hyperlink.",
		new String[]{"a"},
		null,
		"//a[text()]",
		false,
		true,
		false,
		true), 
	HOTSPOT(
		"Clickable graphic that changes the cursor when hovered over to indicate a hotspot for navigation or function trigger.",
		new String[]{"a"},
		null,
		"//a[not(text())] | //a[text() and not(text())]",
		false,
		true,
		false,
		true), 
	SELECT(
		"A dropdown box or listbox",
		new String[]{"select"},
		null,
		"//select",
		false,
		false,
		false,
		false),
	CHECKBOX(
		"Checkable box",
		new String[]{"input"},
		"checkbox",
		"//input[@type='checkbox']",
		false,
		false,
		true,
		false),
	RADIO(
		"Radio Button",
		new String[]{"input"},
		"radio",
		"//input[@type='radio']",
		false,
		false,
		true,
		false),
	SHORTCUT(
		"A hyperlink or hotspot that is located by more than one label heading" ,
		null,
		null,
		null,
		false,
		true,
		false,
		true
	),
	SCREENSCRAPE(
		"Any html element, like a div or span, that is not a field",
		null,
		null,
		null,
		false, 
		false, 
		false,
		false),
	OTHER(
		"None of the above, but clickable",
		null,
		null,
		"//*",
		false,
		false,
		false,
		true);
	
	private String description;
	private String xpathSelector;
	private String[] tagnames;
	private String typeAttribute;
	private boolean acceptsKeystrokes;
	private boolean canNavigate;
	private boolean checkable;
	private boolean useClickEvent;
	
	private ElementType(String description, String[] tagnames, String typeAttribute, String xpathSelector, boolean acceptsKeystrokes, boolean canNavigate, boolean checkable, boolean useClickEvent) {
		this.description = description;
		this.xpathSelector = xpathSelector;
		this.tagnames = tagnames;
		this.typeAttribute = typeAttribute;
		this.acceptsKeystrokes = acceptsKeystrokes;
		this.canNavigate = canNavigate;
		this.checkable = checkable;
		this.useClickEvent = useClickEvent;
	}

	public String getDescription() {
		return description;
	}

	public String getXpathSelector() {
		return xpathSelector;
	}

	public String getTagname() {
		return tagnames == null || tagnames.length == 0 ? null : tagnames[0];
	}

	public String[] getTagnames() {
		return tagnames;
	}
	
	public String getTypeAttribute() {
		return typeAttribute;
	}
	
	public boolean acceptsKeystrokes() {
		return acceptsKeystrokes;
	}

	public boolean canNavigate() {
		return canNavigate;
	}

	public boolean isCheckable() {
		return checkable;
	}

	public boolean useClickEvent() {
		return useClickEvent;
	}

	public boolean is(String name) {
		return this.name().equals(name);
	}
	
	public List<WebElement> findAll(SearchContext ctx, boolean frame) {
		boolean global = true;
		if(ctx instanceof WebElement)
			global = false;
		
		String xpath = getXpath(global);
		
		List<WebElement> flds = XpathElementCache.get(ctx, xpath, frame);
		if(flds.isEmpty()) {
			flds.addAll(AbstractWebElement.wrap(ctx.findElements(By.xpath(xpath))));
			XpathElementCache.put(ctx, xpath, frame, flds);
		}

		return flds;
	}
	
	private String getXpath(boolean global) {
		String xpath = global ? "//" : ".//";
		if(xpathSelector != null) {
			xpath = xpathSelector.replaceAll("//", xpath);
		}
		return xpath;
	}
	
	public static ElementType getInstance(WebElement we) {
		String type = null;
		String tagname = we == null ? null : we.getTagName();
		
		if(!Utils.isEmpty(tagname)) {
			type = we.getAttribute("type");
			for(ElementType et : ElementType.values()) {
				if(tagname != null && tagname.equalsIgnoreCase(et.getTagname())) {
					if(Utils.isEmpty(type) && Utils.isEmpty(et.getTypeAttribute())) {
						return et;
					}
					if(!Utils.isEmpty(type) && !Utils.isEmpty(et.getTypeAttribute())) {
						if(type.equalsIgnoreCase(et.getTypeAttribute())) {
							return et;
						}
					}
				}
			}
		}
		
		if("select".equalsIgnoreCase(tagname)) {
			if("select-one".equalsIgnoreCase(type) || "select-multiple".equalsIgnoreCase(type)){
				return SELECT;
			}
		}
		else if("textarea".equalsIgnoreCase(tagname)) {
			if("textarea".equalsIgnoreCase(type)) {
				return TEXTAREA;
			}
		}
		else if("input".equalsIgnoreCase(tagname)) {
			if("submit".equalsIgnoreCase(type)) {
				return BUTTON;
			}
			else if("button".equalsIgnoreCase(type)) {
				return BUTTON;
			}
			else if(Utils.isEmpty(type)) {
				return TEXTBOX;	// The default type of input is text
			}
		}
		else if("button".equalsIgnoreCase(tagname)) {
			return BUTTON;
		}
		return OTHER;
	}

	public static Map<String, String> toJson() {
		Map<String, String> map = new HashMap<String, String>();
		for(ElementType typ : ElementType.values()) {
			map.put(typ.name(), typ.getDescription());
		}
		return map;
	}
}
