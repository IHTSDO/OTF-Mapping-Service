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
    function($scope, $rootScope, $http, $location, $modal, localStorageService) {

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
      $scope.searchPerformed = false; // initialize variable to track whether
      // search was performed

      // initial work types for each tab
      $scope.assignedWorkType = 'NEW'; // initialize variable to track which
      // type of work has been requested
      $scope.assignedConflictType = 'CONFLICT_NEW'; // initialize variable to
      // track which type of
      // conflict has been
      // requested
      $scope.assignedReviewWorkType = 'REVIEW_NEW';
      $scope.assignedWorkForUserType = 'ALL'; // initialize variable to track
      // which type of work (for another
      // user) has been requested
      $scope.assignedQAWorkType = 'QA_NEW';

      // function to change tab
      $scope.setTab = function(tabNumber) {
        if (tabNumber == null)
          tabNumber = 0;
        console.debug("Switching to tab " + tabNumber);

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

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(
        event, parameters) {
        console
          .debug("MapProjectWidgetCtrl:  Detected change in focus project");
        $scope.focusProject = parameters.focusProject;
      });

      // watch for first retrieval of last tab for this session
      $scope.$watch('assignedTab', function() {
        console.debug('assignedTab retrieved', $scope.assignedTab);

        // unidentified source is resetting the tab to 0 after initial load
        // introduced a brief timeout to ensure correct tab is picked
        setTimeout(function() {
          $scope.setTab($scope.assignedTab);
        }, 200);

      });

      $scope
        .$on(
          'workAvailableWidget.notification.assignWork',
          function(event, parameters) {
            console.debug('assignedlist: assignWork notification');
            console.debug(parameters);
            console.debug($scope.currentRole);

            // perform action based on notification parameters
            // Expect:
            // - assignUser: String, IHTSDO username (e.g. dmo, kli)
            // - assignType: String, either 'concept' or 'conflict' or 'review'
            // or 'qa'
            if ($scope.currentRole === 'Lead') {

              // if user name matches current user's user name, reload work
              if (parameters.assignUser.userName === $scope.currentUser.userName) {

                if (parameters.assignType === 'concept') {
                  $scope.retrieveAssignedWork($scope.assignedWorkPage, null,
                    'NEW');
                  $scope.setTab(0);
                  $scope.assignedWorkType = 'NEW';
                } else if (parameters.assignType === 'conflict') {
                  $scope.retrieveAssignedConflicts(
                    $scope.assignedConflictsPage, null, 'CONFLICT_NEW');
                  $scope.setTab(1);
                  $scope.assignedConflictType = 'CONFLICT_NEW';
                } else if (parameters.assignType === 'review') {
                  $scope.retrieveAssignedReviewWork(
                    $scope.assignedReviewWorkPage, null, 'REVIEW_NEW');
                  $scope.setTab(2);
                  $scope.assignedReviewWorkType = 'REVIEW_NEW';
                } else if (parameters.assignType === 'qa') {
                  $scope.retrieveAssignedQAWork($scope.assignedQAWorkPage,
                    null, 'QA_NEW');
                  $scope.setTab(4);
                  $scope.assignedQAWorkType = 'QA_NEW';
                }
              } else {
                $scope.retrieveAssignedWorkForUser(
                  $scope.assignedWorkForUserPage,
                  parameters.assignUser.userName, 'NEW');
                $scope.setTab(3);
                $scope.mapUserViewed = parameters.assignUser;
                $scope.assignedWorkForUserType = 'NEW';
              }

            } else {
              // reload current assigned concepts, saving page information
              $scope.retrieveAssignedWork($scope.assignedWorkPage, null, 'NEW');
              $scope.setTab(0);
              $scope.assignedWorkType = 'NEW';
            }
          });

      // on any change of relevant scope variables, retrieve work
      $scope
        .$watch(
          [ 'focusProject', 'user', 'userToken', 'currentRole' ],
          function() {
            console
              .debug('assignedListCtrl:  Detected project or user set/change');

            if ($scope.focusProject != null && $scope.currentUser != null
              && $scope.currentUserToken != null && $scope.currentRole != null) {

              $http.defaults.headers.common.Authorization = $scope.currentUserToken;

              $scope.mapUsers = $scope.focusProject.mapSpecialist
                .concat($scope.focusProject.mapLead);

              $scope.retrieveAssignedWork($scope.assignedWorkPage, null,
                $scope.assignedWorkType);
              $scope.retrieveAssignedQAWork(1, null, $scope.assignedQAWorkType);
              $scope.retrieveLabels();
              if ($scope.currentRole === 'Lead'
                || $scope.currentRole === 'Administrator') {
                $scope.retrieveAssignedConflicts(1, null,
                  $scope.assignedConflictType);
                $scope.retrieveAssignedReviewWork(1, null,
                  $scope.assignedReviewWorkType);
                $scope.retrieveAssignedWorkForUser(1, null,
                  $scope.mapUserViewed, $scope.assignedWorkForUserType);
              }
            }
          });

      $scope.retrieveAssignedConflicts = function(page, query,
        assignedConflictType) {

        console.debug('Retrieving Assigned Conflicts: ', page, query,
          assignedConflictType);

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
          "startIndex" : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          "maxResults" : page == -1 ? -1 : $scope.itemsPerPage,
          "sortField" : 'sortKey',
          "queryRestriction" : assignedConflictType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + "project/id/" + $scope.focusProject.id
              + "/user/id/" + $scope.currentUser.userName + "/query/"
              + (query == null ? null : query) + "/assignedConflicts",
            dataType : "json",
            data : pfsParameterObj,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {
            $rootScope.glassPane--;

            $scope.assignedConflictsPage = page;
            $scope.assignedConflicts = data.searchResult;
            console.debug('Assigned Conflicts:');
            console.debug($scope.assignedConflicts);

            // set pagination
            $scope.nAssignedConflicts = data.totalCount;
            $scope.numAssignedConflictsPages = Math.ceil(data.totalCount
              / $scope.itemsPerPage);

            // set title
            $scope.tabs[1].title = "Conflicts (" + data.totalCount + ")";

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;

          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedWork = function(page, query, assignedWorkType) {

        console.debug('Retrieving Assigned Concepts: ', page, query,
          assignedWorkType);

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
          "startIndex" : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          "maxResults" : page == -1 ? -1 : $scope.itemsPerPage,
          "sortField" : 'sortKey',
          "queryRestriction" : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + "project/id/" + $scope.focusProject.id
              + "/user/id/" + $scope.currentUser.userName + "/query/"
              + (query == null ? null : query) + "/assignedConcepts",
            dataType : "json",
            data : pfsParameterObj,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          })
          .success(
            function(data) {
              $rootScope.glassPane--;

              $scope.assignedWorkPage = page;
              $scope.assignedRecords = data.searchResult;
              console.debug($scope.assignedRecords);

              // set pagination
              $scope.numAssignedRecordPages = Math.ceil(data.totalCount
                / $scope.itemsPerPage);
              $scope.nAssignedRecords = data.totalCount;

              // set title
              $scope.tabs[0].title = "Concepts (" + $scope.nAssignedRecords
                + ")";
              console.debug($scope.nAssignedRecords);
              console.debug(data.totalCount);
              console.debug($scope.assignedWorkTitle);

            }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      $scope.retrieveLabels = function() {
        console.debug('assignedListCtrl: Retrieving labels');

        $rootScope.glassPane++;
        $http({
          url : root_reporting + "qaLabel/qaLabels",
          dataType : "json",
          method : "GET",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(function(data) {
          console.debug("Success in getting qa labels.");

          $rootScope.glassPane--;
          for (var i = 0; i < data.searchResult.length; i++) {
            $scope.labelNames.push(data.searchResult[i].value);
          }
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedQAWork = function(page, query, assignedWorkType) {

        console.debug('Retrieving Assigned QA Work: ', page, query,
          assignedWorkType);

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
          "startIndex" : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          "maxResults" : page == -1 ? -1 : $scope.itemsPerPage,
          "sortField" : 'sortKey',
          "queryRestriction" : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + "project/id/" + $scope.focusProject.id
              + "/user/id/" + $scope.currentUser.userName + "/query/"
              + (query == null ? null : query) + "/assignedQAWork",
            dataType : "json",
            data : pfsParameterObj,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {
            $rootScope.glassPane--;

            $scope.assignedQAWorkPage = page;
            $scope.assignedQAWork = data.searchResult;
            console.debug($scope.assignedQAWork);

            // set pagination
            $scope.numAssignedRecordPages = Math.ceil(data.totalCount
              / $scope.itemsPerPage);
            $scope.nAssignedQAWork = data.totalCount;

            // set title
            $scope.tabs[4].title = "QA (" + $scope.nAssignedQAWork + ")";
            console.debug($scope.nAssignedQAWork);
            console.debug(data.totalCount);
            console.debug($scope.assignedQAWorkTitle);

            // set labels
            for (var i = 0; i < $scope.assignedQAWork.length; i++) {
              var concept = $scope.assignedQAWork[i];

              $scope.assignedQAWork[i].name = concept.value;
              $scope.assignedQAWork[i].labels = concept.value2.replace(/;/g,
                ' ');
            }

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedReviewWork = function(page, query,
        assignedWorkType) {

        console.debug('Retrieving Assigned Review Work: ', page, query,
          assignedWorkType);

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
          "startIndex" : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          "maxResults" : page == -1 ? -1 : $scope.itemsPerPage,
          "sortField" : 'sortKey',
          "queryRestriction" : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + "project/id/" + $scope.focusProject.id
              + "/user/id/" + $scope.currentUser.userName + "/query/"
              + (query == null ? null : query) + "/assignedReviewWork",
            dataType : "json",
            data : pfsParameterObj,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {
            $rootScope.glassPane--;

            $scope.assignedReviewWorkPage = page;
            $scope.assignedReviewWork = data.searchResult;
            console.debug($scope.assignedReviewWork);

            // set pagination
            $scope.numAssignedRecordPages = Math.ceil(data.totalCount
              / $scope.itemsPerPage);
            $scope.nAssignedReviewWork = data.totalCount;

            // set title
            $scope.tabs[2].title = "Review (" + $scope.nAssignedReviewWork
              + ")";
            console.debug($scope.nAssignedReviewWork);
            console.debug(data.totalCount);
            console.debug($scope.assignedReviewWorkTitle);

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedWorkForUser = function(page, mapUserName, query,
        assignedWorkType) {

        console.debug("retrieveAssignedWorkForUser:");
        console.debug($scope.mapUserViewed);

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
          $scope.tabs[3].title = "By User";

          return;
        }

        console.debug('Retrieving Assigned Concepts for user ' + mapUserName
          + ': page ' + page);

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          "startIndex" : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          "maxResults" : page == -1 ? -1 : $scope.itemsPerPage,
          "sortField" : 'sortKey',
          "queryRestriction" : assignedWorkType
        };

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + "project/id/" + $scope.focusProject.id
              + "/user/id/" + mapUserName + "/query/"
              + (query == null ? null : query) + "/assignedConcepts",
            dataType : "json",
            data : pfsParameterObj,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {
            $rootScope.glassPane--;

            $scope.assignedWorkForUserPage = page;
            $scope.assignedRecordsForUser = data.searchResult;
            console.debug($scope.assignedRecordsForUser);

            // set pagination
            $scope.numAssignedRecordPagesForUser = Math.ceil(data.totalCount
              / $scope.itemsPerPage);
            $scope.nAssignedRecordsForUser = data.totalCount;
            $scope.numRecordPagesForUser = Math
              .ceil($scope.nAssignedRecordsForUser / $scope.itemsPerPage);

            $scope.tabs[3].title = "By User (" + data.totalCount + ")";

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      // set the pagination variables
      function setPagination(assignedRecordsPerPage, nAssignedRecords) {

        $scope.assignedRecordsPerPage = assignedRecordsPerPage;
        $scope.numRecordPages = Math.ceil($scope.nAssignedRecords
          / assignedRecordsPerPage);
      }
      ;

      // on notification, update assigned work
      $scope.assignWork = function(newRecords) {

        $scope.retrieveAssignedWork($scope.assignedWorkPage);
        if ($scope.currentRole === 'Lead'
          || $scope.currentRole === 'Administrator') {
          $scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
        }
      };

      // function to relinquish work (i.e. unassign the user)
      $scope.unassignWork = function(record, mapUser) {

        console.debug("unassignWork", record, record.terminologyVersion);

        // show a confirmation dialog if requested
        // NOTE: workflow status is contained in terminologyVersion for a
        // searchResult object
        if (record.terminologyVersion === "EDITING_DONE"
          || record.terminologyVersion === "REVIEW_RESOLVED"
          || record.terminologyVersion === "QA_RESOLVED"
          || record.terminologyVersion === "CONFLICT_RESOLVED") {
          var response = confirm("Are you sure you want to return finished work?  You will lose any work done.");
          if (response == false)
            return;
        }

        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + "unassign/project/id/"
              + $scope.focusProject.id + "/concept/id/" + record.terminologyId
              + "/user/id/" + mapUser.userName,
            dataType : "json",
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {

            if ($scope.ownTab == true) {

              $rootScope
                .$broadcast('assignedListWidget.notification.unassignWork');

              $scope.retrieveAssignedWork($scope.assignedWorkPage,
                $scope.queryAssigned);
              $scope.retrieveAssignedQAWork($scope.assignedQAWorkPage,
                $scope.queryQAWork);
              if ($scope.currentRole === 'Lead'
                || $scope.currentRole === 'Administrator') {
                $scope.retrieveAssignedConflicts($scope.assignedConflictsPage,
                  $scope.queryConflict);
                $scope.retrieveAssignedReviewWork(
                  $scope.assignedReviewWorkPage, $scope.queryReviewWork);
              }

            } else {
              $scope.retrieveAssignedWorkForUser(1, mapUser.userName,
                $scope.queryAssignedForUser);
            }

            $rootScope.glassPane--;

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // Unassigns all work (both concepts and conflicts) for the current user
      $scope.unassignAllWork = function(unassignType, unassignEdited, mapUser) {

        $rootScope.glassPane++;

        var unassignUrlEnding = unassignEdited == true ? "/all" : "/unedited";

        $http(
          {
            url : root_workflow + "unassign/project/id/"
              + $scope.focusProject.id + "/user/id/" + mapUser.userName
              + unassignUrlEnding,
            dataType : "json",
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {

            if ($scope.ownTab == true) {
              console.debug('Viewing own work, retrieving assigned work');
              $scope.retrieveAssignedWork($scope.assignedWorkPage);
              if ($scope.currentRole === 'Lead'
                || $scope.currentRole === 'Administrator') {
                $scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
              }
              $rootScope
                .$broadcast('assignedListWidget.notification.unassignWork');
            } else {
              console
                .debug('Viewing other user work, retrieving assigned work for '
                  + mapUser.name);
              $scope.retrieveAssignedWorkForUser(1, mapUser);
            }

            $rootScope.glassPane--;
          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      $scope.setOwnTab = function(ownTab) {
        $scope.ownTab = ownTab;
      };

      // HACKISH: Variable passed in is the currently viewed map user in the
      // lead's View Other Work tab
      $scope.openUnassignModal = function(mapUser, workType) {

        console.debug("openUnassignModal with ", mapUser, workType);

        var modalInstance = $modal.open({
          templateUrl : 'js/widgets/assignedList/assignedListUnassign.html',
          controller : UnassignModalCtrl,
          resolve : {

            // switch on whether Lead is viewing their own work or another
            // user's
            mapUserToUnassign : function() {
              if ($scope.ownTab == true)
                return $scope.currentUser;
              else
                return mapUser;
            },
            isMapLead : function() {
              return $scope.currentRole === 'Lead';
            },
            unassignWorkType : function() {
              return workType;
            }
          }
        });

        // TODO Move API retrieval into neutral functions that don't affect
        // scope
        // i.e. retrieveAvailableConflicts should simply return a
        // SearchResultList
        // that then can be used by either this function or a display-affecting
        // function
        modalInstance.result.then(function(result) {
          console.debug("Unassigning batch work for user " + mapUser.userName
            + ", with parameters: ", result);

          // assemble the list of things to unassign via filtered retrieval APIs
          if (result.unassignConcepts == true) {

            var terminologyIdsConcepts = [];

            $rootScope.glassPane++;
            console.debug("Retrieving concepts to unassign...");
            var pfsParameterObj = {
              "startIndex" : -1,
              "maxResults" : -1,
              "sortField" : 'sortKey',
              "queryRestriction" : result.unassignEditedWork == true ? 'ALL'
                : 'NEW'
            };

            // retrieve the list of assigned conflicts
            $http(
              {
                url : root_workflow + "project/id/" + $scope.focusProject.id
                  + "/user/id/" + mapUser.userName + "/query/null"
                  + "/assignedConcepts",
                dataType : "json",
                data : pfsParameterObj,
                method : "POST",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(
              function(data) {
                $rootScope.glassPane--;
                for (var i = 0; i < data.searchResult.length; i++) {
                  terminologyIdsConcepts
                    .push(data.searchResult[i].terminologyId);
                }

                // call the batch unassign API
                console
                  .debug("Unassigning concepts ("
                    + terminologyIdsConcepts.length + ")",
                    terminologyIdsConcepts);
                unassignBatch(mapUser, terminologyIdsConcepts, 'concept');
                $rootScope.glassPane--;
              }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $rootScope.handleHttpError(data, status, headers, config);
            });
          }

          if (result.unassignConflicts == true) {

            var terminologyIdsConflicts = [];

            $rootScope.glassPane++;
            console.debug("Retrieving conflicts to unassign...");
            // construct a paging/filtering/sorting object
            var pfsParameterObj = {
              "startIndex" : -1,
              "maxResults" : -1,
              "sortField" : 'sortKey',
              "queryRestriction" : result.unassignEditedWork == true ? 'ALL'
                : 'CONFLICT_NEW'
            };

            // retrieve the list of assigned conflicts
            $http(
              {
                url : root_workflow + "project/id/" + $scope.focusProject.id
                  + "/user/id/" + mapUser.userName + "/query/null"
                  + "/assignedConflicts",
                dataType : "json",
                data : pfsParameterObj,
                method : "POST",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(
              function(data) {
                $rootScope.glassPane--;
                for (var i = 0; i < data.searchResult.length; i++) {
                  terminologyIdsConflicts
                    .push(data.searchResult[i].terminologyId);
                }
                // call the batch unassign API
                console.debug("Unassigning conflicts ("
                  + terminologyIdsConflicts.length + ")",
                  terminologyIdsConflicts);
                unassignBatch(mapUser, terminologyIdsConflicts, 'conflict');

                $rootScope.glassPane--;
              }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $rootScope.handleHttpError(data, status, headers, config);
            });
          }

          if (result.unassignReviewWork == true) {

            var terminologyIdsReview = [];

            console.debug("Retrieving review work to unassign...");
            var pfsParameterObj = {
              "startIndex" : -1,
              "maxResults" : -1,
              "sortField" : 'sortKey',
              "queryRestriction" : result.unassignEditedWork == true ? 'ALL'
                : 'REVIEW_NEW'
            };

            $rootScope.glassPane++;

            $http(
              {
                url : root_workflow + "project/id/" + $scope.focusProject.id
                  + "/user/id/" + mapUser.userName + "/query/null"
                  + "/assignedReviewWork",
                dataType : "json",
                data : pfsParameterObj,
                method : "POST",
                headers : {
                  "Content-Type" : "application/json"
                }
              })
              .success(
                function(data) {
                  $rootScope.glassPane--;
                  for (var i = 0; i < data.searchResult.length; i++) {
                    terminologyIdsReview
                      .push(data.searchResult[i].terminologyId);
                  }
                  // call the batch unassign API
                  console.debug("Unassigning review work ("
                    + terminologyIdsReview.length + ")", terminologyIdsReview);
                  unassignBatch(mapUser, terminologyIdsReview, 'review');

                  $rootScope.glassPane--;

                }).error(function(data, status, headers, config) {
                $rootScope.glassPane--;
                $rootScope.handleHttpError(data, status, headers, config);
              });

          }

          if (result.unassignQAWork == true) {

            var terminologyIdsQA = [];

            console.debug("Retrieving qa work to unassign...");
            var pfsParameterObj = {
              "startIndex" : -1,
              "maxResults" : -1,
              "sortField" : 'sortKey',
              "queryRestriction" : result.unassignEditedWork == true ? 'ALL'
                : 'QA_NEW'
            };

            $rootScope.glassPane++;

            $http(
              {
                url : root_workflow + "project/id/" + $scope.focusProject.id
                  + "/user/id/" + mapUser.userName + "/query/null"
                  + "/assignedQAWork",
                dataType : "json",
                data : pfsParameterObj,
                method : "POST",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(
              function(data) {
                $rootScope.glassPane--;
                for (var i = 0; i < data.searchResult.length; i++) {
                  terminologyIdsQA.push(data.searchResult[i].terminologyId);
                }
                // call the batch unassign API
                console.debug("Unassigning qa work (" + terminologyIdsQA.length
                  + ")", terminologyIdsQA);
                unassignBatch(mapUser, terminologyIdsQA, 'qa');

                $rootScope.glassPane--;

              }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $rootScope.handleHttpError(data, status, headers, config);
            });

          }

        });
      };

      var unassignBatch = function(mapUser, terminologyIds, workType) {

        console.debug("unassignBatch", mapUser, workType);

        $rootScope.glassPane++;
        $http(
          {
            url : root_workflow + "unassign/project/id/"
              + $scope.focusProject.id + "/user/id/" + mapUser.userName
              + "/batch",
            dataType : "json",
            data : terminologyIds,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {
            $rootScope.glassPane--;

            // set timeout to avoid database issues where unassign and
            // retrieve overlap
            setTimeout(function() {

              // trigger reload of this type of work via broadcast
              // notification
              $rootScope.$broadcast(
                'workAvailableWidget.notification.assignWork', {
                  assignUser : mapUser,
                  assignType : workType
                });

              // if this user unassigned their own work, broadcast unassign
              if (mapUser.userName === $scope.currentUser.userName)
                $rootScope
                  .$broadcast('assignedListWidget.notification.unassignWork');
            }, 250);
          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      var UnassignModalCtrl = function($scope, $modalInstance,
        mapUserToUnassign, isMapLead, unassignWorkType) {

        console.debug("Entered modal control", mapUserToUnassign,
          unassignWorkType);
        $scope.mapUserToUnassign = mapUserToUnassign;
        $scope.isMapLead = isMapLead;
        $scope.unassignWorkType = unassignWorkType;
        $scope.unassignEditedWork = 'Unedited'; // note, boolean flags were
        // causing errors, possibly
        // related to radio inputs?
        $scope.unassignConcepts = false;
        $scope.unassignConflicts = false;
        $scope.unassignReviewWork = false;
        $scope.unassignQAWork = false;

        if ($scope.unassignWorkType == 'concepts' || isMapLead == false)
          $scope.unassignConcepts = true;
        else if ($scope.unassignWorkType == 'conflicts')
          $scope.unassignConflicts = true;
        else if ($scope.unassignWorkType == 'review')
          $scope.unassignReviewWork = true;
        else if ($scope.unassignWorkType == 'qa')
          $scope.unassignQAWork = true;

        $scope.selectAll = function(isSelected) {
          $scope.unassignConcepts = isSelected;
          $scope.unassignConflicts = isSelected;
          $scope.unassignReviewWork = isSelected;
          $scope.unassignQAWork = isSelected;
        };

        $scope.ok = function(unassignEditedWork, unassignConcepts,
          unassignConflicts, unassignReviewWork, unassignQAWork) {

          if (unassignEditedWork == null)
            alert("You must select whether to delete edited work.");
          if (unassignConcepts == false && unassignConflicts == false
            && unassignReviewWork == false && unassignQAWork == false) {
            alert("You must select a type of work to return");
          } else {
            var result = {
              'unassignEditedWork' : unassignEditedWork === 'Unedited' ? false
                : true,
              'unassignConcepts' : unassignConcepts,
              'unassignConflicts' : unassignConflicts,
              'unassignReviewWork' : unassignReviewWork,
              'unassignQAWork' : unassignQAWork
            };
            $modalInstance.close(result);
          }
        };

        $scope.cancel = function() {
          $modalInstance.dismiss('cancel');
        };
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
      ;

      $scope.goEditRecord = function(id) {
        var path = "/record/recordId/" + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditConflict = function(id) {
        var path = "/record/conflicts/" + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditReviewWork = function(id) {
        var path = "/record/review/" + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditQAWork = function(id) {
        var path = "/record/review/" + id;
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
      }

      /**
       * Function to open finish or publish modal. Argument: workflowStatus: The
       * filter by which to retrieve records Must be a valid workflow status of
       * type *_IN_PROGRESS or *_RESOLVED
       */
      $scope.finishOrPublishBatch = function(workflowStatus) {

        // check arguments
        if (workflowStatus != null
          && workflowStatus.indexOf('_IN_PROGRESS') == -1
          && workflowStatus.indexOf('_RESOLVED') == -1) {
          console
            .error("Invalid workflow status passed to finishOrPublish, must be *_IN_PROGRESS or *_RESOLVED");
        }

        console.debug("Called finishOrPublishBatch with workflowStatus",
          workflowStatus);

        // determine type of work
        var apiWorkflowText;

        // determine the retrieval API text
        // i.e.. whether to call assignedConcepts,
        // assignedReviewWork, or assignedConcepts
        if (workflowStatus === 'CONFLICT_IN_PROGRESS'
          || workflowStatus === 'CONFLICT_RESOLVED')
          apiWorkflowText = 'assignedConflicts';
        else if (workflowStatus === 'REVIEW_IN_PROGRESS'
          || workflowStatus === 'REVIEW_RESOLVED')
          apiWorkflowText = 'assignedReviewWork';
        else if (workflowStatus === 'QA_IN_PROGRESS'
          || workflowStatus === 'QA_RESOLVED')
          apiWorkflowText = 'assignedQAWork';
        else if (workflowStatus === 'EDITING_IN_PROGRESS'
          || workflowStatus === 'EDITING_DONE')
          apiWorkflowText = 'assignedConcepts';
        else {
          console.debug("Could not determine api call from workflow status");
          return;
        }

        console.debug('apiWorkflowText', apiWorkflowText);

        // construct a paging/filtering/sorting object based on work type
        var pfsParameterObj = {
          "startIndex" : -1,
          "maxResults" : -1,
          "sortField" : null,
          "queryRestriction" : workflowStatus
        };

        $rootScope.glassPane++;

        console.debug("Making html call to retrieve eligible records");

        $http(
          {
            url : root_workflow + "project/id/" + $scope.focusProject.id
              + "/user/id/" + $scope.currentUser.userName + "/query/null/"
              + apiWorkflowText, // set above based on specified workflow
            // status
            dataType : "json",
            data : pfsParameterObj,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
          $rootScope.glassPane--;

          // if results found, open the modal
          if (data.searchResult.length > 0) {
            console.debug("Results: ", data.searchResult);
            $scope.openFinishOrPublishModal(data.searchResult);
          } else {
            console.debug("No results returned");
          }
        }).error(function(data, status, headers, config) {
          console.debug("ERROR RETRIEVING SEARCH RESULTS");
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      $scope.openFinishOrPublishModal = function(records) {

        if (records == null || records.length == 0) {
          console.error("openPerformBatchActionModal called with no records");
          return;
        }
        ;

        console.debug("Entered openFinishOrPUblishModal", records);

        // NOTE: Record information is shoehorned into searchResult
        // workflow status is contained in terminologyVersion
        var workflowStatus = records[0].terminologyVersion;
        console.debug("WorkflowStatus = ", workflowStatus);

        var modalInstance = $modal
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
                return (workflowStatus === 'CONFLICT_RESOLVED'
                  || workflowStatus === 'REVIEW_RESOLVED' || workflowStatus === 'QA_RESOLVED') ? 'publish'
                  : 'finish';
              }
            }
          });

        modalInstance.result.then(

        // called on Done clicked by user
        function() {
          console.debug("User closed finish/publish modal");
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
          } else if (workflowStatus === 'QA_IN_PROGRESS'
            || workflowStatus === 'QA_RESOLVED') {
            $scope.retrieveAssignedQAWork(1, null, workflowStatus); // called on
            // Done
          } else if (workflowStatus === 'EDITING_IN_PROGRESS'
            || workflowStatus === 'EDITING_DONE') {
            $scope.retrieveAssignedWork(1, null, workflowStatus); // called on
            // Done
          } else {
            console.debug("Could not determine api call from workflow status");
            return;
          }

          // called on Cancel/Escape, same functionality
        }, function() {
          console.debug("Finish/publish modal dismissed");
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
          } else if (workflowStatus === 'QA_IN_PROGRESS'
            || workflowStatus === 'QA_RESOLVED') {
            $scope.retrieveAssignedQAWork(1, null, workflowStatus);
          } else if (workflowStatus === 'EDITING_IN_PROGRESS'
            || workflowStatus === 'EDITING_DONE') {
            $scope.retrieveAssignedWork(1, null, workflowStatus); // called on
            // Done
          } else {
            console.debug("Could not determine api call from workflow status");
            return;
          }
        });

      };

      var FinishOrPublishWorkModalCtrl = function($scope, $modalInstance, user,
        project, records, action) {

        console.debug("Entered modal control", user, project, records);
        $scope.user = user;
        $scope.project = project;
        $scope.records = records;
        $scope.action = action;
        $scope.index = 1; // note this index is in range [1, n], where n is the
        // number of records

        for (var i = 0; i < $scope.records; i++)
          console.debug(action, $scope.records[i].id);

        // set the dispaly text based on action
        if (action === 'finish') {
          $scope.actionText = 'Finish';
        } else if (action === 'publish') {
          $scope.actionText = 'Publish';
        }

        $scope.selectNextRecord = function() {
          $scope.index = $scope.index == $scope.records.length ? 1
            : $scope.index + 1;
          $scope.loadRecord();
        };

        // declare the function
        $scope.loadRecord = function() {

          $scope.validationResult = null;

          console.debug("Selecting record", $scope.index);

          // get id from list (note index is range [1,n], subtract one for array
          // access)
          var recordId = $scope.records[$scope.index - 1].id;

          console.debug("Retrieving record", recordId);

          $rootScope.glassPane++;

          // perform the retrieval call
          $http({
            url : root_mapping + "record/id/" + recordId,
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          })
            .success(
              function(data) {

                // do not close glass pane here, validate record first

                // set scope record
                $scope.currentRecord = data;

                // check if this record is still in progress, based on requested
                // action
                if ($scope.action === 'publish') {

                  // if in a publication state, this record has been finished
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
                    || $scope.currentRecord.workflowStatus === 'QA_IN_PROGRESS')
                    $scope.currentRecord.isFinished = false;

                  // otherwise, this record has been finished/published via this
                  // modal
                  else
                    $scope.currentRecord.isFinished = true;
                }

                console.debug("Validating the map record");
                // validate the record
                $http({
                  url : root_mapping + "validation/record/validate",
                  dataType : "json",
                  data : $scope.currentRecord,
                  method : "POST",
                  headers : {
                    "Content-Type" : "application/json"
                  }
                })
                  .success(function(data) {
                    $rootScope.glassPane--;
                    console.debug("validation results:");
                    console.debug(data);
                    $scope.validationResult = data;
                  })
                  .error(
                    function(data, status, headers, config) {
                      $rootScope.glassPane--;
                      $scope.validationResult = null;
                      $scope.recordError = "Unexpected error reported by server.  Contact an admin.";
                      console.debug("Failed to validate map record");
                      $rootScope.handleHttpError(data, status, headers, config);
                    });

              }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $scope.error = "Could not retrieve record";
            });
        };

        $scope.finishCurrentRecord = function() {
          $rootScope.glassPane++;

          $http({
            url : root_workflow + $scope.action, // api text is passed in as
            // argument
            dataType : "json",
            data : $scope.currentRecord,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
            $rootScope.glassPane--;
            $scope.currentRecord.isFinished = true;

            // if this was the only record, close the modal
            if ($scope.records.length == 1) {
              $scope.done();
            } else {
              $scope.selectNextRecord();
            }

          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $scope.error = "Error saving record";
          });

        };

        $scope.done = function() {
          $modalInstance.close();
        };

        // get the first record
        $scope.loadRecord($scope.index);
      };
    });
