package edu.bu.ist.apps.kualiautomation.entity;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.bu.ist.apps.kualiautomation.entity.ConfigEnvironment.ConfigFieldSerializer;
import edu.bu.ist.apps.kualiautomation.entity.util.CustomJsonSerializer;
import edu.bu.ist.apps.kualiautomation.services.automate.element.ElementType;
import edu.bu.ist.apps.kualiautomation.util.Utils;


/**
 * The persistent class for the config_shortcut database table.
 * 
 */
@Entity
@Table(name="config_shortcut")
@NamedQueries({
	@NamedQuery(name="ConfigShortcut.findAll", query="SELECT s FROM ConfigShortcut s"),
	@NamedQuery(name="ConfigShortcut.findByConfigId", query="SELECT s FROM ConfigShortcut s WHERE s.config.id = :configid")
})
public class ConfigShortcut extends AbstractEntity implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true, nullable=false)
	private int id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="created_date", nullable=false)
	private Date createdDate;

	@Column(name="element_type", nullable=false, length=45)
	private String elementType;

	@Column(length=45)
	private String identifier;

	@Column(nullable=false)
	private byte include;

	@Column(nullable=false)
	private byte navigate;

	@Column(name="label_hierarchy", nullable=false, length=255)
	private String labelHierarchy;

	@Transient
	private String[] labelHierarchyParts;

	@Column(nullable=false)
	private int sequence;

	//bi-directional many-to-one association to Config
	@ManyToOne
	@JoinColumn(name="config_id", nullable=false)
	private Config config;
	
	/**
	 * JPA lifecycle callback methods
	 */
	@PrePersist
	public void prePersist() {
		createdDate = new Date(System.currentTimeMillis());
	}

	public ConfigShortcut() {
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getCreatedDate() {
		return this.createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getElementType() {
		return this.elementType;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@JsonIgnore
	public byte getInclude() {
		return this.include;
	}
	public boolean getIncluded() {
		return (include != 0);
	}

	@JsonIgnore
	public void setInclude(byte include) {
		this.include = include;
	}
	public void setIncluded(boolean included) {
		this.include = (byte) (included ? 1 : 0);
	}

	
	@JsonIgnore
	public byte getNavigate() {
		return navigate;
	}
	public boolean isNavigates() {
		return (navigate != 0);
	}

	@JsonIgnore
	public void setNavigate(byte navigate) {
		this.navigate = navigate;
	}
	public void setNavigates(boolean navigates) {
		this.navigate = (byte) (navigates ? 1 : 0);
	}

	public int getSequence() {
		return this.sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	@JsonSerialize(using=ConfigFieldSerializer.class)
	public Config getConfig() {
		return this.config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public String getLabelHierarchy() {
		return this.labelHierarchy;
	}
	public void setLabelHierarchy(String labelHierarchy) {
		if(labelHierarchyParts == null) {
			this.labelHierarchy = labelHierarchy;
			// setLabelHierarchyParts() may yet be called and can override this setting
		}
		else {
			// setLabelHierarchyParts() was called and its evaluation of labelHierarchy takes precedence.
		}
	}
	
	@Transient
	public String[] getLabelHierarchyParts() {
		String h = getLabelHierarchy();
		if(Utils.isEmpty(h) || h.trim().isEmpty() || h.trim().equals(LABEL_HIERARCHY_SEPARATOR.trim())) {
			return new String[]{};
		}
		
		return h.trim().split(LABEL_HIERARCHY_SEPARATOR_REGEX);
	}
	
	/**
	 * Flatten the labelHierarchyParts array into a string and set the persistable labelHierarchy field with it.
	 * @param parts
	 */
	public void setLabelHierarchyParts(String[] parts) {
		if(parts == null)
			return;
		StringBuilder s = new StringBuilder();		
		for(int i=0; i<parts.length; i++) {
			if(!Utils.isEmpty(parts[i])) {
				s.append(parts[i].trim());
				if((i+1) != parts.length) {
					String nextpart = parts[i+1].trim();
					if(!nextpart.isEmpty()) {
						s.append(LABEL_HIERARCHY_SEPARATOR);
					}
				}
			}
		}
		if(!Utils.isEmpty(s.toString())) {
			labelHierarchy = s.toString();
			labelHierarchyParts = parts;
		}
	}

	/**
	 * This is a hack. I'm adding this property to be included into the json object created
	 * from an instance of this class for convenience in angularjs 2-way binding UI manipulation.
	 */
	@Transient
	private boolean checked;
	public boolean isChecked() {
		return checked;
	}
	public void setChecked(boolean checked) {
		this.checked = checked;
	}
	

	public static final String LABEL_HIERARCHY_SEPARATOR = " >>> ";
	public static final String LABEL_HIERARCHY_SEPARATOR_REGEX = "\\x20>>>\\x20";
	@Transient
	public String getSeparator() {
		return LABEL_HIERARCHY_SEPARATOR;
	}
	public void setSeparator(String separator) {
		// Do nothing - this field is for the UI only and not for persistence.
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		ConfigShortcut clone = new ConfigShortcut();
		clone.id = this.getId();
		clone.config = this.getConfig();
		clone.createdDate = this.getCreatedDate();
		clone.elementType = this.getElementType();
		clone.identifier = this.getIdentifier();
		clone.include = this.getInclude();
		clone.labelHierarchy = this.getLabelHierarchy();
		clone.labelHierarchyParts = this.getLabelHierarchyParts();
		clone.navigate = this.getNavigate();
		clone.sequence = this.getSequence();
		return clone;
	}
	
	
}