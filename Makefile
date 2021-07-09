#
#    SPDX-License-Identifier: Apache-2.0
#
#    Copyright (C) 2015-2020 Micron Technology, Inc.
#

define HELP_TEXT

Makefile Help
----------------------------------

Primary Targets:

    all             -- clean, build RPMs
    check-hse       -- verify hse RPM is installed
    cleansrcs       -- clean out rpmbuild/SOURCES
    dist            -- build YCSB distribution tarball
    help            -- print this message
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

     RELEASENUM.HSESHA.noarch
endef

ifdef NOTMP
	TOPDIR:="$(HOME)/rpmbuild"
	DEB_TOPDIR:="$(HOME)/debbuild"
else
	TOPDIR:="/tmp/$(shell id -u -n)/rpmbuild"
	DEB_TOPDIR:="/tmp/$(shell id -u -n)/debbuild"
endif

DISTRO_ID_LIKE := $(shell . /etc/os-release && echo $$ID_LIKE)
ifeq ($(DISTRO_ID_LIKE),debian)
	PACKAGE_TARGET = deb
else
	PACKAGE_TARGET = rpm
endif

JENKINS_BUILDNO?=0
REL_CANDIDATE?=FALSE

#
# IMPORTANT NOTES FOR BUILD
#
# RHEL 7 needs to use SCL as follows -
#
# scl enable rh-maven33 devtoolset-7  "make rpm"
#
# Fedora 25 needs to use the toolchain from /shared as follows -
#
# LD_LIBRARY_PATH="/shared/toolchains/rh/devtoolset-7/root/usr/lib64:/shared/toolchains/rh/devtoolset-7/root/usr/lib:/shared/toolchains/rh/devtoolset-7/root/usr/lib64/dyninst:/shared/toolchains/rh/devtoolset-7/root/usr/lib/dyninst:/shared/toolchains/rh/devtoolset-7/root/usr/lib64:/shared/toolchains/rh/devtoolset-7/root/usr/lib" PATH="/shared/toolchains/rh/devtoolset-7/root/usr/bin:$PATH" make rpm
#

RPMSRCDIR:=$(TOPDIR)/SOURCES
TOOLSDIR:=/shared/tools

#
# variables for prebuilt jars/binaries
#
HSE_JAR?="/opt/hse/lib64/hsejni.jar"

RPM_QUERY:=$(shell rpm -q hse >/dev/null; echo $$?)

HSESHA:=.$(shell hse1 -Vv | grep '^sha' | awk '{print $$2}' | cut -c1-7)
ifeq ($(HSESHA),.)
    HSESHA:=.nogit
endif
YCSBSHA:=.$(shell git rev-parse --short=7 HEAD)
ifeq ($(YCSBSHA),.)
    YCSBSHA:=.nogit
endif
TSTAMP:=.$(shell date +"%Y%m%d.%H%M%S")


YCSB_VERSION_SUP=$(shell source hse/VERSION;echo $${YCSB_VERSION_SUP})
HSE_BINDING_VERSION=$(shell source hse/VERSION;echo $${HSE_BINDING_VERSION})
HSE_MIN_VERSION_SUP=$(shell source hse/VERSION;echo $${HSE_MIN_VERSION_SUP})

HSE_YCSB_VERSION:=$(YCSB_VERSION_SUP).$(HSE_BINDING_VERSION)

ifeq ($(REL_CANDIDATE), FALSE)
    RPM_RELEASE:=$(HSE_MIN_VERSION_SUP).${JENKINS_BUILDNO}$(HSESHA)$(YCSBSHA)
    DEB_VERSION:=$(HSE_YCSB_VERSION)-$(HSE_MIN_VERSION_SUP).${JENKINS_BUILDNO}$(HSESHA)$(YCSBSHA)
else
    RPM_RELEASE:=$(HSE_MIN_VERSION_SUP).${JENKINS_BUILDNO}
    DEB_VERSION:=$(HSE_YCSB_VERSION)-$(HSE_MIN_VERSION_SUP).${JENKINS_BUILDNO}
endif

#
# variables for debian
#
DEB_PKGNAME:=hse-ycsb_$(DEB_VERSION)_amd64
DEB_PKGDIR:=$(DEB_TOPDIR)/$(DEB_PKGNAME)
DEB_ROOTDIR:=$(DEB_TOPDIR)/$(DEB_PKGNAME)/opt/hse-ycsb

.PHONY: all check-hse cleansrcs dist help srcs rpm deb
all:	package

check-hse:
	#
	# User or Jenkins must install hse before executing this makefile.
	#
	@if [ ! -f $(HSE_JAR) ]; \
	then \
	    echo "Missing hse package!  Cannot build!"; \
	    exit 1; \
	fi

cleansrcs:
	rm -rf $(RPMSRCDIR)/*

cleanbuilds:
	rm -rf $(TOPDIR)/{BUILD,RPMS,SRPMS}

dist: check-hse
	mvn versions:set-property -DnewVersion=$(HSE_BINDING_VERSION) -Dproperty=hse.version
	mvn install:install-file -Dfile=$(HSE_JAR) -DgroupId=test.org.hse\
		-DartifactId=hse -Dversion=$(HSE_BINDING_VERSION) -Dpackaging=jar
	mvn clean package

help:
	@echo
	$(info $(HELP_TEXT))

rpm: dist srcs
	cp hse-ycsb.spec $(RPMSRCDIR)
	cp distribution/target/ycsb-0.17.0.tar.gz $(RPMSRCDIR)/
	QA_RPATHS=0x0002 rpmbuild -vv -ba \
		--define="tstamp $(TSTAMP)" \
		--define="_topdir $(TOPDIR)" \
		--define="pkgrelease $(RPM_RELEASE)" \
		--define="buildno $(JENKINS_BUILDNO)" \
		--define="hseycsbversion $(HSE_YCSB_VERSION)" \
		$(RPMSRCDIR)/hse-ycsb.spec

deb: dist
	rm -rf $(DEB_TOPDIR)
	mkdir -p $(DEB_TOPDIR)
	mkdir -p $(DEB_PKGDIR)
	mkdir -p $(DEB_ROOTDIR)
	cp distribution/target/ycsb-0.17.0.tar.gz $(DEB_TOPDIR)
	cd $(DEB_TOPDIR) && tar xf ycsb-0.17.0.tar.gz
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/bin $(DEB_ROOTDIR)
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/hse-binding $(DEB_ROOTDIR)
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/lib $(DEB_ROOTDIR)
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/mongodb-binding $(DEB_ROOTDIR)
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/rocksdb-binding $(DEB_ROOTDIR)
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/workloads $(DEB_ROOTDIR)
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/LICENSE.txt $(DEB_ROOTDIR)
	cp -a $(DEB_TOPDIR)/ycsb-0.17.0/NOTICE.txt $(DEB_ROOTDIR)
	mkdir $(DEB_PKGDIR)/DEBIAN
	cp debian/control $(DEB_PKGDIR)/DEBIAN
	sed -i 's/@VERSION@/$(DEB_VERSION)/' $(DEB_PKGDIR)/DEBIAN/control
	cd $(DEB_TOPDIR) && dpkg-deb -b $(DEB_PKGNAME)

package: $(PACKAGE_TARGET)

srcs: cleansrcs
	mkdir -p $(TOPDIR)/{BUILD,RPMS,SOURCES,SPECS,SRPMS}

