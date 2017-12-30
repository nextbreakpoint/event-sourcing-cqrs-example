#!/bin/sh

. $ROOT/bash_aliases

cd $ROOT/terraform/secrets && tf_init
cd $ROOT/terraform/secrets && tf_plan
cd $ROOT/terraform/secrets && tf_apply

cd $ROOT/terraform/ecr && tf_init
cd $ROOT/terraform/rds && tf_init
cd $ROOT/terraform/webserver && tf_init
cd $ROOT/terraform/services && tf_init

cd $ROOT/terraform/ecr && tf_plan
cd $ROOT/terraform/rds && tf_plan
cd $ROOT/terraform/webserver && tf_plan
cd $ROOT/terraform/services && tf_plan

cd $ROOT/terraform/ecr && tf_apply
cd $ROOT/terraform/rds && tf_apply
cd $ROOT/terraform/webserver && tf_apply
cd $ROOT/terraform/services && tf_apply
