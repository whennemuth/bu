package edu.bu.ist.apps.kualiautomation.entity;

import java.io.IOException;

import javax.persistence.Transient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class AbstractEntity {

	@Transient
	private boolean transitory = true;

	/**
	 * The transitory flag on an entity tells all persisting and merging functionality to skip this entity
	 * if the flag value is true. Therefore, this serializer needs to output a value of false so that the
	 * corresponding entity can be persisted or merged later. Custom serializers override this with the opposite  
	 * value (flag transitory as true) for contained entities that share a bi-directional references to their  
	 * parent entity so as to avoid an endless recursive loop when persisting the parent.
	 * 
	 * An Entity whose transitory flag is set to true may have a parent entity that is not transitory.
	 * When this is the case and the parent entity is persisted, the child entity will be reset by the 
	 * persistence manager and its transitory field will get the default value. This default value has to 
	 * be true to avoid the same recursion problem as the EntityPopulator continues.
	 * 
	 * @return
	 */
	@JsonSerialize(using=TransitoryFieldSerializer.class)
	public boolean isTransitory() {
		return transitory;
	}

	public void setTransitory(boolean transitory) {
		this.transitory = transitory;
	}
	
	public static class TransitoryFieldSerializer extends JsonSerializer<Boolean> {
		@Override public void serialize(
				Boolean transitory, 
				JsonGenerator generator, 
				SerializerProvider provider) throws IOException, JsonProcessingException {
			
			generator.writeBoolean(false);
		}
	}

}
