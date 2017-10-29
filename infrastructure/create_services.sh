#!/bin/bash
DIR=$(pwd)
source bash_alias

cd $DIR/terraform/ecr && tf_init && tf_plan && tf_apply

cd $DIR/terraform/rds && tf_init && tf_plan  && tf_apply

cd $DIR/terraform/databases && tf_init && tf_plan && tf_apply && tf_destroy

cd $DIR/terraform/services && tf_init && tf_plan && tf_apply

cd $DIR/terraform/webserver && tf_init && tf_plan && tf_apply
