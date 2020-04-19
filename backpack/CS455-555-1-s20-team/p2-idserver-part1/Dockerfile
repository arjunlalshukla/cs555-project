FROM centos:7

RUN yum update -y

RUN yum install java-1.8.0-openjdk.x86_64 java-devel git net-tools make wget which vim -y
 
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo

RUN yum install sbt -y

RUN mkdir -p ids_demo

WORKDIR ids_demo

# Normally you would just clone a repository, but since this can't be public, we need to do it this way
COPY ./src/ ./src
COPY ./build.sbt .
COPY ./client .
COPY ./Client_Truststore .
COPY ./Makefile .
COPY ./mysecurity.policy .
COPY ./server .
COPY ./Server.cer .
COPY ./Server_Keystore .
COPY ./project/ ./project

RUN make

CMD ./server