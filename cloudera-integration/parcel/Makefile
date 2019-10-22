projectVersion = ${VERSION}
cdh_version = cdh${CDH_VERSION}
parcelVersion = $(projectVersion)-$(cdh_version)
packageName = PULSE-$(parcelVersion)
distro7 = PULSE-$(parcelVersion)-el7
distro6 = PULSE-$(parcelVersion)-el6
distrosles12 = PULSE-$(parcelVersion)-sles12


package: clean jars meta control conf
	../validator -d target/$(packageName)/
	tar  zcvf target/$(distro7).parcel -C target $(packageName)/
	../validator -f target/$(distro7).parcel
	tar  zcvf target/$(distro6).parcel -C target $(packageName)/
	../validator -f target/$(distro6).parcel
	tar  zcvf target/$(distrosles12).parcel -C target $(packageName)/
	../validator -f target/$(distrosles12).parcel
	$(MAKE) sha
	$(MAKE) manifest

version:
	printf $(parcelVersion)


sha:
	sha1sum target/$(distro7).parcel | awk '{ print $$1 }' > target/$(distro7).parcel.sha
	sha1sum target/$(distro6).parcel | awk '{ print $$1 }' > target/$(distro6).parcel.sha
	sha1sum target/$(distrosles12).parcel | awk '{ print $$1 }' > target/$(distrosles12).parcel.sha


install-dir:
	cp target/$(packageName) /opt/cloudera/parcel-repo
	chown cloudera-scm: /opt/cloudera/parcel-repo/$(packageName)

dir:
	mkdir -p target/$(packageName)/lib

control:
	mkdir -p target/$(packageName)/scripts
	cp ../../bin/control.sh target/$(packageName)/scripts

jars: dir 
	cp -r ../../target/lib target/$(packageName)/

meta: dir
	mkdir target/$(packageName)/meta
	sed 's/{{ version }}/$(parcelVersion)/' < meta/parcel.json > target/$(packageName)/meta/parcel.json
	cp meta/alternatives.json target/$(packageName)/meta/alternatives.json
	cp meta/pulse_parcel_env.sh target/$(packageName)/meta/pulse_parcel_env.sh

conf: dir
	cp -r conf$$CDH_VERSION target/$(packageName)/conf

validate-parcel-json: parcel
	../validator -p target/$(packageName)/meta/parcel.json

validate-alternatives-json: parcel
	../validator -a target/$(packageName)/meta/alternatives.json

manifest:                                                                          
	./make_manifest.py target 

clean:
	rm -rf target

