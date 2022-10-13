#!/usr/bin/env bash

# Load code required to run a selected batch job scheduled by AWS thru a Cloudwatch cron event. 

# AWS Batch event must set "reportcd" parameter to identify which code to fetch from directory.
# Code is maintained in a "code" directory with a sub-directory for each batch job. 

reportcd=$1

# Edit supplied "reportcd" parm and if there is an issue, return an error code to AWS Batch to mark job as FAILED.  
if [ -z "$reportcd" ]; then
  echo "Error: 'reportcd' parameter not set. Can't identify code subdirectory to read."
  exit 1 
elif [ ! -d code/"$reportcd" ]; then
  echo "Error: Subdirectory '${reportcd}' not found in 'code' directory."
  exit 1 
elif [ ! -f code/"$reportcd"/_main.py ]; then
  echo "Error: Subdirectory 'code/${reportcd}' does not contain expected program '_main.py'."
  exit 1 
else 
  :
fi 

# Copy code for requested batch job to current "automated" directory. Wait for copy to complete before proceeding.
cp -R code/"$reportcd"/. ./ & 
wait 
  
# Start requested job. Content of 'main.py' varies for each batch job.   
python3 _main.py $reportcd
  
exit 



