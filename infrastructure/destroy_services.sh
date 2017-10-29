#!/bin/bash
DIR=$(pwd)
source bash_alias

cd $DIR/terraform/webserver && tf_destroy

cd $DIR/terraform/services && tf_destroy

cd $DIR/terraform/rds && tf_destroy

#cd $DIR/terraform/ecr && tf_destroy
