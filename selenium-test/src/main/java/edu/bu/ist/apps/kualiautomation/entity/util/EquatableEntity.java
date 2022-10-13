package edu.bu.ist.apps.kualiautomation.entity.util;

import java.lang.reflect.Field;

import edu.bu.ist.apps.kualiautomation.util.ReflectionUtils;

public abstract class EquatableEntity {
	private Object entity;
	
	/**
	 * This factory method is for an instance that will equate one entity with another 
	 * entity if they share the same primary key value (are the same entity).
	 * 
	 * @param em
	 * @param ignoreEmpties
	 */
	public static EquatableEntity getInstance(Object entity) {						
		return getInstance(entity, new ObjectMatcher() {
			@Override public boolean match(Object entity1, Object entity2) throws Exception {
				EntityInspector inspector1 = new EntityInspector(entity1);
				EntityInspector inspector2 = new EntityInspector(entity2);
				if(inspector1.hasPrimaryKey() && inspector2.hasPrimaryKey()) {
					if(inspector1.getPrimaryKeyType().isAssignableFrom(inspector2.getPrimaryKeyType())) {
						
						Object id1 = getPrimaryKeyValue(inspector1, entity1);
						if(id1 == null)
							return false;
						
						Object id2 = getPrimaryKeyValue(inspector2, entity2);
						if(id2 == null)
							return false;
						
						return id1.equals(id2);
					}
				}
				return false;
			}
		});
	}
	
	/**
	 * This factory method is for an instance that will equate one entity with another entity 
	 * if they share some common factor as determined by the supplied ObjectMatcher.
	 * 
	 * @param em
	 * @param ignoreEmpties
	 * @param matcher
	 */
	public static EquatableEntity getInstance(final Object entity, final ObjectMatcher matcher) {
		EquatableEntity equatable = new EquatableEntity() {
			@Override public boolean isEqualTo(EquatableEntity otherEquatable) {
				try {
					return matcher.match(entity, otherEquatable.getEntity());
				} 
				catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		};
		equatable.entity = entity;
		
		return equatable;
	}
	
	private static Object getPrimaryKeyValue(EntityInspector inspector, Object entity) throws Exception {
		Field fld = inspector.getPrimaryKeyField();
		Object pk = null;
		if(fld.isAccessible()) {
			pk = fld.get(entity);
		}
		else {
			pk = ReflectionUtils.getAccessorValue(entity, fld.getName());
		}
		return pk;
	}
	
	public abstract boolean isEqualTo(EquatableEntity otherEquatable);

	public Object getEntity() {
		return entity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EquatableEntity other = (EquatableEntity) obj;
		if (entity == null || other.entity == null) {
			return false;
		} 
		try {
			return this.isEqualTo(other);
		} 
		catch (Exception e) {
			return false;
		}
	}

	public static interface ObjectMatcher {
		boolean match(Object entity1, Object entity2) throws Exception;
	}

}

