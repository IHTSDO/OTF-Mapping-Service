
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
  }).controller('assignedListCtrl', function($scope, $http, localStorageService){
	  // initialize as empty to indicate still initializing database connection
	  $scope.records = [];
	  $scope.user = localStorageService.get('currentUser');
	  $scope.project = localStorageService.get('focusProject');
	  
	  // watch for project change
  	  $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
  		  console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
          $scope.project = parameters.focusProject;
          
          console.debug($scope.project);
	  });	
		
  	  // TODO: don't hard code projectId
      $http({
        url: root_workflow + "assigned/id/1/user/" + $scope.user.userName,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
    	  $scope.records = data.mapRecord;
      }).error(function(error) {
    	  $scope.error = "Error";
      });

  });
