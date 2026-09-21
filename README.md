# Project 1 - Distributed Java Node System

This project is a basic distributed systems assignment in which multiple Java nodes are started from a shared configuration file and communicate over a network. The application is designed to simulate a coordinated multi-node environment where each node follows the setup described by the config and is launched through helper scripts.

The goal of the project is to demonstrate how distributed processes can be initialized, run together, and cleaned up in a controlled way using a common configuration. This makes it easier to test communication between nodes and manage the lifecycle of the system without manually starting each process individually.

## Included files
- `Node.java` - main Java implementation of the distributed node behavior
- `config.txt` - configuration file that defines the node setup and network details
- `launcher.sh` - script used to start the application
- `cleanup.sh` - script used to stop all active node processes
- `Readme.txt` - project notes and basic instructions

## How to run
```bash
javac Node.java
chmod +x launcher.sh cleanup.sh
./launcher.sh config.txt
```

To stop the system:
```bash
./cleanup.sh config.txt
```

## Notes
- Use the same config file name for both launch and cleanup.
- The project is intended for use in a lab or remote environment where distributed nodes are started together and managed as a single system.
- This is a lightweight distributed node setup meant to illustrate process coordination and configuration-driven startup.
