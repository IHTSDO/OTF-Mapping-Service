# OTF-Mapping-Service

This project includes all modules needed to build and deploy a comprehensive solution for terminology mapping.  The data files are not included here due to IP restrictions and must be obtained independently.  

# Config Information

All configuration is managed by the "config" module.  There are two config files within src/main/resources
* config.properties.dev
* config.properties.prod

The files are self-documenting and are used elsewhere throughout the application as maven filters files.  The dev setup assumes development on a windows environment with data files configured in a standard place.  The prod setup assumes develompent on a Unix/Linux style environment also with datafiles configured in a standard place.

# Dev Setup

To build the project as a whole, simply perform a "mvn clean install" at the top level of the project. This will build all the way through the mapping-rest and mapping-webapp war files which can then be deployed to a Tomcat server without any special handling.  The default build uses the config project config.properties.dev configuration file.

To build with the prod configuration you'll need to run "mvn clean install" from the "rest" or "webapp" modules with an additional flag -Dbuild.config=/path/to/config.properties.

# Full documentation

For the complete documentation, see the IHTSDO wiki: https://confluence.ihtsdotools.org/display/MT/Mapping+Tool+Home

(Email ***REMOVED*** to access the wiki if you cannot see it).
