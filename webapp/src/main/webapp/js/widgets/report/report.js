
'use strict';

angular.module('mapProjectApp.widgets.report', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('report', {
		title: 'Reports',
		description: 'Displays requested reports',
		controller: 'reportCtrl',
		templateUrl: 'js/widgets/report/report.html',
		edit: {}
	});
}).controller('reportCtrl', function($scope, $rootScope, $http, $location, $modal, $sce, localStorageService){

	// initialize as empty to indicate still initializing database connection
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');
	
	// datepicker formats
	$scope.format = 'yyyy/MM/dd';
	$scope.startDateOpened = false;
	$scope.endDateOpened = false;
	
	// report options
	$scope.reportTypes = ['Specialist Output'];
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	
	

	// on any change of focusProject, set headers
	$scope.currentUserToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'currentUser', 'userToken'], function() {

		
		if ($scope.focusProject != null && $scope.currentUser != null && $scope.currentUserToken != null) {
			$http.defaults.headers.common.Authorization = $scope.currentUserToken;		
			$scope.specialists = $scope.focusProject.mapSpecialist;
			console.debug('specialists:', $scope.specialists);
		}
	});
	
	$scope.getReport = function(specialist, reportType, startDate, endDate) {
		
		if (specialist == null || specialist == undefined) {
			alert("You must select a specialist");
			return;
		}
		
		if (reportType == null || reportType == undefined) {
			alert("You must select a report type");
			return;
		}
		
		if (startDate == null || startDate == undefined) {
			alert("You must select a start date");
			return;
		}
		
		if (endDate == null || endDate == undefined) {
			alert("You must select a end date");
			return;
		}
		
		var reportTypeApiString = '';
		switch (reportType) {
		case "Specialist Output":
			reportTypeApiString = '/specialistOutput';
			break;
		default:
			break;
		}
		
	
		
		var url = root_report 
			+ "project/id/" + $scope.focusProject.id
			+ "/user/id/" + specialist.userName
			+ "/start/" + startDate.getTime()
			+ "/end/" + endDate.getTime()
			+ reportTypeApiString;
		
		console.debug(url);
		$rootScope.glassPane++;
		
		// obtain the record
		$http({
			url: url,
			dataType: "json",
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).success(function(data) {
			$rootScope.glassPane--;
			$scope.report = data;
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
			$scope.report = null;
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	

   
});
