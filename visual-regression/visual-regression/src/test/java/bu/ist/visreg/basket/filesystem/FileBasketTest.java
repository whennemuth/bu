package bu.ist.visreg.basket.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import bu.ist.visreg.BackstopSplitterMocker;
import bu.ist.visreg.backstop.BackstopJson;
import bu.ist.visreg.backstop.Scenario;
import bu.ist.visreg.backstop.ViewPort;
import bu.ist.visreg.basket.Basket;
import bu.ist.visreg.basket.BasketItem;
import bu.ist.visreg.util.FileUtils;

@RunWith(MockitoJUnitRunner.Silent.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileBasketTest {

	@Mock private File rootDirectory;
	@Mock private FileBasketSystem fileBasketSystem;
	
	private FileUtils basketFolderMock;
	private FileBasket fb;
	private int mkdirAttempts;

	private void setFileBasket() {
		setFileBasket(true);
	}
	
	/**
	 * Establish an instance of FileBasket that will be under test.
	 * Set a mock of FileUtils to the instance so that file/directory operations can be substituted for. 
	 * @param exists
	 */
	private void setFileBasket(boolean exists) {
		mkdirAttempts = 0;
		basketFolderMock = Mockito.mock(FileUtils.class);
		String abspath = "/mock/basket/folder/absolute/path/";
		when(basketFolderMock.isDirectory()).thenAnswer(new Answer<Boolean>() {
			@Override public Boolean answer(InvocationOnMock invocation) throws Throwable {
				boolean retval = mkdirAttempts==0 ? exists : true;
				return retval;
			}			
		});
		when(basketFolderMock.mkdirs()).thenAnswer(new Answer<Boolean>() {
			@Override public Boolean answer(InvocationOnMock invocation) throws Throwable {
				mkdirAttempts++;
				return true;
			}			
		});
		when(basketFolderMock.getAbsolutePath()).thenReturn(abspath);
		fb = new FileBasket(Basket.BasketEnum.INBOX, fileBasketSystem);
		fb.setFolder(basketFolderMock);		
	}
	
	/**
	 * Get a list of BackstopJson instances generated as member collection of the FileBasket under test by invoking its load method.
	 * Sort the list so that the order can be predicted and the right assertions are made against the right list members.
	 * 
	 * @param jsonFileName
	 * @param itemsSize
	 * @return
	 * @throws Exception
	 */
	private List<BackstopJson> getBackstopJsonInstances(int itemsSize, String... jsonFileNames) throws Exception {
		setFileBasket();
		List<File> backstopJsonMocks = new ArrayList<File>();
		for(String jsonFileName : jsonFileNames) {
			File backstopJsonMock = Mockito.mock(File.class);
			when(backstopJsonMock.isFile()).thenReturn(true);
			when(backstopJsonMock.getAbsolutePath()).thenReturn("/" + jsonFileName);
			backstopJsonMocks.add(backstopJsonMock);
			String backstopJsonString = FileUtils.getClassPathResourceContent("job-definitions/" + jsonFileName);
			FileUtils backstopJsonUtils = Mockito.mock(FileUtils.class);
			when(backstopJsonUtils.readFile()).thenReturn(backstopJsonString);
			when(basketFolderMock.newInstance(same(backstopJsonMock))).thenReturn(backstopJsonUtils);
		}
		when(basketFolderMock.listFiles()).thenReturn(
			backstopJsonMocks.toArray(new File[backstopJsonMocks.size()])
		);
		
		fb.load(BackstopSplitterMocker.getInstance());			
		List<BasketItem> items = fb.getBasketItems();
		assertEquals(itemsSize, items.size());

		List<BackstopJson> backstops = new ArrayList<BackstopJson>(items.size());
		for(int i=0; i<items.size(); i++) {
			String content = items.get(i).getContent();
			assertNotNull(content);
			backstops.add(i, BackstopJson.getInstance(content));				
		}
		
		// Order the backstopJson collection by the labels of the Scenario in their single member scenarios collection.
		backstops.sort(new Comparator<BackstopJson>() {
			@Override public int compare(BackstopJson bs1, BackstopJson bs2) {
				return bs1.getScenarios().get(0).getLabel().compareTo(bs2.getScenarios().get(0).getLabel());
			}				
		});

		return backstops;
	}
	
	/**
	 * Make sure that attempts to create the physical directory upon which the FileBasket instance is 
	 * based if the FileUtils mock indicates that it does not already exist. 
	 */
	@Test
	public void testCreateIfNotExists() {		
		try {
			setFileBasket(false);	
			fb.createIfNotExists();			
			verify(basketFolderMock, times(1)).mkdirs();

			setFileBasket(true);	
			fb.createIfNotExists();
			verify(basketFolderMock, never()).mkdirs();
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Not expecting exception");
		}
	}
	
	/**
	 * Provide a single BasketItem instance (based on a BackstopJson file) to an instance of FileBasket.
	 * Test that the load method properly splits this item into the correct number of smaller items, asserting the field values of each.
	 * This test covers the "inheritance", "overriding", and default value scenarios (See: {@link bu.ist.visreg.job.JobDefinition JobDefinition}).
	 */
	@Test
	public void testLoad() {
		try {
			List<BackstopJson> backstops = getBackstopJsonInstances(3, "JobDefinitionThreeScenarios.json");
						
			int counter = 1;
			for(BackstopJson bj : backstops) {
				assertTrue(bj.isValid());
				assertEquals(bj.getId(), "MyJob");
				assertNotNull(bj.getScenarios());
				assertEquals(1, bj.getScenarios().size());
				Scenario scenario = bj.getScenarios().get(0);
				assertTrue(scenario.isValid());
				assertEquals("scenario.label"+String.valueOf(counter), scenario.getLabel());
				assertEquals("backstop_data/engine_scripts/cookies.json", scenario.getCookiePath());
				switch(counter) {
				case 1:
					assertEquals("https://ref-domain.bu.edu/main-test.htm", scenario.getUrl());
					assertEquals("https://ref-domain.bu.edu/main.htm", scenario.getReferenceUrl());
					assertEquals("https://ref-domain.bu.edu/login.htm", scenario.getLoginUrl());
					break;
				case 2:
					assertEquals("https://ref-domain.bu.edu/some/other/page-test.htm", scenario.getUrl());
					assertEquals("https://ref-domain.bu.edu/some/other/page.htm", scenario.getReferenceUrl());
					assertEquals("https://login/url/inherited/from/JobDefinition/login.htm", scenario.getLoginUrl());
					break;
				case 3:
					assertEquals("https://ref-domain.bu.edu/another/web/page-test.htm", scenario.getUrl());
					assertEquals("https://ref-domain.bu.edu/another/web/page.htm", scenario.getReferenceUrl());
					assertEquals("https://login/url/inherited/from/JobDefinition/login.htm", scenario.getLoginUrl());
					break;
				}
				
				counter++;
			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Not expecting exception");
		}
	}

	/**
	 * Provide a single BasketItem instance (based on a BackstopJson file) to an instance of FileBasket.
	 * Test that the load method properly splits this item into the correct number of smaller items, asserting the field values of each.
	 * This test covers the "overriding", and default value scenarios, with nothing to "inherit" (See: {@link bu.ist.visreg.job.JobDefinition JobDefinition}).
	 */
	@Test
	public void testLoadNoInheritance() {
		try {
			List<BackstopJson> backstops = getBackstopJsonInstances(1, "JobDefinitionOneScenarioNoInheritance.json");
			
			BackstopJson bj = backstops.get(0);
			
			// 1) Assert values of some of the attributes
			assertEquals(bj.getId(), "MyJob");
			assertEquals(new Integer(3), bj.getAsyncCaptureLimit());
			
			// 2) Assert length and content of the viewports
			assertEquals(2, bj.getViewports().length);
			ViewPort[] viewports = bj.getViewports().clone();
			Arrays.sort(viewports, new Comparator<ViewPort>() {
				@Override public int compare(ViewPort v1, ViewPort v2) {
					return v1.getLabel().compareTo(v2.getLabel());
				}				
			});
			assertEquals("laptop", viewports[0].getLabel());
			assertEquals(new Integer(900), viewports[0].getHeight());
			assertEquals(new Integer(1440), viewports[0].getWidth());
			assertEquals("smartphone", viewports[1].getLabel());
			assertEquals(new Integer(100), viewports[1].getHeight());
			assertEquals(new Integer(60), viewports[1].getWidth());
			
			// 3) Assert the scenarios length and values
			assertNotNull(bj.getScenarios());
			assertEquals(1, bj.getScenarios().size());
			Scenario scenario = bj.getScenarios().get(0);
			assertEquals("label1", scenario.getLabel());
			assertEquals("https://ref-domain.bu.edu/main-test.htm", scenario.getUrl());
			assertEquals("https://ref-domain.bu.edu/main.htm", scenario.getReferenceUrl());
			assertEquals("https://ref-domain.bu.edu/login.htm", scenario.getLoginUrl());
			assertEquals("computer", scenario.getViewports()[0].getLabel());
			assertEquals(new Integer(1000), scenario.getViewports()[0].getHeight());
			assertEquals(new Integer(1500), scenario.getViewports()[0].getWidth());
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Not expecting exception");
		}
	}
	
	/**
	 * Provide a variety of BasketItem instances to load into the FileBasket whose concrete implementations will indicate that their contents
	 * each do not pass one or more validation checks. Test that the load function keeps these invalid items out of the basket.
	 */
	@Test
	public void testLoadInvalid() {
		try {
			List<BackstopJson> backstops = getBackstopJsonInstances(0, "JobDefinitionOneInvalidAttribute.json");
			assertEquals(1, fb.getRejectedBasketItems());
			
			backstops = getBackstopJsonInstances(0, "JobDefinitionOneInvalidScenario.json");			
			assertEquals(1, fb.getRejectedBasketItems());
			
			backstops = getBackstopJsonInstances(0, "JobDefinitionMixedValidity.json");			
			assertEquals(1, fb.getRejectedBasketItems());
			
			backstops = getBackstopJsonInstances(1, "JobDefinitionOneScenario.json");	
			assertTrue(backstops.get(0).isValid());
			assertEquals(1, backstops.get(0).getScenarios().size());
			assertTrue(backstops.get(0).getScenarios().get(0).isValid());
			assertEquals(0, fb.getRejectedBasketItems());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Not expecting exception");
		}
	}
	
	@Test
	public void testLoadMultipleBasketItems() {
		try {
			List<BackstopJson> backstops = getBackstopJsonInstances(
					5, 
					"JobDefinitionOneScenario.json",
					"JobDefinitionOneScenarioNoInheritance.json",
					"JobDefinitionThreeScenarios.json");			
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Not expecting exception");
		}
	}
}
