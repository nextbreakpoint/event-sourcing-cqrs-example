#!/bin/sh

echo -n password >password.txt

kubectl create secret generic mysql-pass --from-file=password.txt
