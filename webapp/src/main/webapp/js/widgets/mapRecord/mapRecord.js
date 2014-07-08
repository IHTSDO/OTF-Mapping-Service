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
	
	// validation result storage variable
	$scope.savedValidationWarnings = [];

	// initialize accordion variables
	$scope.isConceptOpen = true;
	$scope.isEntriesOpen = true;
	$scope.isPrinciplesOpen = false;
	$scope.isNotesOpen = false;
	$scope.isFlagsOpen = false;

	// accordion functions
	$scope.openAll = function() {
		$scope.isConceptOpen = true;
		$scope.isEntriesOpen = true;
		$scope.isPrinciplesOpen = true;
		$scope.isNotesOpen = true;
		$scope.isFlagsOpen = true;
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
	
	$scope.userToken = localStorageService.get('userToken');
	// on successful retrieval of project, get the record/concept
	$scope.$watch(['project', 'userToken'], function() {
		if ($scope.project != null && $scope.userToken != null) {
	
			$http.defaults.headers.common.Authorization = $scope.userToken;
			retrieveRecord();
		}
	});


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
				$scope.record.mapEntry[i].localId = 	$scope.record.mapEntry[i].id;
			} else {
				console.debug("Setting local id to " + currentLocalId);
				$scope.record.mapEntry[i].localId = currentLocalId++;
			}
		}

		console.debug($scope.record);
		// if no group structure, simply copy and sort
		if ($scope.project.groupStructure == false) {
			$scope.entries = sortByKey($scope.record.mapEntry, 'mapPriority');

			// otherwise, initialize group arrays
		} else {

			// initialize entry arrays for distribution by group
			$scope.entries = new Array(10);

			for (var i=0; i < $scope.entries.length; i++) $scope.entries[i] = new Array();

			// cycle over the entries and assign to group bins
			for (var i=0; i < $scope.record.mapEntry.length; i++) {
				$scope.entries[$scope.record.mapEntry[i].mapGroup].push($scope.record.mapEntry[i]);
			}

			// cycle over group bins and sort contents by map priority
			for (var i=0; i< $scope.entries.length; i++) {
				$scope.entries[i] = sortByKey($scope.entries[i], 'mapPriority');
			}
		}

		// if no entries on this record, assume new and create an entry
		if ($scope.record.mapEntry.length == 0) {
			$scope.addMapEntry(1);
			
		// otherwise, select the first entry
		} else {
			$scope.selectMapEntry($scope.record.mapEntry[0]);
		}

	}

	/**
	 * MAP RECORD FUNCTIONS
	 */
	
	$scope.finishMapRecord = function(returnBack) {
		
		console.debug("finishMapRecord called with " + returnBack);

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

		console.debug("Validating the map record");
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
						$scope.record = data;
						$scope.recordSuccess = "Record saved.";
						$scope.recordError = "";
						
						// user has successfully finished record, page is no longer "dirty"
						$rootScope.currentPageDirty = false;
						
						if (!returnBack) {
							console.debug("************* ReturnBack is false");

							// construct a paging/filtering/sorting object
							var pfsParameterObj = 
							{"startIndex": 0,
									"maxResults": 1, 
									"sortField": 'sortKey',
									"queryRestriction": null};  

							$rootScope.glassPane++;
							
							// if specialist level work, query for assigned concepts
							if ($scope.record.workflowStatus === 'NEW' 
								|| $scope.record.workflowStatus === 'EDITING_IN_PROGRESS' 
								|| $scope.record.workflowStatus === 'EDITING_DONE') {

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
								// get the assigned conflicts
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
		
		$scope.closeConceptBrowser();
	};

	$scope.saveMapRecord = function(returnBack) {

		console.debug("saveMapRecord called with " + returnBack);

		
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
	};

	$scope.removeRecordPrinciple = function(record, principle) {
		record['mapPrinciple'] = removeJsonElement(record['mapPrinciple'], principle);
		$scope.record = record;
		
		console.debug('Removed principle');
	};
	
	$scope.tinymceOptions = {
			
			menubar : false,
			statusbar : false,
			plugins : "autolink link image charmap searchreplace",
			toolbar : "undo redo | styleselect | bold italic underline strikethrough | charmap link image",
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
			mapNote.user = localStorageService.get('currentUser');
			
			// add note to record
			record['mapNote'].addElement(mapNote);
			
			// set the text area to null
			$scope.noteInput = "";


		}
	};

	$scope.removeRecordNote = function(record, note) {
		console.debug("Removing note");
		console.debug(note);
		record['mapNote'].removeElement(note);
		$scope.record = record;
	};
	
	$scope.editRecordNote = function(record, note) {
		console.debug("Editing note", note);
		$scope.noteInput = note.note;
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
	$scope.selectMapEntry = function(entry, group) {
		console.debug("Select entry");
		console.debug(entry);
	
		for (var i = 0; i < $scope.entries.length; i++) {
			for (var j = 0; j < $scope.entries[i].length; j++) {
				console.debug($scope.entries[i][j]);
				$scope.entries[i][j].isSelected = false;
			}
		}
		
		entry.isSelected = true;
		
		$rootScope.$broadcast('mapRecordWidget.notification.changeSelectedEntry',{key: 'changeSelectedEntry', entry: angular.copy(entry), record: $scope.record, project: $scope.project});  

	};

	// function for adding an empty map entry to a record
	$scope.addMapEntry = function(group) {

		// create blank entry associated with this id
		var newEntry = {
				"id": "",
				"mapRecordId": $scope.record.id,
				"targetId":"",
				"targetName":"",
				"rule":"TRUE",
				"mapPriority": "",
				"relationId":"",
				"relationName":"",
				"mapBlock":"",
				"mapGroup": group,
				"mapAdvice":[],
				"mapPrinciples":[],
				"localId": currentLocalId + 1,
				"isSelected" : false
		};

		currentLocalId += 1;

		newEntry.ruleSummary = $scope.getRuleSummary(newEntry) ;

		if ($scope.project.groupStructure == true) {
			$scope.entries[group].push(newEntry);
		} else {
			$scope.entries.push(newEntry);
		}

		$scope.selectMapEntry(newEntry);

	};
	
	$scope.deleteMapEntry = function(entry, group) {
		console.debug("deleteMapEntry: ", entry);
		$scope.entries[group].removeElement(entry);
		
		$rootScope.$broadcast('mapRecordWidget.notification.deleteSelectedEntry',{key: 'deleteSelectedEntry', entry: angular.copy(entry), record: $scope.record, project: $scope.project});  

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
							$scope.entries[i] = entry;
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

	};

	// Adds a map group to the existing list
	$scope.addMapGroup = function() {

		// find first numeric group not already in list
		var i = 1;
		while ($scope.groups.indexOf(i) != -1) i++;

		$scope.groups.push(i);
		$scope.addMapEntry(i);
	};

	// Removes a map group if it exists
	$scope.removeMapGroup = function(group) {   	  
		var newGroups = new Array();
		for (var i = 0; i < $scope.groups.length; i++) {
			if ($scope.groups[i] != group) newGroups.push($scope.groups[i]);
		}
		$scope.groups = newGroups;
	};

	///////////////////////
	// Modal Functions //
	///////////////////////
	
	// HACKISH:  Variable passed in is the currently viewed map user in the lead's View Other Work tab
	$scope.displayMapRecord = function(mapUser) {
		
		$scope.saveMapRecord(false);
		
		console.debug("displayMapRecord with ");
		console.debug(mapUser);
		
		
		console.debug($scope.record);

		var modalInstance = $modal.open({
			templateUrl: 'js/widgets/mapRecord/displayMapRecord.html',
			controller: DisplayMapRecordCtrl,
		    size: 'lg',
			resolve: {				
				record: function() {
					return $scope.record;
				}
			}
		});

	};
	
	var DisplayMapRecordCtrl = function($scope, $modalInstance, record) { 
		
		console.debug("Entered display map record modal control");
		$scope.mapRecord = record;
	

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
		console.debug("to_trusted", $sce.trustAsHtml(html_code));
		return $sce.trustAsHtml(html_code);
	};


	$scope.getBrowserUrl = function() {
		return "http://dailybuild.ihtsdotools.org/index.html?perspective=full&conceptId1=" + $scope.concept.terminologyId + "&diagrammingMarkupEnabled=true&acceptLicense=true";
	};

    $scope.openConceptBrowser = function() {
    	$scope.window = $window.open($scope.getBrowserUrl());
    };
    
    $scope.closeConceptBrowser = function() {
    	if ($scope.window != null)
    	$scope.window.close();
    };
});
