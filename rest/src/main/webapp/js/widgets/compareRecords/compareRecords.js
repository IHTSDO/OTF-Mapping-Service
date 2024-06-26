'use strict';

angular
  .module('mapProjectApp.widgets.compareRecords', [ 'adf.provider' ])
  .config(
    function(dashboardProvider) {
      dashboardProvider
        .widget(
          'compareRecords',
          {
            title : 'Compare Records',
            description : 'Displays map records for a source concept and highlights differences between the records.',
            controller : 'compareRecordsCtrl',
            templateUrl : 'js/widgets/compareRecords/compareRecords.html',
            edit : {}
          });
    })
  .controller(
    'compareRecordsCtrl',
    function($scope, $rootScope, $http, $routeParams, $location, $timeout,
      localStorageService, $sce, $window, gpService, appConfig, utilService) {

      // ///////////////////////////////////
      // Map Record Controller Functions //
      // ///////////////////////////////////

      // initialize scope variables
      $scope.concept = null;
      $scope.project = localStorageService.get('focusProject');
      $scope.user = localStorageService.get('currentUser');
      $scope.role = localStorageService.get('currentRole');

      // flag for whether this record is a false conflict
      $scope.isFalseConflict = null; // set to true or false on first visit

      $scope.record1 = null;
      $scope.groups1 = null;
      $scope.entries1 = null;
      $scope.conversation1 = null;
      $scope.newFeedbackMessages1 = new Array();

      $scope.record2 = null;
      $scope.groups2 = null;
      $scope.entries2 = null;
      $scope.conversation2 = null;
      $scope.newFeedbackMessages2 = new Array();
      $scope.notes = utilService.getNotes($scope.project.id);

      $scope.leadRecord = null;
      $scope.leadConversation = null;
      $scope.historicalConversation = null;
      $scope.newLeadFeedbackMessages = new Array();

      // initialize accordion variables
      $scope.isConceptOpen = true;
      $scope.isEntriesOpen = true;
      $scope.isPrinciplesOpen = true;
      $scope.isNotesOpen = true;
      $scope.isReportOpen = true;
      $scope.isGroupFeedbackOpen = false;
      $scope.isFeedbackHistoryOpen = false;
      
      $scope.isReview = $window.location.hash.includes('review');
      $scope.isConflict = $window.location.hash.includes('conflicts');
      $scope.showFeedbackHistory = $scope.isReview;
      $scope.returnRecipients = new Array();
      $scope.allUsers = new Array();
      $scope.multiSelectSettings = {
        displayProp : 'name',
        scrollableHeight : '150px',
        scrollable : true,
        showCheckAll : false,
        showUncheckAll : false
      };
      $scope.multiSelectCustomTexts = {
        buttonDefaultText : 'Select Users'
      };
      
      // start note edit mode in off mode
      $scope.feedbackEditMode = false;
      $scope.feedbackEditId = null;
      $scope.content = {
          text : ''
      };
      
      $scope.errorMessages = $scope.project.errorMessages;
      $scope.errorMessages.sort();
      $scope.errorMessages.unshift('None');
      $scope.selectedErrorMessage1 = $scope.errorMessages[0];
      $scope.selectedErrorMessage2 = $scope.errorMessages[0];

      // watch for project change and modify the local variable if necessary
      // coupled with $watch below, this avoids premature work fetching
      $scope.$on('localStorageModule.notification.setFocusProject', function(
        event, parameters) {
        $scope.project = parameters.focusProject;
        $scope.errorMessages = $scope.project.errorMessages;
        $scope.errorMessages.unshift('None');
      });

      $scope.$watch(['focusProject'], function(){
		if ($scope.focusProject != null) {
		  // Initialize terminology notes
          utilService.initializeTerminologyNotes($scope.focusProject.id).then(() => {
            $scope.notes = utilService.getNotes($scope.focusProject.id);            
          }).catch(error => {
            console.error('Error initializing terminology notes', error);
          });
		}
	  });

      // watch for change in focus project
      $scope.userToken = localStorageService.get('userToken');
      $scope.$watch([ 'project', 'userToken' ], function() {

        if ($scope.project != null && $scope.userToken != null) {
          $http.defaults.headers.common.Authorization = $scope.userToken;

          // if first visit, retrieve the records to be compared
          if ($scope.leadRecord == null) {

          // Set up "all users"
            $scope.allUsers = $scope.project.mapSpecialist
              .concat($scope.project.mapLead);
            organizeUsers($scope.allUsers);
            
            $scope.getRecordsInConflict();

            gpService.increment();
            $http(
              {
                url : root_workflow + 'record/id/' + $routeParams.recordId
                  + '/isFalseConflict',
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
              gpService.decrement();
              $scope.isFalseConflict = data === 'true' ? true : false;
            }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });

            // otherwise, return to dashboard (mismatch between record and
            // project)
          } else {
            var path = '';

            if ($scope.role === 'Specialist') {
              path = '/specialist/dash';
            } else if ($scope.role === 'Lead') {
              path = '/lead/dash';
            } else if ($scope.role === 'Administrator') {
              path = '/admin/dash';
            } else if ($scope.role === 'Viewer') {
              path = '/viewer/dash';
            }
            $location.path(path);
          }
        }
      });
      
      $scope.isNewFeedback = function(feedback, conversationLocation) {
        if(conversationLocation == '1'){
          return $scope.newFeedbackMessages1.includes(feedback.message);
        }
        if(conversationLocation == '2'){
          return $scope.newFeedbackMessages2.includes(feedback.message);
        }
        if(conversationLocation == 'lead'){
          return $scope.newLeadFeedbackMessages.includes(feedback.message);
        }
        
      }
            
      $scope.editFeedback = function(feedback) {
        $scope.content.text = feedback.message;
        $scope.content.text1 = feedback.message1;
        $scope.content.text2 = feedback.message2;
        $scope.selectedErrorMessage1 = feedback.mapError;
        $scope.feedbackEditMode = true;
        $scope.feedbackEditId = feedback.id ? feedback.id : feedback.localId;
      };

      $scope.cancelEditFeedback = function() {
        $scope.content.text = '';
        $scope.content.text1 = '';
        $scope.content.text2 = '';
        $scope.feedbackEditMode = false;
        $scope.feedbackEditId = null;
        $scope.tinymceContent = '';
      };

      $scope.saveEditFeedback = function(recordInError,feedback) {
        
        var currentConversation = $scope.getCurrentConversation(recordInError);
        if ($scope.feedbackEditMode == true) {
          var feedbackFound = false;
          // find the existing feedback
          for (var i = 0; i < currentConversation.feedback.length; i++) {
            // if this feedback, overwrite it
            if ($scope.feedbackEditId == currentConversation.feedback[i].localId ||
                $scope.feedbackEditId == currentConversation.feedback[i].id) {
              feedbackFound = true;
              currentConversation.feedback[i].message = feedback;
              //$scope.conversation.feedback[i].id = currentLocalId++;
            }
          }
          $scope.feedbackEditMode = false;
          $scope.tinymceContent = null;
          
          console.debug('update conversation', currentConversation);
          $http({
            url : root_workflow + 'conversation/update',
            dataType : 'json',
            data : currentConversation,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            console.debug('  conversation updated = ', data);
            if (recordInError.id == $scope.record1.id) {
              $http({
                url : root_workflow + 'conversation/id/' + recordInError.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
              $scope.newFeedbackMessages1.push(feedback);
                $scope.conversation1 = data;

              });
            } 
            if ($scope.record2 != null && recordInError.id == $scope.record2.id) {
              
              $http({
                url : root_workflow + 'conversation/id/' + recordInError.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                $scope.newFeedbackMessages2.push(feedback);
                $scope.conversation2 = data;

              });
              
            }
            else{
              console.debug('  conversation updated = ', data);
              $http(
                {
                  url : root_workflow + 'conversation/id/'
                    + $scope.leadRecord.id,
                  dataType : 'json',
                  method : 'GET',
                  headers : {
                    'Content-Type' : 'application/json'
                  }
                }).success(function(data) {
                $scope.newLeadFeedbackMessages.push(feedback);
                $scope.leadConversation = data;
              });
              
            }

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.recordError = 'Error updating feedback conversation.';
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.getRecordsInConflict = function() {
        // initialize local variables
        var leadRecordId = $routeParams.recordId;
        
        // get the lead record (do everything else inside the "then")
        gpService.increment();
        $http({
          url : root_mapping + 'record/id/' + leadRecordId,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          gpService.decrement();
          $scope.leadRecord = data;
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
          // obtain the record concept - id from leadRecord
        }).then(
          function(data) {
            gpService.increment();
            $http(
              {
                url : root_content + 'concept/id/'
                  + $scope.project.sourceTerminology + '/'
                  + $scope.project.sourceTerminologyVersion + '/'
                  + $scope.leadRecord.conceptId,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(
              function(data) {
                gpService.decrement();
                $scope.concept = data;
               
                setAccordianTitle($scope.concept.terminologyId,
                  $scope.concept.defaultPreferredName);
              }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });

        // get the conflict records
        gpService.increment();
        $http(
          {
            url : root_mapping + 'record/id/' + $routeParams.recordId
              + '/conflictOrigins',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
          function(data) {
            gpService.decrement();

			// Handle special-case for CONFLICT_AND_REVIEW workflow (QA is handled separately)
            if ($scope.project.workflowType === 'CONFLICT_AND_REVIEW_PATH' &&
				data.mapRecord[0].workflowStatus != 'QA_NEEDED') {

				// If there are 3 previous records, this is a 2nd lead review.  
				// Display the most recent record (1st lead's record)  
				if(data.totalCount == 3){
					// sort by id
					data.mapRecord.sort(function (a, b) {
					  return a.id - b.id;
					});
					
                	$scope.record1 = data.mapRecord[2];
                	$scope.record1.displayName = $scope.record1.owner.name;
              		$scope.record2 = null;							
				}
				else{
					// if a conflict, set the two records
	              if (data.mapRecord[0].workflowStatus === 'CONFLICT_DETECTED'
	                || data.mapRecord[1].workflowStatus === 'CONFLICT_DETECTED') {
		                // set the origin records (i.e. the records in conflict)
		                $scope.record1 = data.mapRecord[0];
		                $scope.record1.displayName = $scope.record1.owner.name;
		                $scope.record2 = data.mapRecord[1];
		                $scope.record2.displayName = $scope.record2.owner.name;
					}
					
					// if a reviewor QA, display one record
					else if (data.mapRecord[0].workflowStatus === 'REVIEW_NEEDED'
	                || data.mapRecord[1].workflowStatus === 'REVIEW_NEEDED') {
						$scope.record1 = data.mapRecord[0];
	                	$scope.record1.displayName = $scope.record1.owner.name;
	              		$scope.record2 = null;
					}
				}

            } else if (data.totalCount == 1) {
	
              $scope.record1 = data.mapRecord[0];
              $scope.record1.displayName = data.mapRecord[0].owner.name;
              $scope.record2 = null;

            } else if (data.totalCount == 2) {

              // if a conflict, just set the two records
              // NOTE: Special case for Simple Path workflow where one record
              // may be EDITING_DONE
              if (data.mapRecord[0].workflowStatus === 'CONFLICT_DETECTED'
                || data.mapRecord[1].workflowStatus === 'CONFLICT_DETECTED') {
                // set the origin records (i.e. the records in conflict)
                $scope.record1 = data.mapRecord[0];
                $scope.record1.displayName = $scope.record1.owner.name;
                $scope.record2 = data.mapRecord[1];
                $scope.record2.displayName = $scope.record2.owner.name;

                // otherwise a review or qa record
              } else {

                // assign the first record as the specialist's revised record
                // assign the second record as the previously published record
                for (var i = 0; i < 2; i++) {
                  if (data.mapRecord[i].workflowStatus === 'REVIEW_NEEDED'
                    || data.mapRecord[i].workflowStatus === 'QA_NEEDED') {
                    $scope.record1 = data.mapRecord[i];
                    $scope.record1.displayName = $scope.record1.owner.name;
                  } else if (data.mapRecord[i].workflowStatus === 'REVISION') {
                    $scope.record2 = data.mapRecord[i];
                    $scope.record2.displayName = 'Previously Published';
                  }
                }
              }
            }

			// auto-populate if there is only one, no split-screen
			// and if this is brand-new record (once the Lead has saved changes, don't populate based on previous records anymore)
			if ($scope.record1 != null && $scope.record2 == null && $scope.leadRecord.workflowStatus.includes('_NEW')){
						              
	          $timeout(function() {
	            $scope.populateMapRecord($scope.record1);
	          }, 400);
			}	


            if ($scope.record1 != null) {
              gpService.increment();
              $http({
                url : root_workflow + 'conversation/id/' + $scope.record1.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                gpService.decrement();
                $scope.conversation1 = data;
              }).error(function(data, status, headers, config) {
                gpService.decrement();
                $rootScope.handleHttpError(data, status, headers, config);
              });
            }

            if ($scope.record2 != null) {
              gpService.increment();
              $http({
                url : root_workflow + 'conversation/id/' + $scope.record2.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                gpService.decrement();
                $scope.conversation2 = data;
              }).error(function(data, status, headers, config) {
                gpService.decrement();
                $rootScope.handleHttpError(data, status, headers, config);
              });
            }

            if ($scope.leadRecord != null) {
              gpService.increment();
              $http(
                {
                  url : root_workflow + 'conversation/id/'
                    + $scope.leadRecord.id,
                  dataType : 'json',
                  method : 'GET',
                  headers : {
                    'Content-Type' : 'application/json'
                  }
                }).success(
                function(data) {
                  gpService.decrement();
                  $scope.leadConversation = data;

                  // if no prior conversation, initialize with the specialists
                  if (!$scope.leadConversation && !$scope.isReview) {

                    if ($scope.record1 != null)
                      $scope.returnRecipients.push($scope.record1.owner);

                    if ($scope.record2 != null)
                      $scope.returnRecipients.push($scope.record2.owner);

                    // otherwise initialize with recipients on prior feedback
                  } else if($scope.isReview){
                    $scope.getUsersForConceptHistorical();
                  } else {
                    initializeReturnRecipients($scope.leadConversation);
                  }
                }).error(function(data, status, headers, config) {
                gpService.decrement();
                $rootScope.handleHttpError(data, status, headers, config);
              });
            }

          }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        }).then(
          function(data) {

            // get the groups
            if ($scope.project.groupStructure == true)
              getGroups();
            
            // initialize the entries
            initializeEntries();

            // obtain the validationResults from compareRecords
            if ($scope.record2 != null) {
              gpService.increment();
              $http(
                {
                  url : root_mapping + 'validation/record/id/'
                    + $scope.record1.id + '/record/id/' + $scope.record2.id
                    + '/compare',
                  dataType : 'json',
                  method : 'GET',
                  headers : {
                    'Content-Type' : 'application/json'
                  }
                }).success(
                function(data) {
                  gpService.decrement();
                  for (var i = 0; i < data.errors.length; i++) {
                    if ($scope.record1 != null)
                      data.errors[i] = data.errors[i].replace('Specialist 1',
                        $scope.record1.owner.name);

                    if ($scope.record2 != null)
                      data.errors[i] = data.errors[i].replace('Specialist 2',
                        $scope.record2.owner.name);
                  }
                  $scope.validationResult = data;
                }).error(function(data, status, headers, config) {
                gpService.decrement();
                $rootScope.handleHttpError(data, status, headers, config);
              });
            }
          });

        });
      };
      
      function getTerminologyNote(id) {
		var note = $scope.notes[id];
		return (note == null || note == undefined) ? "" : note;
	  }

      // /////////////////////////////
      // Initialization Functions ///
      // /////////////////////////////

      // construct an object containing entries, either:
      // 1) a 1-d array, if project has no group structure
      // 2) a 2-d array, with structure [group][mapPriority]
      function initializeEntries() {
        // INITIALIZE FIRST RECORD

        // calculate rule summaries and assign local id equivalent to hibernate
        // id (needed for track by in ng-repeat)
        for (var i = 0; i < $scope.record1.mapEntry.length; i++) {
          $scope.record1.mapEntry[i].ruleSummary = $scope
            .getRuleSummary($scope.record1.mapEntry[i]);
          $scope.record1.mapEntry[i].localId = $scope.record1.mapEntry[i].id;

        }

        // if no group structure, simply copy and sort
        if ($scope.project.groupStructure == false) {
          $scope.entries1 = sortByKey($scope.record1.mapEntry, 'mapPriority');

          // otherwise, initialize group arrays
        } else {

          // initialize entry arrays for distribution by group
          $scope.entries1 = new Array(10);

          for (var i = 0; i < $scope.entries1.length; i++)
            $scope.entries1[i] = new Array();

          // cycle over the entries and assign to group bins
          for (var i = 0; i < $scope.record1.mapEntry.length; i++) {
            $scope.entries1[$scope.record1.mapEntry[i].mapGroup]
              .push($scope.record1.mapEntry[i]);
          }

          // cycle over group bins and sort contents by map priority
          for (var i = 0; i < $scope.entries1.length; i++) {
            $scope.entries1[i] = sortByKey($scope.entries1[i], 'mapPriority');
          }
        }
        // INITIALIZE SECOND RECORD

        if ($scope.record2 != null) {

          // calculate rule summaries and assign local id equivalent to
          // hibernate id (needed for track by in ng-repeat)
          for (var i = 0; i < $scope.record2.mapEntry.length; i++) {
            $scope.record2.mapEntry[i].ruleSummary = $scope
              .getRuleSummary($scope.record2.mapEntry[i]);
            $scope.record2.mapEntry[i].localId = $scope.record2.mapEntry[i].id;
          }

          // if no group structure, simply copy and sort
          if ($scope.project.groupStructure == false) {
            $scope.entries2 = sortByKey($scope.record2.mapEntry, 'mapPriority');

            // otherwise, initialize group arrays
          } else {

            // initiailize entry arrays for distribution by group
            $scope.entries2 = new Array(10);

            for (var i = 0; i < $scope.entries2.length; i++)
              $scope.entries2[i] = new Array();

            // cycle over the entries and assign to group bins
            for (var i = 0; i < $scope.record2.mapEntry.length; i++) {
              $scope.entries2[$scope.record2.mapEntry[i].mapGroup]
                .push($scope.record2.mapEntry[i]);
            }

            // cycle over group bins and sort contents by map priority
            for (var i = 0; i < $scope.entries2.length; i++) {
              $scope.entries2[i] = sortByKey($scope.entries2[i], 'mapPriority');
            }
          }
        }

      }

      /**
       * MAP RECORD FUNCTIONS
       */

      /**
       * MAP ENTRY FUNCTIONS
       */

      // Returns a summary string for the entry rule type
      $scope.getRuleSummary = function(entry) {

        var ruleSummary = '';

        // first, rule summary
        if ($scope.project.ruleBased == true && entry.rule != null
          && entry.rule != undefined) {
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
              if (upperBound != null && upperBound != ''
                && upperBound.length > 0)
                ruleSummary += ' AND ';
            }
            if (upperBound != null && upperBound != '' && upperBound.length > 0)
              ruleSummary += upperBound[0];

            ruleSummary += '] ';
          }
        }

        return ruleSummary;

      };

      // ///////////////////////
      // Map Group Functions //
      // ///////////////////////

      // Retrieves groups from the existing entries
      function getGroups() {

        $scope.groups1 = new Array();
        for (var i = 0; i < $scope.record1.mapEntry.length; i++) {

          if ($scope.groups1.indexOf(parseInt(
            $scope.record1.mapEntry[i].mapGroup, 10)) == -1) {
            $scope.groups1.push(parseInt($scope.record1.mapEntry[i].mapGroup,
              10));
          }

        }

        // if no groups found, add a default group
        if ($scope.groups1.length == 0)
          $scope.groups1.push(1);

        $scope.groups2 = new Array();

        if ($scope.record2 != null) {
          for (var i = 0; i < $scope.record2.mapEntry.length; i++) {

            if ($scope.groups2.indexOf(parseInt(
              $scope.record2.mapEntry[i].mapGroup, 10)) == -1) {
              $scope.groups2.push(parseInt($scope.record2.mapEntry[i].mapGroup,
                10));
            }

          }

          // if no groups found, add a default group
          if ($scope.groups2.length == 0)
            $scope.groups2.push(1);
        }

      }

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

      function setAccordianTitle(id, term) {
        if ($scope.record2 == null && $scope.record1 != null
          && $scope.record1.owner.userName == 'qa') {
          $scope.model.title = 'QA Record: ' + id + getTerminologyNote(id) +' ' + term;
        } else if ($scope.record2 == null) {
          $scope.model.title = 'Review Record: ' + id + getTerminologyNote(id) +' ' + term;
        } else {
          $scope.model.title = 'Compare Records: ' + id + getTerminologyNote(id) +' ' + term;
        }
      }

      // Populates lead record and fires a "selected" event
      $scope.populateMapRecord = function(record) {

        var localId = 1;

        // copy the relevant information into the map lead's record
        $scope.leadRecord.mapEntry = angular.copy(record.mapEntry);
        if (!$scope.leadRecord.mapNote.length) {
          $scope.leadRecord.mapNote = angular.copy(record.mapNote);
        }
        $scope.leadRecord.mapPrinciple = angular.copy(record.mapPrinciple);

        // null the ids of the notes (for later creation as new jpa objects)
        for (var i = 0; i < $scope.leadRecord.mapNote.length; i++) {
          $scope.leadRecord.mapNote[i].localId = localId++;
          $scope.leadRecord.mapNote[i].id = null;
        }

        // null the ids of all the entries (for later creation as new jpa
        // objects)
        for (var i = 0; i < $scope.leadRecord.mapEntry.length; i++) {
          $scope.leadRecord.mapEntry[i].localId = localId++;
          $scope.leadRecord.mapEntry[i].id = null;
        }
        
        // broadcast to the map record widget
        console.debug(
          'broadcast compareRecordsWidget.notification.selectRecord = ',
          $scope.leadRecord);
        $rootScope.$broadcast('compareRecordsWidget.notification.selectRecord',
          {
            record : $scope.leadRecord
          });

      };

      $scope.getEntrySummary = function(entry) {

        var entrySummary = '';
        // first get the rule
        entrySummary += $scope.getRuleSummary(entry);

        // if target is null, check relation id
        if (entry.targetId == null || entry.targetId === '') {

          // if relation id is null or empty, return empty entry string
          if (entry.mapRelation == null || entry.mapRelation === '') {
            entrySummary += '[NO TARGET OR RELATION]';

            // otherwise, return the relation name
          } else {
            entrySummary += entry.mapRelation.name;

          }
          // otherwise return the target code and preferred name
        } else {
          entrySummary += entry.targetId + ' ' + entry.targetName;
        }

        return entrySummary;

      };

      $scope.submitNewFeedback = function(recordInError, errorMessage,
        feedbackMessage) {
        var currentConversation = $scope.getCurrentConversation(recordInError);
        var localTimestamp = new Date().getTime();
        
        // determine if feedback is an error or not and remove 'None' text
        var mapError = '';
        var isError = false;
        if (errorMessage != null && errorMessage != ''
          && errorMessage != 'None') {
          isError = true;
          mapError = errorMessage;
        }

        // if the conversation has not yet been started
        if (currentConversation == '') {

          // create first feedback thread to go into the feedback conversation
          var receivingUsers = [ recordInError.owner ];
          var feedback = {
            'message' : feedbackMessage,
            'mapError' : mapError,
            'timestamp' : localTimestamp,
            'sender' : $scope.user,
            'recipients' : receivingUsers,
            'isError' : isError,
            'feedbackConversation' : currentConversation,
            'viewedBy' : [ $scope.user ]
          };
          
          var feedbacks = new Array();
          feedbacks.push(feedback);

          // create feedback conversation
          var feedbackConversation = {
            'lastModified' : new Date(),
            'terminology' : $scope.project.sourceTerminology,
            'terminologyId' : recordInError.conceptId,
            'terminologyVersion' : $scope.project.sourceTerminologyVersion,
            'isResolved' : 'false',
            'discrepancyReview' : 'false',
            'mapRecordId' : recordInError.id,
            'feedback' : feedbacks,
            'defaultPreferredName' : $scope.concept.defaultPreferredName,
            'title' : $scope.getTitle(false, errorMessage),
            'mapProjectId' : $scope.project.id,
            'userName' : recordInError.owner.userName
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
            console.debug('  feedback conversation = ', data);
            if (recordInError.id == $scope.record1.id){
              $scope.newFeedbackMessages1.push(feedbackMessage);
              $scope.conversation1 = data;
            }
            else{
              $scope.newFeedbackMessages2.push(feedbackMessage);
              $scope.conversation2 = data;
        }
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.recordError = 'Error adding new feedback conversation.';
            $rootScope.handleHttpError(data, status, headers, config);
          });

        } else { // already started a conversation

          // create feedback msg to be added to the conversation
          var receivingUsers = [ recordInError.owner ];

          var feedback = {
            'message' : feedbackMessage,
            'mapError' : mapError,
            'timestamp' : localTimestamp,
            'sender' : $scope.user,
            'recipients' : receivingUsers,
            'isError' : isError,
            'viewedBy' : [ $scope.user ]
          };

          var localFeedback = currentConversation.feedback;
          localFeedback.push(feedback);
          currentConversation.feedback = localFeedback;
          currentConversation.title = $scope.getTitle(false, errorMessage);

          $http({
            url : root_workflow + 'conversation/update',
            dataType : 'json',
            data : currentConversation,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            console.debug('  conversation updated = ', data);
            if (recordInError.id == $scope.record1.id) {
              $http({
                url : root_workflow + 'conversation/id/' + recordInError.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {

                $scope.newFeedbackMessages1.push(feedbackMessage);
                $scope.conversation1 = data;

              });
            } else {
              $http({
                url : root_workflow + 'conversation/id/' + recordInError.id,
                dataType : 'json',
                method : 'GET',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {

                $scope.newFeedbackMessages2.push(feedbackMessage);
                $scope.conversation2 = data;

              });
            }

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.recordError = 'Error updating feedback conversation.';
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.submitGroupFeedback = function(groupFeedbackMessage, recipientList) {

        if (groupFeedbackMessage == null || groupFeedbackMessage == undefined
          || groupFeedbackMessage === '') {
          window.alert('The group feedback message field cannot be blank');
          return;
        }
        var currentConversation = $scope
          .getCurrentConversation($scope.leadRecord);

        var localFeedback = currentConversation.feedback;
        var localTimestamp = new Date().getTime();

        // copy recipient list
        var localRecipients = recipientList.slice(0);
        var newRecipients = new Array();
        for (var i = 0; i < localRecipients.length; i++) {
          for (var j = 0; j < $scope.allUsers.length; j++) {
            if (localRecipients[i].id == $scope.allUsers[j].id)
              newRecipients.push($scope.allUsers[j]);
          }
        }

        // if the conversation has not yet been started
        if (currentConversation == null || currentConversation == '') {

          // create first feedback item to go into the feedback conversation
          var feedback = {
            'message' : groupFeedbackMessage,
            'mapError' : '',
            'timestamp' : localTimestamp,
            'sender' : $scope.user,
            'recipients' : newRecipients,
            'isError' : 'false',
            'feedbackConversation' : currentConversation,
            'viewedBy' : [$scope.user]
          };

          var feedbacks = new Array();
          feedbacks.push(feedback);

          // create feedback conversation
          var feedbackConversation = {
            'lastModified' : new Date(),
            'terminology' : $scope.project.sourceTerminology,
            'terminologyId' : $scope.leadRecord.conceptId,
            'terminologyVersion' : $scope.project.sourceTerminologyVersion,
            'isResolved' : 'false',
            'discrepancyReview' : $scope.indicateDiscrepancyReview,
            'mapRecordId' : $scope.leadRecord.id,
            'feedback' : feedbacks,
            'defaultPreferredName' : $scope.concept.defaultPreferredName,
            'title' : $scope.getTitle(true, ''),
            'mapProjectId' : $scope.project.id,
            'userName' : $scope.leadRecord.owner.userName
          };

          $http({
            url : root_workflow + 'conversation/add',
            dataType : 'json',
            data : feedbackConversation,
            method : 'PUT',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
            .success(function(data) {
              $scope.newLeadFeedbackMessages.push(groupFeedbackMessage);
              $scope.leadConversation = data;
            })
            .error(
              function(data, status, headers, config) {

                $scope.recordError = 'Error adding new feedback conversation for group feedback.';
                $rootScope.handleHttpError(data, status, headers, config);
              });

        } else { // already started a conversation

          // create feedback msg to be added to the conversation
          var feedback = {
            'message' : groupFeedbackMessage,
            'mapError' : '',
            'timestamp' : localTimestamp,
            'sender' : $scope.user,
            'recipients' : newRecipients,
            'isError' : 'false',
            'viewedBy' : []
          };

          var localFeedback = currentConversation.feedback;
          localFeedback.push(feedback);
          currentConversation.feedback = localFeedback;
          currentConversation.discrepancyReview = $scope.indicateDiscrepancyReview;
          currentConversation.title = $scope.getTitle(true, '');

          console.debug('update conversation', $scope.conversation);
          $http({
            url : root_workflow + 'conversation/update',
            dataType : 'json',
            data : currentConversation,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
            .success(
              function(data) {
                console.debug('  conversation updated = ', data);
                $http(
                  {
                    url : root_workflow + 'conversation/id/'
                      + $scope.leadRecord.id,
                    dataType : 'json',
                    method : 'GET',
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                  $scope.newLeadFeedbackMessages.push(groupFeedbackMessage);
                  $scope.leadConversation = data;
                });
              })
            .error(
              function(data, status, headers, config) {
                gpService.decrement();
                $scope.recordError = 'Error updating feedback conversation for group feedback.';
                $rootScope.handleHttpError(data, status, headers, config);
              });
        }

      };
      
   // Delete feedback conversation
      $scope.removeFeedback = function(message,recordType) {
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
          if(recordType == 'record1'){
          $http({
            url : root_workflow + 'conversation/id/'+ $scope.record1.id,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.conversation1 = data;
          });
          }
          if(recordType === 'record2'){
          $http({
            url : root_workflow + 'conversation/id/'+ $scope.record2.id,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.conversation2 = data;
          });
          }
          if(recordType === 'leadRecord'){
            $http({
              url : root_workflow + 'conversation/id/'+ $scope.leadRecord.id,
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.leadConversation = data;
            });
            }
      }).error(function(data, status, headers, config) {
        $scope.recordError = 'Error deleting feedback conversation from application.';
        $rootScope.handleHttpError(data, status, headers, config);
      });
    }
           
      // function to retrieve the feedback conversation based on record id
      $scope.getFeedbackConversation = function(record) {
        $scope.feedbackRecord = record;
        gpService.increment();
        $http({
          url : root_workflow + 'conversation/project/id/' + record.mapProjectId + '/concept/id/' + record.conceptId,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(
          function(data) {
           
            $scope.historicalConversation = data;
            //$scope.markFeedbackViewed($scope.conversation, $scope.currentUser);

            gpService.decrement();
          }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };
      
      $scope.$on('compareRecordsWidget.notification.selectRecord', function(event, parameters) {
        $scope.getFeedbackConversation(parameters.record);
      });      

      function recordToText(record) {

        var recordText = '';
        var entries = getEntriesByGroup(record); // ensure the entries are
        // sorted correctly

        recordText += 'Record ' + record.id + '<br>';
        recordText += 'Owner: ' + record.owner.name + '<br><br>';

        // for each group
        // NOTE: group 0 is by definition empty
        for (var i = 1; i < entries.length; i++) {

          var groupEntries = entries[i];

          recordText += 'Group ' + i + '<br>';

          // for each entry in group

          for (var j = 0; j < groupEntries.length; j++) {

            var entry = groupEntries[j];
            recordText += ' (' + j + ') ' + entry.targetId + ' - '
              + entry.targetName + '<br>';
            if ($scope.project.ruleBased == true)
              recordText += '      RULE:     ' + entry.rule + '<br>';
            if (entry.mapRelation != null)
              recordText += '      RELATION: ' + entry.mapRelation.name
                + '<br>';
            if (entry.mapAdvice.length != 0) {
              recordText += '      ADVICES:  ';

              for (var k = 0; k < entry.mapAdvice.length; k++) {
                if (k > 0)
                  recordText += '                ';
                recordText += entry.mapAdvice[k].name + '<br>';
              }

            }
          }

        }

        recordText += '<br>';

        // add the principles
        if (record.mapPrinciple.length > 0) {

          recordText += 'Map Principles Used:' + '<br>';

          for (var i = 0; i < record.mapPrinciple.length; i++) {
            recordText += '  ' + record.mapPrinciple[i].principleId + ': '
              + record.mapPrinciple[i].name + '<br>';
          }

          recordText += '<br>';
        }

        // add the notes
        if (record.mapNote.length > 0) {

          recordText += 'Notes:' + '<br>';

          for (var i = 0; i < record.mapNote.length; i++) {
            recordText += '   [' + record.mapNote[i].user.userName + '] '
              + record.mapNote[i].note + '<br>';
          }

          recordText += '<br>';
        }

        // check if flagged for map lead
        if (record.flagForMapLeadReview == true) {
          recordText += '<strong>Flagged for Map Lead Review</strong>' + '<br>';
        }

        return recordText;
      }

      function getEntriesByGroup(record) {

        var entries = new Array();
        entries.push(new Array()); // zeroth group is left empty

        // if no group structure, simply copy and sort
        if ($scope.project.groupStructure == false) {
          entries.push(sortByKey(record.mapEntry, 'mapPriority'));

          // otherwise, initialize group arrays
        } else {
          // get the total number of groups
          var maxGroup = 1; // default
          for (var i = 0; i < record.mapEntry.length; i++) {
            if (record.mapEntry[i].mapGroup > maxGroup)
              maxGroup = record.mapEntry[i].mapGroup;
          }

          // initialize the group/entry array
          entries = new Array(maxGroup);
          for (var i = 0; i <= maxGroup; i++)
            entries[i] = new Array();

          // cycle over the entries and assign to group bins
          for (var i = 0; i < record.mapEntry.length; i++) {
            entries[record.mapEntry[i].mapGroup].push(record.mapEntry[i]);
          }

          // cycle over group bins and sort contents by map priority
          for (var i = 0; i < entries.length; i++) {
            entries[i] = sortByKey(entries[i], 'mapPriority');
          }
        }

        return entries;
      }

      $scope.toggleFalseConflict = function() {

        $http(
          {
            url : root_workflow + 'record/id/' + $routeParams.recordId
              + '/falseConflict/'
              + ($scope.isFalseConflict == true ? 'false' : 'true'),
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          // on success, flip the boolean isFalseConflict
          $scope.isFalseConflict = !$scope.isFalseConflict;

          // if record marked in conflict, broadcast the first record
          $scope.populateMapRecord($scope.record1);

        }).error(function(data, status, headers, config) {
          $scope.recordError = 'Error setting false conflict.';
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      // function to return trusted html code (for advice content)
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      $scope.getCurrentConversation = function(currentRecord) {
        if (currentRecord.id == $scope.record1.id)
          return $scope.conversation1;
        else if ($scope.record2 != null
          && currentRecord.id == $scope.record2.id)
          return $scope.conversation2;
        else if (currentRecord.id == $scope.leadRecord.id)
          return $scope.leadConversation;
        return null;
      };

      $scope.isFeedbackViewed = function() {
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

      $scope.getTitle = function(group, errMsg) {
        if (group == true && $scope.indicateDiscrepancyReview == true)
          return 'Discrepancy Review Feedback';
        else if (group == true)
          return 'Group Feedback';
        else if (errMsg != '' && errMsg != 'None')
          return 'Error Feedback';
        else
          return 'Feedback';
      };

      $scope.selectDiscrepancyReview = function(review) {
        $scope.indicateDiscrepancyReview = review;
      };

      $scope.tinymceOptions = {
        menubar : false,
        statusbar : false,
        plugins : 'autolink link image charmap searchreplace',
        toolbar : 'undo redo | styleselect | bold italic underline strikethrough | charmap link image',
        height : "150"
      };
      
      $scope.tinymceOptionsForGroupFeedback = {
      menubar : false,
      statusbar : false,
      plugins : 'autolink link image charmap searchreplace',
      toolbar : 'undo redo | styleselect | bold italic underline strikethrough | charmap link image',
      height : "300"
      };

      $scope.tinymceOptionsForGroupFeedback = {
        menubar : false,
        statusbar : false,
        plugins : 'autolink link image charmap searchreplace',
        toolbar : 'undo redo | styleselect | bold italic underline strikethrough | charmap link image',
        height : "300"
        };
      
      // add current user to list of viewers who have seen the feedback
      // conversation
      $scope.markViewed = function() {
        var needToUpdate = false;
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
          gpService.increment();
          $http({
            url : root_workflow + 'conversation/update',
            dataType : 'json',
            data : $scope.conversation,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            gpService.decrement();
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.recordError = 'Error updating feedback conversation.';
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      // determines default recipients dependending on the conversation
      function initializeReturnRecipients(conversation) {

        // if no previous feedback conversations, return just first map lead in
        // list
        if (conversation == null || conversation == '' || conversation.feedback.length == 0) {
          return;
        }

        // figure out the return recipients based on previous feedback in
        // conversation
        var localFeedback = conversation.feedback;
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
      
      // feedback groups functionality
      var feedbackGroupConfig = appConfig["deploy.feedback.group.names"]; 
      
      $scope.feedbackGroups = (feedbackGroupConfig == null || typeof feedbackGroupConfig == 'undefined' || feedbackGroupConfig === '')
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
      
      $scope.getUsersForConceptHistorical = function() {
        // retrieve all records with this concept id
        //gpService.increment();
        $http(
          {
            url : root_mapping + 'record/concept/id/' + $scope.leadRecord.conceptId 
              + '/project/id/' + $scope.project.id + '/users',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            var users = [];
            var map = new Map();
            if (data.mapUser) {
              for (const user of data.mapUser) {
                if(!map.has(user.id)){
                    map.set(user.id, true);
                    users.push(user);
                }
              }
            }
            $scope.returnRecipients = users;
            //gpService.decrement();
        }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
          //gpService.decrement();
        }).then(function() {          
        });
      };

    });
