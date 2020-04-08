# Project Number 2

* Authors: Ahmad Rezaii, Arjun Shukla
* Class: CS555 [Distributed Systems] Section 001
* Spring 2020

## Overview

Folder synchronization server and client which will keep clients up to date with server contents.

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
Database operations, client interface, and server-database interaction are all
tested with either scalatest or JUnit. We wrote a test suite for RMI, but have
not been able to get it to work. End-to-end client-server-database interaction
was tested manually.

## Reflection
Network error messages are extremely vague, and usually required us to research
them before we understood what they were saying. The RMI test suite we worked on
always failed on the SSLHandshake, in spite of calling exactly the same code as
the manual tests. Something about the testing environment may be throwing it off,
because it works perfectly outside of test. The software is extremely modularized,
which made splitting up the work much easier, as well as testing and debugging.

Our client is not set up well to be easily testable. We might have had to refactor
it a little if we had gotten that far in developing RMITest.

## Video


## TODO
* javadocs
* video
* Finish README
