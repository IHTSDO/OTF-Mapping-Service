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
    [
      '$scope',
      '$window',
      '$rootScope',
      '$http',
      '$routeParams',
      '$location',
      '$sce',
      '$uibModal',
      'localStorageService',
      'utilService',
      'appConfig',
      'gpService',
      function($scope, $window, $rootScope, $http, $routeParams, $location, $sce, $uibModal,
        localStorageService, utilService, appConfig, gpService) {

        // ///////////////////////////////////
        // Map Record Controller Functions //
        // ///////////////////////////////////

        // this controller handles a potentially 'dirty' page
        // $rootScope.currentPageDirty = true;

        var latestNoteId = null;

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
        $scope.enableAuthoringHistoryButton = (appConfig["deploy.show.authoring.history.button"] === 'true') ? true : false;
        
        $scope.returnRecipients = new Array();
        $scope.multiSelectSettings = {
          displayProp : 'name',
          scrollableHeight : '150',
          scrollable : true,
          showCheckAll : false,
          showUncheckAll : true
        };
        $scope.multiSelectCustomTexts = {
          buttonDefaultText : 'Select Leads'
        };

        // start note edit mode in off mode
        $scope.feedbackEditMode = false;
        $scope.feedbackEditId = null;
        $scope.newFeedbackMessages = new Array();
        $scope.feedbackContent = {
          text : ''
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

        for (var i = 0; i < $scope.groupOpen.length; i++) {
          $scope.groupOpen[i] = true;
        }

        // start note edit mode in off mode
        $scope.noteEditMode = false;
        $scope.noteEditId = null;
        $scope.newNoteTimestamps = new Array();
        $scope.noteContent = {
          text : ''
        };

        // tooltip for Save/Next button
        $scope.dynamicTooltip = '';

        // groups tree for dynamic sorting/repositioning
        $scope.groupsTree = [];

        // flag indicating if index viewer is available for dest terminology
        $scope.indexViewerExists = false;

        // options for angular-ui-tree
        $scope.options = {
          accept : function(sourceNode, destNodes, destIndex) {
            var sourceType = sourceNode.$modelValue.type;
            var destType = destNodes.$modelValue[0].type;
            return sourceType == destType;
          },
          dragStart : function(event) {
            $scope.dragging = true;
          },
          dragStop : function(event) {
            $scope.dragging = false;
          },
          dropped : function(event) {
            $scope.saveGroups();
          },
        };

        // function with two purposes:
        // (1) update the group and priority of the group tree
        // (2) update the entries of the record
        $scope.saveGroups = function() {
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
          // n/a
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
            console.debug('  => on compareRecordsWidget.notification.selectRecord = ',
              parameters.record);

            // If not QA_NEW, REVIEW_NEW, or CONFLICT_NEW, bail here
            // and let the "retrieveRecord" load this record.
            if (!$scope.record.workflowStatus.endsWith('_NEW')) {
              return;
            }

            $scope.record = parameters.record;        
            
            //Set the localId to the current max
            for(var i=0; i<$scope.record.mapEntry.length; i++){
              if($scope.record.mapEntry[i].localId > currentLocalId){
                  currentLocalId = $scope.record.mapEntry[i].localId;
              }
            }                
            
            // open principles accordion if one was copied from selectedRecord
            if ($scope.record.mapPrinciple && $scope.record.mapPrinciple.length > 0) {
              $scope.isPrinciplesOpen = true;
            }

            // This MUST not be removed for 'Start here' to work
            initializeGroupsTree();

            // Validate the record immediately
            // this is good to show messages right away
            // if there are problems
            gpService.increment();
            console.debug('Validate record on select', $scope.record);
            $http({
              url : root_mapping + 'validation/record/validate',
              dataType : 'json',
              data : $scope.record,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              console.debug('  validation data = ', data);
              gpService.decrement();
              $scope.validationResult = data;
            }).error(function(data, status, headers, config) {
              gpService.decrement();
              $scope.validationResult = null;
              $rootScope.handleHttpError(data, status, headers, config);
            });
          });

        // on successful retrieval of project, get the
        // record/concept
        $scope.$watch([ 'project', 'userToken', 'role', 'user', 'record' ], function() {
          if ($scope.project != null && $scope.userToken != null) {
            $http.defaults.headers.common.Authorization = $scope.userToken;
            setIndexViewerStatus();

            // Retrieve the record upon being loaded
            retrieveRecord();
            
            // Initialize terminology notes
            utilService.initializeTerminologyNotes($scope.project.id);
          }
        });

        // any time the record changes, broadcast it to the record
        // summary widget
        // $scope.$watch('record', function() {
        // broadcastRecord();
        // });

        function broadcastRecord() {
          console.debug('broadcast mapRecordWidget.notification.recordChanged =', $scope.record);
          $rootScope.$broadcast('mapRecordWidget.notification.recordChanged', {
            record : angular.copy($scope.record),
            project : $scope.project
          });
        }

        // initialize local variables
        var currentLocalId = 1; // used for addition
        // of new entries
        // without hibernate id

        // function to initially retrieve the project

        function retrieveRecord() {

          // obtain the record
          console.debug('Get record', $routeParams.recordId);
          $http({
            url : root_mapping + 'record/id/' + $routeParams.recordId,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
            function(data) {
              console.debug('  record = ', data.id, data.workflowStatus, data);

              // *_NEW workflowStatus is a case where we are going to copy a
              // record from compareRecords to get started so wait for the
              // notification for "selectRecord" happen
              if (!data.workflowStatus.endsWith('_NEW')) {
                $scope.record = data;

                // verify that all entries on this record with no
                // target have 'No
                // target' set as target name
                for (var i = 0; i < $scope.record.mapEntry.length; i++) {
                  if ($scope.record.mapEntry[i].targetId == null
                    || $scope.record.mapEntry[i].targetId == null
                    || $scope.record.mapEntry[i].targetId === '')
                    $scope.record.mapEntry[i].targetName = 'No target';
                }
              } else {
                // Set basic stuff for the "then.." below
                $scope.record = {};
                // used by selectRecord
                $scope.record.workflowStatus = data.workflowStatus;
                $scope.record.conceptId = data.conceptId;
                $scope.record.conceptName = data.conceptName;
                $scope.record.owner = data.owner;
                $scope.record.mapProjectId = data.mapProjectId;
                $scope.record.id = data.id;
                $scope.record.originIds = data.originIds;
              }

            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(
            function() {

              // check that this user actually owns this record
              if ($scope.record.owner.userName != $scope.user.userName) {
                $rootScope.handleReturnToDashboardError(
                  'Attempted to edit a record owned by another user; returned to dashboard',
                  $scope.role);
              } else {
                // obtain the record concept
                console.debug('get concept', $scope.record.conceptId);
                $http(
                  {
                    url : root_content + 'concept/id/' + $scope.project.sourceTerminology + '/'
                      + $scope.project.sourceTerminologyVersion + '/' + $scope.record.conceptId,
                    dataType : 'json',
                    method : 'GET',
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                  console.debug('  concept = ', data);
                  $scope.concept = data;
                  $scope.conceptBrowserUrl = $scope.getBrowserUrl();
                  // initialize the dynamic tooltip on the Save/Next button
                  // with the next concept to be mapped
                  $scope.resolveNextConcept(true);
                }).error(function(data, status, headers, config) {
                  $rootScope.handleHttpError(data, status, headers, config);
                });

                // IF id was set above, do this - otherwise it will get done
                // later
                if ($scope.record.id) {
                  // initialize the entries
                  initializeGroupsTree();

                  // add code to get feedback conversations
                  console.debug('Get feedback conversation for record', $scope.record.id);
                  $http({
                    url : root_workflow + 'conversation/id/' + $scope.record.id,
                    dataType : 'json',
                    method : 'GET',
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                    console.debug('  conversation = ', data);
                    if (data) {
                      $scope.conversation = data;
                      initializeReturnRecipients();
                    }
                  }).error(function(data, status, headers, config) {
                    $rootScope.handleHttpError(data, status, headers, config);
                  });
                }
              }
            });

        }

        $scope.isNewFeedback = function(feedback) {
          if ($scope.newFeedbackMessages.includes(feedback.message)) {
            return true;
          }
          return false;
        }

        $scope.editFeedback = function(feedback) {
          $scope.feedbackContent.text = feedback.message;
          $scope.feedbackEditMode = true;
          $scope.feedbackEditId = feedback.id ? feedback.id : feedback.localId;
        };

        $scope.cancelEditFeedback = function() {
          $scope.feedbackContent.text = '';
          $scope.feedbackEditMode = false;
          $scope.feedbackEditId = null;
          $scope.tinymceContent = '';
        };

        $scope.saveEditFeedback = function(feedback) {

          if ($scope.feedbackEditMode == true) {
            var feedbackFound = false;
            // find the existing feedback
            for (var i = 0; i < $scope.conversation.feedback.length; i++) {
              // if this feedback, overwrite it
              if ($scope.feedbackEditId == $scope.conversation.feedback[i].localId
                || $scope.feedbackEditId == $scope.conversation.feedback[i].id) {
                feedbackFound = true;
                $scope.conversation.feedback[i].message = feedback;
                //$scope.conversation.feedback[i].id = currentLocalId++;
              }
            }
            $scope.feedbackEditMode = false;
            $scope.tinymceContent = null;

            console.debug('update conversation', $scope.conversation);
            $http({
              url : root_workflow + 'conversation/update',
              dataType : 'json',
              data : $scope.conversation,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              console.debug('  conversation updated = ', data);
              $http({
                url : root_workflow + 'conversation/id/' + $scope.record.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                $scope.newFeedbackMessages.push(feedback);
                $scope.conversation = data;
              });
            }).error(function(data, status, headers, config) {
              $scope.recordError = 'Error updating feedback conversation.';
              $rootScope.handleHttpError(data, status, headers, config);
            });
          }
        };

        function setIndexViewerStatus() {
          console.debug('Get index viewer status', $scope.project.destinationTerminology);
          $http(
            {
              url : root_content + 'index/' + $scope.project.destinationTerminology + '/'
                + $scope.project.destinationTerminologyVersion,
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            console.debug('  indexViewerData = ', data);
            if (data.searchResult.length > 0) {
              $scope.indexViewerExists = true;
            } else {
              $scope.indexViewerExists = false;
            }
          }).error(function(data, status, headers, config) {
            $scope.indexViewerExists = false;
          });
        }

        // /////////////////////////////
        // Initialization Functions ///
        // /////////////////////////////

        function initializeGroupsTree() {
          $scope.groupsTree = [];
          // if no entries on this record, create a group and
          // entry
          if ($scope.record.mapEntry.length == 0) {
            $scope.addMapGroup();
          } else {

            for (var i = 0; i < $scope.record.mapEntry.length; i++) {
              // get the entry
              var entry = $scope.record.mapEntry[i];

              // check if group already exists
              var groupExists = false;
              for (var j = 0; j < $scope.groupsTree.length; j++) {
                if ($scope.groupsTree[j].mapGroup === entry.mapGroup) {
                  $scope.groupsTree[j].entry.push(entry);
                  groupExists = true;
                }
              }

              // if group does not exist, add a new group
              if (groupExists == false) {
                var group = {
                  'type' : 'group',
                  'mapGroup' : entry.mapGroup,
                  'entry' : []
                };
                group.entry.push(entry);
                $scope.groupsTree.push(group);

              }

            }

            // groups may be disordered, re-order the groups and add
            // empty
            // groups if necessary
            var groupsTreeTemp = angular.copy($scope.groupsTree);
            $scope.groupsTree = [];

            // get max group present
            var maxGroup = 0;
            for (var i = 0; i < groupsTreeTemp.length; i++) {
              maxGroup = Math.max(maxGroup, groupsTreeTemp[i].mapGroup);
            }
            // cycle from 1:maxGroup and add existing group or blank
            // group
            for (var group = 1; group <= maxGroup; group++) {

              var groupFound = false;

              // cycle over all existing groups
              for (var i = 0; i < groupsTreeTemp.length; i++) {

                // if group matches, push it next onto list
                if (groupsTreeTemp[i].mapGroup == group) {
                  $scope.groupsTree.push(groupsTreeTemp[i]);
                  groupFound = true;
                }
              }

              // if group not found, add a new (blank) group
              if (groupFound == false) {
                $scope.addMapGroup();
              }

            }
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
            if (confirm('You have unsaved text in the Map Notes. Do you wish to continue saving? The note will be lost.') == false) {
              return;
            }
          }

          // validate the record
          gpService.increment();
          console.debug('Validate on finish', $scope.record);
          $http({
            url : root_mapping + 'validation/record/validate',
            dataType : 'json',
            data : $scope.record,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
            .success(function(data) {
              console.debug('  validation result = ' + data);
              gpService.decrement();
              $scope.validationResult = data;
            })
            .error(function(data, status, headers, config) {
              gpService.decrement();
              $scope.validationResult = null;
              $scope.recordError = 'Unexpected error reported by server.  Contact an admin.';
              $rootScope.handleHttpError(data, status, headers, config);
            })
            .then(
              function(data) {

                // if no error messages were returned, stop and display
                if ($scope.validationResult.errors.length == 0) {

                  var warningCheckPassed = true;

                  // if warnings found, check if
                  // this is a second click
                  if ($scope.validationResult.warnings.length != 0) {

                    // if the same number of warnings are present
                    if ($scope.savedValidationWarnings.length == $scope.validationResult.warnings.length) {

                      // check that the warnings are the same
                      for (var i = 0; i < $scope.savedValidationWarnings.length; i++) {
                        if ($scope.savedValidationWarnings[i] != $scope.validationResult.warnings[i]) {
                          warningCheckPassed = false;
                        }
                      }

                      // if a different number of warnings, automatic fail
                    } else {
                      warningCheckPassed = false;
                    }
                  }

                  // if the warning checks are passed, save the record
                  if (warningCheckPassed == true) {

                    // assign the current user to the lastModifiedBy field
                    $scope.record.lastModifiedBy = $scope.user;

                    gpService.increment();
                    console.debug('Finish record', $scope.record);
                    $http({
                      url : root_workflow + 'finish',
                      dataType : 'json',
                      data : $scope.record,
                      method : 'POST',
                      headers : {
                        'Content-Type' : 'application/json'
                      }
                    })
                      .success(function(data) {
                        console.debug('  record saved = ', data);
                        $scope.recordSuccess = 'Record saved.';
                        $scope.recordError = '';

                        // user has successfully finished record, page is no
                        // longer 'dirty'
                        $rootScope.currentPageDirty = false;

                        gpService.decrement();
                        $location.path($scope.role + '/dash');
                      })
                      .error(
                        function(data, status, headers, config) {
                          gpService.decrement();
                          $scope.recordError = 'Unexpected server error.  Try saving your work for later, and contact an admin.';
                          $rootScope.handleHttpError(data, status, headers, config);
                          $scope.recordSuccess = '';
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
                  $scope.recordSuccess = '';
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

          $scope.addMapGroup(); // automatically
          // adds entry as
          // well

          $window.window.scrollTo(0, 0);

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
              'startIndex' : startIndex,
              'maxResults' : 1,
              'sortField' : 'sortKey',
              'queryRestriction' : 'NEW'
            };

            // get the assigned work list
            gpService.increment();
            console.debug('Get assigned concepts', $scope.project.id);
            $http(
              {
                url : root_workflow + 'project/id/' + $scope.project.id + '/user/id/'
                  + $scope.user.userName + '/assignedConcepts',

                dataType : 'json',
                data : pfsParameterObj,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(
              function(data) {
                console.debug('  assignedWork = ', data);
                gpService.decrement();

                var assignedWork = data.searchResult;

                if (tooltipOnly == true) {
                  if (assignedWork.length == 0) {
                    $scope.dynamicTooltip = '';
                  } else {
                    $scope.dynamicTooltip = assignedWork[0].terminologyId + ' '
                      + assignedWork[0].value;
                  }
                } else {
                  // if there is no more
                  // assigned work, return to
                  // dashboard
                  if (assignedWork.length == 0) {
                    $location.path($scope.role + '/dash');

                    // otherwise redirect to
                    // the next record to be
                    // edited
                  } else {
                    $location.path('record/recordId/' + assignedWork[0].id);
                  }
                }

              }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });

            // otherwise, if a conflict record, query available
            // conflicts
          } else if ($scope.record.workflowStatus === 'CONFLICT_NEW'
            || $scope.record.workflowStatus === 'CONFLICT_IN_PROGRESS') {

            // construct a paging/filtering/sorting object
            var pfsParameterObj = {
              'startIndex' : startIndex,
              'maxResults' : 1,
              'sortField' : 'sortKey',
              'queryRestriction' : 'CONFLICT_NEW'
            };

            // get the assigned conflicts
            gpService.increment();
            console.debug('get assigned conflicts', $scope.project.id);
            $http(
              {
                url : root_workflow + 'project/id/' + $scope.project.id + '/user/id/'
                  + $scope.user.userName + '/assignedConflicts',

                dataType : 'json',
                data : pfsParameterObj,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(
              function(data) {
                console.debug('  assigned work = ', data);
                gpService.decrement();

                var assignedWork = data.searchResult;

                if (tooltipOnly == true) {
                  if (assignedWork.length == 0) {
                    $scope.dynamicTooltip = '';
                  } else {
                    $scope.dynamicTooltip = assignedWork[0].terminologyId + ' '
                      + assignedWork[0].value;
                  }
                } else {
                  // if there is no more
                  // assigned work, return to
                  // dashboard
                  if (assignedWork.length == 0) {
                    $location.path($scope.role + '/dash');

                    // otherwise redirect to
                    // the next record to be
                    // edited
                  } else {
                    $location.path('record/conflicts/' + assignedWork[0].id);
                  }
                }

              }).error(function(data, status, headers, config) {
              gpService.decrement();
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
              'startIndex' : startIndex,
              'maxResults' : 1,
              'sortField' : 'sortKey',
              'queryRestriction' : 'REVIEW_NEW'
            };
            // get the assigned review work
            gpService.increment();
            console.debug('get assigned review work', $scope.project.id);
            $http(
              {
                url : root_workflow + 'project/id/' + $scope.project.id + '/user/id/'
                  + $scope.user.userName + '/assignedReviewWork',

                dataType : 'json',
                data : pfsParameterObj,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(
              function(data) {
                console.debug('  assigned work = ', data);
                gpService.decrement();

                var assignedWork = data.searchResult;
                if (tooltipOnly == true) {
                  if (assignedWork.length == 0) {
                    $scope.dynamicTooltip = '';
                  } else {
                    $scope.dynamicTooltip = assignedWork[0].terminologyId + ' '
                      + assignedWork[0].value;
                  }
                } else {
                  // if there is no more
                  // assigned work, return to
                  // dashboard
                  if (assignedWork.length == 0) {
                    $location.path($scope.role + '/dash');

                    // otherwise redirect to
                    // the next record to be
                    // edited
                  } else {
                    $location.path('record/review/' + assignedWork[0].id);
                  }
                }
              }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });
          } else if ($scope.record.workflowStatus === 'QA_NEW'
            || $scope.record.workflowStatus === 'QA_IN_PROGRESS') {

            // construct a paging/filtering/sorting object
            var pfsParameterObj = {
              'startIndex' : startIndex,
              'maxResults' : 1,
              'sortField' : 'sortKey',
              'queryRestriction' : 'QA_NEW'
            };
            // get the assigned review work
            gpService.increment();
            console.debug('get assigned qa work', $scope.project.id);
            $http(
              {
                url : root_workflow + 'project/id/' + $scope.project.id + '/user/id/'
                  + $scope.user.userName + '/assignedQAWork',

                dataType : 'json',
                data : pfsParameterObj,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(
              function(data) {
                console.debug('  assigned work = ', data);
                gpService.decrement();

                var assignedWork = data.searchResult;
                if (tooltipOnly == true) {
                  if (assignedWork.length == 0) {
                    $scope.dynamicTooltip = '';
                  } else {
                    $scope.dynamicTooltip = assignedWork[0].terminologyId + ' '
                      + assignedWork[0].value;
                  }
                } else {
                  // if there is no more
                  // assigned work, return to
                  // dashboard
                  if (assignedWork.length == 0) {
                    $location.path($scope.role + '/dash');

                    // otherwise redirect to
                    // the next record to be
                    // edited
                  } else {
                    $location.path('record/review/' + assignedWork[0].id);
                  }
                }
              }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });
          }
        };

        $scope.saveMapRecord = function(returnBack) {
          // check that note box does not contain unsaved material
          if ($scope.tinymceContent != '' && $scope.tinymceContent != null) {
            if (confirm('You have unsaved text into the Map Notes. Do you wish to continue saving? The note will be lost.') == false) {
              return;
            }

          }
          
          // assign the current user to the lastModifiedBy field
          $scope.record.lastModifiedBy = $scope.user;

          // if only displaying record again, do not make rest call
          // if ($rootScope.currentPageDirty == false &&
          // !returnBack)
          // return;
          
          gpService.increment();
          console.debug('save record', $scope.record);
          $http({
            url : root_workflow + 'save',
            dataType : 'json',
            data : $scope.record,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            console.debug('  record saved = ', data);

            // user has successfully saved
            // record, page is no longer 'dirty'
            $rootScope.currentPageDirty = false;

            // $scope.record = data;
            $scope.recordSuccess = 'Record saved.';
            $scope.recordError = '';
            gpService.decrement();
            if (!returnBack) {
              $scope.resolveNextConcept(false);

            } else {
              $location.path($scope.role + '/dash');
            }
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.recordError = 'Error saving record.';
            $rootScope.handleHttpError(data, status, headers, config);
            $scope.recordSuccess = '';
          });
        };

        // discard changes
        $scope.cancelMapRecord = function() {

          gpService.increment();
          console.debug('cancel editing', $scope.record);
          $http({
            url : root_workflow + 'cancel',
            dataType : 'json',
            data : $scope.record,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

            // user has requested a cancel event, page is no
            // longer 'dirty'
            $rootScope.currentPageDirty = false;
            gpService.decrement();
            $location.path($scope.role + '/dash');
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };

        $scope.removeFeedback = function(message) {
          // confirm delete
          if (confirm('Are you sure that you want to delete a feedback message?') == false)
            return;

          $http({
            url : root_workflow + 'feedback/delete',
            dataType : 'json',
            data : message,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $http({
              url : root_workflow + 'conversation/id/' + $routeParams.recordId,
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.conversation = data;
            });
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error deleting feedback conversation from application.';
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }

        $scope.addRecordPrinciple = function(record, principle) {

          // check if principle valid
          if (principle === '') {
            $scope.errorAddRecordPrinciple = 'Principle cannot be empty';
          } else if (principle == null) {
            $scope.errorAddRecordPrinciple = 'This principle is not found in allowable principles for this map project';
          } else {
            $scope.errorAddRecordPrinciple = '';

            // add localId to this principle
            principle.localId = currentLocalId++;

            // check if principle already present
            var principlePresent = false;
            for (var i = 0; i < record.mapPrinciple.length; i++) {
              if (principle.id == record.mapPrinciple[i].id)
                principlePresent = true;
            }

            if (principlePresent == true) {
              $scope.errorAddRecordPrinciple = 'The principle with id ' + principle.principleId
                + ' is already attached to the map record';
            } else {
              $scope.record['mapPrinciple'].push(principle);
            }

            $scope.principleInput = '';
          }

          broadcastRecord();
        };

        $scope.removeRecordPrinciple = function(record, principle) {
          record['mapPrinciple'] = removeJsonElement(record['mapPrinciple'], principle);
          $scope.record = record;
          broadcastRecord();
        };

        $scope.tinymceOptions = {

          menubar : false,
          statusbar : false,
          plugins : 'autolink link image charmap searchreplace lists paste',
          toolbar : 'undo redo | styleselect lists | bold italic underline strikethrough | charmap link image',

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

        $scope.isEditableNote = function(mapNote) {
          if (($scope.record.workflowStatus != 'PUBLISHED')
            && ($scope.record.workflowStatus != 'READY_FOR_PUBLICATION')) {
            return true;
          }
          return false;
        }

        $scope.editRecordNote = function(record, mapNote) {
          $scope.noteContent.text = mapNote.note;
          $scope.noteEditMode = true;
          $scope.noteEditId = mapNote.id ? mapNote.id : mapNote.localId;
        };

        $scope.cancelEditRecordNote = function() {
          $scope.noteContent.text = '';
          $scope.noteEditMode = false;
          $scope.noteEditId = null;
          $scope.tinymceContent = '';
        };

        $scope.saveEditRecordNote = function(record, note) {

          if ($scope.noteEditMode == true) {
            var noteFound = false;
            // find the existing note
            for (var i = 0; i < record.mapNote.length; i++) {
              // if this note, overwrite it
              if ((record.mapNote[i].localId != null && $scope.noteEditId == record.mapNote[i].localId)
                || (record.mapNote[i].localId == null && $scope.noteEditId == record.mapNote[i].id)) {
                noteFound = true;
                record.mapNote[i].note = note;
                record.mapNote[i].timestamp = (new Date()).getTime();
                record.mapNote[i].user = $scope.user;
                broadcastRecord();
              }
            }
            $scope.noteEditMode = false;
            $scope.tinymceContent = null;
          }
        };

        $scope.addRecordNote = function(record, note) {

          // check if note non-empty
          if (note === '' || note == null) {
            $scope.errorAddRecordNote = 'Note cannot be empty';
          } else {

            $scope.errorAddRecordNote = null;

            // construct note object
            var mapNote = new Object();
            mapNote.id = null;
            mapNote.localId = currentLocalId++;
            mapNote.note = note;
            mapNote.timestamp = (new Date()).getTime();
            mapNote.user = $scope.user;

            // add note's timestamp to the newNote list
            $scope.newNoteTimestamps.push(mapNote.timestamp);

            // add note to record with new localId
            addElementWithId(record.mapNote, mapNote);

            $scope.tinymceContent = null;

          }

          broadcastRecord();
        };

        $scope.removeRecordNote = function(record, note) {
          var index = record.mapNote.indexOf(note);
          record['mapNote'].splice(index, 1);
          $scope.record = record;
          broadcastRecord();

          // if in edit mode, cancel
          if ($scope.noteEditMode == true) {
            $scope.cancelEditRecordNote();
          }
        };

        $scope.sendFeedback = function(record, feedbackMessage, recipientList) {
          
          console.debug("sendFeedback recipientList is ", recipientList);

          if (feedbackMessage == null || feedbackMessage == undefined || feedbackMessage === '') {
            window.alert('The feedback field cannot be blank. ');
            return;
          }

          var localTimestamp = new Date().getTime();

          // copy recipient list
          var localRecipients = recipientList.slice(0);
          var newRecipients = new Array();
          for (var i = 0; i < localRecipients.length; i++) {
            for (var j = 0; j < $scope.project.mapLead.length; j++) {
              if (localRecipients[i].id == $scope.project.mapLead[j].id)
                newRecipients.push($scope.project.mapLead[j]);
            }
          }
          if (newRecipients.length == 0) {
              window.alert('At least one recipient must be selected. ');
              return;
          }

          // if the conversation has not yet been started
          if ($scope.conversation == null || $scope.conversation == '') {

            // create first feedback item to go into the
            // feedback conversation
            var feedback = {
              'message' : feedbackMessage,
              'mapError' : '',
              'timestamp' : localTimestamp,
              'sender' : $scope.user,
              'recipients' : newRecipients,
              'isError' : 'false',
              'feedbackConversation' : $scope.conversation,
              'viewedBy' : [ $scope.user ]
            };

            var feedbacks = new Array();
            feedbacks.push(feedback);

            // create feedback conversation
            var feedbackConversation = {
              'lastModified' : new Date(),
              'terminology' : $scope.project.sourceTerminology,
              'terminologyId' : record.conceptId,
              'terminologyVersion' : $scope.project.sourceTerminologyVersion,
              'isResolved' : 'false',
              'isDiscrepancyReview' : 'false',
              'mapRecordId' : record.id,
              'feedback' : feedbacks,
              'defaultPreferredName' : $scope.concept.defaultPreferredName,
              'title' : 'Feedback',
              'mapProjectId' : $scope.project.id,
              'userName' : record.owner.userName
            };

            console.debug('add conversation', feedbackConversation);
            $http({
              url : root_workflow + 'conversation/add',
              dataType : 'json',
              data : feedbackConversation,
              method : 'PUT',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.newFeedbackMessages.push(feedbackMessage);
              console.debug('  feedback conversation = ', data);
              $scope.conversation = data;
              $scope.tinymceContent = null;
              $scope.feedbackContent.text = '';
            }).error(function(data, status, headers, config) {
              $scope.recordError = 'Error adding new feedback conversation.';
              $rootScope.handleHttpError(data, status, headers, config);
            });

          } else { // already started a conversation

            var localFeedback = $scope.conversation.feedback;
            
            // create feedback msg to be added to the
            // conversation
            var feedback = {
              'message' : feedbackMessage,
              'mapError' : '',
              'timestamp' : localTimestamp,
              'sender' : $scope.user,
              'recipients' : newRecipients,
              'isError' : 'false',
              'viewedBy' : [ $scope.user ]
            };

            localFeedback.push(feedback);
            $scope.tinymceContent = null;

            $scope.conversation.feedback = localFeedback;

            console.debug('update conversation', $scope.conversation);
            $http({
              url : root_workflow + 'conversation/update',
              dataType : 'json',
              data : $scope.conversation,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              console.debug('  conversation updated = ', data);
              $http({
                url : root_workflow + 'conversation/id/' + $scope.record.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                $scope.newFeedbackMessages.push(feedbackMessage);
                $scope.conversation = data;
                $scope.feedbackContent.text = '';
              });
            }).error(function(data, status, headers, config) {
              $scope.recordError = 'Error updating feedback conversation.';
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
                $scope.entries[i][j].mapGroup = i;
                $scope.entries[i][j].mapPriority = j + 1;

                entries.push($scope.entries[i][j]);
              }
            }
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
            if (parseInt($scope.record.mapEntry[i].mapGroup, 10) === parseInt(mapGroup, 10)) {
              entries.push($scope.record.mapEntry[i]);
            }
          }

          return entries;
        };

        $scope.getEntrySummary = function(entry) {

          var entrySummary = '';
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
            var allNotes = utilService.getNotes($scope.project.id);
            var notes = (allNotes && allNotes[entry.targetId]) ? allNotes[entry.targetId] : '';
            entrySummary += entry.targetId + notes + ' ' + entry.targetName;
          }

          return entrySummary;

        };

        // Returns a summary string for the entry rule type
        $scope.getRuleSummary = function(entry) {

          var ruleSummary = '';

          // first, rule summary
          if ($scope.project.ruleBased == true) {
            if (entry.rule.toUpperCase().indexOf('TRUE') != -1)
              ruleSummary += '[TRUE] ';
            else if (entry.rule.toUpperCase().indexOf('FEMALE') != -1)
              ruleSummary += '[FEMALE] ';
            else if (entry.rule.toUpperCase().indexOf('MALE') != -1)
              ruleSummary += '[MALE] ';
            else if (entry.rule.toUpperCase().indexOf('AGE') != -1) {

              var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/);
              var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/);

              ruleSummary += '[AGE ';

              if (lowerBound != null && lowerBound != '' && lowerBound.length > 0) {
                ruleSummary += lowerBound[0];
                if (upperBound != null && upperBound != '' && upperBound.length > 0)
                  ruleSummary += ' AND ';
              }
              if (upperBound != null && upperBound != '' && upperBound.length > 0)
                ruleSummary += upperBound[0];

              ruleSummary += '] ';
            }
          }

          return ruleSummary;

        };
        // Sets the scope variable for the active entry
        $scope.selectMapEntry = function(entry) {
          // set all entries isSelected to false
          for (var i = 0; i < $scope.groupsTree.length; i++) {
            for (var j = 0; j < $scope.groupsTree[i].entry.length; j++) {
              $scope.groupsTree[i].entry[j].isSelected = false;
            }
          }

          // set this entry to selected
          entry.isSelected = true;

          console.debug('broadcast mapRecordWidget.notification.changeSelectedEntry =',
            $scope.record, entry);
          $rootScope.$broadcast('mapRecordWidget.notification.changeSelectedEntry', {
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
            'id' : null,
            'mapRecordId' : $scope.record.id,
            'targetId' : null,
            'targetName' : 'No target',
            'rule' : ($scope.project.ruleBased == true ? 'TRUE' : null),
            'mapPriority' : group.entry.length + 1,
            'mapRelation' : null,
            'mapBlock' : '',
            'mapGroup' : group.mapGroup,
            'mapAdvice' : [],
            'mapPrinciples' : [],
            'localId' : currentLocalId + 1,
            'isSelected' : false
          };

          currentLocalId += 1;

          newEntry.ruleSummary = $scope.getRuleSummary(newEntry);

          group.entry.push(newEntry);
          $scope.selectMapEntry(newEntry);

          $scope.saveGroups();
        };

        $scope.deleteMapEntry = function(entry) {
          var entries = [];
          for (var i = 0; i < $scope.groupsTree[entry.mapGroup - 1].entry.length; i++) {
			// localId can be null if the map record was prepopulated from file
			if (entry.localId == null){
			  if ($scope.groupsTree[entry.mapGroup - 1].entry[i].id != entry.id) {
                entries.push($scope.groupsTree[entry.mapGroup - 1].entry[i]);
              }	
			}
			else{
			  if ($scope.groupsTree[entry.mapGroup - 1].entry[i].localId != entry.localId) {
                entries.push($scope.groupsTree[entry.mapGroup - 1].entry[i]);
              }	
			}            

          }

          $scope.groupsTree[entry.mapGroup - 1].entry = entries;
          $scope.saveGroups();

          // select something that is left
          if ($scope.record.mapEntry && $scope.record.mapEntry.length > 0) {
            $scope.selectMapEntry($scope.record.mapEntry[0]);
          }
        };

        // Notification watcher for save/delete entry events
        $scope
          .$on(
            'mapEntryWidget.notification.modifySelectedEntry',
            function(event, parameters) {
              console.debug('  => on mapEntryWidget.notification.modifySelectedEntry',
                parameters.record, parameters.entry);
              var entry = parameters.entry;
              var record = parameters.record;

              // verify that this entry is attached to
              // this record
              if (record.id == $scope.record.id) {
                $rootScope.currentPageDirty = true;

                if (parameters.action === 'save') {
                  // check that the entry id
                  // matches
                  if ($scope.groupsTree[entry.mapGroup - 1].entry[entry.mapPriority - 1].localId != entry.localId) {
                    return;
                  }

                  // replace the entry
                  $scope.groupsTree[entry.mapGroup - 1].entry[entry.mapPriority - 1] = entry;
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
            'type' : 'group',
            'mapGroup' : $scope.groupsTree.length + 1,
            'entry' : []
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

          var groupsTreeTemp = $scope.groupsTree.filter(function(g) {
            return g.mapGroup != group.mapGroup
          })

          $scope.groupsTree = groupsTreeTemp;

          // re-calculate group and priority
          $scope.saveGroups();

          // select something that is left
          if ($scope.record.mapEntry && $scope.record.mapEntry.length > 0) {
            $scope.selectMapEntry($scope.record.mapEntry[0]);
          }
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

        // function to add an element and assign a local id if not
        // tracked by hibernate
        function addElementWithId(array, elem) {

          // if no hibernate id, assign local id
          if (elem.id == null || elem.id === '') {
            // If first time adding note in session, use note array's max Note Id, else use note array's max Local Id
            if (latestNoteId == null) {
              if (array.length == 0) {
                latestNoteId = 0;
              } else {
                latestNoteId = Math.max.apply(null, array.map(function(v) {
                  return v.id;
                }));
              }
            }

            elem['localId'] = ++latestNoteId;
          }

          array.push(elem);
        }
        ;

        // function to remove an element by id or localid
        // instantiated to negate necessity for equals methods for
        // map objects
        // which may not be strictly identical via string or key
        // comparison
        Array.prototype.removeElement = function(elem) {
          // switch on type of id
          var idType = elem.hasOwnProperty('localId') ? 'localId' : 'id';

          var array = new Array();
          $.map(this, function(v, i) {
            if (v[idType] != elem[idType])
              array.push(v);
          });
          // clear original array
          this.length = 0;
          // push all elements except the one we want to delete
          this.push.apply(this, array);

        };

        function removeJsonElement(array, elem) {
          var newArray = [];
          for (var i = 0; i < array.length; i++) {
            if (array[i].id != elem.id) {
              newArray.push(array[i]);
            }
          }
          return newArray;
        }

        // function to return trusted html code (for tooltip
        // content)
        $scope.to_trusted = function(html_code) {
          return $sce.trustAsHtml(html_code);
        };

        // opens SNOMED CT browser
        $scope.getBrowserUrl = function() {
            if (appConfig['deploy.snomed.browser.force'] === 'true') {
              if ($scope.project.sourceTerminology.includes("SNOMED")) {
                return appConfig['deploy.snomed.browser.url'] + "&conceptId1="
                  + $scope.record.conceptId;
              }
              else {
                return appConfig['deploy.snomed.browser.url'];
              }
            }
            else {
              if ($scope.project.sourceTerminology === 'SNOMEDCT_US') {
                return appConfig['deploy.snomed.dailybuild.url.base']
                  + appConfig['deploy.snomed.dailybuild.url.us'] 
                  + "&conceptId1="
                  + $scope.record.conceptId;
              } else {                
                return appConfig['deploy.snomed.dailybuild.url.base']
                  + appConfig['deploy.snomed.dailybuild.url.other']
                  + "&conceptId1="
                  + $scope.record.conceptId;
              }
            }
        };
        
        $scope.openConceptBrowser = function() {
          window.open($scope.getBrowserUrl(), 'browserWindow');
        };
        
        $scope.openTerminologyBrowser = function(){
          var browserUrl = appConfig['deploy.terminology.browser.url'];
          if (browserUrl == null || browserUrl === "")
          {
            var currentUrl = window.location.href;
            var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
            var browserUrl = baseUrl + '/terminology/browser';
            
            if ($scope.project.sourceTerminology === 'SNOMEDCT' 
                || $scope.project.sourceTerminology === 'SNOMEDCT_US') {
              $scope.browserRequest = 'destination';
            } else {
              $scope.browserRequest = 'source';
            }
            localStorageService.add('browserRequest', $scope.browserRequest);
          }
          
          var myWindow = window.open(browserUrl, 'terminologyBrowserWindow');
          myWindow.focus();
        }

        $scope.isFeedbackViewed = function() {
          if ($scope.conversation == null || $scope.conversation == '')
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
          if ($scope.conversation == null || $scope.conversation == '')
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
            console.debug('update conversation', $scope.conversation);
            $http({
              url : root_workflow + 'conversation/update',
              dataType : 'json',
              data : $scope.conversation,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              console.debug('  conversation updated = ', data);
            }).error(function(data, status, headers, config) {
              $scope.recordError = 'Error updating feedback conversation.';
              $rootScope.handleHttpError(data, status, headers, config);
            });
          }
        };

        function initializeReturnRecipients() {

          // if no previous feedback conversations, return just
          // first map lead in list
          if ($scope.conversation == null || $scope.conversation == '' || $scope.conversation.feedback.length == 0) {
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
            if (arr[i].name.indexOf('demo') > -1) {
              arr.splice(i, 1);
            }
          }

          sortByKey(arr, 'name');
        }

        // sort and return an array by string key
        function sortByKey(array, key) {
          return array.sort(function(a, b) {
            var x = a[key];
            var y = b[key];
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
          });
        }

        $scope.openIndexViewer = function() {
          var currentUrl = window.location.href;
          var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
          var newUrl = baseUrl + '/index/viewer';
          var myWindow = window.open(newUrl, 'indexViewerWindow');
          myWindow.focus();
        };

        // Order by principle id
        $scope.orderByPrincipleId = function(principle) {
          return parseInt(principle.principleId, 10) + 1;
        };

        // //////////////////////////
        // UNDO / REDO funtionality for Entries (New)
        //
        // //////////////////////////        

        // history for undo/redo
        var history = [];
        var historyIndex = 0;
        var historyLock = false;

        $scope.undoDisabled = true;
        $scope.redoDisabled = true;

        $scope.setValues = function(historyRecord) {
          historyLock = true;
          console.debug('SET:', JSON.stringify(historyRecord));
          $scope.groupsTree = historyRecord['groupsTree'];
          $scope.record.mapEntry = historyRecord['record']['mapEntry'];
          $scope.record.mapNote = historyRecord['record']['mapNote'];
          $scope.record.mapPrinciple = historyRecord['record']['mapPrinciple'];
          $scope.record.flagForConsensusReview = historyRecord['record']['flagForConsensusReview'];
          $scope.record.flagForEditorialReview = historyRecord['record']['flagForEditorialReview'];
          $scope.record.flagForMapLeadReview = historyRecord['record']['flagForMapLeadReview'];
          historyLock = false;
          for (var i = 0; i < $scope.groupsTree.length; i++) {
            var entries = $scope.groupsTree[i].entry;
            for (var j = 0; j < entries.length; j++) {
              if (entries[j].isSelected) {
                $scope.selectMapEntry(entries[j]);
              }
            }
          }
          //selectMapEntry($scope.record.mapEntry);
        };

        $scope.saveHistory = function(data) {
          //if data is not the same as last history then add
          if (JSON.stringify(data) !== JSON.stringify(history[historyIndex - 1])) {
            //eliminate the future
            history.length = (historyIndex);
            history.push(data);
            historyIndex++;
          }
          return true;
        }

        //user action to undo
        $scope.undo = function() {
          historyIndex--;
          console.debug('click undo to:', historyIndex);
          var h = JSON.parse(JSON.stringify(history[historyIndex - 1]));
          console.debug("undo to: ", h);
          $scope.setValues(h);
          $scope.setButtons();
          broadcastRecord();
        };

        //user action to redo
        $scope.redo = function() {
          historyIndex++;
          console.debug('click redo to:', historyIndex);
          var h = JSON.parse(JSON.stringify(history[historyIndex - 1]));
          console.debug("redo to: ", h);
          $scope.setValues(h);
          $scope.setButtons();
          broadcastRecord();
        };

        $scope.createHistoryRecord = function() {
          var historyRecord = {};
          historyRecord.groupsTree = (typeof eval($scope.groupsTree) === 'object') ? angular
            .copy($scope.groupsTree) : {};
          historyRecord.record = {};
          historyRecord.record.mapEntry = (typeof $scope.record.mapEntry == 'object') ? angular
            .copy($scope.record.mapEntry) : {};
          historyRecord.record.mapNote = (typeof $scope.record.mapNote == 'object') ? angular
            .copy($scope.record.mapNote) : {};
          historyRecord.record.mapPrinciple = (typeof eval($scope.record.mapPrinciple) === 'object') ? angular
            .copy($scope.record.mapPrinciple)
            : {};
          historyRecord.record.flagForConsensusReview = (typeof eval($scope.record.flagForConsensusReview) !== 'undefined') ? angular
            .copy($scope.record.flagForConsensusReview)
            : false;
          historyRecord.record.flagForEditorialReview = (typeof eval($scope.record.flagForEditorialReview) !== 'undefined') ? angular
            .copy($scope.record.flagForEditorialReview)
            : false;
          historyRecord.record.flagForMapLeadReview = (typeof eval($scope.record.flagForMapLeadReview) !== 'undefined') ? angular
            .copy($scope.record.flagForMapLeadReview)
            : false;

          console.debug("createHistoryRecord", historyRecord);
          return historyRecord;
        }

        //one $watch for each variable, $watchGroup was not working for all
        //groupsTree
        $scope.$watch('groupsTree', function(newVal, oldVal) {
          if (historyLock == false && typeof (oldVal) !== 'undefined'
            && JSON.stringify(newVal) !== JSON.stringify(oldVal)) {
            $scope.saveHistory($scope.createHistoryRecord());
            $scope.setButtons();
          }
        }, true);

        //notes
        $scope.$watch('record.mapNote', function(newVal, oldVal) {
          if (historyLock == false && typeof (oldVal) !== 'undefined'
            && JSON.stringify(newVal) !== JSON.stringify(oldVal)) {
            $scope.saveHistory($scope.createHistoryRecord());
            $scope.setButtons();
          }
        }, true);

        //principle
        $scope.$watch('record.mapPrinciple', function(newVal, oldVal) {
          if (historyLock == false && typeof (oldVal) !== 'undefined'
            && JSON.stringify(newVal) !== JSON.stringify(oldVal)) {
            $scope.saveHistory($scope.createHistoryRecord());
            $scope.setButtons();
          }
        }, true);

        //flagForConsensusReview
        $scope.$watch('record.flagForConsensusReview', function(newVal, oldVal) {
          if (historyLock == false && typeof (oldVal) !== 'undefined'
            && JSON.stringify(newVal) !== JSON.stringify(oldVal)) {
            $scope.saveHistory($scope.createHistoryRecord());
            $scope.setButtons();
          }
        }, true);

        //flagForEditorialReview
        $scope.$watch('record.flagForEditorialReview', function(newVal, oldVal) {
          if (historyLock == false && typeof (oldVal) !== 'undefined'
            && JSON.stringify(newVal) !== JSON.stringify(oldVal)) {
            $scope.saveHistory($scope.createHistoryRecord());
            $scope.setButtons();
          }
        }, true);

        //flagForMapLeadReview
        $scope.$watch('record.flagForMapLeadReview', function(newVal, oldVal) {
          if (historyLock == false && typeof (oldVal) !== 'undefined'
            && JSON.stringify(newVal) !== JSON.stringify(oldVal)) {
            $scope.saveHistory($scope.createHistoryRecord());
            $scope.setButtons();
          }
        }, true);

        //track enable/disable of buttons        
        $scope.setButtons = function() {
          $scope.undoDisabled = (historyIndex > 1) ? false : true;
          $scope.redoDisabled = (historyIndex <= (history.length - 1)) ? false : true;
          console.debug("historyIndex:", historyIndex, "|size:", history.length, "|undoEnabled:",
            $scope.undoDisabled, "|redoEnabled:", $scope.redoDisabled);
        };

        // Open modal to display authoring history for concept
        $scope.openAuthoringHistory = function(concept) {

          if (concept == null) {
            return;
          }

          var modalInstance = $uibModal.open({
            templateUrl : 'js/widgets/mapRecord/authoringHistory.html',
            controller : AuthoringHistoryModalCtrl,
            size : 'lg',
            resolve : {
              concept : function() {
                return concept;
              },
              project : function() {
                return $scope.project;
              }
            }
          });

          modalInstance.result.then(

          // called on Done clicked by user
          function() {
          })

        };

        var AuthoringHistoryModalCtrl = function($scope, $uibModalInstance, $q, concept, project) {

          $scope.concept = concept;
          $scope.projectId = project.id
          $scope.edits = [];
          $scope.filter = '';

          // get history of authoring changes for this concept
          $scope.retrieveAuthoringChanges = function(concept) {
            console.debug('AuthoringHistoryModalCtrl: retrieve Authoring Changes');

            gpService.increment();
            $http({
              url : root_mapping + 'changes/' + $scope.projectId + '/' + concept.terminologyId,
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(
              function(data) {
                console.debug('Success in getting authoring changes.', $scope.filter);
                $scope.edits = [];
                for (var i = 0; i < data.searchResult.length; i++) {
                  var searchResult = data.searchResult[i];

                  // if filter is set, but searchResult doesn't match it, continue without this result
                  if ($scope.filter && searchResult.value.indexOf($scope.filter) == -1
                    && searchResult.value2.indexOf($scope.filter) == -1) {
                    continue;
                  }

                  // split the searchResult and put into edits object array
                  var value = searchResult.value.split(":");
                  var date = value[1].split("T");
                  var value2 = searchResult.value2.split(":");
                  var edit = {
                    author : value[0],
                    date : date[0],
                    conceptId : value2[0],
                    componentId : value2[1],
                    type : value2[2],
                    subcomponentType : value2[3],
                    action : value2[4]
                  }
                  $scope.edits.push(edit);
                }

                gpService.decrement();

              }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });
          };

          $scope.close = function() {
            $uibModalInstance.close();
          };

          $scope.retrieveAuthoringChanges($scope.concept);
        }

        // feedback groups functionality
        var feedbackGroupConfig = appConfig["deploy.feedback.group.names"]; 
        
        $scope.feedbackGroups = (feedbackGroupConfig = null || typeof feedbackGroupConfig == 'undefined' || feedbackGroupConfig === '')
        ? null : JSON.parse(feedbackGroupConfig);

        
        $scope.setGroupRecipients = function(groupId) {
          var recipients = (appConfig["deploy.feedback.group.users." + groupId]).split(',');
          $scope.returnRecipients = [];
          var allUsers = $scope.project.mapLead.concat($scope.project.mapSpecialist);
          recipients.forEach(function(r){
            var fbr = findUser(r, allUsers);
            if ((fbr) && fbr.id != null) {
              $scope.returnRecipients.push({ id: fbr.id});
            }
          });
          
        }
        
        function findUser(userName, array) {
          for (var i=0; i < array.length; i++)
            if (array[i].userName === userName)
                return array[i];
        }
        
        
        //end
      } ]);
