## Git Bisect

My show and tell is on **[git bisect](https://git-scm.com/docs/git-bisect)**

Git bisect is one of the methods git provides for finding commits.

I have a script that automates the following steps:

1. Creates a new git repository
2. Adds a script to the repository that takes input from the user to inventory the bugs they have in their insect farm
   The script replies back with the total number of bugs in the insect farm.
3. Commits the new script to the git repository
4. Makes 100 more commits to the repository and randomly selects one of those 100 commits to introduce a change to the addition logic such that an incorrect answer will be returned.
5. Invokes git bisect and tests the addition functionality checked out for that bisect invocation.
   This is repeated in a loop until the commit in which the bug is first introduced is found.
6. Prints out the bisect activity along with the bad commit number and exits bisect mode.

```
# Initialize the git repo
bash main.sh reset

# Introduce a commit history with a bug nested randomly somewhere in it.
bash main.sh buryTheBug

# Use git bisect to find the commit where the bug was first introduced
bash main.sh digUpTheBug

# Or do all three of the preceding steps at once
bash main.sh all
```

