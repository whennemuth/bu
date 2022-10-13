#!/bin/bash

# Create the upstream repository
git init --bare upstream

# Create the local repository for user A
git init userA
cd userA
git config user.name "Bugs Bunny"
git config user.email "bugs@warnerbros.com"
git remote add upstream ../upstream/
cd ..

# Create the local repository for user B
git init userB
cd userB
git config user.name "Daffy Duck"
git config user.email "daffy@warnerbros.com"
git remote add upstream ../upstream/
cd ..

# Have user A create a file and push it to upstream
cd userA
cat <<EOF > file1.txt
one
two
three
EOF
git add file1.txt
git commit -m "Adding file1.txt"
git push upstream master
cd ..

# Have userB pull the content.
cd userB
git pull upstream master
cd ..

# Modify the content from A, commit and push
cd userA
sed -i 's/two/2/' file1.txt
git commit -a -m "Changing two to 2"
git push upstream master
cd ..

# Make a conflicting edit to content B and try to push. Show that it gets rejected.
cd userB
sed -i 's/two/one plus one/' file1.txt
git commit -a -m "Changing two to one plus one"
git push upstream master

# From B, pull from remote and see a "non-fast forward" message.
git pull upstream master

# Resolve the conflict in the corresponding file and commit (making a merge commit) and push
cat <<EOF > file1.txt
one
2
one plus one plus one
EOF
git commit -a -m "Fine! I'll change the third line instead"
git push upstream master
cd ..

# Go to A and pull. Note that it's a fast-forward pull
cd userA
git pull upstream master