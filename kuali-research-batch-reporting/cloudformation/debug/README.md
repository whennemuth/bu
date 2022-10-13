## Running jobs locally

Since any given batch report generation operates as an [AWS Batch Job](https://docs.aws.amazon.com/batch/latest/userguide/jobs.html), it runs within a docker container. See [Job type](https://docs.aws.amazon.com/batch/latest/userguide/job_definition_parameters.html#type)
This fact of containerization can be leveraged in the typical way to run in any environment as long as that environment can act as a docker host, which includes your localhost/workstation.

The following are directions on how to run a single report as a docker container in [visual studio code.](https://code.visualstudio.com/)

### Prerequisites:

- **AWS CLI:** 
  If you don't have the AWS command-line interface, you can download it here:
  [https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html)
- **IAM Credentials:**
  The cli needs to be configured with sufficient [security credentials](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys). IAM user principals are not allowed by BU policy, so these credentials can be issued through an STS token that you obtain through federated access. The credentials must apply to a role with policies sufficient to cover all of the actions to be carried out by the job (ssm parameter store lookup, dynamodb table lookup, etc.). 
  Preferably an admin role and all policies will be covered.
- **Bash:**
  You will need the ability to run bash scripts. Natively, you can do this on a mac, though there may be some minor syntax/version differences that will prevent the scripts from working correctly. In that event, or if running windows, you can either:
  - Clone the repo on a linux box (ie: an ec2 instance), install the other prerequisites and run there.
  - Download [gitbash](https://git-scm.com/downloads)
- **Docker:**
  Get docker [here](https://docs.docker.com/get-docker/)



### Steps:

1. **Clone this repository:**

   ```
   git clone https://github.com/bu-ist/kuali-research-batch-reporting.git
   ```

2. **Ensure active aws credentials**:
   Make sure the profile *(default or named)* you use to gain CLI access to the cloud account where the job is to run has active [security credentials](https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys)
   A quick test would look like this:

   ```
   aws --profile=[your_profile] sts get-caller-identity
   {
       "UserId": "AROA3GU5SOU7SMTVU2WWV:wrh@bu.edu",
       "Account": "770203350335",
       "Arn": "arn:aws:sts::770203350335:assumed-role/Shibboleth-InfraMgt/wrh@bu.edu"
   }
   ```

3. **Build the docker image for [ECR](https://docs.aws.amazon.com/AmazonECR/latest/userguide/what-is-ecr.html):**
   A helper script *(docker.sh)* exists to make this easier:

   ```
   cd kuali-research-batch-reporting
   sh docker.sh build
   
   # The image will be tagged for upload to the elastic container registry
   docker images
   770203350335.dkr.ecr.us-east-1.amazonaws.com/research-admin-reports
   ...
   ```

4. **[Optional] Push the docker image:**
   Use the helper script again:

   ```
   sh docker.sh push
   ```

5. **Run the docker container:**
   There are two scenarios for running a job yourself:
   
   - **Full**

     In this scenario you would be attempting to generate a report AND invoke all of the AWS services that follow to distribute that report to end users. This means closely impersonating the AWS batch job by running the docker container with precisely the same [environment variables and command](https://docs.aws.amazon.com/batch/latest/userguide/job_definition_parameters.html#containerProperties) as it would within the aws batch service. This requires that the related resources have already been cloud-formed *(s3 bucket, dynamodb, cloudfront, events, etc.)*. Examples:

     Accept all defaults:
   
     ```
     sh docker.sh run profile=[your_profile]
     ```
   
     Or, if the stack was cloud-formed with a non-default landscape, global tag, and report bucket name:
   
     ```
     sh docker.sh run 
       profile=[your_profile] \
       landscape=stg \
       global_tag=research-admin-reports2 \
       report_bucket_name=[name_of_the_reports3__bucket]
     ```
   
     <u>GLOBAL_TAG:</u> The default value for `global_tag` would be "research-admin-reports", which would be overridden to prevent resource name collisions in the case of the account having more than one deployment of this stack at a time.
     The names of all resources that comprise a stack are prefixed with this value.
   
   - **Private:**
     In this scenario you would merely be attempting to generate a report and view its output. This does not require any cloud infrastructure exist. You need only access to the kuali database. Two key environment variables need to be issued to the container when it is run:
     
     - **LOCAL_PARMS:** 
       In the conventional scenario, variables prefixed with `"PARMSTORE_"` are considered to indicate the name of a parameter in the aws ssm parameter store, and the app will obtain values by performing cli lookups against that service. However, if the LOCAL_PARMS environment variable is set to true, the app will consider that any `"PARMSTORE_"` variable contains the value itself, not the parameter name in the store, and no cli lookup is necessary.
     - **PUBLISH_REPORTS:** 
       The app will always attempt to upload reports to the s3 bucket and issue emails to end users in all cases EXCEPT when `"PUBLISH_REPORTS"`is set to "false", in which case the app will end off after generation of the excel files.
     
     The following is an example of running outside of any batch cloud infrastructure - report content is printed to the console, excel files are generated, and nothing else happens.
     
     ```
     sh docker.sh run \
       profile=[your_profile] \
       local_parms=true \
       publish_reports=false \
       db_user=KCBATCH \
       db_pswd=[db_password] \
       db_host=kuali-oracle-stg.clb9d4mkglfd.us-east-1.rds.amazonaws.com
     ```
   
   

