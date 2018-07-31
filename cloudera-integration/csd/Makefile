csdVersion = $(shell sh ./csdVersion)
packageName = PULSE-$(csdVersion)

csd: clean
	mkdir -p target/$(packageName)
	echo building jar
	cp -r aux images scripts default-configs target/$(packageName)
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
	cp target/$(packageName).jar /opt/cloudera/csd
	echo setting permissions
	chown cloudera-scm: /opt/cloudera/csd/$(packageName).jar

clean: 
	rm -rf target
