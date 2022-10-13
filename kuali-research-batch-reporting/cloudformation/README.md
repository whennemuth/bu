## Cloudformation stack creation

The following steps apply to creating all research admin reports cloud infrastructure.
This includes:

- **IAM ROLES:** IAM roles
- **JOB:** Job definition, Job Queue, Compute Environment, Security Group, Log Group
- **CLOUDFRONT:** Public key, Key group, Origin access identity, distribution
- **S3:** Bucket, Bucket Policy
- **PARAMETERS:** 6 ssm parameters
- **DYNAMODB TABLE:** Single table
- **EVENT RULE:** Rule and target
- **SNS:** topic, subscription

This DOES NOT include:

- **Elastic container repository:** 
  A prerequisite of the stack requires that the docker image has been built and pushed to a repository in the elastic container registry of the same account *(Default repo name: "research-admin-reports:latest")*.

### Prerequisites:

- **AWS CLI:** 
  If you don't have the AWS command-line interface, you can download it here:
  [https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html)

- **IAM Credentials:**
  The cli needs to be configured with sufficient [security credentials](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). IAM user principals are not allowed by BU policy, so these credentials can be issued through an STS token that you obtain through federated access. The credentials must apply to a role with policies sufficient to cover all of the actions to be carried out (stack creation, VPC/subnet read access, ssm sessions, secrets manager read/write access, etc.). Preferably an admin role and all policies will be covered.

- **Bash:**
  You will need the ability to run bash scripts. Natively, you can do this on a mac, though there may be some minor syntax/version differences that will prevent the scripts from working correctly. In that event, or if running windows, you can either:
  - Clone the repo on a linux box (ie: an ec2 instance), install the other prerequisites and run there.
  - Download [gitbash](https://git-scm.com/downloads)

### Steps:

1. #### Clone this repository:

   ```
   git clone https://github.com/bu-ist/kuali-research-batch-reporting.git
   ```

2. #### Obtain the AWS credentials through STS

3. #### Create the stack:

   ```
   cd kuali-research-batch-reporting/cloudformation
   ```

   - **Deactivated**: Accept all defaults.

     ```
     sh main.sh create-stack
     ```

     By default the job queue is created in a DISABLED state so no jobs can get executed.
     Also, without specifying an email address to subscribe to, the SNS notification topic is not created.
     This scenario applies if you want to create the stack, but without the capability to go into action of any kind.

   - **Simple**:

     ```
     sh main.sh create-stack \
       landscape=stg \
       db_host=stg.db.kualitest.research.bu.edu \
       sns_subscription_email=wrh@bu.edu
     ```
   
     This is the simplest way to get the stack up *(though jobs will not run as the job queue and event rules default to a DISABLED state)*.
   
   - **Dryrun**:
   
     ```
     sh main.sh create-stack \
       landscape=stg \
       db_host=stg.db.kualitest.research.bu.edu \
       sns_subscription_email=wrh@bu.edu \
       dryrun=true
     ```
   
     The preparatory scripting will execute, but the cloudformation command to create the stack will not get executed.
     Instead the command is printed out to the console.
   
   - **Custom**:
   
     ```
     sh main.sh create-stack \
       landscape=stg \
       docker_image=MyImageOnDockerhub:latest \
       report_bucket_name=MyCustomS3BucketName \
       db_user=KCBATCH2 \
       db_host=kuali-oracle-stg.clb9d4mkglfd.us-east-1.rds.amazonaws.com \
       loggroup_retention_days=14 \
       email_reply_to_parameter=daffyduck@warnerbros.org \
       job_queue_state=ENABLED \
       event_rule_state=ENABLED
     ```
   
     This will indicate:
     
     - The task behind the job derives from an image that is not in the default ECS location
     - A log group whose items live half as long as normal
     - Someone else's email will form the reply to portion of new report link emails
     - The landscape is not defaulting to prod
     - The s3 bucket that reports are archived to has a custom name *(probably to avoid collision with existing buckets for other landscapes)*
     - A database user and hostname that override default settings.
     - The job queue and event rules ENABLED *(default would be DISABLED*). Jobs should automatically run when stack is finished creating according to their cron schedules.
   
4. #### Activate Jobs:

   By default *(if `job_queue_state=ENABLED` and `event_rule_state=ENABLED` stack parameters were omitted)*, the stack is created in an "inactive" state.
   This allows time to inspect the stack to make sure everything looks ok without any concern that possible issues automatically make their way out to end users before they can be addressed. 
   If the stack is in this default state, no jobs will run until you do either/both of the following two things:

   1. Enable the job queue.
   2. Enable each event rule associated with each report you want to run automatically *(by cron schedule).*
      There is a test event rule that runs every 2 minutes that you may want to enable first for an initial smoke test. If your email is associated in dynamodb as the recipient, you should get an email with results of a report *(don't forget to immediately disable this event rule as soon as the associated job shows up in the job queue - you will receive an email every 2 minutes until you do)*.

   > NOTE: It is probably easiest to run stack creation with just `event_rule_state=ENABLED` set. 
   > That way, jobs will still not run automatically until you enable the job queue, but you don't also have to enable a dozen event rules.



### IMPORTANT!

There is the possibility that the RDS database against which the batch jobs operate is replaced.
Two scenarios are possible:

- **Hosted zone record**
  There is a common name, like "prod.db.kuali.research.bu.edu", set up in route53 to use as a host name for the RDS database.
  Whoever replaces/recreates the RDS database has remembered to also set the new endpoint address for the corresponding hosted zone record.
  If so, and the same username and password apply, no changes need be made to the parameter store entries for the database.
  If not, you need to admonish this person and either get them to do it or do it yourself.
- **No hosted zone record**
  There is no common name set up in route53 to use as a host name for the RDS database.
  In this case you will need to update at least the database host endpoint in the parameter store.

In either case above, **the batch process will still not be able to access the new RDS database**.
Another disruption needs to be addressed:
The deleted RDS database had a security group ingress rule that allowed all traffic from anything that originated from the batch compute environment security group. The new RDS database needs to have the same ingress rule added to its own security group.
This can be done as follows:

```
aws ec2 authorize-security-group-ingress \
    --group-id [RdsInstance.VPCSecurityGroupId] \
    --protocol tcp \
    --port 1521 \
    --source-group [ComputeEnvSecurityGroup.GroupId]
```

