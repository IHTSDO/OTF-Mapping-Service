
'use strict';

angular.module('mapProjectApp.widgets.terminologyBrowser', ['adf.provider'])
.config(function(dashboardProvider){ 

	dashboardProvider
	.widget('terminologyBrowser', {
		title: function(config) { return config.terminology + "," + config.terminologyVersion; },
		
		description: 'Tree view for terminology',
		templateUrl: 'js/widgets/terminologyBrowser/terminologyBrowser.html',
		controller: 'terminologyBrowserWidgetCtrl',
		resolve: {
			terminology: function(config) {
				return {name: config.terminology, version: config.terminologyVersion};
			}
		},
		edit: {}   
	});
})

.controller('terminologyBrowserWidgetCtrl', function($scope, $rootScope, $q, $timeout, $http, $routeParams, localStorageService, metadataService, terminology){

	$scope.terminology = terminology.name;
	$scope.terminologyVersion = terminology.version;
	$scope.focusProject = localStorageService.get('focusProject');
	
	// initialize currently displayed concept as empty object
	$scope.currentConcept = {};
	$scope.descTypes = {};
	$scope.relTypes = {};
	
	// retrieve the metadata
	metadataService.get(terminology.name).then(function(response) {
		$scope.metadata = response.keyValuePairList;
		
		// find the description and relation type metadata and convert to normal JSON object structure
		for (var i = 0; i < $scope.metadata.length; i++) {
			if ($scope.metadata[i].name === 'Description Types') {
				
				for (var j = 0; j < $scope.metadata[i].keyValuePair.length; j++) {
					$scope.descTypes[$scope.metadata[i].keyValuePair[j].key] = $scope.metadata[i].keyValuePair[j].value;
				}
				
			}
			else if ($scope.metadata[i].name === 'Relationship Types') {
				for (var j = 0; j < $scope.metadata[i].keyValuePair.length; j++) {
					$scope.relTypes[$scope.metadata[i].keyValuePair[j].key] = $scope.metadata[i].keyValuePair[j].value;
				}
			}
		}
		
		console.debug("Desc types:");
		console.debug($scope.descTypes);
		
		console.debug("Rel types:");
		console.debug($scope.relTypes);
		
	});
	
	// watch for project change and modify the local variable if necessary
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("TerminologyBrowserWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});
	
	// on any change of focusProject, retrieve new available work
	$scope.$watch('focusProject', function() {

		if ($scope.focusProject != null) {
			getRootTree();
		}
	});

	// function to get the root nodes
	function getRootTree() {
	
		$http({
			url: root_mapping + "tree/projectId/" + $scope.focusProject.id + "/terminology/" + $scope.terminology + "/" + $scope.terminologyVersion,
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).then (function(response) {
			console.debug("HTTP RESPONSE");
			console.debug(response);
			$scope.terminologyTree = response.data.treePosition;
			for (var i = 0; i < $scope.terminologyTree; i++) {
				$scope.terminologyTree[i].isOpen = false;
			}
		});
	};
	
	// function to get the root nodes with query
	$scope.getRootTreeWithQuery = function() {
		
		console.debug("QUERYING: " + $scope.query);
		$scope.searchStatus = "Searching...";
		$scope.terminologyTree = [];
		$http({
			url: root_mapping + "tree/projectId/" + $scope.focusProject.id + "/terminology/" + $scope.terminology + "/" + $scope.terminologyVersion + "/query/" + $scope.query,
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).then (function(response) {
			console.debug("HTTP RESPONSE");
			console.debug(response);
			$scope.terminologyTree = response.data.treePosition;
			
			$scope.expandAll($scope.terminologyTree);
			$scope.searchStatus = "";
		});
	};
	
	// TODO Make this model object cleaner
	// For now, expects that a referencedConcept will be a string of one of the following forms:
	// 'conceptId', 'conceptId conceptMarker' where conceptMarker is † or *
	$scope.gotoReferencedConcept = function(referencedConcept) {
		
		var splitString = referencedConcept.split(" ");
		
		$scope.query = splitString[0];
		
		console.debug("Setting query string to " + $scope.query);
		
		$scope.getRootTreeWithQuery();
	}
	

	$scope.getLocalTree = function(terminologyId) {
		
		console.debug("Called getLocalTree with terminologyId = " + terminologyId);
		
		var deferred = $q.defer(); 
		
		$timeout(function() {
			$http({
				url: root_mapping + "tree/projectId/" + $scope.focusProject.id + "/concept/" + $scope.terminology + "/" + $scope.terminologyVersion + "/id/" + terminologyId,
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).then (function(response) {
				console.debug("HTTP RESPONSE");
				deferred.resolve(response);
			});
		});
		
		return deferred.promise;
	};
	
	$scope.expandAll = function(treePositions) {
		for (var i = 0; i < treePositions.length; i++) {
			
			// if children have been loaded, expand
			if (treePositions[i].children.length > 0) {
				treePositions[i].isOpen = true;
				$scope.expandAll(treePositions[i].children);
			}
		}
	};
	
	$scope.toggleChildren = function(node) {
		
		console.debug("getChildren called with " + node.terminologyId);
		
		node.isOpen = !node.isOpen;
		
		// only perform actions if node is open
		if(node.isOpen == true) {
			
			// check if this node has been retrieved
			if (node.children.length == 0 && node.childrenCount > 0) {
			
				$scope.getLocalTree(node.terminologyId).then(function(response) {
				
					// shorthand for the conceptTrees (may be multiple paths)
					var data = response.data.treePosition;
		
					// find the tree path along this node
					for (var i = 0; i < data.length; i++) {
						console.debug(data);
						console.debug(data[i].ancestorPath);
						console.debug(node.ancestorPath);
						if (data[i].ancestorPath === node.ancestorPath) {
							node.children = node.children.concat(data[i].children);
						}
					}
				});
			} else {
				// do nothing, content already loaded
			}

		}
		
	};
	
	// function for toggling retrieval and display of concept details
	$scope.getConceptDetails = function(terminologyId, isConceptDetailCollapsed) {
		
		console.debug('terminologyId' in $scope.currentConcept);
		
		
		// if called when currently displayed, clear current concept
		if ('terminologyId' in $scope.currentConcept && $scope.currentConcept.terminologyId === terminologyId) {
			$scope.currentConcept = [];
		
		// otherwise, retrieve and display this concept
		} else {

			console.debug("Retrieving concept information for " + terminologyId);
			
			// retrieve the concept
			$http({
				url: root_content + "concept/" + $scope.terminology + "/" + $scope.terminologyVersion + "/id/" + terminologyId,
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			
			// on success, set the scope concept
			}).success (function(response) {
				
				// create the display elements
				$scope.currentConcept = response;
				$scope.currentConceptDescriptionGroups = [];
				$scope.currentConceptRelationshipGroups = [];
				
				console.debug(response);
				
				// discover what description descTypes are present
				var descTypes = {};
				for (var i = 0; i < $scope.currentConcept.description.length; i++) {
					
					if (! ( $scope.currentConcept.description[i].typeId in descTypes )) {
						
						if ($scope.descTypes[$scope.currentConcept.description[i].typeId].indexOf('Preferred') == -1) {
								descTypes[$scope.currentConcept.description[i].typeId] = 
									$scope.descTypes[$scope.currentConcept.description[i].typeId];
						}
					}
				};
				console.debug("Description Types found for concept:");
				console.debug(descTypes);
				
				// discover what rel types are present
				var relTypes = {};
				for (var i = 0; i < $scope.currentConcept.relationship.length; i++) {
					
					if (! ( $scope.currentConcept.relationship[i].typeId in relTypes )) {
						
						console.debug("Rel TypeId: " + $scope.currentConcept.relationship[i].typeId);
						console.debug();
						
						// if this relationship is not an Isa relationship AND not a reference, add it to the relTypes
						if ($scope.relTypes[$scope.currentConcept.relationship[i].typeId].indexOf('Isa') == -1) {
								relTypes[$scope.currentConcept.relationship[i].typeId] = 
									$scope.relTypes[$scope.currentConcept.relationship[i].typeId];
						}
					}
				};
				console.debug("Relationship Types found for concept:");
				console.debug(relTypes);
				
				//////////////////
				// DESCRIPTIONS //
				//////////////////
				
				// cycle over discovered descTypes
				for (var key in descTypes) {
					// get the descriptions for this type
					var descGroup = {};
					descGroup['name'] = descTypes[key];
					descGroup['descriptions'] = getFormattedDescriptions($scope.currentConcept.description, key, relTypes);
					
					
					$scope.currentConceptDescriptionGroups.push(descGroup);
				}
				
				console.debug("Extracted descriptions for concept:");
				console.debug($scope.currentConceptDescriptionGroups.length);
				console.debug($scope.currentConceptDescriptionGroups);
				
				///////////////////
				// RELATIONSHIPS //
				///////////////////
				
				
				
				// cycle over discovered relTypes
				for (var key in relTypes) {
					// get the relationships for this type
					
					var relationships = getConceptElementsByTypeId($scope.currentConcept.relationship, key);
					
					if (relationships.length > 0) {
					
						var relGroup = {};
						relGroup['name'] = relTypes[key];
						
						
						relGroup['relationships'] = getConceptElementsByTypeId($scope.currentConcept.relationship, key);
						
						$scope.currentConceptRelationshipGroups.push(relGroup);
					}
				}
				
				console.debug($scope.currentConceptRelationshipGroups);

				
			// otherwise display an error message
			}).error(function(response) {
				$scope.concept = [];
				$scope.concept.name = "Error retrieving concept";
			});
		};
	};
	
	// given a typeId and a list of elements, returns those elements with matching typeId
	function getConceptElementsByTypeId(elements, typeId) {
		var elementsByTypeId = [];
		for (var i = 0; i < elements.length; i++) {
			if (String(elements[i].typeId) === String(typeId)) {
				elementsByTypeId.push(elements[i]);
			}
		}
		return elementsByTypeId;
	};
	
	//////////////////////////////////////////////////////////////////
	// REFERENCE HANDLING
	//
	// Used for ICD10 * and † display
	// For each reference relationship:
	// - find one or more descriptions matching this relationship
	// - attach the reference information to this relationship
	// - remove the description from the description list
	/////////////////////////////////////////////////////////////////
	
	function getFormattedDescriptions(descriptions, typeId, relTypes) {
		
		console.debug('getFormattedDescriptionsn given relTypes:');
		console.debug(relTypes);
		
		// first, get all descriptions for this TypeId
		var descriptions = getConceptElementsByTypeId(descriptions, typeId);
		
		// format each description
		for (var i = 0; i < descriptions.length; i++) {
			descriptions[i] = formatDescription(descriptions[i], relTypes);
		}
		
		return descriptions;
		
	};
	
	function formatDescription(description, relTypes) {
		
		console.debug("Formatting description: " + description.terminologyId + " - " + description.term);
		console.debug('relTypes');
		console.debug(relTypes);
		var relationshipsForDescription = [];
		
		// find any relationship where the terminology id begins with the description terminology id
		for (var i = 0; i < $scope.currentConcept.relationship.length; i++) {
			if ($scope.currentConcept.relationship[i].terminologyId.indexOf(description.terminologyId) == 0) {
				console.debug("    Found relationship: " + $scope.currentConcept.relationship[i].terminologyId);
				relationshipsForDescription.push($scope.currentConcept.relationship[i]);
			}
		}
		
		console.debug("Found these relationships: ");
		console.debug(relationshipsForDescription);
		
		if (relationshipsForDescription.length > 0) {
			description.referencedConcepts = [];
			for (var i = 0; i < relationshipsForDescription.length; i++) {
				
				// add the target id
				var referencedConcept = relationshipsForDescription[i].destinationConceptId;
				
				// if a asterik-to-dagger, add a *
				if (relTypes[relationshipsForDescription[i].typeId].indexOf('Asterisk') == 0) {
					referencedConcept += " *";
				}
				// if a dagger-to-asterik, add a †
				if (relTypes[relationshipsForDescription[i].typeId].indexOf('Dagger') == 0) {
					referencedConcept += " †";
				}
/*				
				// add a comma if not the last element
				description.term += (i == relationshipsForDescription.length -1 ? "" : ", ");*/
				
				description.referencedConcepts.push(referencedConcept);
				
				// remove this relationship from the current concept (now represented in description)
				$scope.currentConcept.relationship.removeElement(relationshipsForDescription[i]);
					
			}
		}
		
		return description;
		
	}
	
	// function to remove an element by id or localid
	// instantiated to negate necessity for equals methods for map objects
	//   which may not be strictly identical via string or key comparison
	Array.prototype.removeElement = function(elem) {
		var array = new Array();
		$.map(this, function(v,i){
			if (v['terminologyId'] != elem['terminologyId']) array.push(v);
		});

		this.length = 0; //clear original array
		this.push.apply(this, array); //push all elements except the one we want to delete
	};
	
	$scope.truncate = function(string, length) {
		if (length == null) length = 100;
		if (string.length > length) return string.slice(0, length-3) + "...";
		else return string;
	};
	
	$scope.selectConcept = function(node) {
		$rootScope.$broadcast('terminologyBrowser.selectConcept', {key: 'concept', concept: node});
		window.scrollTo(0,0);
	};
});


