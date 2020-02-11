define HELP_TEXT

Makefile Help
----------------------------------

Primary Targets:

    all             -- clean, build RPMs
    check-hse       -- verify hse RPM is installed
    cleansrcs       -- clean out rpmbuild/SOURCES
    dist            -- build YCSB distribution tarball
    help            -- print this message
    publish         -- publish to SBU repo
    rpm             -- build RPM
    srcs            -- prepare rpmbuild/SOURCES

  The easiest thing is to type "make" on the command line.

Configuration Variables:

  These get set on the command line.

    NOTMP           -- set to anything to place rpmbuild in homedir instead
                       of in /tmp/<username>

Products:
  hse-ycsb RPM in rpmbuild/RPMS/noarch.
  hse-ycsb RPM in rpmbuild/SRPMS.

  RPMs currently use the following format for their release string:

     RELEASENUM.NFSHA.noarch
endef

ifdef NOTMP
	TOPDIR:="$(HOME)/rpmbuild"
else
	TOPDIR:="/tmp/$(shell id -u -n)/rpmbuild"
endif

#
# IMPORTANT NOTES FOR BUILD
#
# RHEL 7 needs to use SCL as follows -
#
# scl enable rh-maven33 devtoolset-7 rh-maven33 "make rpm"
#
# Fedora 25 needs to use the toolchain from /shared as follows -
#
# LD_LIBRARY_PATH="/shared/toolchains/rh/devtoolset-7/root/usr/lib64:/shared/toolchains/rh/devtoolset-7/root/usr/lib:/shared/toolchains/rh/devtoolset-7/root/usr/lib64/dyninst:/shared/toolchains/rh/devtoolset-7/root/usr/lib/dyninst:/shared/toolchains/rh/devtoolset-7/root/usr/lib64:/shared/toolchains/rh/devtoolset-7/root/usr/lib" PATH="/shared/toolchains/rh/devtoolset-7/root/usr/bin:$PATH" make rpm
#

RPMSRCDIR:=$(TOPDIR)/SOURCES
TOOLSDIR:=/shared/tools

#
# variables for build-rpms
#
PROJECT:=hse-ycsb
BR_PREFIX:=/usr/bin

include /usr/share/mse/mse-rpm.mk

#
# variables for prebuilt jars/binaries
#

# git clone ssh://git@bitbucket.micron.com/sbusw/db.git
# cd db/build_unix/ && ../dist/configure --enable-java && make -j
# ls -l db/build_unix/db.jar
# ls -l db/build_unix/.libs/libdb_java-6.1.so
BDB_JAR:="$(TOOLSDIR)/bdb/libdb-6.1.26/db.jar"


NFKVS_JAR:="/usr/share/hse/jni/nfkvsjni.jar"

# git clone -b v5.4.6 /shared/git/mirrors/rocksdb.git
# cd rocksdb && JAVA_HOME=/etc/alternatives/java_sdk make rocksdbjavastatic -j
ROCKSDB_VERSION:="5.17.2"
ROCKSDB_JAR:="target/rocksdb/java/target/rocksdbjni-$(ROCKSDB_VERSION)-linux64.jar"

TROCKSDB_VERSION:="6.1.2"
TROCKSDB_JAR:="target/trocksdb/java/target/rocksdbjni-$(TROCKSDB_VERSION)-linux64.jar"

# git clone -b r3.4.2 /shared/git/mirrors/mongo.git
# cd mongo/src/third_party/wiredtiger
# ./autogen.sh && ./configure --enable-java && make -j
WIREDTIGER_JAR="$(TOOLSDIR)/wiredtiger/mongodb-3.4.2/wiredtiger.jar"
WIREDTIGER_JAVALIB="$(TOOLSDIR)/wiredtiger/mongodb-3.4.2/libwiredtiger_java.so"
WIREDTIGER_SNAPPYLIB="$(TOOLSDIR)/wiredtiger/mongodb-3.4.2/libwiredtiger_snappy.so"
WIREDTIGER_LIB="$(TOOLSDIR)/wiredtiger/mongodb-3.4.2/libwiredtiger-2.9.1.so"

NFVERSION:=$(shell rpm -q hse --qf "%{VERSION}")
NFRELTYPE:=$(shell rpm -q hse --qf "%{RELEASE}" | grep -o '^[[:alpha:]]*')
# NFRELEASE:=$(shell rpm -q hse --qf "%{RELEASE}")
NFSHA:=.$(word 3,$(subst ., ,$(shell rpm -q hse --qf "%{RELEASE}")))
YCSBSHA:=.$(shell git rev-parse --short=7 HEAD)
TSTAMP:=.$(shell date +"%Y%m%d.%H%M%S")

# original download sites are offline
# need to pass these through to the rocksdb repo Makefile
BZIP2_DOWNLOAD_BASE="http://sbu-web.micron.com/sources/bzip2"
SNAPPY_DOWNLOAD_BASE="http://sbu-web.micron.com/sources/snappy"

.PHONY: all check-hse cleansrcs dist help publish srcs rpm
all:	rpm

check-hse:
	#
	# User or Jenkins must install hse-test before executing this makefile.
	#
	@if [ ! -f /usr/share/hse/jni/nfkvsjni.jar ]; \
	then \
	    echo "Missing hse-test RPM!  Cannot build!"; \
	    exit 1; \
	fi

cleansrcs:
	rm -rf $(RPMSRCDIR)/*

cleanbuilds:
	rm -rf $(TOPDIR)/{BUILD,RPMS,SRPMS}

trocksdbjar:
	@if [ -n "$(REBUILD_TROCKSDB_JNI)" ]; \
	then \
	    rm -rf target/trocksdb; \
	    git clone /shared/git/mirrors/trocksdb.git target/trocksdb && \
	    cd target/trocksdb && \
            sed -i '/JAVA_HOME =/d' Makefile && \
	    JAVA_HOME=/etc/alternatives/java_sdk make rocksdbjavastatic -j$(shell nproc); \
        else \
            mkdir -p `dirname $(TROCKSDB_JAR)` && \
	    cp /shared/static/jarfiles/trocksdb/rocksdbjni-$(TROCKSDB_VERSION)-linux64.jar $(TROCKSDB_JAR); \
	fi

dist: check-hse trocksdbjar
	mvn install:install-file -Dfile=$(TROCKSDB_JAR) \
		-DgroupId=test.org.trocksdb -DartifactId=rocksdbjni \
		-Dversion=$(TROCKSDB_VERSION) -Dpackaging=jar
	mvn install:install-file -Dfile=$(BDB_JAR) -DgroupId=test.org.bdb \
		-DartifactId=bdb -Dversion=6.1.26 -Dpackaging=jar
	mvn install:install-file -Dfile=$(NFKVS_JAR) -DgroupId=test.org.nfkvs\
		-DartifactId=nfkvs -Dversion=0.1 -Dpackaging=jar
	mvn install:install-file -Dfile=$(WIREDTIGER_JAR) -DgroupId=test.org.wt\
		-DartifactId=wt -Dversion=2.9.1 -Dpackaging=jar
	mvn clean package

rpm_ycsblibs:
	rm -rf $(RPMSRCDIR)/ycsblibs
	mkdir -p $(RPMSRCDIR)/ycsblibs
	cp $(WIREDTIGER_JAVALIB) $(RPMSRCDIR)/ycsblibs
	cp $(WIREDTIGER_SNAPPYLIB) $(RPMSRCDIR)/ycsblibs
	cp $(WIREDTIGER_LIB) $(RPMSRCDIR)/ycsblibs
	cd $(RPMSRCDIR) && tar czf ycsblibs.tar.gz ycsblibs
	rm -rf $(RPMSRCDIR)/ycsblibs

help:
	@echo
	$(info $(HELP_TEXT))

publish:
	$(BR_PREFIX)/build-rpms --rpmdir $(TOPDIR) --basename $(PROJECT)\
		--publish --releasever $(RELEASEVER)

#
# NOTE: need to set QA_RPATHS to deal with invalid rpaths in
# libwiredtiger_java.so
#
rpm: dist srcs rpm_ycsblibs
	cp hse-ycsb.spec $(RPMSRCDIR)
	cp distribution/target/ycsb-0.17.0.tar.gz $(RPMSRCDIR)
	QA_RPATHS=0x0002 rpmbuild -vv -ba \
		--define="tstamp $(TSTAMP)" \
		--define="nfversion $(NFVERSION)" \
		--define="nfsha $(NFSHA)" \
		--define="ycsbsha $(YCSBSHA)" \
		--define="_topdir $(TOPDIR)" \
		$(RPMSRCDIR)/hse-ycsb.spec

srcs: cleansrcs
	mkdir -p $(TOPDIR)/{BUILD,RPMS,SOURCES,SPECS,SRPMS}

