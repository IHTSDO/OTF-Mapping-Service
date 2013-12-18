'use strict';

/*  */

var mapProjectApp = angular.module('mapProjectApp', []);

var root = "http://localhost:8080/mapping-rest/mapping/";
 
mapProjectApp.controller('MapProjectListCtrl', 
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
    });
 
    $scope.orderProp = 'id';	
  });

mapProjectApp.controller('MapProjectDetailCtrl', 
  function ($scope, $http) {

     $scope.getProject = function() {
    	 
      $scope.showProject = true;
      $scope.showProjectsQuery = false;
  
      $http({
        url: root + "project/id/" + $scope.projectId,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.project = data;
        $scope.error = "";
      }).error(function(error) {
    	$scope.error = "getProject() failed for id " + $scope.projectId;
      });   
    };
    
    $scope.getProjectsByQuery = function() {
    	  
        $http({
          url: root + "project/query/" + $scope.projectQuery,
          dataType: "xml",
          method: "GET",
          headers: {
            "Content-Type": "application/xml"
          }
        }).success(function(data) {
          $scope.project = data;
          $scope.showProject = false;
          $scope.showProjectsQuery = true;

        }).error(function(error) {
        });   
      };
   
    $scope.showProject = false;
    $scope.showProjectsQuery = false;
    $scope.orderProp = 'id';	
});


//projectApp.controller('MapProjectCtrl', ['$scope', '$routeParams', '$http',
//  function($scope, $routeParams, $http) {
//    $http({
//      url: root.concat("project/id/" + $routeParams.id),
//      dataType: "json",
//      method: "GET",
//      headers: {
//        "Content-Type": "application/json"
//      }
//    }).success(function(data) {
//        $scope.project = data;
//    });
//  }]);



