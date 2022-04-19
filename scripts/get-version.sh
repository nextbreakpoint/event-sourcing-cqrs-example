#!/bin/bash

set -e

mvn -q help:evaluate -Dexpression=project.version -DforceStdout
