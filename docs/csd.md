# Cloudera CSD 
The CSD (Custom Service Descriptor) allows Cloudera Manager to easily
manage and monitor the Pulse log aggregation framework, creating 'roles'
for each of the Alerting Engine, Collection Roller, and Log Collector.

## Building the CSD
```bash
$ make 
```

## Installing the CSD

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

