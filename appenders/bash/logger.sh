#!/bin/bash

logger(){
  if [ "$#" -lt 1 ]
  then
  echo "Usage Info: logger --category=category --level=level --message=message --threadname=threadname"
  exit 1
  fi

  while [ $# -gt 0 ];do
  case "$1" in
    --category=*)
        category="${1#*=}"
    ;;
    --level=*)
        level="${1#*=}"
    ;;
    --message=*)
        message="${1#*=}"
    ;;
    --threadName=*)
        threadName="'${1#*=}'"
    ;;
    --application=*)
        application="'${1#*=}'"
        ;;
    *)
        echo "Usage Info: logger --category=category --level=level --message=message --threadname=threadname"
        exit 1
  esac
    shift
  done

  timestamp=$( date -u +"%Y-%m-%dT%H:%M:%SZ")

  curl -X POST -H 'Content-Type: application/json' -d '{"category": "'$category'","timestamp": '$timestamp', "level": "'$level'", "message": "'$message'", "threadName": "'$threadName'"}' http://0.0.0.0:9005/log?application=$application

}
