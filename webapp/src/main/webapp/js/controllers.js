'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap', 'ui.sortable', 'mapProjectAppDirectives', 'mapProjectAppServices', 'mapProjectAppDashboards']);

//var root_url = "${base.url}/mapping-rest/";
var root_url = "/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";
var root_workflow = root_url + "workflow/";
var root_security = root_url + "security/";

mapProjectAppControllers.run(function($rootScope, $http, localStorageService) {
	$rootScope.glassPane = 0;
});


//Navigation
mapProjectAppControllers.controller('LoginCtrl', ['$scope', 'localStorageService', '$rootScope', '$location', '$http',
                                                  function ($scope, localStorageService, $rootScope, $location, $http) {
	$scope.page =  'login';
	$scope.mapUsers = [];
	$scope.userName = '';

	$scope.globalError = $rootScope.globalError;


	// set the user, role, focus project, and preferences to null (i.e. clear) by broadcasting to rest of app
	$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: null});  
	$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: null});  
	$rootScope.$broadcast('localStorageModule.notification.setFocusProject', {key: 'focusProject', focusProject: null});
	$rootScope.$broadcast('localStorageModule.notification.setPreferences', {key: 'preferences', preferences: null});

	$scope.mapProjects = localStorageService.get('mapProjects');
	$scope.mapUsers = localStorageService.get('mapUsers');

	// initial values for pick-list
	$scope.roles = [
	                'Viewer',
	                'Specialist',
	                'Lead',
	                'Administrator'];
	$scope.role = $scope.roles[0];  

	// login button directs to next page based on role selected
	$scope.goGuest = function () {
		$scope.userName = "guest";
		$scope.role = "Viewer";
		$scope.password = "***REMOVED***";
		$scope.go();
	}

	// login button directs to next page based on role selected
	$scope.go = function () {

		// reset the global error on log in attempt
		$scope.globalError = "";

		console.debug($scope.role);

		var path = "";

		// check that user has been selected
		if ($scope.userName == null) {
			alert("You must specify a user");
		} else if ($scope.password == null) {
			alert("You must enter a password");
		} else {


			// authenticate the user
			var query_url = root_security + "authenticate/" + $scope.userName;

			console.debug($scope.userName);

			// turn on the glass pane during login process/authentication
			// turned off at each error stage or before redirecting to dashboards
			$rootScope.glassPane++;

			$http({
				url: query_url,
				dataType: "json",
				data: $scope.password,
				method: "POST",
				headers: {
					"Content-Type": "text/plain"
						// save userToken from authentication
				}}).success(function(data) {
					console.debug(data);

					localStorageService.add('userToken', data);				
					$scope.userToken = localStorageService.get('userToken');

					// set default header to contain userToken
					$http.defaults.headers.common.Authorization = $scope.userToken;

					// retrieve projects
					$http({
						url: root_mapping + "project/projects",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	

					}).success(
							function(data) {
								localStorageService.add('mapProjects', data.mapProject);
								$rootScope.$broadcast(
										'localStorageModule.notification.setMapProjects', {
											key : 'mapProjects',
											mapProjects : data.mapProject
										});

							}).then(function() {



								// retrieve users
								$rootScope.glassPane++;
								$http({
									url: root_mapping + "user/users",
									dataType: "json",
									method: "GET",
									headers: {
										"Content-Type": "application/json"
									}	
								}).success(function(data) {
									$rootScope.glassPane--;
									localStorageService.add('mapUsers', data.mapUser);
									$rootScope.$broadcast('localStorageModule.notification.setMapUsers',{key: 'mapUsers', mapUsers: data.mapUsers});  
									// find the mapUser object
									for (var i = 0; i < $scope.mapUsers.length; i++)  {
										if ($scope.mapUsers[i].userName === $scope.userName) {
											$scope.mapUser = $scope.mapUsers[i];
										}
									}

									// add the user information to local storage
									localStorageService.add('currentUser', $scope.mapUser);

									// broadcast the user information to rest of app
									$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: $scope.mapUser});
								}).error(function(error) {
									$rootScope.glassPane--;
								}).then(function() {


									// retrieve the user preferences
									$http({
										url: root_mapping + "userPreferences/user/id/" + $scope.userName,
										dataType: "json",
										method: "GET",
										headers: {
											"Content-Type": "application/json"
										}	
									}).success(function(data) {
										$rootScope.glassPane--;
										console.debug($scope.mapProjects);
										console.debug(data);
										$scope.preferences = data;
										$scope.preferences.lastLogin = new Date().getTime();
										localStorageService.add('preferences', $scope.preferences);

										// check for a last-visited project
										$scope.focusProject = null;
										for (var i = 0; i < $scope.mapProjects.length; i++)  {
											if ($scope.mapProjects[i].id === $scope.preferences.lastMapProjectId) {
												$scope.focusProject = $scope.mapProjects[i];
											}
										}

										// if project not found, set to first retrieved project
										if ($scope.focusProject == null) {
											$scope.focusProject = $scope.mapProjects[0];
										}


										console.debug('Last project: ');
										console.debug($scope.focusProject);
										localStorageService.add('focusProject', $scope.focusProject);
										localStorageService.add('userPreferences', $scope.preferences);
										$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', preferences: $scope.preferences});
										$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

									}).error(function(error) {
										$rootScope.glassPane--;
										$scope.error = $scope.error + "Could not retrieve user preferences. "; 

									}).then(function(data) {
										$http({
											url: root_mapping + "project/user/id/" + $scope.userName,
											dataType: "json",
											method: "GET",
											headers: {
												"Content-Type": "application/json"
											}	
										}).success(function(data) {
											console.debug(data);
											// check if user has role in focusProject
											var found = 0;
											for (var i = 0; i < data.mapProject.length; i++) {
												if (data.mapProject[i].id === $scope.focusProject.id) {
													found = 1;
												} 
											}

											// otherwise change focusProject
											if (found == 0 && data.mapProject.length > 0) {
												$scope.focusProject = data.mapProject[0];
												console.debug($scope.focusProject);
												localStorageService.add('focusProject', $scope.focusProject);
												$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  			
											}
										}).error(function(error) {
											$rootScope.glassPane--;
											$scope.error = $scope.error + "Could not retrieve user role. "; 
										}).then(function(data) {
											$rootScope.glassPane++;
											$http({
												url: root_mapping + "userRole/user/id/" + $scope.userName + "/project/id/" + $scope.focusProject.id,
												dataType: "json",
												method: "GET",
												headers: {
													"Content-Type": "application/json"
												}	
											}).success(function(data) {
												console.debug(data);
												$scope.role = data.replace(/"/g, '');


												if ($scope.role.toLowerCase() == "specialist") {
													path = "/specialist/dash";
													$scope.role = "Specialist";
												} else if ($scope.role.toLowerCase() == "lead") {
													path = "/lead/dash";
													$scope.role = "Lead";
												} else if ($scope.role.toLowerCase() == "administrator") {
													path = "/admin/dash";
													$scope.role = "Administrator";
												} else  {
													path = "/viewer/dash";
													$scope.role = "Viewer";
												}

												// add the user information to local storage
												localStorageService.add('currentRole', $scope.role);

												// broadcast the user information to rest of app
												$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: $scope.role});

												$rootScope.glassPane--;

												// redirect page
												$location.path(path);


											}).error(function(error) {
												$rootScope.glassPane--;
												$scope.error = error + "Could not retrieve user role. "; 
											});		
										});
									});
								});

							});
				}).error(function(error) {
					$rootScope.glassPane--;
					$scope.error = error.replace(/"/g, '');
				}).then(function(data) {
					$rootScope.glassPane++;
					$http({
						url: root_metadata + "terminology/terminologies/latest",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(response) {
						$rootScope.glassPane--;
						var keyValuePairs = response.keyValuePair;
						for (var i = 0; i < keyValuePairs.length; i++) {
							console.debug("Retrieving metadata for " + keyValuePairs[i].key + ", " + keyValuePairs[i].value);		
							addMetadataToLocalStorageService(keyValuePairs[i].key, keyValuePairs[i].value);
						}
					}).error(function(error) {
						$rootScope.glassPane--;
					}).then(function(data) {


					});
				});

		}



		// function to add metadata to local storage service
		// written to ensure correct handling of asynchronous responses
		function addMetadataToLocalStorageService(terminology, version) {
			$http({
				url: root_metadata + "metadata/terminology/id/" + terminology + "/" + version,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(response) {
				console.debug("Adding metadata for " + terminology);
				localStorageService.add('metadata_' + terminology, response.keyValuePairList);
			});
		}

	};

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
}]);



/*
 * Controller for retrieving and displaying records associated with a concept
 */
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$q', '$routeParams', '$sce', '$rootScope', '$location', 'localStorageService', 
                                                              function ($scope, $http, $q, $routeParams, $sce, $rootScope, $location, localStorageService) {

	// scope variables
	$scope.page =  'concept';
	$scope.error = "";		// initially empty
	$scope.conceptId = $routeParams.conceptId;
	$scope.recordsInProject = [];
	$scope.recordsNotInProject = [];
	$scope.recordsInProjectNotFound = false; // set to true after record retrieval returns no records for focus project


	// local variables
	var projects = localStorageService.get("mapProjects");

	// retrieve cached values
	$scope.focusProject = localStorageService.get("focusProject");
	$scope.mapProjects = localStorageService.get("mapProjects");
	$scope.currentUser = localStorageService.get("currentUser");
	$scope.currentRole = localStorageService.get("currentRole");
	$scope.userPreferences = localStorageService.get("userPreferences");

	// watch for changes to focus project
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("RecordConceptListCtrl:  Detected change in focus project");      
		$scope.focusProject = parameters.focusProject;
		$scope.filterRecords();
	});	

	// once focus project retrieved, retrieve the concept and records
	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'userToken'], function() {

		// need both focus project and user token set before executing main functions
		if ($scope.focusProject != null &&	$scope.userToken != null ) {
			$http.defaults.headers.common.Authorization = $scope.userToken;
			$scope.go();
		}
	});

	$scope.go = function() {

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
		}).error(function(response) {
			$scope.error = $scope.error + "Could not retrieve projects. "; 

			if (response.indexOf("HTTP Status 401") != -1) {
				$rootScope.globalError = "Authorization failed.  Please log in again.";
				$location.path("/");
			}

		}).then(function() {

			// get all records for this concept
			$scope.getRecordsForConcept();
		});


		// find concept based on source terminology
		$http({
			url: root_content + "concept/id/" 
			+ $scope.focusProject.sourceTerminology + "/" 
			+ $scope.focusProject.sourceTerminologyVersion + "/"
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
				url: root_content + "concept/id/" +
				+ $scope.focusProject.sourceTerminology + "/" 
				+ $scope.focusProject.sourceTerminologyVersion + "/"
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
		}).error(function(response) {
			console.debug("Could not retrieve concept");
			$scope.error = $scope.error + "Could not retrieve Concept. ";    

			if (response.indexOf("HTTP Status 401") != -1) {
				$rootScope.globalError = "Authorization failed.  Please log in again.";
				$location.path("/");
			}
		});
	};

	$scope.goProjectDetails = function() {
		console.debug("Redirecting to project details view");
		$location.path("/project/details");
	};

	$scope.goMapRecords = function () {
		console.debug("Redirecting to project records view");
		$location.path("/project/records");
	};

	// function to return trusted html code (for tooltip content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};

	$scope.getRecordsForConcept = function() {
		// retrieve all records with this concept id
		$http({
			url: root_mapping + "record/concept/id/" + $routeParams.conceptId,
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$scope.records = data.mapRecord;
			$scope.filterRecords();
		}).error(function(response) {
			$scope.error = $scope.error + "Could not retrieve records. ";    

			if (response.indexOf("HTTP Status 401") != -1) {
				$rootScope.globalError = "Authorization failed.  Please log in again.";
				$location.path("/");
			}
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

	$scope.displayToViewer = function(record) {
		if ($scope.currentRole === 'Viewer' &&
				record.workflowStatus === 'READY_FOR_PUBLICATION') {
			return false;
		} else return true; 
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

		// for records in project, check if this user can edit these records
		console.debug($scope.recordsInProject);
		for (var i = 0; i < $scope.recordsInProject.length; i++) {

			setEditable($scope.recordsInProject[i]);
		}

		// if no records for this project found, set flag
		if ($scope.recordsInProject.length == 0) $scope.recordsInProjectNotFound = true;
	};

	function setEditable(record) {

		$http({
			url: root_workflow + "checkRecordEditable/user/id/" + $scope.currentUser.userName,
			method: "POST",
			dataType: 'json',
			data: record,
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(response) {
			record.isEditable = response;
		});
	};

	$scope.editRecord = function(record) {

		// assign the record along the FIX_ERROR_PATH
		$rootScope.glassPane++;

		console.debug("Edit record clicked, assigning record if necessary");
		$http({
			url: root_workflow + "assignFromRecord/user/id/" + $scope.currentUser.userName,
			method: "POST",
			dataType: 'json',
			data: record,
			headers: {
				"Content-Type": "application/json"
			}		
		}).success(function(data) {
			console.debug('Assignment successful');
			$http({
				url: root_workflow + "record/project/id/" + $scope.focusProject.id +
				"/concept/id/" + record.conceptId +
				"/user/id/" + $scope.currentUser.userName,
				method: "GET",
				dataType: 'json',
				data: record,
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				console.debug(data);
				$rootScope.glassPane--;

				// open the record edit view
				$location.path("/record/recordId/" + data.id);
			}).error(function(response) {
				$rootScope.glassPane--;

				if (response.indexOf("HTTP Status 401") != -1) {
					$rootScope.globalError = "Authorization failed.  Please log in again.";
					$location.path("/");
				}
			});


		}).error(function(error) {
			$rootScope.glassPane--;
		});
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
			url: root_mapping + "concept/id" 
			+ $scope.concept.terminology + "/"
			+ $scope.concept.terminologyVersion + "/"
			+ $scope.concept.terminologyId + "/"
			+ "unmappedDescendants/threshold/10",
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


	// change the focus project to the project associated with a specified record
	$scope.changeFocusProjectByRecord = function(record) {

		console.debug("changeFocusProjectByRecord:  record project id = " + record.mapProjectId);

		console.debug($scope.mapProjects);
		for (var i = 0; i < $scope.mapProjects.length; i++) {
			console.debug("  comparing to project id = " + $scope.mapProjects[i].id);
			if ($scope.mapProjects[i].id = record.mapProjectId) {

				$scope.changeFocusProject($scope.mapProjects[i]);
				break;
			}
		}
	};


	// function to change project from the header
	$scope.changeFocusProject = function(mapProject) {
		$scope.focusProject = mapProject;
		console.debug("changing project to " + $scope.focusProject.name);

		// update and broadcast the new focus project
		localStorageService.add('focusProject', $scope.focusProject);
		$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

		// update the user preferences
		$scope.userPreferences.lastMapProjectId = $scope.focusProject.id;
		localStorageService.add('preferences', $scope.preferences);
		$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', userPreferences: $scope.userPreferences});

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

	$scope.page =  'records';

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

	// retrieve the current global variables
	$scope.focusProject = localStorageService.get('focusProject');
	$scope.mapProjects = localStorageService.get("mapProjects");
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');

	// once focus project retrieved, retrieve the concept and records
	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'userToken'], function() {

		// need both focus project and user token set before executing main functions
		if ($scope.focusProject != null && $scope.userToken != null) {
			$http.defaults.headers.common.Authorization = $scope.userToken;
			$scope.projectId = $scope.focusProject.id;
			$scope.getRecordsForProject();
		}
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

		// construct html parameters parameter
		var pfsParameterObj = constructPfsParameterObj(page);
		var query_url = root_mapping + "record/project/id/" + $scope.project.objectId;


		$rootScope.glassPane++;

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

			$rootScope.glassPane--;
			$scope.records = data.mapRecord;
			$scope.statusRecordLoad = "";

			// set pagination variables
			$scope.nRecords = data.totalCount;
			$scope.numRecordPages = Math.ceil(data.totalCount / $scope.recordsPerPage);

		}).error(function(response) {

			$rootScope.glassPane--;
			$scope.errorRecord = "Error retrieving map records";
			console.debug("changeRecordPage error");

			if (response.indexOf("HTTP Status 401") != -1) {
				$rootScope.globalError = "Authorization failed.  Please log in again.";
				$location.path("/");
			}
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
			"queryRestriction": $scope.query == null ? null : $scope.query};  // assigning simply to $scope.query when null produces undefined

	}

	function getUnmappedDescendants(index) {

		// before processing this record, make call to start next async request
		if (index < $scope.records.length-1) {
			getUnmappedDescendants(index+1);
		}

		$scope.records[index].unmappedDescendants = [];

		// if descendants below threshold for lower-level concept, check for unmapped
		if ($scope.records[index].countDescendantConcepts < 11) {

			$http({
				url: root_mapping + "concept/id/" 
				+ $scope.project.sourceTerminology + "/"
				+ $scope.project.sourceTerminologyVersion + "/"
				+ $scope.records[index].conceptId + "/"
				+ "unmappedDescendants/threshold/10",
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

	$scope.isEditable = function(record) {

		if (($scope.currentRole === 'Specialist' ||
				$scope.currentRole === 'Lead' ||
				$scope.currentRole === 'Admin') &&
				(record.workflowStatus === 'PUBLISHED' || record.workflowStatus === 'READY_FOR_PUBLICATION')) {

			return true;

		} else if ($scope.currentUser.userName === record.owner.userName) {
			return true;
		} else return false;
	};

	$scope.editRecord = function(record) {

		// assign the record along the FIX_ERROR_PATH
		$rootScope.glassPane++;

		console.debug("Edit record clicked, assigning record if necessary");
		$http({
			url: root_workflow + "assignFromRecord/user/id/" + $scope.currentUser.userName,
			method: "POST",
			dataType: 'json',
			data: record,
			headers: {
				"Content-Type": "application/json"
			}		
		}).success(function(data) {
			console.debug('Assignment successful');
			$http({
				url: root_workflow + "record/project/id/" + $scope.focusProject.id +
				"/concept/id/" + record.conceptId +
				"/user/id/" + $scope.currentUser.userName,
				method: "GET",
				dataType: 'json',
				data: record,
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {

				$rootScope.glassPane--;

				// open the record edit view
				$location.path("/record/recordId/" + data.id);
			});


		}).error(function(response) {
			$rootScope.glassPane--;

			if (response.indexOf("HTTP Status 401") != -1) {
				$rootScope.globalError = "Authorization failed.  Please log in again.";
				$location.path("/");
			}
		});
	};
}]);


