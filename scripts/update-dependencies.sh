#!/bin/bash

set -e

mvn versions:update-properties -Dcommon=true -Dservice=true -Dservices=true
mvn versions:commit
