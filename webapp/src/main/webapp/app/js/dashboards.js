'use strict';

var mapProjectAppDashboards = angular.module('mapProjectAppDashboards', []);

mapProjectAppDashboards.controller('ResolveConflictsDashboardCtrl', function ($scope, $routeParams, $rootScope, localStorageService) {

	setModel();

	$scope.focusProject = localStorageService.get('focusProject');

	function setModel() {
		$scope.name = 'ResolveConflictsDashboard';
		if (!$scope.model) {
			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "compareRecords",
								title: "Compare Records"
							}]
						}]
					}, { // new row

						columns: [{
							class: 'col-md-6',
							widgets: [{
								type: "mapRecord",
								config: { recordId: $routeParams.recordId},
								title: "Map Record"
							}]
						}, {
							class: 'col-md-6',
							widgets: [{
								type: "mapEntry",
								config: { entry: $scope.entry},
								title: "Map Entry"
							}, {
								type: "terminologyBrowser",
								config: {
									terminology: $scope.focusProject.destinationTerminology,
									terminologyVersion: $scope.focusProject.destinationTerminologyVersion
								},
								title: $scope.focusProject.destinationTerminology + " Terminology Browser"

							}],
						} // end second column
						] // end columns

					}] // end second row


			};
		}
	};

	console.debug("CONTROLLER MODEL");
	console.debug($scope.model);

	// broadcast page to help mechanism  
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'resolveConflictsDashboard'});  

	$scope.$on('adfDashboardChanged', function (event, name, model) {
		console.debug("Dashboard change detected by ResolveConflictsDashboard");
		localStorageService.set(name, model);
	});

	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
		console.debug("MapProjectWidgetCtrl: Detected change in focus project");
		$scope.project = parameters.focusProject;

		console.debug($scope.project);
	});	

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('ResolveConflictsDashboardCtrl: Detected project set/change');
		setModel();


	});
});



mapProjectAppDashboards.controller('dashboardCtrl', function ($rootScope, $scope, $http, localStorageService) {

	$scope.currentRole = localStorageService.get('currentRole');

	console.debug('in dashboardCtrl');

	// watch for preferences change
	$scope.$on('localStorageModule.notification.setUserPreferences', function(event, parameters) { 	
		console.debug("dashboardCtrl:  Detected change in preferences");
		if (parameters.userPreferences != null && parameters.userPreferences != undefined) {
			$http({
				url: root_mapping + "userPreferences/update",
				dataType: "json",
				data: parameters.userPreferences,
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
			});
		}
	});

	// on successful user retrieval, construct the dashboard
	$scope.$watch('currentRole', function() {

		console.debug("Setting the dashboard based on role: " + $scope.currentRole);

		/**
		 * Viewer has the following widgets:
		 * - MapProject
		 */
		if ($scope.currentRole === 'Viewer') {
			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					}]
			};
			/**
			 * Specialist has the following widgets:
			 * - MapProject
			 * - WorkAvailable
			 * - AssignedList
			 * - EditedList
			 */
		} else if ($scope.currentRole === 'Specialist') {

			$scope.model = {

					structure: "12/6-6/12",
					rows: [{	
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					}, {
						columns: [{
							class: 'col-md-6',
							widgets: [{
								type: "workAvailable",
								config: {},
								title: "Available Work"
							}]
						}, {
							class: 'col-md-6',
							widgets: [{
								type: "assignedList",
								config: {},
								title: "Assigned to Me"
							}]
						}]
					}

					, {
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "editedList",
								title: "Recently Edited"
							}]
						}]
					}]
			};

			/**
			 * Lead has the following widgets
			 * -MapProject
			 * - WorkAvailable
			 * - AssignedList
			 * - EditedList
			 * - MetadataList
			 */
		} else if ($scope.currentRole === 'Lead') {

			console.debug("Setting model for lead");

			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					}, {
						columns: [{
							class: 'col-md-6',
							widgets: [{
								type: "workAvailable",
								config: {},
								title: "Available Work"
							}]
						}, {
							class: 'col-md-6',
							widgets: [{
								type: "assignedList",
								config: {},
								title: "Assigned to Me"
							}]
						}]
					}

					, {
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "editedList",
								title: "Recently Edited"
							}]
						}]
					}, {
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "metadataList",
								config: {
									terminology: "SNOMEDCT"
								},
								title: "Metadata"
							}]
						}]
					}]
			};

			console.debug($scope.model);

			/** Admin has the following widgets
			 * - MapProject
			 * - MetadataList
			 * - AdminTools
			 */
		} else if ($scope.currentRole === 'Administrator') {

			$scope.model = {

					structure: "12/6-6/12",
					rows: [{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "mapProject",
								config: {},
								title: "Map Project"
							}]
						}]
					},{
						columns: [{
							class: 'col-md-12',
							widgets: [{
								type: "metadataList",
								config: {
									terminology: "SNOMEDCT"
								},
								title: "Metadata"
							}]
						}]
					}]
			};

		} else {
			alert("Invalid role detected by dashboard");
		}

		$scope.$on('adfDashboardChanged', function (event, name, model) {
			console.debug('adfDashboardChanged in DashBoardCtrl');
			console.debug(event);
			console.debug(name);
			console.debug(model);
			$scope.model = model;
		});
	});

});

mapProjectAppDashboards.controller('MapRecordDashboardCtrl', function ($scope, $rootScope, $routeParams, $location, localStorageService) {

	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');

	setModel();

	function setModel() {
		$scope.name = 'EditingDashboard';
		console.debug("Setting record dashboard model");
		console.debug($scope.model);
		if (!$scope.model) {
			$scope.model = {
					structure: "6-6",                          
					rows: 
						[{
							columns: [{
								class: 'col-md-6',
								widgets: [{
									type: "mapRecord",
									config: { recordId: $routeParams.recordId},
									title: "Map Record"
								}]
							}, {
								class: 'col-md-6',
								widgets: [{
									type: "mapEntry",
									config: { entry: $scope.entry},
									title: "Map Entry"
								}, {
									type: "terminologyBrowser",
									config: { 
										terminology: $scope.focusProject.destinationTerminology,
										terminologyVersion: $scope.focusProject.destinationTerminologyVersion
									},
									title: $scope.focusProject.destinationTerminology + " Terminology Browser"

								}],
							} // end second column
							] // end columns
						}] // end rows
			};

		}
	};
	
	// broadcast page to help mechanism  
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'editDashboard'});  

	$scope.$on('adfDashboardChanged', function (event, name, model) {
		console.debug("Dashboard change detected by MapRecordDashboard");
		localStorageService.set(name, model);
	});

	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("RecordDashboardCtrl:  Detected change in focus project");
		
		// set the model to empty
		$scope.model = {
				structure: "6-6",                          
				rows: 
					[{}]
		};
		
		setModel();
		
		console.debug($scope.currentRole);
		
		var path = "";

		if ($scope.currentRole === "Specialist") {
			path = "/specialist/dash";
		} else if ($scope.currentRole === "Lead") {
			path = "/lead/dash";
		} else if ($scope.currentRole === "Administrator") {
			path = "/admin/dash";
		} else if ($scope.currentRole === "Viewer") {
			path = "/viewer/dash";
		}
		console.debug("redirecting to " + path);
		$location.path(path);
	});	

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('RecordDashBoardCtrl:  Detected project set/change');
		setModel();


	});
});