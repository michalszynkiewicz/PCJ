#!/bin/bash

#TEST_CLASS=PiCalculationTest
TEST_CLASS=BarrierTest
host=max
LIB_BINARY='PCJ-4.0.0.SNAPSHOT-bin.jar'

cp build/libs/${LIB_BINARY} build/tmp/dist
cp src/test/java/${TEST_CLASS}.java build/tmp/dist
pushd build/tmp/dist && javac -cp .::${LIB_BINARY} "${TEST_CLASS}.java"

scp "BarrierTest.class" root@${host}: && scp ${LIB_BINARY} root@${host}:
scp "PiCalculationTest.class" root@${host}: && scp ${LIB_BINARY} root@${host}:
popd

