package edu.bu.ist.apps.kualiautomation.services.automate;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;

public class Window {

	private WebDriver driver;
	private Set<Instance> instances = new LinkedHashSet<Instance>();
	private boolean newWindow;
	private boolean lostWindow;
	
	public Window(WebDriver driver) {
		this.driver = driver;
		for(String ref : driver.getWindowHandles()) {
			addInstance(ref);
		}
	}

	public void focus() {
		focus(true);
	}

	public void focus(boolean leaveFrame) {
		
		try {
System.out.println(this);			
			refresh();
			
			switchTo(leaveFrame);
		} 
		finally {
System.out.println(this);			
			newWindow = false;
			lostWindow = false;
		}
	}
	
	/**
	 * Add any new window references to instances set.
	 * Remove any closed window references from instances set.
	 */
	private void refresh() {
		if(driver.getWindowHandles().size() != instances.size()) {
			for(String ref : driver.getWindowHandles()) {
				if(!instances.contains(new Instance(ref))) {					
					addInstance(ref);
					newWindow = true;
				}				
			}
			for (Iterator<Instance> iterator = instances.iterator(); iterator.hasNext();) {
				Instance instance = (Instance) iterator.next();
				if(!driver.getWindowHandles().contains(instance.getReference())) {
					iterator.remove();
					lostWindow = true;
				}
			}
			
		}
	}
	
	/**
	 * Switch to any new window that has been detected, or switch to a prior window if one seems to have disappeared from
	 * the list of window references being kept track of.
	 * 
	 * @param leaveFrame
	 */
	private void switchTo(boolean leaveFrame) {
		if(newWindow || lostWindow) {
			Instance last = getLast();
			if(!last.isCurrent(driver)) {
				driver.switchTo().window(last.getReference());
			}
		}
		if(leaveFrame) {
			driver.switchTo().defaultContent();
		}
	}


	private void addInstance(String ref) {
		Instance i = new Instance(ref);
		instances.add(i);		
	}
	
	private Instance getLast() {
		if(instances.isEmpty())
			return null;
		Instance[] array = instances.toArray(new Instance[instances.size()-1]);
		return array[array.length-1];
	}

	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Window [driver=").append(driver).append(", instances=[");
		for(Instance i : instances) {
			builder.append("\n    ").append(i);
		}
		builder.append("\n]");
		return builder.toString();
	}


	private class Instance {
		private String reference;
		public Instance(String reference) {
			this.reference = reference;
		}
		public String getReference() {
			return reference;
		}
		public boolean isCurrent(WebDriver driver) {
			try {
				return driver.getWindowHandle().equals(reference);
			} 
			catch (NoSuchWindowException e) {
				System.out.println("Window (handle=" + reference + ") is closed!");
				return false;
			}
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((reference == null) ? 0 : reference.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Instance))
				return false;
			Instance other = (Instance) obj;
			if (reference == null) {
				if (other.reference != null)
					return false;
			} else if (!reference.equals(other.reference))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Instance [reference=").append(reference).append(", isCurrent(driver)=").append(isCurrent(driver)).append("]");
			return builder.toString();
		}
	}
}
