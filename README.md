Project speedd-runtime contains the code for the runtime platform for SPEEDD. 
To build it and run the runtime do as follows:

1. cd speedd-runtime
2. mvn clean install assembly:assembly
3. ./start-speedd-topology

The speedd-runtime module depends on a bunch of other projects (e.g. storm, kafka, etc.). Most of the dependencies are available on the internet and will be resolved by maven. There are two projects that are special - they require your explicit action - as follows:

1. storm-kafka-0.8-plus - clone this repository: [https://github.com/kofman-alex/storm-kafka-0.8-plus.git](https://github.com/kofman-alex/storm-kafka-0.8-plus.git). After cloning, build the storm-kafka-plus by running 'mvn clean install' command from the storm-kafka-plus directory.
2. ProtonOnStorm - implementation of Proton CEP engine on storm platform. Download [proton-on-storm.zip](https://github.com/kofman-alex/speedd/blob/master/proton-on-storm.zip "proton-on-storm.zip") archive and extract it into your local maven repository under com/ibm directory, so that the resulting directory layout will be as follows:
<pre>.m2\repository\com\ibm\hrl
├───eep
│   └───EEP
│       └───0.1.0
├───json
│   └───JSON
│       └───0.1.0
└───proton
        ├───Proton
        │       └───0.1.0
        └───ProtonOnStorm
            └───1.0-SNAPSHOT
 </pre>

