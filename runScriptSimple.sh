#!/bin/bash -l
#SBATCH -J PCJ-tests
#SBATCH -N 1
#SBATCH -n 28
#SBATCH --mem 18000
#SBATCH --time=24:00:00
#SBATCH --output="PCJ-tests_%j.out"
#SBATCH --error="PCJ-tests_%j.err"

timer=`date +%s`

function log {
  output=$(echo "";echo "`date +'%y-%m-%d %H:%M:%S'`" ; while read -r line; do echo -e "\t$line"; done < <(echo -e "$@");echo "")
  echo "$output"
  echo "$output" 1>&2
}

ls -lah

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
        nodeList=("${nodeList[@]}" "${node}")
    fi
done
exec 3>&-

echo "${nodeList[@]}"

function prepareNodes() {
    declare -i targetCount=$1
    declare -i count=0
    nodes=""
    skip=false
    host="x"

    for i in ${nodeList[@]}
    do
        if [ $i == $host ]; then
            if [ "$skip" = false ]; then
                node="${i}:${port}"
                nodes="${nodes},${node}"
                port=$((port + 1))
                count=$((count + 1))
                skip=true
            else
                skip=false
            fi
        else
            host=$i
            skip=true
        fi
        if [ "$count" -eq "$targetCount" ]; then
            break;
        fi
    done
    nodes="${nodes:1}"
}

prepareNodes 20
echo "nodes: $nodes"

LIB_BINARY='PCJ-4.0.0.SNAPSHOT1-binNonFT.jar'
echo "non-ft"
echo "pi calc"
java -cp "${LIB_BINARY}" -Dnodes=${nodes} -Dfails=0 -DpointCount=1000000000 -Dpcj.node.timeout=10 IntegralPiCalcTestNonFT

LIB_BINARY='PCJ-4.0.0.SNAPSHOT1-bin.jar'
for failNo in `seq 0 2`
do
   echo "FAILS: $failNo"
   echo "pi calc"
#   echo "timeout: 5"
#   java -cp "${LIB_BINARY}" -Dnodes=${nodes} -Dfails="${failNo}" -DpointCount=1000000000 -Dpcj.node.timeout=5  PiCalculationTest
#   echo "timeout: 10"
#   java -cp "${LIB_BINARY}" -Dnodes=${nodes} -Dfails="${failNo}" -DpointCount=1000000000 -Dpcj.node.timeout=10  PiCalculationTest
   echo "timeout: 30"
   java -cp "${LIB_BINARY}" -Dnodes=${nodes} -Dfails="${failNo}" -DpointCount=1000000000 -Dpcj.node.timeout=30  IntegralPiCalcTest
   echo "timeout: 30"
   java -cp "${LIB_BINARY}" -Dnodes=${nodes} -Dfails="${failNo}" -DpointCount=1000000000 -Dpcj.node.timeout=30  IntegralPiCalcTest
   echo "timeout: 30"
   java -cp "${LIB_BINARY}" -Dnodes=${nodes} -Dfails="${failNo}" -DpointCount=1000000000 -Dpcj.node.timeout=30  IntegralPiCalcTest
done
