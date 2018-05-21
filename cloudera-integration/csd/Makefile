csdVersion = $(shell sh ./csdVersion)
packageName = PULSE-$(csdVersion)

csd: clean
	mkdir -p target/$(packageName)
	echo building jar
	cp -r aux images scripts target/$(packageName)
	mkdir target/$(packageName)/descriptor
	sed 's/{{ version }}/$(csdVersion)/' < descriptor/service.sdl > target/$(packageName)/descriptor/service.sdl
	jar -cvf target/$(packageName).jar -C target/$(packageName) .
	$(MAKE) validate
	echo complete.

validate: descriptor/service.sdl
	echo validating service.sdl
	../validator -s descriptor/service.sdl

install: 
	echo placing jar in /opt/cloudera/csd
	cp target/$(packageName).jar /opt/cloudera/csd
	echo setting permissions
	chown cloudera-scm: /opt/cloudera/csd/$(packageName).jar

upload: csd
	aws s3 cp target/$(packageName).jar s3://phdata-pulse/csd/$(packageName).jar

clean: 
	rm -rf target
