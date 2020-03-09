<!-- 
SPDX-License-Identifier: Apache-2.0

Copyright (C) 2015-2020 Micron Technology, Inc.

This code is derived from the ycsb project.
-->

## Quick Start

This section describes how to run YCSB on HSE.

### 1. Set Up YCSB

**NOTE:** If you are using a tarball or package distribution, go ahead and skip
to the next step.

Clone the YCSB git repository and compile:

    git clone <YCSB Repo>
    cd YCSB
    mvn clean package

### 2. Run YCSB

Now you are ready to run! First, load the data:

    ./bin/ycsb load hse -s -P workloads/workloada -p hse.kvs_path=mp1/kvs1

Then, run the workload:

    ./bin/ycsb run hse -s -P workloads/workloada -p hse.kvs_path=mp1/kvs1

See the next section for the list of configuration parameters for HSE KVS.

## HSE Configuration Parameters
Only hse.kvs_path is mandatory.
    
1. `hse.kvs_path`: The full path of the KVS storing the YCSB data, e.g. `mp1/kvs1`
2. `hse.params`: HSE params in `key=value` format, separated by commas (,).
3. `hse.pfxlen`: Prefix length to be used in cursor scans.
4. `hse.config_path`: Path to an HSE config.

