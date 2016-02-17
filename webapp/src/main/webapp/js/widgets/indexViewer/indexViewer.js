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
    function($scope, $rootScope, $http, $location, $modal, $sce, $anchorScroll, $templateCache,
      $compile, $timeout, localStorageService, utilService) {

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
      $scope.previousEID = 'A0';


      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        window.alert('The project and terminology cannot be changed on the index viewer.');
      });

      // put the selected target code in storage to trigger any listeners
      $scope.code = function(targetCode) {
        if (!targetCode) {
          return;
        }
        console.debug('testing storage event');
        localStorage.setItem('targetCode', targetCode);

        $timeout(
          function() {
            var val = localStorage.getItem('targetCode');
            if (val) {
              utilService
                .handleError('Target code not received by any listeners. The Mapping Tool is either not open or not in editing view');
              localStorage.removeItem('targetCode');
            } else {
              window.blur();
            }
          }, 500);

      };

      // function to return trusted html code (for tooltip
      // content)
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      $scope.details = function(link) {
        if (!link) {
          return;
        }
        console
          .debug('testing details retrieval', $scope.focusProject, $scope.selectedDomain, link);
        $http.get(
          root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
            + $scope.focusProject.destinationTerminologyVersion + '/' + $scope.selectedDomain
            + '/details/' + link).then(function(response) {

          $scope.testDetailsResult = response.data.replace(/^"(.+(?="$))"$/, '$1');
        }, function(error) {
        });
      };

      // on any change of focusProject, set headers
      $scope.currentUserToken = localStorageService.get('userToken');
      $scope.$watch([ 'focusProject', 'currentUser', 'userToken' ], function() {

        if ($scope.focusProject != null && $scope.currentUser != null
          && $scope.currentUserToken != null) {
          $http.defaults.headers.common.Authorization = $scope.currentUserToken;

          $scope.go();
        }
      });

      $scope.go = function() {

        $rootScope.glassPane++;
        $http(
          {
            url : root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
              + $scope.focusProject.destinationTerminologyVersion,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          for (var i = 0; i < data.searchResult.length; i++) {
            $scope.domains.push(data.searchResult[i].value);
          }

          $scope.domains = $scope.domains.sort();
          $scope.selectedDomain = $scope.domains[0];
          $scope.retrieveIndexPages($scope.domains[0]);
          $scope.mainTermLabel = '';
          $rootScope.glassPane--;
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // parses the text from a link and calls the search method
      $scope.search = function(searchText) {

        $scope.allCheckBox = false;
        var res = searchText.split(',');
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

      $scope.performAggregatedSearch = function(psearchField, psubSearchField, psubSubSearchField) {
        var searchField = psearchField;
        var subSearchField = psubSearchField;
        var subSubSearchField = psubSubSearchField;

        if (searchField == null || searchField == '') {
          window.alert('The first search box must not be empty');
          return;
        }
        if (searchField == '*' && subSearchField == '*' && subSubSearchField == '*') {
          window.alert('Wildcard-only searches are not supported');
          return;
        }
        if (subSearchField == '' || subSearchField == null) {
          subSearchField = 'undefined';
        }
        if (subSubSearchField == '' || subSubSearchField == null) {
          subSubSearchField = 'undefined';
        }

        var url = root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
          + $scope.focusProject.destinationTerminologyVersion + '/' + $scope.selectedDomain
          + '/search/' + searchField + '/subSearch/' + subSearchField + '/subSubSearch/'
          + subSubSearchField + '/' + $scope.allCheckBox;

        $rootScope.glassPane++;
        $http({
          url : url,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          $scope.results = data.searchResult;

          $scope.nResults = data.totalCount;
          if ($scope.nResults > 0) {
            $scope.searchResultsLabel = '1 of ' + $scope.nResults;
            $scope.mainTermLabel = $scope.results[0].value2;
            $scope.searchResultsIndex = 0;

            if ($scope.nResults > 0) {
              $scope.goToElement($scope.results[$scope.searchResultsIndex].value);
            }

            $scope.setBackwardButtonsDisplayed(false);
            if ($scope.nResults == 1) {
              $scope.setForwardButtonsDisplayed(false);
            } else {
              $scope.setForwardButtonsDisplayed(true);
            }
          } else {
            window.alert('No Matching Search Results.');
          }
          $rootScope.glassPane--;

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $scope.results = null;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // returns the set of titles of html pages for the given domain
      $scope.retrieveIndexPages = function(domain) {
        console.debug('retrieveIndexPages', domain);
        $rootScope.glassPane++;
        $http(
          {
            url : root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
              + $scope.focusProject.destinationTerminologyVersion + '/' + domain,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
          function(data) {
            console.debug('Success in getting viewable pages for index.');
            $scope.indexPages = [];
            for (var i = 0; i < data.searchResult.length; i++) {
              $scope.indexPages.push(data.searchResult[i].value);
            }
            $scope.indexPages = $scope.indexPages.sort();

            $scope.selectedDomain = domain;
            $scope.selectedPage = $scope.indexPages[0];
            $scope.updateUrl($scope.indexPages[0]);
            $scope.mainTermLabel = '';
            $rootScope.glassPane--;

            // cache all index pages now so they will be available to ng-include
            // when needed
            for (var i = 0; i < $scope.indexPages.length; i++) {
              $rootScope.glassPane++;
              var url = 'indexViewerData/' + $scope.focusProject.destinationTerminology + '/'
                + $scope.focusProject.destinationTerminologyVersion + '/html/'
                + $scope.selectedDomain + '/' + $scope.indexPages[i] + '.html';

              $http.get(url, {
                cache : $templateCache
              }).then(
              // Success
              function(result) {
                $templateCache.put(url, result);
                $rootScope.glassPane--;
              }, function(result) {
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

        // remove highlighting on previous result
        $scope.removeHighlighting($scope.previousEID);

        // if needing to switch to different html page
        if (eID.charAt(0) != $scope.selectedPage) {
          // parse the eID to find the name of the target html page
          $scope.selectedPage = eID.charAt(0);
          // switch to the target html page
          $scope.updateUrl($scope.selectedPage);
          /*if ($scope.results[eID]) {
            $scope.applyHighlighting($scope.results[eID].value);
          }*/
          
          $scope.eID = eID;
          $scope.previousEID = eID;


          // when the html page finishes loading the scrolling will happen
          // see $rootScope.$on('includeContentLoaded'...
        } else {
          // staying on same html page, so just scroll
          $location.hash(eID);
          $anchorScroll();
          
          $scope.applyHighlighting(eID);

          $scope.previousEID = eID;

        }

      };

      // apply highlighting
      $scope.applyHighlighting = function(eID) {
        if (document.getElementById(eID) != null) {
          document.getElementById(eID).style.backgroundColor = "yellow";            
        }
      }
      
      // remove highlighting
      $scope.removeHighlighting = function(eID) {
        if (document.getElementById(eID) != null) {
          document.getElementById(eID).style.backgroundColor = "white";            
        }
      }

      // updates the url to switch to display a new html page in the index
      // viewer

      $scope.updateUrl = function(pageName) {

        $scope.selectedPage = pageName;

        $scope.tUrl = 'indexViewerData/' + $scope.focusProject.destinationTerminology + '/'
          + $scope.focusProject.destinationTerminologyVersion + '/html/' + $scope.selectedDomain
          + '/' + pageName + '.html';
        $scope.testTemplates = [];
        $scope.testTemplates.push($scope.tUrl);

        console.debug($scope.testTemplates);

      };

      $scope.goFirstResult = function() {
        $scope.searchResultsIndex = 0;
        $scope.goToElement($scope.results[0].value);
        $scope.searchResultsLabel = '1 of ' + $scope.nResults;
        $scope.mainTermLabel = $scope.results[0].value2;
        $scope.setBackwardButtonsDisplayed(false);
        $scope.setForwardButtonsDisplayed(true);
      };

      $scope.goPreviousResult = function() {
        $scope.searchResultsLabel = $scope.searchResultsIndex + ' of ' + $scope.nResults;
        $scope.searchResultsIndex--;
        $scope.goToElement($scope.results[$scope.searchResultsIndex].value);
        $scope.mainTermLabel = $scope.results[$scope.searchResultsIndex].value2;
        if ($scope.searchResultsIndex == 0)
          $scope.setBackwardButtonsDisplayed(false);
        $scope.setForwardButtonsDisplayed(true);
      };

      $scope.goNextResult = function() {
        $scope.searchResultsIndex++;
        $scope.searchResultsLabel = ($scope.searchResultsIndex + 1) + ' of ' + $scope.nResults;
        $scope.mainTermLabel = $scope.results[$scope.searchResultsIndex].value2;
        $scope.goToElement($scope.results[$scope.searchResultsIndex].value);
        if ($scope.results.length == $scope.searchResultsIndex + 1)
          $scope.setForwardButtonsDisplayed(false);
        $scope.setBackwardButtonsDisplayed(true);
      };

      $scope.goLastResult = function() {
        $scope.searchResultsIndex = $scope.results.length - 1;
        $scope.goToElement($scope.results[$scope.results.length - 1].value);
        $scope.searchResultsLabel = $scope.nResults + ' of ' + $scope.nResults;
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
        if (newUrl.indexOf('Help') == -1) {
          ev.preventDefault();
          // if the Help page, allow the default reloading response
        }

      });

      // called when ng-include completes loading an html page
      $rootScope.$on('$includeContentLoaded', function() {
        $location.hash($scope.eID);
        $anchorScroll();
      });

      $scope.set_style = function(indexTab) {
        if (indexTab == $scope.selectedPage) {
          return {
            color : 'red'
          };
        }
      };
    });
