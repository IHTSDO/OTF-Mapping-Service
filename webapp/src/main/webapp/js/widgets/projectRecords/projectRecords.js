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

'use strict';

angular.module('mapProjectApp.widgets.projectRecords', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('projectRecords', {
		title: 'Project Records',
		description: 'Displays map records for a map project.',
		controller: 'projectRecordsCtrl',
		templateUrl: 'js/widgets/projectRecords/projectRecords.html',
		edit: {}
	});
})
.controller('projectRecordsCtrl', function($scope, $rootScope, $http, $routeParams, $location, localStorageService, $sce){

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
	$scope.recordPage = 1;

	// for collapse directive
	$scope.isCollapsed = true;

	// watch for changes to focus project
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("ProjectRecordCtrl:  Detected change in focus project");      
		$scope.focusProject = parameters.focusProject;
		if ($scope.userToken != null) $scope.getRecordsForProject();
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

		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
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
			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);
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
			}).error(function(data, status, headers, config) {
			    $rootScope.glassPane--;

			    $rootScope.handleHttpError(data, status, headers, config);
			});
			
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};

	$scope.truncate = function(string, length) {
		if (length == null) length = 100;
		if (string.length > length) return string.slice(0, length-3);
		else return string;
	};

	$scope.truncated = function(string, length) {
		if (length == null) length = 100;
		if (string.length > length) 
			return true;
		else 
			return false;
	};
});
