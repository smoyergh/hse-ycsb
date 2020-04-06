<!-- 
SPDX-License-Identifier: Apache-2.0

Copyright (C) 2015-2020 Micron Technology, Inc.

This code is derived from the ycsb project.
-->

## Quick Start

This section describes how to run YCSB on HSE.

### 1. Install the HSE runtime package

Make sure you install the HSE runtime package.
e.g., hse*.rpm on RHEL.

### 2. Set Up YCSB

**NOTE:** If you are using a tarball or package distribution, go ahead and skip
to the next step.

Clone the YCSB git repository and compile:

    git clone <YCSB Repo>
    cd YCSB
    mvn clean package

### 3. Run YCSB

Now you are ready to run! First, load the data:

    ./bin/ycsb load hse -s -P workloads/workloada -p hse.mpool_name=mp1

Then, run the workload:

    ./bin/ycsb run hse -s -P workloads/workloada -p hse.mpool_name=mp1

See the next section for the list of configuration parameters for HSE.

## HSE Configuration Parameters
Only hse.mpool_name is mandatory.
    
1. `hse.mpool_name`: Name of the mpool to use storing the YCSB data.`
2. `hse.params`: HSE params in `key=value` format, separated by commas (,).
3. `hse.config_path`: Path to an HSE config.

