package edu.bu.ist.apps.kualiautomation.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import edu.bu.ist.apps.kualiautomation.entity.util.EntityInspector;

public class ReflectionUtils {
	
	/**
	 * Convert a field name to the standard mutator method name, ie: "name" or "getName" becomes "setName"
	 * @param fldName
	 * @return
	 */
	public static String getMutatorName(String fldName) {
		if(fldName == null)
			return null;
		String setter = null;
		if(fldName.matches("((get)|(is))[A-Z].*")) {
			setter = new String(fldName.replaceFirst("get", "set"));
		}
		else if(fldName.matches("(set)[A-Z].*")) {
			setter = new String(fldName);
		}
		else {
			setter = new String("set" + fldName.substring(0, 1).toUpperCase() + fldName.substring(1));
		}
		return setter;
	}
	
	/**
	 * Determine if given method is a mutator.
	 * The method must have one parameter and its name must be of either get[Xxxx...] or is[Xxxx...] formats
	 * @param setterMethod
	 * @return
	 */
	public static boolean isAccessor(Method m) {
		if(m == null)
			return false;
		if(m.getReturnType() == null)
			return false;
		if(!isAccessorName(m.getName())) 
			return false;
		if(m.getParameterTypes().length != 0)
			return false;
		
		return true;
	}
	
	public static boolean isAccessorName(String methodName) {
		if(methodName == null)
			return false;
		if(!methodName.matches("((get)|(is))[A-Z].*")) 
			return false;
		return true;
	}
	
	/**
	 * From the name of an accessor or a mutator, get the name of the target field (remove "set" or "get" and lowercase the first character of the result)
	 * @param methodName
	 * @return
	 */
	public static String getFieldName(String methodName) {
		return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
	}
		
	public static String getAccessorName(String fldName, boolean isBoolean) {
		String prefix = isBoolean ? "is" : "get";
		if(fldName == null)
			return null;
		String retval = null;
		if(fldName.matches("(set)[A-Z].*")) {
			retval = new String(fldName.replaceFirst("set", "get"));
		}
		else if(fldName.matches("(" + prefix + ")[A-Z].*")) {
			retval = new String(fldName);
		}
		else {
			retval = new String(prefix + fldName.substring(0, 1).toUpperCase() + fldName.substring(1));
		}
		return retval;
	}
	
	public static Object getAccessorValue(Object o, String fldName) throws Exception {
		return getAccessorValue(o, fldName, false);
	}
	
	/**
	 * Get the value of one of the accessors of an object identified by its private field name.
	 * IE: fldName = "firstName" returns o.getFirstName().
	 * 
	 * @param o
	 * @param fldName
	 * @param ignoreFldCase
	 * @return
	 * @throws Exception
	 */
	public static Object getAccessorValue(Object o, String fldName, boolean ignoreFldCase) throws Exception {
		if(fldName == null || o == null)
			return null;
		Object val = null;
		boolean isBoolean = false;
		Field fld = getField(fldName, o.getClass());
		if(fld != null)
			isBoolean = fld.getType().getName().toLowerCase().matches("boolean");
		String methodName = getAccessorName(fldName, isBoolean);
		Method method = getMethod(methodName, o.getClass(), true);
		if(method != null) {
			val = method.invoke(o);
		}
		return val;
	}
	
	public static Method getAccessorMethod(Object o, Field fld) {
		if(o == null || fld == null)
			return null;
		boolean isBoolean = fld.getClass().getSimpleName().toLowerCase().matches("boolean");
		String methodName = getAccessorName(fld.getName(), isBoolean);
		return getMethod(methodName, o.getClass());
	}
	
	/**
	 * This method is an alternative to using the getMethod(String name, Class... parameterTypes) method
	 * if the name is known, only one parameter is expected but its Class is unknown.
	 * 
	 * @param name
	 * @param mutatorClass
	 * @return
	 */
	public static Method getMutatorMethod(String name, @SuppressWarnings("rawtypes") Class mutatorClass) {
		Method m = getMethod(name, mutatorClass);
		if(m.getParameterTypes().length == 1) {
			return m;
		}
		return null;
	}
	
	public static Method getMutator(String name, @SuppressWarnings("rawtypes") Class mutatorClass) {
		name = getMutatorName(name);
		return getMutatorMethod(name, mutatorClass);
	}
	
	/**
	 * Get the name of the corresponding mutator method given the accessor method.
	 * The accessor must have no parameters and must be of either get[Xxxx...] or is[Xxxx...] formats.
	 * The mutator must follow the set[Xxxx...] naming format and must have one parameter.
	 * 
	 * @param setterMethod
	 * @return
	 */
	public static Method getMutator(Method getterMethod, @SuppressWarnings("rawtypes") Class mutatorClass) {
		
		if(!isAccessor(getterMethod))
			return null;
		
		String setterName = null;
		if("boolean".equalsIgnoreCase(getterMethod.getReturnType().getSimpleName())) {
			setterName = getterMethod.getName().replaceFirst("is", "set");
			if(setterName.startsWith("get")) {
				setterName = setterName.replaceFirst("get", "set");
			}
		}
		else {
			setterName = getterMethod.getName().replaceFirst("get", "set");
		}
		try {
			Method mutator = getMutatorMethod(setterName, mutatorClass);
			if(mutator.getParameterTypes().length != 1) {
				return null;
			}
			return mutator;
		} 
		catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Obtains and "add" or "remove" mutator method.
	 * @param parent the object that has the mutator.
	 * @param getterMethod Can be an accessor that returns an instance of type required by add OR a collection of same type.
	 * @param action "add" or "remove"
	 * @return
	 * @throws Exception
	 */
	public static Method getMutator(Object parent, Method getterMethod, String action) throws Exception {
		String methodName = getterMethod.getName().replaceFirst("get", action);
		if(methodName.endsWith("s")) {
			methodName = methodName.substring(0, methodName.length() - 1);
		}
		Method method = getMethod(methodName, parent.getClass());
		Type collectionType = EntityInspector.getCollectionType(getterMethod);
		Class<?> collectionClass = Class.forName(collectionType.getTypeName());
		Type adderParmType = method.getParameterTypes()[0];
		Class<?> adderParmClass = Class.forName(adderParmType.getTypeName());
		if(!collectionClass.equals(adderParmClass)) {
			// failed check that adder method must take a class that is equal to the one returned by the getter method
			method = null;
		}
		
		return method;
	}

	/**
	 * This method is an alternative to using the getMethod(String name, Class... parameterTypes) method
	 * 
	 * @param methodName
	 * @param clazz
	 * @return
	 */
	public static Method getMethod(String methodName, @SuppressWarnings("rawtypes") Class clazz) {
		return getMethod(methodName, clazz, false);
	}
	
	public static Method getMethod(String methodName, @SuppressWarnings("rawtypes") Class clazz, boolean ignorecase) {
		Method[] methods = clazz.getMethods();
		for(Method m : methods) {
			if(m.getName().equals(methodName) || (ignorecase && m.getName().equalsIgnoreCase(methodName))) {
				return m;
			}
		}
		return null;
	}
	
	public static Field getField(String fldname, Class<?> clazz) {
		for(Field f : clazz.getDeclaredFields()) {
			if(f.getName().equals(fldname)) {
				return f;
			}
		}
		if(clazz.getSuperclass() != null && clazz.getSuperclass().equals(Object.class) == false) {
			return getField(fldname, clazz.getSuperclass());
		}
		return null;
	}
	
	public static boolean isEmpty(Object o, String fldName) throws Exception {
		Object value = getAccessorValue(o, fldName);
		return Utils.isEmpty(value);
	}

}
