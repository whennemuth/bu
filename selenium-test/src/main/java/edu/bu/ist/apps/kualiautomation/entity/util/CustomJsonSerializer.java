package edu.bu.ist.apps.kualiautomation.entity.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;

import edu.bu.ist.apps.kualiautomation.util.ReflectionUtils;

/**
 * Due to bi-directional references between entity classes, the json ObjectMapper gets stuck in a 
 * recursion loop when serializing them to json. This custom serializer will avoid this by only
 * serializing the id of the child object field. 
 * 
 * @author Warren
 *
 */
public class CustomJsonSerializer <T> {

	@SuppressWarnings("unchecked")
	public void serialize(
			T obj, 
			JsonGenerator generator, 
			SerializerProvider provider) throws IOException, JsonProcessingException {

		try {
			if(obj == null) {
				generator.writeObject(null);
				return;
			}
			else if(obj instanceof Collection) {
				serialize((Collection<T>) obj, generator, provider);
				return;
			}
			
			Integer id = getId(obj);
			
			if(id == null || id == 0) {
				generator.writeNull();
			}
			else {
				generator.writeStartObject();
				if(id == -1) {
					// -1 is a special value that means the id is unknown, but don't write out a null for obj
					generator.writeNullField("id");
				}
				else {
					generator.writeNumberField("id", id);
				}
				/**
				 * The transitory field is output here as true so that the object produced when 
				 * deserialization is invoked later will be flagged to tell custom entity
				 * updating code to ignore this private entity field (and avoid the recursion loop). 
				 */
				generator.writeBooleanField("transitory", true);
				generator.writeEndObject();
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void serialize(
			Collection<T> collection, 
			JsonGenerator generator, 
			SerializerProvider provider) throws IOException, JsonProcessingException {

		try {
			if(collection == null) {
				generator.writeObject(null);
				return;
			}
			
			List<Integer> ids = new ArrayList<Integer>();
			for(T obj : collection) {
				ids.add(getId(obj));
			}
			generator.writeObject(ids);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Integer getId(T obj) {		
		Integer id = null;
		try {
			id = (Integer) ReflectionUtils.getAccessorValue(obj, "id");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}
}
