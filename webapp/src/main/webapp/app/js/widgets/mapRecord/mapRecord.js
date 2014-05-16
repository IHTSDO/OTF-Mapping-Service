
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

.controller('mapRecordWidgetCtrl', function($scope, $rootScope, $http, $routeParams, $location, $sce, localStorageService){

	/////////////////////////////////////
	// Map Record Controller Functions //
	/////////////////////////////////////

	// initialize scope variables
	$scope.record = 	null;
	$scope.project = 	localStorageService.get('focusProject');
	$scope.concept = 	null;
	$scope.groups = 	null;
	$scope.entries =    null;
	$scope.user = 		localStorageService.get('currentUser');
	
	// validation result storage variable
	$scope.savedValidationWarnings = [];
	
	// record change stack
	$scope.recordStack = [];					// the stack of modified records
	$scope.recordStackPosition = -1; 			// will be incremented to 0 upon initialization
	$scope.recordStackInUse = false;			// flag for whether record change is due to user action
	$scope.recordInitializationComplete = false;// flag for whether initialization complete

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
        
        //get the groups
        if ($scope.project.groupStructure == true)
               getGroups();
        
		// This MUST not be removed for "Start here" to work
		initializeEntries();
		

	});
	
	// on successful retrieval of project, get the record/concept
	$scope.$watch('project', function() {
		retrieveRecord();
	});
	
	// watch the record for saving local revision history
	$scope.$watch('record', function() {
		
		// if initializing, do nothing
		if ($scope.recordInitializationComplete == true) {
		
			// if the record has changed but the in use flag is false, add the new revision and update the position
			if ($scope.recordStackInUse == false) {
				
				// if not the last entry, discard any future revisions (i.e. record has changed from this saved state)
				if ($scope.recordStackPosition != $scope.recordStack.length && $scope.recordStack != 0) {
					$scope.recordStack = $scope.recordStack.slice(0, $scope.recordStackPosition + 1);
				}
				
				// push the record onto the stack
				$scope.recordStack.push($scope.record);
				$scope.recordStackPosition++;
				
				console.debug('recordStack:  New version: ' + $scope.recordStackPosition);
			// otherwise, do nothing, and set the in use flag to false
			} else {
				console.debug('recordStack:  New version, but stack in use');
				
				$scope.recordStackInUse = true;
			}
		} else {
			console.debug('recordStack:  Change detected, due to initialization');
		}
	});
	
	// UNDO function: if not at the beginning of the stack, change the record to previous version
	$scope.undoMapRecordChange = function() {
		if ($scope.recordStackPosition > 0) {
			$scope.recordStackInUse = true;
			$scope.record = $scope.recordStack[$scope.recordStackPosition - 1];
			$scope.entries = $scope.record.localEntries;
			$scope.recordStackPosition--;
		}
	};
	
	// REDO function: if at the beginning of the stack, change record to next version
	$scope.redoMapRecordChange = function() {
		if ($scope.recordStackPosition < $scope.recordStack.length) {
			$scope.recordStackInUse = true;
			$scope.record = $scope.recordStack[$scope.recordStackPosition + 1];
			$scope.entries = $scope.record.localEntries;
			$scope.recordStackPosition++;
		}
	};

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
	
		}).error(function(error) {
			$scope.error = $scope.error + "Could not retrieve map record. ";
	
		}).then(function() {
	
			// obtain the record concept
			$http({
				url: root_content + "concept/" 
				+ $scope.project.sourceTerminology + "/"
				+ $scope.project.sourceTerminologyVersion + "/"
				+ "id/" + $scope.record.conceptId,
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.concept = data;
				$scope.conceptBrowserUrl = $scope.getBrowserUrl();
			}).error(function(error) {
				$scope.error = $scope.error + "Could not retrieve record concept. ";
			});
	
	
			// get the groups
			if ($scope.project.groupStructure == true)
				getGroups();
	
			// initialize the entries
			initializeEntries();
			
			// assign the local entries to the map record, triggers recordStack
			console.debug("recordStack: Assigning local entries");
			$scope.record.localEntries = $scope.entries;
	
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
		
		$scope.recordInitializationComplete = false;
		
		// find the maximum hibernate id value
		for (var i = 0; i < $scope.record.mapEntry.length; i++) {
			currentLocalId = Math.max($scope.record.mapEntry[i].id, currentLocalId);
		}

		// calculate rule summaries and assign local id equivalent to hibernate id (needed for track by in ng-repeat)
		for (var i = 0; i < $scope.record.mapEntry.length; i++) {
			$scope.record.mapEntry[i].ruleSummary = 
				$scope.getRuleSummary($scope.record.mapEntry[i]);
			
			// assign the entry localId to the hibernate Id
			$scope.record.mapEntry[i].localId = 	$scope.record.mapEntry[i].id;
		}

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
			$scope.selectEntry($scope.record.mapEntry[0]);
		}
		
		$scope.recordInitializationComplete = true;
	}

	/**
	 * MAP RECORD FUNCTIONS
	 */
	$scope.finishAndNextMapRecord = function($location) {
		$scope.finishMapRecord();
		console.debug($scope.validationResult);
		

		console.debug('Retrieving Assigned Work');

		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": 0,
			 	 	 "maxResults": 1, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_workflow + "assignedWork/projectId/" + $scope.project.id + "/user/" + $scope.user.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;

			$scope.assignedRecords = data.searchResult;
			console.debug('in success' + $scope.assignedRecords[0].id);
			
			var path = "record/recordId/" + $scope.assignedRecords[0].id;
			
			console.debug("go to " + path);
			// redirect page
			$location.path(path);

		}).error(function(error) {
		  	$rootScope.glassPane--;
			$scope.error = "Error";
		});
	};
	
	$scope.finishMapRecord = function() {

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

			console.debug("modified:");
			console.debug(entries);

			$scope.record.mapEntry = entries;
			
			
		}

		console.debug("Validating the map record");
		// validate the record
		$http({
			url: root_validation + "record/validate",
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
		}).error(function(data) {
			$scope.validationResult = null;
			console.debug("Failed to validate map record");
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
						//if (returnBack) {
						  window.history.back();
						//}
					}).error(function(data) {
						$scope.recordSuccess = "";
						$scope.recordError = "Error saving record.";
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

	$scope.saveMapRecord = function() {

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

			console.debug("modified:");
			console.debug(entries);

			$scope.record.mapEntry = entries;
		};


		// assign the current user to the lastModifiedBy field
		$scope.record.lastModifiedBy = $scope.user;

		$http({
			url: root_workflow + "save",
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
			window.history.back(); 
		}).error(function(data) {
			$scope.recordSuccess = "";
			$scope.recordError = "Error saving record.";
		});
	};



	// discard changes
	$scope.cancelMapRecord = function() {

		window.history.back();

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

	$scope.addRecordNote = function(record, note) {
		// check if note non-empty
		if (note === '' || note == null) {
			$scope.errorAddRecordNote = "Note cannot be empty";
		} else {

			// construct note object
			var mapNote = new Object();
			mapNote.localId = currentLocalId++;
			mapNote.note = note;
			mapNote.timestamp = (new Date()).getMilliseconds();
			mapNote.user = localStorageService.get('currentUser');

			console.debug(mapNote);

			// add note to record
			record['mapNote'].addElement(mapNote);




			// set scope record to record
			//$scope.record = record;

			console.debug($scope.record);

		}
	};

	$scope.removeRecordNote = function(record, note) {
		console.debug("Removing note");
		console.debug(note);
		record['mapNote'] = removeJsonElement(record['mapNote'], note);
		$scope.record = record;
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
	$scope.selectEntry = function(entry) {
		console.debug("Select entry");
		console.debug(entry);
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
				"localId": currentLocalId + 1
		};

		currentLocalId += 1;

		newEntry.ruleSummary = $scope.getRuleSummary(newEntry) ;

		if ($scope.project.groupStructure == true) {
			$scope.entries[group].push(newEntry);
		} else {
			$scope.entries.push(newEntry);
		}

		$scope.selectEntry(newEntry);

	};

	// Notification watcher for save/delete entry events
	$scope.$on('mapEntryWidget.notification.modifySelectedEntry', function(event, parameters) { 	
		console.debug("MapRecordWidget: Detected entry modify request");

		var entry = parameters.entry;
		var record = parameters.record;

		// verify that this entry is attached to this record
		if (record.id != $scope.record.id) {
			console.debug("Non-matching record (id= " + $scope.record.id + ") will ignore entry modification request.");
		} else {

			if (parameters.action === "save") {

				console.debug("Action: SAVE");

				// find the entry, based on group structure
				if ($scope.project.groupStructure == false) {

					// simple array to cycle over
					for (var i = 0; i < $scope.entries.length; i++) {

						// when entry found, overwrite it
						if ($scope.entriesEqualById($scope.entries[i], entry) == true) {
							$scope.entries[i] = entry;
						}
					}
				} else {
					// cycle over each group bin's entry list
					for (var i = 0; i < $scope.entries.length; i++) {
						for (var j = 0; j < $scope.entries[i].length; j++) {

							// when entry found, overwrite it
							if ($scope.entriesEqualById($scope.entries[i][j], entry) == true) {
								$scope.entries[i][j] = entry;
							}	
						}
					}
				}

			} else if (parameters.action === "delete") {

				console.debug("Action: DELETE");

				// handle action based on group structure
				if ($scope.project.groupStructure == false) {

					// construct new entries list
					var entries = new Array();

					// push all entries not matching this entry onto new entries list
					for (var i = 0; i < $scope.entries.length; i++) {
						if ($scope.entriesEqualById(entry, $scope.entries[i]) == false) {
							entries.push($scope.entries[i]);
						}
					}

					// overwrite scope entries list
					$scope.entries = entries;

				} else {

					// cycle over all group bins
					for (var i = 0; i < $scope.entries.length; i++) {

						// construct new entries list
						var entries = new Array();

						// push all entries not matching this entry into new entries list for this group bin
						for (var j = 0; j < $scope.entries[i].length; j++) {
							if ($scope.entriesEqualById(entry, $scope.entries[i][j]) == false) {
								entries.push($scope.entries[i][j]);
							}
						}

						// overwrite the group bin
						$scope.entries[i] = entries;
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

		// switch on type of id
		var idType = elem.hasOwnProperty('localId') ? 'localId' : 'id';

		var array = new Array();
		$.map(this, function(v,i){
			if (v[idType] != elem[idType]) array.push(v);
		});

		this.length = 0; //clear original array
		this.push.apply(this, array); //push all elements except the one we want to delete
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
		
		console.debug("After remove, before return:")
		console.debug(newArray)
		return newArray;
	}	
	
	// function to return trusted html code (for tooltip content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};


	$scope.getBrowserUrl = function() {
		return "http://browser.ihtsdotools.org/index.html?perspective=full&conceptId1=" + $scope.concept.terminologyId + "&diagrammingMarkupEnabled=true&acceptLicense=true";
	};

});
