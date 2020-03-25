#
#    SPDX-License-Identifier: Apache-2.0
#
#    Copyright (C) 2015-2020 Micron Technology, Inc.
#

Summary: Yahoo! Cloud Serving Benchmark
Name: hse-ycsb-LargeRecordCount
Version: 2020.03.20
Release: 1%{hsesha}%{ycsbsha}
License: Apache-2.0
Group: Unspecified
URL: https://www.micron.com

Packager: NFSQA@micron.com
Vendor: Micron Technology, Inc.
Requires: java-headless javapackages-tools python2

Source0: ycsb-0.17.0.tar.gz

BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root
BuildArch: x86_64

# Do NOT scan jar files for dependencies
AutoReq: no
AutoProv: no

# Do NOT compress jar files
# http://www.makewhatis.com/2011/15/15/remove-unwanted-commpression-in-during-rpmbuild-for-jar-files/
%define __os_install_post %{nil}

%define __mypkg %{name}-%{version}-%{release}
%define __ycsb /opt/micron/ycsb-LargeRecordCount

%if 0%{?fedora} >= 28 || 0%{?rhel} >= 8
# don't try to generate debuginfo RPM
%global debug_package %{nil}

# https://gnu.wildebeest.org/blog/mjw/2017/06/30/fedora-rpm-debuginfo-improvements-for-rawhidef27/
%global _build_id_links none
%endif

%description
YCSB with custom bindings for Heterogeneous-memory Storage Engine

%prep
%setup -c -T
%{__tar} xf %{SOURCE0}

%build

%install
mkdir -p %{buildroot}%{__ycsb}
mkdir -p %{buildroot}%{_bindir}
mkdir -p %{buildroot}%{_libdir}
mv -v ycsb-0.17.0/bin %{buildroot}%{__ycsb}
mv -v ycsb-0.17.0/lib %{buildroot}%{__ycsb}
mv -v ycsb-0.17.0/mongodb-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.17.0/hse-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.17.0/rocksdb-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.17.0/workloads %{buildroot}%{__ycsb}
mv -v ycsb-0.17.0/LICENSE.txt %{buildroot}%{__ycsb}
mv -v ycsb-0.17.0/NOTICE.txt %{buildroot}%{__ycsb}

%clean
%{__rm} -rf %{buildroot}

%post
/sbin/ldconfig

%postun
/sbin/ldconfig

%files
%defattr(0644, root, root, 0755)
%{__ycsb}/bin/bindings.properties
%{__ycsb}/bin/ycsb.bat
%{__ycsb}/lib
#%{__ycsb}/mapkeeper-binding
%{__ycsb}/mongodb-binding
%{__ycsb}/hse-binding
%{__ycsb}/rocksdb-binding
%{__ycsb}/workloads
%{__ycsb}/LICENSE.txt
%{__ycsb}/NOTICE.txt
%defattr(0755, root, root, 0755)
%{__ycsb}/bin/ycsb
%{__ycsb}/bin/ycsb.sh

%changelog
