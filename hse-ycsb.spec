Summary: Yahoo! Cloud Serving Benchmark
Name: hse-ycsb
Version: 2020.02.11
Release: 1%{nfsha}%{ycsbsha}
License: ASL 2.0
Group: Unspecified
URL: https://www.micron.com

Packager: NFSQA@micron.com
Vendor: Micron Technology
Requires: java-headless javapackages-tools python
Obsoletes: mse-ycsb

Source0: ycsb-0.18.0-SNAPSHOT.tar.gz
Source1: ycsblibs.tar.gz

BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root
BuildArch: x86_64

# Do NOT scan jar files for dependencies
AutoReq: no
AutoProv: no

# Do NOT compress jar files
# http://www.makewhatis.com/2011/15/15/remove-unwanted-commpression-in-during-rpmbuild-for-jar-files/
%define __os_install_post %{nil}

%define __mypkg %{name}-%{version}-%{release}
%define __ycsb /opt/micron/ycsb

%if 0%{?fedora} >= 28 || 0%{?rhel} >= 8
# don't try to generate debuginfo RPM
%global debug_package %{nil}

# https://gnu.wildebeest.org/blog/mjw/2017/06/30/fedora-rpm-debuginfo-improvements-for-rawhidef27/
%global _build_id_links none
%endif

%description
YCSB with custom bindings for Native Flash Engine users

%prep
%setup -c -T
%{__tar} xf %{SOURCE0}
%{__tar} xf %{SOURCE1}

%build

%install
mkdir -p %{buildroot}%{__ycsb}
mkdir -p %{buildroot}%{_bindir}
mkdir -p %{buildroot}%{_libdir}
mv -v ycsb-0.18.0-SNAPSHOT/bin %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/lib %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/mongodb-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/nfkvs-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/rocksdb-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/trocksdb-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/wiredtiger-binding %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/workloads %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/LICENSE.txt %{buildroot}%{__ycsb}
mv -v ycsb-0.18.0-SNAPSHOT/NOTICE.txt %{buildroot}%{__ycsb}
mv -v ycsblibs/libwiredtiger-2.9.1.so %{buildroot}%{_libdir}
mv -v ycsblibs/libwiredtiger_java.so %{buildroot}%{_libdir}
mv -v ycsblibs/libwiredtiger_snappy.so %{buildroot}%{_libdir}

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
%{__ycsb}/nfkvs-binding
%{__ycsb}/rocksdb-binding
%{__ycsb}/trocksdb-binding
%{__ycsb}/wiredtiger-binding
%{__ycsb}/workloads
%{__ycsb}/LICENSE.txt
%{__ycsb}/NOTICE.txt
%{_libdir}/libwiredtiger-2.9.1.so
%{_libdir}/libwiredtiger_java.so
%{_libdir}/libwiredtiger_snappy.so
%defattr(0755, root, root, 0755)
%{__ycsb}/bin/ycsb
%{__ycsb}/bin/ycsb.sh

%changelog
* Tue Feb 11 2020 Tom Blamer <tblamer@micron.com> - 2020.02.11
- Rebase on master (currenty 0.18.0-SNAPSHOT)

* Thu Feb 6 2020 Tom Blamer <tblamer@micron.com> - 2020.02.06
- Convert recordcount and operationcount variables from int to long

* Tue Jan 14 2020 Bhavesh Vasandani <bvasandani@micron.com> - 2020.01.14
- Rename nfe to hse

* Mon Sep 9 2019 Tom Blamer <tblamer@micron.com> - 2019.09.09
- rocksdb: Update rocksdbjni to 6.2.2
- Add optionsfile property to rocksdb and trocksdb
- Remove rocksdbexperimental
- Change version schema to YYYY.MM.DD

* Wed Sep 4 2019 Tom Blamer <tblamer@micron.com> - 0.15.0-25
- Add trocksdb binding

* Wed Aug 14 2019 Tom Blamer <tblamer@micron.com> - 0.15.0-24
- Pass comma instead of semicolon to nfkvsjni API

* Fri Aug 9 2019 Nabeel <nmeeramohide@micron.com> - 0.15.0-23
- Restored csched_scatter_pct for scans

* Tue Jul 9 2019 Tom Blamer <tblamer@micron.com> - 0.15.0-22
- Correction in nfkvs readme

* Tue Jun 25 2019 Tom Blamer <tblamer@micron.com> - 0.15.0-21
- Clean up property names in nfkvs binding

* Thu Jun 20 2019 Sundararaman Mohanram <smohanram@micron.com> - 0.15.0-20
- Removed csched_scatter_pct for scans

* Mon Jun 17 2019 Sundararaman Mohanram <smohanram@micron.com> - 0.15.0-19
- fixed kvdbParams initialization

* Mon Jun 17 2019 Tom Blamer <tblamer@micron.com> - 0.15.0-18
- Rebuild against nfv_1_4_0
- Hack to allow building on fc28

* Tue Apr 23 2019 Tom Blamer <tblamer@micron.com> - 0.15.0-17
- Implement kvdb_params option in nfkvs binding

* Mon Feb 25 2019 Tom Blamer <tblamer@micron.com> - 0.15.0-16
- Implement kvs pfxlen option in nfkvs binding

* Thu Dec 06 2018 Tom Blamer <tblamer@micron.com> - 0.12.0-12
- Build rocksdb jar file from scratch

* Mon Oct 29 2018 Tom Blamer <tblamer@micron.com> - 0.12.0-11
- Rename mse to nfe

* Wed Oct 25 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-10
- Dump key/value sizes in mapkeeper binding

* Tue Oct 03 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-8
- Add more properties to rocksdb binding
- Update rocksdb README
- Update wiredtiger README

* Fri Sep 29 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-7
- RocksDB 5.7.3

* Mon Sep 18 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-6
- Fix rocksdb and wiredtiger for values >= 4KiB
- Fix IntelliJ warnings

* Fri Sep 15 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-5
- Add python to Requires
- Re-enable mongodb binding

* Mon Sep 11 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-4
- Add Java to Requires
- Fix missing WiredTiger dependency

* Wed Sep 06 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-3
- Implement scan in bindings

* Thu Aug 31 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-2
- Add WiredTiger
- Fix typo in nfkvs build

* Tue Aug 29 2017 Tom Blamer <tblamer@micron.com> - 0.12.0-1
- Initial version
