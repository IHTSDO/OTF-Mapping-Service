angular.module('mapProjectApp.widgets.recordAdmin', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('recordAdmin', {
		title: 'Map Record Administrative Tools',
		description: 'In browser administrative tools to modify map records outside of the normal workflow process',
		controller: 'recordAdminCtrl',
		templateUrl: 'js/widgets/recordAdmin/recordAdmin.html',
		edit: {}
	});
}).controller('recordAdminCtrl', function($scope, $rootScope, $http, $location, localStorageService){
	
	$scope.user = localStorageService.get('currentUser');
	$scope.project = localStorageService.get('focusProject');
	
	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'userToken'], function() {
		console.debug('editedListCtrl:  Detected project set/change');

		if ($scope.focusProject != null && $scope.userToken != null) {
			
			$http.defaults.headers.common.Authorization = $scope.userToken;
			
		}
	});
	
	$scope.getRecord = function(id) {
		
		$rootScope.glassPane++;
		$http({
			url: root_mapping + "record/id/" + id,
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;
			
		  	$scope.record = data;
						 
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.deleteRecord = function(id) {
		
		if (confirm("ARE YOU ABSOLUTELY SURE?\n\n  Deleting a record requires recomputing workflow and rerunning indexes, and may cause workflow problems for other records.") == false)
			return;
		
		$rootScope.glassPane++;
		$http({
			url: root_mapping + "record/delete",
			method: "DELETE",
			dataType: "json",
			data: $scope.record,
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

		  	$scope.successMsg = 'Successfully deleted record ' + $scope.record.id;
		  	
			$scope.record = null;
						 
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
		
});
