package edu.bu.ist.ci.test;

import static org.junit.Assert.fail;

/**
 * This class provides a convenience method for asserting that the implemented method runs code that
 * triggers the specified exception with the specified message.
 * 
 * @author wrh
 *
 */
public abstract class ExceptionTester {
	
		public abstract void runCode() throws Exception;
		
		public void Assert(Class<? extends Throwable> clazz, String msg) {
			try {
				runCode();
				fail(msg);
			}
			catch(Throwable e) {
				String class1 = clazz.getName();
				String class2 = e.getClass().getName();
				if(!class1.equals(class2)) {
					fail(msg);
				}
//				if(!clazz.getClass().isAssignableFrom(e.getClass())) {
//					fail(msg);
//				}
//				if(!clazz.getClass().isInstance(e)) {
//					fail(msg);
//				}
			}
		}
	}