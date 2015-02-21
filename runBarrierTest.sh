#!/bin/bash

LIB_BINARY='PCJ-4.0.0.SNAPSHOT-bin.jar'
#HOSTS=("192.168.0.108" "192.168.0.107")
HOSTS=("192.168.0.102" "192.168.0.108")
#HOST=192.168.42.229

if (( $# > 0 )); then
    echo "will build"
    gradle build
fi

cp build/libs/${LIB_BINARY} build/tmp/dist
cp src/test/java/BarrierTest.java build/tmp/dist
pushd build/tmp/dist && javac -cp .::${LIB_BINARY} BarrierTest.java

HOSTS_PROPERTY=$(ifconfig wlan0 | grep inet | grep -v inet6 | awk '{print$2}' | awk -F  ":" '/1/ {print $2}')

for host in ${HOSTS[*]}
do
    scp BarrierTest.class michal@${host}: && scp ${LIB_BINARY} michal@${host}:
    HOSTS_PROPERTY="${HOSTS_PROPERTY},${host}"
done

echo "command: java -Dnodes=$HOSTS_PROPERTY -cp .:${LIB_BINARY} BarrierTest"

java -Dnodes=$HOSTS_PROPERTY -cp .:${LIB_BINARY} BarrierTest; popd
#java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5016 -cp .:PCJ_4.jar BarrierTest; popd
