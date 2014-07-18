
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

.controller('terminologyBrowserWidgetCtrl', function($scope, $rootScope, $q, $timeout, $http, $routeParams, $location, localStorageService, metadataService, terminology){

	$scope.terminology = terminology.name;
	$scope.terminologyVersion = terminology.version;
	$scope.focusProject = localStorageService.get('focusProject');
	$scope.metadata = localStorageService.get('metadata_' + terminology.name);

	console.debug(localStorageService.get('metadata_' + terminology.name));

	// initialize currently displayed concept as empty object
	$scope.currentOpenConcepts = {};
	$scope.descTypes = {};
	$scope.relTypes = {};
	
	// initialize search variables
	$scope.query = "";							// the query input
	$scope.searchBackAllowed = false;			// whether the back button is displayed
	$scope.searchForwardAllowed = false;		// whether the forward button is displayed
	$scope.searchStack = [];					// an array of search terms from query input
	$scope.searchStackPosition = 0;				// the current position in the stack
	$scope.searchStackResults = 0;				// the number of results currently in the array (may be less than array length)

	// watch for project change and modify the local variable if necessary
	// coupled with $watch below, this avoids premature work fetching
	$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) { 	
		console.debug("TerminologyBrowserWidgetCtrl:  Detected change in focus project");
		$scope.focusProject = parameters.focusProject;
	});
	
	$scope.userToken = localStorageService.get('userToken');
	

	// on any change of focusProject, metadata, or user token, perform widget initialization
	$scope.$watch(['focusProject', 'metadata', 'userToken'], function() {

		// once needed state variables are loaded, initialize and make first call
		if ($scope.focusProject != null && $scope.metadata != null && $scope.userToken != null) {

			console.debug("STATE VARIABLES");
			console.debug($scope.focusProject);
			console.debug($scope.metadata);
			
			$http.defaults.headers.common.Authorization = $scope.userToken;
			

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

			// get the root trees
			$scope.getRootTree();
		}
	});

	// function to get the root nodes
	$scope.getRootTree = function() {

		$http({
			url: root_mapping + "treePosition/project/id/" + $scope.focusProject.id + "/terminology/id/" + $scope.terminology + "/" + $scope.terminologyVersion,
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).success (function(response) {
			console.debug("HTTP RESPONSE");
			console.debug(response);
			$scope.terminologyTree = response.treePosition;
			for (var i = 0; i < $scope.terminologyTree; i++) {
				$scope.terminologyTree[i].isOpen = false;
				$scope.terminologyTree[i].isConceptOpen = false;
			}
		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};

	// function to get the root nodes with query
	$scope.getRootTreeWithQuery = function(isNewSearch) {

		console.debug("QUERYING: " + $scope.query);
		$scope.searchStatus = "Searching...";
		$scope.terminologyTree = [];
		$http({
			url: root_mapping + "treePosition/project/id/" + $scope.focusProject.id + "/terminology/id/" + $scope.terminology + "/" + $scope.terminologyVersion + "/query/" + $scope.query,
			method: "GET",
			headers: { "Content-Type": "application/json"}	
		}).success (function(response) {
			console.debug("Query successful with response:");
			console.debug(response);

			// limit result count to 10 root tree positions
			for (var x =0; x < response.treePosition.length && x < 10; x++) {
				$scope.terminologyTree[x] = response.treePosition[x];
			}

			$scope.expandAll($scope.terminologyTree);
			$scope.searchStatus = "";
			
			if (isNewSearch == true) {
			
				// update the position counter
				$scope.searchStackPosition++;
				
				// check that array still has space, if not reallocate			
				if ($scope.searchStack.length <= $scope.searchStackPosition) {
					
					var newSearchStack = new Array($scope.searchStack.length * 2);
					for (var i = 0; i < $scope.searchStack.length; i++) {
						newSearchStack[i] = $scope.searchStack[i];
					}
					
					$scope.searchStack = newSearchStack;
				}
				
				// add the query to the stack
				$scope.searchStack[$scope.searchStackPosition] = $scope.query;
				
				// remove any elements past the search stack position
				for (var i = $scope.searchStackPosition + 1; i < $scope.searchStack.length; i++) {
					$scope.searchStack[i] = "";
				};
				
				// set the total number of results to this position
				$scope.searchStackResults = $scope.searchStackPosition;
				
			// otherwise, this request came from a back/forward button press
			} else {
				// do nothing, no need to modify results
			}
			
			console.debug($scope.searchStackPosition + " - " + $scope.searchStackResults);
						
			// set the variables for back/forward
			$scope.searchBackAllowed = $scope.searchStackPosition > 0 ? true : false;
			$scope.searchForwardAllowed = $scope.searchStackPosition < $scope.searchStackResults ? true : false;
			
		}).error(function(data, status, headers, config) {
		    $rootScope.handleHttpError(data, status, headers, config);
		});
	};
	
	$scope.changeSearch = function(positionChange) {
		
		// alter the position, set the query, and call the search function
		$scope.searchStackPosition += positionChange;
		if ($scope.searchStackPosition < 0) $scope.searchStackPosition = 0;
		$scope.query = $scope.searchStack[$scope.searchStackPosition];
		
		// if query is not populated or undefined, get the root trees, otherwise get query results
		if ($scope.query == undefined || $scope.query === "") $scope.getRootTree();
		else $scope.getRootTreeWithQuery(false);
		
		
	};

	$scope.gotoReferencedConcept = function(referencedConcept) {

		$scope.query = referencedConcept.terminologyId;	
		console.debug("Setting query string to " + $scope.query);	
		$scope.getRootTreeWithQuery();
	};


	$scope.getLocalTree = function(terminologyId) {

		console.debug("Called getLocalTree with terminologyId = " + terminologyId);

		var deferred = $q.defer(); 

		$timeout(function() {
			$http({
				url: root_mapping + "treePosition/project/id/" + $scope.focusProject.id + "/concept/id/" + $scope.terminology + "/" + $scope.terminologyVersion + "/" + terminologyId,
				method: "GET",
				headers: { "Content-Type": "application/json"}	
			}).success (function(response) {
				console.debug("HTTP RESPONSE");
				deferred.resolve(response);
			}).error(function(data, status, headers, config) {
			    $rootScope.handleHttpError(data, status, headers, config);
			});;
		});

		return deferred.promise;
	};

	// function to recursively expand tree positions
	// also returns true/false if:
	// - the id of the node or one of its children exactly matches the search
	$scope.expandAll = function(treePositions) {
		
		for (var i = 0; i < treePositions.length; i++) {
			
			console.debug("Expanding for ", treePositions[i]);

			// if children have been loaded, expand
			if (treePositions[i].children.length > 0) {
				treePositions[i].isOpen = true;
			}
			
			
			// if the node exactly matches a query
			if (treePositions[i].terminologyId.toUpperCase() === $scope.query.toUpperCase()) {
				console.debug("Exact match for query");
				
				// load the concept detalis
				$scope.getConceptDetails(treePositions[i]);
				
				console.debug("Expanding children")
				console.debug(treePositions[i].children);
				
				/*// expand children, but do not expand their info panels
				for (var j = 0; j < treePositions[i].children.length; i++) {
					
					treePositions[i].children[j].isOpen = true;
				}
				
				console.debug("After expanding children")
				console.debug(treePositions[i].children);*/
				

				// stop recursive expansion here;
				return true;
			}
			
			// if a child node reports that this is in direct path of a requested concept id, get details
			else if ($scope.expandAll(treePositions[i].children) == true) {
				console.debug("This node reports exact match among descendants");
				
				// if this is a root node, simply return false to avoid expanding this node
				if (treePositions[i].ancestorPath == null || treePositions[i].ancestorPath === '' )
					return false;
				
				$scope.getConceptDetails(treePositions[i]);
				
				return true;
			}
			
			// return false (not an exact match)
			else return false;
			
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
					var data = response.treePosition;

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
	$scope.getConceptDetails = function(node) {


		// if called when currently displayed, clear current concept
		if (node.isConceptOpen == true) {
			node.isConceptOpen = false;

			// otherwise, retrieve and display this concept
		} else {
			
			console.debug(node.descGroups);

			if (node.descGroups.length > 0)
				node.isConceptOpen = true;
			else node.isConceptOpen = false;
		}
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

	function getFormattedDescriptions(concept, typeId, relTypes) {

		console.debug('getFormattedDescriptions given relTypes:');
		console.debug(relTypes);

		// first, get all descriptions for this TypeId
		var descriptions = getConceptElementsByTypeId(concept.description, typeId);
		
		// format each description
		for (var i = 0; i < descriptions.length; i++) {
			descriptions[i] = formatDescription(descriptions[i], relTypes, concept);
		}

		return descriptions;

	};

	function formatDescription(description, relTypes, concept) {

		console.debug("Formatting description: " + description.terminologyId + " - " + description.term);
		console.debug('relTypes');
		console.debug(relTypes);
		var relationshipsForDescription = [];

		// find any relationship where the terminology id begins with the description terminology id
		for (var i = 0; i < concept.relationship.length; i++) {
			console.debug(concept.relationship[i].terminologyId + ' compared to ' + description.terminologyId + ' -> ' + concept.relationship[i].terminologyId.indexOf(description.terminologyId));
			if (concept.relationship[i].terminologyId.indexOf(description.terminologyId) == 0) {
				console.debug("    Found relationship: " + concept.relationship[i].terminologyId);
				relationshipsForDescription.push(concept.relationship[i]);
			}
		}

		console.debug("Found these relationships: ");
		console.debug(relationshipsForDescription);

		if (relationshipsForDescription.length > 0) {
			description.referencedConcepts = [];
			for (var i = 0; i < relationshipsForDescription.length; i++) {

				// add the target id
				var referencedConcept = {};
				referencedConcept.terminologyId = relationshipsForDescription[i].destinationConceptId;

				// if a asterisk-to-dagger, add a †
				if (relTypes[relationshipsForDescription[i].typeId].indexOf('Asterisk') == 0) {
					console.debug("†");
					referencedConcept.relType = "†";
				}
				// if a dagger-to-asterik, add a *
				if (relTypes[relationshipsForDescription[i].typeId].indexOf('Dagger') == 0) {
					console.debug("*");
					referencedConcept.relType = "*";
				}
				description.referencedConcepts.push(referencedConcept);

				// remove this relationship from the current concept (now represented in description)
				concept.relationship.removeElementByTerminologyId(relationshipsForDescription[i]);

			}
		}

		return description;

	}

	$scope.getDescriptionGroups = function(terminologyId) {

		var concept = $scope.getElementByTerminologyId($scope.currentOpenConcepts, terminologyId);
		$.map(concept.descriptionGroups, function(v, i) {
			if (v['terminologyId'] === terminologyId) return v;
		});
		return null;
	};


	$scope.hasElementByTerminologyid = function(array, terminologyId) {
		$.map(array, function(v, i) {
			if (v['terminologyId'] === terminologyId) return true;
		});
		return false;
	};

	$scope.getElementByTerminologyId = function(array, terminologyId) {
		$.map(array, function(v, i) {
			if (v['terminologyId'] === terminologyId) return v;
		});
		return null;
	};

	// function to remove an element by id or localid
	// instantiated to negate necessity for equals methods for map objects
	//   which may not be strictly identical via string or key comparison
	Array.prototype.removeElementByTerminologyId = function(terminologyId) {
		var array = new Array();
		$.map(this, function(v,i){
			if (v['terminologyId'] != terminologyId) array.push(v);
		});

		this.length = 0; //clear original array
		this.push.apply(this, array); //push all elements except the one we want to delete
	};

	$scope.truncate = function(string, length) {
		if (length == null) length = 100;
		if (string.length > length) return string.slice(0, length-3);
		else return string;
	};

	$scope.truncated = function(string, length) {
		if (length == null) length = 100;
		if (string.length > length) 
			return true;
		else 
			return false;
	};

	$scope.selectConcept = function(node) {
		$rootScope.$broadcast('terminologyBrowser.selectConcept' , {key: 'concept', concept: node});
		window.scrollTo(0,0);
	};
});


