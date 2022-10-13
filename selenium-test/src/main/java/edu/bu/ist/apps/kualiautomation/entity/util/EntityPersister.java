package edu.bu.ist.apps.kualiautomation.entity.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import edu.bu.ist.apps.kualiautomation.util.ReflectionUtils;

/**
 * An entity that has been created by Jackson/Jersey through deserialization would not have any @ManyToOne fields
 * that contain collections that create bi-directional endless loop issues. However, if the entity has been persisted,
 * the entity is managed and the bi-directional issue comes back. This class populates such an entity by traversing 
 * recursively only the @OneToMany collection fields and persisting/merging any new entities found. This is a
 * uni-directional procedure and does not cause endless loop issues.
 * 
 * NOTE: 
 *    1) This has not yet been adapted for @OneToOne fields.
 *    2) Entities must have an "add" method for their @OneToMany collections containing the following convention:
 *          get[childType]s().add(child);
 *          child.set[thisType](this);
 *    3) all accessor and mutator methods for the entities that are used here must follow standard bean naming conventions.
 *    4) Do not set the CascadeType to persist. This class would not be necessary if that worked, but while the @OneToMany
 *       entities are persisted, their own @ManyToOne back-references remain null, which is not what we want. Ostensibly,
 *       you can remedy this by mapping a bidirectional one to many, with the one-to-many side as the owning side. To do 
 *       this you have to remove the mappedBy element from the @OneToMany and set the @ManyToOne @JoinColumn as insertable=false 
 *       and updatable=false. I'm not sure what kind of weirdness may result if this is done, so I'm putting it on the side for now.
 *       
 * @author wrh
 *
 */
public class EntityPersister {

	private EntityManager em;
	private Object entity;
	private EntityInspector inspector;
	
	public EntityPersister(EntityManager em, Object entity) {
		this.em = em;
		this.entity = entity;
		this.inspector = new EntityInspector(entity);
	}
	
	public void persist() throws Exception {
		
		em.persist(entity);
		
		handleChildren();
	}
	
	@SuppressWarnings("unchecked")
	private <T> void handleChildren() throws Exception {
		
		for(Field fld : inspector.getOneToOneFields()) {
			T childEntity = (T) inspector.getValue(fld);
			Method getMethod = ReflectionUtils.getAccessorMethod(entity, fld);	
			Method setMethod = ReflectionUtils.getMutator(getMethod, entity.getClass());
			if(childEntity == null) {
				T existing = (T) getMethod.invoke(entity);
				if(existing != null) {
					setMethod.invoke(entity, childEntity);	// Set to null so as to remove the entity.
				}
			}
			else {
				childEntity = save(childEntity);			
			}
			setMethod.invoke(entity, childEntity);
		}
		
		for(Field manyFld : inspector.getOneToManyFields()) {
			
			/** 1) Cache the children */
			List<T> entities = new ArrayList<T>((Collection<T>) inspector.getValue(manyFld));			
			
			/** 2) Remove the children from the parent entity */
			Method getterMethod = ReflectionUtils.getAccessorMethod(entity, manyFld);
			((Collection<T>) ReflectionUtils.getAccessorValue(entity, manyFld.getName())).clear();
			
			/** 3)
			 * The adder method will not only add the child to the @OneToMany 
			 * collection of the parent, but is expected to also set the parent against to the @ManyToOne 
			 * field of the child. Only then can a persist of the child work without incurring a required 
			 * field exception.
			 */
			Method adderMethod = ReflectionUtils.getMutator(entity, getterMethod, "add");
			if(adderMethod == null) {
				throw new IllegalStateException("Expected \"add\" method for collection " + 
						manyFld.getName() + " in entity " + entity.getClass().getSimpleName() + ", but not found.");
			}
			
			/** 3) Add the @OneToMany entities back one by one after being persisted/merged if needed. */
			for(T childEntity : entities) {
				
				adderMethod.invoke(entity, childEntity);
				
				if(em.contains(childEntity)) {
					/** The entity is managed and so its own @OneToMany field(s) should contain entities that are managed as well. */
				}
				else {
					childEntity = save(childEntity);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> T save(T entity) throws Exception {
		if(entity == null) {
			
		}
		else {
			Object id = (new EntityInspector(entity)).getPrimaryKeyValue();
			if(noId(id)) {
				em.persist(entity);
				// Recurse in case the childEntity has @OneToMany fields of its own.
				EntityPersister saver = new EntityPersister(em, entity);
				saver.handleChildren();
			}
			else {
				if(isTransitory(entity)) {
					entity = (T) em.find(entity.getClass(), id);
				}
				else {
					// Merging might also be handled by a CascadeType=MERGE further up the entity hierarchy.
					entity = em.merge(entity);
				}
			}
		}
		return entity;
	}

	/**
	 * Determines if the @Id field of an entity indicates the entity is unmanaged or unpersisted.
	 * This assumes:
	 *   1) a numeric field.
	 *   2) an autoincrement is set on the field
	 *   3) the autoincrement is set to start seeding at 1 or higher.
	 * 
	 * @param id
	 * @return
	 */
	private boolean noId(Object id) {
		if(id == null)
			return true;
		if(id instanceof Number) {
			if(((Number) id).intValue() == 0) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isTransitory(Object bean) throws Exception {
		Boolean transitory = (Boolean) ReflectionUtils.getAccessorValue(bean, "transitory");
		if(transitory == null)
			return false;
		return transitory;
	}
	
}
