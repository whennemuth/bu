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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.bu.ist.apps.kualiautomation.entity.Config.UserFieldSerializer;
import edu.bu.ist.apps.kualiautomation.services.automate.KerberosLoginParms;


/**
 * The persistent class for the cycle database table.
 * 
 */
@Entity
@Table(name="cycle")
@NamedQueries({
	@NamedQuery(name="Cycle.findAll", query="SELECT c FROM Cycle c"),
	@NamedQuery(name="Cycle.findByUserId", query="SELECT c FROM Cycle c WHERE c.user.id = :userid")
})
public class Cycle extends AbstractEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true, nullable=false)
	private Integer id;

	@Column(nullable=false, length=45)
	private String name;

	@Column(nullable=false)
	private int sequence;
	
	@Transient
	private int repeat = 1;
	
	@Transient
	private KerberosLoginParms kerberosLoginParms = new KerberosLoginParms();

	/**
	 * bi-directional many-to-one association to Suite.
	 * 
	 * NOTE: This is a class whose counterpart on the other side of the @OneToMany relationship itself also
	 * has an eagerly fetched collection. For some reason, JPA imposes a restriction in filling up the "bag"
	 * to one level of eager fetching - nested fetching is restricted unless the bag is based on a Set collection, not a list.
	 * If this collection were a list you would see a MultipleBagFetchException thrown when fetching is triggered.
	 */
	@OrderBy("sequence ASC")
	@OneToMany(cascade={CascadeType.MERGE, CascadeType.REMOVE}, fetch=FetchType.EAGER, mappedBy="cycle")
	private Set<Suite> suites = new LinkedHashSet<Suite>();
	
	//bi-directional many-to-one association to User
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private User user;

	public Cycle() {
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

	public int getSequence() {
		if(this.sequence == 0)
			this.sequence++;
		return this.sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	
	public int getRepeat() {
		return repeat;
	}

	public void setRepeat(int repeat) {
		this.repeat = repeat;
	}

	public KerberosLoginParms getKerberosLoginParms() {
		return kerberosLoginParms;
	}

	public void setKerberosLoginParms(KerberosLoginParms kerberosLoginParms) {
		this.kerberosLoginParms = kerberosLoginParms;
	}

	@JsonSerialize(using=UserFieldSerializer.class)
	public User getUser() {
		return this.user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Set<Suite> getSuites() {
		return suites;
	}

	public void setSuites(Set<Suite> suites) {
		this.suites.clear();
		this.suites.addAll(suites);
	}

	public Suite addSuite(Suite suite) {
		getSuites().add(suite);
		suite.setCycle(this);

		return suite;
	}

	public Suite removeSuite(Suite suite) {
		getSuites().remove(suite);
		suite.setCycle(null);

		return suite;
	}


}