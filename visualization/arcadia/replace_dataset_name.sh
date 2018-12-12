#!/bin/bash -xe
# Usage: ./replace_dataset_name.sh <json_file_to_replace_from> <new_dataset_name> <new_dataset_detail>
# This script will replace value of dataset_name, dataset_detail from pulse.json file

JSON_FILE=$1
NEW_DATASET_NAME=$2
NEW_DATASET_DETAIL=$3

sed -i "" "s/pulse-test-temp/$NEW_DATASET_NAME/g; s/Solr.pulse-test-default_all/$NEW_DATASET_DETAIL/g" $JSON_FILE
