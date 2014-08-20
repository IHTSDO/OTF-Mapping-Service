
'use strict';

angular.module('mapProjectApp.widgets.compareRecords', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('compareRecords', {
		title: 'Compare Records',
		description: 'Displays map records for a source concept and highlights differences between the records.',
		controller: 'compareRecordsCtrl',
		templateUrl: 'js/widgets/compareRecords/compareRecords.html',
		edit: {}
	});
})
.controller('compareRecordsCtrl', function($scope, $rootScope, $http, $routeParams, $location, localStorageService, $sce){

	/////////////////////////////////////
	// Map Record Controller Functions //
	/////////////////////////////////////
	
	console.debug("Entering compareRecordsCtrl");

	// initialize scope variables
	$scope.concept = 	null;
	$scope.project = 	localStorageService.get('focusProject');
	$scope.user = 		localStorageService.get('currentUser');
	$scope.role = 		localStorageService.get('currentRole');
	
	// flag for whether this record is a false conflict
	$scope.isFalseConflict = null;  // set to true or false on first visit

	$scope.record1 = 	null;
	$scope.groups1 = 	null;
	$scope.entries1 =    null;

	$scope.record2 = 	null;
	$scope.groups2 = 	null;
	$scope.entries2 =    null;

	$scope.leadRecord = null;

	// initialize accordion variables
	$scope.isConceptOpen = true;
	$scope.isEntriesOpen = true;
	$scope.isPrinciplesOpen = true;
	$scope.isNotesOpen = true;
	$scope.isReportOpen = true;
	$scope.isDiscrepancyOpen = false;
	
	// TODO: needs to be moved to server-side
	$scope.errorMessages = [{displayName: 'Map Group is not relevant'}, 
                            {displayName: 'Map Group  has been omitted'},
                            {displayName: 'Sequencing of Map Groups is incorrect'}, 
                            {displayName: 'The number of map records per group is incorrect'},
                            {displayName: 'Target code selection for a map record is in error'}, 
                            {displayName: 'Map rule type assignment is in error'},
                            {displayName: 'Map target type assignment is in error'}, 
                            {displayName: 'Map advice missing or incomplete'},
                            {displayName: 'Map advice assignment is in error'}, 
                            {displayName: 'Mapping Personnel Handbook principle not followed'},
                            {displayName: 'Gender rule is not relevant'}, 
                            {displayName: 'Gender rule has been omitted'},
                            {displayName: 'Age rule is not relevant'}, 
                            {displayName: 'Age rule has been omitted'},
                            {displayName: 'Other'}
                            ];
	
    $scope.selectedErrorMessage1 = $scope.errorMessages[0];
    $scope.selectedErrorMessage2 = $scope.errorMessages[0];
	
	// watch for project change and modify the local variable if necessary
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
			$scope.project = parameters.focusProject;
	});
	
	// watch for change in focus project
	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch(['project', 'userToken'], function() {
		
		console.debug('compareRecords:  Detected change in project');
		$http.defaults.headers.common.Authorization = $scope.userToken;
		
		// if first visit, retrieve the records to be compared
		if ($scope.leadRecord == null) {
			console.debug("First visit, getting conflict records");
			$scope.getRecordsInConflict();
			
			console.debug("Checking whether this is a false conflict.");
			$http({
				url: root_workflow + "record/id/" + $routeParams.recordId + "/isFalseConflict",
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.isFalseConflict = data === 'true' ? true : false;
			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);    	  
			});
			
			
		
		// otherwise, return to dashboard (mismatch between record and project)
		} else {
			console.debug("Redirecting");
		
			var path = "";
	
			if ($scope.role === "Specialist") {
				path = "/specialist/dash";
			} else if ($scope.role === "Lead") {
				path = "/lead/dash";
			} else if ($scope.role === "Administrator") {
				path = "/admin/dash";
			} else if ($scope.role === "Viewer") {
				path = "/viewer/dash";
			}
			console.debug("redirecting to " + path);
			$location.path(path);
		}
	});

	$scope.getRecordsInConflict = function() {
		
		console.debug("Entered getRecordsInConflict");

		// initialize local variables
		var leadRecordId=		$routeParams.recordId;

		// get the lead record
		$http({
			url: root_mapping + "record/id/" + leadRecordId,
			dataType: "json",
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).success(function(data) {
			$scope.leadRecord = data;
		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
			// obtain the record concept - id from leadRecord	    	  
		}).then(function(data) {
			$http({
				url: root_content + "concept/id/" 
				+ $scope.project.sourceTerminology + "/"
				+ $scope.project.sourceTerminologyVersion + "/"
				+ $scope.leadRecord.conceptId,
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.concept = data;
				setTitle($scope.concept.terminologyId, $scope.concept.defaultPreferredName);
			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);
			});
		});

		// get the conflict records
		$http({
			url: root_mapping + "record/id/" + $routeParams.recordId + "/conflictOrigins",
			dataType: "json",
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).success(function(data) {
			
			if (data.totalCount == 1) {
				$scope.record1 = data.mapRecord[0];
				$scope.record1.displayName = data.mapRecord[0].owner.name;
				$scope.record2 = null;
				
				console.debug("Review record: ", $scope.record1);
				
			}
			else if (data.totalCount == 2) {
				
				// if a conflict, just set the two records
				if (data.mapRecord[0].workflowStatus === 'CONFLICT_DETECTED') {
					// set the origin records (i.e. the records in conflict)
					$scope.record1 = data.mapRecord[0];
					$scope.record1.displayName = $scope.record1.owner.name;
					$scope.record2 = data.mapRecord[1];
					$scope.record2.displayName = $scope.record2.owner.name;
					
				// otherwise a review record
				} else {
					
					// assign the first record as the specialist's revised record
					// assign the second record as the previously published record
					for (var i = 0; i < 2; i++) {
						if (data.mapRecord[i].workflowStatus === 'REVIEW_NEEDED') {
							$scope.record1 = data.mapRecord[i];
							$scope.record1.displayName = $scope.record1.owner.name;
						} else if (data.mapRecord[i].workflowStatus === 'REVISION') {
							$scope.record2 = data.mapRecord[i];
							$scope.record2.displayName = 'Previously Published';
						}
					}
				}
			}
			

		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
		}).then(function(data) {
			
			// get the groups
			if ($scope.project.groupStructure == true)
				getGroups();

			// initialize the entries
			initializeEntries();

			// obtain the validationResults from compareRecords
			$http({
				url: root_mapping + "validation/record/id/" + $scope.record1.id + "/record/id/" + $scope.record2.id + "/compare",
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				for (var i = 0; i < data.errors.length; i++) {				
				  data.errors[i] = data.errors[i].replace("Specialist 1", $scope.record1.owner.name);
				  data.errors[i] = data.errors[i].replace("Specialist 2", $scope.record2.owner.name);
				}
				$scope.validationResult = data;
			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);
			});
		});

		
	};



	///////////////////////////////
	//	Initialization Functions ///
	///////////////////////////////

	//	construct an object containing entries, either:
	//	1) a 1-d array, if project has no group structure
	//	2) a 2-d array, with structure [group][mapPriority]
	function initializeEntries() {
		
		console.debug("initializing entries");

		// INITIALIZE FIRST RECORD

		// calculate rule summaries and assign local id equivalent to hibernate id (needed for track by in ng-repeat)
		for (var i = 0; i < $scope.record1.mapEntry.length; i++) {
			$scope.record1.mapEntry[i].ruleSummary = 
				$scope.getRuleSummary($scope.record1.mapEntry[i]);
			$scope.record1.mapEntry[i].localId = $scope.record1.mapEntry[i].id;

		}

		// if no group structure, simply copy and sort
		if ($scope.project.groupStructure == false) {
			$scope.entries1 = sortByKey($scope.record1.mapEntry, 'mapPriority');

			// otherwise, initialize group arrays
		} else {

			// initialize entry arrays for distribution by group
			$scope.entries1 = new Array(10);

			for (var i=0; i < $scope.entries1.length; i++) $scope.entries1[i] = new Array();

			// cycle over the entries and assign to group bins
			for (var i=0; i < $scope.record1.mapEntry.length; i++) {
				$scope.entries1[$scope.record1.mapEntry[i].mapGroup].push($scope.record1.mapEntry[i]);
			}

			// cycle over group bins and sort contents by map priority
			for (var i=0; i< $scope.entries1.length; i++) {
				$scope.entries1[i] = sortByKey($scope.entries1[i], 'mapPriority');
			}
		}
		
		console.debug($scope.entries1);

		// INITIALIZE SECOND RECORD
		
		if ($scope.record2 != null) {

			// calculate rule summaries and assign local id equivalent to hibernate id (needed for track by in ng-repeat)
			for (var i = 0; i < $scope.record2.mapEntry.length; i++) {
				$scope.record2.mapEntry[i].ruleSummary = 
					$scope.getRuleSummary($scope.record2.mapEntry[i]);
				$scope.record2.mapEntry[i].localId = $scope.record2.mapEntry[i].id;
			}
	
			// if no group structure, simply copy and sort
			if ($scope.project.groupStructure == false) {
				$scope.entries2 = sortByKey($scope.record2.mapEntry, 'mapPriority');
	
				// otherwise, initialize group arrays
			} else {
	
				// initiailize entry arrays for distribution by group
				$scope.entries2 = new Array(10);
	
				for (var i=0; i < $scope.entries2.length; i++) $scope.entries2[i] = new Array();
	
				// cycle over the entries and assign to group bins
				for (var i=0; i < $scope.record2.mapEntry.length; i++) {
					$scope.entries2[$scope.record2.mapEntry[i].mapGroup].push($scope.record2.mapEntry[i]);
				}
	
				// cycle over group bins and sort contents by map priority
				for (var i=0; i< $scope.entries2.length; i++) {
					$scope.entries2[i] = sortByKey($scope.entries2[i], 'mapPriority');
				}
			}
			
			console.debug("entries2");
			console.debug($scope.entries2);
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
		
		var ruleSummary = "";
		
		// first, rule summary
		if ($scope.project.ruleBased == true && entry.rule != null && entry.rule != undefined) {
			if (entry.rule.toUpperCase().indexOf("TRUE") != -1) ruleSummary += "[TRUE] ";
			else if (entry.rule.toUpperCase().indexOf("FEMALE") != -1) ruleSummary += "[FEMALE] ";
			else if (entry.rule.toUpperCase().indexOf("MALE") != -1) ruleSummary += "[MALE] ";
			else if (entry.rule.toUpperCase().indexOf("AGE") != -1) {
						
				
				var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/ );
				var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/ );
				
				console.debug(lowerBound);
				console.debug(upperBound);

				ruleSummary += '[AGE ';
				
				if (lowerBound != null && lowerBound != '' && lowerBound.length > 0) {
					ruleSummary += lowerBound[0];
					if (upperBound != null && upperBound != '' && upperBound.length > 0) ruleSummary += ' AND ';
				}
				if (upperBound != null && upperBound != '' && upperBound.length > 0) ruleSummary += upperBound[0];
				
				ruleSummary += '] ';				
			}
		}
		
		return ruleSummary;
			
	};



/////////////////////////
//	Map Group Functions //
/////////////////////////

//	Retrieves groups from the existing entries
	function getGroups() {

		$scope.groups1 = new Array();
		for (var i = 0; i < $scope.record1.mapEntry.length; i++) {			  

			if ($scope.groups1.indexOf(parseInt($scope.record1.mapEntry[i].mapGroup, 10)) == -1) {
				$scope.groups1.push(parseInt($scope.record1.mapEntry[i].mapGroup, 10));
			};
		};

		// if no groups found, add a default group
		if ($scope.groups1.length == 0) $scope.groups1.push(1);

		$scope.groups2 = new Array();
		
		if ($scope.record2 != null) {
		for (var i = 0; i < $scope.record2.mapEntry.length; i++) {			  

			if ($scope.groups2.indexOf(parseInt($scope.record2.mapEntry[i].mapGroup, 10)) == -1) {
				$scope.groups2.push(parseInt($scope.record2.mapEntry[i].mapGroup, 10));
			};
		};

		// if no groups found, add a default group
		if ($scope.groups2.length == 0) $scope.groups2.push(1);
		}

	};



	///////////////////////
	//	Utility Functions //
	///////////////////////
	
	//	sort and return an array by string key
	function sortByKey(array, key) {
		return array.sort(function(a, b) {
			var x = a[key]; var y = b[key];
			return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
	};

	function setTitle(id, term) {
		if ($scope.record2 == null) {
			$scope.model.title = "Review Record: " + id + " " + term;
		} else {
			$scope.model.title = "Compare Records: " + id + "  " + term;
		}
	};

	$scope.populateMapRecord = function(record) {
		
		var localId = 1;
		
		console.debug('populating map record');
		console.debug(record);
		
		// copy the relevant information into the map lead's record
		$scope.leadRecord.mapEntry = angular.copy(record.mapEntry);
		$scope.leadRecord.mapNotes = angular.copy(record.mapNotes);
		$scope.leadRecord.mapPrinciples = angular.copy(record.mapPrinciples);
		
		// null the ids of the notes (for later creation as new jpa objects)
		for (var i = 0; i < $scope.leadRecord.mapNote.length; i++) {
			$scope.leadRecord.mapNote[i].localId = localId++;
			$scope.leadRecord.mapNote[i].id = null;
		}
		
		// null the ids of all the entries (for later creation as new jpa objects)
		for (var i = 0; i < $scope.leadRecord.mapEntry.length; i++) {
			console.debug("Setting entry localId to " + localId);
			$scope.leadRecord.mapEntry[i].localId = localId++;
			$scope.leadRecord.mapEntry[i].id = null;
			console.debug($scope.leadRecord.mapEntry[i]);
		}
				
		console.debug("Broadcasting record: ", $scope.leadRecord);
		// broadcast to the map record widget
		$rootScope.$broadcast('compareRecordsWidget.notification.selectRecord',{record: $scope.leadRecord});  
		
	};

	$scope.getEntrySummary = function(entry) {
		
		var entrySummary = "" ;
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
			entrySummary += entry.targetId + " " + entry.targetName;
		}
		
		return entrySummary;
		
	};

	$scope.submitNewUserError = function(recordInError, errorMessage, errorComment) {
		   console.debug("in submitNewUserError");
		   
		   if (errorComment == null || errorComment == undefined || errorComment === '') {
			   window.alert("The error comment cannot be blank");
		   	   return;
		   }
		   
			var obj = {
						"note": errorComment,
						"mapError": errorMessage.displayName,
						"timestamp": new Date(),
						"mapUserReporting": $scope.user,
						"mapUserInError": recordInError.owner,
						"mapRecordId": recordInError.id
					  };
			$http({						
				url: root_mapping + "error/add",
				dataType: "json",
				data: obj,
				method: "PUT",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				console.debug("success to addUserError");
			}).error(function(data, status, headers, config) {
				$scope.recordError = "Error adding new user error.";
				$rootScope.handleHttpError(data, status, headers, config);
			});
		};
		
	$scope.startDiscrepancyReview = function(discrepancyNote) {
		console.debug("Sending discrepancy emails", discrepancyNote);
		
		
		
		// construct a base user email object consisting of sender, subject, and base email text
		var baseFeedbackEmailObj = {};
		var feedbackEmailObj = null;
		var recipients = null;
		
		// add the current user as sender
		baseFeedbackEmailObj['sender'] = $scope.user.userName;
		
		// null recipients
		baseFeedbackEmailObj['recipients'] = [];
		
		// set the subject
		baseFeedbackEmailObj['subject'] = "[OTF-Mapping] Discrepancy Review: Project " + $scope.project.name + ", Concept " + $scope.record1.conceptId + " - " + $scope.record1.conceptName;
	
		console.debug("Feedback Email Object:", feedbackEmailObj);
		
		$scope.discrepancyErrorMessage = '';
		$scope.discrepancySuccessMessage = '';
		
		// set the email text
		var emailText = "<strong>Discrepancy Review Notes</strong><br><br>";
		emailText += discrepancyNote + "<br><br>";

		// link to concept record page
		// TODO ADD
		
		emailText += "<hr>";
		
		// set the project information
		emailText += "Project:                 " + $scope.project.name + "<br>";
		emailText += "Source Terminology:      " + $scope.project.sourceTerminology + ", " + $scope.project.sourceTerminologyVersion  + "<br>";
		emailText += "Destination Terminology: " + $scope.project.destinationTerminology + ", " + $scope.project.destinationTerminologyVersion  + "<br>";
		emailText += "<br>";
		
		// set the concept information
		emailText += "Concept Id:              " + $scope.record1.conceptId + "<br>";
		emailText += "Concept Name:            " + $scope.record1.conceptName + "<br>";
		
		emailText += "<hr>";
		
		// add the current user as sender
		baseFeedbackEmailObj['emailText'] = emailText;
		feedbackEmailObj = angular.copy(baseFeedbackEmailObj);
		
		recipients = new Array();
		recipients.push($scope.user.userName);
		feedbackEmailObj['recipients'] = recipients;
		
		// add both records
		feedbackEmailObj['emailText'] += recordToText($scope.record1);
		feedbackEmailObj['emailText'] += "<hr>";
		feedbackEmailObj['emailText'] += recordToText($scope.record2);
		feedbackEmailObj['emailText'] += "<hr>";
		
		// add list of discrepancies
		feedbackEmailObj['emailText'] += "<strong>Conflict Report</strong><br><br>";
		
		if ($scope.validationResult.errors.length > 0) {
			feedbackEmailObj['emailText'] += "Errors<br>";
			feedbackEmailObj['emailText'] += "<ul>";
			for (var i = 0; i < $scope.validationResult.errors.length; i++) {
				feedbackEmailObj['emailText'] += "<li>" + $scope.validationResult.errors[i] + "</li>";
			}
			feedbackEmailObj['emailText'] += "</ul>";
		}
		
		if ($scope.validationResult.warnings.length > 0) {
			feedbackEmailObj['emailText'] += "Warnings<br>";
			feedbackEmailObj['emailText'] += "<ul>";
			for (var i = 0; i < $scope.validationResult.warnings.length; i++) {
				feedbackEmailObj['emailText'] += "<li>" + $scope.validationResult.warnings[i] + "</li>";
			}
			feedbackEmailObj['emailText'] += "</ul>";
		}
		
		// make the api call
		$rootScope.glassPane++;
		$http({
			url: root_workflow + "project/id/" + $scope.project.id + "/user/id/" + $scope.user.userName + "/sendFeedback",
			dataType: "json",
			data: feedbackEmailObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$rootScope.glassPane--;
			$scope.discrepancySuccessMessage += "Email successfully sent to " + $scope.user.name + ". \n";
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		    $scope.discrepancyErrorMessage += "Failed to send email to " + $scope.user.name + ". \n";
		});
		
		// send email to owner of first record
		feedbackEmailObj = angular.copy(baseFeedbackEmailObj);
		
		// add first records
		feedbackEmailObj['emailText'] += recordToText($scope.record1);
		feedbackEmailObj['emailText'] += "<hr>";
		
		recipients = new Array();
		recipients.push($scope.record1.owner.userName);
		feedbackEmailObj['recipients'] = recipients;
		
		// make the api call
		$rootScope.glassPane++;
		$http({
			url: root_workflow + "project/id/" + $scope.project.id + "/user/id/" + $scope.user.userName + "/sendFeedback",
			dataType: "json",
			data: feedbackEmailObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			$rootScope.glassPane--;
			$scope.discrepancySuccessMessage += "Email successfully sent to " + $scope.record1.owner.name + ". \n";
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		    $scope.discrepancyErrorMessage += "Failed to send email to " + $scope.record1.owner.name + ". \n";
		});
		
		// send email to owner of second record, if it exists
		if ($scope.record2 != null) {
			feedbackEmailObj = angular.copy(baseFeedbackEmailObj);
			
			recipients = new Array();
			recipients.push($scope.record2.owner.userName);
			feedbackEmailObj['recipients'] = recipients;
			
			// add first records
			feedbackEmailObj['emailText'] += recordToText($scope.record2);
			feedbackEmailObj['emailText'] += "<hr>";
			
			// make the api call
			$rootScope.glassPane++;
			$http({
				url: root_workflow + "project/id/" + $scope.project.id + "/user/id/" + $scope.user.userName + "/sendFeedback",
				dataType: "json",
				data: feedbackEmailObj,
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				$rootScope.glassPane--;
				$scope.discrepancySuccessMessage += "Email successfully sent to " + $scope.record1.owner.name + "\n";
			}).error(function(data, status, headers, config) {
				$rootScope.glassPane--;
			    $rootScope.handleHttpError(data, status, headers, config);
			    $scope.discrepancyErrorMessage += "Failed to send email to " + $scope.record1.owner.name + "\n";
			});
		}
	};
	
	function recordToText(record) {
		
		var recordText = '';
		var entries = getEntriesByGroup(record); // ensure the entries are sorted correctly
		
		recordText += "Record " + record.id + "<br>";
		recordText += "Owner: " + record.owner.name + "<br><br>";
		
		// for each group
		// NOTE:  group 0 is by definition empty
		for (var i = 1; i < entries.length; i++) {
			
			var groupEntries = entries[i];
			
			recordText += "Group "+ i + "<br>";
			
			// for each entry in group

			for (var j = 0; j < groupEntries.length; j++) {
				
				var entry = groupEntries[j];
				console.debug("adding entry: ", entry);
			
				recordText += " (" + j + ") " + entry.targetId + " - " + entry.targetName + "<br>";
				if ($scope.project.ruleBased == true)
					recordText += "      RULE:     " + entry.rule + "<br>";
				if (entry.mapRelation != null) 
					recordText += "      RELATION: " + entry.mapRelation.name + "<br>";
				if (entry.mapAdvice.length != 0) {
					recordText += "      ADVICES:  ";
					
					for (var k = 0; k < entry.mapAdvice.length; k++) {
						if (k>0)
							recordText += "                ";
						recordText += entry.mapAdvice[k].name + "<br>";	
					};
				}
			};
		};
		
		recordText += "<br>";
		
		// add the principles
		if (record.mapPrinciple.length > 0) {
			
			recordText += "Map Principles Used:" + "<br>";
			
			for (var i = 0; i < record.mapPrinciple.length; i++) {
				recordText += "  " + record.mapPrinciple[i].principleId + ": " + record.mapPrinciple[i].name + "<br>";
			}
			
			recordText += "<br>";
		}
		
		// add the notes
		if (record.mapNote.length > 0) {
			
			recordText += "Notes:" + "<br>";
			
			for (var i = 0; i < record.mapNote.length; i++) {
				recordText += "   [" + record.mapNote[i].user.userName + "] " + record.mapNote[i].note + "<br>";
			}
			
			recordText += "<br>";
		}
		
		// check if flagged for map lead
		if (record.flagForMapLeadReview == true) {
			recordText += "<strong>Flagged for Map Lead Review</strong>" + "<br>";
		}
		
		console.debug(recordText);

		return recordText;
	};
	
	function getEntriesByGroup(record) {

		console.debug("Initializing map entries -- " + record.mapEntry.length + " found");
		console.debug(record.mapEntry);
		
		var entries = new Array();
		entries.push(new Array()); // zeroth group is left empty

		// if no group structure, simply copy and sort
		if ($scope.project.groupStructure == false) {
			entries.push(sortByKey(record.mapEntry, 'mapPriority'));

			// otherwise, initialize group arrays
		} else {

			console.debug("Initializing entries");
			
			// get the total number of groups
			var maxGroup = 1;  // default
			for (var i = 0; i < record.mapEntry.length; i++) {
				if (record.mapEntry[i].mapGroup > maxGroup) 
					maxGroup = record.mapEntry[i].mapGroup;
			}
			
			// initialize the group/entry array
			entries = new Array(maxGroup);
			for (var i = 0; i <= maxGroup; i++)
				entries[i] = new Array();
			
			console.debug("Existing entries: ");

			// cycle over the entries and assign to group bins
			for (var i=0; i < record.mapEntry.length; i++) {
				entries[record.mapEntry[i].mapGroup].push(record.mapEntry[i]);
			}

			// cycle over group bins and sort contents by map priority
			for (var i=0; i< entries.length; i++) {
				entries[i] = sortByKey(entries[i], 'mapPriority');
			}
		}
		
		return entries;
	};
	
	$scope.toggleFalseConflict = function() {
		
		$http({						
			url: root_workflow + "record/id/" + $routeParams.recordId + "/falseConflict/" + ($scope.isFalseConflict == true ? "false" : "true"),
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			// on success, flip the boolean isFalseConflict
			console.debug("success in toggling false conflict, previous = ", $scope.isFalseConflict);
			$scope.isFalseConflict = ! $scope.isFalseConflict;
			console.debug("New = ", $scope.isFalseConflict);
			
			// if record marked in conflict, broadcast the first record
			$scope.populateMapRecord($scope.record1);
			
		}).error(function(data, status, headers, config) {
			$scope.recordError = "Error setting false conflict.";
			$rootScope.handleHttpError(data, status, headers, config);
		});
		
	}

	
	// function to return trusted html code (for advice content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};

});
