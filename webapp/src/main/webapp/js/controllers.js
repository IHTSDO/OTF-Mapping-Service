'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', ['ui.bootstrap', 'ui.sortable', 'mapProjectAppDirectives', 'mapProjectAppServices', 'mapProjectAppDashboards']);

//var root_url = "${base.url}/mapping-rest/";
var root_url = "/mapping-rest/";

var root_mapping = root_url + "mapping/";
var root_content = root_url + "content/";
var root_metadata = root_url + "metadata/";
var root_workflow = root_url + "workflow/";
var root_security = root_url + "security/";
var root_reporting = root_url + "reporting/";

mapProjectAppControllers.run(function($rootScope, $http, localStorageService, $location) {
	
	// global variable to display a glass pane (if non-zero) preventing user interaction
	$rootScope.glassPane = 0;
	
	// global variable, contains user-viewable error text displayed one very page if not empty
	$rootScope.globalError = '';
	
	// global function to handle any type of error.  Currently only specifically implemented for authorizatoin failures.
    $rootScope.handleHttpError = function (data, status, headers, config) {
		if (status == "401") {
	    	$rootScope.globalError = $rootScope.globalError + " Authorization failed.  Please log in again.";
			$location.path("/");
		} else if (data.indexOf("AuthToken does not have a valid username.") > 0) {
			$location.path("/");
		} else {
			$rootScope.globalError = data.replace(/"/g, '');
		}
		window.scrollTo(0,0);		
    }
    
    // global function to reset the global error
    $rootScope.resetGlobalError = function () {
    	$rootScope.globalError = '';
    };
    
    // variable to indicate whether the current page is "dirty"
    // i.e. leaving this page might cause data to be lost
    // at writing, only two pages with this status are:
    // - mapRecord.html
    // - compareRecords.html
    $rootScope.currentPageDirty = false;
    
    // root watcher to check for page changes, reload events, window closes, etc
    // if on a "dirty" page, prompt for confirmation from user
    $rootScope.$on('$locationChangeStart', function (event) {
    	
    	console.log('$locationChangeStart changed!', $rootScope.currentPageDirty);
    	
    	if ($rootScope.currentPageDirty == true) {
    		if(!confirm("Are you sure you want to leave this page? Any data you have entered will be lost.")) {
    			console.debug("PREVENTING DEFAULT");
   		      event.preventDefault();
   		   	} else {
   		   		// always set this to false
   		   		// it is the responsibility of a "dirty" page controller to set this to true
   		   		console.debug("Setting dirty to false");
   		   		$rootScope.currentPageDirty = false;
   		   	}
    	}
    });
});



// Navigation
mapProjectAppControllers.controller('LoginCtrl', ['$scope', 'localStorageService', '$rootScope', '$location', '$http',
                                                  function ($scope, localStorageService, $rootScope, $location, $http) {
    $scope.page =  'login';
    $scope.mapUsers = [];
    $scope.userName = '';
    
    //$rootScope.globalError = 'rootScopeGlobalError';
    $scope.globalError = $rootScope.globalError;
		
    // clear the local storage service
    localStorageService.clearAll();
	
	// set the user, role, focus project, and preferences to null (i.e. clear) by broadcasting to rest of app
	$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: null});  
	$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: null});  
	$rootScope.$broadcast('localStorageModule.notification.setFocusProject', {key: 'focusProject', focusProject: null});
	$rootScope.$broadcast('localStorageModule.notification.setPreferences', {key: 'preferences', preferences: null});

	// initial values for pick-list
	$scope.roles = [
	                'Viewer',
	                'Specialist',
	                'Lead',
	                'Administrator'];
	$scope.role = $scope.roles[0];  
	
	// login button directs to next page based on role selected
	$scope.goGuest = function () {
		$scope.userName = "guest";
		$scope.role = "Viewer";
		$scope.password = "Sn0m3dDefPass";
		$scope.go();
	}
	
	// login button directs to next page based on role selected
	$scope.go = function () {
		
		// reset the global error on log in attempt
		$scope.globalError = "";

		console.debug($scope.role);

		var path = "";

		// check that user has been selected
		if ($scope.userName == null) {
			alert("You must specify a user");
		} else if ($scope.password == null) {
			alert("You must enter a password");
		} else {
		
			
			// authenticate the user
			var query_url = root_security + "authenticate/" + $scope.userName;
			
			console.debug($scope.userName);
			
			// turn on the glass pane during login process/authentication
			// turned off at each error stage or before redirecting to dashboards
			$rootScope.glassPane++;
			
			$http({
				url: query_url,
				dataType: "json",
				data: $scope.password,
				method: "POST",
				headers: {
					"Content-Type": "text/plain"
				// save userToken from authentication
				}}).success(function(data) {
					console.debug(data);
				
					localStorageService.add('userToken', data);				
					$scope.userToken = localStorageService.get('userToken');
					
					// set default header to contain userToken
					$http.defaults.headers.common.Authorization = $scope.userToken;
					
					// retrieve projects
					$http({
						url: root_mapping + "project/projects",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	

					}).success(
							function(data) {
								localStorageService.add('mapProjects', data.mapProject);
								$rootScope.$broadcast(
										'localStorageModule.notification.setMapProjects', {
											key : 'mapProjects',
											mapProjects : data.mapProject
										});
								console.debug(data.mapProject);
								$scope.mapProjects = data.mapProject;
					}).error(function(data, status, headers, config) {
						$rootScope.glassPane--;
					    $rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {

					// retrieve users
					$http({
						url: root_mapping + "user/users",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {
									$scope.mapUsers = data.mapUser;
						localStorageService.add('mapUsers', data.mapUser);
						$rootScope.$broadcast('localStorageModule.notification.setMapUsers',{key: 'mapUsers', mapUsers: data.mapUsers});  
						// find the mapUser object
						for (var i = 0; i < $scope.mapUsers.length; i++)  {
							if ($scope.mapUsers[i].userName === $scope.userName) {
								$scope.mapUser = $scope.mapUsers[i];
							}
						}
						
						// add the user information to local storage
						localStorageService.add('currentUser', $scope.mapUser);

						// broadcast the user information to rest of app
						$rootScope.$broadcast('localStorageModule.notification.setUser',{key: 'currentUser', currentUser: $scope.mapUser});
					}).error(function(data, status, headers, config) {
						$rootScope.glassPane--;
					    $rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
					
					// retrieve the user preferences
						$http({
										url: root_mapping + "userPreferences/user/id/" + $scope.userName,
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}	
					}).success(function(data) {

						$scope.preferences = data;
						$scope.preferences.lastLogin = new Date().getTime();
						localStorageService.add('preferences', $scope.preferences);
						
						// check for a last-visited project
						$scope.focusProject = null;
						for (var i = 0; i < $scope.mapProjects.length; i++)  {
							if ($scope.mapProjects[i].id === $scope.preferences.lastMapProjectId) {
								$scope.focusProject = $scope.mapProjects[i];
							}
						}
						
						// if project not found, set to first retrieved project
						if ($scope.focusProject == null) {
							$scope.focusProject = $scope.mapProjects[0];
						}
						
						
						console.debug('Last project: ');
						console.debug($scope.focusProject);
						localStorageService.add('focusProject', $scope.focusProject);
						localStorageService.add('userPreferences', $scope.preferences);
						$rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {key: 'userPreferences', preferences: $scope.preferences});
						$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  

					}).error(function(data, status, headers, config) {
						$rootScope.glassPane--;
					    $rootScope.handleHttpError(data, status, headers, config);

					}).then(function(data) {
							$http({
											url: root_mapping + "project/user/id/" + $scope.userName,
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}	
						}).success(function(data) {
							console.debug(data);
							// check if user has role in focusProject
							var found = 0;
							for (var i = 0; i < data.mapProject.length; i++) {
								if (data.mapProject[i].id === $scope.focusProject.id) {
									found = 1;
								} 
							}

							// otherwise change focusProject
							if (found == 0 && data.mapProject.length > 0) {
								$scope.focusProject = data.mapProject[0];
								console.debug($scope.focusProject);
								localStorageService.add('focusProject', $scope.focusProject);
								$rootScope.$broadcast('localStorageModule.notification.setFocusProject',{key: 'focusProject', focusProject: $scope.focusProject});  			
							}
						}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;
						    $rootScope.handleHttpError(data, status, headers, config);
						}).then(function(data) {
;
							$http({
												url: root_mapping + "userRole/user/id/" + $scope.userName + "/project/id/" + $scope.focusProject.id,
								dataType: "json",
								method: "GET",
								headers: {
									"Content-Type": "application/json"
								}	
							}).success(function(data) {
								console.debug(data);
								$scope.role = data.replace(/"/g, '');
								
						
								if ($scope.role.toLowerCase() == "specialist") {
									path = "/specialist/dash";
									$scope.role = "Specialist";
								} else if ($scope.role.toLowerCase() == "lead") {
									path = "/lead/dash";
									$scope.role = "Lead";
								} else if ($scope.role.toLowerCase() == "administrator") {
									path = "/admin/dash";
									$scope.role = "Administrator";
								} else  {
									path = "/viewer/dash";
									$scope.role = "Viewer";
								}

								// add the user information to local storage
								localStorageService.add('currentRole', $scope.role);

								// broadcast the user information to rest of app
								$rootScope.$broadcast('localStorageModule.notification.setRole',{key: 'currentRole', currentRole: $scope.role});
					
								$rootScope.glassPane--;
								
								// redirect page
								$location.path(path);
						

							}).error(function(data, status, headers, config) {
								$rootScope.glassPane--;
							    $rootScope.handleHttpError(data, status, headers, config);
							});		
						  });
						});	
					 });
				  }); 
				}).error(function(data, status, headers, config) {
					  $rootScope.glassPane--;
					  $rootScope.globalError = data.replace(/"/g, '');
					
				      $rootScope.handleHttpError(data, status, headers, config);
				}).then(function(data) {
					$http({
						url: root_metadata + "terminology/terminologies/latest",
						dataType: "json",
						method: "GET",
						headers: {
							"Content-Type": "application/json"
						}
					}).success(function(response) {
						var keyValuePairs = response.keyValuePair;
						for (var i = 0; i < keyValuePairs.length; i++) {
							console.debug("Retrieving metadata for " + keyValuePairs[i].key + ", " + keyValuePairs[i].value);		
							addMetadataToLocalStorageService(keyValuePairs[i].key, keyValuePairs[i].value);
						}
					}).error(function(data, status, headers, config) {
						$rootScope.glassPane--;	
					    $rootScope.handleHttpError(data, status, headers, config);
					}).then(function(data) {
						$http({
							url: root_mapping + "mapProject/metadata",
							dataType: "json",
							method: "GET",
							headers: {
								"Content-Type": "application/json"
							}
						}).success(function(response) {

							localStorageService.add('mapProjectMetadata', response);
							$rootScope.$broadcast(
									'localStorageModule.notification.setMapProjectMetadata', {
										key : 'mapProjectMetadata',
										value : response
									});
						}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;	
						    $rootScope.handleHttpError(data, status, headers, config);
						});
					});

				});
		}
		


		// function to add metadata to local storage service
		// written to ensure correct handling of asynchronous responses
		function addMetadataToLocalStorageService(terminology, version) {
			$http({
				url: root_metadata + "metadata/terminology/id/" + terminology + "/" + version,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(response) {
				console.debug("Adding metadata for " + terminology);
				localStorageService.add('metadata_' + terminology, response.keyValuePairList);
			});
		}

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
}]);


