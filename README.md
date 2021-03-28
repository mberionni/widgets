# Widgets APIs #

## Overview ##
Built with:
* Java 11
* Spring Boot
* Maven

## Usage ##

Clone the github repository:  
`git clone https://github.com/mberionni/widgets.git`  
`cd widgets`  

To start the application, run from the command line:  
    `mvn spring-boot:run`  
At this point the web server is ready to receive requests.

Examples (from the command line)  
If `jq` is not installed it can be removed from the following commands.  
- create 3 widgets:  
  `curl -s -X POST localhost:8080/widgets -H 'Content-type:application/json' -d '{"x": 10, "y": 10, "width": 3, "height" : 40}' | jq`  
  `curl -s -X POST localhost:8080/widgets -H 'Content-type:application/json' -d '{"x": 11, "y": 11, "width": 3, "height" : 41}' | jq`  
  `curl -s -X POST localhost:8080/widgets -H 'Content-type:application/json' -d '{"x": 12, "y": 12, "width": 3, "height" : 42}' | jq`  
  
- query the widget with id=2:  
  `curl -s -X GET localhost:8080/widgets/2 | jq`

- query all the widgets:  
  `curl -s -X GET localhost:8080/widgets | jq`

Run `mvn test` to execute the tests.