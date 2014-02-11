'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap']);
var mapProjectAppDirectives = angular.module('mapProjectAppDirectives', ['ui.boostrap']);

var root_url = "${base.url}/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";
	
//////////////////////////////
// Navigation
//////////////////////////////	

mapProjectAppControllers.controller('MapProjectAppNav',
	function ($scope) {
	
		var changePage = function (newPage) {
			$location.path = newPage;
		};
	});	


	
//////////////////////////////
// Mapping Services
//////////////////////////////	
	
mapProjectAppControllers.controller('MapProjectListCtrl', 
  function ($scope, $http) {
      $http({
        url: root_mapping + "project/projects",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
    	  $scope.projects = data.mapProject;
      }).error(function(error) {
    	  $scope.error = "Error";
      });
 
   /* $scope.orderProp = 'id';	*/
  });



//////////////////////////////
// Content Services
//////////////////////////////	

mapProjectAppControllers.controller('ConceptListCtrl', 
		  function ($scope, $http) {
			      $http({
			        url: root_content + "concept/concepts",
			        dataType: "json",
			        method: "GET",
			        headers: {
			          "Content-Type": "application/json"
			        }
			      }).success(function(data) {
			        $scope.concepts = data.concept;
			      }).error(function(error) {
			    	  $scope.error = "Error";
			    });
			 
			    $scope.orderProp = 'id';	
			  });


mapProjectAppControllers.controller('ConceptDetailCtrl', ['$scope', '$http', '$routeParams',
     function ($scope, $http, $routeParams) {
	  $scope.status = "Loading...";
      $scope.statusnote = "This process has not been optimized, and may be particularly slow on the EC2 server (mapping.snomedtools.org).";
  	  $scope.conceptId = $routeParams.conceptId;
  	  $http({
          url: root_content + "concept/" + $routeParams.terminology + "/" + $routeParams.version + "/id/" +  $routeParams.conceptId,
          dataType: "json",
          method: "GET",
          headers: {
            "Content-Type": "application/json"
          }
        }).success(function(data) {
          $scope.status = "Load complete!";
          $scope.statusnote = "";
          $scope.concept = data;
        }).error(function(error) {
        	$scope.status = "Load error!";
            $scope.statusnote = "";
        	console.print("Error in conceptdetailctrol");
        	
        // check for unmapped descendants
        }).then(function(data) {
        	
        	$http({
                url: "${base.url}/mapping-rest/mapping/concept/" + $routeParams.terminology + "/" + $routeParams.version + "/id/" +  $routeParams.conceptId + "/threshold/11",
                dataType: "json",
                method: "GET",
                headers: {
                  "Content-Type": "application/json"
                }
              }).success(function(data) {
            	  $scope.unmappedDescendants = data.concept;     	  
              }).error(function(error) {
            	  console.print("Error in unmapped descendants");
              });
        	
        });
  	  
  	  $scope.hasUnmappedDescendants = function(id) {
  		  
  	  };
  	    

  }]);

  
//////////////////////////////
// Specialized Services
//////////////////////////////	

mapProjectAppControllers.controller('ProjectCreateCtrl', ['$scope', '$http',,
   function ($scope, $http) {

	$scope.queryConceptStatus = "[No concept query executed]";
}]);

/*
 * Controller for retrieving and displaying records associated with a concept
 */
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	// scope variables
	$scope.error = "";		// initially empty
	$scope.rows = "["; // beginning of Json array
	
	// local variables
	var records = [];
	var projects = [];
	var project_names = [];
	var project_refSetIds = [];
	

	// retrieve all records with this concept id
	$http({
	        url: root_mapping + "record/conceptId/" + $routeParams.conceptId,
	        dataType: "json",
	        method: "GET",
	        headers: {
	          "Content-Type": "application/json"
	        }	
	      }).success(function(data) {
	    	  $scope.records = data.mapRecord;
	          records = data.mapRecord;
	      }).error(function(error) {
	    	  $scope.error = $scope.error + "Could not retrieve records. "; 
	     
	      }).then(function() {
	      
		// retrieve project information   
		 $http({
			 url: root_mapping + "project/projects",
			 dataType: "json",
		        method: "GET",
		        headers: {
		          "Content-Type": "application/json"
		        }	
		      }).success(function(data) {
		          projects = data.mapProject;
		      }).error(function(error) {
		    	  $scope.error = $scope.error + "Could not retrieve projects. "; 
		     
		      }).then(function() {

		    	  // set terminology based on first map record's map project
		    	  var terminology = "null";
		    	  var version = "null";
		    	  
		    	  for (var i = 0; i < projects.length; i++) {
		    		  if (projects[i].id == records[0].mapProjectId) {
		    			  terminology = projects[i].sourceTerminology;
		    			  version = projects[i].sourceTerminologyVersion;
		    			  break;
		    		  }
		    	  }
		    	  
		    	  console.debug(terminology);
		    	  console.debug(version);
		    	  
		    	  
		    	  // find concept based on source terminology
		    	  $http({
	    			 url: root_content + "concept/" 
	    			 				   + terminology + "/" 
	    			 				   + version 
	    			 				   + "/id/" 
	    			 				   + $routeParams.conceptId,
	    			 dataType: "json",
	    		     method: "GET",
	    		     headers: {
	    		          "Content-Type": "application/json"
	    		     }	
	    		  }).success(function(data) {
	    		          $scope.concept = data;
	    		  }).error(function(error) {
	    		    	  $scope.error = $scope.error + "Could not retrieve Concept. ";    
	    		  });
		    	  
		    	  // save refSetIds and project names
		    	  project_names = new Array(projects.length);
		    	  project_refSetIds = new Array(projects.length);
		    	  
		    	  for (var i=0; i<projects.length; i++) {
		    		  project_names[i] = projects[i].name;
		    		  project_refSetIds[i] = projects[i].refSetId;
		    	  }
		      });
	      });
	
		$scope.getProjectName = function(record) {
			var projectId = record.mapProjectId;
			return project_names[parseInt(projectId, 10)-1];
		};
	
		$scope.getProjectRefSetId = function(record) {
			var projectId = record.mapProjectId;
			return project_refSetIds[parseInt(projectId, 10)-1];
		};
	
	}]);
                                                              



// TODO Add test for coming from project list page (i.e. pass the project to this controller)
mapProjectAppControllers.controller('MapProjectDetailCtrl', ['$scope', '$http', '$routeParams', '$sce',
   function ($scope, $http, $routeParams, $sce) {
	
	$scope.headers = [
	                  {value: 'conceptId', title: 'Concept'},
	                  {value: 'conceptName', title: 'Concept Name'},
	                  {value: 'mapGroup', title: 'Group Id'},
	                  {value: 'mapPriority', title: 'Map Priority'},
	                  {value: 'rule', title: 'Rule'},
	                  {value: 'targetId', title: 'Target'},
	                  {value: 'targetName', title: 'Target Name'},
	                  {value: 'actions', title: 'Actions'}
	                  
	                  ];
	$scope.filterCriteria = {
			pageNumber: 1,
			sortDir: 'asc',
			sortedBy: 'conceptId'
	};
	
	
	  $scope.projectId = $routeParams.projectId;
	  
	  // status variables
	  $scope.unmappedDescendantsPresent = false;
	  $scope.mapNotesPresent = false;
	  $scope.mapAdvicesPresent = false;
	  
	  // error variables
	  $scope.errorProject = "";
	  $scope.errorConcept = "";
	  $scope.errorRecords = "";
	 
	  
	  // for collapse directive
	  $scope.isCollapsed = true;
	  
	  // retrieve project information
	 $http({
        url: root_mapping + "project/id/" + $scope.projectId,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.project = data;
        $scope.errorProject = "Project retrieved";
      }).error(function(error) {
    	  $scope.errorProject = "Could not retrieve project"; 
     
      }).then(function(data) {
 		 
    	  // retrieve any concept associated with this project
    	  $http({
    		  url: root_content + "concept/id/" + $scope.project.refSetId,
    		  dataType: "json",
    		  method: "GET",
    		  headers: {
    			  "Content-Type": "application/json"
    		  }
    	  }).success(function(data) {
    		  $scope.concept = data;
    	  }).error(function(error) {
    		  $scope.errorRecord = "Error retrieving concept";
    	  });
      }).then(function(data) {
    	  
    	  // get pagination variables
    	  $scope.setPagination(10);
    	  
    	  // load first page
    	  $scope.retrieveRecords(1);
      });
	 
	 $scope.to_trusted = function(html_code) {
	    return $sce.trustAsHtml(html_code);
	 }
	 
	 // function to set the relevant pagination fields
	 $scope.setPagination = function(recordsPerPage) {
		 
		 // set scope variable for total records
		 $scope.getNRecords();
				 
		 // set pagination variables
		 $scope.recordsPerPage = recordsPerPage;
		 $scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
	 };
    	
	 // function to retrieve records for a specified page
	 $scope.retrieveRecords = function(page) {
		 
		 // retrieve pagination information for the upcoming query
		 $scope.setPagination(10);
		
		 console.debug("Switching to page " + page);
		 
		 var startRecord = (page - 1) * $scope.recordsPerPage; 
		 var query_url;
		 
		 if ($scope.query == null) {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/" + startRecord + "-" + $scope.recordsPerPage;
		 } else {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/" + startRecord + "-" + $scope.recordsPerPage + "/" + $scope.query;
		 }
		 
		 console.debug(query_url);
		
		// retrieve map records
		  $http({
			  url: query_url,
			  dataType: "json",
			  method: "GET",
			  headers: {
				  "Content-Type": "application/json"
			  }
		  }).success(function(data) {
			  $scope.records = data.mapRecord;
			  $scope.statusRecordLoad = "";
			  $scope.recordPage = page;

		  }).error(function(error) {
			  $scope.errorRecord = "Error retrieving map records";
			  console.debug("changeRecordPage error");
		  }).then(function(data) {
			  
			  // check if icon legends are necessary
			  $scope.unmappedDescendantsPresent = false;
			  $scope.mapNotesPresent = false;
			  $scope.mapAdvicesPresent = false;
			  
			  // check if any notes or advices are present are present
			  for (var i = 0; i < $scope.records.length; i++) {
				  if ($scope.records[i].mapNote.length > 0) {
					  $scope.mapNotesPresent = true;
				  }
				  for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {
					  
					  console.debug("  Checking entry "+ j);
					  if ($scope.records[i].mapEntry[j].mapNote.length > 0) {
						  $scope.mapNotesPresent = true;
					  };
					  if ($scope.records[i].mapEntry[j].mapAdvice.length > 0) {
						  $scope.mapAdvicesPresent = true;
					  }
				  };
			  };
					 			  
			  // check if any advices are present
			  
			  // get unmapped descendants (checking done in routine)
			  if ($scope.records.length > 0) {	
				  getUnmappedDescendants(0);
			  }
		  });
	 }; 
	 
	 // function query for the number of records associated with full set or query
	 $scope.getNRecords = function() {
		 
		 var query_url;
		 
		 if ($scope.query == null) {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/nRecords";
		 } else {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/nRecords/" + $scope.query;
		 }
		 
		 // retrieve the total number of records associated with this map project
	   	  $http({
	   		  url: query_url,
	   		  dataType: "json",
	   		  method: "GET",
	   		  headers: {
	   			  "Content-Type": "application/json"
	   		  }
	   	  }).success(function(data) {
	   		  console.debug("getNRecords returned " + data);
	   		  $scope.nRecords = data;
	   		  
	   	  }).error(function(error) {
	   		  $scope.nRecords = 0;
	   		  console.debug("getNRecords error");
	   	  });
	 };
	 
	 $scope.onSort = function (sortedBy, sortDir) {
		 $scope.filterCriteria.sortDir = sortDir;
		 $scope.filterCriteria.sortedBy = sortedBy;
		 $scope.filterCriteria.pageNumber = 1;
	 };
		
		 
	 // TODO This is kind of messy
    function getUnmappedDescendants(index) {
				  
    	  // before processing this record, make call to start next async request
    	  if (index < $scope.records.length-1) {
		  	   getUnmappedDescendants(index+1);
		  }
    	
		  console.debug("Checking record " + index);
		  $scope.records[index].unmappedDescendants = [];
		  
		  // if descendants below threshold for lower-level concept, check for unmapped
		  if ($scope.records[index].countDescendantConcepts < 11) {
			 		  
			  console.debug("  Record has < 11 descendants");

			  $http({
				  url: root_mapping + "concept/" 
				  		+ $scope.project.sourceTerminology + "/"
				  		+ $scope.project.sourceTerminologyVersion + "/"
				  		+ "id/" + $scope.records[index].conceptId + "/"
				  		+ "threshold/11",
				  dataType: "json",
				  method: "GET",
				  headers: {
					  "Content-Type": "application/json"
				  }
			  }).success(function(data) {
				 console.debug("  Found " + data.concept.length + " unmapped descendants");
				 $scope.unmappedDescendantsPresent = true;
				 $scope.records[index].unmappedDescendants = data.concept;
			  });
		  // otherwise check the next record
		  } else {
			  console.debug("Above LLC threshold");
			  
		  }
	  
	 };
}]);


//////////////////////////////////////////////////////
// Directives:  TODO Separate into different file
/////////////////////////////////////////////////////

mapProjectAppControllers.directive('sortBy', function () {
	
	return {
		templateUrl: 'sort-by.html',
		restrict: 'E',
		transclude: true,
		replace: true,
		scope: {
			sortdir: '=',
			sortedby: '=',
			sortvalue: '@',
			onsort: '='
		},
		link: function (scope, element, attrs) {
			scope.sort = function () {
				if (scope.sortedby == scope.sortvalue) 
					scope.sortdir = scope.sortdir == 'asc' ? 'desc' : 'asc';
				else {
					scope.sortedeby = scope.sortvalue;
					scope.sortdir = 'asc';
				}
				scope.onsort(scope.sortedby, scope.sortdir);
			};
		}
	};
});



//////////////////////////////
//Metadata Services
//////////////////////////////

mapProjectAppControllers.controller('MetadataCtrl', 
['$scope', '$http',
                                 
function ($scope, $http) {

$scope.errorMetadata = "";

// retrieve any concept associated with this project
$http({
url: root_metadata + "all/SNOMEDCT/20130131",
dataType: "json",
method: "GET",
headers: {
"Content-Type": "application/json"
}
}).success(function(data) {
$scope.idNameMaps = data.idNameMap;
}).error(function(error) {
$scope.errorMetadata = "Error retrieving metadata";
});
}]);


