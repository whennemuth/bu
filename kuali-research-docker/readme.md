# Docker builds for Kuali modules

This repository contains a "build.context" directory for each kuali module that can be built as a docker image. Docker is not run by the user directly against the Dockerfile within. Instead, users invoke an interactive bash script that prompts for input and runs the docker (and other software) commands itself.

## Builds are scripted

Building kuali modules is not just a matter of running docker commands.
To assist with all the preparation and configuration involved in a build, each build context directory contains a bash script file.

Among the functions performed by the script are:

- ##### Determining who or what is engaged in docker use.

  There are 3 possible scenarios here:

  1. **<u>EC2</u>**. A user is manually building/running a kuali module for the cloud.
     This means the user is shelled into one of the EC2 instances and is running the scripts there.
     The scripts will prompt the user for input parameters.

  2. **<u>Jenkins</u>** is running a job to build/run one of the kuali modules
     In this case everything is automated and parameters are passed through to the scripts from a corresponding set configured against the jenkins job itself.
     Essentially jenkins is behaving like a user who can perform the equivalent of answering all the prompts a human would be presented with automatically in one up-front batch.

  3. **<u>Localhost</u>**. A user is manually building/running a kuali module for development directly on their laptop/computer.
     Again, the scripts will prompt the user for input parameters, one of which will indicate this "local development" use, which will invoke a slightly different operation, such as:

     - Mongo databases will be installed and created locally within the docker container where the consuming app is located and will run on localhost (unless otherwise directed).
     - Dummy, out-of-the-box authentication will be assumed (essentially no authentication).
     - Most of the configurations that reflect environment considerations will automatically default to a localhost setting.

     ##### SEE: [Developer setup step-by-step](developer.md)

  The script is therefore modular and will perform the same steps with slight differences depending on which of the 3 use cases listed above is involved.

- ##### Build the docker image for the kuali module.

  This includes, among other things, the following:

  - Accessing AWS S3 for private keys and configuration files.
  - Selecting the correct arguments to apply to the docker build command.
  - Running the docker build command.
  - Loading the produced docker image to the docker registry in the cloud.

- ##### Prepare directories for use as mount points for docker containers

  We try to make directories available outside docker containers through mount points when their content includes things like:

  - Log files
  - Configuration files
  - Application root directories (for access to the source code)

- ##### Run the docker container from the image

  This involves:

  - Identifying if the image should be obtained locally or from the registry
  - Accessing AWS S3 for private keys and configuration files.
  - Gathering all available configurations into their proper mounted directories and using them along with various prompted input to also compile a list of environment variable settings to set against the docker run command.

  If jenkins is running the script, it is doing so with the AWS cli send command feature. In other words, one EC2 server (jenkins) is executing a bash command through a tunnel on a target application server to:

  1. Download this repository.
  2. Run that portion of the appropriate script to issue the docker run command.
  3. Log the stdout of the script to s3 so it can be accessed and combined with the jenkins job console output.

- ​

- ​

## Available builds

For the most part, a docker build/run for one kuali module is similar to any other in a general way.
However, each module has its own distinct scripting tasks to perform.
These are detailed at the following locations:

- ##### [Docker build for Apache](apache-shib/build.context)

- ##### [Docker build for Centos](centos-java/build.context)

- ##### [Docker build for Core](core/build.context)

- ##### [Docker build for COI](coi/build.context)

- ##### [Docker build for KC](kuali-research/build.context)


