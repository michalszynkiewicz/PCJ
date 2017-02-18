#!/bin/bash -l
#SBATCH -J PCJ-tests
#SBATCH -N 256
#SBATCH -n 256
#SBATCH --mem 18000
#SBATCH --time=8:00:00
#SBATCH --output="PCJ-tests_%j.out"
#SBATCH --error="PCJ-tests_%j.err"
NODE_NUM=256
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
module load java || exit 1


# --- PREPARING NODES LIST ---
log "Preparing nodes list"
mpiexec hostname -s | sort > all_nodes.txt

attempts=4
#mstodo change to 50

function tryTimes() {
    descriptiveName=$1
    shift
    outFile="output${descriptiveName}.txt"
    for i in `seq 1 ${attempts}`
    do
        echo "--------------------------------------$NODE_NUM procesy ------------------------------------------------"
        srun -N $NODE_NUM -n $NODE_NUM hostname > nodes.txt
        uniq nodes.txt > nodes.uniq
        echo "java -Xms2g -Xmx2g -Dpcj.port=8094 -cp .:${LIB_BINARY} $@ ${TEST_CLASS} nodes.txt"
        srun --hint=nomultithread -N $NODE_NUM --nodelist=./nodes.uniq -n $NODE_NUM -c 1 java -Xms2g -Xmx2g -Dpcj.port=8094 -cp .:${LIB_BINARY} $@ ${TEST_CLASS} nodes.txt
        echo "------------------------------------------------------------------------------------------------------------------"
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

LIB_BINARY='PCJ-4.1.0.SNAPSHOT-bin.jar'
TEST_CLASS=BarrierTest
tryWithFailureCount "10k_barrier" -DbarrierCount=10000
tryWithFailureCount "100k_barrier" -DbarrierCount=100000
tryWithFailureCount "1m_barrier" -DbarrierCount=1000000
TEST_CLASS=IntegralPiCalcTest
tryWithFailureCount "IntegralPiCalc" -DpointCount=100000000
TEST_CLASS=PiCalculationTest
tryWithFailureCount "PiCalculationTest" -DpointCount=100000000

LIB_BINARY='PCJ-4.1.0.SNAPSHOT-binNonFT.jar'
TEST_CLASS=BarrierTestNonFT
tryTimes "10k_barrierNonFT" -DbarrierCount=10000
tryTimes "100k_barrierNonFT" -DbarrierCount=100000
tryTimes "1m_barrierNonFT" -DbarrierCount=1000000
TEST_CLASS=IntegralPiCalcTestNonFT
tryTimes "IntegralPiCalcTestNonFT" -DpointCount=100000000
TEST_CLASS=PiCalculationTestNonFT
tryTimes "piCalcTestNonFT" -DpointCount=100000000
