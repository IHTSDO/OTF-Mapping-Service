/**
 * Controller for Map Project Records view
 * 
 * Basic function: 1) Retrieve map project 2) Retrieve concept associated with
 * map project refsetid 3) Retrieve records
 * 
 * Scope functions (accessible from html) - $scope.resetSearch clears query and
 * launches unfiltered record retrieval request - $scope.retrieveRecords
 * launches new record retrieval request based on current
 * paging/filtering/sorting parameters - $scope.to_trusted converts unsafe html
 * into usable/displayable code
 * 
 * Helper functions: - setPagination sets the relevant pagination attributes
 * based on current page - getNRecords retrieves the total number of records
 * available given filtering parameters - getUnmappedDescendants retrieves the
 * unmapped descendants of a concept - applyMapCategoryStyle modifies map
 * entries for display based on MAP_CATEGORY_STYLE - applyRelationshipStyle
 * modifies map entries for display based on RELATIONSHIP_STYLE -
 * constructPfsParameterObj creates a PfsParameter object from current
 * paging/filtering/sorting parameters, consumed by RESTful service
 */

'use strict';

angular
  .module('mapProjectApp.widgets.projectRecords', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('projectRecords', {
      title : 'Project Records',
      description : 'Displays map records for a map project.',
      controller : 'projectRecordsCtrl',
      templateUrl : 'js/widgets/projectRecords/projectRecords.html',
      edit : {}
    });
  })
  .controller(
    'projectRecordsCtrl',
    function($scope, $rootScope, $http, $routeParams, $location, $uibModal, $q,
      localStorageService, $sce, appConfig) {
      $scope.appConfig = appConfig;
      $scope.page = 'records';

      // the project id, extracted from route params
      $scope.projectId = $routeParams.projectId;

      // whether the terminology has viewable index
      $scope.indexViewerExists = false;

      // status variables
      $scope.unmappedDescendantsPresent = false;

      // error variables
      $scope.errorProject = '';
      $scope.errorConcept = '';
      $scope.errorRecords = '';

      // maximum limit for QA access
      $scope.qaRecordLimit = 500;

      // for collapse directive
      $scope.isCollapsed = true;

      $scope.conversation = null;

      // watch for changes to focus project
      $scope.$on('localStorageModule.notification.setFocusProject', function(
        event, parameters) {
        $scope.focusProject = parameters.focusProject;

        $scope.getRecordsForProject();
        $scope.setIndexViewerStatus();
        $scope.initializeSearchParameters();

      });

      // retrieve the current global variables
      $scope.focusProject = localStorageService.get('focusProject');
      $scope.mapProjects = localStorageService.get('mapProjects');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.preferences = localStorageService.get('preferences');

      // once focus project retrieved, retrieve the concept and records
      $scope.userToken = localStorageService.get('userToken');
      $scope.$watch([ 'focusProject', 'userToken' ], function() {
        // need both focus project and user token set before executing main
        // functions
        if ($scope.focusProject != null && $scope.userToken != null) {
          $http.defaults.headers.common.Authorization = $scope.userToken;
          $scope.projectId = $scope.focusProject.id;
          $scope.getRecordsForProject();
          $scope.initializeSearchParameters();
          $scope.setIndexViewerStatus();
        }
      });

      $scope.getRecordsForProject = function() {

        $scope.project = $scope.focusProject;

        // load first page
        $scope.retrieveRecords(1);
      };

      // Constructs trusted html code from raw/untrusted html code
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      // Helper function to retrieve records based on scope variables and
      // variable pfs
      // Two uses currently: normal project records retrieval (paged) and qa
      // record retrieval (unpaged)
      $scope.retrieveRecordsHelper = function(pfs) {

        $rootScope.resetGlobalError();

        var deferred = $q.defer();
        var query_url = root_mapping
          + 'record/project/id/'
          + $scope.project.objectId
          + '?ancestorId='
          + ($scope.searchParameters.ancestorId
            && $scope.searchParameters.advancedMode ? $scope.searchParameters.ancestorId
            : '') 
          + '&relationshipName='
          + ($scope.searchParameters.relationshipName
            && $scope.searchParameters.advancedMode ? encodeURIComponent($scope.searchParameters.relationshipName)
            : '') + '&query='
          + encodeURIComponent($scope.searchParameters.query)
        + '&excludeDescendants='
        + ($scope.searchParameters.advancedMode && $scope.searchParameters.descendants == 'excludes' ? 'true'
                : 'false') ;

        $rootScope.glassPane++;

        // retrieve map records
        $http.post(query_url, pfs).then(
          // Success
          function(response) {
            $rootScope.glassPane--;
            if (!response || !response.data || !response.data.mapRecord) {
              deferred.reject('Bad response');
            } else {
              deferred.resolve(response.data);
            }
          },
          // Error
          function(response) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(response.data, response.status,
              response.headers, response.config);
            deferred.reject('Bad request');
          });
        return deferred.promise;
      };

      // function to retrieve records for a specified page
      $scope.retrieveRecords = function(page) {
        // construct html parameters parameter
        var pfsParameterObj = $scope.getPfsFromSearchParameters(page);

        // NOTE: Glass Pane and error handling are done in helper function
        $scope.retrieveRecordsHelper(pfsParameterObj).then(
          function(data) {

            $scope.records = data.mapRecord;
            $scope.statusRecordLoad = '';

            // set pagination variables
            $scope.nRecords = data.totalCount;
            $scope.numRecordPages = Math.ceil(data.totalCount
              / $scope.searchParameters.recordsPerPage);

            // check if icon legends are necessary
            $scope.unmappedDescendantsPresent = false;

            // check relation syle flags
            if ($scope.project.mapRelationStyle === 'MAP_CATEGORY_STYLE') {
              applyMapCategoryStyle();
            }

            if ($scope.project.mapRelationStyle === 'RELATIONSHIP_STYLE') {
              applyRelationshipStyle();
            }

            // get unmapped descendants (checking done in routine)
            if ($scope.records.length > 0) {
              getUnmappedDescendants(0);
            }
          });
      };

      // Constructs a paging/filtering/sorting parameters object for RESTful
      // consumption
      function constructPfsParameterObj(page) {

        return {
          'startIndex' : (page - 1) * searchParameters.recordsPerPage,
          'maxResults' : searchParameters.recordsPerPage,
          'sortField' : null,
          'queryRestriction' : $scope.searchParameters.query
        }; // assigning simply to $scope.searchParameters.query when null
        // produces undefined

      }

      function getUnmappedDescendants(index) {

        // before processing this record, make call to start next async request
        if (index < $scope.records.length - 1) {
          getUnmappedDescendants(index + 1);
        }

        $scope.records[index].unmappedDescendants = [];

        // if descendants below threshold for lower-level concept, check for
        // unmapped
        if ($scope.records[index].countDescendantConcepts < 11) {

          $http(
            {
              url : root_mapping + 'concept/id/'
                + $scope.records[index].conceptId + '/'
                + 'unmappedDescendants/project/id/' + $scope.project.id,
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            if (data.count > 0)
              $scope.unmappedDescendantsPresent = true;
            $scope.records[index].unmappedDescendants = data.searchResult;
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }

      }

      function applyMapCategoryStyle() {

        // set the category display text
        $scope.mapRelationStyleText = 'Map Category Style';

        // Cycle over all entries. If targetId is blank, show relation name as
        // the target name
        for (var i = 0; i < $scope.records.length; i++) {
          for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {

            if ($scope.records[i].mapEntry[j].targetId === '') {
              $scope.records[i].mapEntry[j].targetName = $scope.records[i].mapEntry[j].mapRelation.name;
            }
          }
        }
      }

      function applyRelationshipStyle() {

        $scope.mapRelationStyleText = 'Relationship Style';

        // Cycle over all entries. Add the relation name to the advice list
        for (var i = 0; i < $scope.records.length; i++) {
          for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {
            if ($scope.records[i].mapEntry[j].targetId === '') {
              // get the object for easy handling
              var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;

              // add the serialized advice
              jsonObj.push({
                'id' : '0',
                'name' : $scope.records[i].mapEntry[j].mapRelation.name,
                'detail' : $scope.records[i].mapEntry[j].mapRelation.name,
                'objectId' : '0'
              });

              $scope.records[i].mapEntry[j].mapAdvice = jsonObj;
            }
          }
        }
      }

      $scope.logout = function() {
        $rootScope.glassPane++;
        $http(
          {
            url : root_security + 'logout/user/id/'
              + $scope.currentUser.userName,
            method : 'POST',
            headers : {
              'Content-Type' : 'text/plain'
            // save userToken from authentication
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $location.path(data);
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $location.path('/');
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      // function to change project from the header
      $scope.changeFocusProject = function(mapProject) {
        $scope.focusProject = mapProject;
        // update and broadcast the new focus project
        localStorageService.add('focusProject', $scope.focusProject);
        $rootScope.$broadcast(
          'localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

        // update the user preferences
        $scope.preferences.lastMapProjectId = $scope.focusProject.id;
        localStorageService.add('preferences', $scope.preferences);
        $rootScope.$broadcast(
          'localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

      };

      $scope.goToHelp = function() {
        var path;
        if ($scope.page != 'mainDashboard') {
          path = 'help/' + $scope.page + 'Help.html';
        } else {
          path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
        }
        // redirect page
        $location.path(path);
      };

      $scope.isEditable = function(record) {

        if (($scope.currentRole === 'Specialist'
          || $scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator')
          && (record.workflowStatus === 'PUBLISHED' || record.workflowStatus === 'READY_FOR_PUBLICATION')) {

          return true;

        } else if ($scope.currentUser.userName === record.owner.userName) {
          return true;
        } else
          return false;
      };

      $scope.editRecord = function(record) {

        // check if this record is assigned to the user and not in a publication
        // ready state
        if (record.owner.userName === $scope.currentUser.userName
          && record.workflowStatus != 'PUBLISHED'
          && record.workflowStatus != 'READY_FOR_PUBLICATION') {

          // go to the edit page
          $location.path('/record/recordId/' + id);

          // otherwise, assign this record along the FIX_ERROR_PATH
        } else {

          $rootScope.glassPane++;
          console
            .debug('Edit record clicked, assigning record along FIX_ERROR_PATH');
          $http(
            {
              url : root_workflow + 'assignFromRecord/user/id/'
                + $scope.currentUser.userName,
              method : 'POST',
              dataType : 'json',
              data : record,
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(
            function(data) {
              console.debug('Assignment successful', data);
              $http(
                {
                  url : root_workflow + 'record/project/id/'
                    + $scope.focusProject.id + '/concept/id/'
                    + record.conceptId + '/user/id/'
                    + $scope.currentUser.userName,
                  method : 'GET',
                  dataType : 'json',
                  data : record,
                  headers : {
                    'Content-Type' : 'application/json'
                  }
                }).success(function(data) {

                $rootScope.glassPane--;

                // open the record edit view
                $location.path('/record/recordId/' + data.id);
              }).error(function(data, status, headers, config) {
                $rootScope.glassPane--;

                $rootScope.handleHttpError(data, status, headers, config);
              });

            }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.truncate = function(string, plength) {
        var length = plength;
        if (length == null)
          length = 100;
        if (string.length > length)
          return string.slice(0, length - 3);
        else
          return string;
      };

      $scope.truncated = function(string, plength) {
        var length = plength;
        if (length == null)
          length = 100;
        if (string.length > length)
          return true;
        else
          return false;
      };

      $scope.openViewerFeedbackModal = function(lrecord, currentUser) {
        var modalInstance = $uibModal
          .open({
            templateUrl : 'js/widgets/projectRecords/projectRecordsViewerFeedback.html',
            controller : ViewerFeedbackModalCtrl,
            resolve : {
              record : function() {
                return lrecord;
              },
              currentUser : function() {
                return currentUser;
              }
            }
          });

      };

      var ViewerFeedbackModalCtrl = function($scope, $uibModalInstance, record) {
        console.debug('Entered modal control', record);

        $scope.record = record;
        $scope.project = localStorageService.get('focusProject');
        $scope.currentUser = localStorageService.get('currentUser');
        $scope.returnRecipients = $scope.project.mapLead;
        $scope.feedbackInput = '';

        $scope.sendFeedback = function(record, feedbackMessage, name, email) {
          console.debug('Sending feedback email', record);

          if (feedbackMessage == null || feedbackMessage == undefined
            || feedbackMessage === '') {
            window.alert('The feedback field cannot be blank. ');
            return;
          }

          if ($scope.currentUser.userName === 'guest'
            && (name == null || name == undefined || name === ''
              || email == null || email == undefined || email === '')) {
            window.alert('Name and email must be provided.');
            return;
          }

          if ($scope.currentUser.userName === 'guest'
            && validateEmail(email) == false) {
            window.alert('Invalid email address provided.');
            return;
          }

          var sList = [ name, email, record.conceptId, record.conceptName,
            $scope.project.refSetId, feedbackMessage ];

          $rootScope.glassPane++;
          $http({
            url : root_workflow + 'message',
            dataType : 'json',
            method : 'POST',
            data : sList,
            headers : {
              'Content-Type' : 'application/json'
            }

          }).success(function(data) {
            console.debug('success to sendFeedbackEmail.');
            $rootScope.glassPane--;
            $uibModalInstance.close();
          }).error(function(data, status, headers, config) {
            $uibModalInstance.close();
            $scope.recordError = 'Error sending feedback email.';
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };

        $scope.cancel = function() {
          $uibModalInstance.dismiss('cancel');
        };

        function validateEmail(email) {
          var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
          return re.test(email);
        }

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

      };

      // //////////////////////////////
      // Advanced Search Components
      // //////////////////////////////
      $scope.searchParameters = {
        query : '',
        page : 1,
        recordsPerPage : 10,
        advancedMode : false,

        // advanced options
        ancestorId : null,
        relationshipName : null,
        descendants : null,
        rootId : null,
        targetId : [],
        targetIdRangeStart : null,
        targetIdRangeEnd : null,
        targetIdRangeIncluded: true,
        targetIdRange2Start : null,
        targetIdRange2End : null,
        targetIdRange2Included: true,
        targetName : null,
        adviceContained : null,
        adviceName : null,
        principleContained : true,
        principleName : null,
        ruleCategory : null,
        mapGroup : null,
        mapPriority : null,
        descendantsOptions :
    		  ['mapped', 'excludes'],
    	        adviceOptions :
    	    		  ['contains', 'does not contain', 'none'],
    	ruleCategories : [ 'TRUE', 'Gender - Male', 'Gender - Female',
    		                            'Age - Chronological', 'Age - At Onset' ],

        // search display data
        roots : [], // source terminology root concepts
        advices : []
      // map advices
      };

      // function to clear input box and return to initial view
      $scope.resetSearch = function() {
        $scope.searchParameters.query = null;
        $scope.searchParameters.page = 1;
        $scope.searchParameters.targetId = [];
        $scope.searchParameters.descendants = null;
        $scope.searchParameters.targetIdRangeStart = null;
        $scope.searchParameters.targetIdRangeEnd = null;
        $scope.searchParameters.targetIdRangeIncluded = true;
        $scope.searchParameters.targetIdRange2Start = null;
        $scope.searchParameters.targetIdRange2End = null;
        $scope.searchParameters.targetIdRange2Included = true;
        $scope.searchParameters.targetName = null;
        $scope.searchParameters.rootId = null;
        $scope.searchParameters.ancestorId = null;
        $scope.searchParameters.relationshipName = null;
        $scope.searchParameters.adviceName = null;
        $scope.searchParameters.adviceContained = null;
        $scope.searchParameters.ruleCategory = null;
        $scope.searchParameters.principleName = null;
        $scope.searchParameters.principleContained = true;
        $scope.searchParameters.mapGroup = null;
        $scope.searchParameters.mapPriority = null;
        $scope.retrieveRecords(1);
      };

      // on load, get the root trees for the terminology
      $scope.initializeSearchParameters = function() {

        console.debug('Getting root trees');
        $rootScope.glassPane++;
        $http.get(
          root_mapping + 'treePosition/project/id/' + $scope.focusProject.id
            + '/source').then(
          // Success
          function(response) {
            $rootScope.glassPane--;
            console.debug('Root trees', response);
            $scope.searchParameters.roots = response.data.treePosition;
          },
          // Error
          function(response) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(response.data, response.status,
              response.headers, response.config);

          });

        $scope.searchParameters.advices = $scope.focusProject.mapAdvice;
        $scope.searchParameters.principles = $scope.focusProject.mapPrinciple;
      };

      // toggle advanced search
      $scope.toggleAdvancedSearch = function() {
        // set the display flag
        $scope.searchParameters.advancedMode = !$scope.searchParameters.advancedMode;
      };

      $scope.getPfsFromSearchParameters = function(page) {
        $scope.searchParameters.page = page;
        console.debug('Getting pfs from search parameters',
          $scope.searchParameters);
        var pfs = {
          'startIndex' : ($scope.searchParameters.page - 1)
            * $scope.searchParameters.recordsPerPage,
          'maxResults' : $scope.searchParameters.recordsPerPage,

          // NOTE: If query specified, do not sort by concept id (preserve
          // result relevance)
          'sortField' : $scope.searchParameters
            && $scope.searchParameters.query ? null : 'conceptId',
          'queryRestriction' : ''
        };

        if ($scope.searchParameters.advancedMode) {
          console.debug('Advanced search mode specified');
          var queryRestrictions = [];
          // check target id
          if ($scope.searchParameters.targetId
            && $scope.searchParameters.targetId.length > 0) {
        	  $scope.searchParameters.targetId.forEach(function (s) {
		            queryRestrictions.push('mapEntries.targetId:'
		              + s);
          		}
            );
          }

          // check target id range
          if ($scope.searchParameters.targetIdRangeStart
            && $scope.searchParameters.targetIdRangeEnd) {
              queryRestrictions.push(($scope.searchParameters.targetIdRangeIncluded ? '' : '!')
                + 'mapEntries.targetId:['
              + $scope.searchParameters.targetIdRangeStart + ' TO '
              + $scope.searchParameters.targetIdRangeEnd + ']')
          }

          if ($scope.searchParameters.targetIdRange2Start
                  && $scope.searchParameters.targetIdRange2End) {
              queryRestrictions.push(($scope.searchParameters.targetIdRange2Included ? '' : '!')
                      + 'mapEntries.targetId:['
                    + $scope.searchParameters.targetIdRange2Start + ' TO '
                    + $scope.searchParameters.targetIdRang2eEnd + ']')
          }

          // check target name
          if ($scope.searchParameters.targetName
            && $scope.searchParameters.targetName.length > 0) {
            queryRestrictions.push('mapEntries.targetName:'
              + $scope.searchParameters.targetName);
          }

          // check map advices
          switch ($scope.searchParameters.adviceContained) {
    	  	case $scope.searchParameters.adviceOptions[0] : if ($scope.searchParameters.adviceName) { queryRestrictions.push(
    	  			'mapEntries.mapAdvices.name:"' + $scope.searchParameters.adviceName + '"'); }
    	  	break;
    	  	case $scope.searchParameters.adviceOptions[1] : if ($scope.searchParameters.adviceName) { queryRestrictions.push(
    	  			'NOT mapEntries.mapAdvices.name:"' + $scope.searchParameters.adviceName + '"'); }
    	  	
    	  	break;
    	  	case $scope.searchParameters.adviceOptions[2] : queryRestrictions.push('-mapEntries.mapAdvices.name:[* TO *]');
    	  	break;
    	  }
    	  
		  switch ($scope.searchParameters.ruleCategory) {
    		  case $scope.searchParameters.ruleCategories[0] : {
    			  queryRestrictions.push('mapEntries.rule:' + $scope.searchParameters.ruleCategories[0]);
    		  	}
    		  break;
    		  case $scope.searchParameters.ruleCategories[1] : {
    			  queryRestrictions.push('mapEntries.rule:' + 'IFA 248153007');
    		  	}
      	  	  break;
    		  case $scope.searchParameters.ruleCategories[2] : {
    			  queryRestrictions.push('mapEntries.rule:' + 'IFA 248152002');
    		  	}
      	  	  break;
    		  case $scope.searchParameters.ruleCategories[3] : {
    			  queryRestrictions.push('mapEntries.rule:' + 'IFA 424144002');
    		  	}
    		  case $scope.searchParameters.ruleCategories[4] : {
    			  queryRestrictions.push('mapEntries.rule:' + 'IFA 445518008');
    		  	}
    		  break;
		  }
    	  
          // check map group
          if ($scope.searchParameters.mapGroup) {
            queryRestrictions.push('mapEntries.mapGroup:'
              + $scope.searchParameters.mapGroup);
            queryRestrictions.push('-mapEntries.mapGroup:{'
              + $scope.searchParameters.mapGroup + ' TO *}');
          }

          // check map priority
          if ($scope.searchParameters.mapPriority) {
            queryRestrictions.push('mapEntries.mapPriority:'
              + $scope.searchParameters.mapPriority);
            queryRestrictions.push('-mapEntries.mapPriority:{'
              + $scope.searchParameters.mapPriority + ' TO *}');
          }

          // check map principles
          if ($scope.searchParameters.principleName) {
            queryRestrictions
              .push(($scope.searchParameters.principleContained ? '' : 'NOT ')
                + 'mapPrinciples.name:"'
                + $scope.searchParameters.principleName + '"');
          }

          // Flags
          if ($scope.searchParameters.flagForMapLeadReview) {
            queryRestrictions.push('flagForMapLeadReview:true');
          }
          if ($scope.searchParameters.flagForEditorialReview) {
            queryRestrictions.push('flagForEditorialReview:true');
          }
          if ($scope.searchParameters.flagForConsensusReview) {
            queryRestrictions.push('flagForConsensusReview:true');
          }

          for (var i = 0; i < queryRestrictions.length; i++) {
            pfs.queryRestriction += (pfs.queryRestriction.length > 0 ? ' AND '
              : '')
              + queryRestrictions[i];
          }

        }

        return pfs;
      };

      // Open the QA Records modal
      $scope.openQaRecordsModal = function() {
        console.debug('openQaRecordsModal');

        var pfs = $scope.getPfsFromSearchParameters(1);
        pfs.startIndex = -1;
        pfs.maxResults = -1;
        $scope
          .retrieveRecordsHelper(pfs)
          .then(
            function(recordList) {
              if (recordList.count !== $scope.nRecords) {
                utilService
                  .handleError('Mismatch between QA record retrieval and existing results. Aborting QA.');
              } else {

                var modalInstance = $uibModal.open({
                  templateUrl : 'js/widgets/projectRecords/qa-records.html',
                  controller : QaRecordsCtrl,
                  resolve : {

                    // used to confirm that unpaged search matches paged search
                    // results
                    records : function() {
                      return recordList.mapRecord;
                    }
                  }
                });

                // on close (success or fail), reload records
                modalInstance.result.then(function() {
                  $scope.retrieveRecords(1);
                }, function() {
                  $scope.retrieveRecords(1);
                });
              }
            });

      };

      // QA records modal controller
      var QaRecordsCtrl = function($scope, $uibModalInstance, $q, utilService,
        records) {
        console.debug('Entered modal control', records);

        if (records.length == 0) {
          utilService
            .handleError('Failed to open QA Modal: No records specified for QA');
          return;
        }

        if (records.length > $scope.qaRecordLimit) {
          utilService
            .handleError('Failed to open QA Modal: Too many records specified for QA');
          return;
        }

        // get the project id from the first record
        var projectId = records[0].mapProjectId;

        if (!projectId) {
          utilService
            .handleError('Failed to open QA Modal: Could not determine map project');
          return;
        }

        // Scope vars
        $scope.isRunning = false;
        $scope.qaSucceeded = 0;
        $scope.qaFailed = 0;
        $scope.qaSkipped = 0;
        $scope.qaComplete = 0;
        $scope.qaTotal = records.length;

        // Cancel
        $scope.cancel = function() {
          $scope.qaCancelled = true;
          $scope.isRunning = false;
        };

        // Close
        $scope.close = function() {
          $uibModalInstance.close();
        };

        // Create QA records
        $scope.qaRecords = function(label) {
          if (label.match(/[^a-zA-Z0-9,\.\- ]/)) {
            window
              .alert("QA labels may contain only alphanumeric characters, dot, dash, and space characters");
            return;
          }
          $scope.isRunning = true;
          $scope.qaSucceeded = 0;
          $scope.qaFailed = 0;
          $scope.qaSkipped = 0;
          $scope.qaComplete = 0;

          // call the recursive helper function
          qaRecordsHelper(records, label, 0);

        };

        // recursively sends records for qa
        function qaRecordsHelper(records, label, index) {

          if (index == records.length) {
            $scope.isRunning = false;
            return;
          }

          qaRecord(records[index], label)['finally'](function() {
            qaRecordsHelper(records, label, index + 1);
          });
        }

        // helper function (with promise) to assign a single record to QA
        function qaRecord(record, label) {
          var deferred = $q.defer();
          // if stop request detected, reject
          if (!$scope.isRunning) {
            $scope.qaSkipped++;
            deferred.reject();
          } else {
            if (!record.labels) {
              record.labels = [];
            }
            record.labels.push(label);
            $rootScope.glassPane++;
            $http.post(root_workflow + 'createQARecord', record).then(
              function() {
                $scope.qaSucceeded++;
                $rootScope.glassPane--;
                deferred.resolve();
              }, function() {
                $scope.qaFailed++;
                $rootScope.glassPane--;
                deferred.reject();
              });
          }

          $scope.qaComplete++; // really should be called submitted, but meh
          return deferred.promise;
        }

        // Clear the error
        $scope.clearError = function() {
          $scope.error = null;
        };

      };

      $scope.setIndexViewerStatus = function() {
        console.debug('Get index viewer status',
          $scope.project.destinationTerminology);
        $http(
          {
            url : root_content + 'index/'
              + $scope.project.destinationTerminology + '/'
              + $scope.project.destinationTerminologyVersion,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          console.debug('  data = ', data);
          if (data.searchResult.length > 0) {
            $scope.indexViewerExists = true;
          } else {
            $scope.indexViewerExists = false;
          }
        }).error(function(data, status, headers, config) {
          $scope.indexViewerExists = false;
        });
      }

      // opens the index viewer
      $scope.openIndexViewer = function() {
        var currentUrl = window.location.href;
        var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
        var newUrl = baseUrl + '/index/viewer';
        var myWindow = window.open(newUrl, 'indexViewerWindow');
        myWindow.focus();
      };

    });
