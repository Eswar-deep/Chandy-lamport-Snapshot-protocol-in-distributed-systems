Project 1 - Distributed Java Node System
=======================================

Brief description
-----------------
This project implements a distributed Java application where multiple nodes communicate using a shared configuration file. The system is launched with helper scripts that start the nodes and clean up the running processes when the experiment is finished.

The goal of this assignment is to run a distributed network of nodes, configure their connections, and manage communication between them in a controlled environment.

Files included
--------------
- Node.java          - Main Java implementation of the distributed node logic
- config.txt        - Configuration file used to define node connections and setup
- launcher.sh       - Starts the system using the selected config file
- cleanup.sh        - Stops all running node processes
- Readme.txt        - Project overview and usage instructions

Requirements
------------
- Java JDK installed
- Linux/Unix environment or UTD DC machine access
- A valid config file present in the project directory

Compile the project
-------------------
From the project directory:

   javac Node.java

Make scripts executable:

   chmod +x launcher.sh cleanup.sh

Run the project
----------------
Start the distributed system:

   ./launcher.sh config.txt

Replace config.txt with the actual configuration file you want to use.

If you are connecting through a UTD DC machine:

   ssh exp240006@dc01.utdallas.edu
   cd /home/012/e/ex/exp240006/project1
   javac Node.java
   chmod +x launcher.sh cleanup.sh
   ./launcher.sh config.txt

Stop the project
----------------
To terminate all running nodes:

   ./cleanup.sh config.txt

Important:
- Use the same config file name for both launch and cleanup.
- If you change the config file, update the command accordingly.

Example
--------
   ./launcher.sh config.txt
   ./cleanup.sh config.txt

Summary
-------
This project is a simple distributed system setup used to launch and manage multiple Java nodes from a configuration file. It demonstrates how to initialize, run, and stop a node-based network in a controlled environment.