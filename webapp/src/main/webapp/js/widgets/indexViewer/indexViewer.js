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
    function($scope, $rootScope, $sce, $http, $location, $anchorScroll, $q, $templateCache, $timeout,
      localStorageService, utilService) {

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
      
      // details display
      $scope.detailsMode = false;

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
            };
            domainTab.pageTabs.push(pageTab);
          });

          domainTab.pageTabs.sort();
          domainTab.pageTabs[0].active = true;
          $scope.domainTabs.push(domainTab);
        });

        // sort domain tabs by name
        $scope.domainTabs.sort(function(a, b) {
          return a.name > b.name;
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

      ////////////////////////////////////////////
      // Searching, Navigation, and Highlighting
      ////////////////////////////////////////////

      $scope.performAggregatedSearch = function(searchField, subSearchField, subSubSearchField,
        requireAll) {

        // check for wildcard-only searches
        if (searchField == '*' && subSearchField == '*' && subSubSearchField == '*') {
          window.alert('Wildcard-only searches are not supported');
          return;
        }

        if (!searchField) {
          window.alert('The first search box must not be empty');
          return;
        }

        // check field requirements
        if (requireAll) {

          if (!subSubearchField) {
            window.alert('The first search box must not be empty');
            return;
          }
          if (!subSubSearchField) {
            window.alert('The first search box must not be empty');
            return;
          }
        }

        var url = root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
          + $scope.focusProject.destinationTerminologyVersion + '/' + $scope.selectedDomain.name
          + '/search/' + searchField + '/subSearch/'
          + (subSearchField ? subSearchField : 'undefined') + '/subSubSearch/'
          + (subSubSearchField ? subSubSearchField : 'undefined') + '/' + $scope.allCheckBox;

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
            $scope.searchResultsLabel = '1 of ' + $scope.nResults;
            $scope.searchResultsLabel = $scope.searchResults[0].value2;
            $scope.searchResultsIndex = 0;

            // goto the first element
            $scope.goToElement($scope.searchResults[0]);

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

      // helper function to find y-pos of element
      function findPos(obj) {
        var curtop = 0;
        if (obj.offsetParent) {
          do {
            curtop += obj.offsetTop;
          } while (obj = obj.offsetParent);
          return [ curtop ];
        }
      }

      $scope.goToElement = function(result) {

        console.debug('Going to result', result);

        if (!result) {
          console.error('Attempted to navigate to null result');
        }

        // remove highlighting from current element
        if ($scope.currentResult) {
          $scope.removeHighlighting($scope.currentResult.value);
        }

        // parse the new search result to determine page
        var page = result.value2.charAt(0);

        // change the page
        $scope.changeTab(page);

        // go to the element on the page, preventing reload
        $rootScope.preventSingleReload = true;
        $location.hash(result.value);
        $anchorScroll(result.value);

        // TODO Apply highlighting is inconsistent, only applied/removed every other search result
        $scope.applyHighlighting(result.value);

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
          console.error('Failed to remove highlighting on item with eid ' + eID);
        }
      };

      /////////////////////////////////////////
      // Element Click Events
      /////////////////////////////////////////

      // search from link
      $scope.search = function(searchStr) {
        $scope.performAggregatedSearch(searchStr, null, null, false);
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

      $scope.detailsMap = {
       
      };
      
      var ct = 0;

      // retrieve popover details
      // TODO Popover introduction dramatically slows compile time of each page, due to additional ng-scopes. Upgrade Angular and resolve this.
      $scope.details = function(link) {
        if (!link) {
          return;
        }
        
        if ($scope.detailsMap.hasOwnProperty(link)) {
         return $scope.detailsMap[link]; 
        }
        
        if (++ct > 10) {
          return;
        }
        
        

        console
          .debug('testing details retrieval', $scope.focusProject, $scope.selectedDomain, link);
        $http.get(
          root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
            + $scope.focusProject.destinationTerminologyVersion + '/' + $scope.selectedDomain.name
            + '/details/' + link).then(function(response) {
              
              // substring to eliminate quotation marks
              // TODO Use a real regular expression for this and stop being lazy
              $scope.detailsMap[link] = response.data.substring(1, response.data.length -2);

        }, function(error) {
        });

      };

      /////////////////////////////////////////
      // Initialization
      /////////////////////////////////////////

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

              // REMOVE THIS AFTER DEV WORK
              // truncate pages for faster testing
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
          function(result) {
            $templateCache.put(url, result);
            $rootScope.glassPane--;
          },

          // Error
          function(result) {
            utilService.handleError('Error caching urls');
            $rootScope.glassPane--;
          });

        });
      };

      //////////////////////////////////
      // Utility
      //////////////////////////////////
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

    });
