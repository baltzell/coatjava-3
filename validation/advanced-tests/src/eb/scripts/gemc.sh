#!/bin/sh

usage() { echo "Usage: $0 [-g GEMC] [-n NEV] [-p PARTS] [-c GCARD] [-t]" 1>&2; exit $1; }

run=11
gemc=5.10
nevnts=100
particles=()

while getopts "g:n:c:p:tdh" o
do
    case ${o} in
        g) gemc=${OPTARG} ;;
        n) nevents=${OPTARG} ;;
        c) gcard=${OPTARG} ;;
        p) particles+=(${OPTARG}) ;;
        t) threads=yes ;;
        d) dryrun=echo ;;
        h) usage 0 ;;
        *) usage 1 ;;
    esac
done

if [ ${#particles[@]} -eq 0 ]
then
    top=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)
    for x in $(awk '{print$1}' $top/list.txt)
    do
        particles+=($x)
    done
fi

if [ -z ${gcard+x} ]
then
    test -d clas12-config || git clone https://github.com/jeffersonlab/clas12-config
    gcard=clas12-config/gemc/$gemc/clas12-default.gcard 
fi

for p in "${particles[@]}"
do
    ! [ -e "$p.txt" ] && echo Missing input file:  $p.txt && exit 2
    [ -e "$p.hipo" ] && echo Output file already exists:  $p.hipo && exit 3
    arg1="-RUNNO=$run -USE_GUI=0 -N=$nevents"
    arg2='-SAVE_ALL_MOTHERS=1 -SKIPREJECTEDHITS=1 -INTEGRATEDRAW="*" -NGENP=50'
    if [ -z ${threads+x} ]
    then
        set -x
        $dryrun gemc \
        $gcard $arg1 $arg2\
        -INPUT_GEN_FILE="LUND, $p.txt" \
        -OUTPUT="hipo, $p.hipo"
        set +x
    else
        set -x
        $dryrun gemc \
        $gcard $arg1 $arg2\
        -INPUT_GEN_FILE="LUND, $p.txt" \
        -OUTPUT="hipo, $p.hipo" &
        set +x
    fi
done

wait

