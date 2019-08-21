projectVersion = ${VERSION}
cdh_version = cdh${CDH_VERSION}

packageName = PULSE-$(projectVersion)-$(cdh_version)
csdVersion = $(projectVersion)-$(cdh_version)

package: clean
	echo "version $(csdVersion)"
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

clean: 
	rm -rf target
