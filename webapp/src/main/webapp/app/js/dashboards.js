'use strict';

var mapProjectAppDashboards = angular.module('mapProjectAppDashboards', ['adf', 'LocalStorageModule']);

mapProjectAppDashboards.controller('ResolveConflictsDashboardCtrl', function ($scope, $routeParams, $rootScope, $location, localStorageService) {

	// model variable
	$scope.model = null;

	// On initialization, reset all values to null -- used to ensure watch functions work correctly
	$scope.mapProjects 	= null;
	$scope.currentUser 	= null;
	$scope.currentRole 	= null;
	$scope.preferences 	= null;
	$scope.focusProject = null;

	// Used for Reload/Refresh purposes -- after setting to null, get the locally stored values
	$scope.mapProjects  = localStorageService.get('mapProjects');
	$scope.currentUser  = localStorageService.get('currentUser');
	$scope.currentRole  = localStorageService.get('currentRole');
	$scope.preferences  = localStorageService.get('preferences');
	$scope.focusProject = localStorageService.get('focusProject');

	setModel();

	$scope.page = 'resolveConflictsDashboard';

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

	// function to change project from the header
	$scope.changeFocusProject = function(mapProject) {
		$scope.focusProject = mapProject;
		console.debug("changing project to " + $scope.focusProject.name);

		// update and broadcast the new focus project
		localStorageService.add('focusProject', $scope.focusProject);
		$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

		// update the user preferences
		$scope.preferences.lastMapProjectId = $scope.focusProject.id;
		localStorageService.add('preferences', $scope.preferences);
		$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', userPreferences: $scope.preferences});

	};

	$scope.goToHelp = function() {
		var path;
		if ($scope.page != 'mainDashboard') {
			path = "help/" + $scope.page + "Help.html";
		} else {
			path = "help/" + $scope.currentRole + "DashboardHelp.html";
		}
		console.debug("go to help page " + path);
		// redirect page
		$location.path(path);
	};
});



mapProjectAppDashboards.controller('dashboardCtrl', function ($rootScope, $scope, $http, $location, localStorageService) {

	// On initialization, reset all values to null -- used to ensure watch functions work correctly
	$scope.mapProjects 	= null;
	$scope.currentUser 	= null;
	$scope.currentRole 	= null;
	$scope.preferences 	= null;
	$scope.focusProject = null;

	// Used for Reload/Refresh purposes -- after setting to null, get the locally stored values
	$scope.mapProjects  = localStorageService.get('mapProjects');
	$scope.currentUser  = localStorageService.get('currentUser');
	$scope.currentRole  = localStorageService.get('currentRole');
	$scope.preferences  = localStorageService.get('preferences');
	$scope.focusProject = localStorageService.get('focusProject');

	$scope.page = 'mainDashboard';

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

	$scope.page = 'editDashboard';

	// must instantiate a default dashboard on call
	setModel();

	// on successful user retrieval, construct the dashboard
	$scope.$watch('currentRole', function() {
		setModel();
	});

	function setModel() {

		console.debug("Setting the dashboard based on role: " + $scope.currentRole);

		$scope.name = 'Dashboard';

		/**
		 * Viewer has the following widgets:
		 * - MapProject
		 */
		if (!$scope.currentRole || $scope.currentRole === 'Viewer') {
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
	}

	$scope.$on('adfDashboardChanged', function (event, name, model) {
		console.debug('adfDashboardChanged in DashBoardCtrl');
		console.debug(event);
		console.debug(name);
		console.debug(model);
		$scope.model = model;
	});

//	function to change project from the header
	$scope.changeFocusProject = function(mapProject) {
		$scope.focusProject = mapProject;
		console.debug("changing project to " + $scope.focusProject.name);

		// update and broadcast the new focus project
		localStorageService.add('focusProject', $scope.focusProject);
		$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

		// update the user preferences
		$scope.preferences.lastMapProjectId = $scope.focusProject.id;
		localStorageService.add('preferences', $scope.preferences);
		$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', userPreferences: $scope.preferences});

	};

	$scope.goToHelp = function() {
		var path;
		if ($scope.page != 'mainDashboard') {
			path = "help/" + $scope.page + "Help.html";
		} else {
			path = "help/" + $scope.currentRole + "DashboardHelp.html";
		}
		console.debug("go to help page " + path);
		// redirect page
		$location.path(path);
	};
});

mapProjectAppDashboards.controller('MapRecordDashboardCtrl', function ($scope, $rootScope, $routeParams, $location, localStorageService) {

	$scope.model = null;

	// On initialization, reset all values to null -- used to ensure watch functions work correctly
	$scope.mapProjects 	= null;
	$scope.currentUser 	= null;
	$scope.currentRole 	= null;
	$scope.preferences 	= null;
	$scope.focusProject = null;

	// Used for Reload/Refresh purposes -- after setting to null, get the locally stored values
	$scope.mapProjects  = localStorageService.get('mapProjects');
	$scope.currentUser  = localStorageService.get('currentUser');
	$scope.currentRole  = localStorageService.get('currentRole');
	$scope.preferences  = localStorageService.get('preferences');
	$scope.focusProject = localStorageService.get('focusProject');

	$scope.page = 'editDashboard';

	setModel();

	function setModel() {
		$scope.name = 'EditingDashboard';
		console.debug("Setting record dashboard model");
		console.debug($scope.model);

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

	};

	$scope.$on('adfDashboardChanged', function (event, name, model) {
		console.debug("Dashboard change detected by MapRecordDashboard");
		localStorageService.set(name, model);
	});

	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("RecordDashboardCtrl:  Detected change in focus project");

		// set the model to empty
		$scope.model = null;

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

	// function to change project from the header
	$scope.changeFocusProject = function(mapProject) {
		$scope.focusProject = mapProject;
		console.debug("changing project to " + $scope.focusProject.name);

		// update and broadcast the new focus project
		localStorageService.add('focusProject', $scope.focusProject);
		$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

		// update the user preferences
		$scope.preferences.lastMapProjectId = $scope.focusProject.id;
		localStorageService.add('preferences', $scope.preferences);
		$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', userPreferences: $scope.preferences});

	};

	$scope.goToHelp = function() {
		var path;
		if ($scope.page != 'mainDashboard') {
			path = "help/" + $scope.page + "Help.html";
		} else {
			path = "help/" + $scope.currentRole + "DashboardHelp.html";
		}
		console.debug("go to help page " + path);
		// redirect page
		$location.path(path);
	};
});