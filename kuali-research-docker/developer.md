# Developer setup step-by-step

 This is a guide for manually building/running one or more kuali modules for development directly on your laptop/computer as localhost. 

[Back to main page...](readme.md)

- Mongo databases will be installed and created locally within the docker container where the consuming app is located and will run on localhost. You will have the option to override this by specifying an existing network/cloud based mongo server. 
- For the kuali research module, an existing oracle database must be specified.
- Dummy, out-of-the-box authentication will be assumed (essentially no authentication).
- Most of the configurations that reflect environment considerations will automatically default to a localhost setting.

## Steps:

1. **Download gitbash**
   Developer setup is bash scripted and downloading gitbash is the easiest way to get both git and bash on your system.

   - This is not necessary if your development box is running linux.
   - This is necessary on a windows machine as bash is not native to it.
   - This is also necessary on a mac.
     The major problem is that the MacOS coreutils are FreeBSD-based while the utilities for linux are most likely from the GNU project. The FreeBSD coreutils are not always compatible with the GNU coreutils. There are performance, behavioral, and options/switches differences between the GNU and FreeBSD versions of [sed](http://forums.freebsd.org/showthread.php?p=207335), [grep](http://lists.freebsd.org/pipermail/freebsd-current/2010-August/019310.html), [ps](https://serverfault.com/questions/324945/what-is-the-os-x-bsd-equivalent-of-the-gnu-ps-auxf-command), and [other utilities](https://unix.stackexchange.com/questions/79355/what-are-the-main-differences-between-bsd-and-gnu-linux-userland/79357#79357) 

   The gitbash download is available [here](https://git-scm.com/downloads) (will also install the latest version of git)

2. **Install Docker**
   The docker download is available [here](https://www.docker.com/community-edition)

3. **Install the AWS command line interface (CLI)**
   Directions for installing the aws cli are available [here](https://docs.aws.amazon.com/cli/latest/userguide/installing.html) 

   *(Note, this requires also installing python 2 version 2.6.5+ or Python 3 version 3.3+, available [here](https://www.python.org/downloads/))*

   The CLI will mainly be used to access an Amazon S3 bucket for application configurations and credentials.
   In order to gain access to S3, the CLI needs to pass along the correct credentials.
   You have two options:

   1. Create a new user in the AWS IAM console with the sufficient policies attached for both S3 and ECS access (new credentials issued)
   2. Obtain existing credentials from whoever the administrator is at the time.

   Once you have the credentials, you again have 2 options:

   1. Create a profile for the credentials.
      Create file ~/.aws/config and paste the following into it, substituting your credentials where necesary:

      ```
      [profile ecr.access]
      aws_access_key_id=[YOUR KEY ID]
      aws_secret_access_key=[YOUR SECRET KEY]
      region=us-east-1
      output=json
      ```

      NOTE: "ecr.access" is a profile name that will be assumed by the upcoming developer.sh, so do not modify it.

   2. The upcoming developer.sh script will prompt you for these values and create the profile itself (recommended)

4. **Clone/pull this repository**
   Open  a gitbash console and perform the following:

   ```
   cd /tmp
   git clone https://github.com/bu-ist/kuali-research-docker.git
   # Prompt for user and password occurs here.
   cd  kuali-research-docker
   git checkout master
   ```

   