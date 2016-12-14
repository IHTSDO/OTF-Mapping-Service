'use strict';

angular.module('mapProjectApp.widgets.report', [ 'adf.provider' ]).config(
  function(dashboardProvider) {
    dashboardProvider.widget('report', {
      title : 'Reports',
      description : 'Widget for viewing and generating reports',
      controller : 'reportCtrl',
      templateUrl : 'js/widgets/report/report.html',
      edit : {}
    });
  }).controller(
  'reportCtrl',
  function($scope, $rootScope, $http, $location, $uibModal, $sce, localStorageService) {

    // initialize as empty to indicate still initializing
    // database connection
    $scope.currentUser = localStorageService.get('currentUser');
    $scope.currentRole = localStorageService.get('currentRole');
    $scope.focusProject = localStorageService.get('focusProject');

    // select options
    $scope.reportSelected = null;

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
      console.debug('MapProjectWidgetCtrl:  Detected change in focus project');
      $scope.focusProject = parameters.focusProject;
    });

    // on any change of focusProject, set headers
    $scope.currentUserToken = localStorageService.get('userToken');
    $scope.$watch([ 'focusProject', 'currentUser', 'userToken' ], function() {

      if ($scope.focusProject != null && $scope.currentUser != null
        && $scope.currentUserToken != null) {
        $http.defaults.headers.common.Authorization = $scope.currentUserToken;

        // retrieve the definitions and sort by name
        $scope.definitions = $scope.focusProject.reportDefinition;
        $scope.definitions.sort();

        // retrieve the first page of
        // reports
        $scope.getReports(1, null, null);
      }
    });

    $scope.getReports = function(page, pdefinition, queryReport) {
      var definition = pdefinition;
      // force reportType to null if undefined or blank string
      if (definition == undefined || definition === '')
        definition = null;

      console.debug('getReports', page, definition, queryReport);

      // construct a PFS object
      var pfsParameterObj = {
        'startIndex' : (page - 1) * $scope.itemsPerPage,
        'maxResults' : $scope.itemsPerPage,
        'sortField' : null,
        'queryRestriction' : null
      };

      $rootScope.glassPane++;

      // construct the url based on whether report type is
      // null
      var url = root_reporting + 'report/reports/project/id/' + $scope.focusProject.id
        + (definition == null ? '' : '/definition/id/' + definition.id);

      // obtain the reports
      $http({
        url : url,
        dataType : 'json',
        data : pfsParameterObj,
        method : 'POST',
        headers : {
          'Content-Type' : 'application/json'
        }
      }).success(function(data) {
        $rootScope.glassPane--;
        $scope.reports = data.report;

        console.debug('Reports fetched: ', data.report);

        // set paging parameters
        $scope.nReports = data.totalCount;
        $scope.nReportPages = Math.ceil(data.totalCount / $scope.itemsPerPage);

      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $scope.reports = null;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    $scope.viewReport = function(report) {
      initializeCollapsed(report); // set the collapses
      // to true
      $scope.reportDisplayed = report; // set the displayed
      // report
    };

    // function to return trusted html code (for advice content)
    $scope.to_trusted = function(html_code) {
      return $sce.trustAsHtml(html_code);
    };

    $scope.generateReport = function(definition) {

      $rootScope.glassPane++;
      console.debug('Definition', definition);
      // obtain the record
      $http(
        {
          url : root_reporting + 'report/generate/project/id/' + $scope.focusProject.id
            + '/user/id/' + $scope.currentUser.userName,
          dataType : 'json',
          data : definition,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
        $rootScope.glassPane--;
        $scope.generatedReport = data;
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $scope.generatedReport = null;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    $scope.toggleResultItems = function(reportResult) {

      // if open, simply close
      if (reportResult.isCollapsed == false) {
        reportResult.isCollapsed = true;

        // if closed, re-open and get result items if
        // necessary
      } else {

        reportResult.isCollapsed = false;
        if (reportResult.reportResultItems == null) {
          $scope.getResultItems(reportResult, reportResult.page);
        }
      }
    };
    // if closed, open
    $scope.getResultItems = function(reportResult, page) {
      $rootScope.glassPane++;
      console.debug('Getting report result items', reportResult, page);

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

    var initializeCollapsed = function(report) {
      for (var i = 0; i < report.results.length; i++) {
        report.results[i].isCollapsed = true;
        report.results[i].reportResultItems = null;
        report.results[i].page = 1;
        report.results[i].nPages = Math.ceil(report.results[i].ct / $scope.itemsPerPage);
      }
    };

    $scope.generateNewReport = function(reportDefinition) {
      $rootScope.glassPane++;
      console.debug('generateNewReport', reportDefinition);

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
        $scope.viewReport(data);
        $scope.getReports(1, null, null);
        $scope.definitionMsg = 'Successfully saved definition';
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    $scope.addReportDefinition = function() {
      console.debug('Adding new report definition');

      var definition = {
        'id' : null,
        'name' : '(New Report)',
        'query' : null,
        'queryType' : null,
        'resultType' : null,
        'roleRequired' : null,
        'isDiffReport' : false,
        'timePeriod' : null
      };

      $scope.definitionEditing = definition;
      $scope.isAddingDefinition = true;

    };

    $scope.deleteReport = function(report, page, selectedDefinition, queryReport) {
      console.debug('in delete Report from reports');

      if (confirm('Are you sure that you want to delete a report?') == false)
        return;

      $http({
        url : root_reporting + 'report/delete',
        dataType : 'json',
        data : report,
        method : 'DELETE',
        headers : {
          'Content-Type' : 'application/json'
        }
      }).success(function(data) {
        console.debug('success to delete report from application');
        $scope.getReports(page, selectedDefinition, queryReport);
      }).error(function(data, status, headers, config) {
        $scope.recordError = 'Error deleting map report from application.';
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    $scope.exportReport = function(report) {
      $rootScope.glassPane++;
      $http({
        url : root_reporting + 'report/export/' + report.id,
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
      var date = new Date(report.timestamp).toISOString().slice(0, 10).replace(/-/g, '');
      return report.name + '.' + date + '.xls';
    };
  });
