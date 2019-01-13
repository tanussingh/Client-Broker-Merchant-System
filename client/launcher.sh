#!/bin/bash

netid=kxc170002

konsole -e "scp -r ./src $netid@dc10.utdallas.edu:./CS6349/Project/client"
konsole --noclose -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@dc10.utdallas.edu javac ./CS6349/Project/client/src/*.java"
konsole --noclose -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@dc10.utdallas.edu"