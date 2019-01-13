#!/bin/bash

netid=kxc170002

konsole -e "scp -r ./src $netid@dc20.utdallas.edu:./CS6349/Project/broker"
konsole --noclose -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@dc20.utdallas.edu javac ./CS6349/Project/broker/src/*.java"
konsole --noclose -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@dc20.utdallas.edu"