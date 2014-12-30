'use strict';

angular
    .module('mapProjectApp.widgets.mapRecord', [ 'adf.provider' ])
    .config(function(dashboardProvider) {

      dashboardProvider.widget('mapRecord', {
        title : 'Map Record',
        description : 'Edit module for a map record',
        templateUrl : 'js/widgets/mapRecord/mapRecord.html',
        controller : 'mapRecordWidgetCtrl',
        resolve : {},
        edit : {}
      });
    })

    .controller(
        'mapRecordWidgetCtrl',
        function($scope, $rootScope, $http, $routeParams, $location, $sce,
            $modal, $window, localStorageService) {

          // ///////////////////////////////////
          // Map Record Controller Functions //
          // ///////////////////////////////////

          // this controller handles a potentially "dirty" page
          // $rootScope.currentPageDirty = true;

          // initialize scope variables
          $scope.record = null;
          $scope.project = localStorageService.get('focusProject');
          $scope.concept = null;
          $scope.groups = null;
          $scope.entries = null;
          $scope.user = localStorageService.get('currentUser');
          $scope.role = localStorageService.get('currentRole');
          $scope.userToken = localStorageService.get('userToken');
          $scope.conversation = null;
          $scope.mapLeads = $scope.project.mapLead;
          organizeUsers($scope.mapLeads);

          $scope.returnRecipients = new Array();
          $scope.multiSelectSettings = {
            displayProp : 'name',
            scrollableHeight : '50px',
            scrollable : true,
            showCheckAll : false,
            showUncheckAll : false
          };
          $scope.multiSelectCustomTexts = {
            buttonDefaultText : 'Select Leads'
          };

          // validation result storage variable
          $scope.savedValidationWarnings = [];

          // initialize accordion variables
          $scope.isConceptOpen = true;
          $scope.isEntriesOpen = true;
          $scope.isPrinciplesOpen = false;
          $scope.isNotesOpen = false;
          $scope.isFlagsOpen = false;
          $scope.groupOpen = new Array(10);
          for (var i = 0; i < $scope.groupOpen.length; i++)
            $scope.groupOpen[i] = true;

          // start note edit mode in off mode
          $scope.noteEditMode = false;
          $scope.noteEditId = null;
          $scope.noteInput = '';

          // tooltip for Save/Next button
          $scope.dynamicTooltip = "";

          // groups tree for dynamic sorting/repositioning
          $scope.groupsTree = [];
      
      // flag indicating if index viewer is available for dest terminology
      $scope.indexViewerExists = false;

          // options for angular-ui-tree
          $scope.options = {
            accept : function(sourceNode, destNodes, destIndex) {
              console.log("accepting drop :", sourceNode, destNodes, destIndex);

              var sourceType = sourceNode.$modelValue.type;
              var destType = destNodes.$modelValue[0].type;

              console.log("extracted types:", sourceType, destType);
              return sourceType == destType;
            },

            dragStart : function(event) {
              $scope.dragging = true;
            },
            dragStop : function(event) {
              $scope.dragging = false;
            },

            dropped : function(event) {
              console.log(event);

              $scope.saveGroups();
            },
          };

          // function with two purposes:
          // (1) update the group and priority of the group tree
          // (2) update the entries of the record
          $scope.saveGroups = function() {

            console.debug("Saving (renumbering) groups");

            var entries = [];

            for (var i = $scope.groupsTree.length - 1; i >= 0; i--) {

              // check if no entries
              if ($scope.groupsTree[i].entry.length == 0) {
                // do nothing
              } else {
                $scope.groupsTree[i].mapGroup = i + 1;

                // assign group and priority to entries
                for (var j = 0; j < $scope.groupsTree[i].entry.length; j++) {
                  $scope.groupsTree[i].entry[j].mapPriority = j + 1;
                  $scope.groupsTree[i].entry[j].mapGroup = i + 1;

                  // add the entry to the local array
                  entries.push($scope.groupsTree[i].entry[j]);
                }
              }
            }

            // assign entries to the record
            $scope.record.mapEntry = entries;

            broadcastRecord();

          };

          $scope.$watch(function() {

          }, function() {

            $scope.groupsTree.sort(function(group1, group2) {
              return group1.mapGroup - group2.mapGroup;
            });

          }, true);

          /** END NEW GROUP STUFF */

          // accordion functions
          $scope.openAll = function() {
            $scope.isConceptOpen = true;
            $scope.isEntriesOpen = true;
            $scope.isPrinciplesOpen = true;
            $scope.isNotesOpen = true;
            $scope.isFlagsOpen = true;
            for (var i = 0; i < $scope.groupOpen.length; i++)
              $scope.groupOpen[i] = true;
          };

          $scope.closeAll = function() {
            $scope.isConceptOpen = false;
            $scope.isEntriesOpen = false;
            $scope.isPrinciplesOpen = false;
            $scope.isNotesOpen = false;
            $scope.isFlagsOpen = false;
          };

          // Watcher for Conflict Resolution Select Record Event
          $rootScope.$on('compareRecordsWidget.notification.selectRecord',
              function(event, parameters) {
                console.debug("received new record");
                console.debug(parameters);
                console.debug(parameters.record);
                $scope.record = parameters.record;

                console.debug("Passed record: ", parameters.record);

                // This MUST not be removed for "Start here" to
                // work
                initializeGroupsTree();

              });

      // on successful retrieval of project, get the
      // record/concept
      $scope.$watch([ 'project', 'userToken', 'role', 'user', 'record' ],
        function() {
          if ($scope.project != null && $scope.userToken != null) {
            $http.defaults.headers.common.Authorization = $scope.userToken;
            setIndexViewerStatus();
            retrieveRecord();
          }
        });

          // any time the record changes, broadcast it to the record
          // summary widget
          $scope.$watch('record', function() {

            broadcastRecord();

          });

          function broadcastRecord() {

            console.debug("Broadcasting record", $scope.record);

            $rootScope.$broadcast('mapRecordWidget.notification.recordChanged',
                {
                  record : angular.copy($scope.record),
                  project : $scope.project

                });
          }

          // initialize local variables
          var currentLocalId = 1; // used for addition of new entries
          // without hibernate id

          // function to initially retrieve the project

          function retrieveRecord() {
            // obtain the record
            $http({
              url : root_mapping + "record/id/" + $routeParams.recordId,
              dataType : "json",
              method : "GET",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
              $scope.record = data;

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            }).then(
                function() {

                  // obtain the record concept
                  $http(
                      {
                        url : root_content + "concept/id/"
                            + $scope.project.sourceTerminology + "/"
                            + $scope.project.sourceTerminologyVersion + "/"
                            + $scope.record.conceptId,
                        dataType : "json",
                        method : "GET",
                        headers : {
                          "Content-Type" : "application/json"
                        }
                      }).success(function(data) {
                    $scope.concept = data;
                    $scope.conceptBrowserUrl = $scope.getBrowserUrl();
                    // initialize
                    // the dynamic
                    // tooltip on
                    // the Save/Next
                    // button with
                    // the next
                    // concept to be
                    // mapped
                    $scope.resolveNextConcept(true);
                  }).error(function(data, status, headers, config) {
                    $rootScope.handleHttpError(data, status, headers, config);
                  });

                  // initialize the entries
                  initializeGroupsTree();

                  // add code to get feedback
                  // conversations
                  $http(
                      {
                        url : root_workflow + "conversation/id/"
                            + $scope.record.id,
                        dataType : "json",
                        method : "GET",
                        headers : {
                          "Content-Type" : "application/json"
                        }
                      }).success(function(data) {
                    $scope.conversation = data;
                    initializeReturnRecipients();
                  }).error(function(data, status, headers, config) {
                    $rootScope.handleHttpError(data, status, headers, config);
                  });
                });
          }
          ;

          
      function setIndexViewerStatus() {       
        $http(
          {
            url : root_content + "index/"
              + $scope.project.destinationTerminology + "/"
              + $scope.project.destinationTerminologyVersion,
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
          console.debug("Success in getting viewable indexes.");
          if (data.searchResult.length > 0) {
            $scope.indexViewerExists = true;
          } else {
            $scope.indexViewerExists = false;
          }
        }).error(function(data, status, headers, config) {
          $scope.indexViewerExists = false;
        });
      };
          // /////////////////////////////
          // Initialization Functions ///
          // /////////////////////////////

          function initializeGroupsTree() {
            $scope.groupsTree = [];

            console.debug($scope.record.mapEntry);

            // if no entries on this record, create a group and
            // entry
            if ($scope.record.mapEntry.length == 0) {

              console.debug("Adding blank map group and entry");
              $scope.addMapGroup();

            } else {

              for (var i = 0; i < $scope.record.mapEntry.length; i++) {

                console.debug("ENTRY: ", $scope.record.mapEntry[i]);
                // get the entry
                var entry = $scope.record.mapEntry[i];

                console.debug("checking entry " + i, entry);

                // check if group already exists
                var groupExists = false;
                for (var j = 0; j < $scope.groupsTree.length; j++) {

                  console.debug($scope.groupsTree[j].mapGroup, entry.mapGroup);

                  if ($scope.groupsTree[j].mapGroup === entry.mapGroup) {
                    console.debug('Group exists');
                    $scope.groupsTree[j].entry.push(entry);
                    groupExists = true;
                  }
                }

                // if group does not exist, add a new group
                if (groupExists == false) {

                  console.debug('Group does not exist');
                  var group = {
                    "type" : "group",
                    "mapGroup" : entry.mapGroup,
                    "entry" : []
                  };
                  group.entry.push(entry);
                  $scope.groupsTree.push(group);

                }

              }

              console.debug('GROUPS TREE BEFORE SORT: ', $scope.groupsTree);

              // groups may be disordered, re-order the groups and add empty
              // groups if necessary
              var groupsTreeTemp = angular.copy($scope.groupsTree);
              $scope.groupsTree = [];

              // get max group present
              var maxGroup = 0;
              for (var i = 0; i < groupsTreeTemp.length; i++) {
                maxGroup = Math.max(maxGroup, groupsTreeTemp[i].mapGroup);
              }
              console.debug('MAX GROUP: ', maxGroup);

              // cycle from 1:maxGroup and add existing group or blank group
              for (var group = 1; group <= maxGroup; group++) {

                var groupFound = false;

                // cycle over all existing groups
                for (var i = 0; i < groupsTreeTemp.length; i++) {

                  console.debug("CHECKING groups", groupsTreeTemp[i].mapGroup,
                      group);

                  // if group matches, push it next onto list
                  if (groupsTreeTemp[i].mapGroup == group) {
                    console.debug("Found mathing group for " + group);
                    $scope.groupsTree.push(groupsTreeTemp[i]);
                    groupFound = true;
                  }
                }

                // if group not found, add a new (blank) group
                if (groupFound == false) {
                  $scope.addMapGroup();
                }

              }

              console.debug('new groups', $scope.groupsTree);
            }

            // select the first map entry of the first group
            $scope.selectMapEntry($scope.groupsTree[0].entry[0]);
          }

          /**
           * MAP RECORD FUNCTIONS
           */

          $scope.finishMapRecord = function(returnBack) {

            // check that note box does not contain unsaved material
            if ($scope.tinymceContent != '' && $scope.tinymceContent != null) {
              if (confirm("You have unsaved text into the Map Notes. Do you wish to continue saving? The note will be lost.") == false) {
                return;
              }
            }

            console.debug("Validating the map record.");
            // validate the record
            $rootScope.glassPane++;
            $http({
              url : root_mapping + "validation/record/validate",
              dataType : "json",
              data : $scope.record,
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
                    })
                .then(
                    function(data) {

                      // if no error messages were
                      // returned, stop and display
                      if ($scope.validationResult.errors.length == 0) {

                        var warningCheckPassed = true;

                        // if warnings found, check if
                        // this is a second click
                        if ($scope.validationResult.warnings.length != 0) {

                          // if the same number of
                          // warnings are present
                          if ($scope.savedValidationWarnings.length == $scope.validationResult.warnings.length) {

                            // check that the
                            // warnings are the same
                            for (var i = 0; i < $scope.savedValidationWarnings.length; i++) {
                              if ($scope.savedValidationWarnings[i] != $scope.validationResult.warnings[i]) {
                                warningCheckPassed = false;
                              }

                            }

                            // if a different number
                            // of warnings,
                            // automatic fail
                          } else {
                            warningCheckPass = false;
                          }
                        }

                        // if the warning checks are
                        // passed, save the record
                        if (warningCheckPassed == true) {

                          // assign the current user
                          // to the lastModifiedBy
                          // field
                          $scope.record.lastModifiedBy = $scope.user;

                          $rootScope.glassPane++;
                          $http({
                            url : root_workflow + "finish",
                            dataType : "json",
                            data : $scope.record,
                            method : "POST",
                            headers : {
                              "Content-Type" : "application/json"
                            }
                          })
                              .success(
                                  function(data) {
                                    $scope.recordSuccess = "Record saved.";
                                    $scope.recordError = "";

                                    // user
                                    // has
                                    // successfully
                                    // finished
                                    // record,
                                    // page
                                    // is no
                                    // longer
                                    // "dirty"
                                    $rootScope.currentPageDirty = false;

                                    $rootScope.glassPane--;
                                    console
                                        .debug("Simple finish called, return to dashboard");
                                    $location.path($scope.role + "/dash");
                                  })
                              .error(
                                  function(data, status, headers, config) {
                                    $rootScope.glassPane--;
                                    $scope.recordError = "Unexpected server error.  Try saving your work for later, and contact an admin.";
                                    $rootScope.handleHttpError(data, status,
                                        headers, config);
                                    console.debug('SERVER ERROR');
                                    $scope.recordSuccess = "";
                                  });

                          // if the warning checks
                          // were not passed, save the
                          // warnings
                        } else {
                          $scope.savedValidationWarnings = $scope.validationResult.warnings;
                        }

                        // if errors found, clear the
                        // recordSuccess field
                      } else {
                        $scope.recordSuccess = "";
                      }

                    });
          };

          $scope.clearMapRecord = function() {
            $scope.groupsTree = new Array();

            $scope.record.mapPrinciple = [];
            $scope.record.mapNote = [];
            $scope.record.flagForLeadReview = false;
            $scope.record.flagForConsensus = false;
            $scope.record.flagForEditorialReview = false;

            $scope.addMapGroup(); // automatically adds entry as
            // well

            window.scrollTo(0, 0);

            broadcastRecord();
          };

          // returns the next concept to be worked on
          // used to get the next concept when Save/Next is pressed
          // also used to get next concept to display conceptId in
          // tooltip on Save/Next button
          $scope.resolveNextConcept = function(tooltipOnly) {
            var startIndex;
            if (tooltipOnly == true) {
              startIndex = 1;
            } else {
              startIndex = 0;
            }

            // if specialist level work, query for assigned concepts
            if ($scope.record.workflowStatus === 'NEW'
                || $scope.record.workflowStatus === 'EDITING_IN_PROGRESS'
                || $scope.record.workflowStatus === 'EDITING_DONE') {

              // construct a paging/filtering/sorting object
              var pfsParameterObj = {
                "startIndex" : startIndex,
                "maxResults" : 1,
                "sortField" : 'sortKey',
                "queryRestriction" : 'NEW'
              };

              // get the assigned work list
              $rootScope.glassPane++;
              $http(
                  {
                    url : root_workflow + "project/id/" + $scope.project.id
                        + "/user/id/" + $scope.user.userName
                        + "/query/null/assignedConcepts",

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

                        var assignedWork = data.searchResult;

                        if (tooltipOnly == true) {
                          if (assignedWork.length == 0) {
                            $scope.dynamicTooltip = "";
                          } else {
                            $scope.dynamicTooltip = assignedWork[0].terminologyId
                                + " " + assignedWork[0].value;
                          }
                        } else {
                          // if there is no more
                          // assigned work, return to
                          // dashboard
                          if (assignedWork.length == 0) {
                            console
                                .debug("No more assigned work, return to dashboard");
                            $location.path($scope.role + "/dash");

                            // otherwise redirect to
                            // the next record to be
                            // edited
                          } else {
                            console.debug("More work, redirecting");
                            $location.path("record/recordId/"
                                + assignedWork[0].id);
                          }
                        }

                      }).error(function(data, status, headers, config) {
                    $rootScope.glassPane--;
                    $rootScope.handleHttpError(data, status, headers, config);
                  });

              // otherwise, if a conflict record, query available
              // conflicts
            } else if ($scope.record.workflowStatus === 'CONFLICT_NEW'
                || $scope.record.workflowStatus === 'CONFLICT_IN_PROGRESS') {

              // construct a paging/filtering/sorting object
              var pfsParameterObj = {
                "startIndex" : startIndex,
                "maxResults" : 1,
                "sortField" : 'sortKey',
                "queryRestriction" : 'CONFLICT_NEW'
              };

              // get the assigned conflicts
              $rootScope.glassPane++;
              $http(
                  {
                    url : root_workflow + "project/id/" + $scope.project.id
                        + "/user/id/" + $scope.user.userName
                        + "/query/null/assignedConflicts",

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

                        var assignedWork = data.searchResult;

                        if (tooltipOnly == true) {
                          if (assignedWork.length == 0) {
                            $scope.dynamicTooltip = "";
                          } else {
                            $scope.dynamicTooltip = assignedWork[0].terminologyId
                                + " " + assignedWork[0].value;
                          }
                        } else {
                          // if there is no more
                          // assigned work, return to
                          // dashboard
                          if (assignedWork.length == 0) {
                            console
                                .debug("No more assigned conflicts, return to dashboard");
                            $location.path($scope.role + "/dash");

                            // otherwise redirect to
                            // the next record to be
                            // edited
                          } else {
                            console.debug("More work, redirecting");
                            $location.path("record/conflicts/"
                                + assignedWork[0].id);
                          }
                        }

                      }).error(function(data, status, headers, config) {
                    $rootScope.glassPane--;
                    $rootScope.handleHttpError(data, status, headers, config);
                  });

              // otherwise, if a review record, query available
              // review work
              // TODO: figure out how to differentiate between
              // QA_PATH and REVIEW_PROJECT_PATH so
              // that we can provide a next concept of the correct
              // type
              // then we can comment this section back in
            } else if ($scope.record.workflowStatus === 'REVIEW_NEW'
                || $scope.record.workflowStatus === 'REVIEW_IN_PROGRESS') {

              // construct a paging/filtering/sorting object
              var pfsParameterObj = {
                "startIndex" : startIndex,
                "maxResults" : 1,
                "sortField" : 'sortKey',
                "queryRestriction" : 'REVIEW_NEW'
              };
              // get the assigned review work
              $rootScope.glassPane++;
              $http(
                  {
                    url : root_workflow + "project/id/" + $scope.project.id
                        + "/user/id/" + $scope.user.userName
                        + "/query/null/assignedReviewWork",

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

                        var assignedWork = data.searchResult;
                        if (tooltipOnly == true) {
                          if (assignedWork.length == 0) {
                            $scope.dynamicTooltip = "";
                          } else {
                            $scope.dynamicTooltip = assignedWork[0].terminologyId
                                + " " + assignedWork[0].value;
                          }
                        } else {
                          // if there is no more
                          // assigned work, return to
                          // dashboard
                          if (assignedWork.length == 0) {
                            console
                                .debug("No more assigned review work, return to dashboard");
                            $location.path($scope.role + "/dash");

                            // otherwise redirect to
                            // the next record to be
                            // edited
                          } else {
                            console.debug("More work, redirecting");
                            $location.path("record/review/"
                                + assignedWork[0].id);
                          }
                        }
                      }).error(function(data, status, headers, config) {
                    $rootScope.glassPane--;
                    $rootScope.handleHttpError(data, status, headers, config);
                  });
            } else if ($scope.record.workflowStatus === 'QA_NEW'
                || $scope.record.workflowStatus === 'QA_IN_PROGRESS') {

              // construct a paging/filtering/sorting object
              var pfsParameterObj = {
                "startIndex" : startIndex,
                "maxResults" : 1,
                "sortField" : 'sortKey',
                "queryRestriction" : 'QA_NEW'
              };
              // get the assigned review work
              $rootScope.glassPane++;
              $http(
                  {
                    url : root_workflow + "project/id/" + $scope.project.id
                        + "/user/id/" + $scope.user.userName
                        + "/query/null/assignedQAWork",

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

                        var assignedWork = data.searchResult;
                        if (tooltipOnly == true) {
                          if (assignedWork.length == 0) {
                            $scope.dynamicTooltip = "";
                          } else {
                            $scope.dynamicTooltip = assignedWork[0].terminologyId
                                + " " + assignedWork[0].value;
                          }
                        } else {
                          // if there is no more
                          // assigned work, return to
                          // dashboard
                          if (assignedWork.length == 0) {
                            console
                                .debug("No more assigned qa work, return to dashboard");
                            $location.path($scope.role + "/dash");

                            // otherwise redirect to
                            // the next record to be
                            // edited
                          } else {
                            console.debug("More work, redirecting");
                            $location.path("record/review/"
                                + assignedWork[0].id);
                          }
                        }
                      }).error(function(data, status, headers, config) {
                    $rootScope.glassPane--;
                    $rootScope.handleHttpError(data, status, headers, config);
                  });
              /*
               * } else { $rootScope.glassPane--; console.debug("MapRecord
               * finish/next can't determine type of work, returning to
               * dashboard"); $location.path($scope.role + "/dash");
               */
            }
          };

          $scope.saveMapRecord = function(returnBack) {

            console.debug("saveMapRecord called with " + returnBack);
            console.debug("Note content: ", $scope.tinymceContent);

            // check that note box does not contain unsaved material
            if ($scope.tinymceContent != '' && $scope.tinymceContent != null) {
              if (confirm("You have unsaved text into the Map Notes. Do you wish to continue saving? The note will be lost.") == false) {
                return;
              }
              ;
            }

            // assign the current user to the lastModifiedBy field
            $scope.record.lastModifiedBy = $scope.user;

            // if only displaying record again, don't make rest call
            // if ($rootScope.currentPageDirty == false &&
            // !returnBack)
            // return;

            $rootScope.glassPane++;

            $http({
              url : root_workflow + "save",
              dataType : "json",
              data : $scope.record,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {

              // user has successfully saved
              // record, page is no longer "dirty"
              $rootScope.currentPageDirty = false;

              // $scope.record = data;
              $scope.recordSuccess = "Record saved.";
              $scope.recordError = "";
              $rootScope.glassPane--;
              if (!returnBack) {
                console.debug("************* ReturnBack is false");

                $scope.resolveNextConcept(false);

              } else {
                console.debug("Simple save called, return to dashboard");
                $location.path($scope.role + "/dash");
              }
            }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $scope.recordError = "Error saving record.";
              $rootScope.handleHttpError(data, status, headers, config);
              $scope.recordSuccess = "";
            });
          };

          // discard changes
          $scope.cancelMapRecord = function() {

            $rootScope.glassPane++;
            $http({
              url : root_workflow + "cancel",
              dataType : "json",
              data : $scope.record,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {

              // user has requested a cancel event, page is no
              // longer "dirty"
              $rootScope.currentPageDirty = false;
              $rootScope.glassPane--;
              $location.path($scope.role + "/dash");
            }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $rootScope.handleHttpError(data, status, headers, config);
            });

          };

          $scope.addRecordPrinciple = function(record, principle) {

            // check if principle valid
            if (principle === '') {
              $scope.errorAddRecordPrinciple = "Principle cannot be empty";
            } else if (principle == null) {
              $scope.errorAddRecordPrinciple = "This principle is not found in allowable principles for this map project";
            } else {
              $scope.errorAddRecordPrinciple = "";

              // add localId to this principle
              principle.localId = currentLocalId++;

              // check if principle already present
              var principlePresent = false;
              for (var i = 0; i < record.mapPrinciple.length; i++) {
                if (principle.id == record.mapPrinciple[i].id)
                  principlePresent = true;
              }

              if (principlePresent == true) {
                $scope.errorAddRecordPrinciple = "The principle with id "
                    + principle.principleId
                    + " is already attached to the map record";
              } else {
                console.debug('Adding principle');
                $scope.record['mapPrinciple'].push(principle);
              }
              ;

              $scope.principleInput = "";
            }
            ;

            broadcastRecord();
          };

          $scope.removeRecordPrinciple = function(record, principle) {
            record['mapPrinciple'] = removeJsonElement(record['mapPrinciple'],
                principle);
            $scope.record = record;

            console.debug('Removed principle');

            broadcastRecord();
          };

          $scope.tinymceOptions = {

            menubar : false,
            statusbar : false,
            plugins : "autolink autoresize link image charmap searchreplace lists paste",
            toolbar : "undo redo | styleselect lists | bold italic underline strikethrough | charmap link image",

            setup : function(ed) {

              // added to fake two-way binding from the html
              // element
              // noteInput is not accessible from this javascript
              // for some reason
              ed.on('keyup', function(e) {
                $scope.tinymceContent = ed.getContent();
                $scope.$apply();
              });
            }
          };

          $scope.editRecordNote = function(record, mapNote) {
            $scope.noteInput = "HELLO HELLO";
            $scope.noteEditMode = true;
            $scope.noteEditId = mapNote.localId;
          };

          $scope.cancelEditRecordNote = function() {
            $scope.noteInput = '';
            $scope.noteEditMode = false;
            $scope.noteEditId = null;
          };

          $scope.saveEditRecordNote = function(record, note) {

            if ($scope.noteEditMode == true) {

              var noteFound = false;

              // find the existing note
              for (var i = 0; i < record.mapNote.length; i++) {

                // if this note, overwrite it
                if ($scope.noteEditId = record.mapNote[i].localId) {
                  noteFound = true;
                  record.mapNote[i].note = note;
                }
              }

              if (noteFound = false) {
                console.debug("ERROR: Could not find note to edit with id = "
                    + $scope.noteEditId);
              }
            } else {
              console
                  .debug("Save Edit Record Note called when not in edit mode");
            }
          };

          $scope.addRecordNote = function(record, note) {
            // check if note non-empty
            if (note === '' || note == null) {
              $scope.errorAddRecordNote = "Note cannot be empty";
            } else {

              // construct note object
              var mapNote = new Object();
              mapNote.id = null;
              mapNote.localId = currentLocalId++;
              mapNote.note = note;
              mapNote.timestamp = (new Date()).getTime();
              mapNote.user = $scope.user;

              // add note to record
              record['mapNote'].addElement(mapNote);

              // set the text area to null
              $scope.tinymceContent = null;

            }

            broadcastRecord();
          };

          $scope.removeRecordNote = function(record, note) {
            console.debug("Removing note");
            console.debug(note);
            record['mapNote'].removeElement(note);
            $scope.record = record;
            broadcastRecord();

            // if in edit mode, cancel
            if ($scope.noteEditMode == true) {
              $scope.cancelEditRecordNote();
            }
          };

          /**
           * FEEDBACK FUNCTIONS
           */
          $scope.sendFeedback = function(record, feedbackMessage, recipientList) {
            console.debug("Adding feedback", record);

            if (feedbackMessage == null || feedbackMessage == undefined
                || feedbackMessage === '') {
              window.alert("The feedback field cannot be blank. ");
              return;
            }

            var localFeedback = $scope.conversation.feedback;

            // copy recipient list
            var localRecipients = recipientList.slice(0);
            var newRecipients = new Array();
            for (var i = 0; i < localRecipients.length; i++) {
              for (var j = 0; j < $scope.project.mapLead.length; j++) {
                if (localRecipients[i].id == $scope.project.mapLead[j].id)
                  newRecipients.push($scope.project.mapLead[j]);
              }
            }

            // if the conversation hasn't yet been started
            if ($scope.conversation == null || $scope.conversation == "") {

              // create first feedback item to go into the
              // feedback conversation
              var feedback = {
                "message" : feedbackMessage,
                "mapError" : "",
                "timestamp" : new Date(),
                "sender" : $scope.user,
                "recipients" : newRecipients,
                "isError" : "false",
                "feedbackConversation" : $scope.conversation,
                "viewedBy" : [ $scope.user ]
              };

              var feedbacks = new Array();
              feedbacks.push(feedback);

              // create feedback conversation
              var feedbackConversation = {
                "lastModified" : new Date(),
                "terminology" : $scope.project.sourceTerminology,
                "terminologyId" : record.conceptId,
                "terminologyVersion" : $scope.project.sourceTerminologyVersion,
                "isResolved" : "false",
                "isDiscrepancyReview" : "false",
                "mapRecordId" : record.id,
                "feedback" : feedbacks,
                "defaultPreferredName" : $scope.concept.defaultPreferredName,
                "title" : "Feedback",
                "mapProjectId" : $scope.project.id,
                "userName" : record.owner.userName
              };

              $http({
                url : root_workflow + "conversation/add",
                dataType : "json",
                data : feedbackConversation,
                method : "PUT",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(function(data) {
                console.debug("success to addFeedbackConversation.");
                $scope.conversation = feedbackConversation;
              }).error(function(data, status, headers, config) {
                $scope.recordError = "Error adding new feedback conversation.";
                $rootScope.handleHttpError(data, status, headers, config);
              });

            } else { // already started a conversation

              // create feedback msg to be added to the
              // conversation
              var feedback = {
                "message" : feedbackMessage,
                "mapError" : "",
                "timestamp" : new Date(),
                "sender" : $scope.user,
                "recipients" : newRecipients,
                "isError" : "false",
                "viewedBy" : [ $scope.user ]
              };

              localFeedback.push(feedback);
              $scope.conversation.feedback = localFeedback;

              $http({
                url : root_workflow + "conversation/update",
                dataType : "json",
                data : $scope.conversation,
                method : "POST",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(function(data) {
                console.debug("success to update Feedback conversation.");
              }).error(function(data, status, headers, config) {
                $scope.recordError = "Error updating feedback conversation.";
                $rootScope.handleHttpError(data, status, headers, config);
              });
            }
          };

          $scope.assignGroupAndPriority = function() {

            // if not group structured project
            if ($scope.project.groupStructure == false) {

              // cycle over entries and assign map priority based
              // on position
              for (var i = 0; i < $scope.entries.length; i++) {
                $scope.entries[i].mapPriority = i + 1;
              }

              formattedRecord.mapEntry = $scope.entries;

              // if group structured project
            } else {

              var entries = new Array();

              // cycle over each group bin
              for (var i = 0; i < $scope.entries.length; i++) {

                // cycle over entries in each group bin
                for (var j = 0; j < $scope.entries[i].length; j++) {

                  console.debug("Assigning group and priority to " + i + " "
                      + j);
                  $scope.entries[i][j].mapGroup = i;
                  $scope.entries[i][j].mapPriority = j + 1;

                  entries.push($scope.entries[i][j]);

                }
                ;
              }
              ;

              console.debug("record formatted:");
              console.debug(entries);

              $scope.record.mapEntry = entries;
            }

          };

          /**
           * MAP ENTRY FUNCTIONS
           */

          $scope.entriesEqualById = function(entry1, entry2) {

            // if hibernate id, test on id
            if (entry1.id != null && entry1.id != '') {
              return entry1.id === entry2.id;
              // otherwise, local id
            } else {
              return entry1.localId === entry2.localId;
            }
          };

          // Returns all entries belonging to a particular map group
          $scope.getEntries = function(mapGroup) {

            // if no argument, return all entries
            if (mapGroup == null) {
              return $scope.record.mapEntry;
            }

            // cycle over map entries and extract those with this
            // map group
            var entries = new Array();

            for (var i = 0; i < $scope.record.mapEntry.length; i++) {
              if (parseInt($scope.record.mapEntry[i].mapGroup, 10) === parseInt(
                  mapGroup, 10)) {
                entries.push($scope.record.mapEntry[i]);
              }
              ;
            }
            ;

            return entries;
          };

          $scope.getEntrySummary = function(entry) {

            var entrySummary = "";
            // first get the rule
            entrySummary += $scope.getRuleSummary(entry);

            // if target is null, check relation id
            if (entry.targetId == null || entry.targetId === '') {

              // if relation id is null or empty, return empty
              // entry string
              if (entry.mapRelation == null || entry.mapRelation === '') {
                entrySummary += '[NO TARGET OR RELATION]';

                // otherwise, return the relation abbreviation
              } else {
                entrySummary += entry.mapRelation.abbreviation;

              }
              // otherwise return the target code and preferred
              // name
            } else {
              entrySummary += entry.targetId + " " + entry.targetName;
            }

            return entrySummary;

          };

          // Returns a summary string for the entry rule type
          $scope.getRuleSummary = function(entry) {

            var ruleSummary = "";

            // first, rule summary
            if ($scope.project.ruleBased == true) {
              if (entry.rule.toUpperCase().indexOf("TRUE") != -1)
                ruleSummary += "[TRUE] ";
              else if (entry.rule.toUpperCase().indexOf("FEMALE") != -1)
                ruleSummary += "[FEMALE] ";
              else if (entry.rule.toUpperCase().indexOf("MALE") != -1)
                ruleSummary += "[MALE] ";
              else if (entry.rule.toUpperCase().indexOf("AGE") != -1) {

                var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/);
                var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/);

                ruleSummary += '[AGE ';

                if (lowerBound != null && lowerBound != ''
                    && lowerBound.length > 0) {
                  ruleSummary += lowerBound[0];
                  if (upperBound != null && upperBound != ''
                      && upperBound.length > 0)
                    ruleSummary += ' AND ';
                }
                if (upperBound != null && upperBound != ''
                    && upperBound.length > 0)
                  ruleSummary += upperBound[0];

                ruleSummary += '] ';
              }
            }

            return ruleSummary;

          };
          // Sets the scope variable for the active entry
          $scope.selectMapEntry = function(entry) {
            console.debug("Select entry");
            console.debug(entry);

            // set all entries isSelected to false
            for (var i = 0; i < $scope.groupsTree.length; i++) {

              for (var j = 0; j < $scope.groupsTree[i].entry.length; j++) {
                console.debug($scope.groupsTree[i].entry[j]);
                $scope.groupsTree[i].entry[j].isSelected = false;
              }
            }

            // set this entry to selected
            entry.isSelected = true;

            console.debug("Entry selected, new entries: ", $scope.entries);

            $rootScope.$broadcast(
                'mapRecordWidget.notification.changeSelectedEntry', {
                  key : 'changeSelectedEntry',
                  entry : angular.copy(entry),
                  record : $scope.record,
                  project : $scope.project
                });

          };

          // function for adding an empty map entry to a record
          $scope.addMapEntry = function(group) {

            if (group == null || group == undefined) {
              $scope.addMapGroup();
              group = $scope.groupsTree[0];
            }

            // create blank entry associated with this id
            var newEntry = {
              "id" : null,
              "mapRecordId" : $scope.record.id,
              "targetId" : null,
              "targetName" : null,
              "rule" : ($scope.project.ruleBased == true ? "TRUE" : null),
              "mapPriority" : group.entry.length + 1,
              "mapRelation" : null,
              "mapBlock" : "",
              "mapGroup" : group.mapGroup,
              "mapAdvice" : [],
              "mapPrinciples" : [],
              "localId" : currentLocalId + 1,
              "isSelected" : false
            };

            currentLocalId += 1;

            newEntry.ruleSummary = $scope.getRuleSummary(newEntry);

            group.entry.push(newEntry);
            $scope.selectMapEntry(newEntry);

            $scope.saveGroups();

          };

          $scope.deleteMapEntry = function(entry) {

            console.debug("deleteMapEntry from group ", entry,
                $scope.groupsTree);

            var entries = [];
            for (var i = 0; i < $scope.groupsTree[entry.mapGroup - 1].entry.length; i++) {
              if ($scope.groupsTree[entry.mapGroup - 1].entry[i].localId != entry.localId) {
                entries.push($scope.groupsTree[entry.mapGroup - 1].entry[i]);
              }
            }

            $scope.groupsTree[entry.mapGroup - 1].entry = entries;

            console.debug("Groups after delete:", $scope.groupsTree);

            $scope.saveGroups();

          };

          // Notification watcher for save/delete entry events
          $scope
              .$on(
                  'mapEntryWidget.notification.modifySelectedEntry',
                  function(event, parameters) {
                    console
                        .debug("MapRecordWidget: Detected entry modify request");

                    var entry = parameters.entry;
                    var record = parameters.record;

                    // verify that this entry is attached to
                    // this record
                    if (record.id != $scope.record.id) {
                      console.error("Non-matching record (id= "
                          + $scope.record.id
                          + ") will ignore entry modification request.");
                    } else {

                      $rootScope.currentPageDirty = true;

                      if (parameters.action === "save") {

                        console.debug("Action: SAVE", entry);

                        // check that the entry id
                        // matches
                        if ($scope.groupsTree[entry.mapGroup - 1].entry[entry.mapPriority - 1].localId != entry.localId) {
                          console.error("Could not find entry ", entry);
                          return;
                        }

                        // replace the entry
                        $scope.groupsTree[entry.mapGroup - 1].entry[entry.mapPriority - 1] = entry;

                      } else {
                        console
                            .error("MapRecordWidget: Invalid action requested for entry modification");
                      }
                    }

                    $scope.saveGroups();
                  });

          // ///////////////////////
          // Map Group Functions //
          // ///////////////////////

          // Adds a map group to the existing list
          $scope.addMapGroup = function() {

            // create a blank group
            var group = {
              "type" : "group",
              "mapGroup" : $scope.groupsTree.length + 1,
              "entry" : []
            };

            // add an entry to this group
            $scope.addMapEntry(group);

            // add the group
            $scope.groupsTree.push(group);

            // save the groups
            $scope.saveGroups();
          };

          // Removes a map group if it exists
          $scope.deleteMapGroup = function(group) {

            console.debug('mapGroup');

            var groupsTreeTemp = new Array();
            $.map($scope.groupsTree, function(v, i) {
              console.debug(v['mapGroup'], group['mapGroup']);
              if (v['mapGroup'] != group['mapGroup'])
                groupsTreeTemp.push(v);
            });

            $scope.groupsTree = groupsTreeTemp;

            // re-calculate group and priority
            $scope.saveGroups();

          };

          // /////////////////////
          // Utility Functions //
          // /////////////////////

          // sort and return an array by string key
          function sortByKey(array, key) {
            return array.sort(function(a, b) {
              var x = a[key];
              var y = b[key];
              return ((x < y) ? -1 : ((x > y) ? 1 : 0));
            });
          }
          ;

          // function to add an element and assign a local id if not
          // tracked by hibernate
          Array.prototype.addElement = function(elem) {

            // if hibernate id, simply add
            if (elem.id != null && elem.id != '') {
              this.push(elem);

              // otherwise, assign a unique localid
            } else {

              // get the maximum local id already assigned in this
              // array
              var maxLocalId = -1;
              $.map(this, function(v, i) {
                if (v.hasOwnProperty("localId")) {
                  if (v['localId'] > maxLocalId)
                    maxLocalId = v['localId'];
                }
              });

              elem['localId'] = maxLocalId == -1 ? 1 : maxLocalId + 1;
            }

            this.push(elem);
          };

          // function to remove an element by id or localid
          // instantiated to negate necessity for equals methods for
          // map objects
          // which may not be strictly identical via string or key
          // comparison
          Array.prototype.removeElement = function(elem) {

            console.debug('before removeElement', elem, $scope.entries);

            // switch on type of id
            var idType = elem.hasOwnProperty('localId') ? 'localId' : 'id';

            console.debug('idType = ', idType);

            var array = new Array();
            $.map(this, function(v, i) {
              console.debug(v[idType], elem[idType]);
              if (v[idType] != elem[idType])
                array.push(v);
            });

            this.length = 0; // clear original array
            this.push.apply(this, array); // push all elements
            // except the one we
            // want to delete

            console.debug('after removeElement', elem, $scope.entries);
          };

          function removeJsonElement(array, elem) {

            console.debug("Removing element");
            var newArray = [];
            for (var i = 0; i < array.length; i++) {
              if (array[i].id != elem.id) {
                console.debug("Pushing element " + array[i].id);
                newArray.push(array[i]);
              }
            }

            console.debug("After remove, before return:");
            console.debug(newArray);
            return newArray;
          }

          // function to return trusted html code (for tooltip
          // content)
          $scope.to_trusted = function(html_code) {
            return $sce.trustAsHtml(html_code);
          };

          $scope.getBrowserUrl = function() {
            if ($scope.user.userName === 'guest')
              return "http://browser.ihtsdotools.org/index.html?perspective=full&conceptId1="
                  + $scope.concept.terminologyId
                  + "&diagrammingMarkupEnabled=true&acceptLicense=true";
            else
              return "http://dailybuild.ihtsdotools.org/index.html?perspective=full&conceptId1="
                  + $scope.concept.terminologyId
                  + "&diagrammingMarkupEnabled=true&acceptLicense=true";
          };

          $scope.openConceptBrowser = function() {
            var myWindow = window.open($scope.getBrowserUrl(), "browserWindow");
            myWindow.focus();
          };

          $scope.isFeedbackViewed = function() {
            if ($scope.conversation == null || $scope.conversation == "")
              return true;
            for (var i = 0; i < $scope.conversation.feedback.length; i++) {
              var alreadyViewedBy = $scope.conversation.feedback[i].viewedBy;
              var found = false;
              for (var j = 0; j < alreadyViewedBy.length; j++) {
                if (alreadyViewedBy[j].userName == $scope.user.userName)
                  found = true;
              }
              if (found == false)
                return false;
            }
            return true;
          };

          // add current user to list of viewers who have seen the
          // feedback conversation
          $scope.markViewed = function() {
            var needToUpdate = false;
            if ($scope.conversation == null || $scope.conversation == "")
              return;
            for (var i = 0; i < $scope.conversation.feedback.length; i++) {
              var alreadyViewedBy = $scope.conversation.feedback[i].viewedBy;
              var found = false;
              for (var j = 0; j < alreadyViewedBy.length; j++) {
                if (alreadyViewedBy[j].userName == $scope.user.userName)
                  found = true;
              }
              if (found == false) {
                alreadyViewedBy.push($scope.user);
                needToUpdate = true;
              }
            }

            if (needToUpdate == true) {
              $http({
                url : root_workflow + "conversation/update",
                dataType : "json",
                data : $scope.conversation,
                method : "POST",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(function(data) {
                console.debug("success to update Feedback conversation.");
              }).error(function(data, status, headers, config) {
                $scope.recordError = "Error updating feedback conversation.";
                $rootScope.handleHttpError(data, status, headers, config);
              });
            }
          };

          function initializeReturnRecipients() {

            // if no previous feedback conversations, return just
            // first map lead in list
            if ($scope.conversation == null || $scope.conversation == "") {
              $scope.returnRecipients.push($scope.project.mapLead[0]);
              return;
            }

            // figure out the return recipients based on previous
            // feedback in conversation
            var localFeedback = $scope.conversation.feedback;
            var localSender = localFeedback[localFeedback.length - 1].sender;
            var localRecipients = localFeedback[localFeedback.length - 1].recipients;
            if (localSender.userName == $scope.user.userName)
              $scope.returnRecipients = localRecipients;
            else {
              $scope.returnRecipients.push(localSender);
              for (var i = 0; i < localRecipients.length; i++) {
                if (localRecipients[i].userName != $scope.user.userName)
                  $scope.returnRecipients.push(localRecipients[i]);
              }
            }
            return;
          }
          ;

          // for multi-select user picklist
          function organizeUsers(arr) {
            // remove Current user
            for (var i = arr.length; i--;) {
              if (arr[i].userName === $scope.user.userName) {
                arr.splice(i, 1);
              }
            }

            // remove demo users
            for (var i = arr.length; i--;) {
              if (arr[i].name.indexOf("demo") > -1) {
                arr.splice(i, 1);
              }
            }

            sortByKey(arr, "name");
          }

          // sort and return an array by string key
          function sortByKey(array, key) {
        return array.sort(function(a, b) {
          var x = a[key];
          var y = b[key];
          return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });
      }
      ;

      $scope.openIndexViewer = function() {
        console.debug("page location is", window.location.href);
        var currentUrl = window.location.href;
        var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
        var newUrl = baseUrl + "/index/viewer";
        var myWindow = window.open(newUrl, "indexViewerWindow");

        myWindow.document.title = 'testing';
        myWindow.focus();
      };

      
    });
