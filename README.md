## What is BlueNimble?

BlueNimble is a Hybrid Serverless Platform, focusing on developer productivity and infrastructure agnosticity.

BlueNimble is a new way of creating and running applications with no binding to a spacific clustering or microservices technology. Making it easy to embrasse new technologies and deployment models in the future. 

Using BlueNimble, Developers focus on coding application business logic, without any knowledge of the microservices architecture.

## Quick Start - Single Node

### Install Java 8 or higher
 * On Mac, Windows or Linux, install the latest [OpenJDK JRE](http://openjdk.java.net)   
 * [ORACLE JRE](http://www.oracle.com/technetwork/java/javase/downloads) is also supported 
 * Previous versions to Java 8 are not supported

### Install BlueNimble

#### Install from binaries
* Download either [bluenimble-1.0.0.zip](https://bluenimble-1.0.0) or [bluenimble-1.0.0.tar.gz](https://bluenimble-1.0.0)
* Untar or Unzip the archive to a folder
* If you're a Mac or Linux user, on the command line, enter:
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
#### Install BlueNimble CLI from binaries
* Download either [bluenimble-cli-1.0.0.zip](https://blueNimble-1.0.0) or [bluenimble-cli-1.0.0.tar.gz](https://blueNimble-1.0.0)
* Untar or Unzip the archive to a folder
* If you're a Mac or Linux user, on the command line, enter:
    ````
    cd ~/blueNimble-cli-1.0.0 && chmod 755 *.sh
    ````
The CLI could be installed in any other machine, not necessarily where the server is installed 

#### Start BlueNimble CLI 
* Mac or Linux users
    ````
    ./bnb.sh
    ````
* Windows users
    ````
    ./bnb.bat
    ````
#### Install from sources
* Install git and maven 
* Clone the [blunimble/serverless repository](http://github.com/bluenimble/serverless) from Github. 
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
This command will build both BlueNimble Server and the CLI 

### Check Server startup and install security keys
#### Check Server startup
Type in https://server-ip:8080 (server-ip is where you installed bluenimble) or localhost if you're in your laptop. If you see a page similar to the one below, then BlueNimble is up and running.   

![BlueNimble Server Install Page](https://github.com/bluenimble/serverless/blob/master/assets/images/bluenimble-install-short.png)

#### Download and Install security keys
By default, BlueNimble's built with a playground space. 
* From the install page, click the green button associated with the playground space to download the security keys.
* In order to install playground.keys into the CLI, type in:  
    ````vim
    load keys path_to/playground.keys
    ````
* You can, eventually, check if the keys were installed, by issuing:   
    ````vim
    keys
    ````

#### Create your first api
* Let's create an uber api. To do so, type in:  
    ````vim
    create api uber
    ````
This command should create the project folder and default out-of-the-box services to handle authentication such as signup and login.

License
=======
Copyright 2018 BlueNimble, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
