
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
  }).controller('MapProjectWidgetCtrl', function($scope, $http, $rootScope, $location, localStorageService){
	  
	  // get the project
	  $scope.project = localStorageService.get('focusProject');
	  $scope.currentRole = localStorageService.get('currentRole');
	  
	  // watch for project change
  	  $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
  		  console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
          $scope.project = parameters.focusProject;
          
          console.debug($scope.project);
	  });	
  	  
  	  // broadcast page to help mechanism
  	  $rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'mainDashboard'});  
  
  	  
		$scope.goProjectDetails = function () {
			console.debug($scope.role);

			var path = "/project/id/" + $scope.project.id;
				// redirect page
				$location.path(path);
		};
		
		$scope.goMapRecords = function () {
			console.debug($scope.role);

			var path = "/record/projectId/" + $scope.project.id;
				// redirect page
				$location.path(path);
		};
  	  
  	  
      
      $scope.computeWorkflow = function() {
			console.debug("Computing workflow");
		  	$rootScope.glassPane++;

			var confirmWorkflow =  confirm("Are you sure you want to compute workflow?");
			if (confirmWorkflow == true) {
			// retrieve project information
			$http({
				url: root_workflow + "project/id/" + $scope.project.id,
				dataType: "json",
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
			  	$rootScope.$broadcast('mapProjectWidget.notification.workflowComputed');
			  	$rootScope.glassPane--;
			}).error(function(error) {
		    	  $scope.error = "Error";
				  	$rootScope.glassPane--;
		    });
				
			} else {
			  	$rootScope.glassPane--;		
			}
		};
		
  });
