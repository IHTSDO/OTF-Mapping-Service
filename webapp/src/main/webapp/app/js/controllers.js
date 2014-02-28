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



mapProjectAppControllers.controller('LoginCtrl', 
	 function ($scope, $rootScope, $location) {
	
	
	$scope.testArray = [{"id":4,"name":"D"},{"id":2,"name":"B"},{"id":1,"name":"A"},{"id":3,"name":"C"}];
	
	
	
	// logout icon returns to login page, so reinitialize
	$rootScope.userName = null;
	$rootScope.role = null;
	
	// initial values for pick-list
	 $scope.roles = [
	       {name:'Viewer', value:1},
	       {name:'Specialist', value:2},
	       {name:'Lead', value:3},
	       {name:'Administrator', value:4}];
	 $scope.role = $scope.roles[0];  
	 
	 // login button directs to next page based on role selected
	 $scope.go = function (path) {
		 if ($scope.role.name == "Specialist") {
			 path = "/specialist/dash";
		 } else if ($scope.role.name == "Lead") {
			 path = "/lead/dash";
		 } else if ($scope.role.name == "Administrator") {
			 path = "/admin/dash";
		 } else if ($scope.role.name == "Viewer") {
			 path = "/project/projects/";
		 }
			$location.path(path);
			$rootScope.userName = $scope.userName;
			$rootScope.role = $scope.role;
		};
	 });


	
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
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams', '$modal', '$sce',
   function ($scope, $http, $routeParams, $modal, $sce) {
	
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
		 
		 // function to remove an element by field 'id', applied to array
		 Array.prototype.removeElementById = function(id) {
			  
			  var array = $.map(this, function(v,i){
			      return v['id'] === id ? null : v;
			   });
			   this.length = 0; //clear original array
			   this.push.apply(this, array); //push all elements except the one we want to delete
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
		 
		 $scope.open = function (record) {

		    var modalInstance = $modal.open({
		      templateUrl: 'partials/modal-record.html',
		      controller: ModalRecordInstanceCtrl,
		      resolve: {
		        args: function () {
		        	
		        	var args = [];
		        	
		        	args.push(record);
		        	args.push($scope.getProject(record));
		        	args.push($scope.concept);
		        	
		        	console.debug("INITIAL RECORD:");
		        	console.debug(record);
		        
		        	
		        	// pass the record
		        	return angular.copy(args);
		        }
		    
		      }
		    	

		    });
		    
		    modalInstance.result.then(function (record) {
		    	console.debug("Modal ok function");
		    	
		    	
		    ////////////////////////////////////////////////////////////////////
		    // CHECK FOR DELETE
		    // - requires id and empty entry set
		    // - a delete request for a record with no id and empty entry set 
		    //   refers to a record that does not exist in DB
		    ////////////////////////////////////////////////////////////////////
		    	
		    if (!(record.id === "") && record.mapEntry.length == 0) {
	    		
	    		console.debug("DELETE RECORD");
		    	console.debug(record);

    			$http({
					  url: root_mapping + "record/delete",
					  dataType: "json",
					  data: record,
					  method: "DELETE",
					  headers: {
						  "Content-Type": "application/json"
					  }
				  }).success(function(data) {
					 console.debug("  Record deleted");
				  }).error(function(data) {
					  console.debug("Delete record ERROR");	  
				  }).then(function(data) {
					  // update the record in scope
					  var newRecords = [];
					  for (var i = 0; i < $scope.records.length; i++) {
						  if (!(record.id === $scope.records[i].id)) {
							  newRecords.push($scope.records[i]);
						  };
					  }
					  $scope.records = newRecords;
				  });
		  
			    	
	    	///////////////////////////////////////////////
		    // CHECK FOR ADD
		    // - requires no id and non-empty entry set
		    // - a record with no id and empty entry set will
		    //   not be added (shouldn't pass save test)
		    ///////////////////////////////////////////////	
		    
	    	// if no id and entries not empty, ADD this record
		    } else if (record.id === "" && record.mapEntry.length > 0) {
	    		console.debug("ADD RECORD");
		    	console.debug(record);
	    		
	    		$http({
					  url: root_mapping + "record/add",
					  dataType: "json",
					  data: record,
					  method: "PUT",
					  headers: {
						  "Content-Type": "application/json"
					  }
				  }).success(function(data) {
					 record = data;
					 console.debug("  Record added");
				  }).error(function(data) {
					  console.debug("Existing record update ERROR");	  
				  }).then(function(data) {
					  // add the record to scope
					  $scope.records.push(record);
				  });
	    	
	    	
		    	
    		///////////////////////////////////////////////
		    // CHECK FOR UPDATE
		    // - requires id and non-empty entry set
		    // - a record with id and empty entry set 
	    	//   indicates a DELETE request
		    ///////////////////////////////////////////////	
	    	} else {
		    	
		    
	    			console.debug("UPDATE RECORD");
			    	console.debug(record);
	
	    			$http({
						  url: root_mapping + "record/update",
						  dataType: "json",
						  data: record,
						  method: "POST",
						  headers: {
							  "Content-Type": "application/json"
						  }
					  }).success(function(data) {
						 record = data;
						 console.debug("  Record updated");
					  }).error(function(data) {
						  console.debug("Existing record update ERROR");	  
					  }).then(function(data) {
						  // update the record in scope
						  for (var i = 0; i < $scope.records.length; i++) {
							  if (record.id === $scope.records[i].id) {
								  $scope.records[i] = record;
							  };
						  };
					  });
		    	}
		    	

		    }, function() {
		    	console.debug("Modal cancel");
		    });
		 };
		 
		 // expects argument in form of [record, project, concept]
		 var ModalRecordInstanceCtrl = function($scope, $http, $modalInstance, args) {
			
			  // argument variables
			  $scope.record = args[0];
			  $scope.project = args[1];
			  $scope.concept = args[2];
			 
			  // set scope variables
			  $scope.modeAddEntry = [];
			  initGroups();

			  //////////////////////////////////////////////////////////
			  // Action Logic:
			  //
			  //		                mapEntries
			  //	  -----------------------------------
			  //	  |			 |	Empty	| Not empty |
			  //      |----------------------------------
			  //  ID  | null	 |	Error	|    ADD    |
			  //	  |	non-null |	DELETE	|   UPDATE  |
			  //	  -----------------------------------
			  //
			  ///////////////////////////////////////////////////////////
			  
			  $scope.saveRecord = function () {
				  
				  // if no entries, check if it exists in database
				  if ($scope.record.mapEntry.length == 0) {
					  
					  // does not exist, just throw an alert
					  if ($scope.record.id === "") {
						  alert("This record cannot be created with no entries.");
						  
					  // does exist, confirm delete request
					  } else {
						 
						  var confirmDelete = confirm("This record has no entries and will be deleted. Are you sure you want to delete this record?");
						  if (confirmDelete == true) $modalInstance.close($scope.record);
					  }
					  
				  }
				  
				  // otherwise pass to handler for ADD or UPDATE
				  else $modalInstance.close($scope.record);
			  };

			  $scope.cancelRecord = function () {
			    $modalInstance.dismiss('cancel');
			  };
			  
			  $scope.deleteRecord = function() {
				var confirmDelete = confirm("Are you sure you want to delete this record?");
				if (confirmDelete == true) {
					$scope.record.mapEntry = []; // set to empty to indicate delete request
					$modalInstance.close($scope.record, "delete");
					
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
						  
						  $scope.addEntryElement(entry, 'mapAdvice', advice);
					  }
				  }
			  };
			  
			  
			  $scope.addEntryElement = function(entry, key, elem) {
				  
				  console.debug("Add Entry Element");
				  console.debug(entry);
				  console.debug(key);
				  console.debug(elem);
				  
				  entry[key].push(elem);
				  $scope.updateEntry(entry);
			  };
			  
			  $scope.removeEntryElement = function(entry, key, elem) {
				  
				  entry[key].removeElementById(elem.id);
				  $scope.updateEntry(entry);
				  
			  };
			  
			  $scope.setEntryTarget = function(entry) {
				  
				  // get concept
				  $http({
					  url: root_content + "concept/" 
					  		+ $scope.project.destinationTerminology + "/"
					  		+ $scope.project.destinationTerminologyVersion + "/"
					  		+ "id/" + entry.targetId,
					  dataType: "json",
					  method: "GET",
					  headers: {
						  "Content-Type": "application/json"
					  }
				  }).success(function(data) {
					 entry.targetId = data.terminologyId;
					 entry.targetName = data.defaultPreferredName;
					 
					 $scope.updateEntry(entry);
					 
					 console.debug("  Found concept " + entry.targetId);
				  }).error(function(data) {
					  console.debug("CONCEPT FIND ERROR");
					 $scope.errorEntrySetTarget = "Could not find " + $scope.project.destinationTerminologyVersion + " concept: " + entry.targetId;
					  
				  });
			  };
			  
			  // function to update an entry given a json array
			  $scope.updateEntry = function(entry) {
				  
				  // find entry
				  for (var i = 0; i < $scope.record.mapEntry.length; i++) {
					  
					  // if this entry, replace
					  if ($scope.record.mapEntry[i].id === entry.id) {
						  $scope.record.mapEntry[i] = entry;
						  break;
					  }
				  }
			  };
			  
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
				  //return entries.length == 0 ? null : entries;
				  
			  };
			  
			  function setGroupEditMode(group, mode) {				  
				  // set mode for this group
				  $scope.modeAddEntry[group] = mode;
				  
				  $scope.modeAddEntryGlobal = (mode == true ? true : false);
			  }
			  
			  
			  function initGroups() {
				  getGroups();
				  for (var i = 0; i < $scope.groups.length; i++) {
					  setGroupEditMode($scope.groups[i], false);
				  }
				  $scope.modeAddEntryGlobal == false;
			  };
			  
			  
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
			  
			  
			  // TODO expand this later
			  $scope.entriesEqual = function(entry1, entry2) {
				  if (! (entry1.targetId === entry2.targetId)) return false;
				  if (! (entry1.rule === entry2.rule)) return false;
				  if (! (entry1.mapGroup === entry2.mapGroup)) return false;
				  return true;
			  };
			  
			  $scope.addMapEntry = function(group) {
				  
				  // create blank entry associated with this id
				  var newEntry = {
						"mapRecordId": $scope.record.id,
						"targetId":"",
						"targetName":"",
						"rule":"",
						"mapPriority":"",
						"relationId":"",
						"relationName":"",
						"mapBlock":"",
						"mapGroup": group,
						"mapAdvice":[],
						"mapPrinciples":[]
				  };
				  
				  setGroupEditMode(group, true);
				  
				  return newEntry;
 
			  };
			  
			  $scope.saveMapEntry = function(entry) {
				  $scope.record['mapEntry'].push(entry);
				  console.debug("REVISED RECORD");
				  console.debug($scope.record);
				  setGroupEditMode(entry.mapGroup, false);
			  };
			  
			  $scope.cancelMapEntry = function(group) {
				  console.debug("cancelMapEntry() - " + group);
				  setGroupEditMode(group, false);
			  }
			  
			  // TODO Figure out splice problems
			  $scope.removeMapEntry = function(entry) {
				  var newEntries = new Array();
				  
				  // cycle over existing entries, push if not this entry
				  for (var i = 0; i < $scope.record.mapEntry.length; i++) {
					  if (! ($scope.entriesEqual(entry, $scope.record.mapEntry[i]))) {
						  newEntries.push($scope.record.mapEntry[i]);
					  }
				  }
				  
				  $scope.record.mapEntry = newEntries;
				  $scope.entryDeleted = true;
			  };
			  
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
		      
		      /**
		       * Adds a map group to existing list
		       */
		      $scope.addMapGroup = function() {
		    	  var groupAdded = 0;
		    	  
		    	  // first attempt to "fill in" the first possible gap in groups
		    	  // i.e. [2 3] -> [1 2 3]
		    	  for (var i = 0; i < $scope.groups.length; i++) {
		    		  console.debug(i+1 + " " + $scope.groups[i]);
		    		  if (i+1 < $scope.groups[i]  && groupAdded == false) {
		    			  groupAdded = i+1;
		    			  $scope.groups.push(groupAdded);
		    			  console.debug("Pushed in fill");
		    		  }
		    	  }
		    	  
		    	  // if no group filled in, add to end
		    	  // i.e. [1 2] -> [1 2 3]
		    	  if (groupAdded == 0) {
		    		  groupAdded = $scope.groups.length + 1;
		    		  $scope.groups.push(groupAdded);
		    	  }
		    	  
		    	  setGroupEditMode(groupAdded, false);
		      };
		      
		      /**
		       * Removes a map group from existing groups if it exists
		       */
		      $scope.removeMapGroup = function(group) {   	  
		    	  var newGroups = new Array();
		    	  for (var i = 0; i < $scope.groups.length; i++) {
		    		  if ($scope.groups[i] != group) newGroups.push($scope.groups[i]);
		    	  }
		    	  $scope.groups = newGroups;
		      };
		 };
			  
			  
		 
		 	
		
	}]);

/**
 * Controller for new test view (i.e. non-modal) for map record edit/create/delete functions
 */
mapProjectAppControllers.controller('MapRecordDetailCtrl', 
	['$scope', '$http', '$routeParams', '$sce',
                                                             
	 function ($scope, $http, $routeParams, $sce) {
		
		// initialize scope variables
		$scope.record = 	null;
		$scope.project = 	null;
		$scope.concept = 	null;
		$scope.groups = 	null;
		
		// initialize local variables
		var recordId = 		$routeParams.recordId;
		
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
		    	  getGroups();
		    	  
	          });
          });
	         
		
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
			$scope.entry = entry;
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

mapProjectAppControllers.controller('MapProjectRecordCtrl', ['$scope', '$http', '$routeParams', '$sce',
   function ($scope, $http, $routeParams, $sce) {
	
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
		['$scope', '$http', '$routeParams', '$sce',
		 function ($scope, $http, $routeParams, $sce) {
			
			
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


mapProjectAppControllers.directive('otfHeaderDirective', function() {
    
	return {
        templateUrl: './partials/header.html',
		restrict: 'E', 
        transclude: true,    // allows us �swap� our content for the calling html
        replace: true,        // tells the calling html to replace itself with what�s returned here
        link: function(scope, element, attrs) { // to get scope, the element, and its attributes
          scope.user = $rootScope.user; 
        }
    };
});

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

mapProjectAppControllers
.controller('SortTestCtrl', function($scope) {
	
	console.debug("IN CONTROLLER");
	$scope.testArray = [{"id":4,"name":"D"},{"id":2,"name":"B"},{"id":1,"name":"A"},{"id":3,"name":"C"}];
});




