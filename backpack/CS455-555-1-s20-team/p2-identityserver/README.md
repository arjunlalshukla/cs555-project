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
Arjun-
Network error messages are extremely vague, and usually required us to research
them before we understood what they were saying. The RMI test suite we worked on
always failed on the SSLHandshake, in spite of calling exactly the same code as
the manual tests. Something about the testing environment may be throwing it off,
because it works perfectly outside of test. The software is extremely modularized,
which made splitting up the work much easier, as well as testing and debugging.

Our client is not set up well to be easily testable. We might have had to refactor
it a little if we had gotten that far in developing RMITest.

Ahmad-
We spent a lot of time developing tests for each level of our application, including
the back end, front end cli, and middleware. Because of the current situation, we 
practiced much less pair programming for this project and instead separated the concerns
to where Arjun was able to work on the front end while I worked on the back end simultaneously.
This worked well because it ensured the front end had no knowledge of the back end and vice-a-versa.
The server was developed as a middle layer between the client and the database, with its own
set of tests for the interface it supports. Having individual test suites for each layer made
implementation of the RMI calls extremely fast as we knew for certain that any bugs we 
encountered were located in the RMI calls from the client to the server. Overall this project
has been a lot of fun and I learned a lot by going through with the MongoDB implementation. 

## Video

Getting SBT and running the server: [https://youtu.be/NWc0hKjXjSA](https://youtu.be/NWc0hKjXjSA)

Client functionality demo: 

## TODO
* video

