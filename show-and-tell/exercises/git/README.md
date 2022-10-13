##  **Git**

Version control is a good first step. Git is the obvious choice here.
The official Git documentation has all the introductory material you may need, though google can always provide.

[https://git-scm.com/doc](https://git-scm.com/doc)

1.  If you’ve never used git, I’d watch the intro videos first:

   1. [What is Version Control](https://git-scm.com/video/what-is-version-control)

   2. [What is Git?](https://git-scm.com/video/what-is-git)

   3. [Get going](https://git-scm.com/video/get-going)

   4. [Quick wins](https://git-scm.com/video/quick-wins)

2. The External Links section also divides learning into stages: 

   [External Documents](https://git-scm.com/doc/ext)

3. And then there’s the git book: 

   [Git Pro Book](https://git-scm.com/book)

   

### Exercises

------

- #### Exercise 1

  Demonstrate how two users can apply conflicting changes to a git repository and how to resolve.

  1. Download and install git with gitbash: 
     [Git Download](https://git-scm.com/downloads)

     The best way to learn git is through the command line. There will be downloads for GUI Clients, but our show and tells may be scripted.

  2. You can create a repository on your computer that can serve as an upstream master, like github.com. You can then pretend you are pushing code up to it and simulate a multi user setup where pushing and merging can take place

     1. Initialize a (bare) headless repository in some directory on your hard drive.
     2. Initialize a repository in another directory for user A
     3. Initialize another repository in one more directory for user B
     4. Configure the headless repository as the remote for A and B *(Hint: You can specify the remote location as a relative file position, ie: "../upstream/")*
     5. Create, add, commit, and push some content to the remote from A
     6. Pull the content from B
     7. Modify the content from A, commit and push
     8. Make a conflicting edit to content B and try to push. Show that it gets rejected.
     9. From B, pull from remote and see a "non-fast forward" message.
     10. Resolve the conflict in the corresponding file and commit (making a merge commit)
     11. Push to the remote
     12. Go to A and pull. Note that it's a fast-forward pull

  3. Put all the commands you used into a single script file with a ".sh" file extension and push to this repository in a new folder:`./exercises/git/exercise1/yourself/your-script.sh`

- #### Exercise 2

  Make your own. Script up something that employs any git feature or use case that you don't already know or is unfamiliar with.

