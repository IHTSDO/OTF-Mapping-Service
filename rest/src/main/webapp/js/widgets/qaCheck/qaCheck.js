'use strict';

angular.module('mapProjectApp.widgets.qaCheck', [ 'adf.provider' ]).config(
  function(dashboardProvider) {
    dashboardProvider.widget('qaCheck', {
      title : 'QA Checks',
      description : 'Widget for viewing and generating qa checks',
      controller : 'qaCheckCtrl',
      templateUrl : 'js/widgets/qaCheck/qaCheck.html',
      edit : {}
    });
  }).controller(
  'qaCheckCtrl',
  function($scope, $rootScope, $http, $location, $uibModal, $sce, localStorageService) {

    // initialize as empty to indicate still initializing
    // database connection
    $scope.currentUser = localStorageService.get('currentUser');
    $scope.currentRole = localStorageService.get('currentRole');
    $scope.focusProject = localStorageService.get('focusProject');

    // select options
    $scope.qaCheckSelected = null;

    $scope.definitionEditing = null;
    $scope.isAddingDefinition = false;

    $scope.resultTypes = [ 'CONCEPT', 'MAP_RECORD' ];
    $scope.availableRoles = [ 'VIEWER', 'SPECIALIST', 'LEAD', 'ADMINISTRATOR' ];
    $scope.queryTypes = [ 'SQL', 'HQL', 'LUCENE' ];
    $scope.timePeriods = [ 'DAILY', 'WEEKLY', 'MONTHLY' ];

    // value field parsing
    $scope.valueFields = [];

    // pagination variables
    $scope.itemsPerPage = 10;

    // watch for project change
    $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
      $scope.focusProject = parameters.focusProject;
    });

    // on any change of focusProject, set headers
    $scope.currentUserToken = localStorageService.get('userToken');
    $scope.$watch([ 'focusProject', 'currentUser', 'userToken' ], function() {

      if ($scope.focusProject != null && $scope.currentUser != null
        && $scope.currentUserToken != null) {
        $http.defaults.headers.common.Authorization = $scope.currentUserToken;

        // retrieve the definitions
        $scope.definitions = $scope.focusProject.reportDefinition.filter(function(item) {
          if ($scope.currentRole == 'Specialist') {
            return item.roleRequired == 'SPECIALIST' || item.roleRequired == 'VIEWER';
          } else if ($scope.currentRole == 'Lead') {
            return item.roleRequired == 'LEAD' || item.roleRequired == 'SPECIALIST'
              || item.roleRequired == 'VIEWER';
          }
          return true;
        });
      }
    });

    $scope.getResultItems = function(reportResult, page) {

      $rootScope.glassPane++;

      // construct a PFS object
      var pfsParameterObj = {
        'startIndex' : (page - 1) * $scope.itemsPerPage,
        'maxResults' : $scope.itemsPerPage,
        'sortField' : null,
        'queryRestriction' : null
      };

      // obtain the reports
      $http({
        url : root_reporting + 'reportResult/id/' + reportResult.id + '/items',
        dataType : 'json',
        data : pfsParameterObj,
        method : 'POST',
        headers : {
          'Content-Type' : 'application/json'
        }
      }).success(function(data) {
        $rootScope.glassPane--;
        reportResult.reportResultItems = data.reportResultItem;
        reportResult.page = page;
        reportResult.nPages = Math.ceil(reportResult.ct / $scope.itemsPerPage);

        return reportResult;
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        reportResult.reportResultItems = null;
        $rootScope.handleHttpError(data, status, headers, config);
        return null;
      });
    };

    $scope.generateNewReport = function(reportDefinition) {
      $rootScope.glassPane++;

      // obtain the record
      $http(
        {
          url : root_reporting + 'report/generate/project/id/' + $scope.focusProject.id
            + '/user/id/' + $scope.currentUser.userName,
          method : 'POST',
          dataType : 'json',
          data : reportDefinition,
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
        $rootScope.glassPane--;
        // clear the items to prevent display of potentially enormous list
        for (var i = 0; i < data.results.length; i++) {
          data.results[i].resultsItems = [];
        }

        // set the report displayed and get the result items for each report
        // result
        $scope.reportDisplayed = data;
        for (var i = 0; i < $scope.reportDisplayed.results.length; i++) {
          $scope.getResultItems($scope.reportDisplayed.results[i]);
        }

        $scope.definitionMsg = 'Successfully generated new qa check';

        // if ($scope.reportDisplayed.results.length > 0) {
        // reportResult = $scope.getResultItems(
        // $scope.reportDisplayed.results[0], 1);
        // }
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    $scope.exportReport = function(report) {
      $rootScope.glassPane++;
      $http({
        url : root_reporting + 'report/export/' + $scope.reportDisplayed.id,
        dataType : 'json',
        method : 'GET',
        headers : {
          'Content-Type' : 'application/json'
        },
        responseType : 'arraybuffer'
      }).success(function(data) {
        $scope.definitionMsg = 'Successfully exported report';
        var blob = new Blob([ data ], {
          type : 'application/vnd.ms-excel'
        });

        // hack to download store a file having its URL
        var fileURL = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = fileURL;
        a.target = '_blank';
        a.download = getReportFileName(report);
        document.body.appendChild(a);
        $rootScope.glassPane--;
        a.click();

      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    var getReportFileName = function(report) {
      var date = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      return report.name + '.' + date + '.xls';
    };

    $scope.addToQAWorkflow = function(report) {
      $rootScope.glassPane++;

      $http({
        url : root_workflow + 'createQAWork',
        method : 'POST',
        dataType : 'json',
        data : $scope.reportDisplayed.id,
        headers : {
          'Content-Type' : 'application/json'
        }
      }).success(function(data) {
        $rootScope.glassPane--;
        $rootScope.$broadcast('qaCheckWidget.notification.qaWorkCreated');
        $scope.reportDisplayed = null;
        $scope.definitionMsg = 'Successfully added concepts to qa workflow';
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

  });
