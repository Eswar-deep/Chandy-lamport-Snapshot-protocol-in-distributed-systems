#!/bin/bash

# Change this to your netid
netid=exp240006

# Root directory of your project on dc machines
PROJDIR=/home/012/e/ex/exp240006/project1

# Config file should be passed as argument
# Usage: ./launcher.sh config.txt
if [ $# -eq 0 ]; then
    echo "Usage: ./launcher.sh <config_file>"
    echo "Example: ./launcher.sh config.txt"
    exit 1
fi

CONFIGFILE=$1

# Check if config file exists
if [ ! -f "$PROJDIR/$CONFIGFILE" ]; then
    echo "Error: Config file $PROJDIR/$CONFIGFILE not found!"
    exit 1
fi

# Your main project class
PROG=Node

n=0

cat $PROJDIR/$CONFIGFILE | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read line
    total_nodes=$( echo $line | awk '{ print $1 }' )
    echo "Total nodes: $total_nodes"
    echo "Using config file: $CONFIGFILE"
    
    # Read node configuration lines
    while [[ $n -lt $total_nodes ]]
    do
        read line
        node_id=$( echo $line | awk '{ print $1 }' )
        host=$( echo $line | awk '{ print $2 }' )
        
        echo "Launching Node $node_id on $host"
        
        ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host "cd $PROJDIR && java -cp . $PROG $node_id $CONFIGFILE > node_${node_id}.log 2>&1" &
        
        n=$(( n + 1 ))
        
        # Small delay to stagger the launches
        sleep 1
    done
)

echo "All nodes launched"
echo "Monitoring nodes and collecting output files..."

# Wait for nodes to start
sleep 5

# First, check if nodes started successfully by looking at logs
echo "Checking node startup status..."
n=0
cat $PROJDIR/$CONFIGFILE | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read line  # Skip first line
    while [[ $n -lt $total_nodes ]]
    do
        read line
        host=$( echo $line | awk '{ print $2 }' )
        node_id=$( echo $line | awk '{ print $1 }' )
        
        echo -n "Node $node_id on $host: "
        if ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host "ps aux | grep -q '[j]ava.*Node.*$node_id'" 2>/dev/null; then
            echo "RUNNING"
        else
            echo "NOT RUNNING (check node_${node_id}.log on $host)"
            # Show last few lines of log
            ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host "tail -5 $PROJDIR/node_${node_id}.log 2>/dev/null" 2>/dev/null || echo "  (log not accessible)"
        fi
        
        n=$(( n + 1 ))
    done
)
echo ""

# Simple approach: periodically copy output files until all nodes finish
# Check every 10 seconds and copy files
max_wait=300  # Maximum wait time (5 minutes)
elapsed=0

while [ $elapsed -lt $max_wait ]; do
    all_done=true
    
    # Copy output files from all hosts
    n=0
    cat $PROJDIR/$CONFIGFILE | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
    (
        read line  # Skip first line (parameters)
        while [[ $n -lt $total_nodes ]]
        do
            read line
            host=$( echo $line | awk '{ print $2 }' )
            node_id=$( echo $line | awk '{ print $1 }' )
            
            # Check if node is still running
            if ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host "ps aux | grep -q '[j]ava.*Node.*$node_id'" 2>/dev/null; then
                all_done=false
            fi
            
            # Copy output file if it exists
            scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host:$PROJDIR/config-${node_id}.out $PROJDIR/ 2>/dev/null
            
            n=$(( n + 1 ))
        done
    )
    
    if [ "$all_done" = true ]; then
        echo "All nodes completed"
        break
    fi
    
    sleep 10
    elapsed=$((elapsed + 10))
done

# Final copy to ensure all files are collected
echo "Performing final copy of output files..."
n=0
cat $PROJDIR/$CONFIGFILE | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read line  # Skip first line
    while [[ $n -lt $total_nodes ]]
    do
        read line
        host=$( echo $line | awk '{ print $2 }' )
        node_id=$( echo $line | awk '{ print $1 }' )
        
        scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host:$PROJDIR/config-${node_id}.out $PROJDIR/ 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "Copied config-${node_id}.out from $host"
        fi
        
        n=$(( n + 1 ))
    done
)
