package edu.bu.ist.apps.kualiautomation.entity.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import edu.bu.ist.apps.kualiautomation.util.ReflectionUtils;

/**
 * This class wraps and entity class and provides for reflective analysis on its fields, methods and annotations
 * 
 * @author whennemuth
 *
 */
public class EntityInspector {
	
	private Object inspectableObj;
	private Class<?> inspectableClass;
	private Class<?> primaryKeyType;
	
	public EntityInspector(Object inspectableObj) {
		this.inspectableObj = inspectableObj;
		this.inspectableClass = inspectableObj.getClass();
	}
	
	public EntityInspector(Class<?> inspectableClass) {
		this.inspectableClass = inspectableClass;
	}

	public boolean isEntity() {
		return inspectableClass.getAnnotation(Entity.class) != null;
	}

	public List<Method> getEntitiesAccessors() {
		List<Method> accessors = new ArrayList<Method>();
		Method[] methods = inspectableClass.getMethods();
		for(Method method : methods) {
			if(ReflectionUtils.isAccessor(method)) {
				EntityInspector inspector = new EntityInspector(method.getReturnType());
				if(inspector.isEntity() && inspector.hasPrimaryKey()) {
					String setterName = method.getName().replaceFirst("get", "set");
					Class<?> entityParm = inspector.getEntityParm(setterName);
					if(entityParm != null) {
						accessors.add(method);
					}
				}
			}
		}
		return accessors;
	}
	
	/**
	 * Iterate over the entity type being inspected for a specified setter method and return its 
	 * parameter if it is an entity.
	 * @param setterMethodName
	 * @return
	 */
	public Class<?> getEntityParm(String setterMethodName) {
		String setterName = ReflectionUtils.getMutatorName(setterMethodName);
		Method[] methods = inspectableClass.getMethods();
		for(Method method : methods) {
			if(method.getName().equals(setterName)) {
				if(method.getParameterTypes().length == 1) {
					Class<?> parmClass = method.getParameterTypes()[0];
					EntityInspector parm = new EntityInspector(parmClass);
					if(parm.isEntity()) {
						return parmClass;
					}
					return null;
				}
			}
		}
		return null;
	}
	
	/**
	 * Indicate if the entity type being inspected has a field or method annotated 
	 * with @Id (entity primary key).
	 * @return
	 */
	public boolean hasPrimaryKey() {
		if(primaryKeyType == null) {
			primaryKeyType = getPrimaryKeyType();
		}
		return primaryKeyType != null;
	}
	
	/**
	 * Get the type (or return type) of the field or method inside the entity type being inspected 
	 * that is annotated with @Id (entity primary key).
	 * @return
	 */
	public Class<?> getPrimaryKeyType() {
		if(primaryKeyType == null) {
			/**
			 * TODO: Add @EmbeddedId as a primary key annotation.
			 */
			primaryKeyType = getAnnotatedMemberType(Id.class);
		}
		return primaryKeyType;
	}

	public Field getPrimaryKeyField() {
		return getAnnotatedField(inspectableClass, Id.class);
	}
	
	public Object getPrimaryKeyValue() throws Exception {
		if(inspectableObj == null)
			return null;
		Field idFld = getPrimaryKeyField();
		return getValue(idFld);
	}

	public Object getValue(Field f) throws Exception {
		if(f == null)
			return null;
		try {
			Object id = f.get(inspectableObj);
			return id;
		} 
		catch (IllegalAccessException e) {
			// The id field is private, so try the getter
			Object id = ReflectionUtils.getAccessorValue(inspectableObj, f.getName());
			return id;
		}
	}
	
	/**
	 * Iterate over fields and methods of the 
	 * @param annotationClass
	 * @return
	 */
	public Class<?> getAnnotatedMemberType(Class<?> annotationClass) {
		Field f = getAnnotatedField(inspectableClass, (Class<Annotation>) annotationClass);
		if(f == null) {
			Method m = getAnnotatedMethod(inspectableClass, (Class<Annotation>) annotationClass);
			if(m == null) {
				return null;
			}
			else {
				return m.getReturnType() == null ? null : m.getReturnType();
			}
		}
		else {
			return f.getType();
		}
	}
	
	/**
	 * Search a specified class for a Field that is annotated with the specified annotation.
	 * Search up through the super-classes as well.
	 * @param clazz
	 * @param annotationClass
	 * @return
	 */
	private Field getAnnotatedField(Class<?> clazz, Class<?> annotationClass) {
		for(Field f : clazz.getDeclaredFields()) {
			Annotation a = f.getAnnotation((Class<Annotation>) annotationClass);
			if(a != null && a.annotationType().equals(annotationClass)) {
				return f;
			}
		}
		if(clazz.getSuperclass() != null && clazz.getSuperclass().equals(Object.class) == false) {
			return getAnnotatedField(clazz.getSuperclass(),  annotationClass);
		}
		return null;
	}
	
	public List<Field> getAnnotatedFields(Class<?> clazz, Class<?> annotationClass) {
		List<Field> flds = new ArrayList<Field>();
		for(Field f : clazz.getDeclaredFields()) {
			@SuppressWarnings("unchecked")
			Annotation a = f.getAnnotation((Class<Annotation>) annotationClass);
			if(a != null && a.annotationType().equals(annotationClass)) {
				flds.add(f);
			}
		}
		if(clazz.getSuperclass() != null && clazz.getSuperclass().equals(Object.class) == false) {
			Field f = getAnnotatedField(clazz.getSuperclass(),  annotationClass);
			if(f != null) 
				flds.add(f);
		}
		return flds;
	}
	
	public List<Field> getOneToManyFields() {
		return getAnnotatedFields(inspectableClass, OneToMany.class);
	}

	public List<Field> getOneToOneFields() {
		return getAnnotatedFields(inspectableClass, OneToOne.class);
	}
	
	/**
	 * Search a specified class for a Method that is annotated with the specified annotation.
	 * Search up through the super-classes as well.
	 * @param clazz
	 * @param annotationClass
	 * @return
	 */
	private Method getAnnotatedMethod(Class<?> clazz, Class<?> annotationClass) {
		for(Method m : clazz.getMethods()) {
			Annotation a = m.getAnnotation((Class<Annotation>) annotationClass);
			if(a != null && a.annotationType().equals(annotationClass)) {
				return m;
			}
		}
		if(clazz.getSuperclass() != null && clazz.getSuperclass().equals(Object.class) == false) {
			return getAnnotatedMethod(clazz.getSuperclass(),  annotationClass);
		}
		return null;
	}
	
	/**
	 * Indicate if the entity being inspected has a specified annotation.
	 * @param annotationClass
	 * @return
	 */
	private boolean hasAnnotation(Class<Annotation> annotationClass) {
		Annotation a = inspectableClass.getAnnotation(annotationClass);
		return a != null;
	}
	
	public static boolean isEntity(Class<?> clazz) {
		if(clazz == null)
			return false;
		try {
			EntityInspector ei = new EntityInspector(clazz);
			return ei.isEntity();
		}
		catch(Exception e) {
			return false;
		}
	}
		
	public static boolean isEntity(Object o) {
		if(o == null)
			return false;
		try {
			EntityInspector ei = new EntityInspector(o);
			return ei.isEntity();
		}
		catch(Exception e) {
			return false;
		}
	}
	
	public static boolean returnsEntity(Method m) {
		if(m.getReturnType() == null)
			return false;
		return isEntity(m.getReturnType());
	}

	public static boolean returnsEntityCollection(Method getterMethod) {
		Type type = getCollectionType(getterMethod);
		Class<?> clazz = null;
		if(type == null) {
			return false;
		}
		else {
			try {
				clazz = Class.forName(type.getTypeName());
			} 
			catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			return isEntity(clazz);
		}
	}

	public static Type getCollectionType(Method getterMethod) {
		Type type = getterMethod.getGenericReturnType();
		if(getterMethod != null) {			
			if(type instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) type;
				if(pt.getActualTypeArguments().length == 1) {
					return pt.getActualTypeArguments()[0];
				}
			}
		}
		return null;
	}
}

