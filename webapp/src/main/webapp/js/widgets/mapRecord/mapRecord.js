'use strict';

angular.module('mapProjectApp.widgets.mapRecord', ['adf.provider'])
.config(function(dashboardProvider){ 

	dashboardProvider
	.widget('mapRecord', {
		title: 'Map Record',
		description: 'Edit module for a map record',
		templateUrl: 'js/widgets/mapRecord/mapRecord.html',
		controller: 'mapRecordWidgetCtrl',
		resolve: {},
		edit: {}   
	});
})

.controller('mapRecordWidgetCtrl', function($scope, $rootScope, $http, $routeParams, $location, $sce, $modal, $window, localStorageService){

	/////////////////////////////////////
	// Map Record Controller Functions //
	/////////////////////////////////////

	// this controller handles a potentially "dirty" page
	$rootScope.currentPageDirty = true;
	
	// initialize scope variables
	$scope.record = 	null;
	$scope.project = 	localStorageService.get('focusProject');
	$scope.concept = 	null;
	$scope.groups = 	null;
	$scope.entries =    null;
	$scope.user = 		localStorageService.get('currentUser');
	$scope.role = 		localStorageService.get('currentRole');
	$scope.userToken = 	localStorageService.get('userToken');
	$scope.conversation = null;
	$scope.mapLeads = $scope.project.mapLead;
	organizeUsers($scope.mapLeads);
	
	$scope.returnRecipients = new Array();
	$scope.multiSelectSettings = {displayProp: 'name', scrollableHeight: '50px',
		    scrollable: true, showCheckAll: false, showUncheckAll: false};
	$scope.multiSelectCustomTexts = {buttonDefaultText: 'Select Leads'};
	
	// validation result storage variable
	$scope.savedValidationWarnings = [];

	// initialize accordion variables
	$scope.isConceptOpen = true;
	$scope.isEntriesOpen = true;
	$scope.isPrinciplesOpen = false;
	$scope.isNotesOpen = false;
	$scope.isFlagsOpen = false;
	$scope.groupOpen = new Array(10);
	for (var i = 0; i < $scope.groupOpen.length; i++) 
		$scope.groupOpen[i] = true;

	// start note edit mode in off mode
	$scope.noteEditMode = false;
	$scope.noteEditId = null;
	$scope.noteInput = '';
	
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
	$rootScope.$on('compareRecordsWidget.notification.selectRecord', function(event, parameters) {    
        console.debug("received new record");
        console.debug(parameters);
        console.debug(parameters.record);
        $scope.record = parameters.record;
        
        console.debug("Passed record: ", parameters.record);
        
        //get the groups
        if ($scope.project.groupStructure == true)
               getGroups();
        
		// This MUST not be removed for "Start here" to work
		initializeEntries();
		

	});

	// on successful retrieval of project, get the record/concept
	$scope.$watch(['project', 'userToken', 'role', 'user', 'record'], function() {
		if ($scope.project != null && $scope.userToken != null) {
			$http.defaults.headers.common.Authorization = $scope.userToken;
			retrieveRecord();
		}
	});
	
	// any time the record changes, broadcast it to the record summary widget
	$scope.$watch('record', function() {
		
		broadcastRecord();
		
	});

	function broadcastRecord() {
		console.debug("Broadcasting record", $scope.getFormattedRecord($scope.record));
		$rootScope.$broadcast('mapRecordWidget.notification.recordChanged',
				{record: angular.copy($scope.getFormattedRecord($scope.record)), project:$scope.project
			
				});  
	}

	// initialize local variables
	var currentLocalId = 1;   // used for addition of new entries without hibernate id
	
	// function to initially retrieve the project

	function retrieveRecord() {
		// obtain the record
		$http({
			url: root_mapping + "record/id/" + $routeParams.recordId,
			dataType: "json",
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).success(function(data) {
			$scope.record = data;
	
		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
		}).then(function() {
	
			// obtain the record concept
			$http({
				url: root_content + "concept/id/" 
				+ $scope.project.sourceTerminology + "/"
				+ $scope.project.sourceTerminologyVersion + "/"
				+ $scope.record.conceptId,
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.concept = data;
				$scope.conceptBrowserUrl = $scope.getBrowserUrl();
			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);
			});
	
	
			// get the groups
			if ($scope.project.groupStructure == true)
				getGroups();
	
			// initialize the entries
			initializeEntries();
			
			//add code to get feedback conversations
			$http({
				url: root_workflow + "conversation/id/" + $scope.record.id,
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.conversation = data;
				initializeReturnRecipients();
			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);  
			});		
		});
	};


	///////////////////////////////
	// Initialization Functions ///
	///////////////////////////////

	// construct an object containing entries, either:
	// 1) a 1-d array, if project has no group structure
	// 2) a 2-d array, with structure [group][mapPriority]
	function initializeEntries() {

		console.debug("Initializing map entries -- " + $scope.record.mapEntry.length + " found");
		console.debug($scope.record.mapEntry);
		
		$scope.entries = new Array();

		// find the maximum hibernate id value
		for (var i = 0; i < $scope.record.mapEntry.length; i++) {
			currentLocalId = Math.max($scope.record.mapEntry[i].id, currentLocalId);
		}

		// calculate rule summaries and assign local id equivalent to hibernate id (needed for track by in ng-repeat)
		for (var i = 0; i < $scope.record.mapEntry.length; i++) {
			$scope.record.mapEntry[i].ruleSummary = 
				$scope.getRuleSummary($scope.record.mapEntry[i]);
			
			// if the hibernate id is defined, set the local id to that value
			if ($scope.record.mapEntry[i].id != null) {
				console.debug("Setting local id to existing hibernate id", $scope.record.mapEntry[i].id);
				$scope.record.mapEntry[i].localId = $scope.record.mapEntry[i].id;
			} else {
				console.debug("Setting local id to " + currentLocalId);
				$scope.record.mapEntry[i].localId = currentLocalId++;
			}
		}

		console.debug("Local ids set: ", $scope.record);
		// if no group structure, simply copy and sort
		if ($scope.project.groupStructure == false) {
			$scope.push(sortByKey($scope.record.mapEntry, 'mapPriority'));

			// otherwise, initialize group arrays
		} else {

			console.debug("Initializing entries");
			
			// get the total number of groups
			var maxGroup = 1;  // default
			for (var i = 0; i < $scope.record.mapEntry.length; i++) {
				if ($scope.record.mapEntry[i].mapGroup > maxGroup) 
					maxGroup = $scope.record.mapEntry[i].mapGroup;
			}
			
			// initialize the group/entry array
			$scope.entries = new Array(maxGroup);
			for (var i = 0; i <= maxGroup; i++)
				$scope.entries[i] = new Array();
			
			console.debug("Existing entries: ");

			// cycle over the entries and assign to group bins
			for (var i=0; i < $scope.record.mapEntry.length; i++) {
				$scope.entries[$scope.record.mapEntry[i].mapGroup].push($scope.record.mapEntry[i]);
			}

			// cycle over group bins and sort contents by map priority
			for (var i=0; i< $scope.entries.length; i++) {
				$scope.entries[i] = sortByKey($scope.entries[i], 'mapPriority');
			}
		}
		
		console.debug("Checking for empty record, current entries = ", $scope.entries);

		// if no entries on this record, assume new and create an entry
		if ($scope.record.mapEntry.length == 0) {
			console.debug("Adding map entry to blank group", $scope.entries[i]);
			$scope.addMapEntry($scope.entries[1]);
			
		// otherwise, select the first entry
		} else {
			$scope.selectMapEntry($scope.record.mapEntry[0]);
		}
		
		console.debug("Completed initializing entries, new entries", $scope.entries);
		
		

	}

	/**
	 * MAP RECORD FUNCTIONS
	 */
	
	$scope.finishMapRecord = function(returnBack) {
		
		// check that note box does not contain unsaved material
		if ($scope.tinymceContent != '' && $scope.tinymceContent != null) {
			if(confirm("You have unsaved text into the Map Notes. Do you wish to continue saving? The note will be lost.") == false) {
				return;
			};
		}
	
		///////////////////////////
		// Group and MapPriority //
		///////////////////////////

		// if not group structured project
		if ($scope.project.groupStructure == false) {

			// cycle over entries and assign map priority based on position
			for (var i = 0; i < $scope.entries.length; i++) {
				$scope.entries[i].mapPriority = i+1;
			}

			$scope.record.mapEntry = $scope.entries;

			// if group structured project
		} else {

			var entries = new Array();

			// cycle over each group bin
			for (var i = 0; i < $scope.entries.length; i++) {

				// cycle over entries in each group bin
				for (var j = 0; j < $scope.entries[i].length; j++) {

					console.debug($scope.entries[i][j]);
					console.debug("Assigning group and priority to " + i + " " + j);
					$scope.entries[i][j].mapGroup = i;
					$scope.entries[i][j].mapPriority = j+1;

					entries.push($scope.entries[i][j]);

				}
			}

			console.debug("finish modified:");
			console.debug(entries);

			$scope.record.mapEntry = entries;
			
			
		}

		console.debug("Validating the map record.");
		// validate the record
		$http({
			url: root_mapping + "validation/record/validate",
			dataType: "json",
			data: $scope.record,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			console.debug("validation results:");
			console.debug(data);
			$scope.validationResult = data;
		}).error(function(data, status, headers, config) {
			$scope.validationResult = null;
			$scope.recordError = "Unexpected error reported by server.  Contact an admin.";
			console.debug("Failed to validate map record");
			$rootScope.handleHttpError(data, status, headers, config);
		}).then(function(data) {

			// if no error messages were returned, stop and display
			if ($scope.validationResult.errors.length == 0)  {
				
				var warningCheckPassed = true;
				
				// if warnings found, check if this is a second click
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
						warningCheckPass = false;
					}
				}
				
				// if the warning checks are passed, save the record
				if (warningCheckPassed == true) {
					
					// assign the current user to the lastModifiedBy field
					$scope.record.lastModifiedBy = $scope.user;
	
					$http({
						url: root_workflow + "finish",
						dataType: "json",
						data: $scope.record,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.recordSuccess = "Record saved.";
						$scope.recordError = "";
						
						// user has successfully finished record, page is no longer "dirty"
						$rootScope.currentPageDirty = false;
						
						if (!returnBack) {
							console.debug("************* ReturnBack is false");

							$rootScope.glassPane++;
							
							// if specialist level work, query for assigned concepts
							if ($scope.record.workflowStatus === 'NEW' 
								|| $scope.record.workflowStatus === 'EDITING_IN_PROGRESS' 
								|| $scope.record.workflowStatus === 'EDITING_DONE') {
								
								// construct a paging/filtering/sorting object
								var pfsParameterObj = 
								{"startIndex": 0,
										"maxResults": 1, 
										"sortField": 'sortKey',
										"queryRestriction": 'NEW'};  

								// get the assigned work list
								$http({
									url: root_workflow + "project/id/" + $scope.project.id 
									+ "/user/id/" + $scope.user.userName
									+ "/query/null/assignedConcepts",
									
									dataType: "json",
									data: pfsParameterObj,
									method: "POST",
									headers: {
										"Content-Type": "application/json"
									}
								}).success(function(data) {
									$rootScope.glassPane--;
	
									var assignedWork = data.searchResult;
									
									// if there is no more assigned work, return to dashboard
									if (assignedWork.length == 0) {
										console.debug("No more assigned work, return to dashboard");
										$location.path($scope.role + "/dash");
									
									// otherwise redirect to the next record to be edited
									} else {
										console.debug("More work, redirecting");
										$location.path("record/recordId/" + assignedWork[0].id);
									}
	
								}).error(function(data, status, headers, config) {
								    $rootScope.glassPane--;
								    $rootScope.handleHttpError(data, status, headers, config);
								});
								
							// otherwise, if a conflict record, query available conflicts
							} else if ($scope.record.workflowStatus === 'CONFLICT_NEW' || $scope.record.workflowStatus === 'CONFLICT_IN_PROGRESS') {
								
								// construct a paging/filtering/sorting object
								var pfsParameterObj = 
								{"startIndex": 0,
										"maxResults": 1, 
										"sortField": 'sortKey',
										"queryRestriction": 'CONFLICT_NEW'};  
								
								// get the assigned conflicts
								$http({
									url: root_workflow + "project/id/" + $scope.project.id 
									+ "/user/id/" + $scope.user.userName
									+ "/query/null/assignedConflicts",
									
									dataType: "json",
									data: pfsParameterObj,
									method: "POST",
									headers: {
										"Content-Type": "application/json"
									}
								}).success(function(data) {
									$rootScope.glassPane--;
	
									var assignedWork = data.searchResult;
									
									// if there is no more assigned work, return to dashboard
									if (assignedWork.length == 0) {
										console.debug("No more assigned conflicts, return to dashboard");
										$location.path($scope.role + "/dash");
									
									// otherwise redirect to the next record to be edited
									} else {
										console.debug("More work, redirecting");
										$location.path("record/conflicts/" + assignedWork[0].id);
									}
	
								}).error(function(data, status, headers, config) {
								    $rootScope.glassPane--;
								    $rootScope.handleHttpError(data, status, headers, config);
								});
								
							// otherwise, if a review record, query available review work
							} else if ($scope.record.workflowStatus === 'REVIEW_NEW' || $scope.record.workflowStatus === 'REVIEW_IN_PROGRESS') {
								
								// construct a paging/filtering/sorting object
								var pfsParameterObj = 
								{"startIndex": 0,
										"maxResults": 1, 
										"sortField": 'sortKey',
										"queryRestriction": 'REVIEW_NEW'}
								;  
								// get the assigned review work
								$http({
									url: root_workflow + "project/id/" + $scope.project.id 
									+ "/user/id/" + $scope.user.userName
									+ "/query/null/assignedReviewWork",
									
									dataType: "json",
									data: pfsParameterObj,
									method: "POST",
									headers: {
										"Content-Type": "application/json"
									}
								}).success(function(data) {
									$rootScope.glassPane--;
	
									var assignedWork = data.searchResult;
									
									// if there is no more assigned work, return to dashboard
									if (assignedWork.length == 0) {
										console.debug("No more assigned review work, return to dashboard");
										$location.path($scope.role + "/dash");
									
									// otherwise redirect to the next record to be edited
									} else {
										console.debug("More work, redirecting");
										$location.path("record/review/" + assignedWork[0].id);
									}
	
								}).error(function(data, status, headers, config) {
								    $rootScope.glassPane--;
								    $rootScope.handleHttpError(data, status, headers, config);
								});
							} else {
								$rootScope.glassPane--;
								console.debug("MapRecord finish/next can't determine type of work, returning to dashboard");
								$location.path($scope.role + "/dash");
							}

						} else {
							console.debug("Simple finish called, return to dashboard");
							$location.path($scope.role + "/dash");
						}
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Unexpected server error.  Try saving your work for later, and contact an admin.";
					    $rootScope.handleHttpError(data, status, headers, config);
						console.debug('SERVER ERROR');
						$scope.recordSuccess = "";
					});
				
				// if the warning checks were not passed, save the warnings
				} else {
					$scope.savedValidationWarnings = $scope.validationResult.warnings;
				}

				
			// if errors found, clear the recordSuccess field
			}  else {
				$scope.recordSuccess = "";
			}

		});
	};
	
	$scope.clearMapRecord = function() {
		$scope.groups = new Array();
		$scope.entries = new Array();

		$scope.record.mapPrinciple = [];
		$scope.record.mapNote = [];
		$scope.record.flagForLeadReview = false;
		$scope.record.flagForConsensus = false;
		$scope.record.flagForEditorialReview = false;
		
		$scope.addMapGroup(); // automatically adds entry as well
		
		window.scrollTo(0,0);
		
		broadcastRecord();
	};

	$scope.saveMapRecord = function(returnBack) {

		console.debug("saveMapRecord called with " + returnBack);
		console.debug("Note content: ", $scope.tinymceContent);
		
		// check that note box does not contain unsaved material
		if ($scope.tinymceContent != '' && $scope.tinymceContent != null) {
			if(confirm("You have unsaved text into the Map Notes. Do you wish to continue saving? The note will be lost.") == false) {
				return;
			};
		}

		
		///////////////////////////
		// Group and MapPriority //
		///////////////////////////

		// if not group structured project
		if ($scope.project.groupStructure == false) {

			// cycle over entries and assign map priority based on position
			for (var i = 0; i < $scope.entries.length; i++) {
				$scope.entries[i].mapPriority = i+1;
			}

			$scope.record.mapEntry = $scope.entries;

			// if group structured project
		} else {

			var entries = new Array();

			// cycle over each group bin
			for (var i = 0; i < $scope.entries.length; i++) {

				// cycle over entries in each group bin
				for (var j = 0; j < $scope.entries[i].length; j++) {

					console.debug("Assigning group and priority to " + i + " " + j);
					$scope.entries[i][j].mapGroup = i;
					$scope.entries[i][j].mapPriority = j+1;

					entries.push($scope.entries[i][j]);

				};
			};

			console.debug("save modified:");
			console.debug(entries);

			$scope.record.mapEntry = entries;
		};
		
		// assign the current user to the lastModifiedBy field
		$scope.record.lastModifiedBy = $scope.user;

		// if only displaying record again, don't make rest call
		if ($rootScope.currentPageDirty == false && !returnBack)
			return;
		
		$http({
			url: root_workflow + "save",
			dataType: "json",
			data: $scope.record,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			
			// user has successfully saved record, page is no longer "dirty"
			$rootScope.currentPageDirty = false;
			
			//$scope.record = data;
			$scope.recordSuccess = "Record saved.";
			$scope.recordError = "";
			if (returnBack) {
			  window.history.back(); 
			}
		}).error(function(data, status, headers, config) {
			$scope.recordError = "Error saving record.";
			$rootScope.handleHttpError(data, status, headers, config);
			$scope.recordSuccess = "";			
		});
	};



	// discard changes
	$scope.cancelMapRecord = function() {

		$rootScope.glassPane++;
		$http({
			url: root_workflow + "cancel",
			dataType: "json",
			data: $scope.record,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
			
			// user has requested a cancel event, page is no longer "dirty"
			$rootScope.currentPageDirty = false;
			$rootScope.glassPane--;
			$location.path($scope.role + "/dash");
		}).error(function(data, status, headers, config) {
			$rootScope.glassPane--;
		    $rootScope.handleHttpError(data, status, headers, config);
		});

	};

	$scope.addRecordPrinciple = function(record, principle) {

		// check if principle valid
		if (principle === '') {
			$scope.errorAddRecordPrinciple = "Principle cannot be empty";
		} else if (principle == null) {
			$scope.errorAddRecordPrinciple = "This principle is not found in allowable principles for this map project";
		} else {
			$scope.errorAddRecordPrinciple = "";

			// add localId to this principle
			principle.localId = currentLocalId++;
			
			// check if principle already present
			var principlePresent = false;
			for (var i = 0; i < record.mapPrinciple.length; i++) {
				if (principle.id == record.mapPrinciple[i].id) principlePresent = true;
			}

			if (principlePresent == true) {
				$scope.errorAddRecordPrinciple = "The principle with id " + principle.principleId  + " is already attached to the map record";
			} else {
				console.debug('Adding principle');
				$scope.record['mapPrinciple'].push(principle);
			};

			$scope.principleInput = "";
		};
		
		broadcastRecord();
	};

	$scope.removeRecordPrinciple = function(record, principle) {
		record['mapPrinciple'] = removeJsonElement(record['mapPrinciple'], principle);
		$scope.record = record;
		
		console.debug('Removed principle');
		
		broadcastRecord();
	};
	
	$scope.tinymceOptions = {
			
			menubar : false,
			statusbar : false,
			plugins : "autolink autoresize link image charmap searchreplace lists paste",
			toolbar : "undo redo | styleselect lists | bold italic underline strikethrough | charmap link image",
			
			setup : function(ed) {
				
				// added to fake two-way binding from the html element
				// noteInput is not accessible from this javascript for some reason
				ed.on('keyup', function(e) {
	                  $scope.tinymceContent = ed.getContent();
	                  $scope.$apply();
	                });
			}
	    };
	
	$scope.editRecordNote = function(record, mapNote) {
		$scope.noteInput = "HELLO HELLO";
		$scope.noteEditMode = true;
		$scope.noteEditId = mapNote.localId;
	}
	
	$scope.cancelEditRecordNote = function() {
		$scope.noteInput = '';
		$scope.noteEditMode = false;
		$scope.noteEditId = null;
	}
	
	$scope.saveEditRecordNote = function(record, note) {
		
		if ($scope.noteEditMode == true) {
			
			var noteFound = false;
			
			// find the existing note
			for (var i = 0; i < record.mapNote.length; i++) {
				
				// if this note, overwrite it
				if ($scope.noteEditId = record.mapNote[i].localId) {
					noteFound = true;
					record.mapNote[i].note = note;
				}
			}
			
			if (noteFound = false) {
				console.debug("ERROR: Could not find note to edit with id = " + $scope.noteEditId);
			}
		} else {
			console.debug("Save Edit Record Note called when not in edit mode");
		}
	};
	
	$scope.addRecordNote = function(record, note) {
		// check if note non-empty
		if (note === '' || note == null) {
			$scope.errorAddRecordNote = "Note cannot be empty";
		} else {

			// construct note object
			var mapNote = new Object();
			mapNote.id = null;
			mapNote.localId = currentLocalId++;
			mapNote.note = note;
			mapNote.timestamp = (new Date()).getTime();
			mapNote.user = $scope.user;
			
			// add note to record
			record['mapNote'].addElement(mapNote);
			
			// set the text area to null
			$scope.tinymceContent = null;


		}
		
		broadcastRecord();
	};

	$scope.removeRecordNote = function(record, note) {
		console.debug("Removing note");
		console.debug(note);
		record['mapNote'].removeElement(note);
		$scope.record = record;
		broadcastRecord();
		
		// if in edit mode, cancel
		if ($scope.noteEditMode == true) {
			$scope.cancelEditRecordNote();
		}
	};
	
	/** 
	 * FEEDBACK FUNCTIONS
	 */
	$scope.sendFeedback = function(record, feedbackMessage, recipientList) {
		console.debug("Adding feedback", record);
		
		   if (feedbackMessage == null || feedbackMessage == undefined || feedbackMessage === '') {
			   window.alert("The feedback field cannot be blank. ");
		   	   return;
		   }

		   
			  var localFeedback = $scope.conversation.feedback;
				
				// copy recipient list
				var localRecipients = recipientList.slice(0);
				var newRecipients = new Array();
				for (var i = 0; i < localRecipients.length; i++) {
					for (var j = 0; j < $scope.project.mapLead.length; j++) {
						if (localRecipients[i].id == $scope.project.mapLead[j].id)
							newRecipients.push($scope.project.mapLead[j]);
					}
				}
				
		   // if the conversation hasn't yet been started
		   if ($scope.conversation == null || $scope.conversation == "") {
			 

			   
			// create first feedback item to go into the feedback conversation
			var feedback = {
						"message": feedbackMessage,
						"mapError": "",
						"timestamp": new Date(),
						"sender": $scope.user,
						"recipients": newRecipients,
						"isError": "false",
						"feedbackConversation": $scope.conversation,
						"viewedBy": [$scope.user]
					  };		
			
			var feedbacks = new Array();
			feedbacks.push(feedback);
						
			  // create feedback conversation
			  var feedbackConversation = {
					"lastModified":  new Date(),
					"terminology": $scope.project.destinationTerminology,
					"terminologyId": record.conceptId,
					"terminologyVersion": $scope.project.destinationTerminologyVersion,
					"isActive": "true",
					"isDiscrepancyReview": "false",
					"mapRecordId": record.id,
					"feedback": feedbacks,
					"defaultPreferredName": $scope.concept.defaultPreferredName,
					"title": "Feedback",
					"mapProjectId": $scope.project.id,
					"userName": record.owner.userName
				  };
			
			  $http({						
				url: root_workflow + "conversation/add",
				dataType: "json",
				data: feedbackConversation,
				method: "PUT",
				headers: {
					"Content-Type": "application/json"
				}
			  }).success(function(data) {
				console.debug("success to addFeedbackConversation.");
				$scope.conversation = feedbackConversation;
			  }).error(function(data, status, headers, config) {
				$scope.recordError = "Error adding new feedback conversation.";
				$rootScope.handleHttpError(data, status, headers, config);
			  });	
			
			
		   } else { // already started a conversation
			   
			  
			  // create feedback msg to be added to the conversation
			  var feedback = {
							"message": feedbackMessage,
							"mapError": "",
							"timestamp": new Date(),
							"sender": $scope.user,
							"recipients": newRecipients,
							"isError": "false",
							"viewedBy": [$scope.user]
						  };
			
			  localFeedback.push(feedback);
			  $scope.conversation.feedback = localFeedback;
				
			  $http({						
				url: root_workflow + "conversation/update",
				dataType: "json",
				data: $scope.conversation,
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
	
	/**
	 * Helper function to assign group and priority to entries
	 * Used for sending email requests, and broadcasting record in expected format
	 */
	$scope.getFormattedRecord = function(record) {
	
		var formattedRecord = record;
		
		// if not group structured project
		if ($scope.project.groupStructure == false) {
	
			// cycle over entries and assign map priority based on position
			for (var i = 0; i < $scope.entries.length; i++) {
				$scope.entries[i].mapPriority = i+1;
			}
	
			formattedRecord.mapEntry = $scope.entries;
	
			// if group structured project
		} else {
	
			var entries = new Array();
	
			// cycle over each group bin
			for (var i = 0; i < $scope.entries.length; i++) {
	
				// cycle over entries in each group bin
				for (var j = 0; j < $scope.entries[i].length; j++) {
	
					console.debug("Assigning group and priority to " + i + " " + j);
					$scope.entries[i][j].mapGroup = i;
					$scope.entries[i][j].mapPriority = j+1;
	
					entries.push($scope.entries[i][j]);
	
				};
			};
	
			console.debug("record formatted:");
			console.debug(entries);
	
			formattedRecord.mapEntry = entries;
		}
		
		return formattedRecord;
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

		// cycle over map entries and extract those with this map group
		var entries = new Array();

		for (var i = 0; i < $scope.record.mapEntry.length; i++) {
			if (parseInt($scope.record.mapEntry[i].mapGroup, 10) === parseInt(mapGroup, 10)) {
				entries.push($scope.record.mapEntry[i]);
			};
		};

		return entries;  
	};
	
	$scope.getEntrySummary = function(entry) {
		
		var entrySummary = "";
		// first get the rule
		entrySummary += $scope.getRuleSummary(entry);
		
		// if target is null, check relation id
		if (entry.targetId == null || entry.targetId === '') {
			
			// if relation id is null or empty, return empty entry string
			if (entry.mapRelation == null || entry.mapRelation === '') {
				entrySummary += '[NO TARGET OR RELATION]';
			
			// otherwise, return the relation abbreviation
			} else {
				entrySummary += entry.mapRelation.abbreviation;
				
			}
		// otherwise return the target code and preferred name
		} else {
			entrySummary += entry.targetId + " " + entry.targetName;
		}
		
		return entrySummary;
		
	};

	// Returns a summary string for the entry rule type
	$scope.getRuleSummary = function(entry) {
		
		var ruleSummary = "";
		
		// first, rule summary
		if ($scope.project.ruleBased == true) {
			if (entry.rule.toUpperCase().indexOf("TRUE") != -1) ruleSummary += "[TRUE] ";
			else if (entry.rule.toUpperCase().indexOf("FEMALE") != -1) ruleSummary += "[FEMALE] ";
			else if (entry.rule.toUpperCase().indexOf("MALE") != -1) ruleSummary += "[MALE] ";
			else if (entry.rule.toUpperCase().indexOf("AGE") != -1) {

				
				var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/ );
				var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/ );

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
	// Sets the scope variable for the active entry
	$scope.selectMapEntry = function(entry) {
		console.debug("Select entry");
		console.debug(entry);
	
		// set all entries isSelected to false
		for (var i = 0; i < $scope.entries.length; i++) {
			for (var j = 0; j < $scope.entries[i].length; j++) {
				console.debug($scope.entries[i][j]);
				$scope.entries[i][j].isSelected = false;
			}
		}
		
		// set this entry to selected
		entry.isSelected = true;
		
		console.debug("Entry selected, new entries: ", $scope.entries);
		
		$rootScope.$broadcast('mapRecordWidget.notification.changeSelectedEntry',{key: 'changeSelectedEntry', entry: angular.copy(entry), record: $scope.record, project: $scope.project});  

	};

	// function for adding an empty map entry to a record
	$scope.addMapEntry = function(group) {
		
		if (group == null || group == undefined) group = new Array();

		// create blank entry associated with this id
		var newEntry = {
				"id": null,
				"mapRecordId": $scope.record.id,
				"targetId":null,
				"targetName":null,
				"rule":"TRUE",
				"mapPriority": "",
				"mapRelation": null,
				"mapBlock":"",
				"mapGroup": "",
				"mapAdvice":[],
				"mapPrinciples":[],
				"localId": currentLocalId + 1,
				"isSelected" : false
		};

		currentLocalId += 1;

		newEntry.ruleSummary = $scope.getRuleSummary(newEntry) ;

		if ($scope.project.groupStructure == true) {
			console.debug("Adding entry to group", group);
			group.push(newEntry);
			// $scope.entries[group].push(newEntry);
		} else {
			$scope.entries.push(newEntry);
		}
		
		console.debug("Entry added, new entries: ", $scope.entries);

		$scope.selectMapEntry(newEntry);
		
		broadcastRecord();

	};
	
	$scope.deleteMapEntry = function(entry) {
		console.debug("deleteMapEntry: ", entry);
		
		var group;
		
		// find group this entry is attached too
		for (var i = 0; i < $scope.entries.length; i++) {
			for (var j = 0; j < $scope.entries[i].length; j++) {
				
				if ($scope.entries[i][j].localId === entry.localId || $scope.entries[i][j].id === entry.id)
					group = i;
				console.debug($scope.entries[i][j]);
			}
		}
		
		$scope.entries[group].removeElement(entry);
		
		broadcastRecord();
	};

	// Notification watcher for save/delete entry events
	$scope.$on('mapEntryWidget.notification.modifySelectedEntry', function(event, parameters) { 	
		console.debug("MapRecordWidget: Detected entry modify request");

		var entry = parameters.entry;
		var record = parameters.record;
		

		// verify that this entry is attached to this record
		if (record.id != $scope.record.id) {
			console.debug("Non-matching rec	ord (id= " + $scope.record.id + ") will ignore entry modification request.");
		} else {

			$rootScope.currentPageDirty = true;
			
			if (parameters.action === "save") {

				console.debug("Action: SAVE", entry);

				// find the entry, based on group structure
				if ($scope.project.groupStructure == false) {

					// simple array to cycle over
					for (var i = 0; i < $scope.entries.length; i++) {

						// when entry found, overwrite it
						if ($scope.entriesEqualById($scope.entries[i], entry) == true) {
							$scope.entries[i] = angular.copy(entry);
							console.debug(" -- Found entry");
						}
					}
				} else {
					// cycle over each group bin's entry list
					for (var i = 0; i < $scope.entries.length; i++) {
						for (var j = 0; j < $scope.entries[i].length; j++) {

							// when entry found, overwrite it
							if ($scope.entriesEqualById($scope.entries[i][j], entry) == true) {
								$scope.entries[i][j] = entry;
								console.debug(" -- found entry, group " + i + ", priority " + j);
							}	
						}
					}
				}
			} else {
				console.error("MapRecordWidget: Invalid action requested for entry modification");
			}
			
			$scope.record.localEntries = $scope.entries;
		}
		
		broadcastRecord();
	});


	/////////////////////////
	// Map Group Functions //
	/////////////////////////

	// Retrieves groups from the existing entries
	function getGroups() {

		$scope.groups = new Array();
		for (var i = 0; i < $scope.record.mapEntry.length; i++) {			  

			if ($scope.groups.indexOf(parseInt($scope.record.mapEntry[i].mapGroup, 10)) == -1) {
				$scope.groups.push(parseInt($scope.record.mapEntry[i].mapGroup, 10));
			};
		};

		// if no groups found, add a default group
		if ($scope.groups.length == 0) $scope.groups.push(1);
		
		$scope.groupOpen = new Array(10);
		for (var i = 0; i < $scope.groups.length; i++) $scope.groupOpen[i] = true;

	};

	// Adds a map group to the existing list
	$scope.addMapGroup = function() {
		
		// check if zero (null) group is present, add if it not
		if ($scope.entries.length == 0)
			$scope.entries.push(new Array());
		
		console.debug("Adding group to: ", $scope.entries);
		
		var newGroup = new Array();
		$scope.addMapEntry(newGroup);
		
		$scope.entries.push(newGroup);
		
		broadcastRecord();
	};

	// Removes a map group if it exists
	$scope.removeMapGroup = function(group) { 
		console.debug("Removing group from " + $scope.entries.length + " groups", group, $scope.entries);
		
		var tempEntries = angular.copy($scope.entries);
		$scope.entries = new Array();
		$scope.entries.push(new Array()); // add empty group for 0, as index = group number
		
		// find the group by first matching entry
		for (var i = 1; i < tempEntries.length; i++) { // first one is always empty, null group
			
			console.debug("Checking group " + i + " of " + tempEntries.length, group[0].localId, tempEntries[i][0].localId);
			
			if (group[0].localId != tempEntries[i][0].localId) {
				
				console.debug("Keeping group");
				$scope.entries.push(tempEntries[i]);
			} else {
				console.debug("Found group to remove");
			}
		}
		console.debug("Entries after: ", $scope.entries);
		
		broadcastRecord();
		
	};

	///////////////////////
	// Modal Functions //
	///////////////////////
	
	// HACKISH:  Variable passed in is the currently viewed map user in the lead's View Other Work tab
	$scope.displayMapRecord = function() {
		
		console.debug("displayMapRecord with ");
		console.debug($scope.project);
		
		
		console.debug($scope.record);

		var modalInstance = $modal.open({
			templateUrl: 'js/widgets/mapRecord/displayMapRecord.html',
			controller: DisplayMapRecordCtrl,
		    size: 'lg',
			resolve: {				
				record: function() {
					return $scope.record;
				},
	            project: function() {
	                return $scope.project;
	            }
			}
		});

	};
	
	var DisplayMapRecordCtrl = function($scope, $modalInstance, record, project) { 
		
		console.debug("Entered display map record modal control");
		$scope.mapRecord = record;
		$scope.project = project;
	

		$scope.cancel = function() {
			$modalInstance.dismiss('cancel');
		};
	}
	
	
	///////////////////////
	// Utility Functions //
	///////////////////////

	// sort and return an array by string key
	function sortByKey(array, key) {
		return array.sort(function(a, b) {
			var x = a[key]; var y = b[key];
			return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
	};

	// function to add an element and assign a local id if not tracked by hibernate
	Array.prototype.addElement = function(elem) {

		// if hibernate id, simply add
		if (elem.id != null && elem.id != '') {
			this.push(elem);

			// otherwise, assign a unique localid
		} else {

			// get the maximum local id already assigned in this array
			var maxLocalId = -1;
			$.map(this, function(v,i) {
				if (v.hasOwnProperty("localId")) {
					if (v['localId'] > maxLocalId) maxLocalId = v['localId'];
				}
			});

			elem['localId'] = maxLocalId == -1 ? 1 : maxLocalId + 1;
		}

		this.push(elem);
	};

	// function to remove an element by id or localid
	// instantiated to negate necessity for equals methods for map objects
	//   which may not be strictly identical via string or key comparison
	Array.prototype.removeElement = function(elem) {

		console.debug('before removeElement', elem, $scope.entries);
		
		// switch on type of id
		var idType = elem.hasOwnProperty('localId') ? 'localId' : 'id';
		
		console.debug('idType = ', idType);

		var array = new Array();
		$.map(this, function(v,i){
			console.debug(v[idType], elem[idType]);
			if (v[idType] != elem[idType]) array.push(v);
		});

		this.length = 0; //clear original array
		this.push.apply(this, array); //push all elements except the one we want to delete
		
		console.debug('after removeElement', elem, $scope.entries);
	};
	
	function removeJsonElement(array, elem) {
				
		console.debug("Removing element");
		var newArray = [];
		for (var i = 0; i < array.length; i++) {
			if (array[i].id != elem.id) {
				console.debug("Pushing element " + array[i].id);
				newArray.push(array[i]);
			}
		}
		
		console.debug("After remove, before return:");
		console.debug(newArray);
		return newArray;
	}	
	
	// function to return trusted html code (for tooltip content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};


	$scope.getBrowserUrl = function() {
		return "http://dailybuild.ihtsdotools.org/index.html?perspective=full&conceptId1=" + $scope.concept.terminologyId + "&diagrammingMarkupEnabled=true&acceptLicense=true";
	};

    $scope.openConceptBrowser = function() {
    	var myWindow = window.open($scope.getBrowserUrl(), "browserWindow");
    	myWindow.focus();
    };
    
	$scope.isFeedbackViewed = function() {
		if ($scope.conversation == null || $scope.conversation == "")
			return true;
    	for (var i = 0; i < $scope.conversation.feedback.length; i++) {
    		var alreadyViewedBy =  $scope.conversation.feedback[i].viewedBy;
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
    
    // add current user to list of viewers who have seen the feedback conversation
    $scope.markViewed = function() {
    	var needToUpdate = false;
		if ($scope.conversation == null || $scope.conversation == "")
			return;
    	for (var i = 0; i < $scope.conversation.feedback.length; i++) {
    		var alreadyViewedBy =  $scope.conversation.feedback[i].viewedBy;
    		var found = false;
    		for (var j = 0; j<alreadyViewedBy.length; j++) {
    			if (alreadyViewedBy[j].userName == $scope.user.userName)
    				found = true;
    		}	
        	if (found == false) {
      		  alreadyViewedBy.push($scope.user);
      		  needToUpdate = true;
        	}
    	}
    	
    	if (needToUpdate == true) {
		  $http({						
				url: root_workflow + "conversation/update",
				dataType: "json",
				data: $scope.conversation,
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
    
    function initializeReturnRecipients() {
		
    	// if no previous feedback conversations, return just first map lead in list
		if ($scope.conversation == null || $scope.conversation == "") {
    	  $scope.returnRecipients.push($scope.project.mapLead[0]);
    	  return;
		}
    	
    	// figure out the return recipients based on previous feedback in conversation
		var localFeedback = $scope.conversation.feedback;
		var localSender = localFeedback[localFeedback.length -1].sender;
		var localRecipients = localFeedback[localFeedback.length -1].recipients;
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
    };
    
    // for multi-select user picklist
    function organizeUsers(arr) {
    	// remove Current user
        for(var i = arr.length; i--;) {
            if(arr[i].userName === $scope.user.userName) {
                arr.splice(i, 1);
            }
        }
        
        // remove demo users
        for(var i = arr.length; i--;) {       	
            if(arr[i].name.indexOf("demo") > -1) {
                arr.splice(i, 1);
            }
        }  
        
    	sortByKey(arr, "name");
    }
    
	// sort and return an array by string key
	function sortByKey(array, key) {
		return array.sort(function(a, b) {
			var x = a[key]; var y = b[key];
			return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
	};
});
