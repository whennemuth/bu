import boto3
import os
from batchrpt import parms
from email.mime.multipart import MIMEMultipart
from email.mime.base import MIMEBase
from email.mime.text import MIMEText
from email.mime.image import MIMEImage
from email import encoders
from batchrpt import urlpresign
from batchrpt import appendfile

# Get email from, to, body text, etc. from AWS parameter Store and DynamoDB table.
def getReportDistributionEntry(rptcd):

	emailfrom = parms.get('PARMSTORE_EMAIL_FROM')
	replyto = parms.get('PARMSTORE_EMAIL_REPLYTO')
	greeting = parms.get('PARMSTORE_EMAIL_GREETING')
	signature = parms.get('PARMSTORE_EMAIL_SIGNATURE')

	dynamodb = boto3.resource('dynamodb')
	distr = dynamodb.Table(os.environ['PARMSTORE_DYNAMODB_TABLE'])
	item = distr.get_item(Key={"ReportCd": rptcd}) # Get an item (row) from table
	
	emailto = item['Item']['EmailList'].replace(" ","").split(',') # convert string to list of addresses
	emailcc = item['Item']['EmailCC'].replace(" ","").split(',')
	emailcc = list(filter(None, emailcc)) # remove empty string from list if there are no Cc addresses
	emailsubject = item['Item']['EmailSubject']
	emailtext = greeting + item['Item']['EmailText'] + signature  # composite email body text

	return emailfrom, replyto, emailto, emailcc, emailsubject, emailtext 


# Generate link to report file on S3. 
def generateFileLink(filename):
	s3_filename = appendfile.appendDateToFile(filename) # filename stored in S3 includes date
	url = urlpresign.generatePresignedURL(s3_filename)
	image = '<img src="cid:icon1">'

	link = '\n\n<a href=' + url + '>' + image + '<br>' + s3_filename + '</a>'  # construct link to file on S3
	
	return link
		

# Convert plain text to HTML
def formatHtmlText(plaintext):
	head = '<head> <style type="text/css"> a {text-decoration:none;} </style> </head>'
	htmltext = plaintext.replace('\n', '<br>')  # convert line returns to html breaks 
	htmltext = '<html>' + head + '<body>' + htmltext + '</body></html>'  # wrap text in standard HTML tags

	return htmltext


# Format an in-line image for use in email. File image is read from local file system.
def formatImage(iconfile, contentid):
	f = open(iconfile, 'rb')
	msgImage = MIMEImage(f.read())
	f.close()

	msgImage.add_header ('Content-ID', contentid)
	
	return msgImage


def sendEmailWithURLs(rptcd, filesToSend):
	print("======= Sending email...")
	emailfrom, replyto, emailto, emailcc, emailsubject, emailtext = getReportDistributionEntry(rptcd)
		
	delim = ', '
	msg = MIMEMultipart('related')
	msg['Subject'] = emailsubject
	msg['From'] = emailfrom
	msg['Reply-To'] = replyto
	msg['To'] = delim.join(emailto)
	if emailcc:
		msg['Cc'] = delim.join(emailcc)

	for f in filesToSend:
		link = generateFileLink(f)
		emailtext = emailtext + link

	htmltext = formatHtmlText(emailtext)		
	msg.attach (MIMEText(htmltext, 'html')) 	
	
	msgImage = formatImage('images/download_icon.png','<icon1>')
	msg.attach (msgImage)

	client = boto3.client('ses')
	print("======= Raw email: from={ef}, destinations: {et}{cc}{rm}".format(ef=emailfrom, et=emailto, cc=emailcc, rm=msg.as_string()[ 0 : 20 ]+'...'))
	response = client.send_raw_email(
	    Source = emailfrom,
	    Destinations = emailto + emailcc,
	    RawMessage = {'Data': msg.as_string()}
	    )
