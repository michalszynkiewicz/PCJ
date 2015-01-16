#!/bin/bash

HOST=192.168.0.105
#HOST=192.168.42.229

if (( $# > 0 )); then
    echo "will build"
    gradle build
fi

cp build/libs/PCJ_4.jar build/tmp/dist
cp src/test/java/BarrierTest.java build/tmp/dist
pushd build/tmp/dist && javac -cp .::PCJ_4.jar BarrierTest.java && scp BarrierTest.class michal@${HOST}: && scp PCJ_4.jar michal@${HOST}: && \
java -cp .:PCJ_4.jar BarrierTest; popd
#java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5016 -cp .:PCJ_4.jar BarrierTest; popd
