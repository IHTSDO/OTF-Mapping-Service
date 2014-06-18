
'use strict';

angular.module('mapProjectApp.widgets.assignedList', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('assignedList', {
		title: 'Assigned Work',
		description: 'Displays a list of assigned records',
		controller: 'assignedListCtrl',
		templateUrl: 'js/widgets/assignedList/assignedList.html',
		edit: {}
	});
}).controller('assignedListCtrl', function($scope, $rootScope, $http, $location, $modal, localStorageService){

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
	$scope.ownTab = true; // variable to track whether viewing own work or other users work
	
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
			
			$scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
			
			$scope.retrieveAssignedWork($scope.assignedWorkPage);
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
				$scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
			}
		}
	});
	
	$scope.retrieveAssignedConflicts = function(page, query) {
		
		console.debug('Retrieving Assigned Conflicts: page ' + page);
		
		// ensure query is set to null if not specified
		if (query == undefined) query = null;
		
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
			+ $scope.user.userName 
			+ "/query/" + (query == null ? null : query)
			+ "/assignedConflicts",
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
	
	$scope.retrieveAssignedWork = function(page, query) {
		
		console.debug('Retrieving Assigned Concepts: page ' + page);

		// ensure query is set to null if undefined
		if (query == undefined) query = null;
		
		// reset the search input box if null
		if (query == null) $scope.queryAssigned = null;
		
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
			+ $scope.user.userName 
			+ "/query/" + (query == null ? null : query)
			+ "/assignedConcepts",
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
	
	$scope.mapUserViewed == null; // initial value
	
	$scope.retrieveAssignedWorkForUser = function(page, mapUser, query) {
		
		console.debug("retrieveAssignedWorkForUser:");
		console.debug($scope.mapUserViewed);
		
		// ensure query is set to null if undefined
		if (query == undefined) query = null
		
		// reset the search box if query is null
		if (query == null) $scope.queryAssignedForUser = null;

		if (mapUser == null) mapUser = $scope.user;

		console.debug('Retrieving Assigned Concepts for user ' + mapUser.userName + ': page ' + page);


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
			+ mapUser.userName 
			+ "/query/" + (query == null ? null : query)
			+ "/assignedConcepts",
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


		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});

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
	$scope.unassignWork = function(record, mapUser) {
		
		$rootScope.glassPane++;
		
		$http({
			url: root_workflow + "unassign/project/id/" + $scope.focusProject.id + "/concept/id/" + record.terminologyId + "/user/id/" + mapUser.userName,
			dataType: "json",
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {

			if ($scope.ownTab == true) {
				
				$rootScope.$broadcast('assignedListWidget.notification.unassignWork',
						{key: 'mapRecord', mapRecord: record});
	
				if ($scope.focusProject != null && $scope.user != null) {
					$scope.retrieveAssignedWork($scope.assignedWorkPage, $scope.queryAssigned);
					if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
						$scope.retrieveAssignedConflicts($scope.assignedConflictsPage, $scope.queryConflict);
					}
				}
			} else {
				$scope.retrieveAssignedWorkForUser(1, mapUser, $scope.queryAssignedForUser);
			}
			
			$rootScope.glassPane--;
			
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};

	// Unassigns all work (both concepts and conflicts) for the current user
	$scope.unassignAllWork = function(unassignEdited, mapUser) {

		$rootScope.glassPane++;
		
		var unassignUrlEnding = unassignEdited == true ? "/all" : "/unedited";
		
		$http({
			url: root_workflow + "unassign/project/id/" + $scope.focusProject.id + "/user/id/" + mapUser.userName + unassignUrlEnding,
			dataType: "json",
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			
			if ($scope.ownTab == true) {
				console.debug('Viewing own work, retrieving assigned work');
				$scope.retrieveAssignedWork($scope.assignedWorkPage);
				if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
					$scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
				}
				$rootScope.$broadcast('assignedListWidget.notification.unassignWork');
			} else {
				console.debug('Viewing other user work, retrieving assigned work for ' + mapUser.name);
				$scope.retrieveAssignedWorkForUser(1, mapUser);
			}

			$rootScope.glassPane--;
			}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
			    $rootScope.handleHttpError(data, status, headers, config);
		});
		
	};
	
	$scope.setOwnTab = function(ownTab) {
		$scope.ownTab = ownTab;
	};
	
	
	// HACKISH:  Variable passed in is the currently viewed map user in the lead's View Other Work tab
	$scope.openUnassignModal = function(mapUser) {
		
		console.debug("openUnassignModal with ");
		console.debug(mapUser);

		var modalInstance = $modal.open({
			templateUrl: 'js/widgets/assignedList/assignedListUnassign.html',
			controller: UnassignModalCtrl,
			resolve: {
				
				// switch on whether Lead is viewing their own work or another user's
				mapUserToUnassign: function() {
					if ($scope.ownTab == true) return $scope.user;
					else return mapUser;
				}
			}
		});

		modalInstance.result.then(function(unassignType) {
			console.debug('Modal Result: ' + unassignType)
			console.debug(mapUser);
			
			if (unassignType === 'all') $scope.unassignAllWork(true, mapUser);
			else if (unassignType === 'unedited') $scope.unassignAllWork(false, mapUser);
			else("Alert: Unexpected error attempting to unassign work");
		});
	};
	
	var UnassignModalCtrl = function($scope, $modalInstance, mapUserToUnassign) { 
		
		console.debug("Entered modal control");
		console.debug(mapUserToUnassign);
		$scope.mapUserToUnassign = mapUserToUnassign;
	
		$scope.ok = function(unassignType) {
			console.debug("Ok clicked, unassignType = " + unassignType);
			if (unassignType == null || unassignType == undefined) alert("You must select an option.")
			else {
				console.debug($scope.mapUserToUnassign);
				$modalInstance.close(unassignType, $scope.mapUserToUnassign);
			}
		};

		$scope.cancel = function(unassignType) {
			$modalInstance.dismiss('cancel');
		};
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
