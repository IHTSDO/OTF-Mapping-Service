
'use strict';

angular.module('mapProjectApp.widgets.applicationAdmin', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('applicationAdmin', {
		title: 'Application Administration',
		description: 'Provides for the addition, deletion and updating of application level metadata.',
		templateUrl: 'js/widgets/applicationAdmin/applicationAdmin.html',
		controller: 'applicationAdminCtrl',
		resolve: {},
		edit: {}
	});
})
.controller('applicationAdminCtrl', 
			['$scope', '$http', '$sce', '$rootScope', '$location', 'localStorageService', '$upload',
			 function ($scope, $http, $sce, $rootScope, $location, localStorageService, $upload) {

			    $scope.page =  'project';

				$scope.currentRole = localStorageService.get('currentRole');
				$scope.currentUser = localStorageService.get('currentUser');
				$scope.focusProject = localStorageService.get('focusProject');
				$scope.mapProjects = localStorageService.get("mapProjects");
				$scope.mapUsers = localStorageService.get('mapUsers');				
				$scope.newAllowableForNullTarget = false;
				$scope.newIsComputed = false;
				$scope.newRelationAllowableForNullTarget = false;
				$scope.newRelationIsComputed = false;
				$scope.newAgeRangeUpperInclusive = false;
				$scope.newAgeRangeLowerInclusive = false;
				$scope.newMapProjectPublished = false;
				$scope.newMapProjectRuleBased = false;
				$scope.newMapProjectGroupStructure = false;
				$scope.newMapProjectScopeDescendantsFlag = false;
				$scope.newMapProjectScopeExcludedDescendantsFlag = false;
				$scope.newMapProjectPublic = false;
				
				$scope.terminologyVersionPairs = new Array();
				var editingPerformed = new Array();
				var previousAdvicePage = 1;
				var previousPrinciplePage = 1;
				var previousRelationPage = 1;
				var previousReportDefinitionPage = 1;
				
				$scope.allowableMapTypes = [{displayName: 'Extended Map', name: 'ExtendedMap'}, 
				                            {displayName: 'Complex Map', name: 'ComplexMap'}, 
				                            {displayName: 'Simple Map', name: 'SimpleMap'}];
				$scope.newMapProjectMapType = $scope.allowableMapTypes[0];
				
				$scope.allowableMapRelationStyles = [{displayName: 'Map Category Style', name: 'MAP_CATEGORY_STYLE'},
				                                {displayName: 'Relationship Style', name: 'RELATIONSHIP_STYLE'}];
				$scope.newMapRelationStyle = $scope.allowableMapRelationStyles[0];
				
				$scope.allowableWorkflowTypes = [{displayName: 'Conflict Project', name: 'CONFLICT_PROJECT'},
				                                 {displayName: 'Review Project', name: 'REVIEW_PROJECT'}];
				$scope.newWorkflowType = $scope.allowableWorkflowTypes[0];
				
				$scope.definitionQueryTypes = ['SQL', 'HQL', 'LUCENE'];
				$scope.definitionResultTypes = ['CONCEPT', 'MAP_RECORD'];
				$scope.definitionRoles = ['VIEWER', 'SPECIALIST', 'LEAD', 'ADMIN'];
				$scope.definitionTimePeriods = ['DAILY', 'WEEKLY', 'MONTHLY', 'ANNUALLY'];
				
				$scope.newHandler;
											
				// watch for focus project change
				$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
					console.debug("MapProjectDetailCtrl: Detected change in focus project");
					$scope.focusProject = parameters.focusProject;  
				});
				
				$scope.userToken = localStorageService.get('userToken');
				
				$scope.$watch(['focusProject', 'userToken'], function() {
					if ($scope.focusProject != null && $scope.userToken != null) {}				
						$http.defaults.headers.common.Authorization = $scope.userToken;
						$scope.go();				
				});
				
				$scope.go = function() {								
					$http({
						url: root_metadata + "terminology/terminologies",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("Success in getting terminologies metadata.");
						for (var i = 0; i < data.keyValuePairList.length; i++) {
							$scope.terminologyVersionPairs.push(
								data.keyValuePairList[i].keyValuePair[0].key + " " + 
								data.keyValuePairList[i].keyValuePair[0].value);
						}
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});					
					
					$http({
						url: root_mapping + "advice/advices",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapAdvices = data.mapAdvice;
						localStorageService.add('mapAdvices', data.mapAdvice);
						$rootScope.$broadcast('localStorageModule.notification.setMapAdvices',{key: 'mapAdvices', mapAdvices: data.mapAdvices});  
						$scope.allowableMapAdvices = localStorageService.get('mapAdvices');
						$scope.getPagedAdvices(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
					
					$http({
						url: root_mapping + "relation/relations",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapRelations = data.mapRelation;
						localStorageService.add('mapRelations', data.mapRelation);
						$rootScope.$broadcast('localStorageModule.notification.setMapRelations',{key: 'mapRelations', mapRelations: data.mapRelations});  
						$scope.allowableMapRelations = localStorageService.get('mapRelations');
						$scope.getPagedRelations(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
					
					$http({
						url: root_mapping + "principle/principles",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapPrinciples = data.mapPrinciple;
						localStorageService.add('mapPrinciples', data.mapPrinciple);
						$rootScope.$broadcast('localStorageModule.notification.setMapPrinciples',{key: 'mapPrinciples', mapPrinciples: data.mapPrinciples});  
						$scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
					    $scope.getPagedPrinciples(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
					
					$http({
						url: root_mapping + "ageRange/ageRanges",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapAgeRanges = data.mapAgeRange;
						localStorageService.add('mapAgeRanges', data.mapAgeRange);
						$rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges',{key: 'mapAgeRanges', mapAgeRanges: data.mapAgeRanges});  
						$scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});

					$http({
						url: root_mapping + "handler/handlers",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
					    $scope.handlers = new Array();
						for (var i = 0; i < data.searchResult.length; i++) {
						  $scope.handlers.push(data.searchResult[i].value);
						}
						$scope.newHandler = $scope.handlers[0];
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
					
					$http({
						url: root_reporting + "definition/definitions",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.reportDefinitions = data.reportDefinition;
						localStorageService.add('reportDefinitions', data.reportDefinition);
						$rootScope.$broadcast('localStorageModule.notification.setReportDefinitions',{key: 'reportDefinitions', reportDefinitions: data.reportDefinitions});  
						$scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');
						$scope.getPagedReportDefinitions(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
					
					// set pagination variables
					$scope.pageSize = 5;
					$scope.maxSize = 5;

					$scope.orderProp = 'id';
				};


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
				$scope.getPagedAdvices = function (page, filter) {
					console.debug('getPagedAdvices', filter);
					if ($scope.adviceInEditingPerformed() == true) {
					  if(confirm("You have unsaved changes.\n\n Are you sure that you want to switch pages?") == false) {
						  $scope.pageAdvice = previousAdvicePage;
						  return;
					  }
					}
					$scope.adviceFilter = filter;
					$scope.pagedAdvice = $scope.sortByKey($scope.mapAdvices, 'id')
					.filter(containsAdviceFilter);
					$scope.pagedAdviceCount = $scope.pagedAdvice.length;
					$scope.pagedAdvice = $scope.pagedAdvice
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
					previousAdvicePage = page;
				};

				$scope.getPagedRelations = function (page, filter) {
					if ($scope.relationInEditingPerformed() == true) {
						  if(confirm("You have unsaved changes.\n\n Are you sure that you want to switch pages?") == false) {
							  $scope.pageRelation = previousRelationPage;
							  return;
						  }
						}
					$scope.relationFilter = filter;
					$scope.pagedRelation = $scope.sortByKey($scope.mapRelations, 'id')
					.filter(containsRelationFilter);
					$scope.pagedRelationCount = $scope.pagedRelation.length;
					$scope.pagedRelation = $scope.pagedRelation
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
					previousRelationPage = page;
				};

				$scope.getPagedPrinciples = function (page, filter) {
					if ($scope.principleInEditingPerformed() == true) {
						  if(confirm("You have unsaved changes.\n\n Are you sure that you want to switch pages?") == false) {
							  $scope.pagePrinciple = previousPrinciplePage;
							  return;
						  }
						}
					$scope.principleFilter = filter;
					$scope.pagedPrinciple = $scope.sortByKey($scope.mapPrinciples, 'id')
					.filter(containsPrincipleFilter);
					$scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
					$scope.pagedPrinciple = $scope.pagedPrinciple
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
					previousPrinciplePage = page;
				};
				
				$scope.getPagedReportDefinitions = function (page, filter) {
					console.debug('getPagedReportDefinitions', filter);
					if ($scope.reportDefinitionInEditingPerformed() == true) {
					  if(confirm("You have unsaved changes.\n\n Are you sure that you want to switch pages?") == false) {
						  $scope.pageReportDefinition = previousReportDefinitionPage;
						  return;
					  }
					}
					$scope.reportDefinitionFilter = filter;
					$scope.pagedReportDefinition = $scope.sortByKey($scope.reportDefinitions, 'id')
					.filter(containsReportDefinitionFilter);
					$scope.pagedReportDefinitionCount = $scope.pagedReportDefinition.length;
					$scope.pagedReportDefinition = $scope.pagedReportDefinition
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
					previousReportDefinitionPage = page;
				};



				// functions to reset the filter and retrieve unfiltered results

				$scope.resetAdviceFilter = function() {
					$scope.adviceFilter = "";
					$scope.getPagedAdvices(1);
				};

				$scope.resetRelationFilter = function() {
					$scope.relationFilter = "";
					$scope.getPagedRelations(1);
				};

				$scope.resetPrincipleFilter = function() {
					$scope.principleFilter = "";
					$scope.getPagedPrinciples(1);
				};
				
				$scope.resetReportDefinitionFilter = function() {
					$scope.reportDefinitionFilter = "";
					$scope.getPagedReportDefinitions(1);
				};
	

				// element-specific functions for filtering
				// don't want to search id or objectId

				function containsAdviceFilter(element) {
					
					console.debug("Checking advice: ", $scope.adviceFilter);

					// check if advice filter is empty
					if ($scope.adviceFilter === "" || $scope.adviceFilter == null) return true;

					// otherwise check if upper-case advice filter matches upper-case element name or detail
					if ( element.detail.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;
					if ( element.name.toString().toUpperCase().indexOf( $scope.adviceFilter.toString().toUpperCase()) != -1) return true;

					// otherwise return false
					return false;
				};
				
				function containsReportDefinitionFilter(element) {
					
					console.debug("Checking definitions: ", $scope.reportDefinitionFilter);

					// check if advice filter is empty
					if ($scope.reportDefinitionFilter === "" || $scope.reportDefinitionFilter == null) return true;

					console.debug('checking name against filter', element.name)
					// otherwise check if upper-case report definition filter matches upper-case element name or detail
					if ( element.name.toString().toUpperCase().indexOf( $scope.reportDefinitionFilter.toString().toUpperCase()) != -1) return true;
					
					console.debug('not found');
					
					// otherwise return false
					return false;
				};

				function containsRelationFilter(element) {
					
					console.debug("Checking relation: ", $scope.relationFilter);

					// check if relation filter is empty
					if ($scope.relationFilter === "" || $scope.relationFilter == null) return true;

					// otherwise check if upper-case relation filter matches upper-case element name or detail
					if ( element.terminologyId.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;
					if ( element.name.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;

					// otherwise return false
					return false;
				};

				function containsPrincipleFilter(element) {

					// check if principle filter is empty
					if ($scope.principleFilter === "" || $scope.principleFilter == null) return true;

					// otherwise check if upper-case principle filter matches upper-case element name or detail
					if ( element.principleId.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
					//if ( element.detail.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
					if ( element.name.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;
					if ( element.sectionRef.toString().toUpperCase().indexOf( $scope.principleFilter.toString().toUpperCase()) != -1) return true;

					// otherwise return false
					return false;
				};

		

	

				// helper function to sort a JSON array by field
				$scope.sortByKey = function sortById(array, key) {
					return array.sort(function(a, b) {
						var x = a[key]; var y = b[key];
						return ((x < y) ? -1 : ((x > y) ? 1 : 0));
					});
				};
				
				$scope.getSourceVersion = function(project) {
					return project.sourceTerminology + " " + project.sourceTerminologyVersion;
				};
				
				$scope.getDestinationVersion = function(project)  {
					return project.destinationTerminology + " " + project.destinationTerminologyVersion;
				};
				
				$scope.getMapType = function(project) {
					for (var i = $scope.allowableMapTypes.length; i--;) {
						if ($scope.allowableMapTypes[i].name === project.mapRefsetPattern)
							return $scope.allowableMapTypes[i];
					}
				};
				
				$scope.getWorkflowType  = function(project) {
					for (var i = $scope.allowableWorkflowTypes.length; i--;) {
						if ($scope.allowableWorkflowTypes[i].name === project.workflowType)
							return $scope.allowableWorkflowTypes[i];
					}
				};
				
				$scope.getMapRelationStyle = function(project) {
					for (var i = $scope.allowableMapRelationStyles.length; i--;) {
						if ($scope.allowableMapRelationStyles[i].name === project.mapRelationStyle)
							return $scope.allowableMapRelationStyles[i];
					}
				};
				
				$scope.getHandler = function(project) {
					for (var i = $scope.handlers.length; i--;) {
						if ($scope.handlers[i].value === project.projectSpecificAlgorithmHandlerClass)
							return $scope.handlers[i];
					}
				};
				
				$scope.setEditingPerformed =  function(component) {
					editingPerformed.push(component);
				};				
				
				$scope.isComponentChanged = function(component) {
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i] === component) {
		        			return true;
		        		}
		        	}
					return false;
				};
				
				function removeComponentFromArray(arr, component) {
		    		// remove component
		        	for(var i = arr.length; i--;) {
		        		if(arr[i] === component) {
		        			arr.splice(i, 1);
		        		}
		        	}
				};
				
				// indicates if any unsaved advice
				$scope.adviceInEditingPerformed = function() {
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].name != null && editingPerformed[i].isComputed != null) {
		        			return true;
		        		}
		        	}
					return false;
				};
				
				// reverts advice to last saved state
				$scope.revertUnsavedAdvices = function() {
					console.log("in resetAdvice");
					
					// clear advice from editingPerformed
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].name != null && editingPerformed[i].isComputed != null) {
		        			editingPerformed.splice(i, 1);
		        		}
		        	}
					// get last saved state of advice
					$http({
						url: root_mapping + "advice/advices",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapAdvices = data.mapAdvice;
						localStorageService.add('mapAdvices', data.mapAdvice);
						$rootScope.$broadcast('localStorageModule.notification.setMapAdvices',{key: 'mapAdvices', mapAdvices: data.mapAdvices});  
						$scope.allowableMapAdvices = localStorageService.get('mapAdvices');
						$scope.getPagedAdvices(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
				};

				// indicates if any unsaved principle
				$scope.principleInEditingPerformed = function() {
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].principleId != null) {
		        			return true;
		        		}
		        	}
					return false;
				};
				
				// reverts principle to last saved state
				$scope.revertUnsavedPrinciples = function() {
					console.log("in resetPrinciple");
					
					// clear principle from editingPerformed
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].principleId != null) {
		        			editingPerformed.splice(i, 1);
		        		}
		        	}
					// get last saved state of principle
					$http({
						url: root_mapping + "principle/principles",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapPrinciples = data.mapPrinciple;
						localStorageService.add('mapPrinciples', data.mapPrinciple);
						$rootScope.$broadcast('localStorageModule.notification.setMapPrinciples',{key: 'mapPrinciples', mapPrinciples: data.mapPrinciples});  
						$scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
						$scope.getPagedPrinciples(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
				};
				
				// indicates if any unsaved relation
				$scope.relationInEditingPerformed = function() {
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].terminologyId != null) {
		        			return true;
		        		}
		        	}
					return false;
				};
				
				// reverts relation to last saved state
				$scope.revertUnsavedRelations = function() {
					console.log("in resetRelation");
					
					// clear relation from editingPerformed
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].terminologyId != null) {
		        			editingPerformed.splice(i, 1);
		        		}
		        	}
					// get last saved state of relation
					$http({
						url: root_mapping + "relation/relations",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapRelations = data.mapRelation;
						localStorageService.add('mapRelations', data.mapRelation);
						$rootScope.$broadcast('localStorageModule.notification.setMapRelations',{key: 'mapRelations', mapRelations: data.mapRelations});  
						$scope.allowableMapRelations = localStorageService.get('mapRelations');
						$scope.getPagedRelations(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
				};
				
				// indicates if any unsaved ageRange
				$scope.ageRangeInEditingPerformed = function() {
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].lowerUnits != null) {
		        			return true;
		        		}
		        	}
					return false;
				};
				
				// reverts ageRange to last saved state
				$scope.revertUnsavedAgeRanges = function() {
					console.log("in resetAgeRange");
					
					// clear ageRange from editingPerformed
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].lowerUnits != null) {
		        			editingPerformed.splice(i, 1);
		        		}
		        	}
					// get last saved state of ageRange
					$http({
						url: root_mapping + "ageRange/ageRanges",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapAgeRanges = data.mapAgeRange;
						localStorageService.add('mapAgeRanges', data.mapAgeRange);
						$rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges',{key: 'mapAgeRanges', mapAgeRanges: data.mapAgeRanges});  
						$scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');
						$scope.getPagedAgeRanges(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
				};
				
				// indicates if any unsaved reportDefinition
				$scope.reportDefinitionInEditingPerformed = function() {
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].resultType != null) {
		        			return true;
		        		}
		        	}
					return false;
				};
				
				// reverts reportDefinition to last saved state
				$scope.revertUnsavedReportDefinitions = function() {
					console.log("in resetReportDefinition");
					
					// clear reportDefinition from editingPerformed
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].reportDefinitionId != null) {
		        			editingPerformed.splice(i, 1);
		        		}
		        	}
					// get last saved state of reportDefinitions
					$http({
						url: root_reporting + "definition/definitions",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.reportDefinitions = data.reportDefinition;
						localStorageService.add('reportDefinitions', data.reportDefinition);
						$rootScope.$broadcast('localStorageModule.notification.setReportDefinitions',{key: 'reportDefinitions', reportDefinitions: data.reportDefinitions});  
						$scope.allowableMapReportDefinitions = localStorageService.get('reportDefinitions');
						$scope.getPagedReportDefinitions(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
				};
				
				// indicates if any unsaved project
				$scope.projectInEditingPerformed = function() {
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].sourceTerminology != null) {
		        			return true;
		        		}
		        	}
					return false;
				};
				
				// reverts project to last saved state
				$scope.revertUnsavedProjects = function() {
					console.log("in resetProject");
					
					// clear project from editingPerformed
					for(var i = editingPerformed.length; i--;) {
		        		if(editingPerformed[i].sourceTerminology != null) {
		        			editingPerformed.splice(i, 1);
		        		}
		        	}
					// get last saved state of project
					$http({
						url: root_mapping + "project/projects",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						$scope.mapProjects = data.mapProject;
						localStorageService.add('mapProjects', data.mapProject);
						$rootScope.$broadcast('localStorageModule.notification.setMapProjects',{key: 'mapProjects', mapProjects: data.mapProjects});  
						$scope.allowableMapProjects = localStorageService.get('mapProjects');
						$scope.getPagedProjects(1, "");
					}).error(function(data, status, headers, config) {
						 $rootScope.handleHttpError(data, status, headers, config);
					});
				};
				
				// function to change project from the header
				$scope.changeFocusProject = function(mapProject) {
					$scope.focusProject = mapProject;
					console.debug("changing project to " + $scope.focusProject.name);

					// update and broadcast the new focus project
					localStorageService.add('focusProject', $scope.focusProject);
					$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

					// update the user preferences
					$scope.preferences.lastMapProjectId = $scope.focusProject.id;
					localStorageService.add('preferences', $scope.preferences);
					$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', userPreferences: $scope.preferences});

				};
				
				$scope.goToHelp = function() {
					var path;
					if ($scope.page != 'mainDashboard') {
						path = "help/" + $scope.page + "Help.html";
					} else {
						path = "help/" + $scope.currentRole + "DashboardHelp.html";
					}
					console.debug("go to help page " + path);
					// redirect page
					$location.path(path);
				};
	
	
			
				$scope.deleteAdvice = function(advice) {
					console.debug("in deleteAdvice from application");
					
					if (confirm("Are you sure that you want to delete a map advice?") == false)
						return;
					
					$http({						
						url: root_mapping + "advice/delete",
						dataType: "json",
						data: advice,
						method: "DELETE",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to deleteMapAdvice from application");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error deleting map advice from application.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "advice/advices",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {				
							$scope.mapAdvices = data.mapAdvice;
							$scope.resetAdviceFilter();
							for (var j = 0; j < $scope.focusProject.mapAdvice.length; j++) {
								if (advice.id === $scope.focusProject.mapAdvice[j].id) {
									$scope.focusProject.mapAdvice[j] = advice;
								}
							}
							localStorageService.add('mapAdvices', data.mapAdvice);
							$rootScope.$broadcast('localStorageModule.notification.setMapAdvices',{key: 'mapAdvices', mapAdvices: data.mapAdvices});  
							$scope.allowableMapAdvices = localStorageService.get('mapAdvices');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
							
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				

				$scope.updateAdvice = function(advice) {
					console.debug("in updateAdvice");
					$http({						
						url: root_mapping + "advice/update",
						dataType: "json",
						data: advice,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to updateMapAdvice");
						removeComponentFromArray(editingPerformed, advice);
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error updating map advice.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "advice/advices",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {				
							$scope.mapAdvices = data.mapAdvice;
							for (var j = 0; j < $scope.focusProject.mapAdvice.length; j++) {
								if (advice.id === $scope.focusProject.mapAdvice[j].id) {
									$scope.focusProject.mapAdvice[j] = advice;
								}
							}
							localStorageService.add('mapAdvices', data.mapAdvice);
							$rootScope.$broadcast('localStorageModule.notification.setMapAdvices',{key: 'mapAdvices', mapAdvices: data.mapAdvices});  
							$scope.allowableMapAdvices = localStorageService.get('mapAdvices');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
							
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				
				$scope.submitNewMapAdvice = function(mapAdviceName, mapAdviceDetail,
						allowableForNullTarget, isComputed) {
					console.debug("in submitNewMapAdvice");
					var obj = 			  
					{"name":mapAdviceName,"detail":mapAdviceDetail,
							"isAllowableForNullTarget":allowableForNullTarget,
							"isComputed":isComputed};
					
					$http({						
						url: root_mapping + "advice/add",
						dataType: "json",
						data: obj,
						method: "PUT",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to addMapAdvice");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error adding new map advice.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "advice/advices",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							$scope.mapAdvices = data.mapAdvice;
							$scope.resetAdviceFilter();
							localStorageService.add('mapAdvices', data.mapAdvice);
							$rootScope.$broadcast('localStorageModule.notification.setMapAdvices',{key: 'mapAdvices', mapAdvices: data.mapAdvices});  
							$scope.allowableMapAdvices = localStorageService.get('mapAdvices');
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});
				};

				$scope.deleteRelation = function(relation) {
					console.debug("in deleteRelation from application");
					
					if (confirm("Are you sure that you want to delete a map relation?") == false)
						return;
					
					$http({						
						url: root_mapping + "relation/delete",
						dataType: "json",
						data: relation,
						method: "DELETE",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to deleteMapRelation from application");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error deleting map relation from application.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "relation/relations",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {				
							$scope.mapRelations = data.mapRelation;
							$scope.resetRelationFilter();
							for (var j = 0; j < $scope.focusProject.mapRelation.length; j++) {
								if (relation.id === $scope.focusProject.mapRelation[j].id) {
									$scope.focusProject.mapRelation[j] = relation;
								}
							}
							localStorageService.add('mapRelations', data.mapRelation);
							$rootScope.$broadcast('localStorageModule.notification.setMapRelations',{key: 'mapRelations', mapRelations: data.mapRelations});  
							$scope.allowableMapRelations = localStorageService.get('mapRelations');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
							
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				
				$scope.updateRelation = function(relation) {
					console.debug("in updateRelation");
					$http({						
						url: root_mapping + "relation/update",
						dataType: "json",
						data: relation,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to updateMapRelation");
						removeComponentFromArray(editingPerformed, relation);
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error updating map relation.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "relation/relations",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {				
							$scope.mapRelations = data.mapRelation;
							for (var j = 0; j < $scope.focusProject.mapRelation.length; j++) {
								if (relation.id === $scope.focusProject.mapRelation[j].id) {
									$scope.focusProject.mapRelation[j] = relation;
								}
							}
							localStorageService.add('mapRelations', data.mapRelation);
							$rootScope.$broadcast('localStorageModule.notification.setMapRelations',{key: 'mapRelations', mapRelations: data.mapRelations});  
							$scope.allowableMapRelations = localStorageService.get('mapRelations');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
							
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				
				$scope.submitNewMapRelation = function(mapRelationName, mapRelationAbbreviation, mapRelationTerminologyId,
						allowableForNullTarget, isComputed) {
					console.debug("in submitNewMapRelation for application");
					var obj = 	
					{"terminologyId":mapRelationTerminologyId,"name":mapRelationName,"abbreviation":mapRelationAbbreviation,
						"isAllowableForNullTarget":allowableForNullTarget,"isComputed":isComputed};
					$http({						
						url: root_mapping + "relation/add",
						dataType: "json",
						data: obj,
						method: "PUT",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to addMapRelation to application");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error adding new map relation for the application.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "relation/relations",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							$scope.mapRelations = data.mapRelation;
							$scope.resetRelationFilter();
							localStorageService.add('mapRelations', data.mapRelation);
							$rootScope.$broadcast('localStorageModule.notification.setMapRelations',{key: 'mapRelations', mapRelations: data.mapRelations});  
							$scope.allowableMapRelations = localStorageService.get('mapRelations');
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});
				};
				
				

				
				$scope.updatePrinciple = function(principle) {
					console.debug("in updatePrinciple for application");
					$http({						
						url: root_mapping + "principle/update",
						dataType: "json",
						data: principle,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to updateMapPrinciple in application");
						removeComponentFromArray(editingPerformed, principle);
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error updating map principle.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "principle/principles",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							
							$scope.mapPrinciples = data.mapPrinciple;
							for (var j = 0; j < $scope.focusProject.mapPrinciple.length; j++) {
								if (principle.id === $scope.focusProject.mapPrinciple[j].id) {
									$scope.focusProject.mapPrinciple[j] = principle;
								}
							}
							localStorageService.add('mapPrinciples', data.mapPrinciple);
							$rootScope.$broadcast('localStorageModule.notification.setMapPrinciples',{key: 'mapPrinciples', mapPrinciples: data.mapPrinciples});  
							$scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				
				$scope.deletePrinciple = function(principle) {
					console.debug("in deletePrinciple from application");
					
					if (confirm("Are you sure that you want to delete a map principle?") == false)
						return;
					
					$http({						
						url: root_mapping + "principle/delete",
						dataType: "json",
						data: principle,
						method: "DELETE",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to deleteMapPrinciple from application");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error deleting map principle from application.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "principle/principles",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {				
							$scope.mapPrinciples = data.mapPrinciple;
							$scope.resetPrincipleFilter();
							for (var j = 0; j < $scope.focusProject.mapPrinciple.length; j++) {
								if (principle.id === $scope.focusProject.mapPrinciple[j].id) {
									$scope.focusProject.mapPrinciple[j] = principle;
								}
							}
							localStorageService.add('mapPrinciples', data.mapPrinciple);
							$rootScope.$broadcast('localStorageModule.notification.setMapPrinciples',{key: 'mapPrinciples', mapPrinciples: data.mapPrinciples});  
							$scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
							
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				$scope.submitNewMapPrinciple = function(mapPrincipleName, mapPrincipleId, mapPrincipleDetail, mapPrincipleSectionRef) {
					console.debug("in submitNewMapPrinciple");
					var obj = 	
					{"name":mapPrincipleName, "principleId":mapPrincipleId,"detail":mapPrincipleDetail,
						"sectionRef":mapPrincipleSectionRef};
					$http({						
						url: root_mapping + "principle/add",
						dataType: "json",
						data: obj,
						method: "PUT",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to addMapPrinciple");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error adding new map principle.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "principle/principles",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							$scope.mapPrinciples = data.mapPrinciple;
							localStorageService.add('mapPrinciples', data.mapPrinciple);
							$rootScope.$broadcast('localStorageModule.notification.setMapPrinciples',{key: 'mapPrinciples', mapPrinciples: data.mapPrinciples});  
							$scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});
				};
				
				$scope.deleteAgeRange = function(ageRange) {
					console.debug("in deleteAgeRange");
					
					if (confirm("Are you sure that you want to delete an age range?") == false)
						return;
					
					for (var j = 0; j < $scope.focusProject.mapAgeRange.length; j++) {
						if (ageRange.name === $scope.focusProject.mapAgeRange[j].name) {
							$scope.focusProject.mapAgeRange.splice(j, 1);
						}
					}
				    // update and broadcast the updated focus project
					localStorageService.set('focusProject', $scope.focusProject);
					$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject}); 
					$scope.updateMapProject($scope.focusProject);
				};
				
				$scope.updateAgeRange = function(ageRange) {
					console.debug("in updateAgeRange for application");
					$http({						
						url: root_mapping + "ageRange/update",
						dataType: "json",
						data: ageRange,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to updateAgeRange");
						removeComponentFromArray(editingPerformed, ageRange);
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error updating age range.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "ageRange/ageRanges",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							
							$scope.mapAgeRanges = data.mapAgeRange;
							for (var j = 0; j < $scope.focusProject.mapAgeRange.length; j++) {
								if (ageRange.id === $scope.focusProject.mapAgeRange[j].id) {
									$scope.focusProject.mapAgeRange[j] = ageRange;
								}
							}
							localStorageService.add('mapAgeRanges', data.mapAgeRange);
							$rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges',{key: 'mapAgeRanges', mapAgeRanges: data.mapAgeRanges});  
							$scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				
				
				$scope.submitNewMapAgeRange = function(name, lowerInclusive, lowerUnits, lowerValue,
						 upperInclusive, upperUnits, upperValue) {
				   console.debug("in submitNewMapAgeRange");
					  var obj =	 {
								"lowerInclusive": true,
								"lowerUnits": lowerUnits,
								"lowerValue": lowerValue,
								"name": name,
								"upperInclusive": true,
								"upperUnits": upperUnits,
								"upperValue": upperValue
							  };
					$http({						
						url: root_mapping + "ageRange/add",
						dataType: "json",
						data: obj,
						method: "PUT",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to addMapAgeRange");
					              //make the record pristine
					              $scope.ageRangeForm.$setPristine();
					              $scope.name = "";
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error adding new map age range.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "ageRange/ageRanges",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							$scope.mapAgeRanges = data.mapAgeRange;
							localStorageService.add('mapAgeRanges', data.mapAgeRange);
							$rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges',{key: 'mapAgeRanges', mapAgeRanges: data.mapAgeRanges});  
							$scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});
				};
				
				$scope.deleteReportDefinition = function(reportDefinition) {
					console.debug("in deleteReportDefinition from application");
					
					if (confirm("Are you sure that you want to delete a map reportDefinition?") == false)
						return;
					
					$http({						
						url: root_reporting + "definition/delete",
						dataType: "json",
						data: reportDefinition,
						method: "DELETE",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to deleteReportDefinition from application");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error deleting map reportDefinition from application.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_reporting + "definition/definitions",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {				
							$scope.reportDefinitions = data.reportDefinition;
							$scope.resetReportDefinitionFilter();
							for (var j = 0; j < $scope.focusProject.reportDefinition.length; j++) {
								if (reportDefinition.id === $scope.focusProject.reportDefinition[j].id) {
									$scope.focusProject.reportDefinition[j] = reportDefinition;
								}
							}
							localStorageService.add('reportDefinitions', data.reportDefinition);
							$rootScope.$broadcast('localStorageModule.notification.setReportDefinitions',{key: 'reportDefinitions', reportDefinitions: data.reportDefinitions});  
							$scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
							
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};

				$scope.updateReportDefinition = function(reportDefinition) {
					console.debug("in updateReportDefinition");
					$http({						
						url: root_reporting + "definition/update",
						dataType: "json",
						data: reportDefinition,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to updateReportDefinition");
						removeComponentFromArray(editingPerformed, reportDefinition);
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error updating map reportDefinition.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_reporting + "definition/definitions",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {				
							$scope.reportDefinitions = data.reportDefinition;
							for (var j = 0; j < $scope.focusProject.reportDefinition.length; j++) {
								if (reportDefinition.id === $scope.focusProject.reportDefinition[j].id) {
									$scope.focusProject.reportDefinition[j] = reportDefinition;
								}
							}
							localStorageService.add('reportDefinitions', data.reportDefinition);
							$rootScope.$broadcast('localStorageModule.notification.setReportDefinitions',{key: 'reportDefinitions', reportDefinitions: data.reportDefinitions});  
							$scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');
							
							// update and broadcast the updated focus project
							localStorageService.add('focusProject', $scope.focusProject);
							$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

							$scope.updateMapProject($scope.focusProject);
							
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});				
				};
				
				$scope.submitNewReportDefinition = function(name, roleRequired, resultType, queryType, diffReport, timePeriod, query) {
				
					if (timePeriod === undefined || timePeriod === '')
						timePeriod = null;
					
					console.debug("in submitNewReportDefinition");
					var obj = 			  
					{		"name":name,
							"roleRequired":roleRequired,
							"resultType":resultType,
							"queryType":queryType,
							"diffReport":diffReport,
							"timePeriod":timePeriod,
							"query":query };
					
					$http({						
						url: root_reporting + "definition/add",
						dataType: "json",
						data: obj,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to addReportDefinition");
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error adding new map reportDefinition.";
						$rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_reporting + "definition/definitions",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							$scope.reportDefinitions = data.reportDefinition;
							$scope.resetReportDefinitionFilter();
							localStorageService.add('reportDefinitions', data.reportDefinition);
							$rootScope.$broadcast('localStorageModule.notification.setReportDefinitions',{key: 'reportDefinitions', reportDefinitions: data.reportDefinitions});  
							$scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						});

					});
				};

				
				$scope.updateMapProject = function(project) {				
					$http({
						url: root_mapping + "project/update",
						dataType: "json",
						data: project,
						method: "POST",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
						console.debug("success to updateMapProject");
						removeComponentFromArray(editingPerformed, project);
						//localStorageService.set('focusProject', project);
						//$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: project});  
					
						// retrieve updated projects and broadcast
						$http({
							url: root_mapping + "project/projects",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}	

						}).success(function(data) {
									localStorageService.add('mapProjects', data.mapProject);
									$rootScope.$broadcast(
											'localStorageModule.notification.setMapProjects', {
												key : 'mapProjects',
												mapProjects : data.mapProject
											});
									$scope.mapProjects = data.mapProject;
						}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;
						    $rootScope.handleHttpError(data, status, headers, config);
						});

						
						$rootScope.$broadcast(
								'localStorageModule.notification.setMapProjects', {
									key : 'mapProjects',
									mapProjects : $scope.mapProjects
								});
						
					}).error(function(data, status, headers, config) {
						$scope.recordError = "Error updating map project.";
						$rootScope.handleHttpError(data, status, headers, config);
					});
				};
				
				$scope.deleteMapProject = function(project) {
					
					if (confirm("ARE YOU ABSOLUTELY SURE?\n\n  Deleting a project requires recomputing workflow and rerunning indexes, and may cause workflow problems for other projects.") == false)
						return;
					
					$rootScope.glassPane++;
					$http({
						url: root_mapping + "project/delete",
						method: "DELETE",
						dataType: "json",
						data: project,
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(data) {
					  	$rootScope.glassPane--;

					  	$scope.successMsg = 'Successfully deleted project ' + project.id;
					  	
					  	var mapProjects = [];
					  	for (var i = 0; i < $scope.mapProjects.length; i++) {
					  		if ($scope.mapProjects[i].id != project.id) {
					  			mapProjects.push($scope.mapProjects[i]);
					  		}
					  	}
					  	$scope.mapProjects = mapProjects;
					  	
					  	localStorageService.add('mapProjects', $scope.mapProjects);
					  	
					  	// broadcast change
					  	$rootScope.$broadcast(
								'localStorageModule.notification.setMapProjects', {
									key : 'mapProjects',
									mapProjects : $scope.mapProjects
								});
					  	
									 
					}).error(function(data, status, headers, config) {
					    $rootScope.glassPane--;
					    $rootScope.handleHttpError(data, status, headers, config);
					});
				};
				
				

				
				$scope.submitNewMapProject = function(
						newMapProjectName, newMapProjectSourceVersion, newMapProjectDestinationVersion, 
						newMapProjectRefSetId, newMapProjectPublished, newMapProjectRuleBased, 
						newMapProjectGroupStructure, newMapProjectPublic, 
					    newMapProjectScopeDescendantsFlag, newMapProjectScopeExcludedDescendantsFlag,
					    newMapProjectMapType, newWorkflowType, newMapRelationStyle, newHandler,
					    newMapProjectMapPrincipleSourceDocumentName) {
						
						// get source and version and dest and version
						var res = newMapProjectSourceVersion.split(" "); 
						var newMapProjectSource = res[0];
						var newMapProjectSourceVersion = res[1];
						res = newMapProjectDestinationVersion.split(" "); 
						var newMapProjectDestination = res[0];
						var newMapProjectDestinationVersion = res[1];
					  	var newMapProjectRefSetName = "";
						
						// check ref set id
					  	if (newMapProjectRefSetId == null || newMapProjectRefSetId.length == 0) {
							alert("You must specify a unique ref set id.");
							return;
						}
					  	
					  	// get the refsetid name
					  	$http({
							url: root_content + "concept/id/" + newMapProjectSource + "/" +
							  newMapProjectSourceVersion + "/" + newMapProjectRefSetId,
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(data) {
							console.debug("Success in getting concept for refsetid.");
							newMapProjectRefSetName = data.defaultPreferredName;
						}).error(function(data, status, headers, config) {
							 $rootScope.handleHttpError(data, status, headers, config);
						}).then(function(data) {
					  						  	
							var project = {
								"name": newMapProjectName,
								"sourceTerminology": newMapProjectSource,
								"sourceTerminologyVersion": newMapProjectSourceVersion,
								"destinationTerminology": newMapProjectDestination,
								"destinationTerminologyVersion": newMapProjectDestinationVersion,
								"refSetId": newMapProjectRefSetId,
								"refSetName": newMapProjectRefSetName,
								"published": newMapProjectPublished,
								"ruleBased": newMapProjectRuleBased,
								"groupStructure": newMapProjectGroupStructure,
								"mapRefsetPattern": newMapProjectMapType.name, 
								"workflowType": newWorkflowType.name, 
								"mapRelationStyle": newMapRelationStyle.name,
								"public": newMapProjectPublic,
								"projectSpecificAlgorithmHandlerClass": newHandler,
								"scopeDescendantsFlag": newMapProjectScopeDescendantsFlag,
								"scopeExcludedDescendantsFlag": newMapProjectScopeExcludedDescendantsFlag,
								"mapPrincipleSourceDocumentName": newMapProjectMapPrincipleSourceDocumentName
							};
							
						
							if ($scope.checkRefSetId(project) == false) {
								alert("The ref set id you provided is not unique.");
								return;
							}
						
							$rootScope.glassPane++;
						

							$http({
								url: root_mapping + "project/add",
								method: "PUT",
								dataType: "json",
								data: project,
								headers: {
								"Content-Type": "application/json"
								}
							}).success(function(data) {
								$rootScope.glassPane--;
						  	
								// set the admin project to response
								var newProject = data;

								$scope.successMsg = 'Successfully added project ' + newProject.id;
								
								newProject.mapAdministrator.push($scope.currentUser);
								$http({
									url: root_mapping + "project/update",
									dataType: "json",
									data: newProject,
									method: "POST",
									headers: {
										"Content-Type": "application/json"
									}
								}).success(function(data) {
									console.debug("success to updateMapProject");
									// add to local projects and to cache
									$scope.mapProjects.push(newProject);
									localStorageService.add('mapProjects', $scope.mapProjects);
							  	
									// broadcast change
									$rootScope.$broadcast(
										'localStorageModule.notification.setMapProjects', {
											key : 'mapProjects',
											mapProjects : $scope.mapProjects
										});
									
								}).error(function(data, status, headers, config) {
									$scope.recordError = "Error updating map project.";
									$rootScope.handleHttpError(data, status, headers, config);
								});
								
								
								
								
							}).error(function(data, status, headers, config) {
								$rootScope.glassPane--;
								$rootScope.handleHttpError(data, status, headers, config);
							});		
						});
					};
					
					$scope.checkRefSetId = function(project) {
						for (var i = 0; i < $scope.mapProjects.length; i++) {
							if ($scope.mapProjects[i].id != project.id
									&& $scope.mapProjects[i].refSetId === project.refSetId) {
								return false;
							}			
						}
						return true;
					};
					
					$scope.onFileSelect = function($files) {
					    //$files: an array of files selected, each file has name, size, and type.
					    for (var i = 0; i < $files.length; i++) {
					      var $file = $files[i];
					      $upload.upload({
					        url: root_mapping + "upload/" + $scope.focusProject.id,
					        file: $file,
					        progress: function(e){}
					      }).then(function(data, status, headers, config) {
					        // file is uploaded successfully
					        console.log(data);
					      }); 
					    }
					};
			}]);

