
'use strict';

angular.module('mapProjectApp.widgets.workAvailable', ['adf.provider'])
.config(function(dashboardProvider){
	
	dashboardProvider
	.widget('workAvailable', {
		title: 'Concepts',
		description: 'Module to assign work to users',
		controller: 'workAvailableWidgetCtrl',
		templateUrl: 'js/widgets/workAvailable/workAvailable.html',
		resolve: {},
		edit: {}
	});
})

.controller('workAvailableWidgetCtrl', function($scope, $rootScope, $http, $routeParams, $modal, $location, localStorageService){

	// local variables
	$scope.batchSizes = [100, 50, 25, 10, 5];
	$scope.batchSize = $scope.batchSizes[2];
	$scope.batchSizeConflict = $scope.batchSizes[4];
	
	// pagination variables
	$scope.itemsPerPage = 10;
	$scope.availableWorkPage = 1;
	$scope.availableConflictsPage = 1;
	
	// initial titles
	$scope.availableWorkTitle = "Concepts";
	$scope.availableConflictsTitle = "Conflicts";
	
	// retrieve focus project, current user, and current role
	$scope.focusProject = localStorageService.get('focusProject');
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.isConceptListOpen = false;
	$scope.queryAvailable = null;
	
	// intiialize the user list
	$scope.mapUsers = {};

	// watch for project change and modify the local variable if necessary
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});
	
	// on unassign notification, refresh the available work widget
	$scope.$on('assignedListWidget.notification.unassignWork', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected unassign work notification");
		$scope.retrieveAvailableWork($scope.availableWorkPage);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
			$scope.retrieveAvailableConflicts($scope.availableConflictsPage);
		}
	});
	
	// on computation of workflow, refresh the available work widget
	$scope.$on('mapProjectWidget.notification.workflowComputed', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected recomputation of workflow");
		$scope.retrieveAvailableWork($scope.availableWorkPage);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
			$scope.retrieveAvailableConflicts($scope.availableConflictsPage);
		}
	});
	
	// on retrieval, set the user drop-down lists to the current user
	$scope.$watch(['currentUser'], function () {
		console.debug('user changed');
		$scope.assignedMapUser = $scope.currentUser;
		$scope.assignedMapLead = $scope.currentUser;
	});

	// on any change of focusProject, retrieve new available work
	$scope.userToken = localStorageService.get('userToken');
	
	// on retrieval of either focus project or user token, try to retrieve work
	$scope.$watch(['focusProject', 'userToken'], function() {
		console.debug('workAvailableWidget:  scope project changed!');

		// both variables must be non-null
		if ($scope.focusProject != null && $scope.userToken != null) {
			
			$http.defaults.headers.common.Authorization = $scope.userToken;

			
			// construct the list of users
			$scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
			console.debug('Project Users:');
			console.debug($scope.projectUsers);
			
			$scope.retrieveAvailableWork($scope.availableWorkPage);
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
				$scope.retrieveAvailableConflicts($scope.availableConflictsPage);
			}
		}
	});

	$scope.retrieveAvailableConflicts = function(page, query, user) {
		console.debug('workAvailableCtrl: Retrieving available work');
		
		// clear local conflict error message
		$scope.errorConflict = null;

		// if user not supplied, assume current user
		if (user == null || user == undefined) user = $scope.currentUser;
		
		// clear the existing work
		$scope.availableConflicts = null;
		
		// set query to null if undefined
		if (query == undefined) query = null;
		
		// if null query, reset the search field
		if (query == null) $scope.queryAvailable = null;
			
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ user.userName
			+ "/query/" + (query == null ? "null" : query)
			+ "/availableConflicts",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
		  	$rootScope.glassPane--;
		  	
		  	console.debug("Retrieve conflicts", data);

			$scope.availableConflicts = data.searchResult;
			
			// set pagination
			$scope.nAvailableConflicts = data.totalCount;
			$scope.numAvailableConflictsPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			
			// set title
			$scope.availableConflictsTitle = "Conflicts (" + data.totalCount + ")";
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);
		});
	} ;
	

	// get a page of available work
	$scope.retrieveAvailableWork = function(page, query, user) {
		console.debug('workAvailableCtrl: Retrieving available work');
		
		// clear local error
		$scope.error = null;

		// if user not supplied, assume current user
		if (user == null || user == undefined) user = $scope.currentUser;
		
		// clear the existing work
		$scope.availableWork = null;
		
		// set query to null if undefined
		if (query == undefined) query = null;
		
		// if null query, reset the search field
		if (query == null) $scope.queryAvailable = null;
			
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": null}; 
		

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ user.userName
			+ "/query/" + (query == null ? 'null' : query)
			+ "/availableConcepts",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
		  	$rootScope.glassPane--;
		  	
		  	// open/close the concept list based on whether a query is used
		  	if (query === 'null') {
		  		console.debug('closing concept list');
		  		$scope.isConceptListOpen = false;
		  	} else  {
		  		console.debug('opening concept list');
		  		$scope.isConceptListOpen = true;
		  	}

			$scope.availableWork = data.searchResult;
			
			// set pagination
			$scope.nAvailableWork = data.totalCount;
			$scope.numAvailableWorkPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			
			// set title
			$scope.availableWorkTitle = "Concepts (" + data.totalCount + ")";
			console.debug($scope.numAvailableWorkPages);
			$scope.availableCount = data.totalCount;
			console.debug(data.totalCount);

		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};

	
	// assign a single concept to the current user
	// query passed in to ensure correct retrieval of work
	$scope.assignWork = function(trackingRecord, mapUser, query, workType) {
		
		console.debug('assignWork called');
		console.debug(trackingRecord);
		console.debug(mapUser);
		console.debug(query);
		console.debug(workType)
		;
		// doublecheck map user and query, assign default values if necessary
		if (mapUser == null) mapUser = $scope.currentUser;
		if (query == undefined) query = null;

	  	$rootScope.glassPane++;
	  	
		$http({
			url: root_workflow + "assign/project/id/" + $scope.focusProject.id +
								 "/concept/id/" + trackingRecord.terminologyId +
								 "/user/id/" + mapUser.userName,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
		  	$rootScope.glassPane--;
			$rootScope.$broadcast('workAvailableWidget.notification.assignWork', { assignUser: mapUser.userName, assignType: workType});
			
			if (workType == 'concept') {
				$scope.retrieveAvailableWork($scope.trackingRecordPage, query);
			} else if (workType === 'conflict') {
				$scope.retrieveAvailableConflicts($scope.availableConflictsPage, query);
			}
			
			
			
			
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
		
	   
	};
	
	// assign a batch of records to the current user
	$scope.assignBatch = function(mapUser, batchSize, query) {
		
		// set query to null string if not provided
		if (query == undefined) query == null;
		
		if (mapUser == null || mapUser == undefined) {
			$scope.error = "Work recipient must be selected from list.";
			return;	
		};
		
		if (batchSize > $scope.availableCount) {
			$scope.error = "Batch size is greater than available number of concepts.";
			return;
		} else {
			$scope.error = null;
		}
				
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": ($scope.availableWorkPage-1)*$scope.itemsPerPage,
			 	 	 "maxResults": batchSize, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": null};  

	  	$rootScope.glassPane++;
		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ mapUser.userName 
			+ "/query/" + (query == null ? 'null' : query)
			+ "/availableConcepts",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {

		  	$rootScope.glassPane--;
			console.debug("Claim batch:  Checking against viewed concepts");
			
			var trackingRecords = data.searchResult;
			var conceptListValid = true;
				
			console.debug(trackingRecords);
			console.debug($scope.availableWork);
			
			// if user is assigning to self, check that first result matches first displayed result
			if ($scope.currentUser.userName === mapUser.userName) {
				for (var i = 0; i < $scope.itemsPerPage && i < batchSize; i++) {
					console.debug(trackingRecords[i]);
					console.debug($scope.availableWork[i]);
					if (trackingRecords[i].id != $scope.availableWork[i].id) {
						retrieveAvailableWork($scope.availableWorkPage, query);
						alert("One or more of the concepts you are viewing are not in the first available batch.  Please try again.  \n\nTo claim the first available batch, leave the Viewer closed and click 'Claim Batch'");
						conceptListValid = false;
					}
				}
			} 
			
			if (conceptListValid == true) {
				console.debug("Claiming batch of size: " + batchSize);
				
				var terminologyIds = [];
				for (var i = 0; i < trackingRecords.length; i++) {
					
					terminologyIds.push(trackingRecords[i].terminologyId);
					console.debug('  -> Concept ' + trackingRecords[i].terminologyId);
				}
				
				console.debug("Calling batch assignment API");

			  	$rootScope.glassPane++;
				$http({
					url: root_workflow + "assignBatch/project/id/" + $scope.focusProject.id 
									   + "/user/id/" + mapUser.userName,	
					dataType: "json",
					data: terminologyIds,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
				  	$rootScope.glassPane--;

				  	// notify other widgets of work assignment
					$rootScope.$broadcast('workAvailableWidget.notification.assignWork', {assignUser: mapUser.userName, assignType: 'concept'});
					
					// refresh the available work list
					$scope.retrieveAvailableWork(1, query, mapUser);				
				}).error(function(data, status, headers, config) {
				  	$rootScope.glassPane--;

				    $rootScope.handleHttpError(data, status, headers, config);
					console.debug("Could not retrieve available work when assigning batch.");
				});
			} else {
				console.debug("Unexpected error in assigning batch");
			}
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);	
		});
				
			
		   
	};
	
	// assign a batch of records to the current user
	$scope.assignBatchConflict = function(mapUser, batchSize, query) {
		
		// set query to null string if not provided
		if (query == undefined) query == null;
		
		if (mapUser == null || mapUser == undefined) {
			$scope.errorConflict = "Work recipient must be selected from list.";
			return;	
		};
		
		if (batchSize > $scope.nAvailableConflicts) {
			$scope.errorConflict = "Batch size is greater than available number of conflicts.";
			return;
		} else {
			$scope.errorConflict = null;
		}
				
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": ($scope.availableWorkPage-1)*$scope.itemsPerPage,
			 	 	 "maxResults": batchSize, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": null};  

	  	$rootScope.glassPane++;
		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ mapUser.userName 
			+ "/query/" + (query == null ? 'null' : query)
			+ "/availableConflicts",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {

		  	$rootScope.glassPane--;
			console.debug("Claim batch:  Checking against viewed conflicts");
			
			var trackingRecords = data.searchResult;
			var conceptListValid = true;
			
			
			console.debug(trackingRecords);
			console.debug($scope.availableConflicts);
			
			// if user is viewing conflicts, confirm that the returned batch matches the displayed conflicts
			if ($scope.currentUser.userName === mapUser.userName) {
				for (var i = 0; i < $scope.itemsPerPage && i < batchSize && i < $scope.availableConflicts; i++) {
					console.debug(trackingRecords[i]);
					console.debug($scope.availableWork[i]);
					if (trackingRecords[i].id != $scope.availableWork[i].id) {
						retrieveAvailableWork($scope.availableWorkPage, query);
						alert("One or more of the conflicts you are viewing are not in the first available batch.  Please try again.  \n\nTo claim the first available batch, leave the Viewer closed and click 'Claim Batch'");
						$scope.isConceptListOpen = false;
						conceptListValid = false;
					}
				}
			} 
			
			if (conceptListValid == true) {
				console.debug("Claiming conflict batch of size: " + batchSize);
				
				var terminologyIds = [];
				for (var i = 0; i < trackingRecords.length; i++) {
					
					terminologyIds.push(trackingRecords[i].terminologyId);
					console.debug('  -> Conflict ' + trackingRecords[i].terminologyId);
				}
				
				console.debug("Calling batch assignment API");

			  	$rootScope.glassPane++;
				$http({
					url: root_workflow + "assignBatch/project/id/" + $scope.focusProject.id 
									   + "/user/id/" + mapUser.userName,	
					dataType: "json",
					data: terminologyIds,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
				  	$rootScope.glassPane--;
				  	
				  	// broadcast the work assignment
					$rootScope.$broadcast('workAvailableWidget.notification.assignWork', {assignUser: mapUser.userName, assignType: 'conflict'});
					
					// refresh the displayed list of conflicts
					$scope.retrieveAvailableConflicts(1, query, mapUser);				
				}).error(function(data, status, headers, config) {
				  	$rootScope.glassPane--;

				    $rootScope.handleHttpError(data, status, headers, config);
					console.debug("Could not retrieve available work when assigning batch.");
				});
			} else {
				console.debug("Unexpected error in assigning batch");
			}
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);	
		});
				
			
		   
	};

	// remove an element from an array by key
	Array.prototype.removeElement = function(elem) {

		// field to switch on
		var idType = 'id';

		var array = new Array();
		$.map(this, function(v,i){
			if (v[idType] != elem[idType]) array.push(v);
		});

		this.length = 0; //clear original array
		this.push.apply(this, array); //push all elements except the one we want to delete
	};
	
	
	// sort and return an array by string key
	function sortByKey(array, key) {
		return array.sort(function(a, b) {
			var x = a[key]; var y = b[key];
			return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
	};
	

});
