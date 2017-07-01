# Akka Http DNS investigation
The project was created to help in investigation of Akka Http client issue. 

# Prerequisite
* sbt 0.13.15 or higher
* docker 

# How to run it
$ sbt ";docker:publishLocal; test"

The command will create two docker images: wojda/server and wojda/akka-client. 
Both are mandatory for running end-to-end AkkaHttpDnsSpec. The spec contains two test, 
one healthy to prove that setup is correct, and second failing test that shows that Akka Http client does not respect DNS positive ttl.



# Components
+-----------------+                +------------+                  +-------------+
|                 |  PUT /start    |            |   GET /hostname  |             |
| AkkaHttpDnsSpec | -------------> | AkkaClient | ---------------> |  Server_v1  |
|                 |                |            |                  |             |
+-----------------+                +------------+                  +-------------+

                                                                   +-------------+
                                                                   |             |
                                                                   |  Server_v2  |
                                                                   |             |
                                                                   +-------------+






