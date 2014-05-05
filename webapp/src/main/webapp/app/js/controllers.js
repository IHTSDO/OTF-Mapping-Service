'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap', 'mapProjectAppDirectives', 'mapProjectAppServices']);

var root_url = "${base.url}/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";
var root_validation = root_url + "validation/";
var root_workflow = root_url + "workflow/";

mapProjectAppControllers.run(function($rootScope) {
	$rootScope.glassPane = 0;

});


mapProjectAppControllers.controller('ResolveConflictsDashboardCtrl', function ($scope, $routeParams, $rootScope, localStorageService) {

	setModel();

	$scope.focusProject = localStorageService.get('focusProject');

	var currentUser = localStorageService.get('currentUser');
	var currentRole = localStorageService.get('currentRole');


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



mapProjectAppControllers.controller('dashboardCtrl', function ($rootScope, $scope, $http, localStorageService) {

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

mapProjectAppControllers.controller('MapRecordDashboardCtrl', function ($scope, $rootScope, $routeParams, $location, localStorageService) {

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


//Navigation

mapProjectAppControllers.controller('LoginCtrl', ['$scope', 'localStorageService', '$rootScope', '$location', '$http',
                                                  function ($scope, localStorageService, $rootScope, $location, $http) {

	// visiting this page clears the local cache
	localStorageService.clearAll();

	// broadcast user/role information clearing to rest of app
	$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: null});  
	$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: null});  
	$rootScope.$broadcast('localStorageModule.notification.setFocusProject', {key: 'focusProject', focusProject: null});
	$rootScope.$broadcast('localStorageModule.notificatoin.setPreferences', {key: 'preferences', preferences: null});

	// broadcast page to help mechanism
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'login'});



	// set all local variables to null
	$scope.user = [];
	$scope.users = [];
	$scope.error = [];
	$scope.preferences = [];

	// retrieve projects for focus controls
	$http({
		url: root_mapping + "project/projects",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(data) {
		$scope.projects = data.mapProject;
		localStorageService.add('mapProjects', data.mapProject);

	}).error(function(error) {
		$scope.error = $scope.error + "Could not retrieve map projects. "; 

	}).then(function(data) {
		console.debug("broadcasting projects");
		console.debug($scope.projects);
		$rootScope.$broadcast('localStorageModule.notification.setMapProjects',{key: 'mapProjects', mapProjects: $scope.projects});  

	});

	// retrieve metadata
	$http({
		url: root_metadata + "terminologies/latest",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}
	}).success(function(response) {
		var keyValuePairs = response.keyValuePair;
		for (var i = 0; i < keyValuePairs.length; i++) {
			console.debug(keyValuePairs[i]);
			$http({
				url: root_metadata + "all/" + keyValuePairs[i].key + "/" + keyValuePairs[i].value,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(metadata) {
			});

		}
	}).error(function() {
		console.debug("error loading response terminology info");
	});


	// retrieve users
	$http({
		url: root_mapping + "user/users",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(data) {
		$scope.users = data.mapUser;
		localStorageService.add('mapUsers', data.mapUser);
	}).error(function(error) {
		$scope.error = $scope.error + "Could not retrieve map users. "; 

	});

	// initial values for pick-list
	$scope.roles = [
	                {name:'Viewer', value:1},
	                {name:'Specialist', value:2},
	                {name:'Lead', value:3},
	                {name:'Administrator', value:4}];
	$scope.role = $scope.roles[0];  

	// login button directs to next page based on role selected
	$scope.go = function () {

		console.debug($scope.role);

		var path = "";

		if ($scope.role.name == "Specialist") {
			path = "/specialist/dash";
		} else if ($scope.role.name == "Lead") {
			path = "/lead/dash";
		} else if ($scope.role.name == "Administrator") {
			path = "/admin/dash";
		} else if ($scope.role.name == "Viewer") {
			path = "/viewer/dash";
		}

		// check that user has been selected
		if ($scope.user == null) {
			alert("You must specify a user");
		} else {

			// retrieve the user preferences
			$http({
				url: root_mapping + "userPreferences/" + $scope.user.userName,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
				console.debug($scope.projects);
				console.debug(data);
				$scope.preferences = data;
				$scope.preferences.lastLogin = new Date().getTime();
				localStorageService.add('preferences', $scope.preferences);
				for (var i = 0; i < $scope.projects.length; i++)  {
					if ($scope.projects[i].id === $scope.preferences.lastMapProjectId) {
						$scope.focusProject = $scope.projects[i];
					}
				}
				console.debug('Last project: ');
				console.debug($scope.focusProject);
				localStorageService.add('focusProject', $scope.focusProject);
				$rootScope.$broadcast('localStorageModule.notification.setPreferences', {key: 'preferences', preferences: $scope.preferences});
				$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  
			});

			// add the user information to local storage
			localStorageService.add('currentUser', $scope.user);
			localStorageService.add('currentRole', $scope.role.name);

			// broadcast the user information to rest of app
			$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: $scope.user});
			$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: $scope.role.name});

			// redirect page
			$location.path(path);
		}
	};
}]);




//Mapping Services


mapProjectAppControllers.controller('MapProjectListCtrl', 
		function ($scope, $http) {

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

});


/*
 * Controller for retrieving and displaying records associated with a concept
 */
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams', '$sce', '$rootScope', '$location', 'localStorageService', 
                                                              function ($scope, $http, $routeParams, $sce, $rootScope, $location, localStorageService) {

	// scope variables
	$scope.error = "";		// initially empty
	$scope.conceptId = $routeParams.conceptId;
	$scope.recordsInProject = [];
	$scope.recordsNotInProject = [];
	$scope.recordsInProjectNotFound = false; // set to true after record retrieval returns no records for focus project
	$scope.focusProject = localStorageService.get("focusProject");
	$scope.mapProjects = localStorageService.get("mapProjects");

	// local variables
	var projects = localStorageService.get("mapProjects");


	// retrieve current user and role
	$scope.currentUser = localStorageService.get("currentUser");
	$scope.currentRole = localStorageService.get("currentRole");

	// retrieve focus project on first call
	$scope.focusProject = localStorageService.get("focusProject");

	// watch for changes to focus project
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("RecordConceptListCtrl:  Detected change in focus project");      
		$scope.focusProject = parameters.focusProject;
		$scope.filterRecords();
	});	


	// broadcast page to help mechanism
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'concept'});

	// once focus project retrieved, retrieve the concept and records
	$scope.$watch('focusProject', function() {
		
		$scope.recordsInProjectNotFound = false;

		console.debug("RecordConceptCtrl:  Focus Project change");
		
		// retrieve projects information to ensure display handled properly
		$http({
			url: root_mapping + "project/projects",
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			projects = data.mapProject;
		}).error(function(error) {
			$scope.error = $scope.error + "Could not retrieve projects. "; 

		}).then(function() {

			// get all records for this concept
			$scope.getRecordsForConcept();
		});


		// find concept based on source terminology
		$http({
			url: root_content + "concept/" 
			+ $scope.focusProject.sourceTerminology + "/" 
			+ $scope.focusProject.sourceTerminologyVersion 
			+ "/id/" 
			+ $routeParams.conceptId,
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$scope.concept = data;
			$scope.findUnmappedDescendants();

			// find children based on source terminology
			$http({
				url: root_content + "concept/" 
				+ $scope.focusProject.sourceTerminology + "/" 
				+ $scope.focusProject.sourceTerminologyVersion 
				+ "/id/" 
				+ $routeParams.conceptId
				+ "/children",
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
				console.debug(data);
				$scope.concept.children = data.searchResult;

			}).error(function(error) {
				$scope.error = $scope.error + "Could not retrieve Concept children. ";    
			});
		}).error(function(error) {
			console.debug("Could not retrieve concept");
			$scope.error = $scope.error + "Could not retrieve Concept. ";    
		});
	});

	// function to return trusted html code (for tooltip content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};

	$scope.getRecordsForConcept = function() {
		// retrieve all records with this concept id
		$http({
			url: root_mapping + "record/conceptId/" + $routeParams.conceptId,
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$scope.records = data.mapRecord;
			$scope.filterRecords();
		}).error(function(error) {
			$scope.error = $scope.error + "Could not retrieve records. ";    
		}).then(function() {

			// check relation style flags
			if ($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE") {
				applyMapCategoryStyle();
			}

			if ($scope.focusProject.mapRelationStyle === "RELATIONSHIP_STYLE") {
				applyRelationshipStyle();
			}
		});
	};

	$scope.isEditable = function(record) {

		console.debug('isEditable');
		console.debug($scope.currentRole);
		console.debug($scope.currentUser);
		console.debug(record.owner);
		if (($scope.currentRole === 'Specialist' ||
				$scope.currentRole === 'Lead' ||
				$scope.currentRole === 'Admin') &&
				(record.workflowStatus === 'PUBLISHED' || record.workflowStatus === 'READY_FOR_PUBLICATION')) {

			return true;

		} else if ($scope.currentUser.userName === record.owner.userName) {
			return true;
		} else return false;
	};

	$scope.filterRecords = function() {
		$scope.recordsInProject = [];
		$scope.recordsNotInProject = [];
		for (var i = 0; i < $scope.records.length; i++) {
			if ($scope.records[i].mapProjectId === $scope.focusProject.id) {
				$scope.recordsInProject.push($scope.records[i]);
			} else {
				$scope.recordsNotInProject.push($scope.records[i]);
			}
		}

		// if no records for this project found, set flag
		if ($scope.recordsInProject.length == 0) $scope.recordsInProjectNotFound = true;
	};

	$scope.getProject = function(record) {
		for (var i = 0; i < projects.length; i++) {
			if (projects[i].id == record.mapProjectId) {
				return projects[i];
			}
		}
		return null;
	};

	$scope.getProjectFromName = function(name) {
		for (var i = 0; i < projects.length; i++) {
			if (projects[i].name === name) {
				return projects[i];
			}
		}
		return null;
	};

	$scope.getProjectName = function(record) {

		for (var i = 0; i < projects.length; i++) {
			if (projects[i].id == record.mapProjectId) {
				return projects[i].name;
			}
		}
		return null;
	};

	$scope.findUnmappedDescendants = function() {


		$http({
			url: root_mapping + "concept/" 
			+ $scope.concept.terminology + "/"
			+ $scope.concept.terminologyVersion + "/"
			+ "id/" + $scope.concept.terminologyId + "/"
			+ "threshold/10",
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			if (data.count > 0) $scope.unmappedDescendantsPresent = true;
			$scope.concept.unmappedDescendants = data.searchResult;
		});
	};

	// given a record, retrieves associated project's ruleBased flag
	$scope.getRuleBasedForRecord = function(record) {
		var project = $scope.getProject(record);
		return project.ruleBased;

	};

	function applyMapCategoryStyle() {

		// Cycle over all entries. If targetId is blank, show relationName as the target name
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 

				if ($scope.records[i].mapEntry[j].targetId === "") {
					$scope.records[i].mapEntry[j].targetName = "\"" + $scope.records[i].mapEntry[j].relationName + "\"";

				}
			}
		}
	};

	function applyRelationshipStyle() {
		// Cycle over all entries. Add the relation name to the advice list
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
				if ($scope.records[i].mapEntry[j].targetId === "") {	 
					// get the object for easy handling
					var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;

					// add the serialized advice	
					jsonObj.push({"id":"0", "name": "\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "detail":"\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "objectId":"0"});

					$scope.records[i].mapEntry[j].mapAdvice = jsonObj;
				}
			}
		}
	};

	$scope.createMapRecord = function(project) {

		if (!(project == null) && !(project === "")) {

			// get the project
			var countDescendantConcepts;

			// find concept based on source terminology
			$http({
				url: root_content + "concept/" 
				+ project.sourceTerminology + "/" 
				+ project.sourceTerminologyVersion 
				+ "/id/" 
				+ $scope.conceptId,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {

				$scope.concept = data;

			}).then(function(data) {


				// get descendant count
				$http({
					url: root_content + "concept/" 
					+ project.sourceTerminology + "/" 
					+ project.sourceTerminologyVersion 
					+ "/id/" 
					+ $scope.conceptId
					+ "/descendants",
					dataType: "json",
					method: "GET",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {    
					countDescendantConcepts = data.count;
				}).error(function(data) {
					countDescendantConcepts = 0;
				}).then(function() {

					// construct the map record
					var record = {
							"id" : "",
							"mapProjectId" : project.id,
							"conceptId" : $scope.concept.terminologyId,
							"conceptName" : $scope.concept.defaultPreferredName,
							"countDescendantConcepts": countDescendantConcepts,
							"mapEntry": [],
							"mapNote": [],
							"mapPrinciple": [],
							"owner" : $scope.currentUser,
							"lastModifiedBy" : $scope.currentUser,
							"workflowStatus" : 'NEW'
					};

					// add the map record
					$http({
						url: root_mapping + "record/add",
						dataType: "json",
						method: "PUT",
						data: record,
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {    
						record = data;
					}).error(function(data) {

					}).then(function() {

						$location.path("/record/recordId/" + record.id);
					});
				});
			});
		};	  
	};
}]);



/**
 * Controller for new test view (i.e. non-modal) for map record edit/create/delete functions
 */
mapProjectAppControllers.controller('MapRecordDetailCtrl', 
		['$scope', '$http', '$routeParams', '$sce', '$modal', 'localStorageService',

		 function ($scope, $http, $routeParams, $sce, $modal, localStorageService) {

			$scope.sortableOptions = {
					placeholder: "entry",
					connectWith: ".entry-container"
			};

			// initialize scope variables
			$scope.record = 	null;
			$scope.project = 	null;
			$scope.concept = 	null;
			$scope.groups = 	null;
			$scope.entries =    null;

			// initialize local variables
			var recordId = 		$routeParams.recordId; 
			var currentLocalId = 0;   // used for addition of new entries without hibernate id

			// obtain the record
			$http({
				url: root_mapping + "record/id/" + recordId,
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.record = data;

			}).error(function(error) {
				$scope.error = $scope.error + "Could not retrieve map record. ";

			}).then(function() {

				// obtain the record project
				$http({
					url: root_mapping + "project/id/" + $scope.record.mapProjectId,
					dataType: "json",
					method: "GET",
					headers: { "Content-Type": "application/json"}	
				}).success(function(data) {
					$scope.project = data;

					// initialize the preset age ranges
					initializePresetAgeRanges();

				}).error(function(error) {
					$scope.error = $scope.error + "Could not retrieve map project. ";
				}).then(function() {

					// obtain the record concept
					$http({
						url: root_content + "concept/" 
						+ $scope.project.sourceTerminology + "/"
						+ $scope.project.sourceTerminologyVersion + "/"
						+ "id/" + $scope.record.conceptId,
						dataType: "json",
						method: "GET",
						headers: { "Content-Type": "application/json"}	
					}).success(function(data) {
						$scope.concept = data;
					}).error(function(error) {
						$scope.error = $scope.error + "Could not retrieve record concept. ";
					});


					// get the groups
					if ($scope.project.groupStructure == true)
						getGroups();

					// intiialize the entries
					initializeEntries();
				});
			});


			/** Initialization functions */

			function initializeEntries() {

				$scope.entries = null;

				// assign rule summaries for display
				for (var i = 0; i < $scope.record.mapEntry.length; i++) {
					$scope.record.mapEntry[i].ruleSummary = 
						$scope.getRuleSummary($scope.record.mapEntry[i]);
				}

				// if no group structure, simply copy and sort
				if ($scope.project.groupStructure == false) {

					$scope.entries = sortByKey($scope.entries, 'mapPriority');

					// otherwise, initialize group arrays
				} else {

					// initiailize entry arrays for distribution by group
					$scope.entries = new Array(10);
					for (var i=0; i < $scope.entries.length; i++) $scope.entries[i] = new Array();

					// cycle over the entries and assign to group bins
					for (var i=0; i < $scope.record.mapEntry.length; i++) {
						$scope.entries[$scope.record.mapEntry[i].mapGroup].push($scope.record.mapEntry[i]);
					}

					// cycle over group bins and sort contents by map priority
					for (var i=0; i< $scope.entries.length; i++) {
						$scope.entries[i] = sortByKey($scope.entries[i], 'mapPriority');
					}
				}


			}

			/**
			 * Utility functions
			 */ 

			function sortByKey(array, key) {
				return array.sort(function(a, b) {
					var x = a[key]; var y = b[key];
					return ((x < y) ? -1 : ((x > y) ? 1 : 0));
				});
			};

			// function to add an element and assign a local id if not tracked by hibernate
			Array.prototype.addElement = function(elem) {

				// if hibernate id, simply add
				if (elem.id != null && elem.id != '') {
					this.push(elem);

					// otherwise, assign a unique localid
				} else {

					// get the maximum local id already assigned
					var maxLocalId = -1;
					$.map(this, function(v,i) {
						if (v.hasOwnProperty("localId")) {
							if (v['localId'] > maxLocalId) maxLocalId = v['localId'];
						}
					});

					elem['localId'] = maxLocalId == -1 ? 1 : maxLocalId + 1;
				}

				this.push(elem);
			};

			// function to remove an element by id or localid
			// instantiated to negate necessity for equals methods for map objects
			//   which may not be strictly identical via string or key comparison
			Array.prototype.removeElement = function(elem) {

				// switch on type of id
				var idType = elem.hasOwnProperty('localId') ? 'localId' : 'id';

				var array = new Array();
				$.map(this, function(v,i){
					if (v[idType] != elem[idType]) array.push(v);
				});

				this.length = 0; //clear original array
				this.push.apply(this, array); //push all elements except the one we want to delete
			};

			Array.prototype.sortByKey = function(array, key) {


			}

			/**
			 * MAP RECORD FUNCTIONS
			 */
			$scope.saveMapRecord = function() {

				///////////////////////////
				// Group and MapPriority //
				///////////////////////////

				// if not group structured project
				if ($scope.project.groupStructure == false) {

					// cycle over entries and assign map priority based on position
					for (var i = 0; i < $scope.entries.length; i++) {
						$scope.entries[i].mapPriority = i+1;
					}

					$scope.record.mapEntry = $scope.entries;

					// if group structured project
				} else {

					var entries = new Array();

					// cycle over each group bin
					for (var i = 0; i < $scope.entries.length; i++) {

						// cycle over entries in each group bin
						for (var j = 0; j < $scope.entries[i].length; j++) {

							console.debug("Assigning group and priority to " + i + " " + j);
							$scope.entries[i][j].mapGroup = i;
							$scope.entries[i][j].mapPriority = j+1;

							entries.push($scope.entries[i][j]);

						}
					}

					console.debug("modified:");
					console.debug(entries);

					$scope.record.mapEntry = entries;
				}


				console.debug($scope.record);
				console.debug($scope.record.mapEntry);


				console.debug("Validating the map entry");
				// validate the record
				$http({
					url: root_validation + "record/validate",
					dataType: "json",
					data: $scope.record,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}
				}).success(function(data) {
					console.debug("validation results:");
					console.debug(data);
					$scope.validationResult = data;
				}).error(function(data) {
					$scope.validationResult = null;
					console.debug("Failed to validate map record");
				}).then(function(data) {

					// if no error messages were returned, save the record
					if ($scope.validationResult.errors.length == 0)  {

						$http({
							url: root_mapping + "record/update",
							dataType: "json",
							data: $scope.record,
							method: "POST",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							$scope.record = data;
							$scope.recordSuccess = "Record saved.";
							$scope.recordError = "";
							window.history.back();
						}).error(function(data) {
							$scope.recordSuccess = "";
							$scope.recordError = "Error saving record.";
						});

						// otherwise, display the errors
					} else {
						$scope.recordError = "Could not save map record due to errors:";

						for (var i = 0; i < $scope.recordValidationMessages.length; i++) {
							$scope.recordError = $scope.recordValidationMessages[i] + "\n\n";
						}
					}

				});
			};

			// discard changes
			$scope.cancelMapRecord = function() {
				window.history.back();
			};

			$scope.deleteMapRecord = function() {
				var confirmDelete = confirm("Deleting this map record will also destroy the map entries attached to this record.\n\nAre you sure you want to delete this record?");
				if (confirmDelete == true) {

					$http({
						url: root_mapping + "record/delete",
						dataType: "json",
						data: $scope.record,
						method: "DELETE",
						headers: {"Content-Type": "application/json"}
					}).success(function(data) {
						$scope.record = data;
					}).error(function(data) {
						console.debug("Existing record update ERROR");	  
					});
				}
			};

			$scope.addRecordPrinciple = function(record, principle) {

				// check if principle valid
				if (principle === '') {
					$scope.errorAddRecordPrinciple = "Principle cannot be empty";
				} else if (principle == null) {
					$scope.errorAddRecordPrinciple = "This principle is not found in allowable principles for this map project";
				} else {
					$scope.errorAddRecordPrinciple = "";

					// check if principle already present
					var principlePresent = false;
					for (var i = 0; i < record.mapPrinciple.length; i++) {
						if (principle.id == record.mapPrinciple[i].id) principlePresent = true;
					}

					if (principlePresent == true) {
						$scope.errorAddRecordPrinciple = "The principle with id " + principle.principleId  + " is already attached to the map record";
					} else {
						$scope.record['mapPrinciple'].push(principle);
					};

					$scope.principleInput = "";
				};
			};

			$scope.removeRecordPrinciple = function(record, principle) {
				record['mapPrinciple'].removeElement(principle);
				$scope.record = record;
			};

			$scope.addRecordNote = function(record, note) {
				// check if note non-empty
				if (note === '' || note == null) {
					$scope.errorAddRecordNote = "Note cannot be empty";
				} else {

					// construct Json user
					var mapUser = null;

					// construct note object
					var mapNote = new Array();
					mapNote.note = note;
					mapNote.timestamp = (new Date()).getMilliseconds();
					mapNote.user = localStorageService.get('currentUser');

					// add note to record
					record['mapNote'].addElement(mapNote);

					// set scope record to record
					$scope.record = record;

				}
			};

			$scope.removeRecordNote = function(record, note) {
				record['mapNote'].removeElement(note);
				$scope.record = record;
			};




			/**
			 * MAP ENTRY FUNCTIONS
			 */

			$scope.entriesEqualById = function(entry1, entry2) {

				// if hibernate id, test on id
				if (entry1.id != null && entry1.id != '') {
					return entry1.id === entry2.id
					// otherwise, local id
				} else {
					return entry1.localId === entry2.localId;
				}
			}

			// Returns all entries belonging to a particular map group
			$scope.getEntries = function(mapGroup) {

				// if no argument, return all entries
				if (mapGroup == null) {
					return $scope.record.mapEntry;
				}

				// cycle over map entries and extract those with this map group
				var entries = new Array();

				for (var i = 0; i < $scope.record.mapEntry.length; i++) {
					if (parseInt($scope.record.mapEntry[i].mapGroup, 10) === parseInt(mapGroup, 10)) {
						entries.push($scope.record.mapEntry[i]);
					};
				};

				return entries;  
			};

			// Returns a summary string for the entry rule type
			$scope.getRuleSummary = function(entry) {
				if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
					return "";
				} else {

					if (entry.rule.toUpperCase().indexOf("GENDER") != -1) return "[GENDER]";
					else if (entry.rule.toUpperCase().indexOf("FEMALE") != -1) return "[FEMALE]";
					else if (entry.rule.toUpperCase().indexOf("MALE") != -1) return "[MALE]";
					else if (entry.rule.toUpperCase().indexOf("AGE") != -1) return "[AGE]";
					else if (entry.rule.toUpperCase().indexOf("TRUE") != -1) return "[TRUE]";
					else return "";
				} 	

			};

			// Sets the scope variable for the active entry
			$scope.selectEntry = function(entry) {
				$scope.entry = angular.copy(entry);
			};

			// function for adding an empty map entry to a record
			$scope.addMapEntry = function(group) {

				// create blank entry associated with this id
				var newEntry = {
						"id": "",
						"mapRecordId": $scope.record.id,
						"targetId":"",
						"targetName":"",
						"rule":"TRUE",
						"mapPriority": "",
						"relationId":"",
						"relationName":"",
						"mapBlock":"",
						"mapGroup": group,
						"mapAdvice":[],
						"mapPrinciples":[],
						"localId": currentLocalId + 1
				};

				newEntry.ruleSummary = $scope.getRuleSummary(newEntry);

				$scope.entries[group].push(newEntry);
				$scope.selectEntry(newEntry);

			};

			// Saves the selected entry to the map record
			$scope.saveMapEntry = function(entry) {

				console.debug('SAVE MAP ENTRY:');
				console.debug(entry);

				// find the entry
				if ($scope.project.groupStructure == false) {
					for (var i = 0; i < $scope.entries.length; i++) {
						if ($scope.entriesEqualById($scope.entries[i], entry) == true) {

							$scope.entries[i] = entry;
						}
					}
				} else {

					for (var i = 0; i < $scope.entries.length; i++) {
						for (var j = 0; j < $scope.entries[i].length; j++) {
							if ($scope.entriesEqualById($scope.entries[i][j], entry) == true) {

								$scope.entries[i][j] = entry;
							}	
						}
					}
				}

				$scope.entry = null;

				/*var index = findEntryIndex(entry);
				if (index == -1) {
					alert("Fatal Error:  Entry could not be saved.\n\nThis entry does not belong to the current Map Record.");
					$scope.entrySuccess = "";
					$scope.entryError = "Error saving entry";
				} else {
					$scope.record.mapEntry[index] = entry;
					$scope.entrySuccess = "Entry saved.";
					$scope.entryError = "";
				}*/
			};

			// Cancels changes to the selected map entry
			$scope.cancelMapEntry = function() {
				$scope.entrySuccess = "";
				$scope.entryError = "";
				$scope.entry = null;
			};

			// Deletes selected map entry
			$scope.deleteMapEntry = function(entry) { 

				$scope.entrySuccess = "";
				$scope.entryError = "";


				var confirmDelete = confirm("Are you sure you want to delete this entry?");
				if (confirmDelete == true) {

					if ($scope.project.groupStructure == false) {

						var entries = new Array();

						for (var i = 0; i < $scope.entries.length; i++) {
							if ($scope.entriesEqualById(entry, $scope.entries[i]) == false) {
								entries.push($scope.entries[i]);
							}
						}

						$scope.entries = entries;

					} else {
						for (var i = 0; i < $scope.entries.length; i++) {

							var entries = new Array();

							for (var j = 0; j < $scope.entries[i].length; j++) {
								if ($scope.entriesEqualById(entry, $scope.entries[i][j]) == false) {
									entries.push($scope.entries[i][j]);
								}
							}

							$scope.entries[i] = entries;
						}
					}
				}
			};

			$scope.addEntryAdvice = function(entry, advice) {

				// check if advice valid
				if (advice == '') {
					$scope.errorAddAdvice = "Advice cannot be empty";
				} else if (advice == null) {
					$scope.errorAddAdvice = "This advice is not found in allowable advices for this project";
				} else {
					$scope.errorAddAdvice = "";

					// check if this advice is already present
					var advicePresent = false;
					for (var i = 0; i < entry.mapAdvice.length; i++) {
						if (advice.id === entry.mapAdvice[i].id) advicePresent = true;
					}

					if (advicePresent == true) {
						$scope.errorAddAdvice = "This advice " + advice.detail + " is already attached to this entry";
					} else {
						$scope.entry['mapAdvice'].push(advice);
					}

					$scope.adviceInput = "";
				}
			};

			$scope.removeEntryAdvice = function(entry, advice) {	  
				entry['mapAdvice'].removeElement(advice);
				$scope.entry = entry;  
			};


			function findEntryIndex(entry) {

				// check if entry has hibernate id
				if (entry.id != null && entry.id != '') {

					// cycle over entries until matching id found and return index
					for (var i = 0; i < $scope.record.mapEntry.length; i++) {
						if (entry.id === $scope.record.mapEntry[i].id) return i;
					}

					// otherwise, check for entries with local id
				} else {

					for (var i = 0; i < $scope.record.mapEntry.length; i++) {
						// if no hibernate id, skip this record, otherwise check by localId
						if ($scope.record.mapEntry[i].id === null || $scope.record.mapEntry[i].id === '') {

							if (entry.localId == $scope.record.mapEntry[i].localId) return i;
						}  
					}
				}

				return -1;
			};

			/**
			 * RULE CONSTRUCTION FUNCTIONS
			 */

			$scope.constructRule = function(entry) {

				$scope.openRuleConstructor();
			};

			$scope.openRuleConstructor = function() {

				var modalInstance = $modal.open({
					templateUrl: 'partials/rule-modal.html',
					controller: RuleConstructorModalCtrl,
					resolve: {
						presetAgeRanges: function() {
							return angular.copy($scope.project.mapAgeRange);
						}
					}
				});

				modalInstance.result.then(function(rule) {
					$scope.entry.rule = rule;
					$scope.entry.ruleSummary = $scope.getRuleSummary($scope.entry);
				});
			};

			// set up the preset age range defaults
			var RuleConstructorModalCtrl = function($scope, $http, $modalInstance, presetAgeRanges) {

				$scope.ageRange={"name":"" , "lowerValue":"", "lowerInclusive":"", "lowerUnits":"", 
						"upperValue":"", "upperInclusive":"", "upperUnits":""},

						$scope.presetAgeRanges = presetAgeRanges;
				$scope.ruleCategories = ['TRUE', 'Gender - Male', 'Gender - Female', 'Age - Chronological', 'Age - At Onset'];


				$scope.saveRule = function() {
					$modalInstance.close($scope.rule);
				};

				$scope.cancelRule = function() {
					$modalInstance.dismiss('cancel');
				};

				$scope.changeRuleCategory = function(ruleCategory) {

					$scope.ageRange = null;
					$scope.constructRule(ruleCategory, null);
				};

				$scope.constructRule = function(ruleCategory, ageRange) {

					$scope.rule = "";

					if (ruleCategory === "TRUE") {
						$scope.rule = "TRUE";
					}

					else if (ruleCategory === "Gender - Male") {
						$scope.rule = "IFA 248153007 | Male (finding) |";
					}

					else if (ruleCategory === "Gender - Female") {
						$scope.rule = "IFA 248152002 | Female (finding) |";
					}

					else if (ageRange != null) {

						if (ruleCategory === "Age - Chronological" || ruleCategory === "Age - At Onset") {

							var ruleText = (ruleCategory === "Age - Chronological") ?
									"IFA 424144002 | Current chronological age (observable entity)" :
										"IFA 445518008 | Age at onset of clinical finding (observable entity)"	;


							if (ageRange.lowerValue != "-1") {
								$scope.rule += ruleText
								+  " | " + (ageRange.lowerInclusive == true ? ">=" : ">") + " "
								+  ageRange.lowerValue + " "
								+  ageRange.lowerUnits;
							}

							if (ageRange.lowerValue != "-1" && ageRange.upperValue != "-1")
								$scope.rule += " AND ";

							if (ageRange.upperValue != "-1") {
								$scope.rule += ruleText
								+  " | " + (ageRange.upperInclusive == true ? "<=" : "<") + " "
								+  ageRange.upperValue + " "
								+  ageRange.upperUnits;
							}			
						}
					} else $scope.rule = null;
				};

				$scope.constructRuleAgeHelper = function(ruleCategory, ageRange) {
					$scope.constructRule($scope.ruleCategory);
				};

			};

			function initializePresetAgeRanges() {  
				$scope.presetAgeRanges = $scope.project.mapAgeRange;

				// set the preset age range strings
				for (var i = 0; i < $scope.presetAgeRanges.length; i++) {
					var presetAgeRangeStr = $scope.presetAgeRanges[i].name + ", ";

					if ($scope.presetAgeRanges[i].lowerValue != null && $scope.presetAgeRanges[i].lowerValue != "-1") {
						presetAgeRangeStr += ($scope.presetAgeRanges[i].lowerInclusive == true ? ">=" : ">") + " "
						+  $scope.presetAgeRanges[i].lowerValue + " "
						+  $scope.presetAgeRanges[i].lowerUnits;
					}

					if ($scope.presetAgeRanges[i].lowerValue != null && $scope.presetAgeRanges[i].lowerValue != "-1" &&
							$scope.presetAgeRanges[i].upperValue != null && $scope.presetAgeRanges[i].upperValue != "-1") {

						presetAgeRangeStr += " and ";
					}

					if ($scope.presetAgeRanges[i].upperValue != null && $scope.presetAgeRanges[i].upperValue != "-1") {

						presetAgeRangeStr += ($scope.presetAgeRanges[i].upperInclusive == true ? "<=" : "<") + " "
						+  $scope.presetAgeRanges[i].upperValue + " "
						+  $scope.presetAgeRanges[i].upperUnits;
					}

					$scope.presetAgeRanges[i].stringName = presetAgeRangeStr;
				};
			};




			/** 
			 * MAP GROUP FUNCTIONS
			 */

			// Retrieves groups from the existing entries
			function getGroups() {

				$scope.groups = new Array();
				for (var i = 0; i < $scope.record.mapEntry.length; i++) {			  

					if ($scope.groups.indexOf(parseInt($scope.record.mapEntry[i].mapGroup, 10)) == -1) {
						$scope.groups.push(parseInt($scope.record.mapEntry[i].mapGroup, 10));
					};
				};

				// if no groups found, add a default group
				if ($scope.groups.length == 0) $scope.groups.push(1);

			};

			// Adds a map group to the existing list
			$scope.addMapGroup = function() {

				// find first numeric group not already in list
				var i = 1;
				while ($scope.groups.indexOf(i) != -1) i++;

				$scope.groups.push(i);
			};

			// Removes a map group if it exists
			$scope.removeMapGroup = function(group) {   	  
				var newGroups = new Array();
				for (var i = 0; i < $scope.groups.length; i++) {
					if ($scope.groups[i] != group) newGroups.push($scope.groups[i]);
				}
				$scope.groups = newGroups;
			};


			/**
			 * ENTRY EDITING FUNCTIONS
			 */
			$scope.retrieveTargetConcepts = function(query) {

				// execute query for concepts
				$http({
					url: root_content + "concept/query/" + query,
					dataType: "json",
					method: "GET",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {

					console.debug(data);

					// eliminate concepts that don't match target terminology

					$scope.targetConcepts = [];

					for (var i = 0; i < data.count; i++) {
						if (data.searchResult[i].terminology === $scope.project.destinationTerminology &&
								data.searchResult[i].terminologyVersion === $scope.project.destinationTerminologyVersion) {

							$scope.targetConcepts.push(data.searchResult[i]);
						};
					};



				}).error(function(data) {
					$scope.errorCreateRecord = "Failed to retrieve entries";
				});
			};

			$scope.resetTargetConcepts = function() {
				console.debug("resetTargetConcepts() called");
				$scope.queryTarget = "";
				$scope.targetConcepts = [];
			};

			$scope.selectTargetConcept = function(entry, target) {
				console.debug("selectTargetConcept() called");
				console.debug(target);
				entry.targetId = target.terminologyId;
				entry.targetName = target.value;
				$scope.resetTargetConcepts();
			};





		}]);


/**
 * Controller for Map Project Records view
 * 
 * Basic function:
 * 1) Retrieve map project
 * 2) Retrieve concept associated with map project refsetid
 * 3) Retrieve records
 * 
 * Scope functions (accessible from html)
 * - $scope.resetSearch 		clears query and launches unfiltered record retrieval request
 * - $scope.retrieveRecords 	launches new record retrieval request based on current paging/filtering/sorting parameters
 * - $scope.to_trusted	 		converts unsafe html into usable/displayable code
 *  
 * Helper functions: 
 * - setPagination				sets the relevant pagination attributes based on current page
 * - getNRecords				retrieves the total number of records available given filtering parameters
 * - getUnmappedDescendants		retrieves the unmapped descendants of a concept
 * - applyMapCategoryStyle		modifies map entries for display based on MAP_CATEGORY_STYLE
 * - applyRelationshipStyle		modifies map entries for display based on RELATIONSHIP_STYLE
 * - constructPfsParameterObj	creates a PfsParameter object from current paging/filtering/sorting parameters, consumed by RESTful service
 */

mapProjectAppControllers.controller('MapProjectRecordCtrl', ['$scope', '$http', '$routeParams', '$sce', '$rootScope', '$location', 'localStorageService',
                                                             function ($scope, $http, $routeParams, $sce, $rootScope, $location, localStorageService) {



	// the project id, extracted from route params
	$scope.projectId = $routeParams.projectId;

	// status variables
	$scope.unmappedDescendantsPresent = false;
	$scope.mapNotesPresent = false;
	$scope.mapAdvicesPresent = false;

	// error variables
	$scope.errorProject = "";
	$scope.errorConcept = "";
	$scope.errorRecords = "";

	// pagination variables
	$scope.recordsPerPage = 10;

	// for collapse directive
	$scope.isCollapsed = true;

	// watch for changes to focus project
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("ProjectRecordCtrl:  Detected change in focus project");      
		$scope.focusProject = parameters.focusProject;
	});	

	// broadcast page to help mechanism
	$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'records'});

	// retrieve the current global variables
	$scope.focusProject = localStorageService.get('focusProject');
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');

	$scope.$watch('focusProject', function() {
		$scope.projectId = $scope.focusProject.id;
		$scope.getRecordsForProject();
	});


	$scope.getRecordsForProject = function() {

		$scope.project = $scope.focusProject;

		// load first page
		$scope.retrieveRecords(1);
	};


	// Constructs trusted html code from raw/untrusted html code
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};

	// function to clear input box and return to initial view
	$scope.resetSearch = function() {
		$scope.query = null;
		$scope.retrieveRecords(1);
	};

	// function to retrieve records for a specified page
	$scope.retrieveRecords = function(page) {

		console.debug('Retrieving records');
		
		// retrieve pagination information for the upcoming query
		setPagination($scope.recordsPerPage);

		// construct html parameters parameter
		var pfsParameterObj = constructPfsParameterObj(page);
		var query_url = root_mapping + "record/projectId/" + $scope.project.objectId;

		// retrieve map records
		$http({
			url: query_url,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$scope.records = data.mapRecord;
			$scope.statusRecordLoad = "";
			$scope.recordPage = page;

		}).error(function(error) {
			$scope.errorRecord = "Error retrieving map records";
			console.debug("changeRecordPage error");
		}).then(function(data) {

			// check if icon legends are necessary
			$scope.unmappedDescendantsPresent = false;
			$scope.mapNotesPresent = false;
			$scope.mapAdvicesPresent = false;

			// check if any notes or advices are present
			for (var i = 0; i < $scope.records.length; i++) {
				if ($scope.records[i].mapNote.length > 0) {
					$scope.mapNotesPresent = true;
				}
				for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {

					if ($scope.records[i].mapEntry[j].mapNote.length > 0) {
						$scope.mapNotesPresent = true;
					};
					if ($scope.records[i].mapEntry[j].mapAdvice.length > 0) {
						$scope.mapAdvicesPresent = true;
					}
				};
			};

			// check relation syle flags
			if ($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE") {
				applyMapCategoryStyle();
			}

			if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
				applyRelationshipStyle();
			}			 			  

			// get unmapped descendants (checking done in routine)
			if ($scope.records.length > 0) {	
				getUnmappedDescendants(0);
			}
		});
	}; 

	// Constructs a paging/filtering/sorting parameters object for RESTful consumption
	function constructPfsParameterObj(page) {

		return {"startIndex": (page-1)*$scope.recordsPerPage,
			"maxResults": $scope.recordsPerPage, 
			"sortField": null, 
			"filterString": $scope.query == null ? null : $scope.query};  // assigning simply to $scope.query when null produces undefined

	}

	// function to set the relevant pagination fields
	function setPagination(recordsPerPage) {

		// set scope variable for total records
		getNRecords();

		// set pagination variables
		$scope.recordsPerPage = recordsPerPage;
		$scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
	};

	// function query for the number of records associated with full set or query
	function getNRecords() {

		var query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/nRecords";
		var pfsParameterObj = constructPfsParameterObj(1);

		// retrieve the total number of records associated with this map project
		$http({
			url: query_url,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$scope.nRecords = data;

		}).error(function(error) {
			$scope.nRecords = 0;
			console.debug("getNRecords error");
		});
	};

	function getUnmappedDescendants(index) {

		// before processing this record, make call to start next async request
		if (index < $scope.records.length-1) {
			getUnmappedDescendants(index+1);
		}

		$scope.records[index].unmappedDescendants = [];

		// if descendants below threshold for lower-level concept, check for unmapped
		if ($scope.records[index].countDescendantConcepts < 11) {

			$http({
				url: root_mapping + "concept/" 
				+ $scope.project.sourceTerminology + "/"
				+ $scope.project.sourceTerminologyVersion + "/"
				+ "id/" + $scope.records[index].conceptId + "/"
				+ "threshold/10",
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				if (data.count > 0) $scope.unmappedDescendantsPresent = true;
				$scope.records[index].unmappedDescendants = data.searchResult;
			});
		} 

	};

	function applyMapCategoryStyle() {

		// set the category display text
		$scope.mapRelationStyleText = "Map Category Style";

		// Cycle over all entries. If targetId is blank, show relationName as the target name
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 

				if ($scope.records[i].mapEntry[j].targetId === "") {
					$scope.records[i].mapEntry[j].targetName = "\"" + $scope.records[i].mapEntry[j].relationName + "\"";
				}
			}
		}
	};

	function applyRelationshipStyle() {

		$scope.mapRelationStyleText = "Relationship Style";

		// Cycle over all entries. Add the relation name to the advice list
		for (var i = 0; i < $scope.records.length; i++) {		 
			for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
				if ($scope.records[i].mapEntry[j].targetId === "") {	 
					// get the object for easy handling
					var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;

					// add the serialized advice	
					jsonObj.push({"id":"0", "name": "\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "detail":"\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "objectId":"0"});

					$scope.records[i].mapEntry[j].mapAdvice = jsonObj;
				}
			}
		}
	};


}]);

mapProjectAppControllers.controller('MapProjectDetailCtrl', 
		['$scope', '$http', '$sce', '$rootScope', '$location', 'localStorageService',
		 function ($scope, $http, $sce, $rootScope, $location, localStorageService) {

			// broadcast page to help mechanism
			$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'project'});

			$scope.focusProject = localStorageService.get('focusProject');
			
			// watch for focus project change
			$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
				console.debug("MapProjectDetailCtrl: Detected change in focus project");
				$scope.focusProject = parameters.focusProject;  
			});
			
			$scope.$watch('focusProject', function() {

				console.debug('Formatting project details');


				// apply map type text styling
				if ($scope.focusProject.mapType === "SIMPLE_MAP") $scope.mapTypeText = "Simple Mapping";
				else if ($scope.focusProject.mapType === "COMPLEX_MAP") $scope.mapTypeText = "Complex Mapping";
				else if($scope.focusProject.mapType === "EXTENDED_MAP") $scope.mapTypeText = "Extended Mapping";
				else $scope.mapTypeText = "No mapping type specified";

				// apply relation style text styling
				console.debug($scope.focusProject.mapRelationStyle);
				console.debug($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE");
				if ($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE") $scope.mapRelationStyleText = "Map Category Style";
				else if ($scope.focusProject.mapRelationStyle === "RELATIONSHIP_STYLE") $scope.mapRelationStyleText = "Relationship Style";
				else $scope.mapRelationStyleText = "No relation style specified";


				// determine if this project has a principles document
				if ($scope.focusProject.destinationTerminology == "ICD10") {
					$scope.focusProject.mapPrincipleDocumentPath = "doc/";
					$scope.focusProject.mapPrincipleDocument = "ICD10_MappingPersonnelHandbook.docx";
					$scope.focusProject.mapPrincipleDocumentName = "Mapping Personnel Handbook";
				} else {
					$scope.focusProject.mapPrincipleDocument = null;
				}

				// set the scope maps
				$scope.scopeMap = {};
				$scope.scopeExcludedMap = {};
				
				// set pagination variables
				$scope.pageSize = 5;
				$scope.maxSize = 5;
				$scope.getPagedAdvices(1);
				$scope.getPagedRelations(1);
				$scope.getPagedPrinciples(1);
				$scope.getPagedScopeConcepts(1);
				$scope.getPagedScopeExcludedConcepts(1);
				$scope.orderProp = 'id';

			});

			$scope.goMapRecords = function () {
				console.debug($scope.role);

				var path = "/project/records";
					// redirect page
					$location.path(path);
			};


			// function to return trusted html code (for tooltip content)
			$scope.to_trusted = function(html_code) {
				return $sce.trustAsHtml(html_code);
			};



			///////////////////////////////////////////////////////////////
			// Functions to display and filter advices and principles
			// NOTE: This is a workaround due to pagination issues
			///////////////////////////////////////////////////////////////

			// get paged functions
			// - sorts (by id) filtered elements
			// - counts number of filtered elmeents
			// - returns artificial page via slice

			$scope.getPagedAdvices = function (page) {

				$scope.pagedAdvice = $scope.sortByKey($scope.focusProject.mapAdvice, 'id')
				.filter(containsAdviceFilter);
				$scope.pagedAdviceCount = $scope.pagedAdvice.length;
				$scope.pagedAdvice = $scope.pagedAdvice
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
			};

			$scope.getPagedRelations = function (page) {

				$scope.pagedRelation = $scope.sortByKey($scope.focusProject.mapRelation, 'id')
				.filter(containsRelationFilter);
				$scope.pagedRelationCount = $scope.pagedRelation.length;
				$scope.pagedRelation = $scope.pagedRelation
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
			};

			$scope.getPagedPrinciples = function (page) {

				$scope.pagedPrinciple = $scope.sortByKey($scope.focusProject.mapPrinciple, 'id')
				.filter(containsPrincipleFilter);
				$scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
				$scope.pagedPrinciple = $scope.pagedPrinciple
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);

				console.debug($scope.pagedPrinciple);
			};

			$scope.getPagedScopeConcepts = function (page) {
				console.debug("Called paged scope concept for page " + page); 
				
				$scope.pagedScopeConcept = $scope.focusProject.scopeConcepts;
				$scope.pagedScopeConceptCount = $scope.pagedScopeConcept.length;
				
				$scope.pagedScopeConcept = $scope.pagedScopeConcept
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
				
				
				// find concept based on source terminology
				for (var i = 0; i < $scope.pagedScopeConcept.length; i++) {
					$rootScope.glassPane++;
					$http({
						url: root_content + "concept/" 
						+ $scope.focusProject.sourceTerminology +  "/" 
						+ $scope.focusProject.sourceTerminologyVersion 
						+ "/id/" 
						+ $scope.focusProject.scopeConcepts[i],
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {
						$rootScope.glassPane--;
						var obj = {
								key: data.terminologyId,
								concept: data
						};  
						$scope.scopeMap[obj.key] = obj.concept.defaultPreferredName;
					}).error(function(error) {
						$rootScope.glassPane--;
						console.debug("Could not retrieve concept");
						$scope.error = $scope.error + "Could not retrieve Concept. ";    
					});

				}
				
				console.debug($scope.pagedScopeConcept);
			};

			$scope.getPagedScopeExcludedConcepts = function (page) {
				console.debug("Called paged scope excluded concept for page " + page);
				$scope.pagedScopeExcludedConcept = $scope.sortByKey($scope.focusProject.scopeExcludedConcepts, 'id')
				.filter(containsScopeExcludedConceptFilter);
				$scope.pagedScopeExcludedConceptCount = $scope.pagedScopeExcludedConcept.length;
				$scope.pagedScopeExcludedConcept = $scope.pagedScopeExcludedConcept
				.slice((page-1)*$scope.pageSize,
						page*$scope.pageSize);
				
				
				// fill the scope map for these variables
				for (var i = 0; i < $scope.pagedScopeExcludedConcept.length; i++) {
					$rootScope.glassPane++;
					$http({
						url: root_content + "concept/" 
						+ $scope.focusProject.sourceTerminology +  "/" 
						+ $scope.focusProject.sourceTerminologyVersion 
						+ "/id/" 
						+ $scope.focusProject.scopeExcludedConcepts[i],
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {
						$rootScope.glassPane--;
						var obj = {
								key: data.terminologyId,
								concept: data
						};  
						$scope.scopeExcludedMap[obj.key] = obj.concept.defaultPreferredName;
					}).error(function(error) {
						$rootScope.glassPane--;
						console.debug("Could not retrieve concept");
						$scope.error = $scope.error + "Could not retrieve Concept. ";    
					});
				}
				

				console.debug($scope.pagedScopeExcludedConcept);
			};

			// functions to reset the filter and retrieve unfiltered results

			$scope.resetAdviceFilter = function() {
				$scope.adviceFilter = "";
				$scope.getPagedAdvices(1);
			};

			$scope.resetRelationFilter = function() {
				$scope.relationFilter = "";
				$scope.getPagedRelationss(1);
			};

			$scope.resetPrincipleFilter = function() {
				$scope.principleFilter = "";
				$scope.getPagedPrinciples(1);
			};

			$scope.resetScopeConceptFilter = function() {
				$scope.scopeConceptFilter = "";
				$scope.getPagedScopeConcepts(1);
			};		

			$scope.resetScopeExcludedConceptFilter = function() {
				$scope.scopeExcludedConceptFilter = "";
				$scope.getPagedScopeExcludedConcepts(1);
			};	

			// element-specific functions for filtering
			// don't want to search id or objectId

			function containsAdviceFilter(element) {

				// check if advice filter is empty
				if ($scope.adviceFilter === "" || $scope.adviceFilter == null) return true;

				// otherwise check if upper-case advice filter matches upper-case element name or detail
				if ( element.detail.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}

			function containsRelationFilter(element) {

				// check if relation filter is empty
				if ($scope.relationFilter === "" || $scope.relationFilter == null) return true;

				// otherwise check if upper-case relation filter matches upper-case element name or detail
				if ( element.terminologyId.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}

			function containsPrincipleFilter(element) {

				// check if principle filter is empty
				if ($scope.principleFilter === "" || $scope.principleFilter == null) return true;

				// otherwise check if upper-case principle filter matches upper-case element name or detail
				if ( element.principleId.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				if ( element.detail.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				if ( element.sectionRef.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}

			function containsScopeConceptFilter(element) {

				// check if scopeConcept filter is empty
				if ($scope.scopeConceptFilter === "" || $scope.scopeConceptFilter == null) return true;

				// otherwise check if upper-case scopeConcept filter matches upper-case element name or detail
				if ( element.scopeConceptId.toString().toUpperCase().indexOf( $scope.scopeConceptFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.scopeConceptFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}		

			function containsScopeExcludedConceptFilter(element) {

				// check if scopeConcept filter is empty
				if ($scope.scopeExcludesConceptFilter === "" || $scope.scopeExcludesConceptFilter == null) return true;

				// otherwise check if upper-case scopeConcept filter matches upper-case element name or detail
				if ( element.scopeExcludesConceptId.toString().toUpperCase().indexOf( $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1) return true;
				if ( element.name.toString().toUpperCase().indexOf( $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1) return true;

				// otherwise return false
				return false;
			}		

			// helper function to sort a JSON array by field

			$scope.sortByKey = function sortById(array, key) {
				return array.sort(function(a, b) {
					var x = a[key]; var y = b[key];
					return ((x < y) ? -1 : ((x > y) ? 1 : 0));
				});
			};



			////////////////////////////////////////////////////////
			// END PRINCIPLE/ADVICE SORTING/FILTERING FUNCTIONS
			////////////////////////////////////////////////////////
		}]);




mapProjectAppControllers
.controller('RecordCreateCtrl', function($scope) {


	$scope.mapEntry =  {
			"mapRecordId": 1,
			"targetId":"testTarget",
			"targetName":"testTargetName",
			"rule":"RULE",
			"mapPriority":"1",
			"relationId":"",
			"relationName":"",
			"mapBlock":"1",
			"mapGroup":"1",
			"mapAdvice":[],
			"mapPrinciples":[]
	};
	console.debug($scope.mapEntry);

});




