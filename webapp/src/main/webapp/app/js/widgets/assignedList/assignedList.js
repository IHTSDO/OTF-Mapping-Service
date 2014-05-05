
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
}).controller('assignedListCtrl', function($scope, $rootScope, $http, localStorageService){
	
	// initialize as empty to indicate still initializing database connection
	$scope.assignedRecords = [];
	$scope.user = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');

	// pagination variables
	$scope.conceptsPerPage = 10;
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	

	$scope.$on('workAvailableWidget.notification.assignWork', function(event, parameters) {
		console.debug('assignedlist: assignWork notification from workAvailableWidget');
		$scope.retrieveAssignedWork(1);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
			$scope.retrieveAssignedConflicts(1);
		}
	});

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('assignedListCtrl:  Detected project set/change');

		if ($scope.focusProject != null) {
			$scope.retrieveAssignedWork(1);
			if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
				$scope.retrieveAssignedConflicts(1);
			}
		}
	});
	
	$scope.retrieveAssignedConflicts = function(page) {
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.conceptsPerPage,
			 	 	 "maxResults": $scope.conceptsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "assignedConflicts/projectId/" + $scope.focusProject.id + "/user/" + $scope.user.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.assignedConflictsPage = page;
			$scope.nAssignedConflicts = data.totalCount;
			$scope.assignedConflicts = data.searchResult;
			$scope.numAssignedConflictsPages = Math.ceil(data.totalCount / $scope.conceptsPerPage);
			console.debug('Assigned Conflicts:');
			console.debug($scope.assignedConflicts);
		}).error(function(error) {
		  	$rootScope.glassPane--;
			$scope.error = "Error";
		});
	};
	
	$scope.retrieveAssignedWork = function(page) {

		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.conceptsPerPage,
			 	 	 "maxResults": $scope.conceptsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "assignedWork/projectId/" + $scope.focusProject.id + "/user/" + $scope.user.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.assignedRecordPage = page;
			$scope.nAssignedRecords = data.totalCount;
			$scope.assignedRecords = data.searchResult;
			console.debug($scope.assignedRecords);
		}).error(function(error) {
		  	$rootScope.glassPane--;
			$scope.error = "Error";
		});
	};
	
	// set the pagination variables
	function setPagination(assignedRecordsPerPage, nAssignedRecords) {
		
		$scope.assignedRecordsPerPage = assignedRecordsPerPage;
		$scope.numRecordPages = Math.ceil($scope.nAssignedRecords / assignedRecordsPerPage);
	};


	// on notification, update assigned work
	$scope.assignWork = function(newRecords) {

		$scope.retrieveAssignedWork(1);
		if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
			$scope.retrieveAssignedConflicts(1);
		}
	};

	// function to relinquish work (i.e. unassign the user)
	$scope.unassignWork = function(record) {
		$http({
			url: root_workflow + "unassign/projectId/" + $scope.focusProject.id + "/concept/" + record.terminologyId + "/user/" + $scope.user.userName,
			dataType: "json",
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$scope.assignedRecords.removeElement(record);
			$scope.nAssignedRecords = Math.max(0, $scope.nAssignedRecords-1);
			$rootScope.$broadcast('assignedListWidget.notification.unassignWork',
					{key: 'mapRecord', mapRecord: record});
			
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
