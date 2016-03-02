'use strict';

angular.module('mapProjectApp.widgets.metadataList', [ 'adf.provider' ]).config(
  function(dashboardProvider) {
    dashboardProvider.widget('metadataList', {
      title : 'Metadata',
      description : 'Display metadata for a terminology',
      templateUrl : 'js/widgets/metadataList/metadataList.html',
      controller : 'metadataCtrl',
      resolve : {
        data : function(metadataService, config) {
          if (!config.terminology) {
            return 'SNOMEDCT';
          }
          return config.terminology;
        }
      },
      edit : {
        templateUrl : 'js/widgets/metadataList/edit.html'
      }
    });
  }).service('metadataService', function($q, $http) {
  return {
    get : function(terminology) {
      var deferred = $q.defer();
      $http({
        url : root_metadata + 'metadata/terminology/id/' + terminology,
        dataType : 'json',
        method : 'GET',
        headers : {
          'Content-Type' : 'application/json'
        }
      }).success(function(data) {
        if (data) {
          data.terminology = terminology;
          deferred.resolve(data);
        } else {
          deferred.reject();
        }
      }).error(function(data, status, headers, config) {
        $rootScope.handleHttpError(data, status, headers, config);
        deferred.reject();
      });
      return deferred.promise;
    }
  };
}).controller('metadataCtrl', function($scope, $http, $location, localStorageService, data) {

  // display data
  $scope.keyValuePairLists = null;
  $scope.terminologies = [];
  $scope.terminology = data;

  // watch for change to terminology
  $scope.$watch([ 'terminology', 'userToken' ], function() {
    if ($scope.terminology != null && $scope.userToken != null) {

      $http.defaults.headers.common.Authorization = $scope.userToken;
      $scope.keyValuePairLists = localStorageService.get('metadata_' + $scope.terminology);

      $scope.go();
    }
  });

  $scope.go = function() {

    // get available terminologies
    $http({
      url : root_metadata + 'terminology/terminologies/latest',
      dataType : 'json',
      method : 'GET',
      headers : {
        'Content-Type' : 'application/json'
      }
    }).success(function(data) {
      for ( var index in data.keyValuePair) {
        if (data.keyValuePair[index].key != undefined)
          $scope.terminologies.push(data.keyValuePair[index].key);
      }
      console.debug($scope.terminologies);
    }).error(function(data, status, headers, config) {
      $rootScope.handleHttpError(data, status, headers, config);
    });
  };

  // watch for project change
  $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
    console.debug('metadataCtrl:  Detected change in focus project');
    $scope.terminology = parameters.focusProject.sourceTerminology;
  });

});