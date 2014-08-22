'use strict';

angular.module('mapProjectApp.widgets.feedbackConversation', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('feedbackConversation', {
		title: 'Feedback Conversation',
		description: 'Displays a Feedback Conversation',
		controller: 'feedbackConversationCtrl',
		templateUrl: 'js/widgets/feedbackConversation/feedbackConversation.html',
		edit: {}
	});
}).controller('feedbackConversationCtrl', function($scope, $rootScope, $routeParams, $http, $location, $modal, $sce, localStorageService){

	// initialize as empty to indicate still initializing database connection
	$scope.currentUser = localStorageService.get('currentUser');
	$scope.currentRole = localStorageService.get('currentRole');
	$scope.focusProject = localStorageService.get('focusProject');
	
    $scope.conversation = null;
    
    $scope.recordId = $routeParams.recordId;
		

	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("MapProjectWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	
	
	// on any change of focusProject, retrieve new available work
	$scope.currentUserToken = localStorageService.get('userToken');
	$scope.$watch(['focusProject', 'user', 'userToken'], function() {
		console.debug('feedbackConversationCtrl:  Detected project or user set/change');

		if ($scope.focusProject != null && $scope.currentUser != null && $scope.currentUserToken != null) {
			$http.defaults.headers.common.Authorization = $scope.currentUserToken;				
			$scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);			
		}
	});
 
	// get feedback conversation associated with given recordId
  	$rootScope.glassPane++;
	$http({
		url: root_workflow + "conversation/id/" + $scope.recordId,
		dataType: "json",
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}
	}).success(function(data) {
	  	$rootScope.glassPane--;		
		$scope.conversation = data;
		console.debug("Feedback Conversation:");
		console.debug($scope.conversation);
		$scope.markViewed($scope.conversation, $scope.currentUser);

		$scope.record = null;
		// load record associated with feedback conversations
		$rootScope.glassPane++;

		// load record to be displayed
		$http({
			url: root_mapping + "record/id/" + $scope.conversation.mapRecordId + "/historical",
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$rootScope.glassPane--;	
			$scope.record = data;
			console.debug("Record:");
			console.debug($scope.record);
			setTitle();
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
			$rootScope.handleHttpError(data, status, headers, config);
		});
		
	}).error(function(data, status, headers, config) {
	    $rootScope.glassPane--;
	    $rootScope.handleHttpError(data, status, headers, config);
	});

	
	// function to return trusted html code (for advice content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};	

	// update the title to contain conceptId and preferred name
	function setTitle() {
		$scope.model.title = "Feedback Conversation: " + $scope.conversation.terminologyId + "  " 
		+ $scope.conversation.defaultPreferredName;
	};
	
	// options for the rich text feedback input field
	$scope.tinymceOptions = {			
			menubar : false,
			statusbar : false,
			plugins : "autolink autoresize link image charmap searchreplace",
			toolbar : "undo redo | styleselect | bold italic underline strikethrough | charmap link image",
	};

	// send feedback on already started conversation
	$scope.sendFeedback = function(record, feedbackMessage, conversation) {
		console.debug("Sending feedback email", record);
		
		   if (feedbackMessage == null || feedbackMessage == undefined || feedbackMessage === '') {
			   window.alert("The feedback field cannot be blank. ");
		   	   return;
		   }
		   // figure out the return recipients based on previous feedback in conversation
			var localFeedback = conversation.feedback;
			var localSender = localFeedback[localFeedback.length -1].sender;
			var localRecipients = localFeedback[localFeedback.length -1].recipients;
			var returnRecipients = new Array();
			if (localSender.userName == $scope.currentUser.userName)
				returnRecipients = localRecipients;
			else {
				returnRecipients.push(localSender);
				for (var i = 0; i < localRecipients.length; i++) {
					if (localRecipients[i].userName != $scope.currentUser.userName)
					  returnRecipients.push(localRecipients[i]);
				}
			}
				
			// create feedback msg to be added to the conversation
			var feedback = {
					"message": feedbackMessage,
					"mapError": "",
					"timestamp": new Date(),
					"sender": $scope.currentUser,
					"recipients": returnRecipients,
					"isError": "true",
					"viewedBy": [$scope.currentUser]
			};
			
			localFeedback.push(feedback);
			conversation.feedback = localFeedback;
				
			$http({						
				url: root_workflow + "conversation/update",
				dataType: "json",
				data: conversation,
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				console.debug("success to update Feedback conversation.");
			}).error(function(data, status, headers, config) {
				$scope.recordError = "Error updating feedback conversation.";
				$rootScope.handleHttpError(data, status, headers, config);
			});
		   
	};
	
    // add current user to list of viewers who have seen the feedback conversation
    $scope.markViewed = function(conversation, user) {
    	var needToUpdate = false;
    	for (var i = 0; i < conversation.feedback.length; i++) {
    		var alreadyViewedBy =  conversation.feedback[i].viewedBy;
    		var found = false;
    		for (var j = 0; j<alreadyViewedBy.length; j++) {
    			if (alreadyViewedBy[j].userName == user.userName)
    				found = true;
    		}	
        	if (found == false) {
      		  alreadyViewedBy.push(user);
      		  needToUpdate = true;
        	}
    	}
    	
    	if (needToUpdate == true) {
		  $http({						
				url: root_workflow + "conversation/update",
				dataType: "json",
				data: conversation,
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				console.debug("success to update Feedback conversation.");
			}).error(function(data, status, headers, config) {
				$scope.recordError = "Error updating feedback conversation.";
				$rootScope.handleHttpError(data, status, headers, config);
			});
    	}
    };
    
	$scope.getBrowserUrl = function() {
		return "http://dailybuild.ihtsdotools.org/index.html?perspective=full&conceptId1=" + $scope.conversation.terminologyId + "&diagrammingMarkupEnabled=true&acceptLicense=true";
	};

    $scope.openConceptBrowser = function() {
    	window.open($scope.getBrowserUrl(), "browserWindow");
    };
    
	$scope.goEdit = function (record) {
		if (record.workflowStatus == 'CONFLICT_NEW') {
			var path = "/record/conflicts/" + record.id;
			// redirect page
			$location.path(path);			
		} else {
		  var path = "/record/recordId/" + record.id;
			// redirect page
			$location.path(path);
		}
	};

	$scope.goConceptView = function (id) {
		var path = "/record/conceptId/" + id;
			// redirect page
			$location.path(path);
	};
	
	$scope.displayEdit = function () {
		if ($scope.currentUser.userName == $scope.record.owner.userName)
			return true;
		else
			return false;
	};
	
});