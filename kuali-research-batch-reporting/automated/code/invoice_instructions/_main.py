# Kuali Research Batch Report Main Driver 

# Copy and modify as needed for a specific report.
# Multiple SQL scripts may be required to marshall multi-tab data for an Excel sheet.
# Product is either a CSV or XLSX report pushed to AWS S3.
# Email users pre-signed URLs to report stored in S3.

import os
import sys
from batchrpt import genrpt, csv2xlsx, s3push, emailrpt

# Distribution email list is maintained in a DynamoDB table entry identified by Report Code.
# Report Code is received as an input parm when the _loader.sh script runs this program.
report_cd = sys.argv[1]

# List of SQL batch scripts to run and name of each CSV file generated by scripts. 
sql_scripts = ["invoice_instructions_active_awards.sql"]
csv_files = ["invoice_instructions_active_awards.csv"]

# Name of Excel file and tab name(s) when an Excel file is created from one or more CSV files.
excel_file = "invoice_instructions_active_awards.xlsx"
tab_names = ["Invoice Instructions"]

# generate CSV report file(s). 
genrpt.generateCsvReports(sql_scripts, csv_files)

# format Excel sheet from CSV file(s)
csv2xlsx.convertCsvToExcel(csv_files, tab_names, excel_file)

# Create list of local report file names to send to S3 and email as attachments. There is only one file in this case.
report_files = []
report_files.append(excel_file)

# Reports will always be published unless PUBLISH_REPORTS is explicitly set as "false"
if os.environ.get('PUBLISH_REPORTS', 'true') != 'false':
  # push reports to S3 
  s3push.pushReportsToS3(report_files)

  # email pre-signed URLs to reports in S3. 
  emailrpt.sendEmailWithURLs(report_cd, report_files)