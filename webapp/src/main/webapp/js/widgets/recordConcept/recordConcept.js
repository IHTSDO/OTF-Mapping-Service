

'use strict';

angular.module('mapProjectApp.widgets.recordConcept', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('recordConcept', {
		title: 'Record Concept',
		description: 'Displays concept for map record.',
		controller: 'recordConceptCtrl',
		templateUrl: 'js/widgets/recordConcept/recordConcept.html',
		edit: {}
	});
})
.controller('recordConceptCtrl', function($scope, $rootScope, $http, $routeParams, $location, localStorageService, $sce){


	// scope variables
	$scope.page =  'concept';
	$scope.error = "";		// initially empty
	$scope.conceptId = $routeParams.conceptId;
	$scope.recordsInProject = [];
	$scope.recordsNotInProject = [];
	$scope.recordsInProjectNotFound = false; // set to true after record retrieval returns no records for focus project
	
	$scope.focusProject = null;
	$scope.mapProjects = null;

	// retrieve cached values
	$scope.focusProject = localStorageService.get("focusProject");
	$scope.mapProjects = localStorageService.get("mapProjects");
	$scope.currentUser = localStorageService.get("currentUser");
	$scope.currentRole = localStorageService.get("currentRole");
	$scope.preferences = localStorageService.get("preferences");

	// watch for changes to focus project
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("RecordConceptListCtrl:  Detected change in focus project");      
		$scope.focusProject = parameters.focusProject;
		$scope.filterRecords();
	});	

	// once focus project retrieved, retrieve the concept and records
	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'userToken', 'mapProjects'], function() {
		
		// need both focus project and user token set before executing main functions
		if ($scope.focusProject != null &&	$scope.userToken != null && $scope.mapProjects != null) {
			$http.defaults.headers.common.Authorization = $scope.userToken;
			console.debug($scope.mapProjects);
			$scope.go();
		}
	});

	$scope.go = function() {

		$scope.recordsInProjectNotFound = false;

		console.debug("RecordConceptCtrl:  Focus Project change");
		
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
			setTitle($scope.focusProject.sourceTerminology, $routeParams.conceptId, 
					$scope.concept.defaultPreferredName);
			$scope.getRecordsForConcept();
			$scope.findUnmappedDescendants();

			// find children based on source terminology
			$http({
				url: root_content + "concept/id/"
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

			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);
			});
		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
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
		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
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
		if ($scope.recordsInProject.length == 0) {
			$scope.recordsInProjectNotFound = true;
		} else {
			$scope.recordsInProjectNotFound = false;			
		}
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
		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
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
			}).error(function(data, status, headers, config) {
			    $rootScope.glassPane--;

			    $rootScope.handleHttpError(data, status, headers, config);
			});

			
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	

	$scope.getProject = function(record) {
		for (var i = 0; i < $scope.mapProjects.length; i++) {
			if ($scope.mapProjects[i].id == record.mapProjectId) {
				return $scope.mapProjects[i];
			}
		}
		return null;
	};

	$scope.getProjectFromName = function(name) {
		for (var i = 0; i < $scope.mapProjects.length; i++) {
			if ($scope.mapProjects[i].name === name) {
				return $scope.mapProjects[i];
			}
		}
		return null;
	};

	$scope.getProjectName = function(record) {

		for (var i = 0; i < $scope.mapProjects.length; i++) {
			if ($scope.mapProjects[i].id == record.mapProjectId) {
				return $scope.mapProjects[i].name;
			}
		}
		return null;
	};

	$scope.findUnmappedDescendants = function() {


		$http({
			url: root_mapping + "concept/id/" 
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
		}).error(function(data, status, headers, config) {

		    $rootScope.handleHttpError(data, status, headers, config);
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
		$scope.preferences.lastMapProjectId = $scope.focusProject.id;
		localStorageService.add('preferences', $scope.preferences);
		$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', userPreferences: $scope.preferences});

		$http({
			url: root_mapping + "userPreferences/update",
			dataType: "json",
			data: $scope.preferences,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
		}).error(function(data) {
			if (response.indexOf("HTTP Status 401") != -1) {
				$rootScope.globalError = "Authorization failed.  Please log in again.";
				$location.path("/");
			}
		});
		
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
	
	function setTitle(terminology, conceptId, defaultPreferredName) {
		$scope.model.title = terminology + " Concept " + conceptId + ": " + defaultPreferredName;
	};

});
