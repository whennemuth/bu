## Bash (and Linux)

Bash is by far the most popular shell among users of linux and the default login shell for most linux distributions, putting it right in our field of interest for devops/aws learning.

Later on we will be working with docker, which began with linux and most docker containers running today are starting against linux.
However, more recently, you can run windows in a docker container with powershell being the shell.
We can explore that option for .NET folks a little more when we get to the docker exercises.

We don't need to become experts in bash to go forward with later exercises, but it is necessary to get some of the basics in. Below are some introductory bash tutorials. 

- [Linux Tutorial](https://ryanstutorials.net/linuxtutorial/)
  Bash runs in a linux environment, so it may make sense to start with this one.  
  
- [Shell Scripting Tutorial](https://www.shellscript.sh/index.html)
This one seems to cover most of the bases, and is at the most introductory level. Recommended.
  
- [Bash Guide for Beginners](https://www.tldp.org/LDP/Bash-Beginners-Guide/html/index.html)
This one is also for beginners, but goes into more depth and covers more.
  
- [Beginners/bashScripting](https://www.tldp.org/LDP/Bash-Beginners-Guide/html/index.html)
This one is more of a cheat-sheet.
  
- [Bash Reference Manual](https://www.gnu.org/software/bash/manual/html_node/index.html)
And the full reference manual.



### Exercises

------

- #### **Exercise 1**

   If you are running on windows, there are a few options for running a bash shell (ie, [virtualbox](https://www.virtualbox.org/) running [linux](https://www.linux.org/threads/what-is-linux.4106/)). However the simplest and fastest option is to download and install gitbash, which comes bundled with git for windows.

   [git for windows download](https://gitforwindows.org/)

   If you wanted to do some practical exercises as part of the linux tutorial above, you can go a step further and have a full linux environment. Again, you could use something like virtualbox for this, but the fastest and easiest way is to run linux in a docker container and shell into that container. We will cover docker next, but for now I've include instruction on how to get into a bash shell in a linux environment with docker below.

   [Install Docker Desktop](docker.md)

   Open a gitbash console window.

   > **NOTE**: *You don't have to do the following in a container - you can do it all directly in gitbash. Future exercises may include performing linux-only actions, so the container is recommended (but not required).*

   Perform the following steps:

   ```
   # Clone this repo if not already done
   git clone https://github.com/whennemuth/Devops-show-and-tell.git
   
   # Navigate to this exercise and make a directory named after you.
   cd Devops-show-and-tell/exercises/linux-and-bash/exercise1
   mkdir myself
   cd myself
   
   # Shell into a container running linux
   sh ../setup.sh
   ```

   You should now be shelled into a centos:7 linux environment.

   1. Navigate to /tmp and list out the contents (you should see an "unsorted " file)

   2. Write a bash script in /tmp that reads the unsorted file and creates a "sorted" file in the same directory where the fruit items now appear in alphabetical order.

   3. When your script works, exit out of the container (type exit) and push your new directory with its contents to github. 

      > **NOTE**: *You can write this script inside the container or outside. The /tmp directory is mounted to ./bash on your host file system.* 
      >
      > *If you choose to create the script file inside the container, you may need a linux text editor.* 
      >
      > *[A Beginner’s Guide to Editing Text Files With Vi](https://www.howtogeek.com/102468/a-beginners-guide-to-editing-text-files-with-vi/)*

      

      

- #### Exercise 2

   Among the most common tasks in a linux environment are:

   - Creating users
   - Modifying file access
   - Setting up public/private keys for shell access

   This exercise uses 2 docker containers to act as linux servers to which we will connect to over ssh.

   Open up gitbash and run the following:

   ```
   # Clone this repo if not already done
   git clone https://github.com/whennemuth/Devops-show-and-tell.git
   
   # Navigate to this exercise and make a directory named after you.
   cd Devops-show-and-tell/exercises/linux-and-bash/exercise2
   mkdir myself
   cd myself
   
   # Shell into a container running linux
   sh ../setup.sh
   ```

   You should see that two new docker containers are running.
   Each container has a bind mount and is running a ubuntu operating system.

   Write 2 bash scripts:

   - ssh-daemon_1: `/tmp/script1.sh`
   - ssh-daemon_2 : `/tmp/script2.sh`

   The bash content you author in each script file should allow you to do the following:
   Shell into each container and run each script.

   ```0
   docker exec -ti ssh-daemon_1 bash
   sh /tmp/script1.sh
   exit
   # Repeat for ssh-daemon_2 container
   ```

   **Each container should now have a new user that can shell into the other container as the other user using a private ssh key (no password).**

   Push your script up to github when you have it working.

   

   Public/private key setup for linux should be well documented on the internet, but a few pointers:
   File permissions and ownership are important.

   - FILE PERMISSIONS AND OWNERSHIP ARE IMPORTANT!
     Usually, mistakes here are the reason why ssh connections fail/reject.

   - The ssh command itself will need to include two pieces of information:

     - The host address. This will be the docker network bridge ip: 172.17.0.1
     - The host port that each container exposes it's internal port 22 to.
       The setup code you ran above prints this port information out to the console.

   - If you find you need to back out any changes, sometimes it's easier to simply blow away the container and start again. These containers are marked to automatically delete themselves when they stop, so to start from scratch, you can run:

     ```
     docker stop ssh-daemon_1 && \
     docker stop ssh-daemon_2 && \
     # Repeat the same two run commands from above...
     ```

   - Vim is installed in each container. Vim is a linux text editor that it may be worth getting used to.
     Instead of editing your bash script files outside the container using a windows editor, you could use vim inside the container.

   - The first time you shell from one container into another, you will see the following 

     ```
     prompt:root@ssh-daemon_1:/# ssh -i /home/myuser1/.ssh/id_rsa2 myuser2@172.17.0.1 -p 32797
     The authenticity of host '[172.17.0.1]:32797 ([172.17.0.1]:32797)' can't be established.
     ECDSA key fingerprint is SHA256:yygaAfN6GNZzm95qJtEHEjsbhJs7rx9A/znK7aJgPwo.
     Are you sure you want to continue connecting (yes/no)?
     ```

     Type "yes" and subsequent attempts will go through unprompted.

     
