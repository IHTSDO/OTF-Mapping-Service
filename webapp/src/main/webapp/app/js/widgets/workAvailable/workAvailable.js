
'use strict';

angular.module('mapProjectApp.widgets.workAvailable', ['adf.provider'])
.config(function(dashboardProvider){
	
	dashboardProvider
	.widget('workAvailable', {
		title: 'Available Concepts',
		description: 'Module to assign work to users',
		controller: 'workAvailableWidgetCtrl',
		templateUrl: 'js/widgets/workAvailable/workAvailable.html',
		resolve: {},
		edit: {}
	});
})

.controller('workAvailableWidgetCtrl', function($scope, $rootScope, $http, $routeParams, $modal, localStorageService){

	// local variables
	$scope.batchSizes = [100, 50, 25, 10, 5];
	$scope.batchSize = $scope.batchSizes[2];
	
	// pagination variables
	$scope.itemsPerPage = 10;
	$scope.availableWorkPage = 1;
	$scope.availableConflictsPage = 1;
	
	// initial titles
	$scope.availableWorkTitle = "Available Concepts";
	$scope.availableConflictsTitle = "Available Conflicts";
	
	// retrieve focus project, current user, and current role
	$scope.focusProject = localStorageService.get('focusProject');
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.isConceptListOpen = false;
	
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

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('my scope project changed!');

		if ($scope.focusProject != null) {
			
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

	$scope.retrieveAvailableConflicts = function(page) {
		console.debug('workAvailableCtrl: Retrieving available work');

		// clear the existing work
		$scope.availableConflicts = [];
			
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "availableConflicts/projectId/" + $scope.focusProject.id + "/user/" + $scope.currentUser.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.availableConflicts = data.searchResult;
			
			// set pagination
			$scope.nAvailableConflicts = data.totalCount;
			$scope.numAvailableConflictsPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			
			// set title
			$scope.availableConflictsTitle = "Available Conflicts (" + data.totalCount + ")";
		}).error(function(error) {
		  	$rootScope.glassPane--;
		});
	};
	

	// get a page of available work
	$scope.retrieveAvailableWork = function(page) {
		console.debug('workAvailableCtrl: Retrieving available work');

		// clear the existing work
		$scope.availableWork = null;
			
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "availableWork/projectId/" + $scope.focusProject.id + "/user/" + $scope.currentUser.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.availableWork = data.searchResult;
			
			// set pagination
			$scope.nAvailableWork = data.totalCount;
			$scope.numAvailableWorkPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			
			// set title
			$scope.availableWorkTitle = "Available Concepts (" + data.totalCount + ")";
			console.debug($scope.numAvailableWorkPages);
			console.debug(data.totalCount);

		}).error(function(error) {
		  	$rootScope.glassPane--;
		});
	};

	
	// assign a single concept to the current user
	$scope.assignWork = function(trackingRecord, mapUser) {
		
		
		if (mapUser == null) mapUser = $scope.currentUser;

	  	$rootScope.glassPane++;
	  	
		$http({
			url: root_workflow + "assign/projectId/" + $scope.focusProject.id +
								 "/concept/" + trackingRecord.terminologyId +
								 "/user/" + $scope.currentUser.userName,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
		  	$rootScope.glassPane--;
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') $scope.availableConflicts.removeElement(trackingRecord);
			$rootScope.$broadcast('workAvailableWidget.notification.assignWork');
			
			$scope.retrieveAvailableWork($scope.trackingRecordPage);
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
				$scope.retrieveAvailableConflicts($scope.availableConflictsPage);
			}
			
		}).error(function(error) {
		  	$rootScope.glassPane--;
		});
		
	   
	};
	
	// assign a batch of records to the current user
	$scope.assignBatch = function(mapUser, batchSize) {
		
		if (mapUser == null) mapUser = $scope.currentUser;
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": ($scope.availableWorkPage-1)*$scope.itemsPerPage,
			 	 	 "maxResults": batchSize, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  

	  	$rootScope.glassPane++;
		$http({
			url: root_workflow + "availableWork/projectId/" + $scope.focusProject.id + "/user/" + mapUser.userName,
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
			
			// if user is viewing concepts , check that first result matches first displayed result
			if ($scope.isConceptListOpen == true && $scope.currentUser.userName != mapUser.userName) {
				for (var i = 0; i < $scope.itemsPerPage; i++) {
					if (trackingRecords[i].id != $scope.availableWork[i].id) {
						retrieveAvailableWork($scope.availableWorkPage);
						alert("One or more of the concepts you are viewing are not in the first available batch.  Please try again.  \n\nTo claim the first available batch, leave the Viewer closed and click 'Claim Batch'");
						$scope.isConceptListOpen = false;
						conceptListValid = false;
					}
				}
			} 
			
			if (conceptListValid == true) {
				console.debug("Claiming batch of size: " + batchSize);
				
				var terminologyIds = [];
				for (var i = 0; i < batchSize; i++) {
					
					terminologyIds.push(trackingRecords[i].terminologyId);
					console.debug('  -> Concept ' + trackingRecords[i].terminologyId);
				}
				
				console.debug("Calling batch assignment API: " + root_workflow + "assign/batch/projectId/" + $scope.focusProject.id 
									   + "/user/" + mapUser.userName);

			  	$rootScope.glassPane++;
				$http({
					url: root_workflow + "assign/batch/projectId/" + $scope.focusProject.id 
									   + "/user/" + mapUser.userName,	
					dataType: "json",
					data: terminologyIds,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
				  	$rootScope.glassPane--;
					$rootScope.$broadcast('workAvailableWidget.notification.assignWork');
					$scope.retrieveAvailableWork(1);				
				}).error(function(data) {
				  	$rootScope.glassPane--;
					console.debug("Could not retrieve available work when assigning batch.");
				});
			} else {
				console.debug("Unexpected error in assigning batch");
			}
		}).error(function(data) {
		  	$rootScope.glassPane--;
			console.debug("Could not retrieve available work when assigning batch.");
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
