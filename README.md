# cassandra-connect-storm

[![Build Status](https://travis-ci.com/LujieDuan/cassandra-connect-storm.svg?token=mQpM995srUnJyVcWmxiz&branch=master)](https://travis-ci.com/LujieDuan/cassandra-connect-storm)

## Setup


### Pulling

- First start the Docker compose: ```docker-compose -f docker-compose-pulling.yml up```
- Then go into the Nimbus node: ```docker exec -it nimbus bash```
- Now submit the topology: ```storm jar storm-cassandra-pulling-1.0-SNAPSHOT.jar App mytopology```
- Now go to the browser and visit ```http://localhost:8088/```


### Kafka CDC:
- Using the ```docker-compose.yml``` for in-cassandra-node file watching;
- Using the ```docker-compose-rsync.yml``` for rsync file watching; there is a 60 seconds delay for each rsync;


### Storm Kafka Consumer:
- WIP