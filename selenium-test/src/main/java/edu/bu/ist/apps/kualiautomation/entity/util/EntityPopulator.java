package edu.bu.ist.apps.kualiautomation.entity.util;

import java.lang.reflect.Method;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import edu.bu.ist.apps.kualiautomation.util.ReflectionUtils;

/**
 * This class is used by EnityPopulator, in conjunction with EntityPersister to recursively populate
 * The fields of a JPA entity with those of another (probably a "shallow" version with updated fields 
 * de-serialized by Jersey/Jackson enroute through a web service endpoint.
 * 
 * @author wrh
 *
 */
public class EntityPopulator {
	private boolean ignoreEmpties;
	private EntityManager em;
	
	@SuppressWarnings("unused")
	private EntityPopulator() { /* Restrict default constructor */ }
	
	public EntityPopulator(EntityManager em, boolean ignoreEmpties) {
		this.em = em;
		this.ignoreEmpties = ignoreEmpties;
	}
	
	public boolean populate(Object beanToPopulate, String getterMethodName, Object sourceEntity) throws Exception {
		Object targetEntity = ReflectionUtils.getAccessorValue(beanToPopulate, getterMethodName);
		BeanPopulator populator = new BeanPopulator(this, ignoreEmpties);
		
		EquatableEntity source = EquatableEntity.getInstance(sourceEntity);
		EquatableEntity target = EquatableEntity.getInstance(targetEntity);
		if(source.equals(target)) {
			populator.populate(target.getEntity(), source.getEntity());
			return true;
		}
		else {
			if(sourceEntity == null) {
				Method setterMethod = ReflectionUtils.getMutator(getterMethodName, beanToPopulate.getClass());
				setterMethod.invoke(beanToPopulate, sourceEntity);
				return true;
			}
			else {
				// Set the sourceEntity, but don't assume it is managed - getting a managed version first if it is not.
				Object managedSource = getManagedEntity(sourceEntity);
				if(managedSource != null) {
					Method setterMethod = ReflectionUtils.getMutator(getterMethodName, beanToPopulate.getClass());
					setterMethod.invoke(beanToPopulate, managedSource);
					return true;
				}				
			}
		}
		return false; // Means sourceEntity is detached and has no primary key set, which
					  // probably a new entity, which is an unaccounted use-case scenario for now.
	}

	public <T> void populateCollection(Object parentEntity, Method getterMethod, Collection<T> sourceCollection) throws Exception {		
		EntityCollection collection = new EntityCollection(this, ignoreEmpties);
		collection.populateCollection(parentEntity, getterMethod, sourceCollection);
	}
	
	/**
	 * The provided entity may not be managed by the entity manager. If so, get a managed copy from a lookup based on the primary key value.
	 * @param entity
	 * @return
	 * @throws Exception
	 */
	public Object getManagedEntity(Object entity) throws Exception {
		Object mangedEntity = null;
		if(em.contains(entity)) {
			mangedEntity = entity;
		}
		else {
			Object id = (new EntityInspector(entity)).getPrimaryKeyValue();
			if(id != null) {
				mangedEntity = em.find(entity.getClass(), id);
			}
		}
		return mangedEntity;
	}
	
	public void checkMerge(Object o) {
		if((new EntityInspector(o)).isEntity()) {
			if(!em.contains(o)) {
				em.merge(o);
			}
		}
	}

	public void rollback() {
		if(em == null)
			return;
	    if(em.isOpen()) {
            if(em.isJoinedToTransaction()) {
                EntityTransaction tx = em.getTransaction();
                if(tx.isActive()) {
                    if(!tx.getRollbackOnly()) {
                        System.out.println("About to rollback transaction, but transaction is already set to rollbackOnly");
                    }
                    try {
                    	System.out.println("Entity populator rolling back!!!");
                        tx.rollback();
                    }
                    catch( Exception e ) {
                       e.printStackTrace(System.out);
                    }
                }
            }
	    }
	}

	public boolean isTransactionActive() {
		if(em != null && em.isOpen()) {
			if(em.isJoinedToTransaction()) {
                EntityTransaction tx = em.getTransaction();
                if(tx.isActive()) {
                    if(!tx.getRollbackOnly()) {
                    	return true;
                    }
                }
			}
		}
		return false;
	}

	public EntityManager getEntityManager() {
		return em;
	}
	
	
}
