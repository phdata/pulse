#!/bin/bash
# Dependencies: curl
# Usage:
# Export the variables: PULSE_COLLECTOR_HOST and PULSE_COLLECTOR_PORT for the host and port caontaing the Pulse
# Log Collector
# ```
# export PULSE_COLLECTOR_HOST=host1.com
# export PULSE_COLLECTOR_PORT=9004
# ```
# Then source this scrip in your own bash scrip
# ``` 
# source logger.sh
# logger --category=category --level=level --message=message --threadname=threadname --application=application-name
# ```
set -euo pipefail

logger(){
  if [ "$#" -lt 1 ]
  then
  echo "Usage Info: logger --category=category --level=level --message=message --threadname=threadname --application=application-name"
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
        threadName="${1#*=}"
    ;;
    --application=*)
        application="${1#*=}"
        ;;
    *)
        echo "Usage Info: logger --category=category --level=level --message=message --threadname=threadname"
        exit 1
  esac
    shift
  done

  timestamp=$( date -u +"%Y-%m-%dT%H:%M:%SZ")

  curl -s -X POST -H 'Content-Type: application/json' -d '{"category": "'$category'","timestamp": "'$timestamp'", "level": "'$level'", "message": "'$message'", "threadName": "'$threadName'"}' http://$PULSE_COLLECTOR_HOST:$PULSE_COLLECTOR_PORT/v2/event/$application

}
