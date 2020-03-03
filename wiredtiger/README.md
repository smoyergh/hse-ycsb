<!--
Copyright (c) 2012 - 2020 YCSB contributors. All rights reserved.

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

This section describes how to run YCSB on WiredTiger. 

### 1. Set Up YCSB

Clone the YCSB git repository and compile:

    git clone <YCSB Repo>
    cd YCSB
    mvn clean package

### 2. Run YCSB

Now you are ready to run! First, load the data:

    ./bin/ycsb load wiredtiger -s -P workloads/workloada -p wiredtiger.dir=/tmp/wiredtiger

Then, run the workload:

    ./bin/ycsb run wiredtiger -s -P workloads/workloada -p wiredtiger.dir=/tmp/wiredtiger

See the next section for the list of configuration parameters for WiredTiger.

## WiredTiger Configuration Parameters

* ```wiredtiger.dir``` - (required) A path to a folder to hold the WiredTiger data files.
    * EX. ```/tmp/ycsb-wiredtiger-data```
* ```wiredtiger.configstring``` - The entire configuration string to be passed to `wiredtiger_open`.  See http://source.wiredtiger.com/2.9.2/classcom_1_1wiredtiger_1_1db_1_1wiredtiger.html for details.
    * A default config string is hard coded in the binding, which is subject to change in future YCSB builds.
