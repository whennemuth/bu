package bu.ist.visreg.basket.s3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import bu.ist.visreg.util.ArgumentParser;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Bucket {

	private String bucketName;
	private Region region = Region.US_EAST_1;
	private boolean create;
	private Map<String, S3Object> s3Objects = new HashMap<String, S3Object>();
	private S3Client s3;

	@SuppressWarnings("unused")
	private S3Bucket() { /* Restrict default constructor */ }
	
	public S3Bucket(String bucketName, Region region, boolean create) throws Exception {
		if(region != null) {
			this.region = region;
		}
		this.bucketName = bucketName;
		this.create = create;
		this.s3 = S3Client.builder().region(region).build();
		
		inventory();
	}
	
	public S3Bucket(String bucketName, boolean create) throws Exception {
		this(bucketName, null, create);
	}
	
	/**
	 * Query S3 for a listing of all objects in the specified bucket and load the results into an array.
	 * @throws Exception 
	 */
	private void inventory() throws Exception {
        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();

            for (ListIterator<S3Object> iterVals = objects.listIterator(); iterVals.hasNext(); ) {
                S3Object s3Obj = (S3Object) iterVals.next();
                s3Objects.put(s3Obj.key(), s3Obj);
            }
        }
        catch (S3Exception e) {
            if("NoSuchBucket".equalsIgnoreCase(e.awsErrorDetails().errorCode()) && create) {
            	createBucket();
            }
            else if("AuthorizationHeaderMalformed".equalsIgnoreCase(e.awsErrorDetails().errorCode())) {
	        	/**
	        	 * Due to a bug, attempts to list objects on a bucket that does not exist in the account, but whose
	        	 * name is taken by someone else out there on the internet will not cause a "NoSuchBucket" exception.
	        	 * Instead, some sort of malformed authorization header exception is thrown with a complaint 
	        	 * that an unexpected region was supplied with a different region expected. Therefore, we check for
	        	 * the existence of the bucket here.
	        	 */
            	throw new Exception("Bucketname taken! Someone on the internet already has a bucket called " + bucketName, e);
            }
        }	    
	}
	
	@SuppressWarnings("unused")
	private boolean bucketExists() {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);        
        return listBucketsResponse.buckets().stream().anyMatch(bucket -> bucket.name().equals(bucketName));
	}
	
	private void createBucket() throws Exception {
		CreateBucketRequest request = null;
		/**
		 * Per: https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLocation.html#API_GetBucketLocation_ResponseSyntax
		 * A region is not something that can be indicated as a location constraint unless it falls within the following list
		 * of regions:
		 */
		if(Arrays.asList(new Region[] {
			Region.EU_WEST_1,
			Region.US_WEST_1,
			Region.US_WEST_2,
			Region.AP_SOUTHEAST_1,
			Region.AP_SOUTHEAST_2,
			Region.AP_NORTHEAST_1,
			Region.SA_EAST_1,
			Region.CN_NORTH_1,
			Region.EU_CENTRAL_1}).stream().anyMatch(r -> r.equals(region))) {
			
			CreateBucketConfiguration bc = CreateBucketConfiguration.builder()
			    .locationConstraint(region.id())
			    .build();
			
			request = CreateBucketRequest
			    .builder()
			    .bucket(bucketName)
			    .createBucketConfiguration(bc)
			    .build();			
		}
		else {	
			request = CreateBucketRequest
			    .builder()
			    .bucket(bucketName)
			    .build();			
		}
		
		s3.createBucket(request);
	}
	
	public boolean hasInventory() {
		return ! s3Objects.isEmpty();
	}
	
	public void createSubfolder(String folderpath) throws Exception {		
		if( ! folderpath.endsWith("/")) {
			folderpath = folderpath + "/";
		}
        upload(folderpath, new byte[0]);
	}
	
	public void uploadFileFromString(String pathname, String content) throws Exception {
		upload(pathname, content.getBytes());		
	}
	
	private void upload(String pathname, byte[] bytes) throws Exception {

        PutObjectRequest request = PutObjectRequest.builder()
        	.bucket(bucketName)
        	.key(pathname)
        	.build();
        		 
		s3.putObject(request, RequestBody.fromBytes(bytes));
	}
	
	public String downloadAsString(String pathname) {
		return s3.getObject(GetObjectRequest.builder().bucket(this.bucketName).key(pathname).build(),
        	ResponseTransformer.toBytes()).asUtf8String();
	}
	
	public byte[] downloadAsByteArray(String pathname) {
		return s3.getObject(GetObjectRequest.builder().bucket(this.bucketName).key(pathname).build(),
	        	ResponseTransformer.toBytes()).asByteArray();
	}
		
	public void deleteObject(String filepath) {
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(filepath).build();
		s3.deleteObject(deleteObjectRequest);
	}
	
	public String moveObject(String filepath, String newFolderPath) throws Exception {
		return moveObject(filepath, newFolderPath, null);
	}
	
	public String moveObject(String filepath, String newFolderPath, String newName) throws Exception {		
		
		if( ! newFolderPath.endsWith("/")) {
			newFolderPath = newFolderPath + "/";
		}
		if(newName == null) {
			String[] parts = filepath.split("/");
			newName = parts[parts.length-1];
		}

		String newFilePath = newFolderPath + newName;
		System.out.println(String.format(
				"Moving %s to %s",
				filepath,
				newFilePath));
		
		/**
		 * NOTE: S3Client.copyObject(CopyObjectRequest) is problematic and results in a 403 (unauthorized) if the
		 * object you are copying has both s3:GetObjectTagging and s3:PutObjectTagging permissions on it. Instead of trying
		 * to add these permissions before copying, it's easier to download the file and then upload to the new location as
		 * two separate steps (which the copyObject method probably does anyway).
		 * SEE: https://medium.com/collaborne-engineering/s3-copyobject-access-denied-5f7a6fe0393e
		 */
		
		// 1) download the file
		byte[] bytes = downloadAsByteArray(filepath);
		
		// 2) upload the file to the new location
		upload(newFilePath, bytes);
		
		// 3) delete the file from its original location
		deleteObject(filepath);
		
		return newFilePath;
	}
	
	public String getBucketName() {
		return bucketName;
	}

	public Region getRegion() {
		return region;
	}

	public Map<String, S3Object> getS3Objects() {
		return s3Objects;
	}
	
	public S3Object getS3Object(String key) {
		return s3Objects.get(key);
	}
	
	public S3Object getSubFolder(String folderName) {
		if( ! folderName.endsWith("/")) {
			folderName = folderName + "/";
		}
		return getS3Object(folderName);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(bucketName);
		s.append("\n");
		for(S3Object s3obj : s3Objects.values()) {
			s.append(s3obj).append("\n");
		}
		return s.toString();
	}

	/**
	 * Accommodate looser entries for the region and still be able to match up the entry to the corresponding Region enumerator.
	 * Example: "us-east-1" can be used to match up to Region.US_EAST_1 
	 * @param region
	 * @return
	 */
	public static Region getRegion(String region) {
		return Region.of(region.replaceAll("[^a-zA-Z\\d_]", "_").toUpperCase());
	}
	
	public static void listBuckets() {
		Region region = Region.US_EAST_1; 
		S3Client s3 = S3Client.builder().region(region).build();
		ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
		ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
		listBucketsResponse.buckets().stream().forEach(x -> System.out.println(x.name()));
	}
	
	/**
	 * Assuming the S3 bucket already exists, download a listing of all its contents.
	 * 
	 * @param args
	 * @return
	 * @throws Exception 
	 */
    public static S3Bucket parseArgs(String[] args) throws Exception {
    	S3Bucket bucket = null;    	
    	ArgumentParser parser = new ArgumentParser(args);
    	boolean createIfNotExists = parser.getBoolean("c|create");
    	if(parser.has(new String[] { "r|region", "b|bucket"})) {
    		bucket = new S3Bucket(
				parser.getString("b|bucket"),
				getRegion(parser.getString("r|region")), 
				createIfNotExists);    		
    	}
    	else if(parser.has("b|bucket")) {
    		bucket  = new S3Bucket(parser.getString("b|bucket"), createIfNotExists);
    	}
    	else {
    		System.err.println("Expected 1 or 2 arguments!");
    		System.exit(1);
    	}
    	return bucket;
    }

    public static void main(String[] args) throws Exception {
    	S3Bucket bucket = parseArgs(args);
    	System.out.print(bucket);
    	
    	// Move some files from one folder to another.
//    	bucket.moveObject("jobs/completed/jobfile1", "jobs/inbox/");
//    	bucket.moveObject("jobs/completed/jobfile2", "jobs/inbox/");
//    	bucket.moveObject("jobs/completed/jobfile3", "jobs/inbox/");
    }
}
