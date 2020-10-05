.PHONY: jars

jars:
	cd data-source-simulator && sbt assembly
	cd kafkacdchandler && mvn package
	# cd stormkafkaconsumer && mvn package
	cd stormcassandrapulling && mvn package
