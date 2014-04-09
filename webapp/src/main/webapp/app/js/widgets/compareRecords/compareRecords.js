
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
	.controller('compareRecordsCtrl', function($scope, $rootScope, $http, $routeParams, localStorageService){
		  
	        /////////////////////////////////////
	        // Map Record Controller Functions //
	        /////////////////////////////////////
		
	  		// initialize scope variables
	  		$scope.project = 	null;
	  		$scope.concept = 	null;
	  		$scope.user = 		localStorageService.get('currentUser');
	  		
	  		$scope.record1 = 	null;
	  		$scope.groups1 = 	null;
	  		$scope.entries1 =    null;
	  		
	  		$scope.record2 = 	null;
	  		$scope.groups2 = 	null;
	  		$scope.entries2 =    null;
	  		
	  		// initialize accordion variables
	  		$scope.isConceptOpen = true;
	  		$scope.isEntriesOpen = true;
	  		$scope.isPrinciplesOpen = true;
	  		$scope.isNotesOpen = true;
	  		
	  		
	  		// broadcast page to help mechanism  
	  		$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'editDashboard'});  
	  		
	  		// initialize local variables
	  		var recordId = 		$routeParams.recordId; 
	  		var currentLocalId = 0;   // used for addition of new entries without hibernate id
	  		
	  		// obtain the record
	  		$http({
	  			 url: root_mapping + "record/id/6136",
	  			 dataType: "json",
	  		        method: "GET",
	  		        headers: { "Content-Type": "application/json"}	
	  	      }).success(function(data) {
	  	    	  $scope.record1 = data;
	  	    	 
	  	      }).error(function(error) {
	  	    	  $scope.error = $scope.error + "Could not retrieve map record. ";
	  	     
	  	      }).then(function() {

	  	    	  // obtain the record project
	  	    	 $http({
	  	 			 url: root_mapping + "project/id/" + $scope.record1.mapProjectId,
	  	 			 dataType: "json",
	  	 		        method: "GET",
	  	 		        headers: { "Content-Type": "application/json"}	
	  		      }).success(function(data) {
	  	 		    	  $scope.project = data;
	  		      }).error(function(error) {
	  	 		    	  $scope.error = $scope.error + "Could not retrieve map project. ";
	  	          }).then(function() {
	  	
	  	        	  // obtain the record concept
	  	        	 $http({
	  	     			 url: root_content + "concept/" 
	  	 				  		+ $scope.project.sourceTerminology + "/"
	  					  		+ $scope.project.sourceTerminologyVersion + "/"
	  					  		+ "id/" + $scope.record1.conceptId,
	  					  	dataType: "json",
	  	     		        method: "GET",
	  	     		        headers: { "Content-Type": "application/json"}	
	  	 		      }).success(function(data) {
	  	     		    	  $scope.concept = data;
	  	 		      }).error(function(error) {
	  	     		    	  $scope.error = $scope.error + "Could not retrieve record concept. ";
	  	 		      });
	  	        	 
	  		    	  
	  		    	  // get the groups
	  	        	  if ($scope.project.groupStructure == true)
	  	        		  getGroups();
	  	        	  
	  	        	  // initialize the entries
	  		    	  initializeEntries();
	  	          });
	            });
	  		

	  		///////////////////////////////
	  		// Initialization Functions ///
	  		///////////////////////////////
	  		
	  		// construct an object containing entries, either:
	  		// 1) a 1-d array, if project has no group structure
	  		// 2) a 2-d array, with structure [group][mapPriority]
	  		
	  		// TODO: rework for 2
	  		function initializeEntries() {
	  			
	  		  // INITIALIZE FIRST RECORD
	  			
	  			// calculate rule summaries and assign local id equivalent to hibernate id (needed for track by in ng-repeat)
	  			for (var i = 0; i < $scope.record1.mapEntry.length; i++) {
	  				$scope.record1.mapEntry[i].ruleSummary = 
	  					$scope.getRuleSummary($scope.record1.mapEntry[i]);
	  				$scope.record1.mapEntry[i].localId = $scope.record1.mapEntry[i].id;
	  				
	  				currentLocalId = Math.max($scope.record1.mapEntry[i].localId, currentLocalId);
	  			}
	  					
	  			// if no group structure, simply copy and sort
	  			if ($scope.project.groupStructure == false) {
	  				$scope.entries1 = sortByKey($scope.record1.mapEntry, 'mapPriority');
	  				
	  			// otherwise, initialize group arrays
	  			} else {
	  				
	  				// TODO Clunky array assignment, consider revisiting
	  				// initiailize entry arrays for distribution by group
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
	  			
	  			// INITIALIZE SECOND RECORD
	  			
	  			// calculate rule summaries and assign local id equivalent to hibernate id (needed for track by in ng-repeat)
	  			for (var i = 0; i < $scope.record2.mapEntry.length; i++) {
	  				$scope.record2.mapEntry[i].ruleSummary = 
	  					$scope.getRuleSummary($scope.record2.mapEntry[i]);
	  				$scope.record2.mapEntry[i].localId = $scope.record2.mapEntry[i].id;
	  				
	  				currentLocalId = Math.max($scope.record2.mapEntry[i].localId, currentLocalId);
	  			}
	  					
	  			// if no group structure, simply copy and sort
	  			if ($scope.project.groupStructure == false) {
	  				$scope.entries2 = sortByKey($scope.record2.mapEntry, 'mapPriority');
	  				
	  			// otherwise, initialize group arrays
	  			} else {
	  				
	  				// TODO Clunky array assignment, consider revisiting
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
	  		}
	  		
	  		/**
	  		 * MAP RECORD FUNCTIONS
	  		 */
	  		

	  		/**
	  		 * MAP ENTRY FUNCTIONS
	  		 */
	  		
	  		// Returns all entries belonging to a particular map group
	  		// TODO: rework for 2 groups; is this used?
	        /**  $scope.getEntries = function(mapGroup) {
	  		  
	  		  // if no argument, return all entries
	  		  if (mapGroup == null) {
	  			  return $scope.entries2.mapEntry;
	  		  }
	  		  
	  		  // cycle over map entries and extract those with this map group
	  		  var entries = new Array();
	  		  
	  		  for (var i = 0; i < $scope.record1.mapEntry.length; i++) {
	  			  if (parseInt($scope.record1.mapEntry[i].mapGroup, 10) === parseInt(mapGroup, 10)) {
	  				  entries.push($scope.record1.mapEntry[i]);
	  			  };
	  		  };
	  		  
	  		  return entries;  
	  	    }; */
	  	    
	  	    // Returns a summary string for the entry rule type
	  	    $scope.getRuleSummary = function(entry) {
	  			  if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
	  				  return "";
	  			  } else {
	  				  
	  				  if (entry.rule.toUpperCase().indexOf("GENDER") != -1) return "[GENDER]";
	  				  else if (entry.rule.toUpperCase().indexOf("FEMALE") != -1) return "[FEMALE]";
	  				  else if (entry.rule.toUpperCase().indexOf("MALE") != -1) return "[MALE]";
	  				  else if (entry.rule.toUpperCase().indexOf("AGE") != -1) return "[AGE]";
	  				  else if (entry.rule.toUpperCase().indexOf("TRUE") != -1) return "[TRUE]";
	  				  else return "";
	  			  } 	

	  		  };
	  		  
	  		// Sets the scope variable for the active entry
	  		  // TODO: get rid of this
	  		$scope.selectEntry = function(entry) {
	  			console.debug("Select entry");
	  			$rootScope.$broadcast('compareRecordsWidget.notification.changeSelectedEntry',{key: 'changeSelectedEntry', entry: angular.copy(entry), record: $scope.record1, project: $scope.project});  
	  	         
	  		};

	 
	  		/////////////////////////
	  		// Map Group Functions //
	  		/////////////////////////
	  		
	  		  // Retrieves groups from the existing entries
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
	  	      // Utility Functions //
	  	      ///////////////////////
	  	      
	  	      // sort and return an array by string key
			  function sortByKey(array, key) {
				    return array.sort(function(a, b) {
				        var x = a[key]; var y = b[key];
				        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
				    });
			  };


	    });
