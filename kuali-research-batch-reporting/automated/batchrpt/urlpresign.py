import os
from botocore.signers import CloudFrontSigner
import datetime
import rsa
from batchrpt import parms

# Determine URL expiration date based on Time-to-Live (TTL), in days, for a generated URL
def getExpireDate():
	print("======= Getting expiry date...")
	ttl_days = int(parms.get('PARMSTORE_CLOUDFRONT_TTL_DAYS'))
	expire_date = datetime.datetime.today() + datetime.timedelta(days=ttl_days)
	print("======= ttl_days: " + str(ttl_days))
	return expire_date
	

def rsa_signer(message):
	# AWS Parameter Store holds the RSA Private Key value needed to access the CloudFront 
	# distribution which returns a selected report files from S3. 
	print("======= Getting cloudfront private key...")
	private_key = parms.get('PARMSTORE_CLOUDFRONT_PK')
	encoded_key = private_key.encode('utf8')
	print("======= Private key obtained, signing message...")
	return rsa.sign(message, rsa.PrivateKey.load_pkcs1(encoded_key),'SHA-1')


def generatePresignedURL(filename):
	print("======= Generating presigned url...")
	key_id = os.environ.get('CLOUDFRONT_PUBLIC_KEY_ID', 'empty')  # CloudFront Public Key ID associated with distribution
	cloudfront_url = os.environ.get('CLOUDFRONT_URL', 'empty')
	url = cloudfront_url + filename
	print("======= Presigned url: {URL}".format(URL=url))
	expire_date = getExpireDate()
	cf_signer = CloudFrontSigner(key_id, rsa_signer)
	signed_url = cf_signer.generate_presigned_url(url, date_less_than=expire_date)
	print("======= Signed url: " + signed_url[ 0 : 20 ] + '...')
	return(signed_url)