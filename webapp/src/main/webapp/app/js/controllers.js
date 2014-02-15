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
	       {name:'Viewer', value:'Viewer'},
	       {name:'Specialist', value:'Specialist'},
	       {name:'Lead', value:'Lead'},
	       {name:'Administrator', value:'Administrator'}];
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
		    			  $scope.project = projects[i];
		    			  terminology = projects[i].sourceTerminology;
		    			  version = projects[i].sourceTerminologyVersion;
		    			  break;
		    		  }
		    	  }	  
		    	  
		    	// check relation syle flags
				  if ($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE") {
					  console.debug("map category style detected");
					  applyMapCategoryStyle();
				  }
				  
				  if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
					  console.debug("Relationship Style detected");
					  applyRelationshipStyle();
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
		
		function applyMapCategoryStyle() {
			 
			 // Cycle over all entries. If targetId is blank, show relationName as the target name
			 for (var i = 0; i < $scope.records.length; i++) {		 
				 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 
					 
					 if ($scope.records[i].mapEntry[j].targetId === "") {
						 $scope.records[i].mapEntry[j].targetName = "\"" + $scope.records[i].mapEntry[j].relationName + "\"";
	
					 }
				 }
			 }
		 };
		 
		function applyRelationshipStyle() {
			// Cycle over all entries. Add the relation name to the advice list
			 for (var i = 0; i < $scope.records.length; i++) {		 
				 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
					 if ($scope.records[i].mapEntry[j].targetId === "") {	 
						 // get the object for easy handling
						 var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;
						 
						 // add the serialized advice	
						 jsonObj.push({"id":"0", "name": "\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "detail":"\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "objectId":"0"});
						 
						 $scope.records[i].mapEntry[j].mapAdvice = jsonObj;
					 }
				 }
			 }
		 };
	
		
	}]);
                                                              
/**
 * Controller for Map Project Records view
 * 
 * Basic function:
 * 1) Retrieve map project
 * 2) Retrieve concept associated with map project refsetid
 * 3) Retrieve records
 * 
 * Scope functions (accessible from html)
 * - $scope.resetSearch 		clears query and launches unfiltered record retrieval request
 * - $scope.retrieveRecords 	launches new record retrieval request based on current paging/filtering/sorting parameters
 * - $scope.to_trusted	 		converts unsafe html into usable/displayable code
 *  
 * Helper functions: 
 * - setPagination				sets the relevant pagination attributes based on current page
 * - getNRecords				retrieves the total number of records available given filtering parameters
 * - getUnmappedDescendants		retrieves the unmapped descendants of a concept
 * - applyMapCategoryStyle		modifies map entries for display based on MAP_CATEGORY_STYLE
 * - applyRelationshipStyle		modifies map entries for display based on RELATIONSHIP_STYLE
 * - constructPfsParameterObj	creates a PfsParameter object from current paging/filtering/sorting parameters, consumed by RESTful service
 */

mapProjectAppControllers.controller('MapProjectRecordCtrl', ['$scope', '$http', '$routeParams', '$sce',
   function ($scope, $http, $routeParams, $sce) {
	
		// header information (not used currently)
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
		
		// header sorting criteria initialization (not used currently)
		$scope.filterCriteria = {
				pageNumber: 1,
				sortDir: 'asc',
				sortedBy: 'conceptId'
		};
	
	  // the project id, extracted from route params
	  $scope.projectId = $routeParams.projectId;
	  
	  // status variables
	  $scope.unmappedDescendantsPresent = false;
	  $scope.mapNotesPresent = false;
	  $scope.mapAdvicesPresent = false;
	  
	  // error variables
	  $scope.errorProject = "";
	  $scope.errorConcept = "";
	  $scope.errorRecords = "";
	  
	  // pagination variables
	  $scope.recordsPerPage = 10;
	  
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
    		  $scope.errorConcept= "Error retrieving concept";
    	  });
      }).then(function(data) {
    	  
    	  // load first page
    	  $scope.retrieveRecords(1);
      });
	 
	 // Constructs trusted html code from raw/untrusted html code
	 $scope.to_trusted = function(html_code) {
	    return $sce.trustAsHtml(html_code);
	 };
	 
	 // function to clear input box and return to initial view
	 $scope.resetSearch = function() {
		 $scope.query = null;
		 $scope.retrieveRecords(1);
	 };
    	
	 // function to retrieve records for a specified page
	 $scope.retrieveRecords = function(page) {
		 
		 // retrieve pagination information for the upcoming query
		 setPagination($scope.recordsPerPage);
		
		 console.debug("Switching to page " + page);
		/*
		   var query_url;
		   var startRecord = (page - 1) * $scope.recordsPerPage;
		   if ($scope.query == null) {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/" + startRecord + "-" + $scope.recordsPerPage;
		 } else {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/" + startRecord + "-" + $scope.recordsPerPage + "/" + $scope.query;
		 }*/
		 // construct html parameters parameter
		 var pfsParameterObj = constructPfsParameterObj(page);
		 var query_url = root_mapping + "record/projectId/" + $scope.project.objectId;
		 
		 console.debug("Retrieve records");
		 console.debug(query_url);
		 console.debug(pfsParameterObj);
		 
		// retrieve map records
		  $http({
			  url: query_url,
			  dataType: "json",
			  data: pfsParameterObj,
			  method: "POST",
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
			  
			  console.debug("Checking map category styles");
			  
			  // check relation syle flags
			  if ($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE") {
				  console.debug("map category style detected");
				  applyMapCategoryStyle();
			  }
			  
			  if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
				  console.debug("Relationship Style detected");
				  applyRelationshipStyle();
			  }
					 			  
			  
			  // get unmapped descendants (checking done in routine)
			  if ($scope.records.length > 0) {	
				  getUnmappedDescendants(0);
			  }
		  });
	 }; 
	 
	 // Constructs a paging/filtering/sorting parameters object for RESTful consumption
	 function constructPfsParameterObj(page) {
		
			 return {"startIndex": (page-1)*$scope.recordsPerPage,
			 	 	 "maxResults": $scope.recordsPerPage, 
			 	 	 "sortField": null, // TODO: Replace this when sorting functional
			 	 	 "filterString": $scope.query == null ? null : $scope.query};  // assigning simply to $scope.query when null produces undefined
		 
	 }
	 
	 // function to set the relevant pagination fields
	 function setPagination(recordsPerPage) {
		 
		 // set scope variable for total records
		 getNRecords();
				 
		 // set pagination variables
		 $scope.recordsPerPage = recordsPerPage;
		 $scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
	 };
	 
	 // function query for the number of records associated with full set or query
	 function getNRecords() {
		 
		 var query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/nRecords";
		 var pfsParameterObj = constructPfsParameterObj(1);
		 
		 console.debug("getNRecords");
		 console.debug(pfsParameterObj);
		 
		 // retrieve the total number of records associated with this map project
		 $http({
			  url: query_url,
			  dataType: "json",
			  data: pfsParameterObj,
			  method: "POST",
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
	 
	 /* For sorting, not currently used
	  *  $scope.onSort = function (sortedBy, sortDir) {
			 $scope.filterCriteria.sortDir = sortDir;
			 $scope.filterCriteria.sortedBy = sortedBy;
			 $scope.filterCriteria.pageNumber = 1;
		 };
	*/

		 
	// TODO This is inefficient, consider implementing a batch request
    function getUnmappedDescendants(index) {
				  
    	  // before processing this record, make call to start next async request
    	  if (index < $scope.records.length-1) {
		  	   getUnmappedDescendants(index+1);
		  }
    	  
		  $scope.records[index].unmappedDescendants = [];
		  
		  // if descendants below threshold for lower-level concept, check for unmapped
		  if ($scope.records[index].countDescendantConcepts < 11) {

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
				 if (data.count > 0) $scope.unmappedDescendantsPresent = true;
				 $scope.records[index].unmappedDescendants = data.searchResult;
			  });
		  } 
	  
	 };
	 
	 function applyMapCategoryStyle() {
		 
		 // Cycle over all entries. If targetId is blank, show relationName as the target name
		 for (var i = 0; i < $scope.records.length; i++) {		 
			 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 
				 
				 if ($scope.records[i].mapEntry[j].targetId === "") {
					 $scope.records[i].mapEntry[j].targetName = "[RELATION NAME NULL]";
						
							     // TODO: Reinsert this once map entries correctly loaded:  $scope.records[i].mapEntry[j].relationName;
				 }
			 }
		 }
	 };
	 
	function applyRelationshipStyle() {
		// Cycle over all entries. Add the relation name to the advice list
		 for (var i = 0; i < $scope.records.length; i++) {		 
			 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
				 if ($scope.records[i].mapEntry[j].targetId === "") {	 
					 // get the object for easy handling
					 var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;
					 
					 // add the serialized advice	
					 jsonObj.push({"id":"0", "name": "***PLACEHOLDER ADVICE***", "detail":"***PLACEHOLDER ADVICE***", "objectId":"0"});
					 
					 // TODO Replace placeholder with $scope.records[i].mapEntry[j].mapRelationName
					 $scope.records[i].mapEntry[j].mapAdvice = jsonObj;
				 }
			 }
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

		    	  // set pagination variables
		    	  $scope.pageSize = 5;
		    	  $scope.getPagedAdvices(1);
		    	  $scope.getPagedPrinciples(1);
		    	  $scope.orderProp = 'id';
		      
		      });
			
			
			// function to return trusted html code (for tooltip content)
			$scope.to_trusted = function(html_code) {
			    return $sce.trustAsHtml(html_code);
			 };
			 
			 ///////////////////////////////////////////////////////////////
			 // Functions to display and filter advices and principles
			 // NOTE: This is a workaround due to pagination issues
			 ///////////////////////////////////////////////////////////////
			 
			 // get paged functions
			 // - sorts (by id) filtered elements
			 // - counts number of filtered elmeents
			 // - returns artificial page via slice
			 
			 $scope.getPagedAdvices = function (page) {
				 console.debug("Called paged advice for page " + page);				 
				 $scope.pagedAdvice = $scope.sortByKey($scope.project.mapAdvice, 'id')
				 								.filter(containsAdviceFilter);
				 $scope.pagedAdviceCount = $scope.pagedAdvice.length;
				 $scope.pagedAdvice = $scope.pagedAdvice
				 								.slice((page-1)*$scope.pageSize,
				 										page*$scope.pageSize);
			 };
			 
			 $scope.getPagedPrinciples = function (page) {
				 console.debug("Called paged principle for page " + page);
				 $scope.pagedPrinciple = $scope.sortByKey($scope.project.mapPrinciple, 'id')
												.filter(containsPrincipleFilter);
				 $scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
				 $scope.pagedPrinciple = $scope.pagedPrinciple
												.slice((page-1)*$scope.pageSize,
														page*$scope.pageSize);
				 
				 console.debug($scope.pagedPrinciple);
			 };
			 
			 // functions to reset the filter and retrieve unfiltered results
			 
			 $scope.resetAdviceFilter = function() {
				 $scope.adviceFilter = "";
				 $scope.getPagedAdvices(1);
			 };
			 
			 $scope.resetPrincipleFilter = function() {
				 $scope.principleFilter = "";
				 $scope.getPagedPrinciples(1);
			 };
			 
			 
			 // element-specific functions for filtering
			 // don't want to search id or objectId
			 
			 function containsAdviceFilter(element) {
				 
				 // check if advice filter is empty
				 if ($scope.adviceFilter === "" || $scope.adviceFilter == null) return true;
				 
				 // otherwise check if upper-case advice filter matches upper-case element name or detail
				 if ( element.detail.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.name.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;
				 
				 // otherwise return false
				 return false;
			 }
			 
			 function containsPrincipleFilter(element) {
				 
				 console.debug("Called containsPrincipleFilter");
				 
				// check if principle filter is empty
				 if ($scope.principleFilter === "" || $scope.principleFilter == null) return true;
				 
				 console.debug("not null")
				 
				 // otherwise check if upper-case principle filter matches upper-case element name or detail
				 if ( element.principleId.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.detail.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.name.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.sectionRef.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 
				 console.debug("did not match for " + $scope.principleFilter);
				 
				 // otherwise return false
				 return false;
			 }
			 
			 // helper function to sort a JSON array by field
			 
			 $scope.sortByKey = function sortById(array, key) {
				    return array.sort(function(a, b) {
				        var x = a[key]; var y = b[key];
				        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
				    });
				};
				
				
			////////////////////////////////////////////////////////
			// END PRINCIPLE/ADVICE SORTING/FILTERING FUNCTIONS
			////////////////////////////////////////////////////////
}]);


//////////////////////////////////////////////////////
// Directives:  TODO Separate into different file
/////////////////////////////////////////////////////


mapProjectAppControllers.directive('headerDirective', function() {
    
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





