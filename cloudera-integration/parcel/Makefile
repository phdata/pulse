parcelVersion = $(shell sh ./parcelVersion)
packageName = PULSE-$(parcelVersion)
distro7 = PULSE-$(parcelVersion)-el7
distro6 = PULSE-$(parcelVersion)-el6

parcel: clean jars meta conf
	../validator -d target/$(packageName)/
	tar  zcvf target/$(distro7).parcel -C target $(packageName)/
	../validator -f target/$(distro7).parcel
	tar  zcvf target/$(distro6).parcel -C target $(packageName)/
	../validator -f target/$(distro6).parcel
	$(MAKE) sha

install:
	cp target/$(distro7).parcel* /opt/cloudera/parcel-repo
	chown cloudera-scm: /opt/cloudera/parcel-repo/$(distro7)*

sha:
	sha1sum target/$(distro7).parcel | awk '{ print $$1 }' > target/$(distro7).parcel.sha
	sha1sum target/$(distro6).parcel | awk '{ print $$1 }' > target/$(distro6).parcel.sha

install-dir:
	cp target/$(packageName) /opt/cloudera/parcel-repo
	chown cloudera-scm: /opt/cloudera/parcel-repo/$(packageName)

dir:
	mkdir -p target/$(packageName)/lib

jars: dir 
	cp -r ../../target/lib/ target/$(packageName)/

meta: dir
	mkdir target/$(packageName)/meta
	sed 's/{{ version }}/$(parcelVersion)/' < meta/parcel.json > target/$(packageName)/meta/parcel.json
	cp meta/alternatives.json target/$(packageName)/meta/alternatives.json
	cp meta/pulse_parcel_env.sh target/$(packageName)/meta/pulse_parcel_env.sh

conf: dir
	cp -r conf target/$(packageName)/

validate-parcel-json: parcel
	../validator -p target/$(packageName)/meta/parcel.json

validate-alternatives-json: parcel
	../validator -a target/$(packageName)/meta/alternatives.json

clean:
	rm -rf target

