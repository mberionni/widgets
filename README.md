# Widgets APIs #

## Overview ##
Built with:
* Java 11
* Spring Boot
* Maven

## Description
A web service to work with widgets via HTTP REST API. The service stores only widgets,
assuming that all clients work with the same board.

###Glossary
A Widget is an object on a plane in a Cartesian coordinate system that has coordinates (X, Y),
Z-index, width, height, last modification date, and a unique identifier. X, Y, and Z-index are
integers (may be negative). Width and height are integers > 0.
Widget attributes should be not null.

A Z-index is a unique sequence common to all widgets that
determines the order of widgets (regardless of their coordinates).
Gaps are allowed. The higher the value, the higher the widget
lies on the plane.
###Details
Operations to be provided by the web service:  
● Creating a widget. Having a set of coordinates, Z-index, width, and height, we get a
complete widget description in the response. The server generates the identifier. If a
Z-index is not specified, the widget moves to the foreground (becomes maximum). If the
existing Z-index is specified, then the new widget shifts widget with the same (and
greater if needed) upwards.  
Examples:
1) Given - 1,2,3; New - 2; Result - 1,2,3,4; Explanation: 2 and 3 has been shifted;
2) Given - 1,5,6; New - 2; Result - 1,2,5,6; Explanation: No one shifted;
3) Given - 1,2,4; New - 2; Result - 1,2,3,4; Explanation: Only 2 has been shifted;

● Changing widget data by Id. In response, we get an updated full description of the
   widget. We cannot change the widget id. All changes to widgets must occur atomically.
   That is, if we change the XY coordinates of the widget, then we should not get an
   intermediate state during concurrent reading. The rules related to the Z-index are the
   same as when creating a widget.  
● Deleting a widget. We can delete the widget by its identifier.   
● Getting a widget by Id. In response, we get a complete description of the widget.  
● Getting a list of widgets. In response, we get a list of all widgets sorted by Z-index, from
   smallest to largest.

While getting the list of widgets, we can specify the area in which widgets are located. Only the
widgets that fall entirely into the region fall into the result. Need to modify the existing endpoint
to get a list of widgets. An important condition is to find an implementation method in which the
runtime complexity of the algorithm is on average less than O(n).
For example, we have 3 widgets that have a width and a height of 100. The centers of these
widgets are at points 50:50, 50:100, and 100:100. We want to get only the widgets that are
inside the rectangle, the lower-left point of which is at 0:0, and the upper right is at 100:150

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