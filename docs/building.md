# Building Pulse

## Building the project
[sbt](https://www.scala-sbt.org) is used for building scala, and Make is added as a wrapper to simplify building tasks.

Makefile targets are run with `make <target-name>`, full documentation can be found in the Makefile:

- `dist`: create a distribution (parcel and csd)
- `test`: run all tests
- `package`: create jars from source. This will place all jars in the `target/lib` director
- `install`: install parcel and CSD. This is only valid on a node running Cloudera Manager and will install
the csd/parcel to /opt/cloudera/csd and /opt/cloudera/parcel-repo. It will not distribute/activate the parcel
or refresh/install the CSD in Cloudera Manager

Before submitting a pull request, please make sure `make test` and `make dist` both pass successfully.


## CSD
The CSD (Custom Service Descriptor) allows Cloudera Manager to easily
manage and monitor the Pulse log aggregation framework, creating 'roles'
for each of the Alerting Engine, Collection Roller, and Log Collector.

### Building the CSD
```bash
$ make 
```

### Installing the CSD

This is for a local/development installation only, see [instalation](./installation.md) for production
installation instructions.

This will copy the CSD to /opt/cloudera/csd. You must be on the Cloudera Manager node
```bash
sudo make install
```

Refresh the CSD list

```bash
GET /cmf/csd/refresh

```

Uninstall any old CSD
```bash
GET /cmf/csd/uninstall?csdName=PULSE-0.1.0
```

Install the CSD
```bash
GET /cmf/csd/install?csdName=PULSE-0.1.1
```

## Cloudera Parcel

### Building the parcel
At the root of the project, run 

```bash
$ make package
```
This will collect all dependent jars into the `lib_managed` folder

Then from this directory run:

```bash
$ make 
```

### Installing the Parcel

This will copy the CSD to /opt/cloudera/csd. You must be on the Cloudera Manager node

```bash
sudo make install
``` 




