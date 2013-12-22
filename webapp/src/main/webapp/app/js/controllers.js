'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap']);

var root_mapping = "${base.url}/mapping-rest/mapping/";
var root_content = "${base.url}/mapping-rest/content/";
	


	
//////////////////////////////
// CONTENT SERVICES
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
        $scope.projects = data.mapProjects;
      }).error(function(error) {
    	  $scope.error = "Error";
    });
 
    $scope.orderProp = 'id';	
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
        $scope.records = data.mapRecords;
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
        $scope.leads = data.mapLeads;
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
             $scope.projects = data.mapProjects;
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
        $scope.specialists = data.mapSpecialists;
      }).error(function(error) {
    	  $scope.error = "Error";
    });
 
    $scope.orderProp = 'id';	
  });

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
			        $scope.concepts = data.concepts;
			      }).error(function(error) {
			    	  $scope.error = "Error";
			    });
			 
			    $scope.orderProp = 'id';	
			  });


mapProjectAppControllers.controller('MapProjectDetailCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	  $scope.projectId = $routeParams.projectId;
	  $http({
        url: root_mapping + "project/id/" + $scope.projectId,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.project = data;
      }).error(function(error) {
    });

}]);

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
        	$scope.status = "Load error!"
            $scope.statusnote = "";
        	console.print("Error in conceptdetailctrol");
      });

  }]);

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


