#!/bin/sh

usage() { echo "Usage: $0 [-g GEMC] [-n NEV] [-p PARTS] [-c GCARD]" 1>&2; exit $1; }

run=11
gemc=5.10
nevnts=100
particles=electronproton

while getopts "g:n:p:c:h" o
do
    case "${o}" in
        g) gemc=${OPTARG} ;;
        n) nevents=${OPTARG} ;;
        p) particles=${OPTARG} ;;
        c) gcard=${OPTARG} ;;
        h) usage 0 ;;
        *) usage 1 ;;
    esac
done

if [ "$particles" == "all" ]
then
    top=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)
    for x in `awk '{print$1}' $top/list.txt`
    do
        $top/gemc.sh -p $x
    done
else 
    ! [ -e "$particles.txt" ] && echo Missing input file:  $particles.txt && exit 2
    [ -e "$particles.hipo" ] && echo Output file already exists:  $particles.hipo && exit 3
    if [ -z ${gcard+x} ]
    then
        test -d clas12-config || git clone https://github.com/jeffersonlab/clas12-config
        gcard=clas12-config/gemc/$gemc/clas12-default.gcard 
    fi

    echo gemc \
    $gcard \
    -INPUT_GEN_FILE="LUND, $particles.txt" \
    -OUTPUT="hipo, $particles.hipo" \
    -RUNNO=$run \
    -USE_GUI=0 \
    -N=$nevents \
    -SAVE_ALL_MOTHERS=1 \
    -SKIPREJECTEDHITS=1 \
    -INTEGRATEDRAW="*" \
    -NGENP=50
fi
