'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap']);
var mapProjectAppDirectives = angular.module('mapProjectAppDirectives', ['ui.boostrap']);

var root_url = "${base.url}/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";

var testArray = [{"id":4,"name":"D"},{"id":2,"name":"B"},{"id":1,"name":"A"},{"id":3,"name":"C"}];


mapProjectAppControllers.run(function($rootScope) {
    $rootScope.userName = null;
    $rootScope.role = null;
    $rootScope.testArray =  [{"id":4,"name":"D"},{"id":2,"name":"B"},{"id":1,"name":"A"},{"id":3,"name":"C"}];
	
});


//////////////////////////////
// Navigation
//////////////////////////////	



mapProjectAppControllers.controller('LoginCtrl', ['$scope', '$rootScope', '$location', '$http',
	 function ($scope, $rootScope, $location, $http) {
	
	$scope.user = "";
	$scope.users = "";
	$scope.error = "";
	
	// retrieve users
	$http({
		 url: root_mapping + "user/users",
		 dataType: "json",
	        method: "GET",
	        headers: {
	          "Content-Type": "application/json"
	        }	
	      }).success(function(data) {
	    	  $scope.users = data.mapUser;
	      }).error(function(error) {
	    	  $scope.error = $scope.error + "Could not retrieve map users. "; 
	     
         });
	
	// logout icon returns to login page, so reinitialize
	$rootScope.userName = null;
	$rootScope.role = null;
	$rootScope.user = null;
	
	// initial values for pick-list
	 $scope.roles = [
	       {name:'Viewer', value:1},
	       {name:'Specialist', value:2},
	       {name:'Lead', value:3},
	       {name:'Administrator', value:4}];
	 $scope.role = $scope.roles[0];  
	 
	 // login button directs to next page based on role selected
	 $scope.go = function () {
		 
		 var path = "";
		 
		 if ($scope.role.name == "Specialist") {
			 path = "/specialist/dash";
		 } else if ($scope.role.name == "Lead") {
			 path = "/lead/dash";
		 } else if ($scope.role.name == "Administrator") {
			 path = "/admin/dash";
		 } else if ($scope.role.name == "Viewer") {
			 path = "/project/projects/";
		 }
		 
		 if ($scope.user == null) {
			 alert("You must specify a user");
		 } else {
			$rootScope.user = $scope.user;
			$rootScope.userName = $scope.user.name;
			$rootScope.role = $scope.role;
			
			do {
				if ($rootScope.userName === $scope.user.name) {
					$location.path(path);
				} else {
					console.debug('test');
				}
			} while ($rootScope.userName != $scope.user.name);
		 }
			
			
		 
	 };
 }]);


	
//////////////////////////////
// Mapping Services
//////////////////////////////	
	
mapProjectAppControllers.controller('MapProjectListCtrl', 
  function ($scope, $http) {
	
	  // initialize as empty to indicate still initializing database connection
	  $scope.projects = [];
	
      $http({
        url: root_mapping + "project/projects",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
    	  $scope.projects = data.mapProject;
      }).error(function(error) {
    	  $scope.error = "Error";
      });

  });



//////////////////////////////
// Content Services
//////////////////////////////	


  
//////////////////////////////
// Specialized Services
//////////////////////////////	


/*
 * Controller for retrieving and displaying records associated with a concept
 */
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams', '$modal', '$sce', '$rootScope', 
   function ($scope, $http, $routeParams, $modal, $sce, $rootScope) {
	
	// scope variables
	$scope.error = "";		// initially empty
	$scope.conceptId = $routeParams.conceptId;
	
	// local variables
	var records = [];
	var projects = [];
	
	// retrieve projects information   
	$http({
		 url: root_mapping + "project/projects",
		 dataType: "json",
	        method: "GET",
	        headers: {
	          "Content-Type": "application/json"
	        }	
	      }).success(function(data) {
	    	  $scope.projects = data.mapProject;
	          projects = data.mapProject;
	      }).error(function(error) {
	    	  $scope.error = $scope.error + "Could not retrieve projects. "; 
	     
          }).then(function() {
        	  $scope.getRecordsForConcept();
          });
	
	// function to return trusted html code (for tooltip content)
	$scope.to_trusted = function(html_code) {
	    return $sce.trustAsHtml(html_code);
	 };

    $scope.getRecordsForConcept = function() {
		// retrieve all records with this concept id
		$http({
	        url: root_mapping + "record/conceptId/" + $routeParams.conceptId,
	        dataType: "json",
	        method: "GET",
	        headers: {
	          "Content-Type": "application/json"
	        }	
	      }).success(function(data) {
	    	  $scope.records = data.mapRecord;
	          records = data.mapRecord;
	      }).error(function(error) {
	    	  $scope.error = $scope.error + "Could not retrieve records. ";    
	      }).then(function() {
      
	    	  // set terminology based on first map record's map project
	    	  var terminology = "null";
	    	  var version = "null";
	    	  
	    	  for (var i = 0; i < projects.length; i++) {
	    		  if (projects[i].id == records[0].mapProjectId) {
	    			  $scope.project = projects[i];
	    			  terminology = projects[i].sourceTerminology;
	    			  version = projects[i].sourceTerminologyVersion;
	    			  break;
	    		  }
	    	  }	  
	    	  
	    	// check relation style flags
			  if ($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE") {
				  console.debug("map category style detected");
				  applyMapCategoryStyle();
			  }
			  
			  if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
				  console.debug("Relationship Style detected");
				  applyRelationshipStyle();
			  }
	    	  
	    	  // find concept based on source terminology
	    	  $http({
    			 url: root_content + "concept/" 
    			 				   + terminology + "/" 
    			 				   + version 
    			 				   + "/id/" 
    			 				   + $routeParams.conceptId,
    			 dataType: "json",
    		     method: "GET",
    		     headers: {
    		          "Content-Type": "application/json"
    		     }	
    		  }).success(function(data) {
    		          $scope.concept = data;
    		          $scope.findUnmappedDescendants();
    		          
    		          // find inverse relationships based on source terminology
    		    	  $http({
    		    			 url: root_content + "concept/" 
    		    			 				   + terminology + "/" 
    		    			 				   + version 
    		    			 				   + "/id/" 
    		    			 				   + $routeParams.conceptId
    		    			 				   + "/children",
    		    			 dataType: "json",
    		    		     method: "GET",
    		    		     headers: {
    		    		          "Content-Type": "application/json"
    		    		     }	
    		    		  }).success(function(data) {
    		    			  	  console.debug(data);
    		    		          $scope.concept.children = data.searchResult;
    		    		  }).error(function(error) {
    		    		    	  $scope.error = $scope.error + "Could not retrieve Concept children. ";    
    		    		  });
    		  }).error(function(error) {
    			  console.debug("Could not retrieve concept");
    		    	  $scope.error = $scope.error + "Could not retrieve Concept. ";    
    		  });
	    	  
	    	  	
	      });
	};
	
		$scope.getProject = function(record) {
			for (var i = 0; i < projects.length; i++) {
				if (projects[i].id == record.mapProjectId) {
					return projects[i];
				}
			}
			return null;
		};
		
		$scope.getProjectFromName = function(name) {
			for (var i = 0; i < projects.length; i++) {
				if (projects[i].name === name) {
					return projects[i];
				}
			}
			return null;
		};
	
		$scope.getProjectName = function(record) {
			
			for (var i = 0; i < projects.length; i++) {
				if (projects[i].id == record.mapProjectId) {
					return projects[i].name;
				}
			}
			return null;
		};
		
		$scope.findUnmappedDescendants = function() {
			
			$scope.concept.unmappedDescendants = [];
			
			if ($scope.records.length > 0) {
				if ($scope.records[0].countDescendantConcepts < 11) {
					
					$http({
						  url: root_mapping + "concept/" 
						  		+ $scope.concept.terminology + "/"
						  		+ $scope.concept.terminologyVersion + "/"
						  		+ "id/" + $scope.concept.terminologyId + "/"
						  		+ "threshold/10",
						  dataType: "json",
						  method: "GET",
						  headers: {
							  "Content-Type": "application/json"
						  }
					  }).success(function(data) {
						 console.debug("  Found " + data.count + " unmapped descendants");
						 if (data.count > 0) $scope.unmappedDescendantsPresent = true;
						 $scope.concept.unmappedDescendants = data.searchResult;
					  });

				}
			}
			
		};
		
		// given a record, retrieves associated project's ruleBased flag
		$scope.getRuleBasedForRecord = function(record) {
			var project = $scope.getProject(record);
			return project.ruleBased;
				
		};
		
		function applyMapCategoryStyle() {
			 
			 // Cycle over all entries. If targetId is blank, show relationName as the target name
			 for (var i = 0; i < $scope.records.length; i++) {		 
				 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 
					 
					 if ($scope.records[i].mapEntry[j].targetId === "") {
						 $scope.records[i].mapEntry[j].targetName = "\"" + $scope.records[i].mapEntry[j].relationName + "\"";
	
					 }
				 }
			 }
		 };
		 
		function applyRelationshipStyle() {
			// Cycle over all entries. Add the relation name to the advice list
			 for (var i = 0; i < $scope.records.length; i++) {		 
				 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
					 if ($scope.records[i].mapEntry[j].targetId === "") {	 
						 // get the object for easy handling
						 var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;
						 
						 // add the serialized advice	
						 jsonObj.push({"id":"0", "name": "\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "detail":"\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "objectId":"0"});
						 
						 $scope.records[i].mapEntry[j].mapAdvice = jsonObj;
					 }
				 }
			 }
		 };
		  
		 $scope.createMapRecord = function(projectName) {
			 
			  if (!(projectName == null) && !(projectName === "")) {
				  
				  
				 
				  // get the project
				  var project = $scope.getProjectFromName(projectName);
				  var countDescendantConcepts;
				  
				  console.debug("PROJECT:");
				  console.debug(project);
				  
				  // find concept based on source terminology
		    	  $http({
	    			 url: root_content + "concept/" 
	    			 				   + project.sourceTerminology + "/" 
	    			 				   + project.sourceTerminologyVersion 
	    			 				   + "/id/" 
	    			 				   + $scope.conceptId,
	    			 dataType: "json",
	    		     method: "GET",
	    		     headers: {
	    		          "Content-Type": "application/json"
	    		     }	
	    		  }).success(function(data) {
	    		     
	    			  $scope.concept = data;
	    			  
	    		  }).then(function(data) {
	    			  
			    		  
						  // get descendant count
				    	  $http({
			    			 url: root_content + "concept/" 
			    			 				   + project.sourceTerminology + "/" 
			    			 				   + project.sourceTerminologyVersion 
			    			 				   + "/id/" 
			    			 				   + $scope.conceptId
			    			 				   + "/descendants",
			    			 dataType: "json",
			    		     method: "GET",
			    		     headers: {
			    		          "Content-Type": "application/json"
			    		     }	
			    		  }).success(function(data) {    
			    			  countDescendantConcepts = data.count;
			    		  }).error(function(data) {
			    			  countDescendantConcepts = 0;
			    		  }).then(function() {
			
					    	  // construct the map record
					    	  var record = {
					    			  "id" : "",
					    			  "mapProjectId" : project.id,
					    			  "conceptId" : $scope.concept.terminologyId,
					    			  "conceptName" : $scope.concept.defaultPreferredName,
					    			  "countDescendantConcepts": countDescendantConcepts,
					    			  "mapEntry": [],
					    			  "mapNote": [],
					    			  "mapPrinciple": []
					    	  };
					    	
					    	  console.debug("EMPTY RECORD");
					    	  console.debug(record);
					    	  
					    	  $scope.open (record);
			    		  });
	    		  });
			  };
    		  
		 };
 
}]);

/**
 * Controller for new test view (i.e. non-modal) for map record edit/create/delete functions
 */
mapProjectAppControllers.controller('MapRecordDetailCtrl', 
	['$scope', '$http', '$routeParams', '$sce', '$modal', '$rootScope',
                                                             
	 function ($scope, $http, $routeParams, $sce, $modal, $rootScope) {
		
		// initialize scope variables
		$scope.record = 	null;
		$scope.project = 	null;
		$scope.concept = 	null;
		$scope.groups = 	null;
		
		// initialize local variables
		var recordId = 		$routeParams.recordId; 
		var currentLocalId = 0;   // used for addition of new entries without hibernate id
		
		// obtain the record
		$http({
			 url: root_mapping + "record/id/" + recordId,
			 dataType: "json",
		        method: "GET",
		        headers: { "Content-Type": "application/json"}	
	      }).success(function(data) {
	    	  $scope.record = data;
	      }).error(function(error) {
	    	  $scope.error = $scope.error + "Could not retrieve map record. ";
	     
	      }).then(function() {

	    	  // obtain the record project
	    	 $http({
	 			 url: root_mapping + "project/id/" + $scope.record.mapProjectId,
	 			 dataType: "json",
	 		        method: "GET",
	 		        headers: { "Content-Type": "application/json"}	
		      }).success(function(data) {
	 		    	  $scope.project = data;
	 				  
	 		    	  // initialize the preset age ranges
	 				  initializePresetAgeRanges();
	 		    	  
		      }).error(function(error) {
	 		    	  $scope.error = $scope.error + "Could not retrieve map project. ";
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
	 		      }).error(function(error) {
	     		    	  $scope.error = $scope.error + "Could not retrieve record concept. ";
	 		      });
	        	 
		    	  
		    	  // get the groups
	        	  if ($scope.project.groupStructure == true)
	        		  getGroups();
		    	  
	          });
          });
		

		
		/**
		 * Utility functions
		 */  
		  
		  // function to add an element and assign a local id if not tracked by hibernate
		  Array.prototype.addElement = function(elem) {
			  
			  console.debug("addElement called");
			  
			  // if hibernate id, simply add
			  if (elem.id != null && elem.id != '') {
				  console.debug("addElement: hibernate id detected");
				  this.push(elem);
				  
			  // otherwise, assign a unique localid
			  } else {
				  
				  console.debug("addElement: Assigning local id");
				  
				  // get the maximum local id already assigned
				  var maxLocalId = -1;
				  $.map(this, function(v,i) {
					  console.debug("Checking element:");
					  console.debug(v);
					  if (v.hasOwnProperty("localId")) {
						 if (v['localId'] > maxLocalId) maxLocalId = v['localId'];
					  }
				  });

				  elem['localId'] = maxLocalId == -1 ? 1 : maxLocalId + 1;
			  	  console.debug(elem);
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
				  if (v[idType] === elem[idType]) array.push(v);
			  });

			  this.length = 0; //clear original array
			  this.push.apply(this, array); //push all elements except the one we want to delete
		  };
		  
		/**
		 * MAP RECORD FUNCTIONS
		 */
		$scope.saveMapRecord = function() {
			
			/*if ($rootScope.user.id == null || $rootScope.user.id === '') {
				alert("Global Error:  The current user is not set.  Please return to the log-in page to log in again.\n\nKNOWN BUG:  Reloading a browser window or navigating this site via external bookmarks can cause this error.");
			} else {
				$scope.record.ownerId = $rootScope.user.id;
			}*/
			
			$http({
				  url: root_mapping + "record/update",
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
			  }).error(function(data) {
				 $scope.recordSuccess = "";
				 $scope.recordError = "Error saving record.";
			  });
		};
		
		// on cancel, discard changes and requery database
		$scope.cancelMapRecord = function() {

			  $http({
				 url: root_mapping + "record/id/" + recordId,
				 dataType: "json",
			        method: "GET",
			        headers: { "Content-Type": "application/json"}	
		      }).success(function(data) {
		    	  $scope.record = data;
		    	  $scope.recordSuccess = "";
				  $scope.recordError = "Record changes aborted.";
		      }).error(function(error) {
		    	  $scope.error = $scope.error + "Could not retrieve map record. ";
		     
		      }).then(function() {
	       	 
		      	  // get the groups
		    	  getGroups();
		    	  
		    	  $scope.entry = null;
		      });	
		};
		
		$scope.deleteMapRecord = function() {
			var confirmDelete = confirm("Deleting this map record will also destroy the map entries attached to this record.\n\nAre you sure you want to delete this record?");
			if (confirmDelete == true) {
			
				$http({
					  url: root_mapping + "record/delete",
					  dataType: "json",
					  data: $scope.record,
					  method: "DELETE",
					  headers: {"Content-Type": "application/json"}
				  }).success(function(data) {
					 $scope.record = data;
					 console.debug("  Record updated");
				  }).error(function(data) {
					  console.debug("Existing record update ERROR");	  
				  });
			}
		};
		
		$scope.addRecordPrinciple = function(record, principle) {
			
			// check if principle valid
			if (principle === '') {
				$scope.errorAddRecordPrinciple = "Principle cannot be empty";
			} else if (principle == null) {
				$scope.errorAddRecordPrinciple = "This principle is not found in allowable principles for this map project";
			} else {
				$scope.errorAddRecordPrinciple = "";
				
				// check if principle already present
				var principlePresent = false;
				for (var i = 0; i < record.mapPrinciple.length; i++) {
					if (principle.id == record.mapPrinciple[i].id) principlePresent = true;
				}
				
				if (principlePresent == true) {
					$scope.errorAddRecordPrinciple = "The principle with id " + principle.principleId  + " is already attached to the map record";
				} else {
					$scope.record['mapPrinciple'].push(principle);
				};
			};
		};
		
		$scope.removeRecordPrinciple = function(record, principle) {
			record['mapPrinciple'].removeElement(principle);
			$scope.record = record;
		};
		
		$scope.addRecordNote = function(record, note) {
			// check if note non-empty
			if (note === '' || note == null) {
				$scope.errorAddRecordNote = "Note cannot be empty";
			} else {
				
				// construct Json user
				var mapUser = null;
				
				// construct note object
				var mapNote = new Array();
				mapNote.note = note;
				mapNote.timestamp = (new Date()).getMilliseconds();
				mapNote.user = mapUser;
				
				// add note to record
				record['mapNote'].addElement(mapNote);
				
				// set scope record to record
				$scope.record = record;
				
			}
		};
		
		$scope.removeRecordNote = function(record, note) {
			record['mapNote'].removeElement(note);
			$scope.record = record;
		};
		
		
	         
		
		/**
		 * MAP ENTRY FUNCTIONS
		 */
		
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
	    
	    // Returns a summary string for the entry rule type
	    $scope.getRuleSummary = function(entry) {
			  if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
				  return "";
			  } else {
				  
				  if (entry.rule.toUpperCase().indexOf("GENDER") != -1) return "[GENDER]";
				  if (entry.rule.toUpperCase().indexOf("AGE OF ONSET") != -1) return "[AGE OF ONSET]";
				  if (entry.rule.toUpperCase().indexOf("AGE") != -1) return "[AGE]";
				  if (entry.rule.toUpperCase().indexOf("TRUE") != -1) return "[TRUE]";
				  return "";
			  } 	

		  };
		  
		// Sets the scope variable for the active entry
		$scope.selectEntry = function(entry) {
			$scope.entry = angular.copy(entry);
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
					"mapPriority": $scope.getEntries(group).length + 1,
					"relationId":"",
					"relationName":"",
					"mapBlock":"",
					"mapGroup": group,
					"mapAdvice":[],
					"mapPrinciples":[],
			  		"localId": currentLocalId + 1
			  };
			  
			  $scope.record.mapEntry.push(newEntry);
			  $scope.selectEntry(newEntry);

		  };
		  
		  // Saves the selected entry to the map record
		  $scope.saveMapEntry = function(entry) {
				console.debug("saveMapEntry");
				
				var index = findEntryIndex(entry);
				if (index == -1) {
					alert("Fatal Error:  Entry could not be saved.\n\nThis entry does not belong to the current Map Record.");
					$scope.entrySuccess = "";
					$scope.entryError = "Error saving entry";
				} else {
					$scope.record.mapEntry[index] = entry;
					$scope.entrySuccess = "Entry saved.";
					$scope.entryError = "";
				}
			};
		  
		  // Cancels changes to the selected map entry
		  $scope.cancelMapEntry = function() {
			    $scope.entrySuccess = "";
				$scope.entryError = "";
		     	$scope.entry = null;
		  };
		  
		  // Deletes selected map entry
		  $scope.deleteMapEntry = function(entry) { 
			  console.debug("deleteMapEntry");
			  $scope.entrySuccess = "";
			  $scope.entryError = "";
			  
			  
			  var confirmDelete = confirm("Are you sure you want to delete this entry?");
			  if (confirmDelete == true) {
				  
				  	var entries = new Array();
				  	var index = findEntryIndex(entry);
				  	
				  	if (index == -1) {
				  		alert("Entry not found, cannot be deleted");
				  	} else {
				  		for (var i = 0; i < $scope.record.mapEntry.length; i++) {
				  			if (i != index) entries.push($scope.record.mapEntry[i]);
				  		}
				  		$scope.record.mapEntry = entries;
				  		$scope.entry = null;
				  	}
				  		
			  }
		  };
		  
		  $scope.addEntryAdvice = function(entry, advice) {
				
			  // check if advice valid
			  if (advice == '') {
				  $scope.errorAddAdvice = "Advice cannot be empty";
			  } else if (advice == null) {
				  $scope.errorAddAdvice = "This advice is not found in allowable advices for this project";
			  } else {
				  $scope.errorAddAdvice = "";
					 
				  console.debug("Add Entry Advice");
				  console.debug(entry);
				  console.debug(advice);
				  
				  // check if this advice is already present
				  var advicePresent = false;
				  for (var i = 0; i < entry.mapAdvice.length; i++) {
					  if (advice.id === entry.mapAdvice[i].id) advicePresent = true;
				  }
				  
				  if (advicePresent == true) {
					  $scope.errorAddAdvice = "This advice " + advice.detail + " is already attached to this entry";
				  } else {
					  $scope.entry['mapAdvice'].push(advice);
				  }
			  }
		  };
		  
		  $scope.removeEntryAdvice = function(entry, advice) {	  
			  	  console.debug("RemoveEntryAdvice()");
				  entry['mapAdvice'].removeElement(advice);
				  $scope.entry = entry;  
		  };
		  
			  
		  function findEntryIndex(entry) {
			  
			  console.debug("findEntryIndex");
			  console.debug($scope.record.mapEntry);
			  console.debug(entry);
			  
			  // check if entry has hibernate id
			  if (entry.id != null && entry.id != '') {
				  
				  console.debug("Has hibernate id");
				  
				  // cycle over entries until matching id found and return index
				  for (var i = 0; i < $scope.record.mapEntry.length; i++) {
					  if (entry.id === $scope.record.mapEntry[i].id) return i;
				  }
				  
			  // otherwise, check for entries with local id
			  } else {
				  
				  
				  console.debug("No hibernate id");
				  
				  for (var i = 0; i < $scope.record.mapEntry.length; i++) {
					  // if no hibernate id, skip this record, otherwise check by localId
					  if ($scope.record.mapEntry[i].id === null || $scope.record.mapEntry[i].id === '') {
						  
						console.debug(entry.localId);
						console.debug($scope.record.mapEntry[i].localId);
						if (entry.localId == $scope.record.mapEntry[i].localId) return i;
					}  
				  }
			  }
			  
			  console.debug("Not found");
			  
			  return -1;
		  };
		  
	    /**
	     * RULE CONSTRUCTION FUNCTIONS
	     */
		  
		  $scope.constructRule = function(entry) {
			  
			  $scope.openRuleConstructor();
		  };
		  
		  $scope.openRuleConstructor = function() {
			  
			  var modalInstance = $modal.open({
				  templateUrl: 'partials/rule-modal.html',
				  controller: RuleConstructorModalCtrl,
				  resolve: {
					  presetAgeRanges: function() {
						  return angular.copy($scope.project.presetAgeRanges);
					  }
				  }
			  });
			  
			  modalInstance.result.then(function(rule) {
				  console.debug("Rule Constructor Modal OK function");
				  console.debug(rule);
				  $scope.entry.rule = rule;
			  });
		  };
		  
		// set up the preset age range defaults
		var RuleConstructorModalCtrl = function($scope, $http, $modalInstance, presetAgeRanges) {
			
			$scope.ageRange={"name":"" , "lowerValue":"", "lowerInclusive":"", "lowerUnits":"", 
					  "upperValue":"", "upperInclusive":"", "upperUnits":""},

			$scope.presetAgeRanges = presetAgeRanges;
			$scope.ruleCategories = ['TRUE', 'Gender - Male', 'Gender - Female', 'Age - Chronological', 'Age - At Onset'];
			
			
			$scope.saveRule = function() {
				$modalInstance.close($scope.rule);
			};
			
			$scope.cancelRule = function() {
				$modalInstance.dismiss('cancel');
			};

			$scope.changeRuleCategory = function(ruleCategory) {
				console.debug("ChangeRuleCategory()");
				
				$scope.ageRange = null;
				$scope.constructRule(ruleCategory, null);
			};
			
			$scope.constructRule = function(ruleCategory, ageRange) {
				
				console.debug("constructRule() with " + ruleCategory);
				console.debug(ageRange);
				
				$scope.rule = "";
				
				if (ruleCategory === "TRUE") {
					console.debug("TRUE selected");
					$scope.rule = "TRUE";
				}
				
				else if (ruleCategory === "Gender - Male") {
					console.debug("ruleGenderMale selected");
					$scope.rule = "IFA 248153007 | Male (finding) |";
				}
				
				else if (ruleCategory === "Gender - Female") {
					console.debug("ruleGenderFemale selected");
					$scope.rule = "IFA 248152002 | Female (finding) |";
				}
				
				else if (ageRange != null) {
					
					if (ruleCategory === "Age - Chronological") {
						console.debug("ruleAge selected");

						if (ageRange.lowerValue != "-1") {
							$scope.rule += "IFA 424144002 | Current chronological age (observable entity)"
										+  " | " + (ageRange.lowerInclusive == true ? ">=" : ">") + " "
										+  ageRange.lowerValue + " "
										+  ageRange.lowerUnits;
						}
						
						if (ageRange.lowerValue != "-1" && ageRange.upperValue != "-1")
							$scope.rule += " | ";
						
						if (ageRange.upperValue != "-1") {
							$scope.rule += "IFA 424144002 | Current chronological age (observable entity)"
										+  " | " + (ageRange.upperInclusive == true ? "<=" : "<") + " "
										+  ageRange.upperValue + " "
										+  ageRange.upperUnits;
						}			
					} else if (ruleCategory === "Age - At Onset") {
						console.debug("ruleAgeAtOnset selected");
						$scope.rule = "IFA 445518008 | Age at onset of clinical finding (observable entity)";
						if (ageRange.upperValue != "-1") {
							$scope.rule += " | " + (ageRange.lowerInclusive == true ? ">=" : ">") + " "
										+  ageRange.lowerValue + " "
										+  ageRange.lowerUnits;
						}
					}
				} else $scope.rule = null;
			};
			
			$scope.constructRuleAgeHelper = function(ruleCategory, ageRange) {
				$scope.constructRule($scope.ruleCategory);
			};
			
		};
		
		function initializePresetAgeRanges() {  
			  $scope.presetAgeRanges = $scope.project.presetAgeRanges;
			  
			  // set the preset age range strings
			  for (var i = 0; i < $scope.presetAgeRanges.length; i++) {
				  var presetAgeRangeStr = $scope.presetAgeRanges[i].name + ", ";
				  
				  if ($scope.presetAgeRanges[i].lowerValue != null && $scope.presetAgeRanges[i].lowerValue != "-1") {
					  presetAgeRangeStr += ($scope.presetAgeRanges[i].lowerInclusive == true ? ">=" : ">") + " "
					  					+  $scope.presetAgeRanges[i].lowerValue + " "
					  					+  $scope.presetAgeRanges[i].lowerUnits;
				  }
				  
				  if ($scope.presetAgeRanges[i].lowerValue != null && $scope.presetAgeRanges[i].lowerValue != "-1" &&
					  $scope.presetAgeRanges[i].upperValue != null && $scope.presetAgeRanges[i].upperValue != "-1") {
					  
					  presetAgeRangeStr += " and ";
				  }
				  
				  if ($scope.presetAgeRanges[i].upperValue != null && $scope.presetAgeRanges[i].upperValue != "-1") {
					  
					  presetAgeRangeStr += ($scope.presetAgeRanges[i].upperInclusive == true ? "<=" : "<") + " "
					  					+  $scope.presetAgeRanges[i].upperValue + " "
					  					+  $scope.presetAgeRanges[i].upperUnits;
				  }
				  
				  $scope.presetAgeRanges[i].stringName = presetAgeRangeStr;
			  };
		  };
		  
				
	
		
		/** 
		 * MAP GROUP FUNCTIONS
		 */
		
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
	    	  var groupAdded = 0;
	    	  
	    	  // first attempt to "fill in" the first possible gap in groups
	    	  for (var i = 0; i < $scope.groups.length; i++) {
	    		  if (i+1 < $scope.groups[i]  && groupAdded == false) {
	    			  groupAdded = i+1;
	    			  $scope.groups.push(groupAdded);
	    		  }
	    	  }
	    	  
	    	  // if no group filled in, add to end
	    	  if (groupAdded == 0) {
	    		  groupAdded = $scope.groups.length + 1;
	    		  $scope.groups.push(groupAdded);
	    	  }
	      };
	      
	      // Function to validate a new map group (i.e. add it to existing list if not present)
	      $scope.validateMapGroup = function(group) {
	    	  console.debug($scope.groups);
	    	  if ($scope.groups.indexOf(group) == -1) {
	    		  console.debug("Group " + group + " not in group list");
	    		  $scope.groups.push(parseInt(group));
	    		  $scope.groups.sort();
	    		  console.debug("New group list: ");
	    		  console.debug($scope.groups);
	    	  }
	      };
	      
	      // Removes a map group if it exists
	      $scope.removeMapGroup = function(group) {   	  
	    	  var newGroups = new Array();
	    	  for (var i = 0; i < $scope.groups.length; i++) {
	    		  if ($scope.groups[i] != group) newGroups.push($scope.groups[i]);
	    	  }
	    	  $scope.groups = newGroups;
	      };
	      
	      
	      /**
	       * ENTRY EDITING FUNCTIONS
	       */
	      $scope.retrieveTargetConcepts = function(query) {
	    	  
	    	  // execute query for concepts
	    	  // TODO Change query format to match records
	    	  $http({
	    			 url: root_content + "concept/query/" + query,
	    			 dataType: "json",
	    		     method: "GET",
	    		     headers: {
	    		          "Content-Type": "application/json"
	    		     }	
	    		  }).success(function(data) {
	    			  
	    			  console.debug(data);
	    		     
	    			  // eliminate concepts that don't match target terminology
	    			  
	    			  $scope.targetConcepts = [];
	    			  
	    			  for (var i = 0; i < data.count; i++) {
	    				  if (data.searchResult[i].terminology === $scope.project.destinationTerminology &&
	    					  data.searchResult[i].terminologyVersion === $scope.project.destinationTerminologyVersion) {
	    					
	    					  $scope.targetConcepts.push(data.searchResult[i]);
	    				  };
	    			  };
	    			  
	    			  

	    		  }).error(function(data) {
	    			  $scope.errorCreateRecord = "Failed to retrieve entries";
	    		  });
	      };
	      
	      $scope.resetTargetConcepts = function() {
	    	  console.debug("resetTargetConcepts() called");
	    	  $scope.queryTarget = "";
	    	  $scope.targetConcepts = [];
	      };
	      
	      $scope.selectTargetConcept = function(entry, target) {
	    	  console.debug("selectTargetConcept() called");
	    	  console.debug(target);
	    	  entry.targetId = target.terminologyId;
	    	  entry.targetName = target.value;
	    	  $scope.resetTargetConcepts();
	      };
	      
	      
		  
		  
		
	}]);

                                                    
/**
 * Controller for Map Project Records view
 * 
 * Basic function:
 * 1) Retrieve map project
 * 2) Retrieve concept associated with map project refsetid
 * 3) Retrieve records
 * 
 * Scope functions (accessible from html)
 * - $scope.resetSearch 		clears query and launches unfiltered record retrieval request
 * - $scope.retrieveRecords 	launches new record retrieval request based on current paging/filtering/sorting parameters
 * - $scope.to_trusted	 		converts unsafe html into usable/displayable code
 *  
 * Helper functions: 
 * - setPagination				sets the relevant pagination attributes based on current page
 * - getNRecords				retrieves the total number of records available given filtering parameters
 * - getUnmappedDescendants		retrieves the unmapped descendants of a concept
 * - applyMapCategoryStyle		modifies map entries for display based on MAP_CATEGORY_STYLE
 * - applyRelationshipStyle		modifies map entries for display based on RELATIONSHIP_STYLE
 * - constructPfsParameterObj	creates a PfsParameter object from current paging/filtering/sorting parameters, consumed by RESTful service
 */

mapProjectAppControllers.controller('MapProjectRecordCtrl', ['$scope', '$http', '$routeParams', '$sce', '$rootScope',
   function ($scope, $http, $routeParams, $sce, $rootScope) {
	
		// header information (not used currently)
		$scope.headers = [
		                  {value: 'conceptId', title: 'Concept'},
		                  {value: 'conceptName', title: 'Concept Name'},
		                  {value: 'mapGroup', title: 'Group Id'},
		                  {value: 'mapPriority', title: 'Map Priority'},
		                  {value: 'rule', title: 'Rule'},
		                  {value: 'targetId', title: 'Target'},
		                  {value: 'targetName', title: 'Target Name'},
		                  {value: 'actions', title: 'Actions'}
		                  ];
		
		// header sorting criteria initialization (not used currently)
		$scope.filterCriteria = {
				pageNumber: 1,
				sortDir: 'asc',
				sortedBy: 'conceptId'
		};
	
	  // the project id, extracted from route params
	  $scope.projectId = $routeParams.projectId;
	  
	  // status variables
	  $scope.unmappedDescendantsPresent = false;
	  $scope.mapNotesPresent = false;
	  $scope.mapAdvicesPresent = false;
	  
	  // error variables
	  $scope.errorProject = "";
	  $scope.errorConcept = "";
	  $scope.errorRecords = "";
	  
	  // pagination variables
	  $scope.recordsPerPage = 10;
	  
	  // for collapse directive
	  $scope.isCollapsed = true;
	  
	  // retrieve project information
	  $http({
        url: root_mapping + "project/id/" + $scope.projectId,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.project = data;
        $scope.errorProject = "Project retrieved";
      }).error(function(error) {
    	  $scope.errorProject = "Could not retrieve project"; 
      }).then(function(data) {

 		 
    	  // retrieve any concept associated with this project
    	  $http({
    		  url: root_content + "concept/id/" + $scope.project.refSetId,
    		  dataType: "json",
    		  method: "GET",
    		  headers: {
    			  "Content-Type": "application/json"
    		  }
    	  }).success(function(data) {
    		  $scope.concept = data;
    	  }).error(function(error) {
    		  $scope.errorConcept= "Error retrieving concept";
    	  });
      }).then(function(data) {
    	  
    	  // load first page
    	  $scope.retrieveRecords(1);
      });
	 
	 // Constructs trusted html code from raw/untrusted html code
	 $scope.to_trusted = function(html_code) {
	    return $sce.trustAsHtml(html_code);
	 };
	 
	 // function to clear input box and return to initial view
	 $scope.resetSearch = function() {
		 $scope.query = null;
		 $scope.retrieveRecords(1);
	 };
    	
	 // function to retrieve records for a specified page
	 $scope.retrieveRecords = function(page) {
		 
		 // retrieve pagination information for the upcoming query
		 setPagination($scope.recordsPerPage);
		
		 console.debug("Switching to page " + page);
		/*
		   var query_url;
		   var startRecord = (page - 1) * $scope.recordsPerPage;
		   if ($scope.query == null) {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/" + startRecord + "-" + $scope.recordsPerPage;
		 } else {
			 query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/" + startRecord + "-" + $scope.recordsPerPage + "/" + $scope.query;
		 }*/
		 // construct html parameters parameter
		 var pfsParameterObj = constructPfsParameterObj(page);
		 var query_url = root_mapping + "record/projectId/" + $scope.project.objectId;
		 
		 console.debug("Retrieve records");
		 console.debug(query_url);
		 console.debug(pfsParameterObj);
		 
		// retrieve map records
		  $http({
			  url: query_url,
			  dataType: "json",
			  data: pfsParameterObj,
			  method: "POST",
			  headers: {
				  "Content-Type": "application/json"
			  }
		  }).success(function(data) {
			  $scope.records = data.mapRecord;
			  $scope.statusRecordLoad = "";
			  $scope.recordPage = page;

		  }).error(function(error) {
			  $scope.errorRecord = "Error retrieving map records";
			  console.debug("changeRecordPage error");
		  }).then(function(data) {
			  
			  // check if icon legends are necessary
			  $scope.unmappedDescendantsPresent = false;
			  $scope.mapNotesPresent = false;
			  $scope.mapAdvicesPresent = false;
			  
			  // check if any notes or advices are present
			  for (var i = 0; i < $scope.records.length; i++) {
				  if ($scope.records[i].mapNote.length > 0) {
					  $scope.mapNotesPresent = true;
				  }
				  for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {
					  
					  console.debug("  Checking entry "+ j);
					  if ($scope.records[i].mapEntry[j].mapNote.length > 0) {
						  $scope.mapNotesPresent = true;
					  };
					  if ($scope.records[i].mapEntry[j].mapAdvice.length > 0) {
						  $scope.mapAdvicesPresent = true;
					  }
				  };
			  };
			  
			  console.debug("Checking map category styles");
			  
			  // check relation syle flags
			  if ($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE") {
				  console.debug("map category style detected");
				  applyMapCategoryStyle();
			  }
			  
			  if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") {
				  console.debug("Relationship Style detected");
				  applyRelationshipStyle();
			  }
			  
			  console.debug("Checking map type");
					 			  
			  
			  // get unmapped descendants (checking done in routine)
			  if ($scope.records.length > 0) {	
				  getUnmappedDescendants(0);
			  }
		  });
	 }; 
	 
	 // Constructs a paging/filtering/sorting parameters object for RESTful consumption
	 function constructPfsParameterObj(page) {
		
			 return {"startIndex": (page-1)*$scope.recordsPerPage,
			 	 	 "maxResults": $scope.recordsPerPage, 
			 	 	 "sortField": null, // TODO: Replace this when sorting functional
			 	 	 "filterString": $scope.query == null ? null : $scope.query};  // assigning simply to $scope.query when null produces undefined
		 
	 }
	 
	 // function to set the relevant pagination fields
	 function setPagination(recordsPerPage) {
		 
		 // set scope variable for total records
		 getNRecords();
				 
		 // set pagination variables
		 $scope.recordsPerPage = recordsPerPage;
		 $scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);
	 };
	 
	 // function query for the number of records associated with full set or query
	 function getNRecords() {
		 
		 var query_url = root_mapping + "record/projectId/" + $scope.project.objectId + "/nRecords";
		 var pfsParameterObj = constructPfsParameterObj(1);
		 
		 console.debug("getNRecords");
		 console.debug(pfsParameterObj);
		 
		 // retrieve the total number of records associated with this map project
		 $http({
			  url: query_url,
			  dataType: "json",
			  data: pfsParameterObj,
			  method: "POST",
			  headers: {
				  "Content-Type": "application/json"
			  }
	   	  }).success(function(data) {
	   		  console.debug("getNRecords returned " + data);
	   		  $scope.nRecords = data;
	   		  
	   	  }).error(function(error) {
	   		  $scope.nRecords = 0;
	   		  console.debug("getNRecords error");
	   	  });
	 };
	 
	 /* For sorting, not currently used
	  *  $scope.onSort = function (sortedBy, sortDir) {
			 $scope.filterCriteria.sortDir = sortDir;
			 $scope.filterCriteria.sortedBy = sortedBy;
			 $scope.filterCriteria.pageNumber = 1;
		 };
	*/

		 
	// TODO This is inefficient, consider implementing a batch request
    function getUnmappedDescendants(index) {
				  
    	  // before processing this record, make call to start next async request
    	  if (index < $scope.records.length-1) {
		  	   getUnmappedDescendants(index+1);
		  }
    	  
		  $scope.records[index].unmappedDescendants = [];
		  
		  // if descendants below threshold for lower-level concept, check for unmapped
		  if ($scope.records[index].countDescendantConcepts < 11) {

			  $http({
				  url: root_mapping + "concept/" 
				  		+ $scope.project.sourceTerminology + "/"
				  		+ $scope.project.sourceTerminologyVersion + "/"
				  		+ "id/" + $scope.records[index].conceptId + "/"
				  		+ "threshold/10",
				  dataType: "json",
				  method: "GET",
				  headers: {
					  "Content-Type": "application/json"
				  }
			  }).success(function(data) {
				 if (data.count > 0) $scope.unmappedDescendantsPresent = true;
				 $scope.records[index].unmappedDescendants = data.searchResult;
			  });
		  } 
	  
	 };
	 
	 function applyMapCategoryStyle() {
		 
		 // set the category display text
		 $scope.mapRelationStyleText = "Map Category Style";
		 
		 // Cycle over all entries. If targetId is blank, show relationName as the target name
		 for (var i = 0; i < $scope.records.length; i++) {		 
			 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 
				 
				 if ($scope.records[i].mapEntry[j].targetId === "") {
					 $scope.records[i].mapEntry[j].targetName = "\"" + $scope.records[i].mapEntry[j].relationName + "\"";
				 }
			 }
		 }
	 };
	 
	function applyRelationshipStyle() {
		
		$scope.mapRelationStyleText = "Relationship Style";
		
		// Cycle over all entries. Add the relation name to the advice list
		 for (var i = 0; i < $scope.records.length; i++) {		 
			 for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {		 	 
				 if ($scope.records[i].mapEntry[j].targetId === "") {	 
					 // get the object for easy handling
					 var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;
					 
					 // add the serialized advice	
					 jsonObj.push({"id":"0", "name": "\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "detail":"\"" + $scope.records[i].mapEntry[j].mapRelationName + "\"", "objectId":"0"});
					 
					 $scope.records[i].mapEntry[j].mapAdvice = jsonObj;
				 }
			 }
		 }
	 };
}]);

mapProjectAppControllers.controller('MapProjectDetailCtrl', 
		['$scope', '$http', '$routeParams', '$sce', '$rootScope',
		 function ($scope, $http, $routeParams, $sce, $rootScope) {
			
			
			console.debug("In MapProjectDetailCtrl");
			
			// retrieve project information
			 $http({
		        url: root_mapping + "project/id/" + $routeParams.projectId,
		        dataType: "json",
		        method: "GET",
		        headers: {
		          "Content-Type": "application/json"
		        }	
		      }).success(function(data) {
		        $scope.project = data;
		        $scope.errorProject = "Project retrieved";
		      }).error(function(error) {
		    	  $scope.errorProject = "Could not retrieve project"; 
		     
		      }).then(function(data) {
		    	  
		    	  // apply map type text styling
		    	  if ($scope.project.mapType === "SIMPLE_MAP") $scope.mapTypeText = "Simple Mapping";
				  else if ($scope.project.mapType === "COMPLEX_MAP") $scope.mapTypeText = "Complex Mapping";
				  else if($scope.project.mapType === "EXTENDED_MAP") $scope.mapTypeText = "Extended Mapping";
				  else $scope.mapTypeText = "No mapping type specified";
		    	  
		    	  // apply relation style text styling
		    	  console.debug($scope.project.mapRelationStyle);
		    	  console.debug($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE");
		    	  if ($scope.project.mapRelationStyle === "MAP_CATEGORY_STYLE") $scope.mapRelationStyleText = "Map Category Style";
		    	  else if ($scope.project.mapRelationStyle === "RELATIONSHIP_STYLE") $scope.mapRelationStyleText = "Relationship Style";
		    	  else $scope.mapRelationStyleText = "No relation style specified";
		    	 
		    	 
		    	  // determine if this project has a principles document
		    	  // TODO: THIS WILL BE CODED INTO PROJECT LATER
		    	  if ($scope.project.destinationTerminology == "ICD10") {
		    		  $scope.project.mapPrincipleDocumentPath = "doc/";
		    		  $scope.project.mapPrincipleDocument = "ICD10_MappingPersonnelHandbook.docx";
		    		  $scope.project.mapPrincipleDocumentName = "Mapping Personnel Handbook";
		    	  } else {
		    		  $scope.project.mapPrincipleDocument = null;
		    	  }

		    	  // set pagination variables
		    	  $scope.pageSize = 5;
		    	  $scope.getPagedAdvices(1);
		    	  $scope.getPagedPrinciples(1);
		    	  $scope.orderProp = 'id';
		      
		      });
			
		
			
			// function to return trusted html code (for tooltip content)
			$scope.to_trusted = function(html_code) {
			    return $sce.trustAsHtml(html_code);
			 };
			 
			 ///////////////////////////////////////////////////////////////
			 // Functions to display and filter advices and principles
			 // NOTE: This is a workaround due to pagination issues
			 ///////////////////////////////////////////////////////////////
			 
			 // get paged functions
			 // - sorts (by id) filtered elements
			 // - counts number of filtered elmeents
			 // - returns artificial page via slice
			 
			 $scope.getPagedAdvices = function (page) {
				 console.debug("Called paged advice for page " + page);				 
				 $scope.pagedAdvice = $scope.sortByKey($scope.project.mapAdvice, 'id')
				 								.filter(containsAdviceFilter);
				 $scope.pagedAdviceCount = $scope.pagedAdvice.length;
				 $scope.pagedAdvice = $scope.pagedAdvice
				 								.slice((page-1)*$scope.pageSize,
				 										page*$scope.pageSize);
			 };
			 
			 $scope.getPagedPrinciples = function (page) {
				 console.debug("Called paged principle for page " + page);
				 $scope.pagedPrinciple = $scope.sortByKey($scope.project.mapPrinciple, 'id')
												.filter(containsPrincipleFilter);
				 $scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
				 $scope.pagedPrinciple = $scope.pagedPrinciple
												.slice((page-1)*$scope.pageSize,
														page*$scope.pageSize);
				 
				 console.debug($scope.pagedPrinciple);
			 };
			 
			 // functions to reset the filter and retrieve unfiltered results
			 
			 $scope.resetAdviceFilter = function() {
				 $scope.adviceFilter = "";
				 $scope.getPagedAdvices(1);
			 };
			 
			 $scope.resetPrincipleFilter = function() {
				 $scope.principleFilter = "";
				 $scope.getPagedPrinciples(1);
			 };
			 
			 
			 // element-specific functions for filtering
			 // don't want to search id or objectId
			 
			 function containsAdviceFilter(element) {
				 
				 // check if advice filter is empty
				 if ($scope.adviceFilter === "" || $scope.adviceFilter == null) return true;
				 
				 // otherwise check if upper-case advice filter matches upper-case element name or detail
				 if ( element.detail.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.name.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;
				 
				 // otherwise return false
				 return false;
			 }
			 
			 function containsPrincipleFilter(element) {
				 
				// check if principle filter is empty
				 if ($scope.principleFilter === "" || $scope.principleFilter == null) return true;
				 
				 // otherwise check if upper-case principle filter matches upper-case element name or detail
				 if ( element.principleId.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.detail.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.name.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 if ( element.sectionRef.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
				 
				 // otherwise return false
				 return false;
			 }
			 
		
			 
			 // helper function to sort a JSON array by field
			 
			 $scope.sortByKey = function sortById(array, key) {
				    return array.sort(function(a, b) {
				        var x = a[key]; var y = b[key];
				        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
				    });
				};
				
				
			////////////////////////////////////////////////////////
			// END PRINCIPLE/ADVICE SORTING/FILTERING FUNCTIONS
			////////////////////////////////////////////////////////
}]);






//////////////////////////////////////////////////////
// Directives:  TODO Separate into different file
/////////////////////////////////////////////////////


mapProjectAppControllers.directive('otfHeaderDirective', ['$rootScope', function() {
    
	return {
        templateUrl: './partials/header.html',
		restrict: 'E', 
        transclude: true,    // allows us �swap� our content for the calling html
        replace: true,        // tells the calling html to replace itself with what�s returned here
        link: function(scope, element, attrs) { // to get scope, the element, and its attributes
          scope.user = $rootScope.user; 
        }
    };
}]);

mapProjectAppControllers.directive('otfFooterDirective', function() {
    
	return {
        templateUrl: './partials/footer.html',
		restrict: 'E', 
        transclude: true,    // allows us �swap� our content for the calling html
        replace: true,        // tells the calling html to replace itself with what�s returned here
        link: function(scope, element, attrs) { // to get scope, the element, and its attributes
          scope.user = $rootScope.user; 
        }
    };
});

mapProjectAppControllers.directive('otfEntry', function() {
    
	return {
        templateUrl: './partials/mapEntry.html',
		restrict: 'E',
		scope: true,
		link: function(scope, element, attrs) {
			console.debug('otfEntry!');
		}
	
       
    };
});

mapProjectAppControllers
	.controller('RecordCreateCtrl', function($scope) {
			
			
			$scope.mapEntry =  {
					"mapRecordId": 1,
					"targetId":"testTarget",
					"targetName":"testTargetName",
					"rule":"RULE",
					"mapPriority":"1",
					"relationId":"",
					"relationName":"",
					"mapBlock":"1",
					"mapGroup":"1",
					"mapAdvice":[],
					"mapPrinciples":[]
			  };
			console.debug($scope.mapEntry);
			
		});




