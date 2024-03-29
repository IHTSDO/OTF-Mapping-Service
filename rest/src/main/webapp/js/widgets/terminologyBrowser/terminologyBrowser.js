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

angular.module('mapProjectApp').controller(
  'terminologyBrowserWidgetCtrl',
  function($scope, $rootScope, $q, $timeout, $http, $routeParams, $location, localStorageService,
    utilService, gpService, appConfig) {

    // Scope variables
    $scope.paging = {};
    $scope.listMode = false;
    $scope.focusProject = localStorageService.get('focusProject');
    $scope.userToken = localStorageService.get('userToken');
    $scope.browserRequest = localStorageService.get('browserRequest');
    $scope.terminology = null;
    $scope.terminologyVersion = null;
	$scope.appConfig = appConfig;

    // initialize currently displayed concept as empty object
    $scope.currentOpenConcepts = {};
    $scope.descTypes = {};
    $scope.relTypes = {};

    // initialize search variables
    // the query input
    $scope.query = '';
    $scope.treeQuery = '';
	$scope.terminologyList = (appConfig['deploy.terminology.browser.sort.id'] == undefined ? '' : appConfig['deploy.terminology.browser.sort.id']);
    $scope.searchResults = [];
    $scope.selectedResult = null;
    $scope.paging.tree = {
      page : 1,
      pageSize : 100,
      pages : null,
      totalCount : null
    }

    // Paging -- list view
    $scope.pagedSearchResults = [];
    $scope.paging.search = {
      page : 1,
      pageSize : 10,
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
      var targetCode = localStorage.getItem('targetCode');

      // if target code is set, remove from storage, and set
      // target
      if (targetCode) {
        localStorage.removeItem('targetCode');
        $scope.query = targetCode;
        $scope.search();
        $timeout(function() {
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
    $scope.$watch([ 'focusProject', 'userToken'], function() {
      if ($scope.focusProject != null && $scope.userToken != null) {
        
        if ($scope.browserRequest === 'source' && $location.path().includes('terminology/browser')) {
          $scope.terminology = $scope.focusProject.sourceTerminology;
          $scope.terminologyVersion = $scope.focusProject.sourceTerminologyVersion;  
          
        } else {
          $scope.terminology = $scope.focusProject.destinationTerminology;
          $scope.terminologyVersion = $scope.focusProject.destinationTerminologyVersion;
        }
        
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
    $scope.search = function(page) {

      // Query is implied
      if (!$scope.query) {
        return;
      }

      // bail on only whitespace search
      if (new RegExp('^\s+$').test($scope.query)) {
        return;
      }

      // set the srt parameters
      $scope.srtParameters.query = $scope.query;

      // clear the search results
      $scope.searchResults = [];

      // list or tree mode
      if ($scope.listMode) {
        $scope.performSearch($scope.query);
      } else {
        // Perform tree search
        if (!page) {
          $scope.paging.tree.page = 1;
        }
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
      $scope.srtParameters.query = result.terminologyId;
      $scope.query = result.terminologyId;
      $scope.treeQuery = result.terminologyId;
      $scope.getRootTreeWithQuery(true);
      $scope.toggleListMode(true);
    };

    // Page search results
    $scope.getPagedSearchResults = function() {
      $scope.pagedSearchResults = utilService.getPagedArray($scope.searchResults,
        $scope.paging['search'], $scope.paging['search'].pageSize);
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
     
      var url = root_mapping
        + 'treePosition/project/id/'
        + $scope.focusProject.id
        + (($scope.browserRequest == 'source' && $location.path().includes('terminology/browser')) ? '/source' : '/destination');
      
      gpService.increment();
      $http({
        url : url,
        method : 'GET',
        headers : {
          'Content-Type' : 'application/json'
        }
      }).success(function(response) {
        gpService.decrement();
        
        $scope.terminologyTree = response.treePosition;
      }).error(function(data, status, headers, config) {
        gpService.decrement();
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
      gpService.increment();
      var pfs = {
        'startIndex' : ($scope.paging.tree.page - 1) * $scope.paging.tree.pageSize,
        'maxResults' : $scope.paging.tree.pageSize,
        'sortField' : 'ancestorPath',
        'queryRestriction' : $scope.query.replace(/:/g,' ')
      };
         
      var url = root_mapping
        + 'treePosition/project/id/' 
        + $scope.focusProject.id
        + (($scope.browserRequest === 'source' && $location.path().includes('terminology/browser')) ? '/source' : '')
        + '?query='
        + encodeURIComponent($scope.treeQuery.replace(/:/g,' '));
      
      $http.post(

          url , pfs).success(function(response) {
        
        $scope.searchStatus = '';
        gpService.decrement();

        $scope.terminologyTree = response.treePosition;

        $scope.paging.tree.pages = Math.ceil(response.totalCount / $scope.paging.tree.pageSize);
        $scope.paging.tree.totalCount = response.totalCount

        if (response.totalCount == 1 && $scope.terminology.indexOf('ICD10') == 0) {
          $scope.srtParameters.expandAll = true;
        } else {
          $scope.srtParameters.expandAll = false;
        }

        if ($scope.terminologyTree.length == 0) {
          $scope.searchStatus = 'No results';
        }

        if (isNewSearch) {
          $scope.manageStack($scope.treeQuery);
        }

      }).error(function(data, status, headers, config) {
        gpService.decrement();
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
      $scope.query = referencedConcept.terminologyId;
      $scope.treeQuery = referencedConcept.terminologyId;
      $scope.srtParameters.query = referencedConcept.terminologyId;
      $scope.getRootTreeWithQuery(true);
    };

    $scope.getLocalTree = function(terminologyId) {
      var deferred = $q.defer();
      
      //existing API call did not specify destination or source.  Not changing
      //the existing method for destination but adding one for source.
      var url = root_mapping
        + 'treePosition/project/id/'
        + $scope.focusProject.id
        + '/concept/id/'
        + terminologyId
        + (($scope.browserRequest == 'source' && $location.path().includes('terminology/browser')) ? '/source' : '');
      
      gpService.increment();
      $http(
        {
          url: url,
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(response) {
        gpService.decrement();
        deferred.resolve(response);
      }).error(function(data, status, headers, config) {
        gpService.decrement();
        $rootScope.handleHttpError(data, status, headers, config);
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
                     * // expand children, but do not expand their info panels for (var j = 0; j <
                     * treePositions[i].children.length; i++) {
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

          // $scope.getConceptDetails(treePositions[i]);
          retval = true;
        }

      }

      // Return true if any exact match was found
      return retval;
    };

    // Toggle the list/tree mode
    $scope.toggleListMode = function(skipSearch) {
      $scope.listMode = !$scope.listMode;
      if (!skipSearch) {
        $scope.search(1);
      }
    }

    // Toggle child nodes
    $scope.getTreeChildren = function(node) {

      var deferred = $q.defer();

      $scope.getLocalTree(node.terminologyId).then(function(response) {

        // shorthand for the conceptTrees (may be multiple paths)
        var data = response.treePosition;

        // find the tree path along this node
        for (var i = 0; i < data.length; i++) {
          if (data[i].ancestorPath === node.ancestorPath) {
            deferred.resolve(data[i].children);
          }
        }
      }, function(error) {
        deferred.reject(error);
      });

      return deferred.promise;

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

    //
    // Search Result Tree Renderer callbacks
    //
    $scope.srtCallbacks = {
      getTreeChildren : $scope.getTreeChildren,
      selectConcept : $scope.selectConcept,
      gotoReferencedConcept : $scope.gotoReferencedConcept
    }

    $scope.srtParameters = {
      query : $scope.query,
      expandAll : false,
	  terminologyList : $scope.terminologyList
    }

  });
