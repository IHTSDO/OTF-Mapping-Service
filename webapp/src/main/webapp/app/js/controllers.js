'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap', 'mapProjectAppDirectives', 'mapProjectAppServices', 'mapProjectAppDashboards']);

var root_url = "${base.url}/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";
var root_validation = root_url + "validation/";
var root_workflow = root_url + "workflow/";

mapProjectAppControllers.run(function($rootScope, $http, localStorageService) {
	$rootScope.glassPane = 0;
	
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
			console.debug("Retrieving metadata for " + keyValuePairs[i].key + ", " + keyValuePairs[i].value);		
			addMetadataToLocalStorageService(keyValuePairs[i].key, keyValuePairs[i].value);
				}
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
		localStorageService.add('mapUsers', data.mapUser);
		$rootScope.$broadcast('localStorageModule.notification.setMapUsers',{key: 'mapUsers', mapUsers: data.mapUsers});  

	});

	// function to add metadata to local storage service
	// written to ensure correct handling of asynchronous responses
	function addMetadataToLocalStorageService(terminology, version) {
		$http({
			url: root_metadata + "all/" + terminology + "/" + version,
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

});



//Navigation
mapProjectAppControllers.controller('LoginCtrl', ['$scope', 'localStorageService', '$rootScope', '$location', '$http',
                                                  function ($scope, localStorageService, $rootScope, $location, $http) {
    $scope.page =  'login';
		
	
	// set the user, role, focus project, and preferences to null (i.e. clear) by broadcasting to rest of app
	$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: null});  
	$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: null});  
	$rootScope.$broadcast('localStorageModule.notification.setFocusProject', {key: 'focusProject', focusProject: null});
	$rootScope.$broadcast('localStorageModule.notificatoin.setPreferences', {key: 'preferences', preferences: null});

	// explicitly retrieve the available users and projects to guarantee availability on first visit
	$http({
		url: root_mapping + "project/projects",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(response) {
		$scope.mapProjects = response.mapProject;
			});

	// retrieve users
	$http({
		url: root_mapping + "user/users",
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}	
	}).success(function(response) {
		$scope.mapUsers = response.mapUser;
	});

	// initial values for pick-list
	$scope.roles = [
	                'Viewer',
	                'Specialist',
	                'Lead',
	                'Administrator'];
	$scope.role = $scope.roles[0];  
	
	// login button directs to next page based on role selected
	$scope.go = function () {

		console.debug($scope.role);

		var path = "";

		if ($scope.role == "Specialist") {
			path = "/specialist/dash";
		} else if ($scope.role == "Lead") {
			path = "/lead/dash";
		} else if ($scope.role == "Administrator") {
			path = "/admin/dash";
		} else if ($scope.role == "Viewer") {
			path = "/viewer/dash";
		}

		// check that user has been selected
		if ($scope.mapUser == null) {
			alert("You must specify a user");
		} else if ($scope.role == null) {
			alert("You must specify a role");
		} else {

			// retrieve the user preferences
			$http({
				url: root_mapping + "userPreferences/" + $scope.mapUser.userName,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}	
			}).success(function(data) {
				console.debug($scope.mapProjects);
				console.debug(data);
				$scope.preferences = data;
				$scope.preferences.lastLogin = new Date().getTime();
				localStorageService.add('preferences', $scope.preferences);
				for (var i = 0; i < $scope.mapProjects.length; i++)  {
					if ($scope.mapProjects[i].id === $scope.preferences.lastMapProjectId) {
						$scope.focusProject = $scope.mapProjects[i];
					}
				}
				console.debug('Last project: ');
				console.debug($scope.focusProject);
				localStorageService.add('focusProject', $scope.focusProject);
				$rootScope.$broadcast('localStorageModule.notification.setPreferences', {key: 'preferences', preferences: $scope.preferences});
				$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  
			});

			// add the user information to local storage
			localStorageService.add('currentUser', $scope.mapUser);
			localStorageService.add('currentRole', $scope.role);

			// broadcast the user information to rest of app
			$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: $scope.mapUser});
			$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: $scope.role});

			// redirect page
			$location.path(path);
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
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams', '$sce', '$rootScope', '$location', 'localStorageService', 
                                                              function ($scope, $http, $routeParams, $sce, $rootScope, $location, localStorageService) {

	// scope variables
	$scope.page =  'concept';
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
	
	$scope.displayToViewer = function(record) {

		console.debug('displayToViewer');
		console.debug($scope.currentRole);
		console.debug($scope.currentUser);
		console.debug(record.owner);
		if ($scope.currentRole === 'Viewer' &&
				record.workflowStatus === 'READY_FOR_PUBLICATION') {
			return false;
		} else return true; 
	};

	$scope.editRecord = function(record) {

		// assign the record along the FIX_ERROR_PATH
		$rootScope.glassPane++;

		$http({
			url: root_workflow + "assign/projectId/" + $scope.focusProject.id +
			"/concept/" + record.conceptId +
			"/user/" + $scope.currentUser.userName,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$rootScope.glassPane--;

			// open the record edit view
			window.location("index.html#/record/recordId/" + record.id);
		}).error(function(error) {
			$rootScope.glassPane--;
		});
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

			// set pagination variables
			$scope.nRecords = data.totalCount;
			$scope.numRecordPages = Math.ceil(data.totalCount / $scope.recordsPerPage);

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

	$scope.editRecord = function(record) {

		// assign the record along the FIX_ERROR_PATH
		$rootScope.glassPane++;

		$http({
			url: root_workflow + "assign/projectId/" + $scope.focusProject.id +
			 "/concept/" + record.conceptId +
			 "/user/" + $scope.currentUser.userName,
			 method: "POST",
			 headers: {
				 "Content-Type": "application/json"
			 }	
		}).success(function(data) {
			$rootScope.glassPane--;

			// open the record edit view
			window.location("index.html#/record/recordId/" + record.id);
		}).error(function(error) {
		  	$rootScope.glassPane--;
		});
	};
}]);

mapProjectAppControllers.controller('MapProjectDetailCtrl', 
		['$scope', '$http', '$sce', '$rootScope', '$location', 'localStorageService',
		 function ($scope, $http, $sce, $rootScope, $location, localStorageService) {

		    $scope.page =  'project';

			$scope.currentRole = localStorageService.get('currentRole');
			$scope.currentUser = localStorageService.get('currentUser');
			$scope.focusProject = localStorageService.get('focusProject');
			$scope.mapProjects = localStorageService.get("mapProjects");
			
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
				//if ( element.detail.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
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
			
			$scope.isEmailViewable = function(email) {
				console.debug('isEmailViewable');
				if (email.indexOf("ihtsdo.org") > -1) {
					return true;
				} else
					return false;
			};
		}]);

