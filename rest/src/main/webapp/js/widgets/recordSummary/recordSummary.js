'use strict';

angular.module('mapProjectApp.widgets.recordSummary', [ 'adf.provider' ]).config(
  function(dashboardProvider) {
    dashboardProvider.widget('recordSummary', {
      title : 'Record Summary',
      description : 'Displays a full summary of a map record.',
      controller : 'recordSummaryCtrl',
      templateUrl : 'js/widgets/recordSummary/recordSummary.html',
      edit : {}
    });
  }).controller('recordSummaryCtrl',
  function($scope, $rootScope, $http, $routeParams, $location, localStorageService, $sce, utilService, appConfig) {

    // record initially null
    $scope.record = null;
    $scope.fullexpression = null;
    $scope.project = localStorageService.get('focusProject');
	$scope.additionalMapEntryInfoOrderingMap = utilService.processOrderingInfo(appConfig['deploy.additional.map.entry.info.ordering']);

    // watch for updates from the map record widget
    $rootScope.$on('mapRecordWidget.notification.recordChanged', function(event, parameters) {
      $scope.record = parameters.record;
	  $scope.record.fullexpression = utilService.getFullExpression($scope.record);
	  $scope.record = utilService.addOrderIds($scope.record, $scope.additionalMapEntryInfoOrderingMap);
    });
    $rootScope.$on('mapRecordWidget.notification.changeSelectedEntry', function(event, parameters) {
      $scope.record = parameters.record;
	  $scope.record.fullexpression = utilService.getFullExpression($scope.record);
	  $scope.record = utilService.addOrderIds($scope.record, $scope.additionalMapEntryInfoOrderingMap);
    });

    
    // function to return trusted html code (for tooltip content)
    $scope.to_trusted = function(html_code) {
      return $sce.trustAsHtml(html_code);
    };

  });
