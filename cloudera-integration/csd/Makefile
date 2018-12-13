projectVersion = $(shell make --no-print-directory --silent -C ../../ version)
csdVersion = $(projectVersion)-cdh5

packageName = PULSE-$(csdVersion)

package: clean
	mkdir -p target/$(packageName)
	echo building jar
	cp -r aux images scripts target/$(packageName)
	mkdir target/$(packageName)/descriptor
	sed 's/{{ version }}/$(csdVersion)/' < descriptor/service.sdl > target/$(packageName)/descriptor/service.sdl
	jar -cvf target/$(packageName).jar -C target/$(packageName) .
	sha1sum target/$(packageName).jar | awk '{ print $$1 }' > target/$(packageName).jar.sha
	$(MAKE) validate
	echo complete.

validate: descriptor/service.sdl
	echo validating service.sdl
	../validator -s descriptor/service.sdl

place: 
	echo placing jar in /opt/cloudera/csd
	scp target/$(packageName).jar manager.valhalla.phdata.io:/opt/cloudera/csd
	echo setting permissions
	ssh manager.valhalla.phdata.io chown cloudera-scm: /opt/cloudera/csd/$(packageName).jar

clean: 
	rm -rf target
