
'use strict';

angular.module('mapProjectApp.widgets.editedList', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('editedList', {
		title: 'Recently Edited',
		description: 'Displays a list of records that have been recently modified by the current user',
		controller: 'editedListCtrl',
		templateUrl: 'js/widgets/editedList/editedList.html',
		edit: {}
	});
}).controller('editedListCtrl', function($scope, $rootScope, $http, localStorageService){
	
	// initialize as empty to indicate still initializing database connection
	$scope.editedRecords = [];
	$scope.user = localStorageService.get('currentUser');
	$scope.focusProject = localStorageService.get('focusProject');

	// pagination variables
	$scope.recordsPerPage = 10;
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.project = parameters.focusProject;

		console.debug($scope.project);
	});	

	$scope.$on('availableWork.notification.editWork', function(event, parameters) {

		console.debug("editedListCtrl: Detected editWork notificatoin from availableWork widget");
		$scope.editWork(parameters.editedWork);

	});

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('editedListCtrl:  Detected project set/change');

		if ($scope.focusProject != null) {
			$scope.retrieveEditedWork(1);
		}
	});
	
	$scope.retrieveEditedWork = function(page) {

		 
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.recordsPerPage,
			 	 	 "maxResults": $scope.recordsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  
		
		$http({
			url: root_mapping + "recentRecords/" + $scope.focusProject.id + "/" + $scope.user.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			
			$scope.recordPage = page;
			$scope.nRecords = data.totalCount;
			$scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
			 
			$scope.editedRecords = data.mapRecord;
			

			 
		}).error(function(error) {
			$scope.error = "Error";
		});
	};
	
	/**function getDate(timestamp) {
		var d = new Date(timestamp);
		alert(d.getDate() + '/' + (d.getMonth()+1) + '/' + d.getFullYear());
		$scope.date = d;
	}*/
	



});
