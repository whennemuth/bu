package edu.bu.ist.apps.kualiautomation.entity;

import java.io.IOException;
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
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.bu.ist.apps.kualiautomation.entity.util.CustomJsonSerializer;


/**
 * The persistent class for the suite database table.
 * 
 */
@Entity
@Table(name="suite")
@NamedQuery(name="Suite.findAll", query="SELECT s FROM Suite s")
public class Suite extends AbstractEntity implements Serializable {
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

	/**
	 * bi-directional many-to-one association to LabelAndValue.
	 * 
	 * NOTE: This is a class whose counterpart on the other side of the @OneToMany relationship itself also
	 * has an eagerly fetched collection. For some reason, JPA imposes a restriction in filling up the "bag"
	 * to one level of eager fetching - nested fetching is restricted unless the bag is based on a Set collection, not a list.
	 * If this collection were a list you would see a MultipleBagFetchException thrown when fetching is triggered.
	 */
	@OrderBy("sequence ASC")
	@OneToMany(cascade={CascadeType.MERGE, CascadeType.REMOVE}, fetch=FetchType.EAGER, mappedBy="suite")
	private Set<LabelAndValue> labelAndValues = new LinkedHashSet<LabelAndValue>();

	//bi-directional many-to-one association to Cycle
	@ManyToOne
	@JoinColumn(name="cycle_id", nullable=false)
	private Cycle cycle;

	public Suite() {
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

	public Set<LabelAndValue> getLabelAndValues() {
		return labelAndValues;
	}

	public void setLabelAndValues(Set<LabelAndValue> labelAndValues) {
		this.labelAndValues.clear();
		this.labelAndValues.addAll(labelAndValues);
	}

	public LabelAndValue addLabelAndValue(LabelAndValue labelAndValue) {
		getLabelAndValues().add(labelAndValue);
		labelAndValue.setSuite(this);

		return labelAndValue;
	}

	public LabelAndValue removeLabelAndValue(LabelAndValue labelAndValue) {
		getLabelAndValues().remove(labelAndValue);
		labelAndValue.setSuite(null);

		return labelAndValue;
	}

	@JsonSerialize(using=CycleFieldSerializer.class)
	public Cycle getCycle() {
		return this.cycle;
	}

	public void setCycle(Cycle cycle) {
		this.cycle = cycle;
	}
	
	public int getRepeat() {
		return repeat;
	}

	public void setRepeat(int repeat) {
		this.repeat = repeat;
	}
	
	public static class CycleFieldSerializer extends JsonSerializer<Cycle> {
		@Override public void serialize(
				Cycle cycle, 
				JsonGenerator generator, 
				SerializerProvider provider) throws IOException, JsonProcessingException {
			
			(new CustomJsonSerializer<Cycle>()).serialize(cycle, generator, provider);
		}
	}

}