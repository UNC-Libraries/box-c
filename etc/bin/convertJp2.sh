#!/bin/bash

cwd_vagrant=${vagrant.cwd}
file_path=${1}
deriv_path=${3}

vagrant_dir=/vagrant
guest_path_orig=${file_path/$cwd_vagrant/$vagrant_dir}
guest_path_deriv=${deriv_path/$cwd_vagrant/$vagrant_dir}

cd $cwd_vagrant
resp=`vagrant ssh -c "/usr/local/bin/convertJp2.sh $guest_path_orig ${2} $guest_path_deriv"`

echo ${resp/$vagrant_dir/$cwd_vagrant}