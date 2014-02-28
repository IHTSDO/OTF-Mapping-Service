
'use strict';

angular.module('mapProjectApp.widgets.metadataList', ['adf.provider'])
  .value('terminology', 'SNOMEDCT')
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('metadataList', {
        title: 'Metadata',
        description: 'Displays metadata for a terminology',
        controller: 'metadataCtrl',
        templateUrl: 'js/widgets/metadataList/metadataList.html',
        edit: {      
          templateUrl: 'js/widgets/metadataList/edit.html',
          reload: true,
          controller: 'metadataEditCtrl'          
        }
      });
  }).controller('metadataCtrl', function($scope, $http, terminology){

		$http({
			  url: root_metadata + "all/" + terminology,
			  dataType: "json",
			  method: "GET",
			  headers: {
				  "Content-Type": "application/json"
			  }
		  }).success(function(response) {
		      $scope.terminology = terminology;
		      $scope.keyValuePairLists = response.keyValuePairList;
		  }).error(function(error) {
			  $scope.errorMetadata = "Error retrieving all metadata";
		 });
	  	   $http({
				  url: root_metadata + "terminologies/latest/",
				  dataType: "json",
				  method: "GET",
				  headers: {
					  "Content-Type": "application/json"
				  }
			  }).success(function(response) {
			      $scope.termVersionPairs = response;
			  }).error(function(error) {
				  $scope.latestTerminologiesStatus = "Error retrieving metadata terminologies";
			  });
  }).controller('metadataEditCtrl', function($scope, $http, terminology){
		      $scope.terminology = terminology;
		      $http({
				  url: root_metadata + "terminologies/latest/",
				  dataType: "json",
				  method: "GET",
				  headers: {
					  "Content-Type": "application/json"
				  }
			  }).success(function(response) {
			      $scope.termVersionPairs = response;
			  }).error(function(error) {
				  $scope.latestTerminologiesStatus = "Error retrieving metadata terminologies";
			  });
  });

	
