#!/bin/bash
if [ ! -f sbt-1.3.8.tgz ] ; then
	wget https://piccolo.link/sbt-1.3.8.tgz
fi
if [ ! -d sbt ] ; then
	tar -xzf sbt-1.3.8.tgz
fi
export PATH=$PATH:$PWD/sbt/bin
exec /bin/bash