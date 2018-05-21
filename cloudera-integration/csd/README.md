# Cloudera CSD (Custom Service Descriptor)

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

