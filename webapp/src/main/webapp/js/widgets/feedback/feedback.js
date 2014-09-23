
'use strict';

angular.module('mapProjectApp.widgets.feedback', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('feedback', {
		title: 'Feedback',
		description: 'Displays a list of feedback notes',
		controller: 'feedbackCtrl',
		templateUrl: 'js/widgets/feedback/feedback.html',
		edit: {}
	});
}).controller('feedbackCtrl', function($scope, $rootScope, $http, $location, $modal, $sce, localStorageService){
    $scope.currentUser = null;
	$scope.currentRole = null;
	$scope.focusProject = null;
	$scope.feedbackConversations = null;
	
	// initialize as empty to indicate still initializing database connection
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentUserToken = localStorageService.get('userToken');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');
	
    
	// table sort fields
	$scope.tableFields = [ {id: 0, title: 'id', sortDir: 'asc', sortOn: false}];
	
	$scope.mapUserViewed == null;
	$scope.searchPerformed = false;  		// initialize variable to track whether search was performed

	
	// pagination variables
	$scope.recordsPerPage = 10;
	$scope.recordPage = 1;
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	
	



	// on any change of focusProject, retrieve new available work

	$scope.$watch(['focusProject', 'currentUser', 'currentUserToken', 'currentRole'], function() {
		
		if ($scope.focusProject != null && $scope.currentUser != null && $scope.currentUserToken != null
				&& $scope.currentRole != null) {
			$http.defaults.headers.common.Authorization = $scope.currentUserToken;			
			$scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
			$scope.retrieveFeedback(1);
		}
	});
	

    $scope.retrieveFeedback = function(page) {
    	
    	if ($scope.currentRole == 'Viewer')
  		  return;
    	
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.recordsPerPage,
			 	 	 "maxResults": $scope.recordsPerPage,
			 	 	 "sortField":  null,
			 	 	 "queryRestriction": $scope.query == null ? null : $scope.query};  
	
	  	$rootScope.glassPane++;
	
		$http({
			url: root_workflow + "conversation/project/id/" + $scope.focusProject.id + "/" + $scope.currentUser.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;
			
			// set pagination variables
			$scope.nRecords = data.totalCount;
			$scope.numRecordPages = Math.ceil(data.totalCount / $scope.recordsPerPage);
	
			$scope.feedbackConversations = data.feedbackConversation;
			console.debug("Feedback Conversations:");
			console.debug($scope.feedbackConversations);
						 
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});

    };

	
	$scope.isFeedbackViewed = function(conversation) {
    	for (var i = 0; i < conversation.feedback.length; i++) {
    		var alreadyViewedBy =  conversation.feedback[i].viewedBy;
    		var found = false;
    		for (var j = 0; j < alreadyViewedBy.length; j++) {
    			if (alreadyViewedBy[j].userName == $scope.currentUser.userName)
    				found = true;
    		}	
    		if (found == false)
    			return false;
    	}
    	return true;
	};
	
	$scope.goFeedbackConversations = function (id) {
		var path = "/conversation/recordId/" + id;
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
		$scope.retrieveFeedback(1);
	};
});
