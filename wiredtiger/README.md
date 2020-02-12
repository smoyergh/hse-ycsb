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

Clone the YCSB git repository and compile:

    git clone <YCSC Repo>
    cd YCSB
    mvn clean package

### 2. Run YCSB
    
Now you are ready to run! First, load the data:

    ./bin/ycsb load wiredtiger -s -P workloads/workloada -p wiredtiger.path=/tmp/wiredtiger

Then, run the workload:

    ./bin/ycsb run wiredtiger -s -P workloads/workloada -p wiredtiger.path=/tmp/wiredtiger

See the next section for the list of configuration parameters for WiredTiger.

## WiredTiger Configuration Parameters

- `wiredtiger.configstring`
  - The entire configuration string to be passed to `wiredtiger_open`.  See http://source.wiredtiger.com/2.9.1/classcom_1_1wiredtiger_1_1db_1_1wiredtiger.html#a41881fa91c0b8820ad8a9f5bb91950f8 for details.
  - Default value is hard coded in the binding, subject to change in future YCSB builds.
- `wiredtiger.path` (**required**)
  - The database path.
  - No default.

