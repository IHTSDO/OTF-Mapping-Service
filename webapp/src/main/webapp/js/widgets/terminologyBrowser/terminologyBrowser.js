'use strict';

angular.module('mapProjectApp.widgets.terminologyBrowser', [ 'adf.provider' ]).config(
  function(dashboardProvider) {

    dashboardProvider.widget('terminologyBrowser', {
      title : function() {
        return 'Terminology Browser';
      },

      description : 'Tree view for terminology',
      templateUrl : 'js/widgets/terminologyBrowser/terminologyBrowser.html',
      controller : 'terminologyBrowserWidgetCtrl',
      resolve : {},
      edit : {}
    });
  })

.controller(
  'terminologyBrowserWidgetCtrl',
  function($scope, $rootScope, $q, $timeout, $http, $routeParams, $location, localStorageService,
    utilService, gpService) {

    // Scope variables
    $scope.listMode = false;
    $scope.focusProject = localStorageService.get('focusProject');
    $scope.userToken = localStorageService.get('userToken');
    $scope.terminology = null;
    $scope.terminologyVersion = null;

    // initialize currently displayed concept as empty object
    $scope.currentOpenConcepts = {};
    $scope.descTypes = {};
    $scope.relTypes = {};

    // initialize search variables
    // the query input
    $scope.query = '';
    $scope.treeQuery = '';
    $scope.searchResults = [];
    $scope.selectedResult = null;

    // Paging
    $scope.pageSize = 5;
    $scope.pagedSearchResults = [];
    $scope.paging = {};
    $scope.paging['search'] = {
      page : 1,
      filter : '',
      sortField : null
    };

    // whether the back button is displayed
    $scope.searchBackAllowed = false;
    // whether the forward button is
    $scope.searchForwardAllowed = false;
    // displayed
    // an array of search terms from query input
    $scope.searchStack = [];
    // the current position in the stack
    $scope.searchStackPosition = 0;
    // the number of results currently in the
    $scope.searchStackResults = 0;
    // array (may be less than array length)

    // watch for project change and modify the local variable if necessary
    // coupled with $watch below, this avoids premature work fetching
    $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
      $scope.focusProject = parameters.focusProject;
      // Set default list mode
      if ($scope.focusProject.destinationTerminology === 'GMDN') {
        $scope.listMode = true;
      }
    });
    // Check current focus project setting
    if ($scope.focusProject.destinationTerminology === 'GMDN') {
      $scope.listMode = true;
    }

    $scope.$on('mapEntryWidget.notification.clearTargetConcept', function(event, parameters) {
      $scope.query = '';
      $scope.treeQuery = '';
    });

    // Code to auto-perform a search - turns out to be not that useful.
    // when mapEntry.js is populated
    // $scope.$on('mapRecordWidget.notification.changeSelectedEntry',
    // function(event, parameters) {
    // // If no query and the entry is empty, search the map record
    // automatically
    // if (!$scope.query && !parameters.entry.targetId) {
    // $scope.query = parameters.record.conceptName.toLowerCase().replace(/[^
    // A-Za-z0-9\.]/g, "");
    // $scope.search();
    // }
    // });

    // function called on storage listener event
    function onStorageEvent(storageEvent) {
      console.debug(storageEvent);
      var targetCode = localStorage.getItem('targetCode');

      // if target code is set, focus window, remove from storage, and set
      // target
      if (targetCode) {
        localStorage.removeItem('targetCode');
        $scope.query = targetCode;
        $scope.search();
        $timeout(function() {
          window.alert('Target code selected in index viewer: ' + targetCode);
        }, 250);

      }
    }

    // add the storage listener
    window.addEventListener('storage', onStorageEvent, false);

    // remove listener on location change
    $scope.$on('$locationChangeStart', function(event) {
      window.removeEventListener('storage', onStorageEvent);
    });

    // REQUIRED WATCH VARIABLES: focusProject, userToken. None others needed.
    $scope.$watch([ 'focusProject', 'userToken' ], function() {
      if ($scope.focusProject != null && $scope.userToken != null) {

        $scope.terminology = $scope.focusProject.destinationTerminology;
        $scope.terminologyVersion = $scope.focusProject.destinationTerminologyVersion;
        $scope.model.title = $scope.terminology + ' Terminology Browser';

        $http.defaults.headers.common.Authorization = $scope.userToken;

        // get the root trees
        $scope.getRootTree();
      }
    });

    // Helper function to ensure all the collapsible truncated information is
    // initially not expanded
    function initTruncationWells(node) {
      // if the first time this has been viewed, close the truncation wells
      for (var i = 0; i < node.descGroups.length; i++) {
        for (var j = 0; j < node.descGroups[i].treePositionDescriptions.length; j++) {
          if (node.descGroups[i].treePositionDescriptions[j].isCollapsed == null
            || node.descGroups[i].treePositionDescriptions[j].isCollapsed == undefined) {
            node.descGroups[i].treePositionDescriptions[j].isCollapsed = true;
          }
        }
      }
    }

    // Handler for the "Search" button
    // Perform a search - list or tree depending on the state
    $scope.search = function() {

      // Query is implied
      if (!$scope.query) {
        return;
      }

      // bail on only whitespace search
      if (new RegExp('^\s+$').test($scope.query)) {
        return;
      }

      // clear the search results
      $scope.searchResults = [];

      // list or tree mode
      if ($scope.listMode) {
        $scope.performSearch($scope.query);
      } else {
        // Perform tree search
        $scope.treeQuery = $scope.query;
        $scope.getRootTreeWithQuery(true);
      }
    };

    // Handler for the 'Reset' button
    // Perform a search - list or tree depending on the state
    $scope.clearSearch = function() {
      $scope.searchStatus = '';
      $scope.query = '';
      $scope.searchResults = [];
      $scope.pagedSearchResults = [];

      $scope.searchStack = [];
      $scope.searchStackPosition = 0;
      $scope.searchStackResults = 0;

      // Get root tree
      $scope.treeQuery = '';
      $scope.getRootTree();
    };

    // Search a terminologyId in the tree from the list
    $scope.selectResult = function(result) {
      $scope.selectedResult = result;
      $scope.treeQuery = result.terminologyId;
      $scope.getRootTreeWithQuery(true);
    };

    // Page search results
    $scope.getPagedSearchResults = function() {
      $scope.pagedSearchResults = utilService.getPagedArray($scope.searchResults,
        $scope.paging['search'], $scope.pageSize);
    };

    // Perform a query search for a list
    $scope.performSearch = function(query) {
      $scope.searchStatus = 'Searching...';
      var t = $scope.focusProject.destinationTerminology;
      var v = $scope.focusProject.destinationTerminologyVersion;
      var lquery = query + ' AND terminology:' + t + ' AND terminologyVersion:' + v;

      gpService.increment();
      $http.get(root_content + 'concept?query=' + encodeURIComponent(lquery)).then(
      // Success
      function(response) {
        $scope.searchStatus = '';

        // if just 0 results, show a message
        if (response.data.searchResult.length == 0) {
          $scope.searchResults = response.data.searchResult;
          $scope.searchStatus = 'No results';
          $scope.getRootTree();
          $scope.manageStack($scope.query);
        }

        // If just 1 result, leave results blank, set tree query and search
        // the root tree
        else if (response.data.searchResult.length == 1) {
          $scope.selectResult(response.data.searchResult[0]);
        }

        // otherwise display results and root tree
        else {
          $scope.searchResults = response.data.searchResult;
          $scope.getPagedSearchResults();
          $scope.getRootTree();
          $scope.manageStack($scope.query);
        }
        gpService.decrement();
      },
      // error
      function(response) {
        gpService.decrement();
        utilService.handleError(response.data);
      });
    };

    // function to get the root nodes
    $scope.getRootTree = function() {
      console.debug('get root tree');

      $rootScope.glassPane++;
      $http({
        url : root_mapping + 'treePosition/project/id/' + $scope.focusProject.id + '/destination',
        method : 'GET',
        headers : {
          'Content-Type' : 'application/json'
        }
      }).success(function(response) {
        console.debug('  tree = ', response);
        $rootScope.glassPane--;
        $scope.terminologyTree = response.treePosition;
        for (var i = 0; i < $scope.terminologyTree; i++) {
          $scope.terminologyTree[i].isOpen = false;
          $scope.terminologyTree[i].isConceptOpen = false;
        }
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    // function to get the root nodes with query
    $scope.getRootTreeWithQuery = function(isNewSearch) {

      // Bail on an empty search
      if ($scope.treeQuery == '') {
        return;
      }
      // bail on only whitespace search
      if (new RegExp('^\s+$').test($scope.treeQuery)) {
        return;
      }
      $scope.searchStatus = 'Searching...';
      $scope.terminologyTree = [];
      console.debug('get root tree with query', $scope.treeQuery);
      $rootScope.glassPane++;
      $http(
        {
          url : root_mapping + 'treePosition/project/id/' + $scope.focusProject.id + '?query='
            + encodeURIComponent($scope.treeQuery),
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(response) {
        $scope.searchStatus = '';
        console.debug('  result = ', response.data);
        $rootScope.glassPane--;

        // limit result count to 10 root tree positions
        for (var x = 0; x < response.treePosition.length && x < 10; x++) {
          $scope.terminologyTree[x] = response.treePosition[x];
        }
        if ($scope.terminologyTree.length == 0) {
          $scope.searchStatus = 'No results';
        }
        $scope.expandAll($scope.terminologyTree);
        if (isNewSearch) {
          $scope.manageStack($scope.treeQuery);
        }

      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    // Manage the search stack
    $scope.manageStack = function(query) {
      // update the position counter
      $scope.searchStackPosition++;

      // check that array still has space, if not reallocate
      if ($scope.searchStack.length <= $scope.searchStackPosition) {

        var newSearchStack = new Array($scope.searchStack.length * 2);
        for (var i = 0; i < $scope.searchStack.length; i++) {
          newSearchStack[i] = $scope.searchStack[i];
        }

        $scope.searchStack = newSearchStack;
      }

      // add the query to the stack
      $scope.searchStack[$scope.searchStackPosition] = query;

      // remove any elements past the search stack position
      for (var i = $scope.searchStackPosition + 1; i < $scope.searchStack.length; i++) {
        $scope.searchStack[i] = '';
      }

      // set the total number of results to this position
      $scope.searchStackResults = $scope.searchStackPosition;

      // set the variables for back/forward
      $scope.searchBackAllowed = $scope.searchStackPosition > 0;
      $scope.searchForwardAllowed = $scope.searchStackPosition < $scope.searchStackResults;

    };

    // search history
    $scope.changeSearch = function(positionChange) {
      $scope.searchStatus = '';

      // alter the position, set the query, and call the search function
      $scope.searchStackPosition += positionChange;
      if ($scope.searchStackPosition < 0)
        $scope.searchStackPosition = 0;
      $scope.query = $scope.searchStack[$scope.searchStackPosition];
      // set the variables for back/forward
      $scope.searchBackAllowed = $scope.searchStackPosition > 0;
      $scope.searchForwardAllowed = $scope.searchStackPosition < $scope.searchStackResults;

      // clear the search results
      $scope.searchResults = [];
      // if query is not populated or undefined, get the root trees, otherwise
      // get query results
      if ($scope.listMode) {
        $scope.performSearch($scope.query);
      } else {
        // Perform tree search
        $scope.treeQuery = $scope.query;
        if (!$scope.treeQuery) {
          $scope.getRootTree();
        } else {
          $scope.getRootTreeWithQuery(false);
        }
      }
    };

    $scope.gotoReferencedConcept = function(referencedConcept) {
      $scope.treeQuery = referencedConcept.terminologyId;
      $scope.getRootTreeWithQuery(true);
    };

    $scope.getLocalTree = function(terminologyId) {
      var deferred = $q.defer();
      $timeout(function() {
        $rootScope.glassPane++;
        console.debug('get local tree', terminologyId);
        $http(
          {
            url : root_mapping + 'treePosition/project/id/' + $scope.focusProject.id
              + '/concept/id/' + terminologyId,
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(response) {
          console.debug('  tree = ', response);
          $rootScope.glassPane--;
          deferred.resolve(response);
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      });

      return deferred.promise;
    };

    // function to recursively expand tree positions
    // also returns true/false if:
    // - the id of the node or one of its children exactly matches the search
    $scope.expandAll = function(treePositions) {
      var retval = false;
      for (var i = 0; i < treePositions.length; i++) {

        // initialize the truncation wells
        initTruncationWells(treePositions[i]);

        // if children have been loaded, expand
        if (treePositions[i].children.length > 0) {
          treePositions[i].isOpen = true;
        }

        // Stop on an exact id search match
        if (treePositions[i].terminologyId.toUpperCase() === $scope.treeQuery.toUpperCase()) {
          // load the concept detalis
          $scope.getConceptDetails(treePositions[i]);

          /*
           * // expand children, but do not expand their info panels for (var j =
           * 0; j < treePositions[i].children.length; i++) {
           * 
           * treePositions[i].children[j].isOpen = true; }
           */

          // stop recursive expansion here;
          retval = true;
        }

        // If this will stop an an exact id search, expand the concept details
        // concept id, get details
        else if ($scope.expandAll(treePositions[i].children) == true) {

          // if this is a root node, bail immediately without expanding anything
          if (treePositions[i].ancestorPath == null || treePositions[i].ancestorPath === '') {
            return false;
          }

          $scope.getConceptDetails(treePositions[i]);
          retval = true;
        }

      }

      // Return true if any exact match was found
      return retval;
    };

    // Toggle the list/tree mode
    $scope.toggleListMode = function() {
      $scope.listMode = !$scope.listMode;
    }

    // Toggle child nodes
    $scope.toggleChildren = function(node) {
      node.isOpen = !node.isOpen;

      // only perform actions if node is open
      if (node.isOpen == true) {
        // check if this node has been retrieved
        if (node.children.length == 0 && node.childrenCount > 0) {
          $scope.getLocalTree(node.terminologyId).then(function(response) {

            // shorthand for the conceptTrees (may be multiple paths)
            var data = response.treePosition;

            // find the tree path along this node
            for (var i = 0; i < data.length; i++) {
              if (data[i].ancestorPath === node.ancestorPath) {
                node.children = node.children.concat(data[i].children);
              }
            }
          });
        } else {
          // do nothing, content already loaded
        }

      }

    };

    // function for toggling retrieval and display of concept details
    $scope.getConceptDetails = function(node) {

      // initialize truncation wells
      initTruncationWells(node);

      // if called when currently displayed, clear current concept
      if (node.isConceptOpen == true) {
        node.isConceptOpen = false;

        // otherwise, retrieve and display this concept
      } else {
        if (node.descGroups.length > 0)
          node.isConceptOpen = true;
        else
          node.isConceptOpen = false;
      }
    };

    // given a typeId and a list of elements, returns those elements with
    // matching typeId
    function getConceptElementsByTypeId(elements, typeId) {
      var elementsByTypeId = [];
      for (var i = 0; i < elements.length; i++) {
        if (String(elements[i].typeId) === String(typeId)) {
          elementsByTypeId.push(elements[i]);
        }
      }
      return elementsByTypeId;
    }

    // ////////////////////////////////////////////////////////////////
    // REFERENCE HANDLING
    //
    // Used for ICD10 * and † display
    // For each reference relationship:
    // - find one or more descriptions matching this relationship
    // - attach the reference information to this relationship
    // - remove the description from the description list
    // ///////////////////////////////////////////////////////////////

    function getFormattedDescriptions(concept, typeId, relTypes) {
      // first, get all descriptions for this TypeId
      var descriptions = getConceptElementsByTypeId(concept.description, typeId);

      // format each description
      for (var i = 0; i < descriptions.length; i++) {
        descriptions[i] = formatDescription(descriptions[i], relTypes, concept);
      }

      return descriptions;

    }

    function formatDescription(description, relTypes, concept) {
      var relationshipsForDescription = [];

      // find any relationship where the terminology id begins with the
      // description terminology id
      for (var i = 0; i < concept.relationship.length; i++) {
        if (concept.relationship[i].terminologyId.indexOf(description.terminologyId) == 0) {
          relationshipsForDescription.push(concept.relationship[i]);
        }
      }
      if (relationshipsForDescription.length > 0) {
        description.referencedConcepts = [];
        for (var i = 0; i < relationshipsForDescription.length; i++) {

          // add the target id
          var referencedConcept = {};
          referencedConcept.terminologyId = relationshipsForDescription[i].destinationConceptId;

          // if a asterisk-to-dagger, add a †
          if (relTypes[relationshipsForDescription[i].typeId].indexOf('Asterisk') == 0) {
            referencedConcept.relType = '†';
          }
          // if a dagger-to-asterik, add a *
          if (relTypes[relationshipsForDescription[i].typeId].indexOf('Dagger') == 0) {
            referencedConcept.relType = '*';
          }
          description.referencedConcepts.push(referencedConcept);

          // remove this relationship from the current concept (now
          // represented in description)
          concept.relationship.removeElementByTerminologyId(relationshipsForDescription[i]);

        }
      }

      return description;

    }

    $scope.getDescriptionGroups = function(terminologyId) {

      var concept = $scope.getElementByTerminologyId($scope.currentOpenConcepts, terminologyId);
      $.map(concept.descriptionGroups, function(v, i) {
        if (v['terminologyId'] === terminologyId)
          return v;
      });
      return null;
    };

    $scope.hasElementByTerminologyid = function(array, terminologyId) {
      $.map(array, function(v, i) {
        if (v['terminologyId'] === terminologyId)
          return true;
      });
      return false;
    };

    $scope.getElementByTerminologyId = function(array, terminologyId) {
      $.map(array, function(v, i) {
        if (v['terminologyId'] === terminologyId)
          return v;
      });
      return null;
    };

    // function to remove an element by id or localid
    // instantiated to negate necessity for equals methods for map objects
    // which may not be strictly identical via string or key comparison
    Array.prototype.removeElementByTerminologyId = function(terminologyId) {
      var array = new Array();
      $.map(this, function(v, i) {
        if (v['terminologyId'] != terminologyId)
          array.push(v);
      });

      this.length = 0; // clear original array
      this.push.apply(this, array); // push all elements except the one we
      // want to delete
    };

    $scope.truncate = function(string, plength) {
      var length = plength;
      if (length == null)
        length = 150;
      if (string.length > length)
        return string.slice(0, length - 3);
      else
        return string;
    };

    $scope.truncated = function(string, plength) {
      var length = plength;
      if (length == null)
        length = 150;
      if (string.length > length)
        return true;
      else
        return false;
    };

    $scope.selectConcept = function(node) {
      $rootScope.$broadcast('terminologyBrowser.selectConcept', {
        key : 'concept',
        concept : node
      });
    };
  });
