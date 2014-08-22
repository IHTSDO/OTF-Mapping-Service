
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

	// initialize as empty to indicate still initializing database connection
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');
	
    $scope.feedbackConversations = null;
	
	// table sort fields
	$scope.tableFields = [ {id: 0, title: 'id', sortDir: 'asc', sortOn: false}];
	
	$scope.mapUserViewed == null;
	$scope.searchPerformed = false;  		// initialize variable to track whether search was performed

	
	// pagination variables
	$scope.itemsPerPage = 10;
	$scope.assignedWorkPage = 1;
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	
	



	// on any change of focusProject, retrieve new available work
	$scope.currentUserToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'user', 'userToken'], function() {
		console.debug('feedbackCtrl:  Detected project or user set/change');
		if ($scope.focusProject != null && $scope.currentUser != null && $scope.currentUserToken != null) {
			$http.defaults.headers.common.Authorization = $scope.currentUserToken;			
			$scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);			
		}
	});
	

	// construct a paging/filtering/sorting object
	var pfsParameterObj = 
				{/*"startIndex": (page-1)*$scope.recordsPerPage,
		 	 	 "maxResults": $scope.recordsPerPage, */
				 "startIndex": 0,
				 "maxResults": 10,
		 	 	 "sortField":  null,
		 	 	 "queryRestriction": null};  

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
		
		/*$scope.recordPage = page;
		$scope.nRecords = data.totalCount;
		$scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
		 */
		$scope.feedbackConversations = data.feedbackConversation;
		console.debug("Feedback Conversations:");
		console.debug($scope.feedbackConversations);
					 
	}).error(function(data, status, headers, config) {
	    $rootScope.glassPane--;
	    $rootScope.handleHttpError(data, status, headers, config);
	});

	

	
	// remove an element from an array by key
	Array.prototype.removeElement = function(elem) {

		// field to switch on
		var idType = 'id';

		var array = new Array();
		$.map(this, function(v,i){
			if (v[idType] != elem[idType]) array.push(v);
		});

		this.length = 0; //clear original array
		this.push.apply(this, array); //push all elements except the one we want to delete
	};
	
	
	// sort and return an array by string key
	function sortByKey(array, key) {
		return array.sort(function(a, b) {
			var x = a[key]; var y = b[key];
			return ((x < y) ? -1 : ((x > y) ? 1 : 0));
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
	
	$scope.retrieveFeedback = function(page, query) {
		
		console.debug('Retrieving Feedback: ', page, query);

		// ensure query is set to null if undefined
		if (query == undefined) query = null;
		
		// reset the search input box if null
		if (query == null) {
			$scope.searchPerformed = false;
		} else {
			$scope.searchPerformed = true;
		
		}
		
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": page == -1 ? -1 : (page-1)*$scope.itemsPerPage,
			 	 	 "maxResults": page == -1 ? -1 : $scope.itemsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "queryRestriction": null};

	 /* 	$rootScope.glassPane++;

		$http({
			url: root_workflow + "project/id/" 
			+ $scope.focusProject.id 
			+ "/user/id/" 
			+ $scope.currentUser.userName 
			+ "/query/" + (query == null ? null : query)
			+ "/assignedConcepts",
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.assignedWorkPage = page;
			$scope.assignedRecords = data.searchResult;
			console.debug($scope.assignedRecords);
		
			// set pagination
			$scope.numAssignedRecordPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
			$scope.nAssignedRecords = data.totalCount;
			
			// set title
			$scope.tabs[0].title = "Concepts (" + $scope.nAssignedRecords + ")";
			console.debug($scope.nAssignedRecords);
			console.debug(data.totalCount);
			console.debug($scope.assignedWorkTitle);
			
			
		}).error(function(data, status, headers, config) {
		  	$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});*/
	};
});
