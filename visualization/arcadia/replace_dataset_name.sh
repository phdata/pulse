#!/bin/bash -xe
# Usage: ./replace_dataset_name.sh <json_file_to_replace_from> <your_dataset_name>
# This script will replace value of dataset_name from the json file provided


JSON_FILE=$1
NEW_DATASET_NAME=$2

sed -i "" "s/\"dataset_name\": \"[^\"]*\"/\"dataset_name\": \"$NEW_DATASET_NAME\"/" $JSON_FILE
