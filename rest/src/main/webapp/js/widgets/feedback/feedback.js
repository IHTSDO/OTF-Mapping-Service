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
    function($scope, $rootScope, $http, $location, $uibModal, $sce, localStorageService, gpService) {
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
      $scope.resolvedType = 'Active';
      $scope.reviewedType = 'All';
      $scope.ownedByMe = 'All';
      $scope.recordIdOwnerMap = new Array();

      // pagination variables
      $scope.recordsPerPage = 10;
      $scope.recordPage = 1;
      
      
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
    	  if (table === 'feedback') {
    		  $scope.retrieveFeedback(1, $scope.feedbackType, $scope.reviewedType,
                  $scope.resolvedType, $scope.ownedByMe, $scope.query);
    	  }
      };

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
          'sortField' : (sortField['feedback']) ? sortField['feedback'] : 'lastModified',
          'ascending' : sortAscending['feedback'],   
        };
        
        console.log("pfsParameterObj", pfsParameterObj);
        
        gpService.increment();

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
          gpService.decrement();

          // set pagination variables
          $scope.nRecords = data.totalCount;
          $scope.numRecordPages = Math.ceil(data.totalCount / $scope.recordsPerPage);

          $scope.feedbackConversations = data.feedbackConversation;

        }).error(function(data, status, headers, config) {
          gpService.decrement();
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
        $scope.retrieveFeedback($scope.ppage, $scope.feedbackType, $scope.reviewedType, $scope.resolvedType, $scope.ownedByMe, $scope.pquery);
      };
      
      function updateFeedbackConversation(conversation) {
        gpService.increment();

        $http({
          url : root_workflow + 'conversation/update',
          dataType : 'json',
          data : conversation,
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

      $scope.goFeedbackConversations = function(id) {
        var path = '/conversation/recordId/' + id;
        // redirect page
        $location.path(path);
      };

      // function to return trusted html code (for advice content)
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

	  // return a list of everyone included in the conversation
	  $scope.getParticipants = function(conversation){
		var participantsList = [];
		for (var i = 0; i < conversation.feedback.length; i++) {
          var partipicants = conversation.feedback[i].recipients;
			for(var j = 0; j < partipicants.length; j++){
				var participantUserName = partipicants[j].userName;
				if(!participantsList.includes(participantUserName)){
					participantsList.push(participantUserName);
				}
			}
		}
		
		var participantListString = '';
		participantsList.forEach(participantName => participantListString += participantName + '\n');
		
		return participantListString;
	  };

      // function to clear input box and return to initial view
      $scope.resetSearch = function() {
        $scope.query = null;
        $scope.retrieveFeedback(1, 'All Feedback', 'All', 'All', 'All', '');
      };
    });
