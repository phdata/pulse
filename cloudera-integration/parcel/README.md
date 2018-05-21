# Cloudera Parcel

## Building the parcel
At the root of the project, run 
```bash
$ make package
```
This will collect all dependent jars into the `lib_managed` folder

Then from this directory run:
```bash
$ make 
```

## Installing the Parcel

This will copy the CSD to /opt/cloudera/csd. You must be on the Cloudera Manager node

```bash
sudo make install
``` 

