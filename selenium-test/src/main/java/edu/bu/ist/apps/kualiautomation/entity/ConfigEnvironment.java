package edu.bu.ist.apps.kualiautomation.entity;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.bu.ist.apps.kualiautomation.entity.util.CustomJsonSerializer;


/**
 * The persistent class for the config_server database table.
 * 
 */
@Entity
@Table(name="config_environment")
@NamedQuery(name="ConfigEnvironment.findAll", query="SELECT c FROM ConfigEnvironment c")
//@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id", scope=ConfigEnvironment.class) // Avoids infinite loop in bidirectional joins
public class ConfigEnvironment extends AbstractEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true, nullable=false)
	private Integer id;

	@Column(nullable=false, length=45)
	private String name;

	@Column(nullable=false, length=1000)
	private String url;

	@Column(nullable=false)
	private boolean current;

	@Column(nullable=false)
	private int sequence;

	//bi-directional many-to-one association to Config
	@ManyToOne
	@JoinColumn(name="config_id", nullable=false)
	private Config parentConfig;

	public ConfigEnvironment() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public int getSequence() {
		return this.sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	@JsonSerialize(using=ConfigFieldSerializer.class)
	public Config getParentConfig() {
		return this.parentConfig;
	}

	public void setParentConfig(Config config) {
		this.parentConfig = config;
	}
	
	public static class ConfigFieldSerializer extends JsonSerializer<Config> {
		@Override public void serialize(
				Config config, 
				JsonGenerator generator, 
				SerializerProvider provider) throws IOException, JsonProcessingException {
			
			(new CustomJsonSerializer<Config>()).serialize(config, generator, provider);
		}
	}

}