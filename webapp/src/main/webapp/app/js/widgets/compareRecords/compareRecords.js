
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
	  		
	  		$scope.leadRecord = null;
	  		
	  		// initialize accordion variables
	  		$scope.isConceptOpen = true;
	  		$scope.isEntriesOpen = true;
	  		$scope.isPrinciplesOpen = true;
	  		$scope.isNotesOpen = true;
	  		$scope.isReportOpen = true;
	  		
	  		
	  		// broadcast page to help mechanism  
	  		$rootScope.$broadcast('localStorageModule.notification.page',{key: 'page', newvalue: 'resolveConflictsDashboard'});  
	  		
	  		// initialize local variables
	  		var leadRecordId=		$routeParams.recordId;
	  		var currentLocalId = 0;   // used for addition of new entries without hibernate id
	  		// TODO get from focus project
	  		var project = 1;
	  		// get leadRecord
		  	$http({
	  		  	url: root_mapping + "record/id/" + leadRecordId,
	  		  	dataType: "json",
	  		  	method: "GET",
	  		  	headers: { "Content-Type": "application/json"}	
	  		  }).success(function(data) {
	  		  	    	  $scope.leadRecord = data;
	  		  }).error(function(error) {
	  		  	    	  $scope.error = $scope.error + "Could not retrieve map record. "; 
 	          }).then(function() {
  	    	  // obtain the record project
	  	    	 $http({
	  	 			 url: root_mapping + "project/id/" + project,
	  	 			 dataType: "json",
	  	 		        method: "GET",
	  	 		        headers: { "Content-Type": "application/json"}	
	  		      }).success(function(data) {
	  	 		    	  $scope.project = data;
	  		      }).error(function(error) {
	  	 		    	  $scope.error = $scope.error + "Could not retrieve map project. ";
    
	  	          }).then(function() {
	  	      	  // obtain the record concept - id from leadRecord
	  		          $http({
	  		     			 url: root_content + "concept/" 
	  		 				  		+ $scope.project.sourceTerminology + "/"
	  						  		+ $scope.project.sourceTerminologyVersion + "/"
	  						  		+ "id/" + conceptId,
	  						  	dataType: "json",
	  		     		        method: "GET",
	  		     		        headers: { "Content-Type": "application/json"}	
	  		 		      }).success(function(data) {
	  		     		    	  $scope.concept = data;
	  		     		    	  setTitle($scope.concept.terminologyId, $scope.concept.defaultPreferredName);
	  		 		      }).error(function(error) {
	  		     		    	  $scope.error = $scope.error + "Could not retrieve record concept. ";
	  		 		     	  	

	  		  	      }).then(function() {
	  	  		  		// obtain the records associated with the concept
  	  		  	    	  // TODO change this call to call getRecordsInConflict()
	  	  		  		$http({
	  	  		  			 url: root_mapping + "record/conceptId/" + conceptId,
	  	  		  			 dataType: "json",
 	  	  		  		        method: "GET",
	  	  		  		        headers: { "Content-Type": "application/json"}	
	  	  		  	      }).success(function(data) {
	  	  		  	    	  $scope.record1 = data.mapRecord[0];
	  	  		  	    	  $scope.record2 = data.mapRecord[1];
	  	  		  	      }).error(function(error) {
	  	  		  	    	  $scope.error = $scope.error + "Could not retrieve map record. ";
	  	  		  	      }).then(function() {
		  	  		  		// obtain the validationResults from compareRecords
		  	  		  		$http({
		  	  		  			 url: root_mapping + "record/compare/" + record1.id + "/" + record2.id,
		  	  		  			 dataType: "json",
		  	  		  		        method: "GET",
		  	  		  		        headers: { "Content-Type": "application/json"}	
		  	  		  	      }).success(function(data) {
		  	  		  	    	  $scope.validationResult = data;
		  	  		  	      }).error(function(error) {
		  	  		  	    	  $scope.error = $scope.error + "Could not retrieve comparison report. ";   		  	      
		  	  		  	      });
	  	  		  	      });
	  		    	  
	  		    	  // get the groups
	  	        	  if ($scope.project.groupStructure == true)
	  	        		  getGroups();
	  	        	  
	  	        	  // initialize the entries
	  		    	  initializeEntries();
	  	          });
	            });
 	          });
	  		

	  		///////////////////////////////
	  		// Initialization Functions ///
	  		///////////////////////////////
	  		
	  		// construct an object containing entries, either:
	  		// 1) a 1-d array, if project has no group structure
	  		// 2) a 2-d array, with structure [group][mapPriority]
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

			  function setTitle(id, term) {
				    $scope.model.title = "Compare Records: " + id + "  " + term;
			  };
			  
		  	  $scope.populateMapRecord = function(record) {
		  		  // save id
		  		  // angular.copy record to leadRecord 
		  		  // replace id
				  $location.path("/record/conflicts/" + record.id);	
			  };

	    });
