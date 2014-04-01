
'use strict';

angular.module('mapProjectApp.widgets.assignedList', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('assignedList', {
		title: 'Assigned To Me',
		description: 'Displays a list of assigned records',
		controller: 'assignedListCtrl',
		templateUrl: 'js/widgets/assignedList/assignedList2.html',
		edit: {}
	});
}).controller('assignedListCtrl', function($scope, $rootScope, $http, localStorageService){
	
	// initialize as empty to indicate still initializing database connection
	$scope.assignedRecords = [];
	$scope.user = localStorageService.get('currentUser');
	$scope.focusProject = localStorageService.get('focusProject');

	// pagination variables
	$scope.conceptsPerPage = 10;
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.project = parameters.focusProject;

		console.debug($scope.project);
	});	

	$scope.$on('availableWork.notification.assignWork', function(event, parameters) {

		console.debug("assignedListCtrl: Detected assignWork notificatoin from availableWork widget");
		$scope.assignWork(parameters.assignedWork);

	});

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('assignedListCtrl:  Detected project set/change');

		if ($scope.focusProject != null) {
			$scope.retrieveAssignedWork(1);
		}
	});
	
	$scope.retrieveAssignedWork = function(page) {

		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.conceptsPerPage,
			 	 	 "maxResults": $scope.conceptsPerPage-1, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  
		
		$http({
			url: root_workflow + "assigned/id/" + $scope.focusProject.id + "/user/" + $scope.user.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$scope.nAssignedRecords = data.count;
			$scope.assignedRecords = data.mapRecord;
			console.debug($scope.assignedRecords);
		}).error(function(error) {
			$scope.error = "Error";
		});
	};
	
	// set the pagination variables
	function setPagination(assignedRecordsPerPage, nAssignedRecords) {
		
		$scope.assignedRecordsPerPage = assignedRecordsPerPage;
		$scope.numRecordPages = Math.ceil($scope.nAssignedRecords / assignedRecordsPerPage);
	};

	// adds work to the visual display
	// TODO Add more explicit check to enforce contract
	//      Currently notification is passed from available work after successful update
	//      This may not be an ideal way to do it (i.e. this widget dependent)
	$scope.assignWork = function(newRecords) {

		$scope.assignedRecords = $scope.assignedRecords.concat(newRecords);
	};

	// function to relinquish work (i.e. unassign the user)
	$scope.unassignWork = function(record) {
		$http({
			url: root_workflow + "unassign/projectId/" + $scope.focusProject.id + "/conceptId/" + record.conceptId + "/user/" + $scope.user.userName,
			dataType: "json",
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$scope.assignedRecords.removeElement(record);
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
