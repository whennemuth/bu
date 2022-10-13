close=$1

if [ $# -ne 1 ]; then
   eval `ssh-agent -s`
   ssh-add C:/Users/wrh/.ssh/buaws-kuali-rsa
   ssh -A wrh@10.57.236.4
else
   eval `ssh-agent -k`
   exit
fi
