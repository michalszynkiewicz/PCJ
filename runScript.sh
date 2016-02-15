#!/bin/bash -l
# #SBATCH -J PCJ-tests
# #SBATCH -N 1
# #SBATCH -n 28
# #SBATCH --mem 18000
# #SBATCH --time=24:00:00
# #SBATCH --output="PCJ-tests_%j.out"
# #SBATCH --error="PCJ-tests_%j.err"

timer=`date +%s`

function log {
  output=$(echo "";echo "`date +'%y-%m-%d %H:%M:%S'`" ; while read -r line; do echo -e "\t$line"; done < <(echo -e "$@");echo "")
  echo "$output"
  echo "$output" 1>&2
}

log "Current date: `date`"
log "Master host: `/bin/hostname`"
log "Working directory: `pwd`"
log "Environment variables: `env`"
log "Set variables: `set`"
log "CPU info: `cat /proc/cpuinfo`"
log "MEM info: `cat /proc/meminfo`"


# --- LOADING MODULES ---
log "Loading modules"
module load plgrid/tools/openmpi || exit 1
module load plgrid/tools/java8 || exit 1


# --- PREPARING NODES LIST ---
log "Preparing nodes list"
mpiexec hostname -s | sort > all_nodes.txt

declare -i port
port=8050

nodeList=()
exec 3<> all_nodes.txt
while read -u 3 node
do
    if [ "x$node" != "x" ]
    then
        nodeList=("${nodeList[@]}" "${node}:${port}")
        port=$((port + 1))
    fi
done
exec 3>&-

function prepareNodes() {
    declare -i nodeCount=$1
    nodeCount=$((nodeCount-1))
    nodes="${nodeList[0]}"
    for i in ${nodeList[@]:1:$nodeCount}
    do
        nodes="${nodes},${i}"
    done
}

echo "will print result"
echo $nodes

prepareNodes 20

attempts=4
#mstodo change to 50

function tryTimes() {
    descriptiveName=$1
    shift
    outFile="output${descriptiveName}.txt"
    for i in `seq 1 ${attempts}`
    do
        outFile="output${descriptiveName}-${i}.txt"
        echo "java -Dnodes=$nodes -DpcjNodeDiscriminator $@ -cp .:${LIB_BINARY} ${TEST_CLASS}"
        java -Dnodes=${nodes} -DpcjNodeDiscriminator $@ -cp .:${LIB_BINARY} ${TEST_CLASS} > $outFile
#        if grep -q 'too long' "${outFile}"
#        then
#           echo "will retry barrier"
#        else
#           echo "success. will quit"
#           break
#        fi
    done
}

function tryWithFailureCount() {
    name=$1
    shift
    for failNo in `seq 0 2`
    do
        tryTimes "${name}-${failNo}fails" -Dfails=${failNo} $@
    done
}

LIB_BINARY='PCJ-4.0.0.SNAPSHOT-bin.jar'
TEST_CLASS=BarrierTest
tryWithFailureCount "10k_barrier" -DbarrierCount=10000
tryWithFailureCount "100k_barrier" -DbarrierCount=100000
TEST_CLASS=IntegralPiCalcTest
tryWithFailureCount "IntegralPiCalculation" -DbarrierCount=10000
TEST_CLASS=PiCalculationTest
tryWithFailureCount "piCalcTest" -DpointCount=100000

LIB_BINARY='PCJ-NonFT.jar'
TEST_CLASS=BarrierTestNonFT
tryTimes "10k_barrierNonFT" -DbarrierCount=10000
tryTimes "100k_barrierNonFT" -DbarrierCount=100000
TEST_CLASS=IntegralPiCalculationNonFT
tryTimes "IntegralPiCalculationNonFT"
TEST_CLASS=PiCalculationTestNonFT
tryTimes "piCalcTestNonFT" -DpointCount=100000
