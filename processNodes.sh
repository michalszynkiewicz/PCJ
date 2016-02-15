#!/bin/bash -l
# #SBATCH -J PCJ-tests
# #SBATCH -N 1
# #SBATCH -n 28
# #SBATCH --mem 18000
# #SBATCH --time=24:00:00
# #SBATCH --output="PCJ-tests_%j.out"
# #SBATCH --error="PCJ-tests_%j.err"

# module load plgrid/tools/openmpi || exit 1
# module load plgrid/tools/java8 || exit 1

# mpiexec hostname -s | sort > all_nodes.txt

# !/bin/bash

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



#java -Dnodes=$result -DpcjNodeDiscriminator -DbarrierCount=$BARRIER_COUNT -DpointCount=$POINT_COUNT -cp .:${LIB_BINARY} ${TEST_CLASS}

#getNodes 2
#echo $result

