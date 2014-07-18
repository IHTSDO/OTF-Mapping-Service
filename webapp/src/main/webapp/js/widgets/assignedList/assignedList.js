
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
	$scope.assignedTab = localStorageService.get('assignedTab');
	
	// tab variables, defaults to first active tab?
	$scope.tabs = [ {id: 0, title: 'Concepts', active:false}, 
	                {id: 1, title: 'Conflicts', active:false},
	                {id: 2, title: 'Review', active:false},
	                {id: 3, title: 'By User', active:false}];
	
	
	// table sort fields
	$scope.tableFields = [ {id: 0, title: 'id', sortDir: 'asc', sortOn: false}];
	
	$scope.mapUserViewed == null;
	$scope.ownTab = true; // variable to track whether viewing own work or other users work
	$scope.searchPerformed = false;  		// initialize variable to track whether search was performed
	
	$scope.assignedWorkType = 'NEW'; 		// initialize variable to track which type of work has been requested
	$scope.assignedConflictType = 'CONFLICT_NEW'; 	// initialize variable to track which type of conflict has been requested
	$scope.assignedReviewWorkType = 'REVIEW_NEW'
	$scope.assignedWorkForUserType = 'ALL';	// initialize variable to track which type of work (for another user) has been requested
	
	// function to change tab
	$scope.setTab = function(tabNumber) {
		if (tabNumber == null) tabNumber = 0;
		console.debug("Switching to tab " + tabNumber);
		
		angular.forEach($scope.tabs, function(tab) {
			tab.active = (tab.id == tabNumber? true : false);
		});
		
		// set flag for ByUser tab, i.e. whether viewing user's own work
		if (tabNumber == 3) $scope.ownTab = false;
		else $scope.ownTab = true;
		
		// add the tab to the loocal storage service for the next visit
		localStorageService.add('assignedTab', tabNumber);
	
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
	
	// watch for first retrieval of last tab for this session
	$scope.$watch('assignedTab', function () {
		console.debug('assignedTab retrieved', $scope.assignedTab);
		
		// unidentified source is resetting the tab to 0 after initial load
		// introduced a brief timeout to ensure correct tab is picked
		setTimeout(function() {
			$scope.setTab($scope.assignedTab);
		}, 200);
		
	});

	$scope.$on('workAvailableWidget.notification.assignWork', function(event, parameters) {
		console.debug('assignedlist: assignWork notification');
		console.debug(parameters);
		console.debug($scope.currentRole);
		
		// perform action based on notification parameters
		// Expect:
		// - assignUser: String, IHTSDO username (e.g. dmo, kli)
		// - assignType: String, either 'concept' or 'conflict'
		if ($scope.currentRole === 'Lead') {
			
			// if user name matches current user's user name, reload work
			if (parameters.assignUser.userName === $scope.currentUser.userName) {		
				
				if (parameters.assignType === 'concept') {
					$scope.retrieveAssignedWork($scope.assignedWorkPage, null);
					$scope.setTab(0);
				
				} else if (parameters.assignType === 'conflict') {
					$scope.retrieveAssignedConflicts($scope.assignedConflictsPage, null);
					$scope.setTab(1);
				} else if (parameters.assignType === 'review') {
					$scope.retrieveAssignedReviewWork($scope.assignedReviewWorkPage, null);
					$scope.setTab(2);
				}
			} else {
				$scope.retrieveAssignedWorkForUser($scope.assignedWorkForUserPage, parameters.assignUser.userName, null);
				$scope.setTab(3);
				$scope.mapUserViewed = parameters.assignUser;
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
				$scope.retrieveAssignedConflicts(1, null);
				$scope.retrieveAssignedReviewWork(1, null);
				$scope.retrieveAssignedWorkForUser(1, $scope.mapUserViewed, null);
			}
		}
	});
	
	$scope.retrieveAssignedConflicts = function(page, query, assignedConflictType) {
		
		console.debug('Retrieving Assigned Conflicts: page ' + page);
		
		// ensure query is set to null if not specified
		if (query == undefined) query == null;
		
		// set global search performed varaiable based on query
		if (query == null) {
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		}
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": page == -1 ? -1 : (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": page == -1 ? -1 : $scope.itemsPerPage, 
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
			$scope.tabs[1].title = "Conflicts (" + data.totalCount + ")";
			
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
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		
		}
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": page == -1 ? -1 : (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": page == -1 ? -1 : $scope.itemsPerPage, 
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
			$scope.tabs[0].title = "Concepts (" + $scope.nAssignedRecords + ")";
			console.debug($scope.nAssignedRecords);
			console.debug(data.totalCount);
			console.debug($scope.assignedWorkTitle);
			
			
		}).error(function(data, status, headers, config) {
		  	$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.retrieveAssignedReviewWork = function(page, query, assignedWorkType) {
		
		console.debug('Retrieving Assigned Review Work: page ' + page);

		// ensure query is set to null if undefined
		if (query == undefined) query = null;
		
		// reset the search input box if null
		if (query == null) {
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		
		}
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": page == -1 ? -1 : (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": page == -1 ? -1 : $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": assignedWorkType};

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ $scope.currentUser.userName 
			+ "/query/" + (query == null ? null : query)
			+ "/assignedReviewWork",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.assignedReviewWorkPage = page;
			$scope.assignedReviewWork = data.searchResult;
			console.debug($scope.assignedReviewWork);
		
			// set pagination
			$scope.numAssignedRecordPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			$scope.nAssignedReviewWork = data.totalCount;
			
			// set title
			$scope.tabs[2].title = "Review (" + $scope.nAssignedReviewWork + ")";
			console.debug($scope.nAssignedReviewWork);
			console.debug(data.totalCount);
			console.debug($scope.assignedReviewWorkTitle);
			
			
		}).error(function(data, status, headers, config) {
		  	$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.retrieveAssignedWorkForUser = function(page, mapUserName, query, assignedWorkType) {
		
		console.debug("retrieveAssignedWorkForUser:");
		console.debug($scope.mapUserViewed);
		
		// ensure query is set to null if undefined
		if (query == undefined) query = null;
		
		// reset the search box if query is null
		if (query == null) {
			$scope.queryAssignedForUser = null;
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		}

		// if no user specified, set to empty record set, with appropriate pagination variables
		if (mapUserName == null) {
			$scope.assignedWorkForUserPage = 1;
			$scope.assignedRecordsForUser = {};

			// set pagination
			$scope.numAssignedRecordPagesForUser = 0;
			$scope.nAssignedRecordsForUser = 0;
			
			// set title
			$scope.tabs[3].title = "By User";
			
			return;
		}

		console.debug('Retrieving Assigned Concepts for user ' + mapUserName + ': page ' + page);


		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
		{"startIndex": page == -1 ? -1 : (page-1)*$scope.itemsPerPage,
				"maxResults": page == -1 ? -1 : $scope.itemsPerPage, 
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
			
			$scope.tabs[3].title = "By User (" + data.totalCount + ")";


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

				$scope.retrieveAssignedWork($scope.assignedWorkPage, $scope.queryAssigned);
				if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
					$scope.retrieveAssignedConflicts($scope.assignedConflictsPage, $scope.queryConflict);
					$scope.retrieveAssignedReviewWork($scope.assignedReviewWorkPage, $scope.queryReviewWork);
				}
			
			} else {
				$scope.retrieveAssignedWorkForUser(1, mapUser.userName, $scope.queryAssignedForUser);
			}
			
			$rootScope.glassPane--;
			
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};

	// Unassigns all work (both concepts and conflicts) for the current user
	$scope.unassignAllWork = function(unassignType, unassignEdited, mapUser) {

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
	$scope.openUnassignModal = function(mapUser, workType) {
		
		console.debug("openUnassignModal with ", mapUser, workType);

		var modalInstance = $modal.open({
			templateUrl: 'js/widgets/assignedList/assignedListUnassign.html',
			controller: UnassignModalCtrl,
			resolve: {
				
				// switch on whether Lead is viewing their own work or another user's
				mapUserToUnassign: function() {
					if ($scope.ownTab == true) return $scope.currentUser;
					else return mapUser;
				},
				isMapLead: function() { return $scope.currentRole === 'Lead'; },
				unassignWorkType: function() { return workType; }
			}
		});

		// TODO Move API retrieval into neutral functions that don't affect scope
		//  i.e. retrieveAvailableConflicts should simply return a SearchResultList
		//  that then can be used by either this function or a display-affecting function
		modalInstance.result.then(function(result) {
			console.debug("Unassigning batch work for user " + mapUser.userName + ", with parameters: ", result);

			// assemble the list of things to unassign via filtered retrieval APIs
			if (result.unassignConcepts == true) {
				
				var terminologyIdsConcepts = [];
				
				$rootScope.glassPane++;
				console.debug("Retrieving concepts to unassign...");
				var pfsParameterObj = {
						"startIndex": -1,
						"maxResults": -1,
						"sortField": 'sortKey',
						"queryRestriction": result.unassignEditedWork == true ? 'ALL' : 'NEW'};
				
				// retrieve the list of assigned conflicts
				$http({
					url: root_workflow + "project/id/" 
					+ $scope.focusProject.id 
					+ "/user/id/" 
					+ mapUser.userName 
					+ "/query/null"
					+ "/assignedConcepts",
					dataType: "json",
					data: pfsParameterObj,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}
				}).success(function(data) {
					$rootScope.glassPane--;
					for (var i = 0; i < data.searchResult.length; i++) {
						terminologyIdsConcepts.push(data.searchResult[i].terminologyId);
					}
					
					// call the batch unassign API
					console.debug("Unassigning concepts (" + terminologyIdsConcepts.length + ")", terminologyIdsConcepts);
					unassignBatch(mapUser, terminologyIdsConcepts, 'concept');
					$scope.rootScope--;
				}).error(function(data, status, headers, config) {
				  	$rootScope.glassPane--;
				    $rootScope.handleHttpError(data, status, headers, config);
				});
			}
			
			if (result.unassignConflicts == true) {
				
				var terminologyIdsConflicts = [];
				
				$rootScope.glassPane++;
				console.debug("Retrieving conflicts to unassign...");
				// construct a paging/filtering/sorting object
				var pfsParameterObj = {
						"startIndex": -1,
						"maxResults": -1,
						"sortField": 'sortKey',
						"queryRestriction": result.unassignEditedWork == true ? 'ALL' : 'CONFLICT_NEW'};
				
				// retrieve the list of assigned conflicts
				$http({
					url: root_workflow + "project/id/" 
					+ $scope.focusProject.id 
					+ "/user/id/" 
					+ mapUser.userName 
					+ "/query/null"
					+ "/assignedConflicts",
					dataType: "json",
					data: pfsParameterObj,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}
				}).success(function(data) {
					$rootScope.glassPane--;
					for (var i = 0; i < data.searchResult.length; i++) {
						terminologyIdsConflicts.push(data.searchResult[i].terminologyId);
					}
					// call the batch unassign API
					console.debug("Unassigning conflicts (" + terminologyIdsConflicts.length + ")", terminologyIdsConflicts);
					unassignBatch(mapUser, terminologyIdsConflicts, 'conflict');
					
					$scope.rootScope--;
				}).error(function(data, status, headers, config) {
				  	$rootScope.glassPane--;
				    $rootScope.handleHttpError(data, status, headers, config);
				});
			}
			
			if (result.unassignReviewWork == true) {
				
				var terminologyIdsReview = [];
				
				$rootScope.glassPane++;
				console.debug("Retrieving review work to unassign...");
				var pfsParameterObj = {
						"startIndex": -1,
						"maxResults": -1,
						"sortField": 'sortKey',
						"queryRestriction": result.unassignEditedWork == true ? 'ALL' : 'REVIEW_NEW'};
				
				$http({
					url: root_workflow + "project/id/" 
					+ $scope.focusProject.id 
					+ "/user/id/" 
					+ mapUser.userName 
					+ "/query/null"
					+ "/assignedReviewWork",
					dataType: "json",
					data: pfsParameterObj,
					method: "POST",
					headers: {
						"Content-Type": "application/json"
					}
				}).success(function(data) {
					$rootScope.glassPane--;
					for (var i = 0; i < data.searchResult.length; i++) {
						terminologyIdsReview.push(data.searchResult[i].terminologyId);
					}
					// call the batch unassign API
					console.debug("Unassigning review work (" + terminologyIdsReview.length + ")", terminologyIdsReview);
					unassignBatch(mapUser, terminologyIdsReview, 'review');
					
					$scope.rootScope--;
					
					
				}).error(function(data, status, headers, config) {
				  	$rootScope.glassPane--;
				    $rootScope.handleHttpError(data, status, headers, config);
				});
			
			}
			
			
			
			
			
			
		/*	if (unassignEditType === 'all') $scope.unassignAllWork(mapUser, workType, true);
			else if (unassignEditType === 'unedited') $scope.unassignAllWork(mapUser, workType, false);
			else("Alert: Unexpected error attempting to unassign work");*/
		});
	};
	
	var unassignBatch = function(mapUser, terminologyIds, workType) {
		
		console.debug("unassignBatch", mapUser, workType);
		
		$rootScope.glassPane++;
		$http({
			url: root_workflow + "unassign/project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ mapUser.userName 
			+ "/batch",
			dataType: "json",
			data: terminologyIds,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$rootScope.glassPane--;
			
			// set timeout to avoid database issues where unassign and retrieve overlap
			setTimeout(function() {

				// trigger reload of this type of work via broadcast notification
				$rootScope.$broadcast('workAvailableWidget.notification.assignWork', {assignUser: mapUser, assignType: workType});		
			
				// if this user unassigned their own work, broadcast unassign
				if (mapUser.userName === $scope.currentUser.userName)
					$rootScope.$broadcast('assignedListWidget.notification.unassignWork');
			}, 250);	
		}).error(function(data, status, headers, config) {
		  	$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	
	}
	
	var UnassignModalCtrl = function($scope, $modalInstance, mapUserToUnassign, isMapLead, unassignWorkType) { 
		
		console.debug("Entered modal control", mapUserToUnassign, unassignWorkType);
		$scope.mapUserToUnassign = mapUserToUnassign;
		$scope.isMapLead = isMapLead;
		$scope.unassignWorkType = unassignWorkType;
		$scope.unassignEditedWork = 'Unedited'; // note, boolean flags were causing errors, possibly related to radio inputs?
		$scope.unassignConcepts = false;
		$scope.unassignConflicts = false;
		$scope.unassignReviewWork = false;
		
		if ($scope.unassignWorkType == 'concepts' || isMapLead == false) 
			$scope.unassignConcepts = true;
		else if ($scope.unassignWorkType == 'conflicts') 
			$scope.unassignConflicts = true;
		else if ($scope.unassignWorkType == 'review') 
			$scope.unassignReviewWork = true;
		
		$scope.selectAll = function(isSelected) {
			$scope.unassignConcepts = isSelected;
			$scope.unassignConflicts = isSelected;
			$scope.unassignReviewWork = isSelected;
		};
	
		$scope.ok = function(unassignEditedWork, unassignConcepts, unassignConflicts, unassignReviewWork) {
			
			if (unassignEditedWork == null) alert("You must select whether to delete edited work.");
			if (unassignConcepts == false && unassignConflicts == false && unassignReviewWork == false) {
				alert("You must select a type of work to return");
			}
			else {
				var result = {
						'unassignEditedWork':	unassignEditedWork === 'Unedited' ? false : true,
						'unassignConcepts':		unassignConcepts,
						'unassignConflicts':	unassignConflicts,
						'unassignReviewWork':	unassignReviewWork
				};				
				$modalInstance.close(result);
			}
		};

		$scope.cancel = function() {
			$modalInstance.dismiss('cancel');
		};
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
	
	$scope.goEditReviewWork = function (id) {
		var path = "/record/review/" + id;
			// redirect page
			$location.path(path);
	};
});
