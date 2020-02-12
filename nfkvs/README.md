<!-- LICENSE TODO
Copyright (c) 2012 YCSB contributors. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You
may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. See accompanying
LICENSE file.
-->

## Quick Start

This section describes how to run YCSB on NFKVS.

### 1. Set Up YCSB

**NOTE:** If you are using a tarball or package distribution, go ahead and skip
to the next step.

Clone the YCSB git repository and compile:

    git clone <YCSB Repo>
    cd YCSB
    mvn clean package

### 2. Run YCSB

Now you are ready to run! First, load the data:

    ./bin/ycsb load nfkvs -s -P workloads/workloada -p nfkvs.kvs_path=mp1/db1/kvs1

Then, run the workload:

    ./bin/ycsb run nfkvs -s -P workloads/workloada -p nfkvs.kvs_path=mp1/db1/kvs1

See the next section for the list of configuration parameters for NFKVS.

## NFKVS Configuration Parameters

1. `nfkvs.kvs_path`: The full path of the KVS storing the YCSB data, e.g. `mp1/db1/kvs1`
2. `nfkvs.kvdb_rparams`: KVDB rparams in `key=value` format, separated by commas (,).
3. `nfkvs.kvs_rparams`: KVS rparams in `key=value` format, separated by commas (,).
4. `nfkvs.pfxlen`: Prefix length to be used in cursor scans.

Some older property names are supported for compatibility with scripts:
- `nfkvs_mpool_name`
- `nfkvs_kvs_name` (<kvdbname>/<kvsname> format)
- `nfkvs_pfxlen`
- `kvdb_params`
- `kvs_params`
