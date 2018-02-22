'use strict';

angular.module('mapProjectApp.widgets.feedback', [ 'adf.provider' ]).config(
  function(dashboardProvider) {
    dashboardProvider.widget('feedback', {
      title : 'Feedback',
      description : 'Displays a list of feedback notes',
      controller : 'feedbackCtrl',
      templateUrl : 'js/widgets/feedback/feedback.html',
      edit : {}
    });
  })
  .controller(
    'feedbackCtrl',
    function($scope, $rootScope, $http, $location, $uibModal, $sce, localStorageService) {
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.focusProject = null;
      $scope.feedbackConversations = null;

      $scope.feedbackTypes = [ 'All Feedback', 'Feedback', 'Group Feedback', 'Error Feedback',
        'Discrepancy Review Feedback' ];
      $scope.reviewedTypes = [ 'All', 'Viewed', 'Unviewed' ];
      $scope.resolvedTypes = [ 'All', 'Active', 'Resolved' ];
      $scope.ownedByList = [ 'All', 'Owned By Me', 'Not Owned By Me' ];

      // initialize as empty to indicate still initializing database connection
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentUserToken = localStorageService.get('userToken');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.focusProject = localStorageService.get('focusProject');

      // table sort fields
      $scope.tableFields = [ {
        id : 0,
        title : 'id',
        sortDir : 'asc',
        sortOn : false
      } ];

      $scope.mapUserViewed == null;
      $scope.searchPerformed = false; // initialize variable to track whether
      // search was performed
      $scope.feedbackType = 'All Feedback';
      $scope.resolvedType = 'All';
      $scope.reviewedType = 'All';
      $scope.ownedByMe = 'All';
      $scope.recordIdOwnerMap = new Array();

      // pagination variables
      $scope.recordsPerPage = 10;
      $scope.recordPage = 1;

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        $scope.focusProject = parameters.focusProject;
      });
      
      // on publish, update feedbacks to display newly resolved
      $scope.$on('feedbackWidget.notification.retrieveFeedback', function(event, parameters) {
          $scope.retrieveFeedback(1, $scope.feedbackType, $scope.reviewedType,
                  $scope.resolvedType, $scope.ownedByMe, $scope.query);
      });

      // on any change of focusProject, retrieve new available work
      $scope
        .$watch([ 'focusProject', 'currentUser', 'currentUserToken', 'currentRole' ],
          function() {

            if ($scope.focusProject != null && $scope.currentUser != null
              && $scope.currentUserToken != null && $scope.currentRole != null) {
              $http.defaults.headers.common.Authorization = $scope.currentUserToken;
              $scope.mapUsers = $scope.focusProject.mapSpecialist
                .concat($scope.focusProject.mapLead);
              $scope.retrieveFeedback(1, $scope.feedbackType, $scope.reviewedType,
                $scope.resolvedType, $scope.ownedByMe, $scope.query);
            }
          });

      $scope.retrieveFeedback = function(ppage, feedbackType, reviewedType, resolvedType, ownedByMe, pquery) {

    	console.log("ppage", ppage, 
    			"feedbackType", feedbackType,
    			"reviewedType", reviewedType, 
    			"resolvedType", resolvedType, 
    			"ownedByMe", ownedByMe, 
    			"pquery", pquery);
    	  
        var query = pquery;
        var page = ppage;

        $scope.feedbackType = feedbackType;
        $scope.resolvedType = resolvedType;
        $scope.reviewedType = reviewedType;
        $scope.ownedByMe = ownedByMe;

        // add a check to ensure page is not null
        if (page == null)
          page = 1;

        // construct full query based on all parameters
        if (query == null || query == 'undefined' || query == '')
          query = 'mapProjectId:' + $scope.focusProject.id;
        else
          query = query + ' AND mapProjectId:' + $scope.focusProject.id;

        if (feedbackType == 'Feedback')
          query = query
            + ' AND title:Feedback NOT title:Discrepancy NOT title:Error NOT title:Group';
        else if (feedbackType != null && feedbackType != 'undefined' && feedbackType != ''
          && feedbackType != 'All Feedback')
          query = query + ' AND title:\"' + feedbackType + '\"';
        if (reviewedType != null && reviewedType != 'undefined' && reviewedType != ''
          && reviewedType != 'All')
          query = query + ' AND viewed:' + (reviewedType == 'Viewed' ? 'true' : 'false');
        if (resolvedType != null && resolvedType != 'undefined' && resolvedType != ''
          && resolvedType != 'All')
          query = query + ' AND resolved:' + (resolvedType == 'Active' ? 'false' : 'true');
        //owned by Me
        if (ownedByMe != null && ownedByMe != 'undefined' && ownedByMe != ''
            && ownedByMe != 'All')
            query = query + ' AND ownedByMe:' + (ownedByMe == 'Owned By Me' ? 'true' : 'false');
        
        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : (page - 1) * $scope.recordsPerPage,
          'maxResults' : $scope.recordsPerPage,
          'sortField' : 'lastModified'      
        };
        
        console.log("pfsParameterObj", pfsParameterObj);
              
        $rootScope.glassPane++;

        $http(
          {
            url : root_workflow + 'conversation/project/id/' + $scope.focusProject.id + '/'
              + $scope.currentUser.userName + '?query=' + encodeURIComponent(query),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          $rootScope.glassPane--;

          // set pagination variables
          $scope.nRecords = data.totalCount;
          $scope.numRecordPages = Math.ceil(data.totalCount / $scope.recordsPerPage);

          $scope.feedbackConversations = data.feedbackConversation;

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      // if any of the feedbacks are not yet viewed, return false indicating
      // that conversation is not yet viewed
      $scope.isFeedbackViewed = function(conversation) {
    	      	  
        for (var i = 0; i < conversation.feedback.length; i++) {
          var alreadyViewedBy = conversation.feedback[i].viewedBy;
          var found = false;
          
          for (var j = 0; j < alreadyViewedBy.length; j++) {
        	  if (alreadyViewedBy[j].userName == $scope.currentUser.userName) {
              	found = true;
        	  }
          }
          if (found == false) {
            return false;
          }
        }
        return true;
      };

      $scope.markActive = function(conversation) {
        conversation.resolved = false;
        updateFeedbackConversation(conversation);
      };

      $scope.markFeedbackResolved = function(conversation) {
        conversation.resolved = true;
        updateFeedbackConversation(conversation);
      };
      
      $scope.markFeedbackViewed = function(conversation) {
        for (var i = 0; i < conversation.feedback.length; i++) {
          var alreadyViewedBy = conversation.feedback[i].viewedBy;
          alreadyViewedBy.push($scope.currentUser);
          conversation.feedback[i].viewedBy = alreadyViewedBy;
        }
        updateFeedbackConversation(conversation);
      };
      
      function updateFeedbackConversation(conversation) {
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
          $rootScope.glassPane--;
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $scope.recordError = 'Error updating feedback conversation.';
          $rootScope.handleHttpError(data, status, headers, config);
        });
      }

      $scope.goFeedbackConversations = function(id) {
        var path = '/conversation/recordId/' + id;
        // redirect page
        $location.path(path);
      };

      // function to return trusted html code (for advice content)
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      // function to clear input box and return to initial view
      $scope.resetSearch = function() {
        $scope.query = null;
        $scope.retrieveFeedback(1, 'All Feedback', 'All', 'All', 'All', '');
      };
    });
