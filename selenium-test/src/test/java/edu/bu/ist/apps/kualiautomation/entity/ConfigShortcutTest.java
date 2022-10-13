package edu.bu.ist.apps.kualiautomation.entity;

import static org.junit.Assert.*;

import org.junit.Test;

//import edu.bu.ist.apps.kualiautomation.entity.ConfigShortcut.LabelHierarchy;

public class ConfigShortcutTest {

	@Test
	public void testGetLabelHierarchyObject() {
		
//		ConfigShortcut shortcut = new ConfigShortcut();
//		LabelHierarchy hierarchy = shortcut.getLabelHierarchyObject();
//		assertNull(hierarchy);
//		
//		shortcut.setLabelHierarchy("");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertNull(hierarchy);
//		
//		shortcut.setLabelHierarchy(" ");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertNull(hierarchy);
//		
//		shortcut.setLabelHierarchy(shortcut.getSeparator());
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertNull(hierarchy);
//		
//		shortcut.setLabelHierarchy("apples");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNull(hierarchy.getChildHierachy());
//
//		shortcut.setLabelHierarchy("apples " + shortcut.getSeparator());
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNull(hierarchy.getChildHierachy());
//
//		shortcut.setLabelHierarchy("apples " + shortcut.getSeparator() + "   ");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNull(hierarchy.getChildHierachy());
//
//		shortcut.setLabelHierarchy("apples" + shortcut.getSeparator() + "oranges");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNotNull(hierarchy.getChildHierachy());
//		assertEquals("oranges", hierarchy.getChildHierachy().getLabel());
//		assertNull(hierarchy.getChildHierachy().getChildHierachy());
//
//		shortcut.setLabelHierarchy(" apples " + shortcut.getSeparator() + " oranges ");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNotNull(hierarchy.getChildHierachy());
//		assertEquals("oranges", hierarchy.getChildHierachy().getLabel());
//		assertNull(hierarchy.getChildHierachy().getChildHierachy());
//
//		shortcut.setLabelHierarchy(" apples " + shortcut.getSeparator() + " oranges " + shortcut.getSeparator());
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNotNull(hierarchy.getChildHierachy());
//		assertEquals("oranges", hierarchy.getChildHierachy().getLabel());
//		assertNull(hierarchy.getChildHierachy().getChildHierachy());
//
//		shortcut.setLabelHierarchy(" apples " + shortcut.getSeparator() + " oranges " + shortcut.getSeparator() + "   ");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNotNull(hierarchy.getChildHierachy());
//		assertEquals("oranges", hierarchy.getChildHierachy().getLabel());
//		assertNull(hierarchy.getChildHierachy().getChildHierachy());
//
//		shortcut.setLabelHierarchy("apples" + shortcut.getSeparator() + "oranges" + shortcut.getSeparator() + "pears");
//		hierarchy = shortcut.getLabelHierarchyObject();
//		assertEquals("apples", hierarchy.getLabel());
//		assertNotNull(hierarchy.getChildHierachy());
//		assertEquals("oranges", hierarchy.getChildHierachy().getLabel());
//		assertNotNull(hierarchy.getChildHierachy().getChildHierachy());
//		assertEquals("pears", hierarchy.getChildHierachy().getChildHierachy().getLabel());
//		assertNull(hierarchy.getChildHierachy().getChildHierachy().getChildHierachy());
	}

	@Test
	public void testGetLabelHierarchy() {		
//		ConfigShortcut shortcut = new ConfigShortcut();
//		assertNull(shortcut.getLabelHierarchy());
//		
//		shortcut.setLabelHierarchyObject(new LabelHierarchy());
//		assertNull(shortcut.getLabelHierarchy());
//		
//		shortcut.setLabelHierarchyObject(new LabelHierarchy(""));
//		assertNull(shortcut.getLabelHierarchy());
//		
//		shortcut.setLabelHierarchyObject(new LabelHierarchy("  "));
//		assertNull(shortcut.getLabelHierarchy());
//		
//		shortcut.setLabelHierarchyObject(new LabelHierarchy("apples"));
//		assertEquals("apples", shortcut.getLabelHierarchy());
//		
//		shortcut.setLabelHierarchyObject(new LabelHierarchy("  apples  "));
//		assertEquals("apples", shortcut.getLabelHierarchy());
//		
//		LabelHierarchy hierarchy = new LabelHierarchy("apples");
//		hierarchy.setChildHierachy(new LabelHierarchy());
//		shortcut.setLabelHierarchyObject(hierarchy);
//		assertEquals("apples", shortcut.getLabelHierarchy());
//		
//		hierarchy.setChildHierachy(new LabelHierarchy(""));
//		shortcut.setLabelHierarchyObject(hierarchy);
//		assertEquals("apples", shortcut.getLabelHierarchy());
//		
//		hierarchy.setChildHierachy(new LabelHierarchy("   "));
//		shortcut.setLabelHierarchyObject(hierarchy);
//		assertEquals("apples", shortcut.getLabelHierarchy());
//		
//		hierarchy.setChildHierachy(new LabelHierarchy("oranges"));
//		shortcut.setLabelHierarchyObject(hierarchy);
//		assertEquals("apples" + shortcut.getSeparator() + "oranges", shortcut.getLabelHierarchy());
//		
//		hierarchy.setChildHierachy(new LabelHierarchy("  oranges  "));
//		shortcut.setLabelHierarchyObject(hierarchy);
//		assertEquals("apples" + shortcut.getSeparator() + "oranges", shortcut.getLabelHierarchy());
//		
//		LabelHierarchy hierarchy2 = new LabelHierarchy("oranges");
//		hierarchy2.setChildHierachy(new LabelHierarchy());
//		hierarchy.setChildHierachy(hierarchy2);		
//		shortcut.setLabelHierarchyObject(hierarchy);
//		assertEquals("apples" + shortcut.getSeparator() + "oranges", shortcut.getLabelHierarchy());
//		
//		hierarchy2 = new LabelHierarchy("oranges");
//		hierarchy2.setChildHierachy(new LabelHierarchy("pears"));
//		hierarchy.setChildHierachy(hierarchy2);		
//		shortcut.setLabelHierarchyObject(hierarchy);
//		assertEquals("apples" + shortcut.getSeparator() + "oranges" + shortcut.getSeparator() + "pears", shortcut.getLabelHierarchy());
	}

}
