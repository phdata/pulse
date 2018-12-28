version = $(shell sh ./version)

package: clean sbt-assembly libdir sbt-package jars

jars:
	cp collection-roller/target/scala-2.11/collection-roller-assembly-$(version).jar target/lib/
	cp alert-engine/target/scala-2.11/alert-engine-assembly-$(version).jar target/lib/
	cp log-collector/target/scala-2.11/log-collector-assembly-$(version).jar target/lib/
	cp log-appender/target/scala-2.11/log-appender-assembly-$(version).jar target/lib/appenders/
	cp common/target/scala-2.11/common-assembly-$(version).jar target/lib/


test:
	sbt -mem 4096 test

version:
	sh ./version

dist: parcel csd
	mkdir -p dist
	cp cloudera-integration/parcel/target/*.parcel* dist
	cp cloudera-integration/parcel/target/*.json dist
	cp cloudera-integration/csd/target/*.jar* dist

sbt-package:
	sbt package

libdir: 
	mkdir -p target/lib/appenders

parcel: package
	$(MAKE) -C cloudera-integration/parcel/ package

clean:
	sbt clean
	rm -rf dist
	$(MAKE) -C cloudera-integration/parcel/ clean
	$(MAKE) -C cloudera-integration/csd/ clean

sbt-assembly:
	sbt -mem 4096 assembly

sbt-compile:
	sbt -mem 4096 compile

csd: package
	$(MAKE) -C cloudera-integration/csd/ package

install-parcel: parcel
	$(MAKE) -C cloudera-integration/parcel install

install-csd: csd
	$(MAKE) -C cloudera-integration/csd install

.PHONY: version

install: install-parcel install-csd

.PHONY: docs

docs:
	mkdocs serve
