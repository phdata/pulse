#!/bin/bash 
set -xe
# Usage: ./replace_dataset_name.sh <template> <new_dataset_name> <new_dataset_detail>
# This script will replace value of dataset_name, dataset_detail from pulse.json file

if [ "$#" -ne 2 ]; then
    echo "Usage './replace-data-set-name.sh <dataset_name> <dataset_source>"
    exit 1
fi

NEW_DATASET_NAME=$2
DATASET_SOURCE=$3

sed "s/pulse-test-temp/$NEW_DATASET_NAME/g; s/Solr.pulse-test-default_all/$DATASET_SOURCE/g" pulse-dashboard-template.json > dashboard.json
