

# Docker build for Core

## Instructions to build and run

These directions are intended as a step-by-step guide for getting core running in the Boston University cloud-based AWS server environment with its use of shibboleth for SAML authentication. It is supplemental to the README instructions at the KualiCo github page for cor-main.

[Back to main page...](/../..)

1. Shell into the EC2 instance and pull the source code from core from git as follows:

   ```
   if [ ! -d /opt/kuali-research-docker ] ; then
     # There is no git build context for core, coi, etc. so clone it from remote.
     cd /opt
     git clone https://github.com/bu-ist/kuali-research-docker.git
     cd  kuali-research-docker
     git checkout master
   elif [ ! -d /opt/kuali-research-docker/.git ] ; then
     # The build context directory was found, but it does not appear to be 
     # a git repository, so initialize it and pull the master branch down.
     cd /opt/kuali-research-docker
     git init
     git remote add bu https://github.com/bu-ist/kuali-research-docker.git
     git pull bu master
   else
     cd /opt/kuali-research-docker
     git checkout master
     git pull bu master
   
     # If the branch is not master and that branch only exists upstream, then 
     # assuming the branch name is "feature" and the remote name is origin do the following:
     cd /opt/kuali-research-docker
     git fetch origin feature
     git checkout -b feature remotes/origin/feature
   fi
     
   cd core/build.context
   ```
   ​

2. Additional files need to be added to the docker build context:

   - **Github keys**: The core docker image build requires 3 private SSH keys granting access to the git repository for core, cor-common and kuali-ui be present in the docker build context and that they match in name with "*_rsa". 
     You can obtain the appropriate keys from the S3 bucket with:

     ```
     cd /opt/kuali-research-docker/core/build.context
     aws s3 cp s3://kuali-research-ec2-setup/bu_github_id_core_rsa bu_github_id_core_rsa
     aws s3 cp s3://kuali-research-ec2-setup/bu_github_id_core_common_rsa
     aws s3 cp s3://kuali-research-ec2-setup/bu_github_id_kualiui_rsa
     ```
     *NOTE: You will need to include a --profile argument to the commands above that provide authentication to access S3.* 

   - **samlMeta.xml**: This is the meta data file that the single sign-on identity provider, or IDP *(in our case, Shibboleth)* has issued for our service provider. The purpose of this file is covered [here](https://wiki.shibboleth.net/confluence/display/CONCEPT/MetadataForSP). In short, this file contains a key and assertions so the IdP can ensure that the user's information is sent only to authorized locations when that IdP is redirected to by the SP for the purpose of authenticating the user.

     If this file is not present in the docker build context, the docker build  won't break, but node will be instructed to try and curl it from the IDP, which requires being on the BU network which is less preferable if you have the choice. You can find a copy of this file in S3 at: 

     `https://s3.amazonaws.com/kuali-research-ec2-setup/[env]/core/`

     where "env" can be sb, ci, qa, stg, prod – depending on the environment you are deploying to.

     You can use the aws cli to retrieve this file as follows:

     ```
     cd /opt/kuali-research-docker/core/build.context
     aws s3 cp s3://kuali-research-ec2-setup/[env]/core/samlMeta.xml samlMeta.xml
     ```

     **local.js:** A generic copy of this file will have been pulled down with the other git content as the file config/default.js. Copy this file to the docker build context under the name local.js and modify the following to reflect the environment being targeted for the build:

     1. module.exports.auth.casCallbackUrl
        set to: `https://[host]/auth/saml/consume?redirect_to=https://[host]/coi/`
        ie: host for the sandbox environment would be: `kuali-research-sb.bu.edu`
     2. module.exports.auth.casCallbackUrl.samlMeta
        set to: `/var/core/services/auth/server/samlMeta.xml`
     3. module.exports.auth.casCallbackUrl.samlMeta2
        set to: `/var/core/services/auth/server/samlMeta.xml`
     4. module.exports.auth.casCallbackUrl.samlKeys.v0
        Before core became the saml SP for shibboleth, apache filled the SP role with its shibboleth module. Core now needs the private key that apache used to talk to shibboleth. This private key can be found on the related ec2 instance at:
        `/etc/pki/tls/certs/sp-key.pem`
        Replace the v0 value with the contents of sp-key.pem
        *IMPORTANT!!!: Make sure any line breaks in sp-key.pem are represented in v0 with "\n"*
     5. module.exports.auth.casCallbackUrl.samlKeys.v2
        Repeat the above step (v0 and v2 should have the same value).

     Alternatively, if kept up to date, S3 should have a bucket where this file can be found.
     You would obtain the file with:

     ```
     cd /opt/kuali-research-docker/core/build.context
     aws s3 cp s3://kuali-research-ec2-setup/ci/core/local.js local-ci.js
     ```

     *NOTE: The docker.sh script will automatically create a [docker build context]/config directory if it does not exist already and place copies of the local.js and samlMeta.xml files there for mounting to the docker container for core. So be aware that subsequent docker image builds will not incorporate changes to either of these two files if they were made to the copies in the build context. You have to either delete or edit the copies in their mounted directory locations.*

     *IMPORTANT: If you are building a new release or cor-main from kualico, you will want to incorporate any new/removed properties from default.js into your copy of local.js and update the copy being stored in the S3 bucket*.

3. You will need to open up some ports with the AWS EC2 in'[stance you are deploying the core app to. In the AWS management console, go to the security group for the EC2 instance and add the following ports to the incoming tab:

   | Type            | Protocol | Port Range | Source    |
   | --------------- | -------- | ---------- | --------- |
   | Custom TCP Rule | TCP      | 9229       | 0.0.0.0/0 |
   | Custom TCP Rule | TCP      | 27017      | 0.0.0.0/0 |
   | Custom TCP Rule | TCP      | 8090       | 0.0.0.0/0 |
   | Custom TCP Rule | TCP      | 3000       | 0.0.0.0/0 |

   ​

4. You can now call the main bash script (docker.sh) that will build the docker image and run a container from it.

   - Source the main bash script file

      ```
      cd /opt/kuali-research-docker/core/build.context
      source docker.sh
      ```

   - To build the docker image:

      ```
      build
      ```

   - To run the docker container from the built image:

      ```
      runapp
      ```

   - To combine both of the above into one command use:

      ```
      deploy
      ```

5. 


