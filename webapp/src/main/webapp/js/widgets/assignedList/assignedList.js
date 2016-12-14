'use strict';

angular
  .module('mapProjectApp.widgets.assignedList', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('assignedList', {
      title : 'Assigned Work',
      description : 'Displays a list of assigned records',
      controller : 'assignedListCtrl',
      templateUrl : 'js/widgets/assignedList/assignedList.html',
      edit : {}
    });
  })
  .controller(
    'assignedListCtrl',
    function($scope, $rootScope, $http, $location, $uibModal, localStorageService) {

      // on initialization, explicitly assign to null and/or empty array
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.focusProject = null;
      $scope.assignedTab = null;
      $scope.currentUserToken = null;
      $scope.assignedRecords = [];

      // retrieve the necessary scope variables from local storage service
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.focusProject = localStorageService.get('focusProject');
      $scope.currentUserToken = localStorageService.get('userToken');
      $scope.assignedTab = localStorageService.get('assignedTab');

      // tab variables
      $scope.tabs = [ {
        id : 0,
        title : 'Concepts',
        active : false
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
        title : 'By User',
        active : false
      }, {
        id : 4,
        title : 'QA',
        active : false
      } ];

      // labels for QA filtering
      $scope.labelNames = [];

      // table sort fields - currently unused
      $scope.tableFields = [ {
        id : 0,
        title : 'id',
        sortDir : 'asc',
        sortOn : false
      } ];

      $scope.mapUserViewed == null;
      $scope.ownTab = true; // variable to track whether viewing own work or
      // other users work
      $scope.searchPerformed = false; // initialize variable to track
      // whether
      // search was performed

      // function to change tab
      $scope.setTab = function(tabNumber) {
        if (tabNumber == null) {
          tabNumber = 0;
        }

        angular.forEach($scope.tabs, function(tab) {
          tab.active = (tab.id == tabNumber ? true : false);
        });

        // set flag for ByUser tab, i.e. whether viewing user's own work
        if (tabNumber == 3)
          $scope.ownTab = false;
        else
          $scope.ownTab = true;

        // add the tab to the loocal storage service for the next visit
        localStorageService.add('assignedTab', tabNumber);
      };

      // pagination variables
      $scope.itemsPerPage = 10;
      $scope.assignedWorkPage = 1;
      $scope.assignedConflictsPage = 1;
      $scope.assignedReviewWorkPage = 1;
      $scope.assignedQaWorkPage = 1;
      $scope.assignedWorkForUserPage = 1;

      // query variables
      $scope.queryAssigned = null;
      $scope.queryConflict = null;
      $scope.queryReviewWork = null;
      $scope.queryQaWork = null;
      $scope.queryAssignedForUser = null;

      // work type filter variables
      $scope.assignedWorkType = 'NEW'; // initialize variable to track
      $scope.assignedConflictType = 'CONFLICT_NEW'; // initialize variable
      $scope.assignedReviewWorkType = 'REVIEW_NEW';
      $scope.assignedWorkForUserType = 'ALL'; // initialize variable to
      $scope.assignedQAWorkType = 'QA_NEW';

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        console.debug('on focus project change');
        $scope.focusProject = parameters.focusProject;
      });

      // watch for first retrieval of last tab for this session
      $scope.$watch('assignedTab', function() {
        console.debug('watch assigned tab', $scope.assignedTab);

        // unidentified source is resetting the tab to 0 after initial load
        // introduced a brief timeout to ensure correct tab is picked
        setTimeout(function() {
          $scope.setTab($scope.assignedTab);
        }, 200);

      });

      // Event on assigned work being ready
      $scope.$on('workAvailableWidget.notification.assignWork', function(event, parameters) {
        console.debug('on available work ready', parameters);

        // perform action based on notification parameters
        // Expect:
        // - assignUser: String, IHTSDO username (e.g. dmo, kli)
        // - assignType: String, either 'concept' or 'conflict' or
        // 'review' or 'qa'
        if ($scope.currentRole === 'Lead') {

          // if user name matches current user's user name, reload
          // work
          if (parameters.assignUser.userName === $scope.currentUser.userName) {

            if (parameters.assignType === 'concept') {

              // set the tab
              $scope.setTab(0);

              // retrieve the work
              $scope.retrieveAssignedWork($scope.assignedWorkPage, $scope.queryAssigned,
                $scope.assignedWorkType);

              // Conflicts
            } else if (parameters.assignType === 'conflict') {
              // set the tab
              $scope.setTab(1);

              // retrieve the work
              $scope.retrieveAssignedConflicts($scope.assignedConflictPage, $scope.queryConflict,
                $scope.assignedConflictType);

              // Review Work
            } else if (parameters.assignType === 'review') {
              // set the tab
              $scope.setTab(2);

              // retrieve the work
              $scope.retrieveAssignedReviewWork($scope.assignedReviewWorkPage,
                $scope.queryReviewWork, $scope.assignedReviewWorkType);

            } else if (parameters.assignType === 'conceptsByUser') {

              // set the tab
              $scope.setTab(3);

              // retrieve the work
              $scope.retrieveAssignedWorkForUser($scope.assignedWorkForUserPage,
                parameters.assignUser.userName, $scope.queryAssignedForUser,
                $scope.assignedWorkForUserType);

              // QA Work
            } else if (parameters.assignType === 'qa') {
              // set the tab
              $scope.setTab(4);

              // retrieve the work
              $scope.retrieveAssignedQAWork($scope.assignedQaWorkPage, $scope.queryQaWork,
                $scope.assignedQaWorkType);
            }
          } else {

            // set the tab
            $scope.setTab(4);

            $scope.retrieveAssignedWorkForUser($scope.assignedWorkForUserPage,
              parameters.assignUser.userName, 'NEW');
            $scope.mapUserViewed = parameters.assignUser;
            $scope.assignedWorkForUserType = 'NEW';
          }

          // SPECIALIST TABS
        } else {

          if (parameters.assignType === 'concept') {

            // set the tab
            $scope.setTab(0);

            // retrieve the work
            $scope.retrieveAssignedWork($scope.assignedWorkPage, $scope.assignedWorkQuery,
              $scope.assignedWorkType);
          } else if (parameters.assignType === 'qa') {
            // set the tab
            $scope.setTab(4);

            // retrieve the work
            $scope.retrieveAssignedQAWork($scope.assignedQaWorkPage, $scope.assignedQaWorkQuery,
              $scope.assignedQaWorkType);
          }
        }
      });

      // on any change of relevant scope variables, retrieve work
      $scope.$watch([ 'focusProject', 'user', 'userToken', 'currentRole' ], function() {
        console.debug('on focusProject ready');

        if ($scope.focusProject != null && $scope.currentUser != null
          && $scope.currentUserToken != null && $scope.currentRole != null) {
          $http.defaults.headers.common.Authorization = $scope.currentUserToken;

          $scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
          $scope.retrieveAssignedWork($scope.assignedWorkPage, null, $scope.assignedWorkType);
          $scope.retrieveAssignedQAWork(1, null, $scope.assignedQAWorkType);
          $scope.retrieveLabels();
          if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
            $scope.retrieveAssignedConflicts(1, null, $scope.assignedConflictType);
            $scope.retrieveAssignedReviewWork(1, null, $scope.assignedReviewWorkType);
            $scope.retrieveAssignedWorkForUser(1, null, $scope.mapUserViewed,
              $scope.assignedWorkForUserType);
          }
        }
      });

      $scope.retrieveAssignedConflicts = function(page, query, assignedConflictType) {

        // hard set the conflict PFS scope variables
        $scope.assignedConflictsPage = page;
        $scope.assignedConflictsType = assignedConflictType;
        $scope.queryConflict = query;

        // ensure query is set to null if not specified
        if (query == undefined)
          query == null;

        // set global search performed varaiable based on query
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;
        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : 'sortKey',
          'queryRestriction' : assignedConflictType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedConflicts?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;

          $scope.assignedConflictsPage = page;
          $scope.assignedConflicts = data.searchResult;

          // set pagination
          $scope.nAssignedConflicts = data.totalCount;
          $scope.numAssignedConflictsPages = Math.ceil(data.totalCount / $scope.itemsPerPage);

          // set title
          $scope.tabs[1].title = 'Conflicts (' + data.totalCount + ')';

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;

          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedWork = function(page, pquery, assignedWorkType) {
        var query = pquery;
        // set the scope variables
        // this is necessary due to some frustrating non-functional two-way
        // binding
        $scope.assignedWorkPage = page;
        $scope.queryAssigned = query;
        $scope.assignedWorkType = assignedWorkType;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search input box if null
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;

        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : 'sortKey',
          'queryRestriction' : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedConcepts?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;

          $scope.assignedWorkPage = page;
          $scope.assignedRecords = data.searchResult;

          // set pagination
          $scope.numAssignedRecordPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedRecords = data.totalCount;

          // set title
          $scope.tabs[0].title = 'Concepts (' + $scope.nAssignedRecords + ')';

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveLabels = function() {

        $rootScope.glassPane++;
        $http({
          url : root_reporting + 'qaLabel/qaLabels/' + $scope.focusProject.id,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          $rootScope.glassPane--;
          for (var i = 0; i < data.searchResult.length; i++) {
            $scope.labelNames.push(data.searchResult[i].value);
          }

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedQAWork = function(page, pquery, assignedWorkType) {
        var query = pquery;
        // hard set the PFS variables
        $scope.assignedQaWorkPage = page;
        $scope.assignedQaWorkType = assignedWorkType;
        $scope.queryQaWork = query;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search input box if null
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;

        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : 'sortKey',
          'queryRestriction' : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedQAWork?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;

          $scope.assignedQAWorkPage = page;
          $scope.assignedQAWork = data.searchResult;

          // set pagination
          $scope.numAssignedRecordPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedQAWork = data.totalCount;

          // set title
          $scope.tabs[4].title = 'QA (' + $scope.nAssignedQAWork + ')';

          // set labels
          for (var i = 0; i < $scope.assignedQAWork.length; i++) {
            var concept = $scope.assignedQAWork[i];

            $scope.assignedQAWork[i].name = concept.value;
            $scope.assignedQAWork[i].labels = concept.value2.replace(/;/g, ' ');
          }

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedReviewWork = function(page, pquery, assignedWorkType) {
        var query = pquery;

        // hard set the PFS variables
        $scope.assignedReviewWorkPage = page;
        $scope.assignedReviewWorkType = assignedWorkType;
        $scope.queryReviewWork = query;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search input box if null
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;

        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : 'sortKey',
          'queryRestriction' : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedReviewWork?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;

          $scope.assignedReviewWorkPage = page;
          $scope.assignedReviewWork = data.searchResult;

          // set pagination
          $scope.numAssignedRecordPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedReviewWork = data.totalCount;

          // set title
          $scope.tabs[2].title = 'Review (' + $scope.nAssignedReviewWork + ')';

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedWorkForUser = function(page, mapUserName, query, assignedWorkType) {

        // hard set the PFS variables
        $scope.assignedWorkForUserPage = page;
        $scope.assignedWorkForUserType = assignedWorkType;
        $scope.queryWorkForUser = query;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search box if query is null
        if (query == null) {
          $scope.queryAssignedForUser = null;
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;
        }

        // if no user specified, set to empty record set, with appropriate
        // pagination variables
        if (mapUserName == null) {
          $scope.assignedWorkForUserPage = 1;
          $scope.assignedRecordsForUser = {};

          // set pagination
          $scope.numAssignedRecordPagesForUser = 0;
          $scope.nAssignedRecordsForUser = 0;

          // set title
          $scope.tabs[3].title = 'By User';

          return;
        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : 'sortKey',
          'queryRestriction' : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + mapUserName + '/assignedConcepts?query=' + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
          .success(
            function(data) {
              $rootScope.glassPane--;

              $scope.assignedWorkForUserPage = page;
              $scope.assignedRecordsForUser = data.searchResult;

              // set pagination
              $scope.numAssignedRecordPagesForUser = Math.ceil(data.totalCount
                / $scope.itemsPerPage);
              $scope.nAssignedRecordsForUser = data.totalCount;
              $scope.numRecordPagesForUser = Math.ceil($scope.nAssignedRecordsForUser
                / $scope.itemsPerPage);

              $scope.tabs[3].title = 'By User (' + data.totalCount + ')';

            }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });

      };

      // set the pagination variables
      function setPagination(assignedRecordsPerPage, nAssignedRecords) {

        $scope.assignedRecordsPerPage = assignedRecordsPerPage;
        $scope.numRecordPages = Math.ceil($scope.nAssignedRecords / assignedRecordsPerPage);
      }

      // on notification, update assigned work
      $scope.assignWork = function(newRecords) {

        $scope.retrieveAssignedWork($scope.assignedWorkPage);
        if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
          $scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
        }
      };

      // function to relinquish work (i.e. unassign the user)
      $scope.unassignWork = function(record, mapUser, workType) {

        // show a confirmation dialog if requested
        // NOTE: workflow status is contained in terminologyVersion for a
        // searchResult object
        if (record.terminologyVersion === 'EDITING_DONE'
          || record.terminologyVersion === 'REVIEW_RESOLVED'
          || record.terminologyVersion === 'QA_RESOLVED'
          || record.terminologyVersion === 'CONFLICT_RESOLVED') {
          var response = confirm('Are you sure you want to return finished work?  You will lose any work done.');
          if (response == false)
            return;
        }

        $rootScope.glassPane++;
        $http(
          {
            url : root_workflow + 'unassign/project/id/' + $scope.focusProject.id + '/concept/id/'
              + record.terminologyId + '/user/id/' + mapUser.userName,
            dataType : 'json',
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

          $rootScope.glassPane--;

          // trigger reload of this type of work via broadcast
          // notification
          $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
            assignUser : mapUser,
            assignType : workType,
            resetFilters : false
          });

          // if this user unassigned their own work, broadcast
          // unassign
          if (mapUser.userName === $scope.currentUser.userName)
            $rootScope.$broadcast('assignedListWidget.notification.unassignWork');

          // if this user is viewing their assigned concepts via the By User
          // tab, re-retrieve

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // Unassigns all currently viewed work (by tab, query, and workflow
      // status)
      // Parameters:
      // user: the map user to unassign (may not be the current user)
      // workType: the type of work, e.g. Concepts, Conflicts, Review...
      // workStatus: the currently selected workflow status, e.g. All, New,
      // Editing...
      // query: any text filter currently applied
      $scope.unassignAllWork = function(user, workType, workStatus, query) {

        if (confirm('Are you sure you want to return all displayed work?') == false)
          return;

        // get the full list of currently assigned work for this query and
        // workType
        $rootScope.glassPane++;
        var pfsParameterObj = {
          'startIndex' : -1,
          'maxResults' : -1,
          'sortField' : 'sortKey',
          'queryRestriction' : workStatus
        };

        var workTypeText = null;
        switch (workType) {
        case 'concept':
          workTypeText = 'assignedConcepts';
          break;
        case 'conflict':
          workTypeText = 'assignedConflicts';
          break;
        case 'review':
          workTypeText = 'assignedReviewWork';
          break;
        case 'qa':
          workTypeText = 'assignedQAWork';
          break;

        }

        // retrieve the list of assigned work
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + user.userName + '/' + workTypeText + '?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

          var terminologyIds = new Array();
          for (var i = 0; i < data.searchResult.length; i++) {
            terminologyIds.push(data.searchResult[i].terminologyId);
          }
          unassignBatch(user, terminologyIds, workType);

          $rootScope.glassPane--;

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      $scope.setOwnTab = function(ownTab) {
        $scope.ownTab = ownTab;
      };

      var unassignBatch = function(mapUser, terminologyIds, workType, workStatus) {

        $rootScope.glassPane++;
        $http(
          {
            url : root_workflow + 'unassign/project/id/' + $scope.focusProject.id + '/user/id/'
              + mapUser.userName + '/batch',
            dataType : 'json',
            data : terminologyIds,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        }).then(function() {

          // trigger reload of this type of work via broadcast
          // notification
          $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
            assignUser : mapUser,
            assignType : workType,
            assignWorkflowStatus : workStatus
          });

          // if this user unassigned their own work, broadcast
          // unassign
          if (mapUser.userName === $scope.currentUser.userName)
            $rootScope.$broadcast('assignedListWidget.notification.unassignWork');

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

      $scope.goEditRecord = function(id) {
        var path = '/record/recordId/' + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditConflict = function(id) {
        var path = '/record/conflicts/' + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditReviewWork = function(id) {
        var path = '/record/review/' + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditQAWork = function(id) {
        var path = '/record/review/' + id;
        // redirect page
        $location.path(path);
      };

      /**
       * Helper function to open Finish Or Publish modal (record in form of
       * search result from assigned list
       */
      $scope.finishOrPublish = function(searchResult) {

        // convert single search result into a single-element array
        var records = [];
        records.push(searchResult);

        $scope.openFinishOrPublishModal(records);
      };

      /**
       * Function to open finish or publish modal. Argument: workflowStatus: The
       * filter by which to retrieve records Must be a valid workflow status of
       * type *_IN_PROGRESS or *_RESOLVED
       */
      $scope.finishOrPublishBatch = function(workflowStatus) {

        // check arguments
        if (workflowStatus != null && workflowStatus.indexOf('_IN_PROGRESS') == -1
          && workflowStatus.indexOf('_RESOLVED') == -1) {
          console
            .error('Invalid workflow status passed to finishOrPublish, must be *_IN_PROGRESS or *_RESOLVED');
        }
        // determine type of work
        var apiWorkflowText;

        // determine the retrieval API text
        // i.e.. whether to call assignedConcepts,
        // assignedReviewWork, or assignedConcepts
        if (workflowStatus === 'CONFLICT_IN_PROGRESS' || workflowStatus === 'CONFLICT_RESOLVED')
          apiWorkflowText = 'assignedConflicts';
        else if (workflowStatus === 'REVIEW_IN_PROGRESS' || workflowStatus === 'REVIEW_RESOLVED')
          apiWorkflowText = 'assignedReviewWork';
        else if (workflowStatus === 'QA_IN_PROGRESS' || workflowStatus === 'QA_RESOLVED')
          apiWorkflowText = 'assignedQAWork';
        else if (workflowStatus === 'EDITING_IN_PROGRESS' || workflowStatus === 'EDITING_DONE')
          apiWorkflowText = 'assignedConcepts';
        else {
          return;
        }

        // construct a paging/filtering/sorting object based on work type
        var pfsParameterObj = {
          'startIndex' : -1,
          'maxResults' : -1,
          'sortField' : null,
          'queryRestriction' : workflowStatus
        };

        $rootScope.glassPane++;
        // set based on specified workflow status
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/' + apiWorkflowText,
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

          // if results found, open the modal
          if (data.searchResult.length > 0) {
            $scope.openFinishOrPublishModal(data.searchResult);
          }
          $rootScope.glassPane--;
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      $scope.openFinishOrPublishModal = function(records) {

        if (records == null || records.length == 0) {
          console.error('openPerformBatchActionModal called with no records');
          return;
        }

        // NOTE: Record information is shoehorned into searchResult
        // workflow status is contained in terminologyVersion
        var workflowStatus = records[0].terminologyVersion;
        var modalInstance = $uibModal
          .open({
            templateUrl : 'js/widgets/assignedList/assignedListFinishOrPublish.html',
            controller : FinishOrPublishWorkModalCtrl,
            size : 'lg',
            resolve : {
              records : function() {
                return records;
              },
              project : function() {
                return $scope.focusProject;
              },
              user : function() {
                return $scope.currentUser;
              },
              action : function() {

                // catch simple workflow case
                if ($scope.focusProject.workflowType === 'SIMPLE_PATH') {
                  return 'publish';
                }

                // otherwise, distinguish between lead work and specialist work
                return (workflowStatus === 'CONFLICT_RESOLVED'
                  || workflowStatus === 'REVIEW_RESOLVED' || workflowStatus === 'QA_RESOLVED') ? 'publish'
                  : 'finish';
              }
            }
          });

        modalInstance.result
          .then(

            // called on Done clicked by user
            function() {
              if (workflowStatus === 'CONFLICT_IN_PROGRESS'
                || workflowStatus === 'CONFLICT_RESOLVED') {
                $scope.retrieveAssignedConflicts(1, null, workflowStatus); // called
                // on
                // Done
              } else if (workflowStatus === 'REVIEW_IN_PROGRESS'
                || workflowStatus === 'REVIEW_RESOLVED') {
                if ($scope.currentRole === 'Lead') {
                  $scope.retrieveAssignedReviewWork(1, null, workflowStatus); // called
                  // on
                  // Done
                }
              } else if (workflowStatus === 'QA_IN_PROGRESS' || workflowStatus === 'QA_RESOLVED') {
                $scope.retrieveAssignedQAWork(1, null, workflowStatus); // called
                // on
                // Done
              } else if (workflowStatus === 'EDITING_IN_PROGRESS'
                || workflowStatus === 'EDITING_DONE') {
                $scope.retrieveAssignedWork(1, null, workflowStatus); // called
                // on
                // Done
              } else {
                return;
              }

              // called on Cancel/Escape, same functionality
            }, function() {
              if (workflowStatus === 'CONFLICT_IN_PROGRESS'
                || workflowStatus === 'CONFLICT_RESOLVED') {
                $scope.retrieveAssignedConflicts(1, null, workflowStatus); // called
                // on
                // Done
              } else if (workflowStatus === 'REVIEW_IN_PROGRESS'
                || workflowStatus === 'REVIEW_RESOLVED') {
                if ($scope.currentRole === 'Lead') {
                  $scope.retrieveAssignedReviewWork(1, null, workflowStatus); // called
                  // on
                  // Done
                }
              } else if (workflowStatus === 'QA_IN_PROGRESS' || workflowStatus === 'QA_RESOLVED') {
                $scope.retrieveAssignedQAWork(1, null, workflowStatus);
              } else if (workflowStatus === 'EDITING_IN_PROGRESS'
                || workflowStatus === 'EDITING_DONE') {
                $scope.retrieveAssignedWork(1, null, workflowStatus); // called
                // on
                // Done
              } else {
                return;
              }
            });

      };

      var FinishOrPublishWorkModalCtrl = function($scope, $uibModalInstance, $q, user, project,
        records, action) {

        $scope.user = user;
        $scope.project = project;
        $scope.records = records;
        $scope.action = action;
        $scope.index = 1; // note this index is in range [1, n], where n is
        // the
        // number of records

        // set the display text based on action
        if (action === 'finish') {
          $scope.actionText = 'Finish';
        } else if (action === 'publish') {
          $scope.actionText = 'Publish';
        }

        // Select the next record in the records array
        $scope.selectNextRecord = function() {
          var deferred = $q.defer();
          $scope.index = $scope.index == $scope.records.length ? 1 : $scope.index + 1;
          $scope.loadRecord().then(function() {
            deferred.resolve();
          }, function() {
            deferred.reject();
          });
          return deferred.promise;

        };

        // Load the current record
        $scope.loadRecord = function() {

          var deferred = $q.defer();
          $scope.validationResult = null;

          // get id from list (note index is range [1,n], subtract one for
          // array
          // access)
          var recordId = $scope.records[$scope.index - 1].id;
          $rootScope.glassPane++;
          // perform the retrieval call
          $http({
            url : root_mapping + 'record/id/' + recordId,
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
            function(data) {

              // do not close glass pane here, validate record first

              // set scope record
              $scope.currentRecord = data;

              // check if this record is still in progress, based on
              // requested
              // action
              if ($scope.action === 'publish') {

                // if in a publication state, this record has been
                // finished
                if ($scope.currentRecord.workflowStatus === 'READY_FOR_PUBLICATION'
                  || $scope.currentRecord.workflowStatus === 'PUBLISHED')
                  $scope.currentRecord.isFinished = true;
                else
                  $scope.currentRecord.isFinished = false;

              } else if ($scope.action === 'finish') {

                // if an *_IN_PROGRESS record, not finished
                if ($scope.currentRecord.workflowStatus === 'EDITING_IN_PROGRESS'
                  || $scope.currentRecord.workflowStatus === 'CONFLICT_IN_PROGRESS'
                  || $scope.currentRecord.workflowStatus === 'REVIEW_IN_PROGRESS'
                  || $scope.currentRecord.workflowStatus === 'QA_IN_PROGRESS') {
                  $scope.currentRecord.isFinished = false;
                }

                // If using "editing done" as a pre-step before review
                else if ($scope.currentRecord.workflowStatus === 'EDITING_DONE') {
                  $scope.currentRecord.isFinished = false;
                }

                // otherwise, this record has been finished/published
                // via this
                // modal
                else {
                  $scope.currentRecord.isFinished = true;
                }
              }

              // validate the record
              $http({
                url : root_mapping + 'validation/record/validate',
                dataType : 'json',
                data : $scope.currentRecord,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                $rootScope.glassPane--;
                $scope.validationResult = data;
                deferred.resolve($scope.currentRecord);
              }).error(function(data, status, headers, config) {
                $rootScope.glassPane--;
                $scope.validationResult = null;
                $scope.recordError = 'Unexpected error reported by server.  Contact an admin.';
                $rootScope.handleHttpError(data, status, headers, config);
                deferred.reject('Map record failed validation');
              });

            }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $scope.error = 'Could not retrieve record';
            deferred.reject('Could not retrieve record');
          });

          return deferred.promise;
        };

        $scope.finishCurrentRecord = function() {

          if ($scope.validationResult.valid == true && $scope.currentRecord.isFinished == false) {
            finishRecord($scope.currentRecord).then(function(response) {

              // flag this record as finished
              $scope.currentRecord.isFinished = true;

              // if this was the only record, close the modal
              if ($scope.records.length == 1) {
                $scope.done();
              } else {
                $scope.selectNextRecord();
              }
            }, function(response) {
              $scope.error('Unexpected error finishing record.');
            });
          }
        };

        // helper function to return a promise resolved
        // or rejected when finish/publish call is complete
        function finishRecord(record) {
          var deferred = $q.defer();

          $rootScope.glassPane++;

          $http({
            url : root_workflow + $scope.action, // api text is passed in
            // as
            // argument
            dataType : 'json',
            data : record,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $rootScope.glassPane--;
            deferred.resolve();

          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            deferred.reject();
          });

          return deferred.promise;

        }

        // function to finish the current record,
        // wait for completion, then continue
        function finishAllRecordsHelper() {

          // select the next record
          $scope.selectNextRecord().then(function() {
            if ($scope.validationResult.valid == true && $scope.currentRecord.isFinished == false) {

              // finish the record, then WAIT for the promise to resolve
              finishRecord($scope.currentRecord).then(
              // success function
              function() {
                $scope.recordsFinished++;

                // flag current record as finished
                $scope.currentRecord.isFinished = true;

                // call the helper again if more records
                if ($scope.index < $scope.records.length)
                  finishAllRecordsHelper();

                // error function
              }, function() {
                $scope.recordsNotFinished++;

                // call the helper again if more records
                if ($scope.index < $scope.records.length)
                  finishAllRecordsHelper();

              });
            } else {

              // increment counter only if this record is not already
              // finished
              if ($scope.currentRecord.isFinished == false)
                $scope.recordsNotFinished++;

              // call the helper again if more records
              if ($scope.index < $scope.records.length)
                finishAllRecordsHelper();
            }
          });

        }

        $scope.finishAllRecords = function() {
          // set index to before the first record
          $scope.index = 0;

          // instantiate the reporting counters
          $scope.recordsFinished = 0;
          $scope.recordsNotFinished = 0;

          // call the sequential finishAllRecords helper function
          finishAllRecordsHelper();
        };

        $scope.done = function() {
          $uibModalInstance.close();
        };

        // get the first record
        $scope.loadRecord($scope.index);
      };
    });
