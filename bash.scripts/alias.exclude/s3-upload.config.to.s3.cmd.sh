export AWS_PROFILE=legacy
 
echo 'Pushing kc-config.xml to s3...'
aws s3 cp /c/whennemuth/scrap/s3/stg/kc/kc-config.xml s3://kuali-research-ec2-setup/stg/kuali/main/config/kc-config.xml
 
echo 'Pulling kc-config.xml from s3 to target ec2 instance...'
aws ssm send-command \
  --instance-ids "i-090d188ea237c8bcf" \
  --document-name "AWS-RunShellScript" \
  --comment "Refreshing kc-config.xml on i-090d188ea237c8bcf" \
  --parameters \
  '{"commands":["aws s3 cp s3://kuali-research-ec2-setup/stg/kuali/main/config/kc-config.xml /opt/kuali/main/config/kc-config.xml"]}'
aws ssm send-command \
  --instance-ids "i-0cb479180574b4ba2" \
  --document-name "AWS-RunShellScript" \
  --comment "Refreshing kc-config.xml on i-0cb479180574b4ba2" \
  --parameters \
  '{"commands":["aws s3 cp s3://kuali-research-ec2-setup/stg/kuali/main/config/kc-config.xml /opt/kuali/main/config/kc-config.xml"]}'
aws ssm send-command \
  --instance-ids "i-090d188ea237c8bcf" \
  --document-name "AWS-RunShellScript" \
  --comment "Refreshing kc-config.xml on i-090d188ea237c8bcf" \
  --parameters \
  '{"commands":["docker restart kuali-research"]}'
aws ssm send-command \
  --instance-ids "i-0cb479180574b4ba2" \
  --document-name "AWS-RunShellScript" \
  --comment "Refreshing kc-config.xml on i-0cb479180574b4ba2" \
  --parameters \
  '{"commands":["docker restart kuali-research"]}'
