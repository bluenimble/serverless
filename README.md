[![The Hybrid Serverless Platform](./assets/images/bluenimble-main.jpg)](https://www.bluenimble.com)

## Getting Started

### Install Java 8
 * On Windows and Linux, install the latest [JRE 8](http://www.oracle.com/technetwork/java/javase/downloads) from Oracle. 
 * On Mac OS X, download and install [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads). 
 * Previous versions of Java are [not supported](https://github.com/processing/processing/wiki/Supported-Platforms#java-versions).
 * OpenJDK is also [not supported](https://github.com/processing/processing/wiki/Supported-Platforms#linux)

### Install BlueNimble
#### Install from sources
* Clone the [source repository](http://github.com/bluenimble/serverless) from Github. 
    * On the command line, enter:
    ````
    git clone https://github.com/bluenimble/serverless.git
    ````
    * You can probably use [Git for Windows](http://windows.github.com/) or [Git for Mac](http://mac.github.com/) instead of the command line, however these aren't tested/supported and we only use the command line for development.
* Build binaries
    * On the command line, enter:
    ````
    mvn install
    ````
#### Install from binaries
* Download either [bluenimble-1.0.0.zip](https://blueNimble-1.0.0) or [bluenimble-1.0.0.tar.gz](https://blueNimble-1.0.0)
* Untar or Unzip the archive to a folder
   * Mac or Linux users
    ````
    ./bnb.sh
    ````
   * Windows users
    ````
    ./bnb.bat
    ````
* If you're using Mac or Linux, on the command line, enter:
    ````
    cd ~/blueNimble-1.0.0 && chmod 755 *.sh
    ````
#### Start BlueNimble
* Mac or Linux users
    ````
    ./bnb.sh
    ````
* Windows users
    ````
    ./bnb.bat
    ````

License
=======
Copyright 2016 BlueNimble, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
