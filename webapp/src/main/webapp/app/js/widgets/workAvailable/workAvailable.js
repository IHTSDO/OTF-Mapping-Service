
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

	console.debug('LIST OF USERS:');
	console.debug($scope.mapUsers);
	
	
	
	

	// watch for project change and modify the local variable if necessary
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});
	
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('assignedListWidget.notification.unassignWork', function(event, parameters) { 	
		console.debug("WorkAvailableCtrl:  Detected unassign work notification");
		$scope.retrieveAvailableWork(1);
	});

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('my scope project changed!');

		if ($scope.focusProject != null) {
			$scope.retrieveAvailableWork(1);
		}
	});


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

		$http({
			url: root_workflow + "work/projectId/" + $scope.focusProject.id +
			"/userId/" + $scope.currentUser.id,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$scope.availableWork = data.searchResult;
			$scope.nTrackingRecords = data.totalCount;
		});
	};
	
	// set the pagination variables
	function setPagination(trackingRecordsPerPage, nTrackingRecords) {
		
		$scope.trackingRecordsPerPage = trackingRecordsPerPage;
		$scope.numRecordPages = Math.ceil($scope.nTrackingRecords / trackingRecordsPerPage);
	};
	
	// assign a single concept to the current user
	// TODO Insert check before assignment
	// TODO Implement refresh after successful claim
	$scope.assignWork = function(trackingRecord, mapUser) {

		$http({
			url: root_workflow + "assign/projectId/" + $scope.focusProject.id +
								 "/concept/" + trackingRecord.terminologyId +
								 "/user/" + $scope.currentUser.userName,
			dataType: "json",
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$scope.availableWork.removeElement(trackingRecord);
			$rootScope.$broadcast('availableWork.notification.assignWork',{key: 'assignedWork', assignedWork: data});  
		});
		
	   
	};
	
	// assign a batch of records to the current user
	$scope.assignBatch = function(mapUser, batchSize) {
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": 0,
			 	 	 "maxResults": $scope.batchSize, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  
		
		$http({
			url: root_workflow + "work/projectId/" + $scope.focusProject.id +
			"/userId/" + $scope.currentUser.id,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			
			var trackingRecords = data.searchResult;
			
			// if user is viewing concepts, check that first result matches first displayed result
			if (trackingRecords[0].id != $scope.availableWork[0].id) {
				retrieveAvailableWork(1);
				alert("The work you are viewing has been claimed by another user.  Please try again.  To claim a set of concepts without viewing them, close the concept viewer and request a batch.");
			} else {
				console.debug("Claimed batch:");
				console.debug(trackingRecords);
				
				var terminologyIds = [];
				for (var i = 0; i < trackingRecords.length; i++) {
					terminologyIds.push(trackingRecords[i].terminologyId);
				}
				
				$http({
					url: root_workflow + "assign/batch/projectId/" + $scope.focusProject.id 
									   + "/user/" + $scope.currentUser.userName,	
					dataType: "json",
					data: terminologyIds,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}	
				}).success(function(data) {
					$rootScope.$broadcast('workAvailableWidget.notification.assignWork',
							{key: 'trackingRecords', trackingRecords: data.mapRecord});
				
				});
			}
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
