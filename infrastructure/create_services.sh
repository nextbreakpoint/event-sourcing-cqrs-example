#!/bin/bash
DIR=$(pwd)
source bash_alias

cd $DIR/terraform/ecr && tf_init && tf_apply

cd $DIR/terraform/rds && tf_init && tf_apply

cd $DIR/terraform/services && tf_init && tf_apply
