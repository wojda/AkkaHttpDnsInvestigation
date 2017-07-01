# Akka Http DNS investigation
The project was created to help in investigation of Akka Http client issue. 

# Prerequisite
* sbt 0.13.15 or higher
* docker 

# How to run it
$ sbt ";docker:publishLocal; test"

The command will create two docker images: wojda/server and wojda/akka-client. 
Both are mandatory for running end-to-end AkkaHttpDnsSpec.
The test can be found here: org.danielwojda.investigation.AkkaHttpDnsSpec.

Currently the test is failing and that is the purpose of this project.