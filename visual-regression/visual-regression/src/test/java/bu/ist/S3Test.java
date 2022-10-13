package bu.ist;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

public class S3Test {

	private static void method1() {
		Region region = Region.US_EAST_1; 
		S3Client s3 = S3Client.builder().region(region).build();
		ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
		ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
		listBucketsResponse.buckets().stream().forEach(x -> System.out.println(x.name()));
		
		
	}
	
	public static void method2() {
		S3Client s3 = S3Client.builder().region(Region.US_EAST_1)
        .credentialsProvider(new AwsCredentialsProvider() {

			@Override
			public AwsCredentials resolveCredentials() {
				return new AwsCredentials() {

					@Override public String accessKeyId() {
						return "[ACCESS_KEY_ID_VALUE]";
					}

					@Override public String secretAccessKey() {
						return "[SECRET_ACCESS_KEY_VALUE]";
					}
					
				};
			}
        }).build();
		ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
		ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);
		listBucketsResponse.buckets().stream().forEach(x -> System.out.println(x.name()));
	}
	
	public static void main(String[] args) {
		method1();
	}

}
