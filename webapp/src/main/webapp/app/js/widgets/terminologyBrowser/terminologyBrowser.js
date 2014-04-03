
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

.controller('terminologyBrowserWidgetCtrl', function($scope, $rootScope, $q, $timeout, $http, $routeParams, localStorageService, terminology){

	$scope.terminology = terminology.name;
	$scope.terminologyVersion = terminology.version;
	
	console.debug(terminology);
	
	getRootTree();
	
	// function to get the root nodes
	function getRootTree() {
	
		$http({
			url: root_content + "tree/terminology/" + $scope.terminology + "/" + $scope.terminologyVersion,
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
			url: root_content + "tree/terminology/" + $scope.terminology + "/" + $scope.terminologyVersion + "/query/" + $scope.query,
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
	

	$scope.getLocalTree = function(terminologyId) {
		
		console.debug("Called getLocalTree with terminologyId = " + terminologyId);
		
		var deferred = $q.defer(); 
		
		$timeout(function() {
			$http({
				url: root_content + "tree/concept/" + $scope.terminology + "/" + $scope.terminologyVersion + "/id/" + terminologyId,
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
	
	$scope.truncate = function(string) {
		if (string.length > 100) return string.slice(0, 96) + "...";
		else return string;
	};
	
	$scope.selectConcept = function(node) {
		$rootScope.$broadcast('terminologyBrowser.selectConcept', {key: 'concept', concept: node});
	};
});


