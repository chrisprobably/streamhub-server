#!/bin/bash

cd ../target/classes && java -cp .:../../lib/log4j-1.2.14.jar:../../lib/json-20080701.jar -Xms768m -Xmx768m -server -Xconcurrentio -Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=192.168.1.65 com.streamhub.demo.DemoNIOServer

