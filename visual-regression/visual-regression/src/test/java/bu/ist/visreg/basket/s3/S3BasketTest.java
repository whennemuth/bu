package bu.ist.visreg.basket.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import bu.ist.visreg.BackstopSplitterMocker;
import bu.ist.visreg.backstop.EngineOptions;
import bu.ist.visreg.backstop.Paths;
import bu.ist.visreg.backstop.Scenario;
import bu.ist.visreg.backstop.ViewPort;
import bu.ist.visreg.basket.BasketItem;
import bu.ist.visreg.basket.Basket.BasketEnum;
import bu.ist.visreg.util.FileUtils;
import bu.ist.visreg.util.TestUtils;
import software.amazon.awssdk.services.s3.model.S3Object;

@RunWith(MockitoJUnitRunner.Silent.class)
public class S3BasketTest {

	@Mock private S3Bucket bucket;
	@Mock private S3BasketSystem s3BasketSystem;
	/**
	 * SEE: https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2#mock-the-unmockable-opt-in-mocking-of-final-classesmethods
	 * S3Object is a final class, and so must be mocked as follows (see above link):
	 */
	S3Object subfolder = S3Object.builder().build();
	S3Object subfolderMock = mock(S3Object.class);
	S3Object folderItem = S3Object.builder().build();
	S3Object folderItemMock = mock(S3Object.class);
	
	private static BasketEnum inbox = BasketEnum.INBOX;	
	private static final String BUCKET_NAME = "MyS3Bucket";	
	private S3Basket sb;
	
	@Before
	public void setup() {
		when(s3BasketSystem.getRootLocation()).thenReturn(BUCKET_NAME);
		when(subfolderMock.key()).thenReturn(inbox.getBasketRelativeLocation());
		// Return nothing first (means S3 bucket does not exit), this invokes the bucket creation path.
		// Then return the subfolder (means the S3 bucket was created).
		when(bucket.getSubFolder(subfolderMock.key()))
			.thenReturn(null)
			.thenReturn(subfolderMock);
		when(bucket.getS3Object(subfolderMock.key()))
			.thenReturn(subfolderMock);
		when(s3BasketSystem.getBucket()).thenReturn(bucket);
	}
	
	@Test
	public void testCreateIfNotExists() throws Exception {		
		sb = new S3Basket(inbox, s3BasketSystem);
		try {
			sb.createIfNotExists();
			verify(bucket, times(1)).createSubfolder(inbox.getBasketRelativeLocation());
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Not expecting exception");
		}
	}
	
	@Test
	public void testLoad3Scenarios() {
		sb = new S3Basket(inbox, s3BasketSystem);
		try {
			sb.createIfNotExists();
			
			String objectKey = inbox.getBasketRelativeLocation() + "JobDefinitionThreeScenarios.json";
			String jobdef = FileUtils.getClassPathResourceContent("job-definitions/JobDefinitionThreeScenarios.json");			
			Map<String, S3Object> folderItems = new HashMap<String, S3Object>();
			
			when(folderItemMock.key()).thenReturn(objectKey);
			when(bucket.downloadAsString(objectKey)).thenReturn(jobdef);			
			when(bucket.getS3Objects()).thenReturn(folderItems);
			
			folderItems.put(objectKey, folderItemMock);
			
			sb.load(BackstopSplitterMocker.getInstance());
			
			List<BasketItem> items = sb.getBasketItems();
			
			// The original JobDefinition BasketItem should have been split into 3 separate BackstopJson BasketItem instances.
			// Each new BasketItem should be persisted and the original parent BasketItem deleted.
			assertEquals(3, items.size());
			verify(items.get(0), times(1)).persist();
			verify(items.get(1), times(1)).persist();
			verify(items.get(2), times(1)).persist();
			verify(bucket, times(1)).deleteObject(objectKey);
			
			// The new BasketItems should have names that incorporate the scenarios on which they are based.
			assertEquals("jobs/inbox/JobDefinitionThreeScenarios_MyJob_scenario.label1.json", items.get(0).getPathname());
			assertEquals("jobs/inbox/JobDefinitionThreeScenarios_MyJob_scenario.label2.json", items.get(1).getPathname());
			assertEquals("jobs/inbox/JobDefinitionThreeScenarios_MyJob_scenario.label3.json", items.get(2).getPathname());
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Not expecting exception");
		}
	}
	
	@Test
	public void testLoad1Scenario() {
		sb = new S3Basket(inbox, s3BasketSystem);
		try {
			sb.createIfNotExists();
			
			final String jsonFileName = "JobDefinitionOneScenario.json";
			String objectKey = inbox.getBasketRelativeLocation() + jsonFileName;
			String jobdef = FileUtils.getClassPathResourceContent("job-definitions/" + jsonFileName);			
			Map<String, S3Object> folderItems = new HashMap<String, S3Object>();
			
			when(folderItemMock.key()).thenReturn(objectKey);
			when(bucket.downloadAsString(objectKey)).thenReturn(jobdef);			
			when(bucket.getS3Objects()).thenReturn(folderItems);
			
			folderItems.put(objectKey, folderItemMock);
			
			sb.load(BackstopSplitterMocker.getInstance());
			
			List<BasketItem> items = sb.getBasketItems();
			
			// The original JobDefinition BasketItem should have been converted into a single BackstopJson BasketItem instance.
			// The new BasketItem should be persisted and the original parent BasketItem deleted.
			assertEquals(1, items.size());
			verify(items.get(0), times(1)).persist();
			// The new basketItem should overwrite the original, not delete it.
			verify(bucket, times(0)).deleteObject(objectKey);
			
			// The new BasketItem should retain its original name since having only one scenario it is 
			// not necessary to distinguish it from other scenarios in that name.
			assertEquals("jobs/inbox/JobDefinitionOneScenario.json", items.get(0).getPathname());
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			fail("Not expecting exception");
		}
	}
	
	
}
