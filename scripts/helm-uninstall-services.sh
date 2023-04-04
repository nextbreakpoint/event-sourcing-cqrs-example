#!/bin/bash

set +e

helm uninstall service-frontend -n services

helm uninstall service-designs-query -n services
helm uninstall service-designs-command -n services
helm uninstall service-designs-aggregate -n services
helm uninstall service-designs-notify -n services
helm uninstall service-designs-render -n services

helm uninstall service-authentication -n services

helm uninstall service-accounts -n services
