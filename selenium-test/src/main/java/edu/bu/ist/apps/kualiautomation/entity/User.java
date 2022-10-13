package edu.bu.ist.apps.kualiautomation.entity;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;


/**
 * The persistent class for the user database table.
 * 
 */
@Entity
@Table(name="user")
@NamedQuery(name="User.findAll", query="SELECT u FROM User u")
public class User extends AbstractEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true, nullable=false)
	private Integer id;

	@Column(name="first_name", nullable=false, length=45)
	private String firstName;

	@Column(name="last_name", nullable=false, length=45)
	private String lastName;

	//bi-directional many-to-one association to Suite
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="user")
	private Set<Cycle> cycles = new LinkedHashSet<Cycle>();

	//bi-directional many-to-one association to Config
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="user")
	private Set<Config> configs = new LinkedHashSet<Config>();

	public User() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Set<Cycle> getCycles() {
		return this.cycles;
	}

	public void setCycles(Set<Cycle> cycles) {
		this.cycles = cycles;
	}

	public Cycle addCycle(Cycle cycle) {
		getCycles().add(cycle);
		cycle.setUser(this);

		return cycle;
	}

	public Cycle removeCycle(Cycle cycle) {
		getCycles().remove(cycle);
		cycle.setUser(null);

		return cycle;
	}
	
	public Set<Config> getConfigs() {
		return this.configs;
	}

	public void setConfigs(Set<Config> configs) {
		this.configs = configs;
	}

	public Config addConfig(Config config) {
		getConfigs().add(config);
		config.setUser(this);

		return config;
	}

	public Config removeConfig(Config config) {
		getConfigs().remove(config);
		config.setUser(null);

		return config;
	}

}