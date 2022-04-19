#!/bin/bash

set -e

mvn versions:update-properties -Dcommon=true -Dservices=true -Dplatform=true
mvn versions:commit
