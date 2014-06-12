
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
.controller('compareRecordsCtrl', function($scope, $rootScope, $http, $routeParams, $location, localStorageService){

	/////////////////////////////////////
	// Map Record Controller Functions //
	/////////////////////////////////////
	
	console.debug("Entering compareRecordsCtrl");

	// initialize scope variables
	$scope.concept = 	null;
	$scope.project = 	localStorageService.get('focusProject');
	$scope.user = 		localStorageService.get('currentUser');
	$scope.role = 		localStorageService.get('currentRole');

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
	
	// watch for project change and modify the local variable if necessary
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
			$scope.project = parameters.focusProject;
	});
	
	// on any change of focusProject, retrieve new available work
	$scope.$watch('project', function() {
		
		console.debug('compareRecords:  Detected change in project');
		
		// if first visit, retrieve the conflict records
		if ($scope.leadRecord == null) {
			console.debug("First visit, getting conflict records");
			$scope.getRecordsInConflict();
		
		// otherwise, return to dashboard (mismatch between record and project
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
		}).error(function(response) {
			$scope.error = $scope.error + "Could not retrieve map record. "; 

			if (response.indexOf("HTTP Status 401") != -1) {
				$rootScope.globalError = "Authorization failed.  Please log in again.";
				$location.path("/");
			}

			// obtain the record concept - id from leadRecord	    	  
		}).then(function(data) {
			$http({
				url: root_content + "concept/" 
				+ $scope.project.sourceTerminology + "/"
				+ $scope.project.sourceTerminologyVersion + "/"
				+ "id/" + $scope.leadRecord.conceptId,
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.concept = data;
				setTitle($scope.concept.terminologyId, $scope.concept.defaultPreferredName);
			}).error(function(error) {
				$scope.error = $scope.error + "Could not retrieve record concept. ";


			});
		});

		// get the conflict records
		$http({
			url: root_mapping + "record/conflictRecords/" + $routeParams.recordId,
			dataType: "json",
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).success(function(data) {
			if(data.totalCount < 2) console.debug("ERROR:  Could not retrieve at least two records in conflict");
			else {
				// set the origin records (i.e. the records in conflict)
				$scope.record1 = data.mapRecord[0];
				$scope.record2 = data.mapRecord[1];
			}

		}).error(function(error) {
			$scope.error = $scope.error + "Could not retrieve conflict records. ";
		}).then(function(data) {
			
			// get the groups
			if ($scope.project.groupStructure == true)
				getGroups();

			// initialize the entries
			initializeEntries();

			// obtain the validationResults from compareRecords
			$http({
				url: root_mapping + "record/compare/" + $scope.record1.id + "/" + $scope.record2.id,
				dataType: "json",
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success(function(data) {
				$scope.validationResult = data;
			}).error(function(error) {
				$scope.error = $scope.error + "Could not retrieve comparison report. ";   		  	      
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
		for (var i = 0; i < $scope.record2.mapEntry.length; i++) {			  

			if ($scope.groups2.indexOf(parseInt($scope.record2.mapEntry[i].mapGroup, 10)) == -1) {
				$scope.groups2.push(parseInt($scope.record2.mapEntry[i].mapGroup, 10));
			};
		};

		// if no groups found, add a default group
		if ($scope.groups2.length == 0) $scope.groups2.push(1);

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
		$scope.model.title = "Compare Records: " + id + "  " + term;
	};

	$scope.populateMapRecord = function(record) {
		
		console.debug('populating map record');
		console.debug(record);
		
		var newLeadRecord = angular.copy(record);
		
		for (var i = 0; i < newLeadRecord.mapNote.length; i++) {
			console.debug("nulling map note id");
			newLeadRecord.mapNote[i].id = null;
		}
		
		
		
		newLeadRecord.id = $scope.leadRecord.id;
		newLeadRecord.workflowStatus = 'CONFLICT_IN_PROGRESS';
		$rootScope.$broadcast('compareRecordsWidget.notification.selectRecord',{record: newLeadRecord});  
		
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
	
	// function to return trusted html code (for advice content)
	$scope.to_trusted = function(html_code) {
		return $sce.trustAsHtml(html_code);
	};

});
