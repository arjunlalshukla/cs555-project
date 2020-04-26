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
Makefile - make wrapper for sbt
mysecurity.policy - SSL file for testing
README.md - this file
server - script to start the server
Server.cer - SSL file for testing
Server_Keystore - SSL file for testing
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


## Reflection
Arjun-


Ahmad-


## Extras


## Videos
