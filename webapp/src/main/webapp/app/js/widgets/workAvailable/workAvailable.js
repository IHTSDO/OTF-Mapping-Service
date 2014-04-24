
'use strict';

angular.module('mapProjectApp.widgets.workAvailable', ['adf.provider'])
.config(function(dashboardProvider){
	
	dashboardProvider
	.widget('workAvailable', {
		title: 'Available Work',
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
	$scope.conceptsPerPage = 10;

	// retrieve focus project, current user, and user list
	$scope.focusProject = localStorageService.get('focusProject');
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.mapUsers = localStorageService.get('mapUsers');
	$scope.isConceptListOpen = false;

	console.debug('LIST OF USERS:');
	console.debug($scope.mapUsers);
	
	
	
	

	// watch for project change and modify the local variable if necessary
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});
	
	// on unassign notification, refresh the available work widget
	$scope.$on('assignedListWidget.notification.unassignWork', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected unassign work notification");
		$scope.retrieveAvailableWork(1);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
			$scope.retrieveAvailableConflicts(1);
		}
	});
	
	// on unassign notification, refresh the available work widget
	$scope.$on('mapProjectWidget.notification.workflowComputed', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected recomputation of workflow");
		$scope.retrieveAvailableWork(1);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
			$scope.retrieveAvailableConflicts(1);
		}
	});

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('my scope project changed!');

		if ($scope.focusProject != null) {
			$scope.retrieveAvailableWork(1);
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
				$scope.retrieveAvailableConflicts(1);
			}
		}
	});

	$scope.retrieveAvailableConflicts = function(page) {
		console.debug('workAvailableCtrl: Retrieving available work');

		// clear the existing work
		$scope.availableConflicts = [];
			
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.conceptsPerPage,
			 	 	 "maxResults": $scope.conceptsPerPage, 
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
			$scope.nAvailableConflicts = data.totalCount;
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
					{"startIndex": (page-1)*$scope.conceptsPerPage,
			 	 	 "maxResults": $scope.conceptsPerPage, 
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
			$scope.nTrackingRecords = data.totalCount;
		}).error(function(error) {
		  	$rootScope.glassPane--;
		});
	};
	
	// set the pagination variables
	function setPagination(trackingRecordsPerPage, nTrackingRecords) {
		
		$scope.trackingRecordsPerPage = trackingRecordsPerPage;
		$scope.numRecordPages = Math.ceil($scope.nTrackingRecords / trackingRecordsPerPage);

	
		$scope.numAvailableConflictsPages = Math.ceil($scope.nAvailableConflicts / trackingRecordsPerPage);
	};
	
	// assign a single concept to the current user
	// TODO Insert check before assignment
	// TODO Implement refresh after successful claim
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
			$scope.availableWork.removeElement(trackingRecord);
			$rootScope.$broadcast('workAvailableWidget.notification.assignWork',{key: 'assignedWork', assignedWork: data});  
			$rootScope.$broadcast('availableWork.notification.assignWork',{key: 'assignedWork', assignedWork: data});  
		}).error(function(error) {
		  	$rootScope.glassPane--;
		});
		
	   
	};
	
	// assign a batch of records to the current user
	$scope.assignBatch = function(mapUser, batchSize) {
		
		if (mapUser == null) mapUser = $scope.currentUser;
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": 0,
			 	 	 "maxResults": $scope.batchSize, 
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
				for (var i = 0; i < $scope.trackingRecordPerPage; i++) {
					if (trackingRecords[i].id != $scope.availableWork[i].id) {
						retrieveAvailableWork(1);
						alert("One or more of the concepts you are viewing are not in the first available batch.  Please try again.  \n\nTo claim the first available batch, leave the Viewer closed and click 'Claim Batch'");
						$scope.isConceptListOpen = false;
						conceptListValid = false;
					}
				}
			} 
			
			if (conceptListValid == true) {
				console.debug("Claimed batch:");
				
				var terminologyIds = [];
				for (var i = 0; i < $scope.batchSize; i++) {
					console.debug(trackingRecords[i]);
					
					terminologyIds.push(trackingRecords[i].terminologyId);
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
					$rootScope.$broadcast('workAvailableWidget.notification.assignWork',
							{key: 'trackingRecords', trackingRecords: null}); // TODO: This used to pass actual tracking records, but model structure changed.  Need to bring in line.  Currently using the notification to retrieve assigned work in AssignedList widget
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
