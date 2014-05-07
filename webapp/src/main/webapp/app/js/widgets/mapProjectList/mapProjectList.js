
'use strict';

angular.module('mapProjectApp.widgets.mapProjectList', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('mapProjectList', {
        title: 'Map Projects',
        description: 'Displays a list of map projects',
        controller: 'mpCtrl',
        templateUrl: 'js/widgets/mapProjectList/mapProjectList.html',
        edit: {}
      });
  }).controller('mpCtrl', function($scope, $rootScope, $http){
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

      // broadcast page to help mechanism
  	  //$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'mainDashboard'});  
  
  });
