# Project Number 1

* Authors: Ahmad Rezaii, Arjun Shukla
* Class: CS555 [Distributed Systems] Section 001
* Spring 2020

## Overview

Folder synchronization server and client which will keep clients up to date with server contents.

## Building the code

The project is an sbt project, so... 

to build the project:
```
make
```

clean up:
```
make clean
```


## Included Files

* build.sbt - builder file containing dependencies and minimum versions
* Makefile - file to control building and cleaning up of project code
* README.md - this file
* src/main/scala/Client.scala - source file for the client code
* src/main/scala/FileSync.scala - source file for driver code
* src/main/scala/IOActor.scala - source file for the logging Actor
* src/main/scala/Server.scala - source file for the server code
* src/main/scala/ServiceThread.scala - source file for threaded service


## Testing

To test the server, we performed several different test scenarios:
1. Server up for entire timeout value with no connections incoming
2. Server up with one client connected for two sync intervals, then disconnect client, wait for server timeout.
3. Server up with two clients connected for two sync intervals, then disconnect one client, wait for an additional sync interval, then exit client and wait for timeout on server.
4. Server up with four clients connected. Additional client tries to connect and is rejected. One client disconnects, and new client is able to connect. All clients disconnect, wait for server timeout on server.
5. Server up, two clients connected. One client disconnects during sync, second client continues.
6. Server up with large files in folder. Client connects and begins downloading files, while downloading, user on server deletes a file that client was told to download.
7. Server not running, client starts and then closes.
8. Server up, client not in clientlist tries to connect.
9. Server up, clients connected, server adds files between sync intervals.
10. Server up, clients connected, server deletes files between sync intervals.
11. Server up, clients connected, server modifies files between sync intervals.
12. Server up, client connected, during sync process, client loses network connection. Reconnecting client properly syncs on next interval.


## Reflection

Things that were challenging: 
* Trying to use a new programming language.
* Wanting TCP to have a notification when a socket closed from the remote side.
* Working with a new framework, Akka, and trying to understand the Actor model.
* Debugging errors encountered with synchronized methods and the thread pool.
* Pair programming, being physically located in the same location to get everything done.
* The client hostname is not localhost when connecting from localhost to localhost.
* Testing a distributed app is hard, and there are very many edge and corner cases to consider.

Things that were rewarding: 
* Working with a knowledgeable and dedicated partner.
* Learning new things about the Scala language and the Akka framework.
* Realizing that we must open the outputstream *before* the inputstream or else weird things happen.
* Monitors aren't so bad and can still lead to a pretty clean solution.
* The Scala language has some nice syntactic sugar that remove some of the verbosity found in Java.
* Realizing that four is less than five, and that's important!
