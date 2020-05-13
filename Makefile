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
else
	TOPDIR:="/tmp/$(shell id -u -n)/rpmbuild"
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
# variables for build-rpms
#
PROJECT:=hse-ycsb
BR_PREFIX:=/usr/bin
RELEASEVER:=$(shell rpm --eval "%{dist}" | sed -e 's/^\.//' -e 's/^[a-z]*//') 

#
# variables for prebuilt jars/binaries
#

HSE_JAR:="/usr/share/hse/jni/hsejni.jar"

HSEVERSION:=$(shell rpm -q hse --qf "%{VERSION}")
RELTYPE:=$(shell rpm -q hse --qf "%{RELEASE}" | grep -o '^[[:alpha:]]*')
# HSERELEASE:=$(shell rpm -q hse --qf "%{RELEASE}")
HSESHA:=.$(word 6,$(subst ., ,$(shell hse version)))
ifeq ($(HSESHA),.)
    HSESHA:=".nogit"
endif
YCSBSHA:=.$(shell git rev-parse --short=7 HEAD)
ifeq ($(YCSBSHA),.)
    YCSBSHA:=".nogit"
endif
TSTAMP:=.$(shell date +"%Y%m%d.%H%M%S")

.PHONY: all check-hse cleansrcs dist help srcs rpm
all:	rpm

check-hse:
	#
	# User or Jenkins must install hse before executing this makefile.
	#
	@if [ ! -f /usr/share/hse/jni/hsejni.jar ]; \
	then \
	    echo "Missing hse RPM!  Cannot build!"; \
	    exit 1; \
	fi

cleansrcs:
	rm -rf $(RPMSRCDIR)/*

cleanbuilds:
	rm -rf $(TOPDIR)/{BUILD,RPMS,SRPMS}

dist: check-hse
	mvn install:install-file -Dfile=$(HSE_JAR) -DgroupId=test.org.hse\
		-DartifactId=hse -Dversion=2.0 -Dpackaging=jar
	mvn clean package

help:
	@echo
	$(info $(HELP_TEXT))

rpm: dist srcs
	cp hse-ycsb.spec $(RPMSRCDIR)
	cp distribution/target/ycsb-0.17.0.tar.gz $(RPMSRCDIR)
	QA_RPATHS=0x0002 rpmbuild -vv -ba \
		--define="tstamp $(TSTAMP)" \
		--define="hseversion $(HSEVERSION)" \
		--define="hsesha $(HSESHA)" \
		--define="ycsbsha $(YCSBSHA)" \
		--define="_topdir $(TOPDIR)" \
		--define="rel_candidate $(REL_CANDIDATE)" \
		--define="buildno $(JENKINS_BUILDNO)" \
		$(RPMSRCDIR)/hse-ycsb.spec

srcs: cleansrcs
	mkdir -p $(TOPDIR)/{BUILD,RPMS,SOURCES,SPECS,SRPMS}

