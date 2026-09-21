#!/bin/bash

# Change this to your netid
netid=exp240006

# Root directory of your project
PROJDIR=/home/012/e/ex/exp240006/project1

# Config file should be passed as argument
# Usage: ./cleanup.sh config.txt
if [ $# -eq 0 ]; then
    echo "Usage: ./cleanup.sh <config_file>"
    echo "Example: ./cleanup.sh config.txt"
    exit 1
fi

CONFIGFILE=$1

# Check if config file exists
if [ ! -f "$PROJDIR/$CONFIGFILE" ]; then
    echo "Error: Config file $PROJDIR/$CONFIGFILE not found!"
    exit 1
fi

n=0

cat $PROJDIR/$CONFIGFILE | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read line
    total_nodes=$( echo $line | awk '{ print $1 }' )
    echo "Total nodes to cleanup: $total_nodes"
    echo "Using config file: $CONFIGFILE"
    
    # Read node configuration lines
    while [[ $n -lt $total_nodes ]]
    do
        read line
        host=$( echo $line | awk '{ print $2 }' )
        
        echo "Cleaning up processes on $host"
        ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host "pkill -u $netid java 2>/dev/null || true; rm -f $PROJDIR/config-*.out $PROJDIR/node_*.log 2>/dev/null || true" &
        
        n=$(( n + 1 ))
        sleep 0.2
    done
    
    wait
)

# Also clean up local files on dc01
echo "Cleaning up local files..."
rm -f $PROJDIR/config-*.out $PROJDIR/node_*.log 2>/dev/null || true

echo "Cleanup complete"