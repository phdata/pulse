version = ${VERSION}

package: clean sbt-assembly libdir sbt-package jars

jars:
	cp collection-roller/target/scala-2.11/collection-roller-assembly-$(version).jar target/lib/
	cp alert-engine/target/scala-2.11/alert-engine-assembly-$(version).jar target/lib/
	cp log-collector/target/scala-2.11/log-collector-assembly-$(version).jar target/lib/
	cp log-appender/target/scala-2.11/log-appender-assembly-$(version).jar target/lib/appenders/
	cp common/target/scala-2.11/common-assembly-$(version).jar target/lib/
	cp log-example/target/scala-2.11/log-example-assembly-$(version).jar target/lib/

test:
	sbt test

test-all: test-cdh5 test-cdh6

test-all-linux: export KUDU_BINARY_CLASSIFIER=linux-x86_64
test-all-linux: test-cdh5 test-cdh6

test-cdh5: export CDH_VERSION=5
test-cdh5: 
	$(MAKE) test

test-cdh6: export CDH_VERSION=6
test-cdh6:
	$(MAKE) test

version:
	echo $$VERSION

version-cdh:
	echo cdh$$CDH_VERSION

dist: parcel csd
	mkdir -p dist$$CDH_VERSION
	cp cloudera-integration/parcel/target/*.parcel* dist$$CDH_VERSION
	cp cloudera-integration/parcel/target/*.json dist$$CDH_VERSION
	cp cloudera-integration/csd/target/*.jar* dist$$CDH_VERSION

dist-cdh5: export CDH_VERSION=5
dist-cdh5: 
	$(MAKE) dist

dist-cdh6: export CDH_VERSION=6
dist-cdh6:
	$(MAKE) dist

sbt-package:
	sbt package

libdir: 
	mkdir -p target/lib/appenders

parcel: package
	$(MAKE) -C cloudera-integration/parcel/ package

clean:
	sbt clean
	rm -rf dist5
	rm -rf dist6
	$(MAKE) -C cloudera-integration/parcel/ clean
	$(MAKE) -C cloudera-integration/csd/ clean

sbt-assembly:
	sbt assembly

sbt-compile:
	sbt compile

csd: package
	$(MAKE) -C cloudera-integration/csd/ package

.PHONY: version
.PHONY: version-cdh

.PHONY: docs

docs:
	mkdocs serve
