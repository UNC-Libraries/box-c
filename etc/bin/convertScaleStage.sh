#!/bin/bash

cwd_vagrant=${vagrant.cwd}
file_path=${1}
deriv_path=${5}

vagrant_dir=/vagrant
guest_path_orig=${file_path/$cwd_vagrant/$vagrant_dir}
guest_path_deriv=${deriv_path/$cwd_vagrant/$vagrant_dir}

cd $cwd_vagrant
resp=`vagrant ssh -c "/usr/local/bin/convertScaleStage.sh $guest_path_orig ${2} ${3} ${4} $guest_path_deriv"`

echo ${resp/$vagrant_dir/$cwd_vagrant}