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
      $templateCache, $compile, localStorageService) {

      // initialize as empty to indicate still initializing
      // database connection
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.focusProject = localStorageService.get('focusProject');

      $scope.domains = [];
      $scope.indexPages = [];
      $scope.selectedPage;
      $scope.searchResultsLabel = '';
      $scope.searchResultsIndex = 0;
      $scope.mainTermLabel = '';
      $scope.allCheckBox = false;

      // watch for project change
      $scope
        .$on(
          'localStorageModule.notification.setFocusProject',
          function(event, parameters) {
            window
              .alert("The project and terminology cannot be changed on the index viewer.");
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

              $scope.go();
            }
          });

      $scope.go = function() {

        $http(
          {
            url : root_content + "index/"
              + $scope.focusProject.destinationTerminology + "/"
              + $scope.focusProject.destinationTerminologyVersion,
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
          console.debug("Success in getting viewable indexes.");
          for (var i = 0; i < data.searchResult.length; i++) {
            $scope.domains.push(data.searchResult[i].value);
          }

          $scope.domains = $scope.domains.sort();
          $scope.selectedDomain = $scope.domains[0];
          $scope.retrieveIndexPages($scope.domains[0]);
          $scope.mainTermLabel = '';
        }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // parses the text from a link and calls the search method
      $scope.performSearchFromLink = function(searchText) {
        console.debug("searchText:", searchText);

        $scope.allCheckBox = false;
        var res = searchText.split(",");
        if (res.length == 1) {
          $scope.performAggregatedSearch(res[0], 'undefined', 'undefined');
          $scope.searchField = res[0];
          $scope.subSearchField = '';
          $scope.subSubSearchField = '';
          $scope.mainTermLabel = '';
        } else if (res.length == 2) {
          $scope.performAggregatedSearch(res[0], res[1].trim(), 'undefined');
          $scope.searchField = res[0];
          $scope.subSearchField = res[1];
          $scope.subSubSearchField = '';
          $scope.mainTermLabel = '';
        } else {
          $scope.performAggregatedSearch(res[0], res[1].trim(), res[2].trim());
          $scope.searchField = res[0];
          $scope.subSearchField = res[1];
          $scope.subSubSearchField = res[2];
          $scope.mainTermLabel = '';
        }
      };

      $scope.performAggregatedSearch = function(searchField, subSearchField,
        subSubSearchField) {

        console.debug(searchField + " " + subSearchField + " "
          + subSubSearchField);

        if (searchField == null || searchField == '') {
          window.alert("The first search box must not be empty");
          return;
        }
        if (searchField == '*' && subSearchField == '*'
          && subSubSearchField == '*') {
          window.alert("Oh behave - That search isn't useful!");
          return;
        }
        if (subSearchField == '' || subSearchField == null) {
          subSearchField = 'undefined';
        }
        if (subSubSearchField == '' || subSubSearchField == null) {
          subSubSearchField = 'undefined';
        }

        var url = root_content + "index/"
          + $scope.focusProject.destinationTerminology + "/"
          + $scope.focusProject.destinationTerminologyVersion + "/"
          + $scope.selectedDomain + "/search/" + searchField + "/subSearch/"
          + subSearchField + "/subSubSearch/" + subSubSearchField + "/"
          + $scope.allCheckBox;

        $http({
          url : url,
          dataType : "json",
          method : "GET",
          headers : {
            "Content-Type" : "application/json"
          }
        })
          .success(
            function(data) {
              $scope.results = data.searchResult;

              $scope.nResults = data.totalCount;
              if ($scope.nResults > 0) {
                $scope.searchResultsLabel = "1 of " + $scope.nResults;
                $scope.mainTermLabel = $scope.results[0].value2;
                $scope.searchResultsIndex = 0;

                if ($scope.nResults > 0) {
                  $scope
                    .goToElement($scope.results[$scope.searchResultsIndex].value);
                }

                $scope.setBackwardButtonsDisplayed(false);
                if ($scope.nResults == 1) {
                  $scope.setForwardButtonsDisplayed(false);
                } else {
                  $scope.setForwardButtonsDisplayed(true);
                }
              } else {
                window.alert("No Matching Search Results.");
              }

            }).error(function(data, status, headers, config) {
            $scope.results = null;
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      // returns the set of titles of html pages for the given domain
      $scope.retrieveIndexPages = function(domain) {
        $rootScope.glassPane++;
        console.debug('retrieveIndexPages', domain);
        $http(
          {
            url : root_content + "index/"
              + $scope.focusProject.destinationTerminology + "/"
              + $scope.focusProject.destinationTerminologyVersion + "/"
              + domain,
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {
            $rootScope.glassPane--;
            console.debug("Success in getting viewable pages for index.");
            $scope.indexPages = [];
            for (var i = 0; i < data.searchResult.length; i++) {
              $scope.indexPages.push(data.searchResult[i].value);
            }
            $scope.indexPages = $scope.indexPages.sort();

            $scope.selectedDomain = domain;
            $scope.selectedPage = $scope.indexPages[0];
            $scope.updateUrl($scope.indexPages[0]);
            $scope.mainTermLabel = '';

            // cache all index pages now so they will be available to ng-include
            // when needed
            for (var i = 0; i < $scope.indexPages.length; i++) {
              $rootScope.glassPane++;
              var url = "indexViewerData/"
                + $scope.focusProject.destinationTerminology + "/"
                + $scope.focusProject.destinationTerminologyVersion + "/html/"
                + $scope.selectedDomain + "/" + $scope.indexPages[i] + ".html"

              $http.get(url, {
                cache : $templateCache
              }).then(function(result) {
                console.log(result);
                $templateCache.put(url, result);
                $rootScope.glassPane--;
              });
            }

          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // scrolling to the given eID on the correct html page
      $scope.goToElement = function(eID) {
        $rootScope.glassPane++;

        // if needing to switch to different html page
        if (eID.charAt(0) != $scope.selectedPage) {
          // parse the eID to find the name of the target html page
          $scope.selectedPage = eID.charAt(0);
          // switch to the target html page
          $scope.updateUrl($scope.selectedPage);
          $scope.eID = eID;

          // when the html page finishes loading the scrolling will happen
          // see $rootScope.$on('includeContentLoaded'...
        } else {
          // staying on same html page, so just scroll
          $location.hash(eID);
          $anchorScroll();
        }
        $rootScope.glassPane--;

      };

      // updates the url to switch to display a new html page in the index
      // viewer
      $scope.updateUrl = function(pageName) {
        
        $scope.selectedPage = pageName;

        $scope.tUrl = "indexViewerData/" + $scope.focusProject.destinationTerminology + "/"
          + $scope.focusProject.destinationTerminologyVersion + "/html/"
          + $scope.selectedDomain + "/" + pageName + ".html";

      };

      $scope.goFirstResult = function() {
        console.debug('goFirstResult called', $scope.searchResultsIndex);
        $scope.goToElement($scope.results[0].value);
        $scope.searchResultsLabel = "1 of " + $scope.nResults;
        $scope.mainTermLabel = $scope.results[0].value2;
        $scope.setBackwardButtonsDisplayed(false);
        $scope.setForwardButtonsDisplayed(true);
      };

      $scope.goPreviousResult = function() {
        console.debug('goPreviousResult called', $scope.searchResultsIndex);
        $scope.searchResultsLabel = $scope.searchResultsIndex + " of "
          + $scope.nResults;
        $scope.searchResultsIndex--;
        $scope.goToElement($scope.results[$scope.searchResultsIndex].value);
        $scope.mainTermLabel = $scope.results[$scope.searchResultsIndex].value2;
        if ($scope.searchResultsIndex == 0)
          $scope.setBackwardButtonsDisplayed(false);
        $scope.setForwardButtonsDisplayed(true);
      };

      $scope.goNextResult = function() {
        console.debug('goNextResult called', $scope.searchResultsIndex);
        $scope.searchResultsIndex++;
        $scope.searchResultsLabel = ($scope.searchResultsIndex + 1) + " of "
          + $scope.nResults;
        $scope.mainTermLabel = $scope.results[$scope.searchResultsIndex].value2;
        $scope.goToElement($scope.results[$scope.searchResultsIndex].value);
        if ($scope.results.length == $scope.searchResultsIndex + 1)
          $scope.setForwardButtonsDisplayed(false);
        $scope.setBackwardButtonsDisplayed(true);
      };

      $scope.goLastResult = function() {
        console.debug('goLastResult called', $scope.searchResultsIndex);
        $scope.goToElement($scope.results[$scope.results.length - 1].value);
        $scope.searchResultsLabel = $scope.nResults + " of " + $scope.nResults;
        $scope.mainTermLabel = $scope.results[$scope.results.length - 1].value2;
        $scope.setForwardButtonsDisplayed(false);
        $scope.setBackwardButtonsDisplayed(true);
      };

      $scope.setBackwardButtonsDisplayed = function(b) {
        $scope.previousArrow = b;
        $scope.firstArrow = b;
      };

      $scope.setForwardButtonsDisplayed = function(b) {
        $scope.nextArrow = b;
        $scope.lastArrow = b;
      };

      $scope.$on('$locationChangeStart', function(ev, newUrl, oldUrl) {
        // prevent reloading because it messes up the scrolling
        if (newUrl.indexOf("Help") == -1) {
          ev.preventDefault();
          // if the Help page, allow the default reloading response
        } else {
          $rootScope.glassPane++;
        }

      });

      // called when ng-include completes loading an html page
      $rootScope.$on('$includeContentLoaded', function() {

        console.debug('includeContentLoaded', $scope.eID);
        $location.hash($scope.eID);

        $anchorScroll();
        $rootScope.glassPane--;

      });

      $scope.set_style = function(indexTab) {
        if (indexTab == $scope.selectedPage) {
          return {
            color : "red"
          }
        }
      };
    });
