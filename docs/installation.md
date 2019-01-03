# Installation
Pulse can be installed as a Cloudera CSD (Custom Service Descriptor).

## Installing the CSD

1. Download the latest CSD jar, see versions list below
2. Place the jar in your Cloudera Manager CSD directory, usually `/opt/cloudera/csd`
3. Modify the ownership of the jar `chown cloudera-scm:cloudera-scm /opt/cloudera/csd/pulse-<version>.jar`
4. Restart Cloudera Manager to install the jar

## Installing the Parcel

1. The parcel repo should be automatically added with the CSD. The url to add it manually:
2. Download, distribute, activate the parcel

Versions: 
(Note, CSDs are included in the parcel repo)

- 2.1.0: https://repository.phdata.io/artifactory/list/parcels-release/phdata/pulse/2.1.0-ada1-cdh5/

## Installing the Pulse service

Pulse can be installed through the "Add New Service" button for your cluster.

The wizard will ask you for:

- `smtp user`: This will be the 'from' address for alerts
- `smtp password`: This is only necessary if your smtp server uses authentication
- `smtp address`: The hostname of your smtp server
- `smtp port`: The port of your smtp server

### Installation for use with Spark
To install the log4j appender for use with Apache Spark, the `log-appender-{version}.jar`  needs
to be added to the classpath in `spark-env.sh`

To modify the classpath in Cloudera, add this line to the `spark-env.sh` safety valve and redeploy
client configuration:

```bash
export SPARK_DIST_CLASSPATH="$SPARK_DIST_CLASSPATH:/opt/cloudera/parcels/PULSE/lib/appenders/*"
```

## Ansible Installation

Ansible playbooks for installing Pulse from the `install` folder of this repository.

## csd-playbook.yml
Installs the CSD from the phData parcel repository.
This installation method requires [ansible](https://github.com/ansible/ansible)

The `csd-playbook`

```bash 
$ cd install
$ ansible-playbook csd-playbook.yml -u <user> -i <host>, --extra-vars "version=<version>"  
```

## dev-playbook.yml
The dev playbook will install a Cloudera CSD and parcel from local artifacts

```bash 
$ cd install
$ ansible-playbook dev-playbook.yml -u <user> -i <host>, --extra-vars "version=$(sh ../version)"  
```
