<a href="https://www.bluenimble.com">
<img src="assets/images/bnb-banner.jpg" width="100%" alt="BlueNimble Hybrid Serverless Platform">
</a>

## What is BlueNimble?

BlueNimble is a Hybrid Serverless Platform focusing on developer productivity and application portability. 

Create and run scalable APIs and applications without coding or by coding less. 

Focus on application business logic without any knowledge of the underlying microservices architecture.

## Quick Start - Single Node

### Install Java 8 or higher
 * On Mac, Windows or Linux, install the latest [OpenJDK JRE](http://openjdk.java.net) or [ORACLE JRE](http://www.oracle.com/technetwork/java/javase/downloads). Previous versions to Java 8 are not supported.

### Install BlueNimble

#### Install from binaries
* Download either [bluenimble-2.27.0-bin.tar.gz](https://github.com/bluenimble/serverless/releases/download/v2.27.0/bluenimble-2.27.0-bin.tar.gz) or [bluenimble-2.27.0-bin.zip](https://github.com/bluenimble/serverless/releases/download/v2.27.0/bluenimble-2.27.0-bin.zip) and decompress in a folder of your choice
* If you're a Mac or Linux user, set the right .sh file mode:
    ````
    cd ~/bluenimble-2.27.0 && sudo chmod 755 *.sh
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
#### Install the CLI from binaries
* Download either [bluenimble-cli-2.27.0-bin.tar.gz](https://github.com/bluenimble/serverless/releases/download/v2.27.0/bluenimble-cli-2.27.0-bin.tar.gz) or [bluenimble-cli-2.27.0-bin.zip](https://github.com/bluenimble/serverless/releases/download/v2.27.0/bluenimble-cli-2.27.0-bin.zip) and decompress in a folder of your choice
* If you're a Mac or Linux user, set the right .sh file mode:
    ````
    cd ~/blueNimble-cli-2.27.0 && chmod 755 *.sh
    ````
The CLI could be installed in any other machine, not necessarily where the server is installed 

#### Start the CLI 
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
    mvn clean install
    ````
This command will build both BlueNimble Server and the CLI 

### Check Server startup and install security keys
#### Check Server startup
Type in http://server-ip:9090 (server-ip is where you installed bluenimble) or localhost if you're in your laptop. If you see a page similar to the one below, then BlueNimble is up and running.   

![BlueNimble Server Install Page](https://github.com/bluenimble/serverless/blob/master/assets/images/2-server.png)

#### Download and Install security keys
By default, BlueNimble is built with a playground space. 
* From the install page, click the green button associated with the playground space to download the security keys.
* In order to install playground.keys into the CLI, type in:  
![BlueNimble Server Install Page](https://github.com/bluenimble/serverless/blob/master/assets/images/2-icli-s.png)

#### Create your first api
* Let's create an uber api. To do so, type in:  
    ````sql
    create api uber
    ````
This command should create the api project in your local machine. Add 2 security schemes by default 'token' and 'signature' and 5 default IM services (Signup, Activate, Login, OAuth, ChangePassword).  
* Let's create a service. Type in:  
    ````sql
    create service * car
    ````
This command will create 5 services (endpoints) and their functions for the model 'Car' corresponding to 'create', 'update', 'delete', 'get' and 'find'. 
* Now, we are good to run the uber api. To do so, type in:
    ````sql
    push api uber
    ````
* Try it out. 
By using the CLI default template, apis are secure by default, only IM services could be called without providing required authentication information (Token for example). 
First, call the 'Signup' service, simulating a user signing up to your 'uber' web/mobile application. Here if a CURL example:
    ```
    curl -H "Content-Type: application/json" -X POST -d '{"user":"alien@uranus.space","password":"Alien!2025"}' http://server-ip:9090/playground/uber/security/signup
    ```
You should get a token back in response.

Now, call the 'CreateCar' service using this token. Here is a CURL example: 

    
    curl -H "Authorization: Token TheToken" -H "Content-Type: application/json" -X POST -d '{"name":"SpiralOrbit","scope":"SolarSystem"}' http://server-ip:9090/playground/uber/cars
    
    
* Using the default CLI api template, services already storing and reading data from the default database feature added to the current space. Visit the documentation to change it or add a new one database. 

* Access the uber api sources, make some changes to the code, run "push api uber" to try it again. The api sources are located under the CLI workspace, type in: 
    ````sql
    ws
    ````
This command will print out where your api code is stored. You can change the workspace folder by issuing:  

    ws pathToNewFolder
    
From now on, any api you create, will be stored in this folder. 

Normaly, creating a secure api, generate 20 model-services (105 endpoints in total) and pushing the api to run, could be done is 10 minutes. Then, you can visit functions code if required. for 90% of the apps, 80% is CRUD operations with single variations, which means you'll be touching max. to 20 functions instead to 105.    

## Terminology

* BlueNimble can act as an Api Gateway and an Execution Runtime or both. In the single node setup we did, we are running Bluenimble for both. 

* BlueNimble runs a set of Spaces. Each space defines a set of features to use and accessible by the Apis you will push to it. For example, if a space defines a database feature, all Apis, thus the functions deployed to it, will share this same database instance.

* An Api is a set of services and corresponding functions. An Api may also define which security scheme to be used, tracing (logging) and requests tracking. An Api could eventually be pushed to multiple spaces (Dev, QA, Prod, ...) since the only dependency is the set of features this api is using. 

* A Service is a an interface specification which is defined by the service.json file. A service can define validation rules to apply on requests, specific security and eventually an SPI function (Service Provider Implementation)

* Plugins are one of the most important components of the BlueNimble architecture when it comes to application portability. Plugins aren't just extensions, such as supporting a new feature, but they can change the behaviour of anything happening in the server. <br/>
You can create plugins to accept requests through a new network protocol such as COAP, to support new security mechanisms, change the flow of an incoming request, etc. <br/>
Plugins also receive events of changes happening to a space or an api. For example, the Kubernetes or Swarm plugins intercept the "`Push Api`" event to push to the cluster, they also change services SPI fuctions to delegate load to the cluster instead of the Api Gateway.   
  
## Architecture

### High Level Flow Diagram
The figure below is a hight level flow diagram

![BlueNimble Hight-Level Flow-architecture](https://github.com/bluenimble/serverless/blob/master/assets/images/main-opt.png)

### Application Portability  
Even if developers can use any external library in their functions code. We recommend to use assets through the **features/plugins** interfaces. For example, the datasource plugin provides native support to a number of relational databases, you can add a new one to the plugin by only registring the vendor and it's driver. This will free developers from managing security and opening/recycling/pooling of connections. 

Here is the list of the out-of-the-box features:

- Database: All major NoSql and RDBMS databases supprted You can add other vendors by implementing the **Database Feature** plugin. 

![Out-Of-The-Box Database Vendors](https://github.com/bluenimble/serverless/blob/master/assets/images/database-vendors.png)

- Storage: Supports FileSystem - S3 and other blob storage services could be used through a posix compliant interface.

- Messenger: Supports Mail and Mobile Push Notifications and STOMP. Other vendors such as APMQ and Kafka could be added by implementing the Messenger plugin.

- Indexer: Only ElasticSearch is supported. You can implement your own Indexer feature plugin.

- Remoting: Supports HTTP and Binary protocols to integrate with REST/SOAP services and Runtimes in Kubernetes and Swarm. To support additional protocols, such as COAP, You can extend or implement a new Remoting plugin.

## Documentation
- [Developer Guide](https://www.bluenimble.com/docs/guides/developer.html)
- [CLI Guide](https://www.bluenimble.com/docs/guides/icli.html) 
- [Javascript SDK](https://www.bluenimble.com/docs/sdks/javascript/index.html)
- [Java, Scala & Kotlin SDK](https://www.bluenimble.com/docs/sdks/jvm/javadoc/index.html)

Note: Documentation for python and ruby still on the works. 


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
