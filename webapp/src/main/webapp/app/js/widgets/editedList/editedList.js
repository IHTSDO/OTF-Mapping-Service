
'use strict';

angular.module('mapProjectApp.widgets.editedList', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('editedList', {
		title: 'Recently Edited',
		description: 'Displays a list of records that have been recently modified by the current user',
		controller: 'editedListCtrl',
		templateUrl: 'js/widgets/editedList/editedList.html',
		edit: {}
	});
}).controller('editedListCtrl', function($scope, $rootScope, $http, localStorageService){
	
	// initialize as empty to indicate still initializing database connection
	$scope.editedRecords = [];
	$scope.user = localStorageService.get('currentUser');
	$scope.focusProject = localStorageService.get('focusProject');

	// pagination variables
	$scope.recordsPerPage = 10;
	$scope.editedRecordsPage = 1;
	
	// watch for project change
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("editedListWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});	

	$scope.$on('availableWork.notification.editWork', function(event, parameters) {

		console.debug("editedListCtrl: Detected editWork notificatoin from availableWork widget");
		$scope.editWork(parameters.editedWork);

	});

	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {
		console.debug('editedListCtrl:  Detected project set/change');

		if ($scope.focusProject != null) {
			$scope.retrieveEditedWork($scope.editedRecordsPage);
		}
	});
	
	$scope.retrieveEditedWork = function(page) {

		// set the page
		$scope.editedRecordsPage = page;
		 
		// construct a paging/filtering/sorting object
		var pfsParameterObj = 
					{"startIndex": (page-1)*$scope.recordsPerPage,
			 	 	 "maxResults": $scope.recordsPerPage, 
			 	 	 "sortField": 'sortKey',
			 	 	 "filterString": null};  

	  	$rootScope.glassPane++;

		$http({
			url: root_mapping + "recentRecords/" + $scope.focusProject.id + "/" + $scope.user.userName,
			dataType: "json",
			data: pfsParameterObj,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}
		}).success(function(data) {
		  	$rootScope.glassPane--;
			
			$scope.recordPage = page;
			$scope.nRecords = data.totalCount;
			$scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
			 
			$scope.editedRecords = data.mapRecord;
			console.debug("Edited records:");
			console.debug($scope.editedRecords);
						 
		}).error(function(error) {
		  	$rootScope.glassPane--;
			$scope.error = "Error";
		});
	};
	
	// returns a short summary of the record based on number of entries
	$scope.getRecordSummary = function(record) {
		
		// if no entries, return null
		if (record.mapEntry.length == 0) {
			return "";
		}
		
		// if only one entry, display the full entry summary
		else if (record.mapEntry.length == 1) {
			return $scope.getEntrySummary(record.mapEntry[0]);
		
		// otherwise simply return a string indicating the number of entries and groups
		} else if ($scope.focusProject.groupStructure == true) {
			
			var maxGroup = 0;
			for (var i = 0; i < record.mapEntry.length; i++) {
				if (record.mapEntry[i].mapGroup > maxGroup) {
					maxGroup = record.mapEntry[i].mapGroup;
				};
			}
			return "" + maxGroup + " groups, " + record.mapEntry.length + " entries";
			
		} else {
			return "" + record.mapEntry.length + " entries";
		};
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
				
			};
		// otherwise return the target code and preferred name
		} else {
			entrySummary += entry.targetId + " " + entry.targetName;
		};
		
		return entrySummary;
		
	};
	
	// Returns a summary string for the entry rule type
	$scope.getRuleSummary = function(entry) {
		
		var ruleSummary = "";
		
		// first, rule summary
		if ($scope.focusProject.ruleBased == true) {
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
	

});
