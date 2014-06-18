
'use strict';

angular.module('mapProjectApp.widgets.assignedList', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('assignedList', {
		title: 'Assigned To Me',
		description: 'Displays a list of assigned records',
		controller: 'assignedListCtrl',
		templateUrl: 'js/widgets/assignedList/assignedList.html',
		edit: {}
	});
}).controller('assignedListCtrl', function($scope, $rootScope, $http, $location, localStorageService){

	// initialize as empty to indicate still initializing database connection
	$scope.assignedRecords = [];
	$scope.user = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');

	// pagination variables
	$scope.itemsPerPage = 10;
	$scope.assignedWorkPage = 1;
	$scope.assignedConflictsPage = 1;
	
	// initial tab titles
	$scope.assignedWorkTitle = "Assigned Concepts";
	$scope.assignedConflictsTitle = "Assigned Conflicts";
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	

	$scope.$on('workAvailableWidget.notification.assignWork', function(event, parameters) {
		console.debug('assignedlist: assignWork notification from workAvailableWidget');
		$scope.retrieveAssignedWork($scope.assignedWorkPage);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
			$scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
			$scope.retrieveAssignedWorkForUser(1, $scope.mapUserViewed);
		}
	});

	// on any change of focusProject, retrieve new available work
	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'user', 'userToken'], function() {
		console.debug('assignedListCtrl:  Detected project or user set/change');

		if ($scope.focusProject != null && $scope.user != null && $scope.userToken != null) {

			$http.defaults.headers.common.Authorization = $scope.userToken;	
			
			$scope.mapUsers = $scope.focusProject.mapSpecialist;
			
			$scope.retrieveAssignedWork($scope.assignedWorkPage);
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
				$scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
			}
		}
	});
	
	$scope.retrieveAssignedConflicts = function(page) {
		
		console.debug('Retrieving Assigned Conflicts: page ' + page);
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" + $scope.focusProject.id + "/user/id/" + $scope.user.userName + "/assignedConflicts",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.assignedConflictsPage = page;
			$scope.assignedConflicts = data.searchResult;
			console.debug('Assigned Conflicts:');
			console.debug($scope.assignedConflicts);
			
			// set pagination
			$scope.nAssignedConflicts = data.totalCount;
			$scope.numAssignedConflictsPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			
			// set title
			$scope.assignedConflictsTitle = "Assigned Conflicts (" + data.totalCount + ")";
			
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.retrieveAssignedWork = function(page) {
		
		console.debug('Retrieving Assigned Concepts: page ' + page);

		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" + $scope.focusProject.id + "/user/id/" + $scope.user.userName + "/assignedConcepts",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.assignedWorkPage = page;
			$scope.assignedRecords = data.searchResult;
			console.debug($scope.assignedRecords);
		
			// set pagination
			$scope.numAssignedRecordPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			$scope.nAssignedRecords = data.totalCount;
			
			// set title
			$scope.assignedWorkTitle = "Assigned Concepts (" + $scope.nAssignedRecords + ")";
			console.debug($scope.nAssignedRecords);
			console.debug(data.totalCount);
			console.debug($scope.assignedWorkTitle);
			
			
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.mapUserViewed == null; // intial value
	
	$scope.retrieveAssignedWorkForUser = function(page, mapUser) {
	
		if (mapUser == null || mapUser == undefined && $scope.mapUserViewed != null) {
			console.debug("No map user specified for viewing other user's work.");
			
			console.debug("Selected user: ");
			console.debug($scope.mapUserViewed);
			$scope.assignedRecordsForUser = {};
		} else {
			
			$scope.mapUserViewed = mapUser;
			
			
			console.debug('Retrieving Assigned Concepts for user ' + mapUser.userName + ': page ' + page);

		
			// construct a paging/filtering/sorting object
			var pfsParameterObj = 
						{"startIndex": (page-1)*$scope.itemsPerPage,
				 	 	 "maxResults": $scope.itemsPerPage, 
				 	 	 "sortField": 'sortKey',
				 	 	 "queryRestriction": null};  
	
		  	$rootScope.glassPane++;
	
			$http({
				url: root_workflow + "project/id/" + $scope.focusProject.id + "/user/id/" + mapUser.userName + "/assignedConcepts",
				dataType: "json",
				data: pfsParameterObj,
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
			  	$rootScope.glassPane--;
	
				$scope.assignedWorkForUserPage = page;
				$scope.assignedRecordsForUser = data.searchResult;
				console.debug($scope.assignedRecordsForUser);
			
				// set pagination
				$scope.numAssignedRecordPagesForUser = Math.ceil(data.totalCount / $scope.itemsPerPage);
				$scope.nAssignedRecordsForUser = data.totalCount;
				$scope.numRecordPagesForUser = Math.ceil($scope.nAssignedRecordsForUser / $scope.itemsPerPage);
	
				// set human readable status
				for (var i = 0; i < $scope.assignedRecordsForUser.length; i++) {
					switch ($scope.assignedRecordsForUser[i].terminologyVersion) {
					case 'NEW':
						console.debug("new record");
						$scope.assignedRecordsForUser[i].terminologyVersion = 'New';
						break;
					case 'EDITING_IN_PROGRESS':
						$scope.assignedRecordsForUser[i].terminologyVersion = 'Editing';
						break;
					case 'EDITING_DONE':
						$scope.assignedRecordsForUser[i].terminologyVersion = 'Done';
						break;
					}	
				}
				
							
			}).error(function(data, status, headers, config) {
			    $rootScope.glassPane--;
			    $rootScope.handleHttpError(data, status, headers, config);
			});
		}
	};
	
	// set the pagination variables
	function setPagination(assignedRecordsPerPage, nAssignedRecords) {
		
		$scope.assignedRecordsPerPage = assignedRecordsPerPage;
		$scope.numRecordPages = Math.ceil($scope.nAssignedRecords / assignedRecordsPerPage);
	};


	// on notification, update assigned work
	$scope.assignWork = function(newRecords) {

		$scope.retrieveAssignedWork($scope.assignedWorkPage);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
			$scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
		}
	};

	// function to relinquish work (i.e. unassign the user)
	$scope.unassignWork = function(record) {
		
		$rootScope.glassPane++;
		
		$http({
			url: root_workflow + "unassign/project/id/" + $scope.focusProject.id + "/concept/id/" + record.terminologyId + "/user/id/" + $scope.user.userName,
			dataType: "json",
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {

			$rootScope.$broadcast('assignedListWidget.notification.unassignWork',
					{key: 'mapRecord', mapRecord: record});

			if ($scope.focusProject != null && $scope.user != null) {
				$scope.retrieveAssignedWork($scope.assignedWorkPage);
				if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
					$scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
				}
			}
			
			$rootScope.glassPane--;
			
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};

	// Unassigns all work (both concepts and conflicts) for the current user
	$scope.unassignAllWork = function() {
		
		var confirmUnassign =  confirm("Are you sure you want to return all work? Any editing performed on your assigned work will be lost.");
		if (confirmUnassign == true) {
		
			$rootScope.glassPane++;
			
			$http({
				url: root_workflow + "unassign/project/id/" + $scope.focusProject.id + "/user/id/" + $scope.user.userName,
				dataType: "json",
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				$scope.retrieveAssignedWork($scope.assignedWorkPage);
				if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
					$scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
				}
				$rootScope.$broadcast('assignedListWidget.notification.unassignWork');
				$rootScope.glassPane--;
			}).error(function(data, status, headers, config) {
			    $rootScope.glassPane--;
			    $rootScope.handleHttpError(data, status, headers, config);
			});
		}
	};
	
	$scope.unassignAllConflicts = function() {
		
	}
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
	
	$scope.goEditRecord = function (id) {
		var path = "/record/recordId/" + id;
			// redirect page
			$location.path(path);
	};

	$scope.goEditConflict = function (id) {
		var path = "/record/conflicts/" + id;
			// redirect page
			$location.path(path);
	};
});
