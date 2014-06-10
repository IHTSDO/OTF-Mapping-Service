
'use strict';

angular.module('mapProjectApp.widgets.projectDetails', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('projectDetails', {
		title: 'Project Details',
		description: 'Displays details for a specific map project.',
		templateUrl: 'js/widgets/projectDetails/projectDetails.html',
		controller: 'projectDetailsCtrl',
		resolve: {
			data: function(projectDetailsService, config){
				if (!config.terminology){
					return 'SNOMEDCT';
				} 
				return config.terminology;
			}
		},
		edit: {}
	});
})
.service('projectDetailsService', function($q, $http){
	return {
		get: function(terminology){
			var deferred = $q.defer();
			$http({
				url: root_metadata + "all/" + terminology,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				if (data){
					data.terminology = terminology;
					deferred.resolve(data);
				} else {
					deferred.reject();
				}
			}).error(function() {
				deferred.reject();
			});
			return deferred.promise;
		}
	};
})
.controller('projectDetailsCtrl', 
			['$scope', '$http', '$sce', '$rootScope', '$location', 'localStorageService',
			 function ($scope, $http, $sce, $rootScope, $location, localStorageService) {

			    $scope.page =  'project';

				$scope.currentRole = localStorageService.get('currentRole');
				$scope.currentUser = localStorageService.get('currentUser');
				$scope.focusProject = localStorageService.get('focusProject');
				$scope.mapProjects = localStorageService.get("mapProjects");
				
				// watch for focus project change
				$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
					console.debug("MapProjectDetailCtrl: Detected change in focus project");
					$scope.focusProject = parameters.focusProject;  
				});
				
				$scope.$watch('focusProject', function() {

					console.debug('Formatting project details');


					// apply map type text styling
					if ($scope.focusProject.mapType === "SIMPLE_MAP") $scope.mapTypeText = "Simple Mapping";
					else if ($scope.focusProject.mapType === "COMPLEX_MAP") $scope.mapTypeText = "Complex Mapping";
					else if($scope.focusProject.mapType === "EXTENDED_MAP") $scope.mapTypeText = "Extended Mapping";
					else $scope.mapTypeText = "No mapping type specified";

					// apply relation style text styling
					console.debug($scope.focusProject.mapRelationStyle);
					console.debug($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE");
					if ($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE") $scope.mapRelationStyleText = "Map Category Style";
					else if ($scope.focusProject.mapRelationStyle === "RELATIONSHIP_STYLE") $scope.mapRelationStyleText = "Relationship Style";
					else $scope.mapRelationStyleText = "No relation style specified";

					// determine if this project has a principles document
					if ($scope.focusProject.destinationTerminology == "ICD10") {
						$scope.focusProject.mapPrincipleDocumentPath = "doc/";
						$scope.focusProject.mapPrincipleDocument = "ICD10_MappingPersonnelHandbook.docx";
						$scope.focusProject.mapPrincipleDocumentName = "Mapping Personnel Handbook";
					} else {
						$scope.focusProject.mapPrincipleDocument = null;
					}

					// set the scope maps
					$scope.scopeMap = {};
					$scope.scopeExcludedMap = {};
					
					// set pagination variables
					$scope.pageSize = 5;
					$scope.maxSize = 5;
					$scope.getPagedAdvices(1);
					$scope.getPagedRelations(1);
					$scope.getPagedPrinciples(1);
					$scope.getPagedScopeConcepts(1);
					$scope.getPagedScopeExcludedConcepts(1);
					$scope.orderProp = 'id';

				});

				$scope.goMapRecords = function () {
					console.debug($scope.role);

					var path = "/project/records";
						// redirect page
						$location.path(path);
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

				$scope.getPagedAdvices = function (page) {

					$scope.pagedAdvice = $scope.sortByKey($scope.focusProject.mapAdvice, 'id')
					.filter(containsAdviceFilter);
					$scope.pagedAdviceCount = $scope.pagedAdvice.length;
					$scope.pagedAdvice = $scope.pagedAdvice
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
				};

				$scope.getPagedRelations = function (page) {

					$scope.pagedRelation = $scope.sortByKey($scope.focusProject.mapRelation, 'id')
					.filter(containsRelationFilter);
					$scope.pagedRelationCount = $scope.pagedRelation.length;
					$scope.pagedRelation = $scope.pagedRelation
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
				};

				$scope.getPagedPrinciples = function (page) {

					$scope.pagedPrinciple = $scope.sortByKey($scope.focusProject.mapPrinciple, 'id')
					.filter(containsPrincipleFilter);
					$scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
					$scope.pagedPrinciple = $scope.pagedPrinciple
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);

					console.debug($scope.pagedPrinciple);
				};

				$scope.getPagedScopeConcepts = function (page) {
					console.debug("Called paged scope concept for page " + page); 
					
					$scope.pagedScopeConcept = $scope.focusProject.scopeConcepts;
					$scope.pagedScopeConceptCount = $scope.pagedScopeConcept.length;
					
					$scope.pagedScopeConcept = $scope.pagedScopeConcept
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
					
					
					// find concept based on source terminology
					for (var i = 0; i < $scope.pagedScopeConcept.length; i++) {
						$rootScope.glassPane++;
						$http({
							url: root_content + "concept/" 
							+ $scope.focusProject.sourceTerminology +  "/" 
							+ $scope.focusProject.sourceTerminologyVersion 
							+ "/id/" 
							+ $scope.focusProject.scopeConcepts[i],
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}	
						}).success(function(data) {
							$rootScope.glassPane--;
							var obj = {
									key: data.terminologyId,
									concept: data
							};  
							$scope.scopeMap[obj.key] = obj.concept.defaultPreferredName;
						}).error(function(error) {
							$rootScope.glassPane--;
							console.debug("Could not retrieve concept");
							$scope.error = $scope.error + "Could not retrieve Concept. ";    
						});

					}
					
					console.debug($scope.pagedScopeConcept);
				};

				$scope.getPagedScopeExcludedConcepts = function (page) {
					console.debug("Called paged scope excluded concept for page " + page);
					$scope.pagedScopeExcludedConcept = $scope.sortByKey($scope.focusProject.scopeExcludedConcepts, 'id')
					.filter(containsScopeExcludedConceptFilter);
					$scope.pagedScopeExcludedConceptCount = $scope.pagedScopeExcludedConcept.length;
					$scope.pagedScopeExcludedConcept = $scope.pagedScopeExcludedConcept
					.slice((page-1)*$scope.pageSize,
							page*$scope.pageSize);
					
					
					// fill the scope map for these variables
					for (var i = 0; i < $scope.pagedScopeExcludedConcept.length; i++) {
						$rootScope.glassPane++;
						$http({
							url: root_content + "concept/" 
							+ $scope.focusProject.sourceTerminology +  "/" 
							+ $scope.focusProject.sourceTerminologyVersion 
							+ "/id/" 
							+ $scope.focusProject.scopeExcludedConcepts[i],
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}	
						}).success(function(data) {
							$rootScope.glassPane--;
							var obj = {
									key: data.terminologyId,
									concept: data
							};  
							$scope.scopeExcludedMap[obj.key] = obj.concept.defaultPreferredName;
						}).error(function(error) {
							$rootScope.glassPane--;
							console.debug("Could not retrieve concept");
							$scope.error = $scope.error + "Could not retrieve Concept. ";    
						});
					}
					

					console.debug($scope.pagedScopeExcludedConcept);
				};

				// functions to reset the filter and retrieve unfiltered results

				$scope.resetAdviceFilter = function() {
					$scope.adviceFilter = "";
					$scope.getPagedAdvices(1);
				};

				$scope.resetRelationFilter = function() {
					$scope.relationFilter = "";
					$scope.getPagedRelationss(1);
				};

				$scope.resetPrincipleFilter = function() {
					$scope.principleFilter = "";
					$scope.getPagedPrinciples(1);
				};

				$scope.resetScopeConceptFilter = function() {
					$scope.scopeConceptFilter = "";
					$scope.getPagedScopeConcepts(1);
				};		

				$scope.resetScopeExcludedConceptFilter = function() {
					$scope.scopeExcludedConceptFilter = "";
					$scope.getPagedScopeExcludedConcepts(1);
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

				function containsRelationFilter(element) {

					// check if relation filter is empty
					if ($scope.relationFilter === "" || $scope.relationFilter == null) return true;

					// otherwise check if upper-case relation filter matches upper-case element name or detail
					if ( element.terminologyId.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;
					if ( element.name.toString().toUpperCase().indexOf( $scope.relationFilter.toString().toUpperCase()) != -1) return true;

					// otherwise return false
					return false;
				}

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
				}

				function containsScopeConceptFilter(element) {

					// check if scopeConcept filter is empty
					if ($scope.scopeConceptFilter === "" || $scope.scopeConceptFilter == null) return true;

					// otherwise check if upper-case scopeConcept filter matches upper-case element name or detail
					if ( element.scopeConceptId.toString().toUpperCase().indexOf( $scope.scopeConceptFilter.toString().toUpperCase()) != -1) return true;
					if ( element.name.toString().toUpperCase().indexOf( $scope.scopeConceptFilter.toString().toUpperCase()) != -1) return true;

					// otherwise return false
					return false;
				}		

				function containsScopeExcludedConceptFilter(element) {

					// check if scopeConcept filter is empty
					if ($scope.scopeExcludesConceptFilter === "" || $scope.scopeExcludesConceptFilter == null) return true;

					// otherwise check if upper-case scopeConcept filter matches upper-case element name or detail
					if ( element.scopeExcludesConceptId.toString().toUpperCase().indexOf( $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1) return true;
					if ( element.name.toString().toUpperCase().indexOf( $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1) return true;

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
				
				$scope.isEmailViewable = function(email) {
					console.debug('isEmailViewable');
					if (email.indexOf("ihtsdo.org") > -1) {
						return true;
					} else
						return false;
				};
			}]);



