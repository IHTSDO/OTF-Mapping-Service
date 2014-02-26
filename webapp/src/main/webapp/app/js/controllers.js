'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap']);
var mapProjectAppDirectives = angular.module('mapProjectAppDirectives', ['ui.boostrap']);

var root_url = "${base.url}/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";

mapProjectAppControllers.run(function($rootScope) {
    $rootScope.userName = null;
    $rootScope.role = null;
});


//////////////////////////////
// Navigation
//////////////////////////////	



mapProjectAppControllers.controller('LoginCtrl', 
	 function ($scope, $rootScope, $location) {
	// logout icon returns to login page, so reinitialize
	$rootScope.userName = null;
	$rootScope.role = null;
	
	// initial values for pick-list
	 $scope.roles = [
	       {name:'Viewer', value:1},
	       {name:'Specialist', value:2},
	       {name:'Lead', value:3},
	       {name:'Administrator', value:4}];
	 $scope.role = $scope.roles[0];  
	 
	 // login button directs to next page based on role selected
	 $scope.go = function (path) {
		 if ($scope.role.name == "Specialist") {
			 path = "/specialist/dash";
		 } else if ($scope.role.name == "Lead") {
			 path = "/lead/dash";
		 } else if ($scope.role.name == "Administrator") {
			 path = "/admin/dash";
		 } else if ($scope.role.name == "Viewer") {
			 path = "/project/projects/";
		 }
			$location.path(path);
			$rootScope.userName = $scope.userName;
			$rootScope.role = $scope.role;
		};
	 });


	
//////////////////////////////
// Mapping Services
//////////////////////////////	
	
mapProjectAppControllers.controller('MapProjectListCtrl', 
  function ($scope, $http) {
	
	  // initialize as empty to indicate still initializing database connection
	  $scope.projects = [];
	
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

  });



//////////////////////////////
// Content Services
//////////////////////////////	


  
//////////////////////////////
// Specialized Services
//////////////////////////////	


/*
 * Controller for retrieving and displaying records associated with a concept
 */
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	// scope variables
	$scope.error = "";		// initially empty
	
	// local variables
	var records = [];
	var projects = [];

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
	    		          $scope.findUnmappedDescendants();
	    		          
	    		          // find inverse relationships based on source terminology
	    		    	  $http({
	    		    			 url: root_content + "concept/" 
	    		    			 				   + terminology + "/" 
	    		    			 				   + version 
	    		    			 				   + "/id/" 
	    		    			 				   + $routeParams.conceptId
	    		    			 				   + "/children",
	    		    			 dataType: "json",
	    		    		     method: "GET",
	    		    		     headers: {
	    		    		          "Content-Type": "application/json"
	    		    		     }	
	    		    		  }).success(function(data) {
	    		    			  	  console.debug(data);
	    		    		          $scope.concept.children = data.searchResult;
	    		    		  }).error(function(error) {
	    		    		    	  $scope.error = $scope.error + "Could not retrieve Concept children. ";    
	    		    		  });
	    		  }).error(function(error) {
	    		    	  $scope.error = $scope.error + "Could not retrieve Concept. ";    
	    		  });
		    	  
		    	  	
		      });
	      });
	
		$scope.getProjectName = function(record) {
			
			for (var i = 0; i < projects.length; i++) {
				if (projects[i].id == record.mapProjectId) {
					return projects[i].name;
				}
			}
			return null;
		};
		
		$scope.findUnmappedDescendants = function() {
			
			$scope.concept.unmappedDescendants = [];
			
			if ($scope.records.length > 0) {
				if ($scope.records[0].countDescendantConcepts < 11) {
					
					$http({
						  url: root_mapping + "concept/" 
						  		+ $scope.concept.terminology + "/"
						  		+ $scope.concept.terminologyVersion + "/"
						  		+ "id/" + $scope.concept.terminologyId + "/"
						  		+ "threshold/10",
						  dataType: "json",
						  method: "GET",
						  headers: {
							  "Content-Type": "application/json"
						  }
					  }).success(function(data) {
						 console.debug("  Found " + data.count + " unmapped descendants");
						 if (data.count > 0) $scope.unmappedDescendantsPresent = true;
						 $scope.concept.unmappedDescendants = data.searchResult;
					  });

				}
			}
			
		};
	
		
	}]);
                                                              



// TODO Add test for coming from project list page (i.e. pass the project to this controller)
mapProjectAppControllers.controller('MapProjectRecordCtrl', ['$scope', '$http', '$routeParams', '$sce',
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
	 };
	 
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
			  
			  // check if any notes or advices are present
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
	 
	 // stuck here for html cleanliness -- probably want to move
	 $scope.getSearchHelp = 
		"<strong>Search Options</strong>" +
		 "<table>" +
		 "<thead>" +
		 "<tr>" +
		 "	<th>Option</th>" +
		 " 	<th>Usage</th>" +
		 "	<th>Example</th>" +
		 "</tr>" +
		 "</thead>" +
		 "</table>";
	
	 
	// function to return trusted html code (for tooltip content)
	$scope.to_trusted = function(html_code) {
	    return $sce.trustAsHtml(html_code);
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
				  		+ "threshold/10",
				  dataType: "json",
				  method: "GET",
				  headers: {
					  "Content-Type": "application/json"
				  }
			  }).success(function(data) {
				 console.debug("  Found " + data.count + " unmapped descendants");
				 if (data.count > 0) $scope.unmappedDescendantsPresent = true;
				 $scope.records[index].unmappedDescendants = data.searchResult;
			  });
		  // otherwise check the next record
		  } else {
			  console.debug("Above LLC threshold");
			  
		  }
	  
	 };
}]);

mapProjectAppControllers.controller('MapProjectDetailCtrl', 
		['$scope', '$http', '$routeParams', '$sce',
		 function ($scope, $http, $routeParams, $sce) {
			
			
			console.debug("In MapProjectDetailCtrl");
			
			// retrieve project information
			 $http({
		        url: root_mapping + "project/id/" + $routeParams.projectId,
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
		    	  
		    	  console.debug($scope.project.destinationTerminology);
		    	 
		    	  // determine if this project has a principles document
		    	  // TODO: THIS WILL BE CODED INTO PROJECT LATER
		    	  if ($scope.project.destinationTerminology == "ICD10") {
		    		  $scope.project.mapPrincipleDocumentPath = "doc/";
		    		  $scope.project.mapPrincipleDocument = "ICD10_MappingPersonnelHandbook.docx";
		    		  $scope.project.mapPrincipleDocumentName = "Mapping Personnel Handbook";
		    	  } else {
		    		  $scope.project.mapPrincipleDocument = null;
		    	  }
		    	  
		    	  // sort the principles and advices
		    	  $scope.sortedAdvice = $scope.sortByKey($scope.project.mapAdvice, 'id');
		    	  $scope.sortedPrinciples = $scope.sortByKey($scope.project.mapPrinciple, 'id');
		    	  
		    	  // set pagination variables
		    	  $scope.pageSize = 5;
		    	  $scope.getPagedAdvice(1);
		    	  $scope.getPagedPrinciple(1);
		    	  $scope.orderProp = 'id';
		    	  

		    	  
		    	  console.debug("Advice Pages: " + $scope.numPagesAdvice);
		    	  console.debug("Principle Pages: " + $scope.numPagesPrinciple);
		      
		      });
			
			
			// function to return trusted html code (for tooltip content)
			$scope.to_trusted = function(html_code) {
			    return $sce.trustAsHtml(html_code);
			 };
			 
			 $scope.getPagedAdvice = function (page) {
				 console.debug("Called paged advice for page " + page);				 
				 $scope.pagedAdvice = $scope.sortedAdvice.slice(
						 (page-1)*$scope.pageSize,
						 page*$scope.pageSize);
			 };
			 
			 $scope.getPagedPrinciple = function (page) {
				 console.debug("Called paged principle for page " + page);
				 $scope.pagedPrinciple = $scope.sortedPrinciples.slice(
						 (page-1)*$scope.pageSize,
						 page*$scope.pageSize);
			 };
			 
			 $scope.sortByKey = function sortById(array, key) {
				    return array.sort(function(a, b) {
				        var x = a[key]; var y = b[key];
				        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
				    });
				};
			 
}]);


//////////////////////////////////////////////////////
// Directives:  TODO Separate into different file
/////////////////////////////////////////////////////


mapProjectAppControllers.directive('otfHeaderDirective', function() {
    
	return {
        templateUrl: './partials/header.html',
		restrict: 'E', 
        transclude: true,    // allows us �swap� our content for the calling html
        replace: true,        // tells the calling html to replace itself with what�s returned here
        link: function(scope, element, attrs) { // to get scope, the element, and its attributes
          scope.user = $rootScope.user; 
        }
    };
});

mapProjectAppControllers.directive('otfFooterDirective', function() {
    
	return {
        templateUrl: './partials/footer.html',
		restrict: 'E', 
        transclude: true,    // allows us �swap� our content for the calling html
        replace: true,        // tells the calling html to replace itself with what�s returned here
        link: function(scope, element, attrs) { // to get scope, the element, and its attributes
          scope.user = $rootScope.user; 
        }
    };
});


mapProjectAppControllers.directive('sortBy', function () {
	
	return {
		templateUrl: './partials/sort-by.html',
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





