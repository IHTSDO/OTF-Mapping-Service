
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
  	  
  	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch('userToken', function() {
		
		$http.defaults.headers.common.Authorization = $scope.userToken;
		
	});
	
		$scope.goProjectDetails = function () {
			console.debug($scope.role);

			var path = "/project/details";
				// redirect page
				$location.path(path);
		};
		
		$scope.goMapRecords = function () {
			console.debug($scope.role);

			var path = "/project/records";
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
				url: root_workflow + "project/id/" + $scope.project.id + "/compute",
				dataType: "json",
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
			  	$rootScope.$broadcast('mapProjectWidget.notification.workflowComputed');
			  	$rootScope.glassPane--;
			}).error(function(response) {
				$scope.error = "Error";
				$rootScope.glassPane--;

				if (response.indexOf("HTTP Status 401") != -1) {
					$rootScope.globalError = "Authorization failed.  Please log in again.";
					$location.path("/");
				}
		    });
				
			} else {
			  	$rootScope.glassPane--;		
			}
		};

		$scope.generateTestData = function() {

			if ($scope.nConflicts == undefined || $scope.nConflicts == null) {
				alert("You must specify the number of conflicts to be generated.")
			} else {

				console.debug("Generating test data");
				$rootScope.glassPane++;

				var confirmGenerate =  confirm("Are you sure you want to generate test data?");
				if (confirmGenerate == true) {
					
					// call the generate API
					$http({
						url: root_workflow + "project/id/" + $scope.project.id + "/generateConflicts/maxConflicts/" + $scope.nConflicts,
						dataType: "json",
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {
						$rootScope.glassPane--;
					}).error(function(response) {
						$scope.error = "Error generating test data.";
						$rootScope.glassPane--;
						
						if (response.indexOf("HTTP Status 401") != -1) {
							$rootScope.globalError = "Authorization failed.  Please log in again.";
							$location.path("/");
						}
					});

				} else {
					$rootScope.glassPane--;		
					
				}
			}
		};
		
		$scope.generateTestingState = function() {
			
			console.debug("Generating mapping testing state");
			$rootScope.glassPane++;
			
			var confirmGenerate = confirm("Are you sure you want to generate the clean mapping user testing state?");
			if (confirmGenerate == true) {
				// call the generate API
				$http({
					url: root_workflow + "project/id/" + $scope.project.id + "/generateTestingState",
					dataType: "json",
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
					$rootScope.glassPane--;
				}).error(function(response) {
					$scope.error = "Error generating test data.";
					$rootScope.glassPane--;

					if (response.indexOf("HTTP Status 401") != -1) {
						$rootScope.globalError = "Authorization failed.  Please log in again.";
						$location.path("/");
					}
				});
			}
		};
		
  });
