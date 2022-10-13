package edu.bu.ist.apps.kualiautomation.entity;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.bu.ist.apps.kualiautomation.entity.util.CustomJsonSerializer;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.util.DateOffset;
import edu.bu.ist.apps.kualiautomation.util.DateOffset.DatePart;


/**
 * The persistent class for the label_and_value database table.
 * 
 */
@Entity
@Table(name="label_and_value")
@NamedQuery(name="LabelAndValue.findAll", query="SELECT l FROM LabelAndValue l")
public class LabelAndValue extends AbstractEntity implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true, nullable=false)
	private Integer id;

	@Column(length=45)
	private String label;

	@Column(nullable=false)
	private int sequence;

	@Column(length=1000)
	private String value;
	
	@Column(name="element_type", nullable=false, length=45)
	private String elementType;
	
	@Column(nullable=true, length=100)
	private String identifier;

	@Column(nullable=false)
	private byte navigate;

	@Column(name="screen_scrape_id", nullable=false)
	private int screenScrapeId;
	
	@Transient
	private String screenScrapeType;
	
	@Transient
	private String screenScrapeValue;
	
	@Transient
	private String dateFormatChoice;
	
	@Column(name="date_units", nullable=true)
	private Integer dateUnits;
	
	@Column(name="date_part", nullable=true, length=25)
	private String datePart;
	
	@Column(name="date_format", nullable=true, length=25)
	private String dateFormat;
	
	// uni-directional one-to-one association to ConfigShortcut (ConfigShortcut cannot "see" LabelAndValue). 
	// NOTE: Don't use CascadeType.REMOVE as removals of this entity will try to cascade the removal of the ConfigShortcut
	//       entity from the database, which we don't want because it's a configuration setting and we want it available
	//       for other suites to use, which if they were, you'd get a ConstraintViolationException when commiting the transaction. 
	@OneToOne(cascade={CascadeType.MERGE}, fetch=FetchType.EAGER)
	@JoinColumn(name="shortcut_id", nullable=true)
	private ConfigShortcut configShortcut;
	
	/**
	 * The value field does the double-duty of holding the screenscrape type value.
	 */
	@PrePersist
	@PreUpdate
	private void checkScreenScrape() {
		if(ElementType.SCREENSCRAPE.is(elementType) && value == null) {
			this.value = screenScrapeType;
		}		
	}
	
	/**
	 * This is a hack. I'm adding this property to be included into the json object created
	 * from an instance of this class for convenience in angularjs 2-way binding UI manipulation.
	 */
	@Transient
	public boolean isChecked() {
		return false;
	}
	@Transient
	public void setChecked(boolean checked) {
		/* do nothing */
	}
	
	@Transient
	public String getBooleanValue() {
		if(elementType == null)
			return "false";
		if(value == null)
			return "false";
		return value.trim().equalsIgnoreCase("true") ? "true" : "false";
	}
	@Transient
	public void setBooleanValue(String bool) {
		/* do nothing */
	}

	
	//bi-directional many-to-one association to Suite
	@ManyToOne
	@JoinColumn(name="suite_id", nullable=false)
	private Suite suite;

	public LabelAndValue() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getSequence() {
		if(this.sequence == 0)
			this.sequence++;
		return this.sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public String getValue() {
		if(ElementType.SCREENSCRAPE.is(elementType) && value == null) {
			this.value = screenScrapeType;
		}
		else if(ElementType.TEXTBOX.is(elementType) && value == null && dateFormat != null) {
			this.value = DateOffset.valueOf(dateFormat).getOffsetDate(DatePart.valueOf(datePart), dateUnits);
		}
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Integer getDateUnits() {
		return dateUnits;
	}

	public void setDateUnits(Integer dateUnits) {
		this.dateUnits = dateUnits;
	}

	public String getDatePart() {
		return datePart;
	}

	public void setDatePart(String datePart) {
		this.datePart = datePart;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	@Transient
	public String getDateFormatChoice() {
		if(dateFormatChoice != null)
			return dateFormatChoice;
		if(dateFormat == null)
			return null;
		DateOffset format = DateOffset.valueOfOrNull(dateFormat);
		if(format == null)
			return DateOffset.CUSTOM.name();
		else 
			return format.name();
	}
	
	@Transient
	public void setDateFormatChoice(String dateFormatChoice) {
		this.dateFormatChoice = dateFormatChoice;
	}
	
	public String getElementType() {
		return elementType;
	}
	
	@JsonIgnore @Transient
	public ElementType getElementTypeEnum() {
		try {
			return ElementType.valueOf(elementType);
		} 
		catch (RuntimeException e) {
			return null;
		}
	}
	
	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	@Transient
	public String getScreenScrapeType() {
		if(ElementType.SCREENSCRAPE.is(elementType) && screenScrapeType == null) {
			this.screenScrapeType = value;
		}
		return screenScrapeType;
	}
	
	@Transient
	public void setScreenScrapeType(String screenScrapeType) {
		this.screenScrapeType = screenScrapeType;
	}

	@Transient
	public String getScreenScrapeValue() {
		return screenScrapeValue;
	}

	@Transient
	public void setScreenScrapeValue(String screenScrapeValue) {
		this.screenScrapeValue = screenScrapeValue;
	}

	/**
	 * @return The id of another LabelAndValue entity whose entityType is SCREENSCRAPE and whose
	 * value indicates a screenScrapeType. That entity will be used to dynamically "scrape the screen"
	 * for the text sought which will be used in place of the value property of this instance.
	 */
	public int getScreenScrapeId() {
		return screenScrapeId;
	}

	public void setScreenScrapeId(int screenScrapeId) {
		this.screenScrapeId = screenScrapeId;
	}
	
	@JsonIgnore
	@Transient
	public boolean isScreenScrape() {
		return ElementType.SCREENSCRAPE.is(elementType);
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

//	@JsonSerialize(using=ShortcutFieldSerializer.class)
	public ConfigShortcut getConfigShortcut() {
		return configShortcut;
	}
	
	public void setConfigShortcut(ConfigShortcut shortcut) {
		this.configShortcut = shortcut;
	}
	
	@JsonIgnore
	public byte getNavigate() {
		return navigate;
	}
	public boolean isNavigates() {
		boolean retval = (navigate != 0);
		if(configShortcut != null) {
			retval |= configShortcut.isNavigates();
		}
		
		return retval;
	}

	@JsonIgnore
	public void setNavigate(byte navigate) {
		this.navigate = navigate;
	}
	public void setNavigates(boolean navigates) {
		this.navigate = (byte) (navigates ? 1 : 0);
	}
	
	@JsonSerialize(using=SuiteFieldSerializer.class)
	public Suite getSuite() {
		return this.suite;
	}

	public void setSuite(Suite suite) {
		this.suite = suite;
	}
	
	public static class SuiteFieldSerializer extends JsonSerializer<Suite> {
		@Override public void serialize(
				Suite suite, 
				JsonGenerator generator, 
				SerializerProvider provider) throws IOException, JsonProcessingException {
			
			(new CustomJsonSerializer<Suite>()).serialize(suite, generator, provider);
		}
	}

	/**
 * 
	 * @return A clone of everything except the id (leave null). Would be a detached instance that
	 * wouldn't seem to have been persisted yet as far as JPA is concerned.
	 * @throws CloneNotSupportedException
	 */
	public LabelAndValue copy() {
		// TODO Auto-generated method stub
		LabelAndValue lv = null;
		try {
			lv = (LabelAndValue) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
		lv.setId(null);
		return lv;
	}
	
//	public static class ShortcutFieldSerializer extends JsonSerializer<ConfigShortcut> {
//		@Override public void serialize(
//				ConfigShortcut shortcut, 
//				JsonGenerator generator, 
//				SerializerProvider provider) throws IOException, JsonProcessingException {
//			
//			(new CustomJsonSerializer<ConfigShortcut>()).serialize(shortcut, generator, provider);
//		}
//	}

}