# Kuali Research Batch Report Main Driver 

# Copy and modify as needed for a specific report.
# Multiple SQL scripts may be required to marshall multi-tab data for an Excel sheet.
# Product is either a CSV or XLSX report pushed to AWS S3.
# Email users pre-signed URLs to report stored in S3.

import sys 

# A parameter identifies report request.

reportcd = sys.argv[1] 

print ("running Python test/_main.py module")
print ("reportcd parm value is: ", reportcd)