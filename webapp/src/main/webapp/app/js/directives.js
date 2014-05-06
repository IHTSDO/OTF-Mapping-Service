'use strict';

var mapProjectAppDirectives = angular.module('mapProjectAppDirectives', []);

/////////////////////////////////////////////////////
//Directives:
/////////////////////////////////////////////////////


mapProjectAppDirectives.directive('otfFooterDirective', ['$rootScope', 'localStorageService', function($rootScope, localStorageService) {

	return {
		templateUrl: './partials/footer.html',
		restrict: 'E', 
		transclude: true,    // allows us �swap� our content for the calling html
		replace: true,        // tells the calling html to replace itself with what�s returned here
		link: function(scope, element, attrs) { // to get scope, the element, and its attributes
			scope.user = localStorageService.get('currentUser');
		}
	};
}]);

mapProjectAppDirectives.directive(
		'otfHeaderDirective', 
		['$rootScope', '$http', '$location', 'localStorageService', 
		 function($rootScope, $http, $location, localStorageService) {

			return {
				templateUrl: './partials/header.html',
				restrict: 'E', 
				transclude: true,    // allows us swap our content for the calling html
				replace: true,        // tells the calling html to replace itself with what�s returned here
				link: function($scope, element, attrs) { // to get $scope, the element, and its attributes

					/*
					 * NOTE: None of these functions use passed parameters at this time.
					 * Instead they explicitly retrieve the locally stored value from the localStorageService
					 * 
					 * This is a possible optimization location if local storage becomes unwieldy
					 */

					// On initialization, reset all values to null -- used to ensure watch functions work correctly
					$scope.mapProjects 	= null;
					$scope.currentUser 	= null;
					$scope.currentRole 	= null;
					$scope.preferences 	= null;
					$scope.focusProject = null;

					// Used for Reload/Refresh purposes -- after setting to null, get the locally stored values
					$scope.mapProjects  = localStorageService.get('mapProjects');
					$scope.currentUser  = localStorageService.get('currentUser');
					$scope.currentRole  = localStorageService.get('currentRole');
					$scope.preferences  = localStorageService.get('preferences');
					$scope.focusProject = localStorageService.get('focusProject');

					// Notifications from LoginPage (i.e. initialization of webapp on first visit)
					// watch for user change
					$scope.$on('localStorageModule.notification.setUser', function(event, parameters) { 	
						console.debug("HEADER: Detected change in current user: " + parameters.currentUser);
						$scope.currentUser = parameters.currentUser;
					});

					// watch for user preferences change
					$scope.$on('localStorageModule.notification.setPreferences', function(event, parameters) { 	
						console.debug("HEADER: Detected change in preferences");
						$scope.preferences = parameters.preferences;
					});

					// watch for role change
					$scope.$on('localStorageModule.notification.setRole', function(event, parameters) { 	
						console.debug("HEADER: Detected change in current role: " + parameters.currentRole);
						$scope.currentRole = parameters.currentRole;
					});	

					// watch for focus project change (called when user clicks Login)
					$scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
						console.debug("HEADER: Detected change in focus project");
						$scope.focusProject = parameters.focusProject;  
					});

					// watch for change in available projects (called by LoginCtrl on load of web application)
					$scope.$on('localStorageModule.notification.setMapProjects', function(event, parameters) {	
						console.debug("HEADER: Detected change in map projects");
						$scope.mapProjects = parameters.mapProjects;
					});

					// watch for help notification events
					$scope.$on('localStorageModule.notification.page', function(event, parameters) { 	
						console.debug("HEADER:  Detected change in page");
						$scope.page = parameters.newvalue;

					});	

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
				}
			};
		}]);


mapProjectAppDirectives.directive('draggable', function() {
  return function(scope, element) {
    // this gives us the native JS object
    var el = element[0];
    
    el.draggable = true;
    
    el.addEventListener(
      'dragstart',
      function(e) {
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('Text', this.id);
        this.classList.add('drag');
        return false;
      },
      false
    );
    
    el.addEventListener(
      'dragend',
      function(e) {
        this.classList.remove('drag');
        return false;
      },
      false
    );
  };
});

mapProjectAppDirectives.directive('droppable', function() {
  return {
    scope: {
      drop: '&',
      bin: '='
    },
    link: function(scope, element) {
      // again we need the native object
      var el = element[0];
      
      el.addEventListener(
        'dragover',
        function(e) {
          e.dataTransfer.dropEffect = 'move';
          // allows us to drop
          if (e.preventDefault) e.preventDefault();
          this.classList.add('over');
          return false;
        },
        false
      );
      
      el.addEventListener(
        'dragenter',
        function(e) {
          this.classList.add('over');
          return false;
        },
        false
      );
      
      el.addEventListener(
        'dragleave',
        function(e) {
          this.classList.remove('over');
          return false;
        },
        false
      );
      
      el.addEventListener(
        'drop',
        function(e) {
          // Stops some browsers from redirecting.
          if (e.stopPropagation) e.stopPropagation();
          
          this.classList.remove('over');
          
          var binId = this.id;
          var item = document.getElementById(e.dataTransfer.getData('Text'));
          this.appendChild(item);
          // call the passed drop function
          scope.$apply(function(scope) {
            var fn = scope.drop();
            if ('undefined' !== typeof fn) {            
              fn(item.id, binId);
            }
          });
          
          return false;
        },
        false
      );
    }
  };
});






