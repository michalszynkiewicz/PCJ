#!/usr/bin/env bash

#TEST_CLASS=BarrierTest
#TEST_CLASS=IntegralPiCalcTest
#TEST_CLASS=NonFTPi
#TEST_CLASS=PiCalculationTest
LIB_BINARY='PCJ-4.1.0.SNAPSHOT-bin'

./gradlew build 

#rm build/tmp/dist/*.class

cp build/libs/${LIB_BINARY}.jar build/tmp/dist
cp build/libs/${LIB_BINARY}.jar build/libs/PCJ-4.0.0.SNAPSHOT2-bin.jar

rm build/tmp/dist/*.class

for TEST_CLASS in BarrierTest IntegralPiCalcTest PiCalculationTest
do

    cp src/test/java/${TEST_CLASS}.java build/tmp/dist
    cp src/test/java/${TEST_CLASS}NonFT.java build/tmp/dist

    pushd build/tmp/dist

    javac -nowarn -cp "${LIB_BINARY}.jar" "${TEST_CLASS}.java"
    javac -nowarn -cp "${LIB_BINARY}NonFT.jar" "${TEST_CLASS}NonFT.java"

    popd
done

scp /home/michal/phd/dev/pcj-lib/build/tmp/dist/* michalsz@hpc.icm.edu.pl:tests
scp runScript-okeanos.sh michalsz@hpc.icm.edu.pl:tests

ssh michalsz@hpc.icm.edu.pl ssh okeanos ./archive.sh

echo -ne "Copying to okeanos...\t"
ssh michalsz@hpc.icm.edu.pl scp -r tests michalsz@okeanos:
echo "DONE"