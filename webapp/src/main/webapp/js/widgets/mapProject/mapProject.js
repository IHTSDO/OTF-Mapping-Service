
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
  }).controller('MapProjectWidgetCtrl', function($scope, $http, $rootScope, $location, $modal, localStorageService){
	  
	  // get the local storage variables
	  $scope.project = localStorageService.get('focusProject');
	  $scope.currentUser = localStorageService.get('currentUser');
	  $scope.currentRole = localStorageService.get('currentRole');
	  $scope.userToken = localStorageService.get('userToken');
	  
	  // watch for project change
  	  $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
  		  console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
          $scope.project = parameters.focusProject;
          
          console.debug($scope.project);
	  });	
  	  

  	 // the only local storage variable required for this app is userToken
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
			}).error(function(data, status, headers, config) {
			    $rootScope.glassPane--;
			    $rootScope.handleHttpError(data, status, headers, config);
		    });
				
			} else {
			  	$rootScope.glassPane--;		
			}
		};

		$scope.generateTestData = function() {

			if ($scope.nConflicts == undefined || $scope.nConflicts == null) {
				alert("You must specify the number of conflicts to be generated.");
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
					}).error(function(data, status, headers, config) {
					    $rootScope.glassPane--;
					    $rootScope.handleHttpError(data, status, headers, config);
					});

				} else {
					$rootScope.glassPane--;		
					
				}
			}
		};
		
		$scope.generateTestingStateForKLININ = function() {
			
			console.debug("Generating mapping testing state");
			$rootScope.glassPane++;
			
			var confirmGenerate = confirm("Are you sure you want to generate the clean mapping user testing state?");
			if (confirmGenerate == true) {
				// call the generate API
				$http({
					url: root_workflow + "project/id/" + $scope.project.id + "/generateTestingStateKLININ",
					dataType: "json",
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
					$rootScope.glassPane--;
				}).error(function(data, status, headers, config) {
				    $rootScope.glassPane--;
				    $rootScope.handleHttpError(data, status, headers, config);
				});
			}
		};
		
		$scope.generateTestingStateForBHEKRE = function() {
			
			console.debug("Generating mapping testing state");
			$rootScope.glassPane++;
			
			var confirmGenerate = confirm("Are you sure you want to generate the clean mapping user testing state?");
			if (confirmGenerate == true) {
				// call the generate API
				$http({
					url: root_workflow + "project/id/" + $scope.project.id + "/generateTestingStateBHEKRE",
					dataType: "json",
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
					$rootScope.glassPane--;
				}).error(function(data, status, headers, config) {
				    $rootScope.glassPane--;
				    $rootScope.handleHttpError(data, status, headers, config);
				});
			}
		};			
		
		$scope.showDelta = function() {
			
			var modalInstance = $modal.open({
				templateUrl: 'partials/delta-concepts.html',
				controller: ShowDeltaModalCtrl,
				resolve: {
					terminology: function() { return $scope.project.sourceTerminology;},
					version: function() { return $scope.project.sourceTerminologyVersion;}
				}
			});
			
			modalInstance.result.then(function() {
				// do nothing, placeholder
			});
			
		};
		
		var ShowDeltaModalCtrl = function($scope, $http, $modalInstance, terminology, version) {
			
			$scope.pageSize = 10;
			$scope.terminology = terminology; // used for title
			
			
			$scope.close = function() {
				$modalInstance.close();
			};
			
			$scope.getConcepts = function(page, filter) {
				$rootScope.glassPane++;
				var pfsParameterObj = 
					
				{"startIndex": page == -1 ? -1 : (page-1)*$scope.pageSize,
		 	 	 "maxResults": page == -1 ? -1 : $scope.pageSize, 
		 	 	 "sortField": null,
		 	 	 "queryRestriction": filter }; 
				
				console.debug(pfsParameterObj);
				
				$http({
					url: root_content + "terminology/id/" + terminology + "/" + version + "/delta",
					dataType: "json",
					method: "POST",
					data: pfsParameterObj,
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
					$rootScope.glassPane--;
					
					$scope.concepts = data.searchResult;
					$scope.nConcepts = data.totalCount;
					$scope.numConceptPages = Math.ceil(data.totalCount/$scope.pageSize);
					
				}).error(function(data, status, headers, config) {
				    $rootScope.glassPane--;
				    $scope.concepts = [];
				    //$rootScope.handleHttpError(data, status, headers, config);
				});
				
				
			};
			$scope.getConcepts(1, null);
		};
		

	    $scope.openConceptBrowser = function() {
	    	if ($scope.currentUser.userName === 'guest') 
	    		var myWindow = window.open("http://browser.ihtsdotools.org/index.html?perspective=full&diagrammingMarkupEnabled=true&acceptLicense=true", "browserWindow");
	    	else
	    		var myWindow = window.open("http://dailybuild.ihtsdotools.org/index.html?perspective=full&diagrammingMarkupEnabled=true&acceptLicense=true", "browserWindow");
	    	myWindow.focus();
	    };
  });
