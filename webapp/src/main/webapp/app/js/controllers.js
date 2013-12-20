'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap']);

var root = "http://localhost:8080/mapping-rest/mapping/";
 
mapProjectAppControllers.controller('MapProjectListCtrl', 
  function ($scope, $http) {
      $http({
        url: root + "project/projects",
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
        url: root + "record/records",
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
        url: root + "lead/leads",
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
             url: root + "lead/id/" + id + "/projects",
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
        url: root + "specialist/specialists",
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


// not functioning, projectId is undefined for some reason
mapProjectAppControllers.controller('MapProjectDetailCtrl', ['$scope', '$routeParams',
   function ($scope, $http, $routeParams) {
	  $scope.projectId = $routeParams.projectId;
	  $http({
        url: root + "project/id/" + $scope.projectId,
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
