'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap']);

var root_mapping = "${base.url}/mapping-rest/mapping/";
var root_content = "${base.url}/mapping-rest/content/";
	
//////////////////////////////
// Navigation
//////////////////////////////	

mapProjectAppControllers.controller('MapProjectAppNav',
	function ($scope) {
	
		var changePage = function (newPage) {
			$location.path = newPage;
		}
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

mapProjectAppControllers.controller('MapRecordListCtrl', 
  function ($scope, $http) {
      $http({
        url: root_mapping + "record/records",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.records = data.mapRecord;
      }).error(function(error) {
    	$scope.error = "Error";
    });
 
    $scope.orderProp = 'id';	
  });

mapProjectAppControllers.controller('MapLeadListCtrl', 
  function ($scope, $http) {
      $http({
        url: root_mapping + "lead/leads",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.leads = data.mapLead;
      }).error(function(error) {
    	  $scope.error = "Error";
      });
      
      $scope.getProjects = function(id) {
          $http({
             url: root_mapping + "lead/id/" + id + "/projects",
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
      };
      
    $scope.toggleEdit = function() {
    	if($scope.editMode == false) {
    		$scope.editMode = true;
    		$scope.editModeValue = 'Stop Editing';
    	} else {
    		$scope.editMode = false;
    		$scope.editModeValue = 'Edit';
    	}
    };
 
    $scope.editMode = false;
    $scope.editModeValue = 'Edit';
    $scope.orderProp = 'id';	
  });

mapProjectAppControllers.controller('MapSpecialistListCtrl', 
  function ($scope, $http) {
      $http({
        url: root_mapping + "specialist/specialists",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.specialists = data.mapSpecialist;
      }).error(function(error) {
    	  $scope.error = "Error";
    });
 
    $scope.orderProp = 'id';	
  });

mapProjectAppControllers.controller('MapRecordDetailCtrl', ['$scope', '$http', '$routeParams',
    function ($scope, $http, $routeParams) {
 	  $scope.recordId = $routeParams.recordId;
 	  $http({
         url: root_mapping + "record/id/" + $scope.recordId,
         dataType: "json",
         method: "GET",
         headers: {
           "Content-Type": "application/json"
         }
       }).success(function(data) {
         $scope.record = data;
       }).error(function(error) {
     });

 }]);




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
          url: "${base.url}/mapping-rest/content/concept/id/" + $scope.conceptId,
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
      });

  }]);

//////////////////////////////
// Query Services
//////////////////////////////	

mapProjectAppControllers.controller('QueryCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	$scope.searchConceptsStatus = "";
	$scope.searchProjectsStatus = "";
	$scope.searchRecordsStatus = "";
	
	$scope.searchConcepts = function(id) {
		
	  $scope.searchConceptsStatus = "[Searching...]";
		
	  $http({
        url: root_content + "concept/query/" + $scope.queryConcept,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.conceptResults = data;
        $scope.searchConceptsStatus= $scope.conceptResults.count + " results found:";
       
      }).error(function(error) {
    	$scope.searchConceptsStatus = "Could not retrieve concepts.";
      });
	};
	
	$scope.resetConcepts = function(id) {
		$scope.conceptResults = "";
		$scope.searchConceptsStatus = "";
	};
	
	$scope.searchProjects = function(id) {
		
	  $scope.searchProjectsStatus = "[Searching...]";
	  
	  $http({
        url: root_mapping + "project/query/" + $scope.queryProject,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.projectResults = data;
        $scope.searchProjectsStatus= $scope.projectResults.count + " results found:";
      }).error(function(error) {
    	$scope.searchProjectsStatus = "Could not retrieve projects." 
      });
	};
	
	$scope.resetProjects = function(id) {
		$scope.projectResults = "";
		$scope.searchProjectsStatus = "";
	};
	
	$scope.searchRecords = function(id) {
		
	  $scope.searchRecordsStatus = "[Searching...]";
	  
	  $http({
        url: root_mapping + "record/query/" + $scope.queryRecord,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.recordResults = data;
        $scope.searchRecordsStatus= $scope.recordResults.count + " results found, listing by Concept ID:";
      }).error(function(error) {
    	$scope.searchRecordsStatus = "Could not retrieve records."; 
      });
	};
	
	$scope.resetRecords = function(id) {
		$scope.recordResults = "";
		$scope.searchRecordsStatus = "";
	};
}]);


mapProjectAppControllers.controller('QueryConceptCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	$scope.query = $routeParams.query;
	$scope.searchConceptsStatus = "Searching concepts for query: " + $routeParams.query;
	
	$http({
      url: root_content + "concept/query/" + $routeParams.query,
      dataType: "json",
      method: "GET",
      headers: {
        "Content-Type": "application/json"
      }	
    }).success(function(data) {
      $scope.conceptResults = data;
      $scope.searchConceptsStatus= $scope.conceptResults.count + " results found:";
   
    }).error(function(error) {
    	$scope.searchConceptsStatus = "Could not retrieve concepts.";
    });
    	$scope.resetConcepts = function(id) {
		$scope.conceptResults = "";
		$scope.searchConceptsStatus = "[No concept query executed]";
	};
	
}]);
  
//////////////////////////////
// Specialized Services
//////////////////////////////	



// TODO Add test for coming from project list page (i.e. pass the project to this controller)
mapProjectAppControllers.controller('MapProjectDetailCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	  $scope.projectId = $routeParams.projectId;
	  
	  $scope.errorProject = "";
	  $scope.errorConcept = "";
	  $scope.errorRecords = "";
	  $scope.statusRecordLoad = "";
	  
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
        $scope.statusRecordLoad = "[Loading...]";
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
  		 
    	  // retrieve any map records associated with this project
    	  $http({
    		  url: root_mapping + "record/projectId/" + $scope.project.objectId,
    		  dataType: "json",
    		  method: "GET",
    		  headers: {
    			  "Content-Type": "application/json"
    		  }
    	  }).success(function(data) {
    		  $scope.records = data.mapRecord;
    		  $scope.statusRecordLoad = "";
    	  }).error(function(error) {
    		  $scope.errorRecord = "Error retrieving map records";
    	  });
      });
    	  
	  
}]);




