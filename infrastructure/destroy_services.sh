#!/bin/bash
DIR=$(pwd)
source bash_alias

cd $DIR/terraform/services && tf_destroy -force

cd $DIR/terraform/rds && tf_destroy -force

cd $DIR/terraform/ecr && tf_destroy -force
