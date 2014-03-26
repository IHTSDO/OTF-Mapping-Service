
'use strict';

angular.module('mapProjectApp.widgets.mapProject', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('mapProject', {
        title: 'Map Project Details',
        description: 'Map Project Details',
        controller: 'MapProjectWidgetCtrl',
        templateUrl: 'js/widgets/mapProject/mapProject.html',
        edit: {}
      });
  }).controller('MapProjectWidgetCtrl', function($scope, $http, localStorageService){
	 
	  // get the project
	  $scope.project = localStorageService.get('focusProject');
	  
	  // watch for project change
  	  $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
  		  console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
          $scope.project = parameters.focusProject;
          
          console.debug($scope.project);
	  });	
  });
