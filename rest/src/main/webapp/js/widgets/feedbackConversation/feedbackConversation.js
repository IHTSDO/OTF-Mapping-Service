'use strict';

angular
  .module('mapProjectApp.widgets.feedbackConversation', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('feedbackConversation', {
      title : 'Feedback Conversation',
      description : 'Displays a Feedback Conversation',
      controller : 'feedbackConversationCtrl',
      templateUrl : 'js/widgets/feedbackConversation/feedbackConversation.html',
      edit : {}
    });
  })
  .controller(
    'feedbackConversationCtrl',
    function($scope, $rootScope, $routeParams, $http, $location, $uibModal, $sce,
      localStorageService) {

      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.focusProject = null;
      $scope.conversation = null;
      $scope.record = null;
      // Mark false because loading this is viewing the record
      $scope.markUnviewed = false;

      // initialize as empty to indicate still initializing database
      // connection
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.currentUserToken = localStorageService.get('userToken');
      $scope.focusProject = localStorageService.get('focusProject');

      $scope.recordId = $routeParams.recordId;

      // conflict records
      $scope.record1 = null;
      $scope.record2 = null;

      // flag indicating if index viewer is available for dest terminology
      $scope.indexViewerExists = false;

      // settings for recipients mechanism
      $scope.allUsers = new Array();
      $scope.returnRecipients = new Array();
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

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        $scope.focusProject = parameters.focusProject;
        $scope.allUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
        organizeUsers($scope.allUsers);
        initializeReturnRecipients($scope.conversation);
      });

      // required for authorization when right-clicking to open feedback
      // conversation from feedback list

      // on any change of focusProject, retrieve new available work
      $scope.$watch([ 'focusProject', 'currentUser', 'currentUserToken' ], function() {
        if ($scope.focusProject != null && $scope.currentUser != null
          && $scope.currentUserToken != null) {
          $http.defaults.headers.common.Authorization = $scope.currentUserToken;
          $scope.allUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
          organizeUsers($scope.allUsers);
          $scope.getFeedbackConversation();

          setIndexViewerStatus();
        }
      });

      $scope.allUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
      organizeUsers($scope.allUsers);

      // function to retrieve the feedback conversation based on record id
      $scope.getFeedbackConversation = function() {
        $rootScope.glassPane++;
        $http({
          url : root_workflow + 'conversation/id/' + $scope.recordId,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          $scope.conversation = data;
          $scope.markFeedbackViewed($scope.conversation, $scope.currentUser);
          initializeReturnRecipients($scope.conversation);

          $scope.record = null;

          // load record to be displayed; try to find active
          // record first
          $http({
            url : root_mapping + 'record/id/' + $scope.conversation.mapRecordId + '/historical',
            dataType : 'json',
            method : 'GET',
            authorization : $scope.currentUserToken,
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

            $rootScope.glassPane--;

            $scope.record = data;

            setTitle();

            // get the conflict records if they exist
            var originIds = $scope.record.originIds;
            if (originIds != null && originIds.length > 0) {

              $rootScope.glassPane++;
              $http({
                url : root_mapping + 'record/id/' + originIds[0] + '/historical',
                dataType : 'json',
                method : 'GET',
                authorization : $scope.currentUserToken,
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {

                $rootScope.glassPane--;

                $scope.record1 = data;

                if (originIds != null && originIds.length == 2) {
                  $rootScope.glassPane++;
                  $http({
                    url : root_mapping + 'record/id/' + originIds[1] + '/historical',
                    dataType : 'json',
                    method : 'GET',
                    authorization : $scope.currentUserToken,
                    headers : {
                      'Content-Type' : 'application/json'
                    }
                  }).success(function(data) {
                    $rootScope.glassPane--;
                    $scope.record2 = data;

                    setDisplayRecords();
                  }).error(function(data, status, headers, config) {
                    $rootScope.glassPane--;
                    $rootScope.handleHttpError(data, status, headers, config);
                  });
                }
              }).error(function(data, status, headers, config) {
                $rootScope.glassPane--;
                $rootScope.handleHttpError(data, status, headers, config);
              });
            }
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      function setDisplayRecords() {
        if ($scope.currentRole == 'Lead') {
          // keep main record and both conflict records if they exist
          // do nothing - keep all records

          // MAP-1354: always show all records involved in a feedback
          // conversation
          // } else if ($scope.currentRole == 'Specialist') {
          // // check if owner of main record
          // if ($scope.record.owner.userName == $scope.currentUser.userName) {
          // // set blank conflict records
          // $scope.record1 = null;
          // $scope.record2 = null;
          // } else {
          // // check if owner of either conflict record
          // if ($scope.record1 != null
          // && $scope.record1.owner.userName == $scope.currentUser.userName) {
          // // set blank main record and other conflict record
          // $scope.record = null;
          // $scope.record2 = null;
          // } else if ($scope.record2 != null
          // && $scope.record2.owner.userName == $scope.currentUser.userName) {
          // // set blank main record and other conflict record
          // $scope.record = null;
          // $scope.record1 = null;
          // } else { // specialist is not involved
          // // display only main record, if exists
          // $scope.record1 = null;
          // $scope.record2 = null;
          // }
          // }
        }
      }

      // function to return trusted html code
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      // update the title to contain conceptId and preferred name
      function setTitle() {
        $scope.model.title = $scope.conversation.title + ' - Concept '
          + $scope.conversation.terminologyId + ':  ' + $scope.conversation.defaultPreferredName;
      }

      // options for the rich text feedback input field
      $scope.tinymceOptions = {
        menubar : false,
        statusbar : false,
        plugins : 'autolink link image charmap searchreplace',
        toolbar : 'undo redo | styleselect | bold italic underline strikethrough | charmap link image',
      };

      // send feedback on already started conversation
      $scope.sendFeedback = function(record, feedbackMessage, conversation, recipientList) {

        if (feedbackMessage == null || feedbackMessage == undefined || feedbackMessage === '') {
          window.alert('The feedback field cannot be blank. ');
          return;
        }
        // figure out the return recipients based on previous feedback
        // in
        // conversation
        var localFeedback = conversation.feedback;

        // copy recipient list
        var localRecipients = recipientList.slice(0);
        var newRecipients = new Array();
        for (var i = 0; i < localRecipients.length; i++) {
          for (var j = 0; j < $scope.allUsers.length; j++) {
            if (localRecipients[i].id == $scope.allUsers[j].id)
              newRecipients.push($scope.allUsers[j]);
          }
        }

        // create feedback msg to be added to the conversation
        var feedback = {
          'message' : feedbackMessage,
          'mapError' : '',
          'timestamp' : new Date(),
          'sender' : $scope.currentUser,
          'recipients' : newRecipients,
          'isError' : 'false',
          'viewedBy' : [ $scope.currentUser ]
        };

        localFeedback.push(feedback);
        conversation.feedback = localFeedback;

        updateFeedbackConversation(conversation);

      };

      // add current user to list of viewers who have seen the feedback
      // conversation
      $scope.markFeedbackViewed = function(conversation, user) {
        var needToUpdate = false;
        for (var i = 0; i < conversation.feedback.length; i++) {
          var alreadyViewedBy = conversation.feedback[i].viewedBy;
          var found = false;
          for (var j = 0; j < alreadyViewedBy.length; j++) {
            if (alreadyViewedBy[j].userName == user.userName)
              found = true;
          }
          if (found == false) {
            alreadyViewedBy.push(user);
            needToUpdate = true;
          }
        }

        if (needToUpdate == true) {
          updateFeedbackConversation(conversation, false);
        }
      };

      // Makr this conversation as unviewed and save.
      $scope.markFeedbackUnviewed = function(conversation) {
        for (var i = conversation.feedback.length; i--;) {
          var alreadyViewedBy = conversation.feedback[i].viewedBy;
          for (var j = 0; j < alreadyViewedBy.length; j++) {
            if (alreadyViewedBy[j].userName == $scope.currentUser.userName) {
              alreadyViewedBy.splice(j, 1);
              updateFeedbackConversation(conversation);
              break;
            }
          }
        }
      };

      // Toggle mark unviewed flag
      $scope.toggleMarkUnviewed = function() {
        if ($scope.markUnviewed) {
          $scope.markFeedbackUnviewed($scope.conversation);
        } else {
          $scope.markFeedbackViewed($scope.conversation, $scope.currentUser);
        }
      }

      // opens SNOMED CT browser
      $scope.getBrowserUrl = function() {
        if ($scope.currentUser.userName === 'guest') {
          return 'http://browser.ihtsdotools.org/index.html?perspective=full&conceptId1='
            + $scope.conversation.terminologyId + '&acceptLicense=true';

        } else {
          return 'http://dailybuild.ihtsdotools.org/index.html?perspective=full&conceptId1='
            + $scope.conversation.terminologyId + '&acceptLicense=true';
        }

      };

      $scope.openConceptBrowser = function() {
        window.open($scope.getBrowserUrl(), 'browserWindow');
      };

      $scope.openIndexViewer = function() {
        var currentUrl = window.location.href;
        var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
        var newUrl = baseUrl + '/index/viewer';
        var myWindow = window.open(newUrl, 'indexViewerWindow');
        myWindow.focus();
      };

      // redirects to the record editing or conflict editing page
      $scope.goEdit = function(record) {
        if (record.workflowStatus == 'CONFLICT_NEW') {
          var path = '/record/conflicts/' + record.id;
          // redirect page
          $location.path(path);
        } else {
          var path = '/record/recordId/' + record.id;
          // redirect page
          $location.path(path);
        }
      };

      // redirect to the concept view
      $scope.goConceptView = function(id) {
        var path = '/record/conceptId/' + id;
        // redirect page
        $location.path(path);
      };

      // determines if the 'Edit Record' button should be displayed
      $scope.displayEdit = function() {
        if ($scope.record == null || $scope.record == undefined)
          return false;

        if (($scope.record.workflowStatus == 'CONFLICT_DETECTED' || $scope.record.workflowStatus == 'CONFLICT_NEW')
          && $scope.currentRole != 'Lead')
          return false;

        if ($scope.currentUser.userName == $scope.record.owner.userName
          && $scope.conversation.active == true)
          return true;
        else
          return false;
      };

      // determines default recipients dependending on the conversation
      function initializeReturnRecipients(conversation) {

        // if no previous feedback conversations, return just first map
        // lead in
        // list
        if (conversation == null || conversation == '') {
          $scope.returnRecipients.push($scope.focusProject.mapLead[0]);
          return;
        }

        // figure out the return recipients based on previous feedback
        // in
        // conversation
        var localFeedback = conversation.feedback;
        var localSender = localFeedback[localFeedback.length - 1].sender;
        var localRecipients = localFeedback[localFeedback.length - 1].recipients;
        if (localSender.userName == $scope.currentUser.userName)
          $scope.returnRecipients = localRecipients;
        else {
          $scope.returnRecipients.push(localSender);
          for (var i = 0; i < localRecipients.length; i++) {
            if (localRecipients[i].userName != $scope.currentUser.userName)
              $scope.returnRecipients.push(localRecipients[i]);
          }
        }
        return;
      }

      // Scope function for updating feedback conversation
      $scope.updateFeedbackConversation = function(conversation, refreshFlag) {
        updateFeedbackConversation(conversation, refreshFlag);
      }
      function updateFeedbackConversation(conversation, refreshFlag) {
        $rootScope.glassPane++;

        $http({
          url : root_workflow + 'conversation/update',
          dataType : 'json',
          data : conversation,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          if (refreshFlag) {
            $scope.getFeedbackConversation();
          }
          $rootScope.glassPane--;
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $scope.recordError = 'Error updating feedback conversation.';
          $rootScope.handleHttpError(data, status, headers, config);
        });
      }

      function organizeUsers(arr) {
        // remove Current user
        for (var i = arr.length; i--;) {
          if (arr[i].userName === $scope.currentUser.userName) {
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

      function setIndexViewerStatus() {
        $http(
          {
            url : root_content + 'index/' + $scope.focusProject.destinationTerminology + '/'
              + $scope.focusProject.destinationTerminologyVersion,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          if (data.searchResult.length > 0) {
            $scope.indexViewerExists = true;
          } else {
            $scope.indexViewerExists = false;
          }
        }).error(function(data, status, headers, config) {
          $scope.indexViewerExists = false;
        });
      }

    });