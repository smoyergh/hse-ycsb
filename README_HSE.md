# YCSB with HSE

This fork of YCSB (Yahoo!&reg; Cloud Serving Benchmark) integrates HSE
with YCSB 0.17.0.  The goal is to demonstrate the benefits of HSE
within an industry-standard benchmark.

The reader is assumed to be familiar with configuring and running YCSB,
as well as HSE concepts and terminology.
The information provided here is specific to using YCSB with HSE.

## Installing HSE

Clone the [`hse`](https://github.com/hse-project/hse) repo
and follow the documentation in
[`README.md`](https://github.com/hse-project/hse/blob/master/README.md)
to build and install HSE.

You must

* Use HSE version 2.0 or higher
* Perform `meson setup` specifying the `-Dycsb=true` option


## Installing YCSB Dependencies

You may need to install certain tools to build YCSB on your platform.
For example, building YCSB requires Maven 3.

You may also need to install certain libraries.  Below are representative
examples to help you determine what is needed for your particular platform.

### RHEL 8

    $ sudo dnf install maven rpm-build

### Ubuntu 18.04

    $ sudo apt-get install maven


> TODO: Validate if this is the minimum list needed for a vanilla
> RHEL 8 and Ubuntu 18.04 build with Maven.  E.g., we can probably
> eliminate rpm-build, but there could be other packages we need to add.


## Installing YCSB with HSE

Clone the [`hse-ycsb`](https://github.com/hse-project/hse-ycsb) repo
and checkout the latest release tag.  Releases are named `rA.B.C.D.E-hse` where

* `A.B.C` is the YCSB version (e.g., 0.17.0)
* `D.E` is our YCSB integration version

For example

    $ git clone https://github.com/hse-project/hse-ycsb.git
    $ cd hse-ycsb
    $ git checkout rA.B.C.D.E-hse

Build YCSB with HSE as follows, which assumes HSE is installed in
directory `/opt/hse`:

    $ HSE_JAR="/opt/hse/lib64/hsejni.jar"
    $ mvn install:install-file -Dfile="${HSE_JAR}" -DgroupId=test.org.hse -DartifactId=hse -Dversion=0.0 -Dpackaging=jar
    $ mvn -pl hse -am clean package

Extract the resulting tarball to a convenient directory.

    $ tar xf ./hse/target/ycsb-hse-binding-0.17.0.tar.gz -C /tmp


## Configuring YCSB Options

YCSB with HSE adds the following command-line parameters to `ycsb`.

* `hse.kvdb_home` is the path to the KVDB home directory storing the YCSB data


## YCSB Data Storage

YCSB data is stored in an HSE KVDB.  This KVDB must be created before
running the `ycsb` command.  However, `ycsb` will create the required KVS
in the KVDB.


## Running YCSB with HSE

Create a KVDB for running YCSB.

    $ cd /tmp/ycsb-hse-binding-0.17.0
    $ mkdir ycsbKVDB
    $ hse -C ./ycsbKVDB kvdb create

Run YCSB Workload A as follows.

    $ LD_LIBRARY_PATH=/opt/hse/lib64 python2 ./bin/ycsb load hse -P workloads/workloada -p hse.kvdb_home=./ycsbKVDB
    $ LD_LIBRARY_PATH=/opt/hse/lib64 python2 ./bin/ycsb run hse -P workloads/workloada -p hse.kvdb_home=./ycsbKVDB

