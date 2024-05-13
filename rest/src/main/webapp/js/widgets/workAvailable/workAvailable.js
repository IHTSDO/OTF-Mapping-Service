'use strict';

angular
  .module('mapProjectApp.widgets.workAvailable', [ 'adf.provider' ])
  .config(function(dashboardProvider) {

    dashboardProvider.widget('workAvailable', {
      title : 'Concepts',
      description : 'Module to assign work to users',
      controller : 'workAvailableWidgetCtrl',
      templateUrl : 'js/widgets/workAvailable/workAvailable.html',
      resolve : {},
      edit : {}
    });
  })

  .controller(
    'workAvailableWidgetCtrl',
    function($scope, $rootScope, $http, $routeParams, $uibModal, $location, localStorageService, utilService, appConfig, gpService) {

      // local variables
      $scope.batchSizes = [ 100, 50, 25, 10, 5 ];
      $scope.batchSize = $scope.batchSizes[2];
      $scope.batchSizeConflict = $scope.batchSizes[4];
      $scope.batchSizeReview = $scope.batchSizes[4];
      $scope.availableCount = [0, 0, 0, 0]; // holding available count for each tab in assignment

      // pagination variables
      $scope.itemsPerPage = 10;
      $scope.availableWorkPage = 1;
      $scope.availableConflictsPage = 1;
      $scope.availableReviewPage = 1;
      $scope.availableQAPage = 1;

      // initial titles
      $scope.availableWorkTitle = 'Concepts';
      $scope.availableConflictsTitle = 'Conflicts';
      $scope.availableReviewWorkTitle = 'Review';
      $scope.availableQAWorkTitle = 'QA';

      // retrieve focus project, current user, and current role
      $scope.focusProject = localStorageService.get('focusProject');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.userToken = localStorageService.get('userToken');
      $scope.availableTab = localStorageService.get('availableTab');
      $scope.isConceptListOpen = false;
      $scope.queryAvailable = null;

      // innitialize the search fields
      $scope.queryAvailableWork = null;
      $scope.queryAvailableReview = null;
      $scope.queryAvailableConflict = null;
      $scope.queryAvailableQaWork = null;
      $scope.queryAvailableWorkForUser = null;

      // intiialize the user list
      $scope.mapUsers = {};

	  // MIMS Condition project wants default sort by preferred name
	  if ($scope.focusProject.projectSpecificAlgorithmHandlerClass == 'org.ihtsdo.otf.mapping.jpa.handlers.MIMSConditionToSnomedProjectSpecificAlgorithmHandler' ||
		  $scope.focusProject.projectSpecificAlgorithmHandlerClass == 'org.ihtsdo.otf.mapping.jpa.handlers.MIMSAllergyToSnomedProjectSpecificAlgorithmHandler') {
		$scope.defaultSortField = 'defaultPreferredName';
	  } else {
		$scope.defaultSortField = 'terminologyId';
	  }

	  // tags for tag filtering (if applicable for the project)
      $scope.selectedTags = new Array();
      $scope.allTags = new Array();

      $scope.multiSelectSettings = {
        displayProp : 'tag',
        scrollableHeight : '150px',
        scrollable : true,
        showCheckAll : false,
        showUncheckAll : false
      };
      $scope.multiSelectCustomTexts = {
        buttonDefaultText : 'Select Tags'
      };

      // tab variables, defaults to first active tab?
      $scope.tabs = [ {
        id : 0,
        title : 'Concepts',
        active : true
      }, {
        id : 1,
        title : 'Conflicts',
        active : false
      }, {
        id : 2,
        title : 'Review',
        active : false
      }, {
        id : 3,
        title : 'QA',
        active : false
      } ];

      // labels for QA filtering
      $scope.labelNames = [];
      		
	  //UI Labels
	  $scope.getUILabel = function(label){
		if (utilService.checkIfProjectRelabels($scope.focusProject.refSetId, appConfig['deploy.ui.relabel.refsetids'])){
		  return utilService.getUILabel(label, appConfig['deploy.ui.relabel.' + $scope.focusProject.refSetId]);
		}
		else {
		  return label;
		}
	  }

      // watch for project change and modify the local variable if necessary
      // coupled with $watch below, this avoids premature work fetching
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        $scope.focusProject = parameters.focusProject;
      });

      // on unassign notification, refresh the available work widget
      $scope.$on('workAssignedWidget.notification.unassignWork', function(event, parameters) {
        $scope.retrieveAvailableWork(1, $scope.queryAvailableWork);
        $scope.retrieveAvailableQAWork(1, $scope.queryAvailableQaWork);
        if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
          $scope.retrieveAvailableConflicts(1, $scope.queryAvailableConflicts);
          $scope.retrieveAvailableReviewWork(1, $scope.queryAvailableReviewWork);
        }
      });

      // on computation of workflow, refresh the available work widget
      $scope.$on('mapProjectWidget.notification.workflowComputed', function(event, parameters) {
        $scope.retrieveAvailableWork($scope.availableWorkPage);
        $scope.retrieveAvailableQAWork($scope.availableQAWorkPage);
        if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
          $scope.retrieveAvailableConflicts($scope.availableConflictsPage);
          $scope.retrieveAvailableReviewWork($scope.availableReviewWorkPage);
        }
      });

      // on creation of qa work, refresh the available work widget
      $scope.$on('qaCheckWidget.notification.qaWorkCreated', function(event, parameters) {
        $scope.retrieveAvailableQAWork($scope.availableQAWorkPage);
      });

      // watch for first retrieval of last tab for this session
      $scope.$watch('availableTab', function() {
        // unidentified source is resetting the tab to 0 after initial load
        // introduced a brief timeout to ensure correct tab is picked
        setTimeout(function() {
          $scope.setTab($scope.availableTab);
        }, 200);

      });

      $scope.setTab = function(tabNumber) {
    	$scope.error = null;
        if (tabNumber == null)
          tabNumber = 0;
        angular.forEach($scope.tabs, function(tab) {
          tab.active = (tab.id == tabNumber ? true : false);
        });
        localStorageService.add('availableTab', tabNumber);
        $scope.availableTab = tabNumber;
      };

      $scope.addAssignment = function(name) {
        alert(name);
        return;
      };

      // on retrieval, set the user drop-down lists to the current user
      $scope.$watch([ 'currentUser' ], function() {
        $scope.assignedMapUser = $scope.currentUser;
        $scope.assignedMapLead = $scope.currentUser;
      });

      // on retrieval of either focus project or user token, try to retrieve
      // work
      $scope.$watch([ 'focusProject', 'userToken', 'currentUser', 'currentRole' ], function() {
        // both variables must be non-null
        if ($scope.focusProject != null && $scope.userToken != null && $scope.currentUser != null
          && $scope.currentRole != null) {

          // set the authorization header
          $http.defaults.headers.common.Authorization = $scope.userToken;

          // construct the list of users
          $scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
          $scope.retrieveLabels();
		  $scope.retrieveTags();
          $scope.retrieveAvailableWork($scope.availableWorkPage);
          $scope.retrieveAvailableQAWork($scope.availableQAWorkPage);
          if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Admin') {
            $scope.retrieveAvailableConflicts($scope.availableConflictsPage);
            $scope.retrieveAvailableReviewWork($scope.availableReviewWorkPage);
          }
        }
      });

      $scope.retrieveLabels = function() {
        gpService.increment();
        $http({
          url : root_reporting + 'qaLabel/qaLabels/' + $scope.focusProject.id,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          gpService.decrement();
          for (var i = 0; i < data.searchResult.length; i++) {
            $scope.labelNames.push(data.searchResult[i].value);
          }
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveTags = function() {
        gpService.increment();
        $http({
          url : root_mapping + 'tags/' + $scope.focusProject.id,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          gpService.decrement();
          for (var i = 0; i < data.searchResult.length; i++) {
			$scope.allTags.push({ id: i+1, tag: data.searchResult[i].value });
          }
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.clearSelectedTags = function() {
		$scope.selectedTags = new Array();
	  };

      //sort direction
      var sortAscending = [];
      var sortField = [];
      
      $scope.getSortIndicator = function(table, field){
		if (sortField[table] !== field) return '';
		if (sortField[table] === field && sortAscending[table]) return '▴';
		if (sortField[table] === field && !sortAscending[table]) return '▾';
      };
      
      //sort field and get data
      $scope.setSortField = function(table, field) {
    	  sortAscending[table] = !sortAscending[table];
    	  sortField[table] = field;
    	  if (table === 'concepts') {
    		  $scope.retrieveAvailableWork(
    				  1, $scope.queryAvailable, $scope.assignedMapUser);
    	  } else if (table === 'conflicts') {
    		  $scope.retrieveAvailableConflicts(
    				  1, $scope.queryAvailableConflict, $scope.assignedMapUser);
    	  } else if (table === 'review') {
    		  $scope.retrieveAvailableReviewWork(
    				  1, $scope.queryAvailableReview, $scope.assignedMapUser);
    	  } else if (table === 'qa') {
    		  $scope.retrieveAvailableQAWork(
    				  1, $scope.selectedQaLabel, $scope.assignedMapUser);
    	  }
      };
      
      $scope.retrieveAvailableConflicts = function(page, pquery, puser) {
        var query = pquery;
        var user = puser;

        // clear local conflict error message
        $scope.errorConflict = null;

		$scope.availableConflictsPage = page;

        // if user not supplied, assume current user
        if (user == null || user == undefined)
          user = $scope.currentUser;

        // clear the existing work
        $scope.availableConflicts = null;

        // set query to null if undefined
        if (query == undefined)
          query = null;

        // if null query, reset the search field
        if (query == null)
          $scope.queryAvailable = null;

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : (page - 1) * $scope.itemsPerPage,
          'maxResults' : $scope.itemsPerPage,
          'sortField' : (sortField['conflicts']) ? sortField['conflicts'] : $scope.defaultSortField,
          'ascending' : sortAscending['conflicts'],
          'queryRestriction' : null
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + user.userName + '/availableConflicts?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.availableConflicts = data.searchResult;

          // set pagination
          $scope.numAvailableConflictsPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAvailableConflicts = data.totalCount;

          // set title
          $scope.tabs[1].title = 'Conflicts (' + data.totalCount + ')';
          $scope.availableCount[1] = data.totalCount;
        }).error(function(data, status, headers, config) {
          gpService.decrement();

          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // get a page of available work
      $scope.retrieveAvailableWork = function(page, pquery, puser) {
        var query = pquery;
        var user = puser;

        // clear local error
        $scope.error = null;

		$scope.availableWorkPage = page;

        // if user not supplied, assume current user
        if (user == null || user == undefined)
          user = $scope.currentUser;

        // clear the existing work
        $scope.availableWork = null;

        // set query to null if undefined
        if (query == undefined)
          query = null;

        // if null query, reset the search field
        if (query == null)
          $scope.queryAvailable = null;

		// copy tag filter and add to query
		var pselectedTags = "";
		  for (var i = 0; i < $scope.selectedTags.length; i++) {
			for(var j = 0; j < $scope.allTags.length; j++) {
				if($scope.selectedTags[i].id == $scope.allTags[j].id){
					if(pselectedTags == ""){
						pselectedTags = $scope.allTags[j].tag;
					}
					else{
						pselectedTags = pselectedTags.concat(' AND ', $scope.allTags[j].tag);
					}
				}
			}
          }

		if(query == null){
			query = pselectedTags;
		}
		if(query != null && pselectedTags != ""){
			query = query.concat(' AND ', pselectedTags);
		}

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : (page - 1) * $scope.itemsPerPage,
          'maxResults' : $scope.itemsPerPage,
          'sortField' : (sortField['concepts']) ? sortField['concepts'] : $scope.defaultSortField,
          'ascending' : (sortAscending['concepts']) ? sortAscending['concepts'] : false,
          'queryRestriction' : null
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + user.userName + '/availableConcepts' + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();
          $scope.availableWork = data.searchResult;

          // set pagination
          $scope.numAvailableWorkPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAvailableWork = data.totalCount;

          // set title
          $scope.tabs[0].title = 'Concepts (' + data.totalCount + ')';
          $scope.availableCount[0] = data.totalCount;

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAvailableQAWork = function(page, pquery) {
        var query = pquery;
        // clear local error
        $scope.error = null;

		$scope.availableQAPage = page;
		
        // clear the existing work
        $scope.availableQAWork = null;

        // set query to null if undefined
        if (query == undefined)
          query = null;

        // if null query, reset the search field
        if (query == null)
          $scope.queryAvailable = null;

        // construct a paging/filtering/sorting object
        // if page is null, get all results
        var pfsParameterObj = {
          'startIndex' : (page - 1) * $scope.itemsPerPage,
          'maxResults' : $scope.itemsPerPage,
          'sortField' : (sortField['qa']) ? sortField['qa'] : $scope.defaultSortField,
          'ascending' : (sortAscending['qa']) ? sortAscending['qa'] : false,
          'queryRestriction' : null
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/availableQAWork'
              + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.availableQAWork = data.searchResult;

          // set pagination
          $scope.numAvailableQAWorkPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAvailableQAWork = data.totalCount;

          // set title
          $scope.tabs[3].title = 'QA (' + data.totalCount + ')';
          $scope.availableCount[3] = data.totalCount;

          // set labels
          for (var i = 0; i < $scope.availableQAWork.length; i++) {
            var concept = $scope.availableQAWork[i];

            $scope.availableQAWork[i].name = concept.value;
            $scope.availableQAWork[i].labels = concept.value2.replace(/;/g, ' ');
          }

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.removeQaWork = function(conceptId, query, page) {
        gpService.increment();

        // clear local error
        $scope.error = null;

        // call unassign for the QA user
        // TODO: Get QA user name from cached metadata or a separate REST
        // call
        $http(
          {
            url : root_workflow + 'unassign/project/id/' + $scope.focusProject.id + '/concept/id/'
              + conceptId + '/user/id/qa',
            dataType : 'json',
            data : null,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

          $scope.retrieveAvailableQAWork(page, query);

          gpService.decrement();

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.removeAllQaWork = function(pquery) {
        var query = pquery;

        gpService.increment();

        // clear local error
        $scope.error = null;

        // clear the existing work
        $scope.availableQAWork = null;

        // set query to null if undefined
        if (query == undefined)
          query = null;

        // if null query, reset the search field
        if (query == null)
          $scope.queryAvailable = null;

        // construct a blank paging/filtering/sorting object
        // unnecessary construction, but left in for possible future use
        var pfsParameterObj = {
          'startIndex' : -1,
          'maxResults' : -1,
          'sortField' : $scope.defaultSortField,
          'queryRestriction' : null
        };

        // first, get the currently available work (refresh)
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/availableQAWork'
              + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
          function(data) {
            var workToUnassign = [];
            for (var i = 0; i < data.searchResult.length; i++) {
              workToUnassign.push(data.searchResult[i].terminologyId);
            }

            $http(
              {
                // TODO Get qa user name from either previously
                // retrieved metadata
                // or a separate REST call
                url : root_workflow + 'unassign/project/id/' + $scope.focusProject.id
                  + '/user/id/qa/batch',
                dataType : 'json',
                data : workToUnassign,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {

              $scope.retrieveAvailableQAWork(1, query);

              gpService.decrement();

            }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });

          }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAvailableReviewWork = function(page, pquery, puser) {
        var query = pquery;
        var user = puser;
        // clear local review error message
        $scope.errorReview = null;

		$scope.availableReviewPage = page;
		
        // if user not supplied, assume current user
        if (user == null || user == undefined)
          user = $scope.currentUser;

        // clear the existing work
        $scope.availableReviewWork = null;

        // set query to null if undefined
        if (query == undefined)
          query = null;

        // if null query, reset the search field
        if (query == null)
          $scope.queryAvailable = null;

		// copy tag filter and add to query
		var pselectedTags = "";
		  for (var i = 0; i < $scope.selectedTags.length; i++) {
			for(var j = 0; j < $scope.allTags.length; j++) {
				if($scope.selectedTags[i].id == $scope.allTags[j].id){
					if(pselectedTags == ""){
						pselectedTags = $scope.allTags[j].tag;
					}
					else{
						pselectedTags = pselectedTags.concat(' AND ', $scope.allTags[j].tag);
					}
				}
			}
          }

		if(query == null){
			query = pselectedTags;
		}
		if(query != null && pselectedTags != ""){
			query = query.concat(' AND ', pselectedTags);
		}

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : (page - 1) * $scope.itemsPerPage,
          'maxResults' : $scope.itemsPerPage,
          'sortField' : (sortField['review']) ? sortField['review'] : $scope.defaultSortField,
          'ascending' : (sortAscending['review']) ? sortAscending['review'] : false,
          'queryRestriction' : null
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + user.userName + '/availableReviewWork' + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.availableReviewWork = data.searchResult;

          // set pagination
          $scope.numAvailableReviewWorkPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAvailableReviewWork = data.totalCount;

          // set title
          $scope.tabs[2].title = 'Review (' + data.totalCount + ')';
          $scope.availableCount[2] = data.totalCount;
        }).error(function(data, status, headers, config) {
          gpService.decrement();

          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      /**
       * assign a single concept to the current user Arguments: -
       * trackingRecord: the full search result object representing the tracking
       * record - mapUser: the full user object representing the user to assign
       * to - query: the query for this type of work, passed in to ensure
       * correct refresh of available work - workType: the type of work
       * ('concept', 'conflict', 'review', 'qa'), used for broadcasting
       * assignment query passed in to ensure correct retrieval of work
       */
      $scope.assignWork = function(trackingRecord, pmapUser, pquery, workType, workPage) {
        var mapUser = pmapUser;
        var query = pquery;

        // doublecheck map user and query, assign default values if
        // necessary
        if (mapUser == null)
          mapUser = $scope.currentUser;
        if (query == undefined)
          query = null;

        gpService.increment();

        $http(
          {
            url : root_workflow + 'assign/project/id/' + $scope.focusProject.id + '/concept/id/'
              + trackingRecord.terminologyId + '/user/id/' + mapUser.userName,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();
          $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
            assignUser : mapUser,
            assignType : workType
          });

          if (workType == 'concept') {
            $scope.retrieveAvailableWork(workPage, query, mapUser);
          } else if (workType === 'conflict') {
            $scope.retrieveAvailableConflicts(workPage, query, mapUser);
          } else if (workType === 'review') {
            $scope.retrieveAvailableReviewWork(workPage, query, mapUser);
          } else if (workType === 'qa') {
            $scope.retrieveAvailableQAWork(workPage, query, mapUser);
          }
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      // assign a batch of records to the current user
      $scope.assignBatch = function(mapUser, batchSize, query) {
        // set query to null string if not provided
        if (query == undefined)
          query == null;

        if (mapUser == null || mapUser == undefined) {
          $scope.error = 'Work recipient must be selected from list.';
          return;
        }

        if (batchSize > $scope.availableCount[$scope.availableTab]) {
          alert('Batch size is greater than available number of concepts.');
          return;
        } else {
          $scope.error = null;
        }

		// copy tag filter and add to query
		var pselectedTags = "";
		  for (var i = 0; i < $scope.selectedTags.length; i++) {
			for(var j = 0; j < $scope.allTags.length; j++) {
				if($scope.selectedTags[i].id == $scope.allTags[j].id){
					if(pselectedTags == ""){
						pselectedTags = $scope.allTags[j].tag;
					}
					else{
						pselectedTags = pselectedTags.concat(' AND ', $scope.allTags[j].tag);
					}
				}
			}
          }

		if(query == null){
			query = pselectedTags;
		}
		if(query != null && pselectedTags != ""){
			query = query.concat(' AND ', pselectedTags);
		}

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : ($scope.availableWorkPage - 1) * $scope.itemsPerPage,
          'maxResults' : batchSize,
          'sortField' : (sortField['concepts']) ? sortField['concepts'] : $scope.defaultSortField,
          'ascending' : (sortAscending['concepts']) ? sortAscending['concepts'] : false,
          'queryRestriction' : null
        };

        gpService.increment();
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + mapUser.userName + '/availableConcepts' + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
          .success(
            function(data) {
              var trackingRecords = data.searchResult;
              var conceptListValid = true;

              // if user is assigning to self, check that first result
              // matches
              // first displayed result
              if ($scope.currentUser.userName === mapUser.userName) {
                for (var i = 0; i < $scope.itemsPerPage && i < batchSize; i++) {
                  if (trackingRecords[i].id != $scope.availableWork[i].id) {
                    $scope.retrieveAvailableWork($scope.availableWorkPage, query);
                    alert('The list of available concepts has changed.  Please check the refreshed list and try again');
                    gpService.decrement();
                    return;
                    conceptListValid = false;
                  }
                }
              }

              if (conceptListValid == true) {
                var terminologyIds = [];
                for (var i = 0; i < trackingRecords.length; i++) {

                  terminologyIds.push(trackingRecords[i].terminologyId);
                }
                $http(
                  {
                    url : root_workflow + 'assignBatch/project/id/' + $scope.focusProject.id
                      + '/user/id/' + mapUser.userName,
                    dataType : 'json',
                    data : terminologyIds,
                    method : 'POST',
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                  gpService.decrement();

                  // notify other widgets of work assignment
                  $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
                    assignUser : mapUser,
                    assignType : 'concept'
                  });

                  // refresh the available work list
                  $scope.retrieveAvailableWork(1, query, mapUser);
                }).error(function(data, status, headers, config) {
                  gpService.decrement();

                  $rootScope.handleHttpError(data, status, headers, config);
                });
              } else {
                gpService.decrement();
              }
            }).error(function(data, status, headers, config) {
            gpService.decrement();

            $rootScope.handleHttpError(data, status, headers, config);
          });

      };

      // assign a batch of conflicts to the current user
      $scope.assignBatchConflict = function(mapUser, batchSize, query) {
        // set query to null string if not provided
        if (query == undefined)
          query == null;

        if (mapUser == null || mapUser == undefined) {
          $scope.errorConflict = 'Work recipient must be selected from list.';
          return;
        }
        
        if (batchSize > $scope.availableCount[$scope.availableTab]) {
        	alert('Batch size is greater than available number of concepts.');
            return;
          } else {
            $scope.error = null;
          }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : ($scope.availableConflictsPage - 1) * $scope.itemsPerPage,
          'maxResults' : batchSize,
          'sortField' : (sortField['conflicts']) ? sortField['conflicts'] : $scope.defaultSortField,
          'ascending' : (sortAscending['conflicts']) ? sortAscending['conflicts'] : false,
          'queryRestriction' : null
        };

        gpService.increment();
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + mapUser.userName + '/availableConflicts' + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
          .success(
            function(data) {
              var trackingRecords = data.searchResult;
              var conceptListValid = true;

              // if user is viewing conflicts, confirm that the returned
              // batch
              // matches the displayed conflicts
              if ($scope.currentUser.userName === mapUser.userName) {
                for (var i = 0; i < $scope.itemsPerPage && i < batchSize
                  && i < $scope.availableConflicts; i++) {
                  if (trackingRecords[i].id != $scope.availableWork[i].id) {
                    $scope.retrieveAvailableConflicts($scope.availableConflictsPage, query);
                    alert('The available conflicts list has changed since loading.  Please review the new available conflicts and try again.');
                    $scope.isConceptListOpen = false;
                    conceptListValid = false;
                  }
                }
              }

              if (conceptListValid == true) {

                var terminologyIds = [];
                for (var i = 0; i < trackingRecords.length; i++) {

                  terminologyIds.push(trackingRecords[i].terminologyId);
                }
                $http(
                  {
                    url : root_workflow + 'assignBatch/project/id/' + $scope.focusProject.id
                      + '/user/id/' + mapUser.userName,
                    dataType : 'json',
                    data : terminologyIds,
                    method : 'POST',
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                  gpService.decrement();

                  // broadcast the work assignment
                  $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
                    assignUser : mapUser,
                    assignType : 'conflict'
                  });

                  // refresh the displayed list of conflicts
                  $scope.retrieveAvailableConflicts(1, query, mapUser);
                }).error(function(data, status, headers, config) {
                  gpService.decrement();
                  $rootScope.handleHttpError(data, status, headers, config);
                });
              } else {
                gpService.decrement();
              }
            }).error(function(data, status, headers, config) {
            gpService.decrement();

            $rootScope.handleHttpError(data, status, headers, config);
          });

      };

      // assign a batch of review work to the current user
      $scope.assignBatchReview = function(mapUser, batchSize, query) {

        // set query to null string if not provided
        if (query == undefined)
          query == null;

        if (mapUser == null || mapUser == undefined) {
          $scope.errorReview = 'Work recipient must be selected from list.';
          return;
        }
        
        if (batchSize > $scope.availableCount[$scope.availableTab]) {
            alert('Batch size is greater than available number of concepts.');
            return;
          } else {
            $scope.error = null;
          }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : ($scope.availableReviewPage - 1) * $scope.itemsPerPage,
          'maxResults' : batchSize,
          'sortField' : (sortField['review']) ? sortField['review'] : $scope.defaultSortField,
          'ascending' : (sortAscending['review']) ? sortAscending['review'] : false,
          'queryRestriction' : null
        };

        gpService.increment();
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + mapUser.userName + '/availableReviewWork' + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
          .success(
            function(data) {
              var trackingRecords = data.searchResult;
              var conceptListValid = true;

              // if user is viewing conflicts, confirm that the returned
              // batch
              // matches the displayed conflicts
              if ($scope.currentUser.userName === mapUser.userName) {
                for (var i = 0; i < $scope.itemsPerPage && i < batchSize
                  && i < $scope.availableReviewWork; i++) {

                  if (trackingRecords[i].id != $scope.availableReviewWork[i].id) {
                    $scope.retrieveAvailableReviewWork($scope.availableReviewPage, query);
                    alert('The list of available review work has changed.  Please check the refreshed list and try again.');
                    $scope.isConceptListOpen = false;
                    conceptListValid = false;
                  }
                }
              }

              if (conceptListValid == true) {

                var terminologyIds = [];
                for (var i = 0; i < trackingRecords.length; i++) {

                  terminologyIds.push(trackingRecords[i].terminologyId);
                }

                $http(
                  {
                    url : root_workflow + 'assignBatch/project/id/' + $scope.focusProject.id
                      + '/user/id/' + mapUser.userName,
                    dataType : 'json',
                    data : terminologyIds,
                    method : 'POST',
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                  gpService.decrement();

                  // broadcast the work assignment
                  $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
                    assignUser : mapUser,
                    assignType : 'review'
                  });

                  // refresh the displayed list of conflicts
                  $scope.retrieveAvailableReviewWork(1, query, mapUser);
                }).error(function(data, status, headers, config) {
                  gpService.decrement();
                  $rootScope.handleHttpError(data, status, headers, config);
                });
              } else {
                gpService.decrement();
              }
            }).error(function(data, status, headers, config) {
            gpService.decrement();

            $rootScope.handleHttpError(data, status, headers, config);
          });

      };

      // assign a batch of qa work to the current user
      $scope.assignBatchQA = function(mapUser, batchSize, query) {

        // set query to null string if not provided
        if (query == undefined)
          query == null;

        if (mapUser == null || mapUser == undefined) {
          $scope.errorReview = 'Work recipient must be selected from list.';
          return;
        }
        
        if (batchSize > $scope.availableCount[$scope.availableTab]) {
            alert('Batch size is greater than available number of concepts.');
            return;
          } else {
            $scope.error = null;
          }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : ($scope.availableQAWorkPage - 1) * $scope.itemsPerPage,
          'maxResults' : batchSize,
          'sortField' : (sortField['qa']) ? sortField['qa'] : $scope.defaultSortField,
          'ascending' : (sortAscending['qa']) ? sortAscending['qa'] : false,
          'queryRestriction' : null
        };

        gpService.increment();
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/availableQAWork'
              + '?query=' + (query ? encodeURIComponent(query) : 'null'),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
          .success(
            function(data) {
              var trackingRecords = data.searchResult;
              var conceptListValid = true;

              // if user is viewing conflicts, confirm that the returned
              // batch
              // matches the displayed conflicts
              if ($scope.currentUser.userName === mapUser.userName) {
                for (var i = 0; i < $scope.itemsPerPage && i < batchSize
                  && i < $scope.availableQAWork; i++) {

                  if (trackingRecords[i].id != $scope.availableQAWork[i].id) {
                    retrieveAvailableQAWork($scope.availableQAWorkPage, query);
                    alert('The list of available QA work has changed.  Please check the refreshed list and try again.');
                    $scope.isConceptListOpen = false;
                    conceptListValid = false;
                  }
                }
              }

              if (conceptListValid == true) {
                var terminologyIds = [];
                for (var i = 0; i < trackingRecords.length; i++) {

                  terminologyIds.push(trackingRecords[i].terminologyId);
                }

                $http(
                  {
                    url : root_workflow + 'assignBatch/project/id/' + $scope.focusProject.id
                      + '/user/id/' + mapUser.userName,
                    dataType : 'json',
                    data : terminologyIds,
                    method : 'POST',
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                  gpService.decrement();

                  // broadcast the work assignment
                  $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
                    assignUser : mapUser,
                    assignType : 'qa'
                  });

                  // refresh the displayed list of qa items
                  $scope.retrieveAvailableQAWork(1, query, mapUser);
                }).error(function(data, status, headers, config) {
                  gpService.decrement();
                  $rootScope.handleHttpError(data, status, headers, config);
                });
              } else {
                gpService.decrement();
              }
            }).error(function(data, status, headers, config) {
            gpService.decrement();

            $rootScope.handleHttpError(data, status, headers, config);
          });

      };

      // remove an element from an array by key
      Array.prototype.removeElement = function(elem) {

        // field to switch on
        var idType = 'id';

        var array = new Array();
        $.map(this, function(v, i) {
          if (v[idType] != elem[idType])
            array.push(v);
        });

        this.length = 0; // clear original array
        this.push.apply(this, array); // push all elements except the one we
        // want to delete
      };

      // sort and return an array by string key
      function sortByKey(array, key) {
        return array.sort(function(a, b) {
          var x = a[key];
          var y = b[key];
          return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });
      }
    });
