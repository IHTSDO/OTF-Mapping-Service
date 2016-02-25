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
    function($scope, $rootScope, $sce, $http, $location, $anchorScroll, $q, $templateCache,
      $timeout, localStorageService, utilService) {

      // the index domains, domain = { name : '', active : '', pages : ''}
      $scope.domains = [];

      // array objects for tab management
      $scope.domainTabs = null;
      $scope.pageTabs = null;

      // the name and page currently viewed
      $scope.selectedDomain = null;
      $scope.selectedPage = null;

      // search results variables
      $scope.searchResults = null;
      $scope.currentResult = null;
      $scope.searchResultsLabel = null;
      $scope.searchResultIndex = 0;
      $scope.indexTrail = null;

      // get the local storage variables
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.focusProject = localStorageService.get('focusProject');
      $scope.currentUserToken = localStorageService.get('userToken');

      // watch for all required values
      $scope.$watch([ 'focusProject', 'currentUser', 'userToken' ], function() {

        if ($scope.focusProject != null && $scope.currentUser != null
          && $scope.currentUserToken != null) {
          $http.defaults.headers.common.Authorization = $scope.currentUserToken;

          $scope.initialize();
        }
      });

      // create a list of page/active pairs, with first tab active
      // cumbersome hack due to old version of angular-ui-bootstrap
      // that can only receive assignable values for 'active'
      $scope.initializeTabs = function() {
        $scope.domainTabs = [];
        angular.forEach($scope.domains, function(domain) {

          // create the domain tab
          var domainTab = {
            domain : domain,
            active : false,
            pageTabs : []
          };

          // add the page tabs
          angular.forEach(domain.pages, function(page) {
            var pageTab = {
              name : page,
              active : false
            // tabs ordered with orderBy:'name', first tab's active autoset
            };
            domainTab.pageTabs.push(pageTab);
          });

          domainTab.pageTabs.sort(function(a, b) {
            return a.name > b.name;
          });

          console.debug(domainTab.pageTabs);

          $scope.domainTabs.push(domainTab);
        });

        // select the first domain and first page
        $scope.selectDomainTab($scope.domainTabs[0]);
        $scope.selectPageTab($scope.domainTabs[0].pageTabs[0]);
      };

      // processes tab click events
      $scope.selectDomainTab = function(selectedDomainTab) {
        $scope.selectedDomain = selectedDomainTab.domain;
      };

      $scope.selectPageTab = function(pageTab) {
        $scope.selectedPage = pageTab.name;
      };

      // //////////////////////////////////////////
      // Searching, Navigation, and Highlighting
      // //////////////////////////////////////////

      $scope.performAggregatedSearch = function(searchField, subSearchField, subSubSearchField,
        searchAllLevels, suppressAlerts) {

        var deferred = $q.defer();

        // check for wildcard-only searches
        if (searchField == '*' && subSearchField == '*' && subSubSearchField == '*') {
          window.alert('Wildcard-only searches are not supported');
          deferred.reject(null);
          return;
        }

        if (!searchField) {
          window.alert('The first search box must not be empty');
          deferred.reject(null);
          return;
        }

        var url = root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
          + $scope.focusProject.destinationTerminologyVersion + '/' + $scope.selectedDomain.name
          + '/search/' + searchField + '/subSearch/'
          + (subSearchField ? subSearchField : 'undefined') + '/subSubSearch/'
          + (subSubSearchField ? subSubSearchField : 'undefined') + '/' + searchAllLevels;

        $rootScope.glassPane++;
        $http({
          url : url,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {

          $scope.searchResults = data.searchResult;
          $scope.nResults = data.totalCount;

          if ($scope.nResults > 0) {
            $scope.searchResultsLabel = $scope.searchResults[0].value2;
            $scope.goToSearchResult(0);

            deferred.resolve();

          } else {
            // show alert if not suppressed
            if (!suppressAlerts) {
              window.alert('No Matching Search Results.');
            }
            deferred.reject('no results');
          }
          $rootScope.glassPane--;

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $scope.searchResults = null;
          $rootScope.handleHttpError(data, status, headers, config);
          deferred.reject();
        });

        return deferred.promise;
      };
      $scope.goToElement = function(result) {

        console.debug('Going to result', result);

        if (!result) {
          console.error('Attempted to navigate to null result');
        }

        // remove highlighting from current element
        if ($scope.currentResult) {
          $scope.removeHighlighting($scope.currentResult.value);
        }
        
        // get the index trail for this
        $scope.detailsHighlighted(result.value);
      
        // parse the new search result to determine page
        var page = result.value.charAt(0);

        // change the page
        $scope.changeTab(page);

        // apply very short timeout to allow tab change
        $timeout(function() {
          // go to the element on the page, preventing reload
          $rootScope.preventSingleReload = true;
          $location.hash(result.value);

          $scope.applyHighlighting(result.value);
          $anchorScroll(result.value);
        }, 100);
        // update the current result
        $scope.currentResult = result;
      };

      $scope.goToSearchResult = function(index) {
        $scope.searchResultIndex = index;
        $scope.goToElement($scope.searchResults[index]);
      };

      $scope.changeTab = function(tabName) {

        angular.forEach($scope.domainTabs, function(domainTab) {
          if (domainTab.domain.name === $scope.selectedDomain.name) {
            angular.forEach(domainTab.pageTabs, function(pageTab) {
              if (pageTab.name === tabName) {
                pageTab.active = true;
              } else {
                pageTab.active = false;
              }
            });
          }
        });

      };

      // apply highlighting
      $scope.applyHighlighting = function(eID) {
        if (document.getElementById(eID) != null) {
          document.getElementById(eID).style.backgroundColor = "yellow";
        } else {
          console.error('Failed to apply highlighting on item with eid ' + eID);
        }
      };

      // remove highlighting
      $scope.removeHighlighting = function(eID) {
        if (document.getElementById(eID) != null) {
          document.getElementById(eID).style.backgroundColor = "white";
        } else {
          console.error('Failed to remove highlighting on item with eid ' + eID);
        }
      };

      // ///////////////////////////////////////
      // Element Click Events
      // ///////////////////////////////////////

      // search from link
      $scope.search = function(searchStr) {
        // attempt single term search
        $scope.performAggregatedSearch(searchStr, null, null, false, true).then(function() {
          // on success do nothing
        }, function() {

          // try to search sub levels if commas are present
          var splitStr = searchStr.split(',');
          if (splitStr.length === 2) {
            $scope.performAggregatedSearch(splitStr[0], splitStr[1], null, false, false);
          } else if (splitStr.length === 3) {
            $scope.performAggregatedSearch(splitStr[0], splitStr[1], splitStr[2], false, false);
          }
        });
      };

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
    
      
      // retrieve popover details
      function detailsHelper(link) {
        
        var deferred = $q.defer();

        if (!link) {
          deferred.resolve(null);
          return;
        }

        console
          .debug('testing details retrieval', $scope.focusProject, $scope.selectedDomain, link);
        $http.get(
          root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
            + $scope.focusProject.destinationTerminologyVersion + '/' + $scope.selectedDomain.name
            + '/details/' + link).then(
          // Success
          function(response) {
            
            console.debug('resolving', response.data.substring(1, response.data.length - 2));

            // substring to eliminate quotation marks
            deferred.resolve(response.data.substring(1, response.data.length - 2));

          },
          // Error
          function(response) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(response.data, response.status, response.headers,
              response.config);
            deferred.resolve(null);
          });
        
        return deferred.promise;

      };
      
      
      $scope.detailsHighlighted = function(link) {
        console.debug('Getting SR details', link);
        detailsHelper(link).then(function(response) {
          console.debug('details', response);
          $scope.indexTrailHighlighted = response;
        });
      };
      
      $scope.details = function(link) {
        console.debug('Getting details', link);
        detailsHelper(link).then(function(response) {
          console.debug('details', response);
          $scope.indexTrail = response;
        });
      };

      // ///////////////////////////////////////
      // Initialization
      // ///////////////////////////////////////

      $scope.initialize = function() {
        // get the domains
        $scope.getDomains();
      };

      // Initializes all domains
      $scope.getDomains = function() {

        $rootScope.glassPane++;
        $http.get(
          root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
            + $scope.focusProject.destinationTerminologyVersion)

        // on success
        .success(function(domainNames) {

          $rootScope.glassPane--;

          console.debug('Domain names retrieved', domainNames);

          // get the pages
          angular.forEach(domainNames.searchResult, function(searchResult) {
            $scope.getDomainPages(searchResult.value).then(function(domainPages) {

              // construct the domain object
              var domain = {
                name : searchResult.value,
                pages : domainPages.sort(),

              };

              // FOR DEV WORK ONLY: truncate pages for faster testing
              // domain.pages = domain.pages.slice(0, 3);

              // push onto the domains array
              $scope.domains.push(domain);

              // if all domains loaded, sort mark first one as selected
              if ($scope.domains.length == domainNames.searchResult.length) {
                $scope.domains.sort(function(a, b) {
                  return a.name > b.name;
                });

                $scope.initializeTabs();
              }

              // cache the template urls for this domain
              $scope.cacheUrls(domain);
            });

          });

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // Retrieves the pages for a domain name
      $scope.getDomainPages = function(domainName) {

        console.debug('Retrieving pages for domain ' + domainName);
        var deferred = $q.defer();

        $rootScope.glassPane++;
        $http.get(
          root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
            + $scope.focusProject.destinationTerminologyVersion + '/' + domainName)

        // success
        .success(function(searchResults) {
          $rootScope.glassPane--;
          console.debug('Pages for ' + domainName, searchResults);
          var domainPages = searchResults.searchResult.map(function(searchResult) {
            return searchResult.value;
          });

          deferred.resolve(domainPages);

        })

        // failure
        .error(function() {
          $rootScope.glassPane--;
          deferred.reject('Could not load domain pages for domain ' + domainName);
        });
        return deferred.promise;
      };

      $scope.getPageUrl = function(domainName, page) {
        return 'indexViewerData/' + $scope.focusProject.destinationTerminology + '/'
          + $scope.focusProject.destinationTerminologyVersion + '/html/' + domainName + '/' + page
          + '.html';
      };

      $scope.cacheUrls = function(domain) {

        angular.forEach(domain.pages, function(page) {

          var url = $scope.getPageUrl(domain.name, page);

          $rootScope.glassPane++;
          $http.get(url, {
            cache : $templateCache
          }).then(
          // Success
          function(response) {
            $templateCache.put(url, response);
            $rootScope.glassPane--;
          },

          // Error
          function(response) {
            utilService.handleError('Error caching urls');
            $rootScope.glassPane--;
          });

        });
      };

      // ////////////////////////////////
      // Utility
      // ////////////////////////////////
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

    });
