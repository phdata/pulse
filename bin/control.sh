#!/usr/bin/env bash
#
# Copyright 2018 phData Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# this is set automatically in the CSD but helpful when running during development
set -x

# fail if variables unset, on failure, or after pipe
set -eo pipefail

CMD=$1

function log {
  timestamp=$(date)
  echo "$timestamp: $1"
}

export DEFAULT_JAVA_HOME=$(readlink -f /usr/java/latest/)
export JAVA_HOME=${JAVA_HOME:-$DEFAULT_JAVA_HOME}
export DEFAULT_JAAS_CONF_PATH="${CONF_DIR}/jaas.conf"
export JAAS_CONF_PATH="${JAAS_CONF_PATH:-$DEFAULT_JAAS_CONF_PATH}"
export JAVA_PROPERTIES="-Djava.security.auth.login.config=${JAAS_CONF_PATH} -Dsun.security.krb5.debug=false -Djava.net.preferIPv4Stack=true"
export DEFAULT_CLASS_PATH="$PULSE_DIST/lib/*"
export CLASS_PATH=${CLASS_PATH:-$DEFAULT_CLASS_PATH}
export KEYTAB_FILE=${KEYTAB_FILE:-"${CONF_DIR}/pulse.keytab"}
export AKKA_CONF=${AKKA_CONF:-"application.conf"}
export ALERT_ENGINE_MAX_HEAP=${ALERT_ENGINE_MAX_HEAP:-"2G"}
export COLLECTION_ROLLER_MAX_HEAP=${COLLECTION_ROLLER_MAX_HEAP:-"2G"}
export LOG_COLLECTOR_MAX_HEAP=${LOG_COLLECTOR_MAX_HEAP:-"8G"}
export KAFKA_PROPS=${KAFKA_PROPS:-"kafka.properties"}
DEFAULT_COLLECTION_ROLLER_CONFIG=aux/collection-roller-default.yml
DEFAULT_ALERT_ENGINE_CONFIG=aux/alert-engine-default.yml

# If configs are empty (aren't set in the safety valve) use defaults.
if [ ! -s "$COLLECTION_ROLLER_CONFIG" ]; then
    echo "No collection roller config found, using the default."
    COLLECTION_ROLLER_CONFIG="$DEFAULT_COLLECTION_ROLLER_CONFIG"
fi

if [[ ! -s "$ALERT_ENGINE_CONFIG" ]]; then
    echo "No alert engine config found, using the default."
    ALERT_ENGINE_CONFIG="$DEFAULT_ALERT_ENGINE_CONFIG"
fi

export JAAS_CONFIG="Client {
   com.sun.security.auth.module.Krb5LoginModule required
   doNotPrompt=true
   useKeyTab=true
   storeKey=true
   keyTab=\"$KEYTAB_FILE\"
   principal=\"$PULSE_PRINCIPAL\";
};
KafkaClient {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  keyTab=\"$KEYTAB_FILE\"
  principal=\"$PULSE_PRINCIPAL\";
};"

echo "${JAAS_CONFIG}" > "${CONF_DIR}/jaas.conf"

# Dump environmental variables
log "DEFAULT_JAVA_HOME : $DEFAULT_JAVA_HOME"
log "JAVA_HOME : $JAVA_HOME"
log "DEFAULT_JAAS_CONF_PATH : $DEFAULT_JAAS_CONF_PATH"
log "JAAS_CONF_PATH : $JAAS_CONF_PATH"
log "JAVA_PROPERTIES : $JAVA_PROPERTIES"
log "DEFAULT_CLASS_PATH : $DEFAULT_CLASS_PATH"
log "CLASS_PATH : $CLASS_PATH"
log "KEYTAB_FILE : $KEYTAB_FILE"
log "AKKA_CONF : $AKKA_CONF"
log "CLASS_PATH: $CLASS_PATH"
log "ZK_QUORUM: $ZK_QUORUM"
log "SMTP_SERVER: $SMTP_SERVER"
log "SMTP_USER: $SMTP_USER"
log "SMTP_PASSWORD: $SMTP_PASSWORD"
log "SMTP_PORT: $SMTP_PORT"
log "SMTP_TLS: $SMTP_TLS"
log "ALERT_ENGINE_CONFIG: $ALERT_ENGINE_CONFIG"
log "COLLECTION_ROLLER_CONFIG: $COLLECTION_ROLLER_CONFIG"
log "ALERT_ENGINE_MAX_HEAP: $ALERT_ENGINE_MAX_HEAP"
log "ALERT_ENGINE_EXTRA_OPTS: $ALERT_ENGINE_EXTRA_OPTS"
log "COLLECTION_ROLLER_MAX_HEAP: $COLLECTION_ROLLER_MAX_HEAP"
log "COLLECTION_ROLLER_EXTRA_OPTS: $COLLECTION_ROLLER_EXTRA_OPTS"
log "LOG_ROLLER_MAX_HEAP: $LOG_ROLLER_MAX_HEAP"
log "LOG_ROLLER_EXTRA_OPTS: $LOG_ROLLER_EXTRA_OPTS"
log "WEBSERVER_PORT: $WEBSERVER_PORT"
log "AKKA_CONF: $AKKA_CONF"
log "PULSE_DIST: $PULSE_DIST"
log "CONF_DIR: $CONF_DIR"
log "PULSE_PRINCIPAL: $PULSE_PRINCIPAL"
log "KEYTAB_FILE: $KEYTAB_FILE"
log "CSD_JAVA_OPTS: $CSD_JAVA_OPTS"
log "KAFKA_PROPS: $KAFKA_PROPS"
log "KAFKA_TOPIC: $KAFKA_TOPIC"

case $CMD in

  (start_alert_engine)
    if [ "${SMTP_TLS}" = true ]
    then
      SMTP_TLS_FLAG="--smtp-tls"
    else
      SMTP_TLS_FLAG=""
    fi

    exec $JAVA_HOME/bin/java \
    $JAVA_PROPERTIES \
    $CSD_JAVA_OPTS \
    -Xmx$ALERT_ENGINE_MAX_HEAP \
    $ALERT_ENGINE_EXTRA_OPTS \
    -cp "$CLASS_PATH" io.phdata.pulse.alertengine.AlertEngineMain \
    --daemonize \
    --zk-hosts $ZK_QUORUM \
    --smtp-server  $SMTP_SERVER  \
    --smtp-user  $SMTP_USER  \
    --smtp-port $SMTP_PORT \
    --conf $ALERT_ENGINE_CONFIG \
    $SMTP_TLS_FLAG \
    --silenced-application-file silenced-applications.txt
    ;;

  (start_collection_roller)
    echo "Starting Server with Zookeeper $ZK_QUORUM"
    exec $JAVA_HOME/bin/java \
    $CSD_JAVA_OPTS \
    $JAVA_PROPERTIES \
    -Xmx$COLLECTION_ROLLER_MAX_HEAP \
    $COLLECTION_ROLLER_EXTRA_OPTS \
    -cp "$CLASS_PATH" io.phdata.pulse.collectionroller.CollectionRollerMain \
    --daemonize \
    --conf $COLLECTION_ROLLER_CONFIG \
    --zk-hosts $ZK_QUORUM
    ;;

  (start_log_collector)
    if [[ -n "$KUDU_MASTERS" ]]
    then
      KUDU_MASTERS_FLAG="--kudu-masters $KUDU_MASTERS"
    else
      KUDU_MASTERS_FLAG=""
    fi

    echo "Starting Server on port $WEBSERVER_PORT"
    exec $JAVA_HOME/bin/java \
    -XX:+UseG1GC \
    $CSD_JAVA_OPTS \
    -Dconfig.file="$AKKA_CONF" \
    $JAVA_PROPERTIES \
    -Xmx$LOG_COLLECTOR_MAX_HEAP \
    $LOG_COLLECTOR_EXTRA_OPTS \
    -cp "$CLASS_PATH" io.phdata.pulse.logcollector.LogCollector \
    --port $WEBSERVER_PORT \
    --zk-hosts $ZK_QUORUM \
    $KUDU_MASTERS_FLAG
    ;;

  (start_log_collector_kafka)
    echo "Starting Log Collector Kafka Mode on topic $KAFKA_TOPIC"
    exec $JAVA_HOME/bin/java \
    -XX:+UseG1GC \
    $CSD_JAVA_OPTS \
    -Dconfig.file="$AKKA_CONF" \
    $JAVA_PROPERTIES \
    -Xmx$LOG_COLLECTOR_MAX_HEAP \
    $LOG_COLLECTOR_EXTRA_OPTS \
    -cp "$CLASS_PATH:/opt/cloudera/parcels/CDH/lib/kafka/libs/*:/opt/cloudera/parcels/KAFKA/lib/kafka/libs/*" io.phdata.pulse.logcollector.LogCollector \
    --zk-hosts $ZK_QUORUM \
    --consume-mode kafka \
    --kafka-properties $KAFKA_PROPS \
    --topic $KAFKA_TOPIC
    ;;

  (*)
    echo "Unrecognized command [$CMD]"
    ;;

esac
