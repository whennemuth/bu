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
sql_scripts = ["01_saved_proposals.sql", "02_saved_award_docs.sql", "03_saved_tm.sql", "04_tm_corrections.sql", "05_awards_no_linked_proposals.sql", "06_pending_proposals.sql", "07_external_parent_transacations.sql", "08_negative_transactions.sql", "09_miscoded.sql", "10_awards_with_budgets_in_progress.sql", "11_saved_subaward_docs.sql"]
csv_files = ["01_saved_proposals.csv", "02_saved_award_docs.csv", "03_saved_tm.csv", "04_tm_corrections.csv", "05_awards_no_linked_proposals.csv", "06_pending_proposals.csv", "07_external_parent_transacations.csv", "08_negative_transactions.csv", "09_miscoded.csv", "10_awards_with_budgets_in_progress.csv", "11_saved_subaward_docs.csv"]

# Name of Excel file and tab name(s) when an Excel file is created from one or more CSV files.
excel_file = "kc_qa_reports.xlsx"
tab_names = ["1. Saved Proposals", "2. Saved Awards", "3. Saved T&M", "4. T&M Corrections", "5. Awards No Link to Proposal", "6. Pending Proposals", "7. External Parent Trans", "8. Negative Trans", "9. Miscoded Trans", "10. Budgets in Progress", "11. Saved Subawards"]

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