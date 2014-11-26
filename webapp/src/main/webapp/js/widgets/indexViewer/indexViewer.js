'use strict';

angular
  .module('mapProjectApp.widgets.indexViewer', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('indexViewer', {
      title : 'Index Viewer',
      description : 'Widget for viewing indexes',
      controller : 'indexViewerCtrl',
      templateUrl : 'js/widgets/indexViewer/indexViewer.html',
      edit : {}
    });
  })
  .controller(
    'indexViewerCtrl',
    function($scope, $rootScope, $http, $location, $modal, $sce, $anchorScroll,
      localStorageService) {

      // initialize as empty to indicate still initializing
      // database connection
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.focusProject = localStorageService.get('focusProject');

      // value field parsing
      $scope.domainTabs = [];
      $scope.indexTabs = [];
      $scope.searchResultsLabel = 'Initial label';
      $scope.searchResultsIndex = 0;
      $scope.mainTermLabel = 'Initial main term label';

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(
        event, parameters) {
        console
          .debug("MapProjectWidgetCtrl:  Detected change in focus project");
        $scope.focusProject = parameters.focusProject;
      });

      // on any change of focusProject, set headers
      $scope.currentUserToken = localStorageService.get('userToken');
      $scope
        .$watch(
          [ 'focusProject', 'currentUser', 'userToken' ],
          function() {

            if ($scope.focusProject != null && $scope.currentUser != null
              && $scope.currentUserToken != null) {
              $http.defaults.headers.common.Authorization = $scope.currentUserToken;

              $scope.indexTabs.push({
                title : "A",
                contents : "<html><b>A content</b></html>",
                active : true
              });
              $scope.indexTabs.push({
                title : "B",
                contents : "<html><b>B content</b></html>",
                active : true
              });

              $scope.templateUrl = "partials/doc/ICD10CM/2010/Index/"
                + $scope.indexTabs[1].title + ".html";
              console.debug("templateUrl: ", $scope.templateUrl);

              console.debug("Index Tabs: ", $scope.indexTabs);

              // retrieve the first page of
              // reports
              //$scope.getReports(1, null, null);

              $scope.go();
            }
          });

      $scope.renderHtml = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      $scope.go = function() {
        $http(
          {
            url : root_content + "indexViewer/"
              + $scope.focusProject.destinationTerminology + "/"
              + $scope.focusProject.destinationTerminologyVersion,
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
          console.debug("Success in getting viewable indexes.");
          $scope.domains = data.searchResult;
        }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.performAggregatedSearch = function(allFlag, searchField,
        subSearchField, subSubSearchField) {

        console.debug(searchField + " " + subSearchField + " "
          + subSubSearchField);

        if (searchField == null || searchField == '') {
          window.alert("The first search box must not be empty");
          return;
        }
        if ((subSearchField == null || subSearchField == '')
          && (!subSubSearchField == null || subSubSearchField == '')) {
          window
            .alert("If using the third search box,\nthe second one must not be empty");
          return;
        }
        if (searchField == '*' && subSearchField == '*'
          && subSubSearchField == '*') {
          window.alert("Oh behave - That search isn't useful!");
          return;
        }

        $rootScope.glassPane++;

        var url = root_content + "index/indexes/project/id/"
          + $scope.focusProject.id + "/search/" + searchField + "/subSearch/"
          + subSearchField + "/subSubSearch/" + subSubSearchField;

        $http({
          url : url,
          dataType : "json",
          //data : pfsParameterObj,
          method : "POST",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(function(data) {
          $rootScope.glassPane--;
          $scope.results = data.searchResult;

          $scope.nResults = data.totalCount;
          if ($scope.nResults > 0) {
            $scope.searchResultsLabel = "1 of " + $scope.nResults;
            $scope.searchResultsIndex = 0;
            setBackwardButtonsEnabled(false);
            if ($scope.nResults == 1) {
              setNavigationButtonsEnabled(false);
            } else {
              setNavigationButtonsEnabled(true);
            }
          } else {
            window.alert("No Matching Search Results.");
          }

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $scope.results = null;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.goFirstResult = function() {
        console.debug('goFirstResult called');
      };

      $scope.goPreviousResult = function() {
        console.debug('goPreviousResult called');

      };

      $scope.gotoElement = function(eID) {

        // set the location.hash to the id of
        // the element you wish to scroll to.
        $location.hash(eID);

        // call $anchorScroll()
        $anchorScroll();
      };
    });
