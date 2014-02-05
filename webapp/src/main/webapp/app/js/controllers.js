'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap']);

var root_url = "${base.url}/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";
	
//////////////////////////////
// Navigation
//////////////////////////////	

mapProjectAppControllers.controller('MapProjectAppNav',
	function ($scope) {
	
		var changePage = function (newPage) {
			$location.path = newPage;
		};
	});	

mapProjectAppControllers.controller('PostTestCtrl',
	
	
	function ($scope, $http) {
	
		$scope.data = "";
		$scope.error = "Error";
		$http({
		     url: root_mapping + "lead/id/1",
		     dataType: "json",
		     method: "GET",
		     headers: {
		       "Content-Type": "application/json"
	
		      }
		}).success(function(data) {
		  	$scope.data = data.mapProject;
		}).error(function(error) {
			$scope.error = "Error";
		});
});

	
//////////////////////////////
// Mapping Services
//////////////////////////////	
	
mapProjectAppControllers.controller('MapProjectListCtrl', 
  function ($scope, $http) {
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
 
   /* $scope.orderProp = 'id';	*/
  });

mapProjectAppControllers.controller('MapRecordListCtrl', 
  function ($scope, $http) {
      $http({
        url: root_mapping + "record/records",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.records = data.mapRecord;
      }).error(function(error) {
    	$scope.error = "Error";
    });
 
    $scope.orderProp = 'id';	
  });

mapProjectAppControllers.controller('MapLeadListCtrl', 
  function ($scope, $http) {
      $http({
        url: root_mapping + "lead/leads",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.leads = data.mapLead;
      }).error(function(error) {
    	  $scope.error = "Error";
      });
      
      $scope.getProjects = function(id) {
          $http({
             url: root_mapping + "lead/id/" + id + "/projects",
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
      };
      
    $scope.toggleEdit = function() {
    	if($scope.editMode == false) {
    		$scope.editMode = true;
    		$scope.editModeValue = 'Stop Editing';
    	} else {
    		$scope.editMode = false;
    		$scope.editModeValue = 'Edit';
    	}
    };
 
    $scope.editMode = false;
    $scope.editModeValue = 'Edit';
    $scope.orderProp = 'id';	
  });

mapProjectAppControllers.controller('MapSpecialistListCtrl', 
  function ($scope, $http) {
      $http({
        url: root_mapping + "specialist/specialists",
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }
      }).success(function(data) {
        $scope.specialists = data.mapSpecialist;
      }).error(function(error) {
    	  $scope.error = "Error";
    });
 
    $scope.orderProp = 'id';	
  });

mapProjectAppControllers.controller('MapRecordDetailCtrl', ['$scope', '$http', '$routeParams',
    function ($scope, $http, $routeParams) {
 	  $scope.recordId = $routeParams.recordId;
 	  $http({
         url: root_mapping + "record/id/" + $scope.recordId,
         dataType: "json",
         method: "GET",
         headers: {
           "Content-Type": "application/json"
         }
       }).success(function(data) {
         $scope.record = data;
       }).error(function(error) {
     });

 }]);

mapProjectAppControllers.controller('EditDemoCtrl', 
		['$scope', '$http', '$routeParams',
		 
   function ($scope, $http, $routeParams) {
			
		$scope.get = function() {
			$http({
				url: root_mapping + "principle/id/" + $scope.principleId,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				$scope.principle = data;
				$scope.currentPrinciple = $scope.principleId;
			}).error(function(error) {
				$scope.error = "ERROR";
			});
		};
 	  
		$scope.save = function() {
			$http({
				url: root_mapping + "principle/id/" + $scope.currentPrinciple,
				dataType: "json",
				method: "POST",
				data: $scope.principle,
				headers: {
					"Content-Type": "application/json"
				}
			});
		};
		
		$scope.reset = function() {
			$scope.principleId = $scope.currentPrinciple;
	    	$scope.get();
	    };
	 
	}]);




//////////////////////////////
// Content Services
//////////////////////////////	

mapProjectAppControllers.controller('ConceptListCtrl', 
		  function ($scope, $http) {
			      $http({
			        url: root_content + "concept/concepts",
			        dataType: "json",
			        method: "GET",
			        headers: {
			          "Content-Type": "application/json"
			        }
			      }).success(function(data) {
			        $scope.concepts = data.concept;
			      }).error(function(error) {
			    	  $scope.error = "Error";
			    });
			 
			    $scope.orderProp = 'id';	
			  });


mapProjectAppControllers.controller('ConceptDetailCtrl', ['$scope', '$http', '$routeParams',
     function ($scope, $http, $routeParams) {
	  $scope.status = "Loading...";
      $scope.statusnote = "This process has not been optimized, and may be particularly slow on the EC2 server (mapping.snomedtools.org).";
  	  $scope.conceptId = $routeParams.conceptId;
  	  $http({
          url: root_content + "concept/" + $routeParams.terminology + "/" + $routeParams.version + "/id/" +  $routeParams.conceptId,
          dataType: "json",
          method: "GET",
          headers: {
            "Content-Type": "application/json"
          }
        }).success(function(data) {
          $scope.status = "Load complete!";
          $scope.statusnote = "";
          $scope.concept = data;
        }).error(function(error) {
        	$scope.status = "Load error!";
            $scope.statusnote = "";
        	console.print("Error in conceptdetailctrol");
        	
        // check for unmapped descendants
        }).then(function(data) {
        	
        	$http({
                url: "${base.url}/mapping-rest/mapping/concept/" + $routeParams.terminology + "/" + $routeParams.version + "/id/" +  $routeParams.conceptId + "/threshold/11",
                dataType: "json",
                method: "GET",
                headers: {
                  "Content-Type": "application/json"
                }
              }).success(function(data) {
            	  $scope.unmappedDescendants = data.concept;     	  
              }).error(function(error) {
            	  console.print("Error in unmapped descendants");
              });
        	
        });
  	  
  	  $scope.hasUnmappedDescendants = function(id) {
  		  
  	  };
  	    

  }]);

//////////////////////////////
// Query Services
//////////////////////////////	

mapProjectAppControllers.controller('QueryCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	$scope.searchConceptsStatus = "";
	$scope.searchProjectsStatus = "";
	$scope.searchRecordsStatus = "";
	
	$scope.searchConcepts = function(id) {
		
	  $scope.searchConceptsStatus = "[Searching...]";
		
	  $http({
        url: root_content + "concept/query/" + $scope.queryConcept,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.conceptResults = data;
        $scope.searchConceptsStatus= $scope.conceptResults.count + " results found:";
       
      }).error(function(error) {
    	$scope.searchConceptsStatus = "Could not retrieve concepts.";
      });
	};
	
	$scope.resetConcepts = function(id) {
		$scope.conceptResults = "";
		$scope.searchConceptsStatus = "";
	};
	
	$scope.searchProjects = function(id) {
		
	  $scope.searchProjectsStatus = "[Searching...]";
	  
	  $http({
        url: root_mapping + "project/query/" + $scope.queryProject,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.projectResults = data;
        $scope.searchProjectsStatus= $scope.projectResults.count + " results found:";
      }).error(function(error) {
    	$scope.searchProjectsStatus = "Could not retrieve projects."; 
      });
	};
	
	$scope.resetProjects = function(id) {
		$scope.projectResults = "";
		$scope.searchProjectsStatus = "";
	};
	
	$scope.searchRecords = function(id) {
		
	  $scope.searchRecordsStatus = "[Searching...]";
	  
	  $http({
        url: root_mapping + "record/query/" + $scope.queryRecord,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
      }).success(function(data) {
        $scope.recordResults = data;
        $scope.searchRecordsStatus= $scope.recordResults.count + " results found, listing by Concept ID:";
      }).error(function(error) {
    	$scope.searchRecordsStatus = "Could not retrieve records."; 
      });
	};
	
	$scope.resetRecords = function(id) {
		$scope.recordResults = "";
		$scope.searchRecordsStatus = "";
	};
}]);


mapProjectAppControllers.controller('QueryConceptCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	$scope.query = $routeParams.query;
	$scope.searchConceptsStatus = "Searching concepts for query: " + $routeParams.query;
	
	$http({
      url: root_content + "concept/query/" + $routeParams.query,
      dataType: "json",
      method: "GET",
      headers: {
        "Content-Type": "application/json"
      }	
    }).success(function(data) {
      $scope.conceptResults = data;
      $scope.searchConceptsStatus= $scope.conceptResults.count + " results found:";
   
    }).error(function(error) {
    	$scope.searchConceptsStatus = "Could not retrieve concepts.";
    });
    	$scope.resetConcepts = function(id) {
		$scope.conceptResults = "";
		$scope.searchConceptsStatus = "[No concept query executed]";
	};
	
}]);
  
//////////////////////////////
// Specialized Services
//////////////////////////////	



mapProjectAppControllers.controller('ProjectCreateCtrl', ['$scope', '$http',,
   function ($scope, $http) {

	$scope.queryConceptStatus = "[No concept query executed]";
}]);

/*
 * Controller for retrieving and displaying records associated with a concept
 */
mapProjectAppControllers.controller('RecordConceptListCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	$scope.conceptId = $routeParams.conceptId;
	$scope.error = "";
	
	var entryRows = "";
	var records;
	
	// retrieve the concept
	$http({
		url: root_mapping + "concept//" + $scope.conceptId,
        dataType: "json",
        method: "GET",
        headers: {
          "Content-Type": "application/json"
        }	
	})
	
	// retrieve all map records where this concept appears
	$http({
       url: root_mapping + "record/conceptId/" + $scope.conceptId,
       dataType: "json",
       method: "GET",
       headers: {
         "Content-Type": "application/json"
       }	
     }).success(function(data) {
    	 records = data.mapRecord;
       $scope.records = data.mapRecord;
       $scope.error = "Records retrieved";
     }).error(function(error) {
   	  $scope.error = "Error retrieving records records.  ";
     }).then(function() {
			
		 // retrieve project information
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
	    	  $scope.error = $scope.error + "Error retrieving projects.  ";
	      });
     });
 
	 $scope.getEntries = function(project) {
		 console.debug("Constructing entries");
		 console.debug(records);
		 
	 	 // instantiate empty entry set
		 var mapRecords = "";
		 var entryRows = "";
	 
		 console.debug("Finding entries for project " + project);

		 // cycle over records to check if they belong to this project
		 for (var i = 0; i < records.length; i++) {
			 
			 console.debug("Record");
			 console.debug(records[i]);
			 if (records[i].mapProjectId == project.id) {
				 console.debug("Adding record " + records[i].conceptId);
				mapRecords = mapRecords.concat(records[i]);
			 }
		 }

		 // construct table elements from MapRecords and MapEntries
		 for (var i = 0; i < mapRecords.length; i++) {
			 
			 entries = records[i].mapEntry;
		 
		 	 console.debug("Entries: " + entries);
			 
			 for (var j = 0; j < entries.length; j++) {
				 
				 var entry = entries[j];
				 
				 // give entries data from record
				 entry.conceptId = records[i].conceptId;
				 entry.conceptName = records[i].conceptName;
				 entry.countDescendantConcepts = records[i].countDescendantConcepts;
				 
				 // add row
				 entryRows = entryRows.concat(entry);
			 }
			 
		 }
		 
		 return entryRows;
     };

}]);
                                                              



// TODO Add test for coming from project list page (i.e. pass the project to this controller)
mapProjectAppControllers.controller('MapProjectDetailCtrl', ['$scope', '$http', '$routeParams',
   function ($scope, $http, $routeParams) {
	
	  $scope.projectId = $routeParams.projectId;
	  
	  $scope.errorProject = "";
	  $scope.errorConcept = "";
	  $scope.errorRecords = "";
	  
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
    		  $scope.errorRecord = "Error retrieving concept";
    	  });
      }).then(function(data) {
  
    	  // set scope variable for total records
    	  $scope.getNRecords();
    	  
          // pagination variables
		  $scope.recordsPerPage = 5;
		  $scope.numRecordPages = Math.ceil($scope.nRecords / $scope.recordsPerPage);

    	  // load first page
    	  $scope.changeRecordPage(1);
    	  
    	  console.debug($scope.nRecords);
    	  console.debug($scope.recordsPerPage);
    	  console.debug($scope.numRecordPages); 
      });
    	
	  

	 // record pagination controls
	 $scope.changeRecordPage = function(page) {
		 
		 var startRecord = (page - 1) * $scope.recordsPerPage + 1;
	 
		  // retrieve any map records associated with this project
		  $http({
			  url: root_mapping + "record/projectId/" + $scope.project.objectId + "/" + startRecord + "-" + $scope.recordsPerPage,
			  dataType: "json",
			  method: "GET",
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
		  });
	 };
	 
	 $scope.getNRecords = function() {
		 // retrieve the total number of records associated with this map project
	   	  $http({
	   		  url: root_mapping + "record/projectId/" + $scope.project.objectId + "/nRecords",
	   		  dataType: "json",
	   		  method: "GET",
	   		  headers: {
	   			  "Content-Type": "application/json"
	   		  }
	   	  }).success(function(data) {
	   		  $scope.nRecords = data;
	   		  
	   	  }).error(function(error) {
	   		  $scope.nRecords = 0;
	   		  console.debug("getNRecords error");
	   	  });
	 };
	  
}]);

//////////////////////////////
//Metadata Services
//////////////////////////////	

mapProjectAppControllers.controller('MetadataCtrl', 
		['$scope', '$http',
                                                             
		function ($scope, $http) {
			
			$scope.errorMetadata = "";
			
			// retrieve any concept associated with this project
	    	 $http({
	    		  url: root_metadata + "all/SNOMEDCT/20130131",
	    		  dataType: "json",
	    		  method: "GET",
	    		  headers: {
	    			  "Content-Type": "application/json"
	    		  }
	    	  }).success(function(data) {
	    		  $scope.idNameMaps = data.idNameMap;
	    	  }).error(function(error) {
	    		  $scope.errorMetadata = "Error retrieving metadata";
	    	  });
		}]);





