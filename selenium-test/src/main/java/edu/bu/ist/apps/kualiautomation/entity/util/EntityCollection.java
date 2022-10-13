package edu.bu.ist.apps.kualiautomation.entity.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import edu.bu.ist.apps.kualiautomation.util.ReflectionUtils;

/**
 * This class is used to perform updates to the @OneToMany annotated collections of an entity
 * 
 * An entity is provided so that one of its @OneToMany annotated collection fields (targetCollection)
 * can be updated by a corresponding collection. By "updated" is meant that the targetCollection will be modified to
 * look like the sourceCollection, but not by adding members of the sourceCollection or by modifying the targetCollection
 * directly. Instead, use of add and remove methods of the parent entity along with EntityManager persist and remove methods 
 * are favored so as not to introduce detached entities and other errors that would show up during a cascading merge or a commit.
 * 
 * Essentially the effect is to perform what you would expect of an entity with CascadeType=ALL set on all of
 * its foreign key bi-directionally annotated fields.
 * 
 * @author wrh
 *
 */
public class EntityCollection {
	private boolean ignoreEmpties;
	private EntityPopulator entity;
	private EntityManager em;
	
	@SuppressWarnings("unused")
	private EntityCollection() { /* Restrict default constructor */ }
	
	public EntityCollection(EntityPopulator entity, boolean ignoreEmpties) {
		this.entity = entity;
		this.em = entity.getEntityManager();
		this.ignoreEmpties = ignoreEmpties;
	}

	/**
	 * 
	 * @param parentEntity The entity in which targetCollection is a field.
	 * @param getterMethod The getter method that was used to obtained the sourceCollection against the source entity.
	 * @param sourceCollection The collection that is used to populate the targetCollection.
	 * @throws Exception
	 */
	public <T> void populateCollection(Object parentEntity, Method getterMethod, Collection<T> sourceCollection) throws Exception {		
		List<EquatableEntity> sources = new ArrayList<EquatableEntity>();
		List<EquatableEntity> targets = new ArrayList<EquatableEntity>();
		
		// 1) Get the method to use to add entities to the parentEntity
		Method adderMethod = ReflectionUtils.getMutator(parentEntity, getterMethod, "add");
		
		Method removeMethod = ReflectionUtils.getMutator(parentEntity, getterMethod, "remove");
		
		// 2) Get the collection that is to be updated
		@SuppressWarnings("unchecked")
		Collection<T> targetCollection = (Collection<T>) ReflectionUtils.getAccessorValue(parentEntity, getterMethod.getName());
		
		// 3) Populate equatable sources and targets lists
		for (Iterator<T> iterator = sourceCollection.iterator(); iterator.hasNext();) {
			sources.add(EquatableEntity.getInstance(iterator.next()));
		}
		for (Iterator<T> iterator = targetCollection.iterator(); iterator.hasNext();) {
			targets.add(EquatableEntity.getInstance(iterator.next()));
		}
		
		// 4) Perform additions and updates to entities in the target collection
		for (Iterator<EquatableEntity> iterator = sources.iterator(); iterator.hasNext();) {
			EquatableEntity source = (EquatableEntity) iterator.next();
			if(targets.contains(source)) {
				// 4a) Populate the target entity with changes in the source entity
				for (Iterator<EquatableEntity> iterator2 = targets.iterator(); iterator2.hasNext();) {
					EquatableEntity target = (EquatableEntity) iterator2.next();
					if(target.equals(source)) {
						BeanPopulator populator = new BeanPopulator(entity, ignoreEmpties);
						System.out.println("Start populating entity [" + target.getEntity().getClass().getSimpleName() + "]...");
						populator.populate(target.getEntity(), source.getEntity());
						System.out.println("End populating entity [" + target.getEntity().getClass().getSimpleName() + "]...");
						// Assuming calling process will call merge on the EntityManager for an entity 
						// further up the parent hierchy. NOTE: This requires CascadeType=MERGE or CascadeType=ALL
						// em.merge(parentEntity);
					}
				}
			}
			else {
				// 4b) Add the source entity to the target collection
				// REQUIREMENT: the target parent entity must have an "add" method for adding to corresponding collection.
				System.out.println("Invoking method: " + adderMethod.getName() + " [" + 
						source.getEntity().getClass().getSimpleName() + " to " + 
						parentEntity.getClass().getSimpleName() + "]");				
				adderMethod.invoke(parentEntity, source.getEntity());
				
				// 4c) Handle the @OneToMany fields of the entity
				Object managedSource = entity.getManagedEntity(source.getEntity());
				if(managedSource == null) {					
					EntityPersister persister = new EntityPersister(em, source.getEntity());
					persister.persist();
				}
				else {
					// Assuming calling process will call merge on the EntityManager for an entity 
					// further up the parent hierchy. NOTE: This requires CascadeType=MERGE or CascadeType=ALL					
				}
			}
		}

		// 5) Perform entity removals from the target collection
		for (Iterator<EquatableEntity> iterator = targets.iterator(); iterator.hasNext();) {
			EquatableEntity target = (EquatableEntity) iterator.next();
			if(!sources.contains(target)) {
				System.out.println("Invoking method: " + removeMethod.getName() + " [" + 
						target.getEntity().getClass().getSimpleName() + " from " + 
						parentEntity.getClass().getSimpleName() + "]");
				removeMethod.invoke(parentEntity, target.getEntity());
				em.remove(target.getEntity());
			}
		}	
	}	
}
