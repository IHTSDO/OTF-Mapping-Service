
'use strict';

angular.module('mapProjectApp.widgets.metadataList', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('metadataList', {
        title: 'Metadata',
        description: 'Display metadata for a terminology',
        templateUrl: 'js/widgets/metadataList/metadataList.html',
        controller: 'metadataCtrl',
        resolve: {
          data: function(metadataService, config){
            if (!config.terminology){
                config.terminology = 'SNOMEDCT';
            } 
            return metadataService.get(config.terminology);
          }
        },
        edit: {
          templateUrl: 'js/widgets/metadataList/edit.html'
        }
      });
  })
  .service('metadataService', function($q, $http){
    return {
      get: function(terminology){
        var deferred = $q.defer();
		$http({
			  url: root_metadata + "all/" + terminology,
			  dataType: "json",
			  method: "GET",
			  headers: {
				  "Content-Type": "application/json"
			  }
		  }).success(function(data) {
	            if (data){
	                deferred.resolve(data);
	              } else {
	                deferred.reject();
	              }
		  }).error(function() {
              deferred.reject();
		 });
        return deferred.promise;
      }
    };
  })
  .controller('metadataCtrl', function($scope, metadataService, data){
    $scope.data = data;
    $scope.keyValuePairLists = data.keyValuePairList;
    
    // watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("metadataCtrl:  Detected change in focus project");
        $scope.data = metadataService.get(parameters.focusProject.sourceTerminology);
        $scope.keyValuePairLists = data.keyValuePairList;
	});
    
    
  });