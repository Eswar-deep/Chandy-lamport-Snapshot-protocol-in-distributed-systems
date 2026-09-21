Project 1 - Distributed Node Launch Setup
========================================

Overview
--------
This project runs a Java-based distributed system using a configuration file and helper scripts to launch and stop all nodes.

Files included
--------------
- Node.java          - Main Java implementation
- config.txt        - Node configuration file used by the application
- launcher.sh       - Starts the project with the selected config file
- cleanup.sh        - Stops all running nodes for that config
- Readme.txt        - Project instructions

Requirements
------------
- Java JDK installed
- Access to UTD DC machines if running remotely
- A valid config file in the same directory

Compile the project
-------------------
From the project directory:

   javac Node.java

Make the scripts executable:

   chmod +x launcher.sh cleanup.sh

Run the project
----------------
Start all nodes using your config file:

   ./launcher.sh config.txt

Replace config.txt with the actual config file you want to use.

If you are running on a UTD remote machine, first connect to the server:

   ssh exp240006@dc01.utdallas.edu

Then move to the project directory:

   cd /home/012/e/ex/exp240006/project1

Then compile and launch:

   javac Node.java
   chmod +x launcher.sh cleanup.sh
   ./launcher.sh config.txt

Stop the project
----------------
To stop all running nodes:

   ./cleanup.sh config.txt

Important:
- Use the same config file name in the cleanup command that was used to launch the system.
- If the config file name changes, update both launcher.sh and cleanup.sh commands accordingly.

Example
--------
   ./launcher.sh config.txt
   ./cleanup.sh config.txt

This project is intended for running the Java distributed node system in a controlled lab environment and should be used with the matching config file for the setup being tested.