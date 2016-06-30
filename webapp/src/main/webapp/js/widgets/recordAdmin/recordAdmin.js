angular
  .module('mapProjectApp.widgets.recordAdmin', [ 'adf.provider' ])
  .config(
    function(dashboardProvider) {
      dashboardProvider
        .widget(
          'recordAdmin',
          {
            title : 'Map Record Administrative Tools',
            description : 'In browser administrative tools to modify map records outside of the normal workflow process',
            controller : 'recordAdminCtrl',
            templateUrl : 'js/widgets/recordAdmin/recordAdmin.html',
            edit : {}
          });
    })
  .controller(
    'recordAdminCtrl',
    function($scope, $rootScope, $http, $location, localStorageService) {

      $scope.currentUser = localStorageService.get('currentUser');
      $scope.focusProject = localStorageService.get('focusProject');

      $scope.currentUserToken = localStorageService.get('userToken');
      $scope.$watch([ 'focusProject', 'userToken' ], function() {

        if ($scope.focusProject != null && $scope.currentUserToken != null) {

          $http.defaults.headers.common.Authorization = $scope.currentUserToken;

            $scope.focusProject.mapSpecialist, $scope.focusProject.mapLead);

          // construct list of specialists and leads
          $scope.projectUsers = $scope.focusProject.mapSpecialist
            .concat($scope.focusProject.mapLead);

        }
      });

      $scope.getRecord = function(id, createQA) {

        $rootScope.glassPane++;
        $http({
          url : root_mapping + 'record/id/' + id,
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          $rootScope.glassPane--;

          $scope.record = data;
          if (createQA == true) {
            $scope.createQARecord(id);
          }

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.createQARecord = function(id) {
        $rootScope.glassPane++;
        $http({
          url : root_workflow + 'createQARecord',
          dataType : 'json',
          data : $scope.record,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
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

        if (confirm('ARE YOU ABSOLUTELY SURE?\n\n  Deleting a record requires recomputing workflow and rerunning indexes, and may cause workflow problems for other records.') == false)
          return;

        $rootScope.glassPane++;
        $http({
          url : root_mapping + 'record/delete',
          method : 'DELETE',
          dataType : 'json',
          data : $scope.record,
          headers : {
            'Content-Type' : 'application/json'
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

      $scope.saveRecord = function(id) {

        if (confirm('ARE YOU ABSOLUTELY SURE?\n\n  Updating a record through this interface requires recomputing workflow and rerunning indexes, and may cause workflow problems for other records.') == false)
          return;

        $rootScope.glassPane++;
        $http({
          url : root_mapping + 'record/update',
          method : 'POST',
          dataType : 'json',
          data : $scope.record,
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          $rootScope.glassPane--;

          $scope.successMsg = 'Successfully updated record ' + $scope.record.id;

          $scope.record = null;

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.removeRecordBatch = function(terminologyIdsUnsplit) {

        if (confirm('ARE YOU ABSOLUTELY SURE?\n\n  Deleting records through this interface requires recomputing workflow and rerunning indexes, and may cause workflow problems for other records.') == false)
          return;
        console.debug('Removing batch of records by terminologyId', terminologyIdsUnsplit);

        var terminologyIds = terminologyIdsUnsplit.split(/,\s*|\s+/);

        $rootScope.glassPane++;
        $http(
          {
            url : root_mapping + 'record/records/delete/project/id/' + $scope.focusProject.id
              + '/batch',
            method : 'DELETE',
            dataType : 'json',
            data : terminologyIds,
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $scope.validationResult = data;
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      $scope.assignFixError = function(terminologyIdsUnsplit, mapUser) {

        if (mapUser == null || mapUser == undefined) {
          alert('You must specify a user');
          return;
        }

        if (confirm('ARE YOU ABSOLUTELY SURE? Any eligible concepts in this list will be re-inserted into the workflow') == false)
          return;

        console.debug('Removing batch of records by terminologyId', terminologyIdsUnsplit);

        var terminologyIds = terminologyIdsUnsplit.split(/,\s*|\s+/);

        $rootScope.glassPane++;
        $http(
          {
            url : root_workflow + 'assign/fixErrorPath/project/id/' + $scope.focusProject.id
              + '/user/id/' + mapUser.userName,
            method : 'POST',
            dataType : 'json',
            data : terminologyIds,
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $scope.validationResultAssign = data;
          console.debug('validation result: ', $scope.validationResultAssign);
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };
    });
