#!/bin/sh

. $ROOT/bash_aliases

cd $ROOT/terraform/services && tf_destroy
cd $ROOT/terraform/webserver && tf_destroy
cd $ROOT/terraform/rds && tf_destroy
# cd $ROOT/terraform/ecr && tf_destroy
