#!/bin/bash
# Dependencies: curl
# Usage:
# ```
# source logger.sh
# pulse_debug "debug message"
# pulse_info "info message"
# pulse_warn "warn message"
# pulse_trace "trace message"
# ```
# Export the variables: PULSE_COLLECTOR_HOST and PULSE_COLLECTOR_PORT for the host and port caontaing the Pulse
# Log Collector
# ```
# export PULSE_LOG_COLLECTOR_HOST=host.com
# export PULSE_LOG_COLLECTOR_PORT=9004
# export APPLICATION_NAME=pulse_test_default
# ```

declare ERROR_MESSAGE="Error! Message info is only possible for its argument."

logger(){
  # $1 = Level Info
  # $2 = the number of function parameters
  # $3 = Message to be logged
  # $4 = Pulse Logging text
  if [ -z "$APPLICATION_NAME" ] || [ -z "$PULSE_LOG_COLLECTOR_HOST" ] || [ -z "$PULSE_LOG_COLLECTOR_PORT" ]
  then
    echo "Missing configuration! Environment variables PULSE_LOG_COLLECTOR_HOST, PULSE_LOG_COLLETOR_PORT, and APPLICATION_NAME must be set."
  else
    if [ "$2" -ne 1 ]
    then
      echo $ERROR_MESSAGE
    else
      timestamp=$( date -u +"%Y-%m-%dT%H:%M:%SZ")
      if [ $PULSE_DEBUG ]
      then
        echo $timestamp
        echo "Pulse is "$4" \"$3\" message"
      fi
      curl -s -X POST -H 'Content-Type: application/json' -d '{"category": "","timestamp": "'$timestamp'", "level": "'$1'", "message": "'$3'", "threadName": ""}' http://$PULSE_LOG_COLLECTOR_HOST:$PULSE_LOG_COLLECTOR_PORT/v2/event/$APPLICATION_NAME
    fi
  fi
}

pulse_debug(){
  logger "debug" "$#" "$1" "debugging"
}

pulse_warn(){
  logger "warn" "$#" "$1" "warning"
}

pulse_trace(){
  logger "trace" "$#" "$1" "tracing"
}

pulse_info(){
  logger "info" "$#" "$1" "logging"
}