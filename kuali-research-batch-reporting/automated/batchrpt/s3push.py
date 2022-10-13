import os
import boto3
from batchrpt import appendfile

# Copy each report from local file system to AWS S3 bucket.
def pushReportsToS3(localfiles):
	print("======= Pushing report(s) to s3 bucket...")
	bucketname = os.environ['REPORT_BUCKET_NAME']
	s3 = boto3.client('s3')
	for f in localfiles:
		s3_filename = appendfile.appendDateToFile(f) # append date to filename stored in S3
		print("======= {f} ==> {bn} as {sfn}".format(f=f, bn=bucketname, sfn=s3_filename))
		s3.upload_file(f, bucketname, s3_filename)