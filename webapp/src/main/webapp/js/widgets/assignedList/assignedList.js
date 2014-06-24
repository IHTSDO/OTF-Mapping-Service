
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
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');
	
	// tab variables
	$scope.tabs = [ {id: 0, title: 'Assigned Concepts', active:true}, 
	                {id: 1, title: 'Assigned Conflicts', active:false}, 
	                {id: 2, title: 'Assigned Work By User', active:false}];
	
	
	// table sort fields
	$scope.tableFields = [ {id: 0, title: 'id', sortDir: 'asc', sortOn: false},]
	
	
	$scope.ownTab = true; // variable to track whether viewing own work or other users work

	$scope.searchPerformed = false;  		// initialize variable to track whether search was performed
	$scope.assignedWorkType = 'ALL'; 		// initialize variable to track which type of work has been requested
	$scope.assignedConflictType = 'ALL'; 	// initialize variable to track which type of conflict has been requested
	$scope.assignedWorkForUserType = 'ALL';	// initialize variable to track which type of work (for another user) has been requested
	
	// function to change tab
	$scope.setTab = function(tabNumber) {
		console.debug("Switching to tab " + tabNumber);

		$scope.searchPerformed = false;
		
		angular.forEach($scope.tabs, function(tab) {
			tab.active = (tab.id == tabNumber? true : false);
		});
		console.debug($scope.tabs);
		
		// set flag for whether viewing user's own work
		if (tabNumber == 2) $scope.ownTab = false;
		else $scope.ownTab = true;
	
	};
	
	
	// pagination variables
	$scope.itemsPerPage = 10;
	$scope.assignedWorkPage = 1;
	$scope.assignedConflictsPage = 1;
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	

	$scope.$on('workAvailableWidget.notification.assignWork', function(event, parameters) {
		console.debug('assignedlist: assignWork notification from workAvailableWidget');
		console.debug(parameters);
		
		// perform action based on notification parameters
		// Expect:
		// - assignUser: String, IHTSDO username (e.g. dmo, kli)
		// - assignType: String, either 'concept' or 'conflict'
		if ($scope.currentRole === 'Lead') {
			
			// if user name matches current user's user name, reload work
			if (parameters.assignUser === $scope.currentUser.userName) {		
				
				if (parameters.assignType === 'concept') {
					$scope.retrieveAssignedWork($scope.assignedWorkPage, null);
					$scope.setTab(0);
				
				} else if (parameters.assignType === 'conflict') {
					$scope.retrieveAssignedConflicts($scope.assignedConflictsPage, null);
					$scope.setTab(1);
				}
			} else {
				$scope.retrieveAssignedWorkForUser($scope.assignedWorkForUserPage, parameters.assignUser, null);
				$scope.setTab(2);
			}
			
		}
		else {
			// reload current assigned concepts, saving page information
			$scope.retrieveAssignedWork($scope.assignedWorkPage);
			$scope.setTab(0);
		}
	});

	// on any change of focusProject, retrieve new available work
	$scope.currentUserToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'user', 'userToken'], function() {
		console.debug('assignedListCtrl:  Detected project or user set/change');

		if ($scope.focusProject != null && $scope.currentUser != null && $scope.currentUserToken != null) {

			$http.defaults.headers.common.Authorization = $scope.currentUserToken;	
			
			$scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
			
			$scope.retrieveAssignedWork($scope.assignedWorkPage, null);
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
				$scope.retrieveAssignedConflicts($scope.assignedConflictsPage, null);
			}
		}
	});
	
	$scope.retrieveAssignedConflicts = function(page, query, assignedConflictType) {
		
		console.debug('Retrieving Assigned Conflicts: page ' + page);
		
		// ensure query is set to null if not specified
		if (query == undefined) {
			query = null;
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		}
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": assignedConflictType};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ $scope.currentUser.userName 
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
			$scope.tabs[1].title = "Assigned Conflicts (" + data.totalCount + ")";
			
		}).error(function(data, status, headers, config) {
		  	$rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.retrieveAssignedWork = function(page, query, assignedWorkType) {
		
		console.debug('Retrieving Assigned Concepts: page ' + page);

		// ensure query is set to null if undefined
		if (query == undefined) query = null;
		
		// reset the search input box if null
		if (query == null) {
			$scope.queryAssigned = null;
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		
		}
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": assignedWorkType};

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ $scope.currentUser.userName 
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
			$scope.tabs[0].title = "Assigned Concepts (" + $scope.nAssignedRecords + ")";
			console.debug($scope.nAssignedRecords);
			console.debug(data.totalCount);
			console.debug($scope.assignedWorkTitle);
			
			
		}).error(function(data, status, headers, config) {
		  	$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.mapUserViewed == null; // initial value
	
	$scope.retrieveAssignedWorkForUser = function(page, mapUserName, query, assignedWorkType) {
		
		console.debug("retrieveAssignedWorkForUser:");
		console.debug($scope.mapUserViewed);
		
		// ensure query is set to null if undefined
		if (query == undefined) query = null
		
		// reset the search box if query is null
		if (query == null) {
			$scope.queryAssignedForUser = null;
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		}

		if (mapUserName == null) mapUserName = $scope.currentUser.userName;

		console.debug('Retrieving Assigned Concepts for user ' + mapUserName + ': page ' + page);


		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
		{"startIndex": (page-1)*$scope.itemsPerPage,
				"maxResults": $scope.itemsPerPage, 
				"sortField": 'sortKey',
				"queryRestriction": assignedWorkType};  

		$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ mapUserName
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
			
			$scope.tabs[2].title = "Assigned Work By User (" + data.totalCount + ")";


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
				
				$rootScope.$broadcast('assignedListWidget.notification.unassignWork');
	
				if ($scope.focusProject != null && $scope.currentUser != null) {
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
					if ($scope.ownTab == true) return $scope.currentUser;
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
