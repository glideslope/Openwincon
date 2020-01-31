#! /bin/bash

slice_conf_path="/home/djchoi/opensource/openairinterface5g/"

export OPENAIR_HOME=$(pwd)
export OPENAIR_DIR=$(pwd)
export OPENAIR1_DIR=$OPENAIR_HOME/openair1
export OPENAIR2_DIR=$OPENAIR_HOME/openair2
export OPENAIR3_DIR=$OPENAIR_HOME/openair3
export OPENAIR_TARGETS=$OPENAIR_HOME/targets

export PATH=$PATH:$OPENAIR_TARGETS/bin

alias  oai='cd $OPENAIR_HOME'
alias oai0='cd $OPENAIR0_DIR'
alias oai1='cd $OPENAIR1_DIR'
alias oai2='cd $OPENAIR2_DIR'
alias oai3='cd $OPENAIR3_DIR'
alias oait='cd $OPENAIR_TARGETS'
alias oailte='cd $OPENAIR_TARGETS/RT/USER'
alias oais='cd $OPENAIR_TARGETS/SIMU/USER'
alias oaiex='cd $OPENAIR_TARGETS/SIMU/EXAMPLES'

$slice_conf_path/cmake_targets/build_oai -w USRP -x -c --eNB
sudo -E $slice_conf_path/cmake_targets/lte_build_oai/build/lte-softmodem -O $OPENAIR_DIR/targets/PROJECTS/GENERIC-LTE-EPC/CONF/enb.band7.tm1.usrpb210.conf -d
sudo -E $slice_conf_path/cmake_targets/lte_build_oai/build/lte-softmodem -h
