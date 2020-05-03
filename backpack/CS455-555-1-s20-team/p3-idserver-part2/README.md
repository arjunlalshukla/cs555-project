# Project Number 3

* Authors: Ahmad Rezaii, Arjun Shukla
* Class: CS555 [Distributed Systems] Section 001
* Spring 2020

## Overview

Identity server which maintains user information and can be used for authentication

## Building the code

The project is an sbt project, with a provide Makefile. 

To first install sbt, run the provided script get_sbt.sh:
```
./get_sbt.sh
```

to build the project:
```
make
```

run the server
```
./server
```

run the client
```
./client <args>
```

clean up:
```
make clean
```


## Included Files
```
build.sbt - build file
client - script to start the client
Client_Truststore - SSL file for testing
Dockerfile - Docker definition file for containerizing the server
get_sbt.sh - installs sbt if you don't have it
Makefile - make wrapper for sbt
mysecurity.policy - SSL file for testing
README.md - this file
server - script to start the server
Server.cer - SSL file for testing
Server_Keystore - SSL file for testing
```
src
├── main
│   ├── java
│   │   ├── mongo.AbstractIdentityDao.java
│   │   ├── mongo.User.java
│   │   └── mongo.UserDao.java
│   ├── resources
│   │   └── application.properties
│   └── scala
│       ├── client.IdentityClient.scala
│       ├── client.IdentityException.scala
│       └── server.IdentityServer.scala
└── test
    ├── java
    │   ├── ConnectionTest.java
    │   └── UserTest.java
    └── scala
        ├── CLISuite.scala
        ├── RMITest.scala
        └── ServerTestSuite.scala
```

## Testing
All testing was done manually. Both the server and client code were refactored enough too
much to make use of the current test code. We loooked at Chaos Monkey as a possible testing
framework, but did not have the time to fully learn about the software. Chaos Monkey has a
convoluted installation process with dependencies abound, and requires use of other 
frameworks like Spinnaker.

## Reflection
Arjun - Working with an experieced partner (especially with networking) is very rewarding. 
We were able to complete the project in a short amount of time thanks to a diverse
knowledge base between the 2 of us. Remote pair programming comes with bigger hurdles, but
we made it work. Most of the code writing was done together. Throughout the project, I
gained new appreciation for Docker and its uses. Having an isolated environment with a
single network interface is extremely helpful.

Ahmad-


## Video
