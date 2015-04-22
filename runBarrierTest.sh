#!/bin/bash


#TEST_CLASS=WaitForOnNode0Test
TEST_CLASS=BarrierTest
LIB_BINARY='PCJ-4.0.0.SNAPSHOT-bin.jar'
#HOSTS=("192.168.0.108" "192.168.0.107")
HOSTS=("192.168.0.100" "192.168.0.101")
#HOST=192.168.42.229

BARRIER_COUNT=10000

if (( $# > 0 )); then
    echo "will build"
    gradle build
    if (( $? != 0 )); then
        exit 0
    fi
fi

cp build/libs/${LIB_BINARY} build/tmp/dist
cp src/test/java/${TEST_CLASS}.java build/tmp/dist
pushd build/tmp/dist && javac -cp .::${LIB_BINARY} "${TEST_CLASS}.java"

LOCAL=$(ifconfig wlan0 | grep inet | grep -v inet6 | awk '{print$2}' | awk -F  ":" '/1/ {print $2}')
#LOCAL=$(ifconfig eth0 | grep inet | grep -v inet6 | awk '{print$2}' | awk -F  ":" '/1/ {print $2}')
HOSTS_PROPERTY=${LOCAL}:8087,${LOCAL}:8187,${LOCAL}:8287,${LOCAL}:8387,${LOCAL}:8487,${LOCAL}:8587,${LOCAL}:8687,${LOCAL}:8787,${LOCAL}:8887,${LOCAL}:8987
HOSTS_PROPERTY=${HOSTS_PROPERTY},${LOCAL}:9087,${LOCAL}:9187,${LOCAL}:9287,${LOCAL}:9387,${LOCAL}:9487,${LOCAL}:9587,${LOCAL}:9687,${LOCAL}:9787,${LOCAL}:9887,${LOCAL}:9987


#for host in ${HOSTS[*]}
#do
#    scp "${TEST_CLASS}.class" michal@${host}: && scp ${LIB_BINARY} michal@${host}:
#    HOSTS_PROPERTY="${HOSTS_PROPERTY},${host}:8087,${host}:8187,${host}:8287,${host}:8387,${host}:8487,${host}:8587,${host}:8687,${host}:8787,${host}:8887,${host}:8987"
#done

echo "command: java  -Dpcj.debug=7 -DbarrierCount=$BARRIER_COUNT -Dnodes=$HOSTS_PROPERTY -cp .:${LIB_BINARY} ${TEST_CLASS}"

java -Dnodes=$HOSTS_PROPERTY -DpcjNodeDiscriminator -DbarrierCount=$BARRIER_COUNT -cp .:${LIB_BINARY} ${TEST_CLASS}
#/home/michal/phd/dev/pcj-lib/killOneNode.sh
popd
#java -Dnodes=$HOSTS_PROPERTY -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5016 -cp .:${LIB_BINARY} ${TEST_CLASS}; popd
