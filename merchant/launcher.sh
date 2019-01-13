#!/bin/bash

# Change this to your netid
netid=kxc170002

# Root directory of your project
PROJDIR=./CS6349/Project/merchant/src

# Directory where the config file is located on your local system
CONFIGLOCAL=./src/config_file.txt

# Directory your java classes are in
BINDIR=$PROJDIR

# Your main project class
PROG=Main

n=0

konsole -e "scp -r ./src $netid@dc15.utdallas.edu:./CS6349/Project/merchant"
konsole --noclose -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@dc15.utdallas.edu javac ./CS6349/Project/merchant/src/*.java"

cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read line
    i=$( echo $line | awk '{ print $1 }' )
    echo $i
    while [[ $n -lt $i ]]
    do
        read line
        host=$( echo $line | awk '{ print $2 }' )
    
    konsole -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $n; exec bash" &
        read line
        read line
        read line
        read line
        read line
        read line
        read line

        n=$(( n + 1 ))
    done
)
