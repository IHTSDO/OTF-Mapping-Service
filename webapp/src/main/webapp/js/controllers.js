'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', [ 'ui.bootstrap',
  'ui.sortable', 'mapProjectAppDirectives', 'mapProjectAppDashboards' ]);

// var root_url = '${base.url}/mapping-rest/';
var root_url = '/mapping-rest/';

var root_mapping = root_url + 'mapping/';
var root_content = root_url + 'content/';
var root_metadata = root_url + 'metadata/';
var root_workflow = root_url + 'workflow/';
var root_security = root_url + 'security/';
var root_reporting = root_url + 'reporting/';

mapProjectAppControllers
  .run(function($rootScope, $http, localStorageService, $location, utilService) {

    // global function to handle any type of error. Currently only
    // specifically implemented for authorization failures.
    $rootScope.handleHttpError = function(data, status, headers, config) {
      // $rootScope.globalError = data.replace(/"/g, '');
      utilService.handleError(data.replace(/"/g, ''));
      if (status == '401') {
        $location.path('/');
      }
      window.scrollTo(0, 0);
    };

    // global function to handle an error that returns user to dashboard
    // currently used for improper viewing of records in editing
    $rootScope.handleReturnToDashboardError = function(errorString, currentRole) {
      $rootScope.globalError = errorString;
      $location.path('/' + currentRole.toLowerCase() + '/dash');
      window.scrollTo(0, 0);

    };

    // global function to reset the global error
    $rootScope.resetGlobalError = function() {
      $rootScope.globalError = '';
      $rootScope.globalLongError = '';
    };

    // global variable to display a glass pane (if non-zero) preventing user
    // interaction
    $rootScope.glassPane = 0;

    // global variable, contains user-viewable error text displayed one very
    // page if not empty
    $rootScope.resetGlobalError();

    // check if local storage service can be accessed
    if (localStorageService.isSupported() == false) {
      $rootScope.globalError = 'It appears your browsers security settings will prevent the tool from functioning correctly.  Check that cookies are enabled and/or that your browser allows setting local data, then reload this page.';
    }

    // variable to indicate whether the current page is 'dirty'
    // i.e. leaving this page might cause data to be lost
    // at writing, only two pages with this status are:
    // - mapRecord.html
    // - compareRecords.html
    $rootScope.currentPageDirty = false;
    
    $rootScope.title = 'IHTSDO Mapping';

    // root watcher to check for page changes, reload events, window closes,
    // etc
    // if on a 'dirty' page, prompt for confirmation from user
    
    // TODO RIGHT HERE IS THE THING OMG THE THING
    $rootScope
      .$on(
        '$locationChangeStart',
        function(event) {
          if ($rootScope.currentPageDirty == true) {
            if (!confirm('Are you sure you want to leave this page? Any data you have entered will be lost.')) {
              event.preventDefault();
            } else {
              // always set this to false
              // it is the responsibility of a 'dirty' page controller to
              // set
              // this to true
              $rootScope.currentPageDirty = false;
            }
          }
        });

  });

// Navigation
mapProjectAppControllers.controller('LoginCtrl', [
  '$scope',
  'localStorageService',
  '$rootScope',
  '$location',
  '$http',
  '$routeParams',
  function($scope, localStorageService, $rootScope, $location, $http, $routeParams) {
    $scope.page = 'login';
    $scope.mapUsers = [];
    $scope.userName = '';
    // $rootScope.globalError = 'rootScopeGlobalError';
    $scope.globalError = $rootScope.globalError;

    // login button directs to next page based on role selected
    $scope.goGuest = function(autologinLocation, refSetId) {
      $scope.userName = 'guest';
      $scope.role = 'Viewer';
      $scope.password = 'guest';
      $scope.go(autologinLocation, refSetId);
    };

    // login button directs to next page based on role selected
    $scope.go = function(autologinLocation, refSetId) {

      // reset the global error on log in attempt
      $rootScope.resetGlobalError();

      var path = '';

      // check that user has been selected
      if ($scope.userName == null) {
        alert('You must specify a user');
      } else if ($scope.password == null) {
        alert('You must enter a password');
      } else {

        // authenticate the user
        var query_url = root_security + 'authenticate/' + $scope.userName;

        // turn on the glass pane during login process/authentication
        // turned off at each error stage or before redirecting to
        // dashboards
        $rootScope.glassPane++;

        $http({
          url : query_url,
          dataType : 'json',
          data : $scope.password,
          method : 'POST',
          headers : {
            'Content-Type' : 'text/plain'
          // save userToken from authentication
          }
        }).success(
          function(data) {

            localStorageService.add('userToken', data);
            $scope.userToken = localStorageService.get('userToken');

            // set default header to contain userToken
            $http.defaults.headers.common.Authorization = $scope.userToken;

            // retrieve projects
            console.debug('retrieving map projects ');
            $http({
              url : root_mapping + 'project/projects',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }

            }).success(function(data) {

              localStorageService.add('mapProjects', data.mapProject);
              $rootScope.$broadcast('localStorageModule.notification.setMapProjects', {
                key : 'mapProjects',
                mapProjects : data.mapProject
              });
              $scope.mapProjects = data.mapProject;
            }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $rootScope.handleHttpError(data, status, headers, config);
            }).then(
              function(data) {

                console.debug('retrieving users');

                // retrieve users
                $http({
                  url : root_mapping + 'user/users',
                  dataType : 'json',
                  method : 'GET',
                  headers : {
                    'Content-Type' : 'application/json'
                  }
                }).success(function(data) {
                  $scope.mapUsers = data.mapUser;
                  localStorageService.add('mapUsers', data.mapUser);
                  $rootScope.$broadcast('localStorageModule.notification.setMapUsers', {
                    key : 'mapUsers',
                    mapUsers : data.mapUsers
                  });
                  // find the mapUser object
                  for (var i = 0; i < $scope.mapUsers.length; i++) {
                    if ($scope.mapUsers[i].userName === $scope.userName) {
                      $scope.mapUser = $scope.mapUsers[i];
                    }
                  }

                  // add the user information to
                  // local storage
                  localStorageService.add('currentUser', $scope.mapUser);

                  // broadcast the user
                  // information to rest of app
                  $rootScope.$broadcast('localStorageModule.notification.setUser', {
                    key : 'currentUser',
                    currentUser : $scope.mapUser
                  });
                }).error(function(data, status, headers, config) {
                  $rootScope.glassPane--;
                  $rootScope.handleHttpError(data, status, headers, config);
                }).then(
                  function(data) {

                    console.debug('retrieving user preferences');

                    // retrieve the user preferences
                    $http({
                      url : root_mapping + 'userPreferences/user/id/' + $scope.userName,
                      dataType : 'json',
                      method : 'GET',
                      headers : {
                        'Content-Type' : 'application/json'
                      }
                    }).success(function(data) {

                      console.debug('getting focus project ' + $scope.mapProjects);

                      $scope.preferences = data;
                      $scope.preferences.lastLogin = new Date().getTime();
                      localStorageService.add('preferences', $scope.preferences);

                      if (typeof refSetId === 'undefined') {
                        // check for a
                        // last-visited
                        // project
                        $scope.focusProject = null;
                        for (var i = 0; i < $scope.mapProjects.length; i++) {
                          if ($scope.mapProjects[i].id === $scope.preferences.lastMapProjectId) {
                            $scope.focusProject = $scope.mapProjects[i];
                          }
                        }
                      }

                      else {
                        $scope.focusProject = null;
                        for (var i = 0; i < $scope.mapProjects.length; i++) {
                          if ($scope.mapProjects[i].refSetId == refSetId) {
                            $scope.focusProject = $scope.mapProjects[i];
                            break;
                          }
                        }
                      }

                      // if project not
                      // found, set to first
                      // retrieved project
                      if ($scope.focusProject == null) {
                        $scope.focusProject = $scope.mapProjects[0];
                      }

                      localStorageService.add('focusProject', $scope.focusProject);
                      localStorageService.add('userPreferences', $scope.preferences);
                      $rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {
                        key : 'userPreferences',
                        preferences : $scope.preferences
                      });
                      $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                        key : 'focusProject',
                        focusProject : $scope.focusProject
                      });

                    }).error(function(data, status, headers, config) {
                      $rootScope.glassPane--;
                      $rootScope.handleHttpError(data, status, headers, config);

                    }).then(
                      function(data) {

                        $http(
                          {
                            url : root_mapping + 'userRole/user/id/' + $scope.userName
                              + '/project/id/' + $scope.focusProject.id,
                            dataType : 'json',
                            method : 'GET',
                            headers : {
                              'Content-Type' : 'application/json'
                            }
                          }).success(function(data) {

                          $scope.role = data.replace(/"/g, '');
                          if ($scope.role === 'VIEWER')
                            $scope.role = 'Viewer';
                          else if ($scope.role === 'SPECIALIST')
                            $scope.role = 'Specialist';
                          else if ($scope.role === 'LEAD')
                            $scope.role = 'Lead';
                          else if ($scope.role === 'ADMINISTRATOR')
                            $scope.role = 'Administrator';
                          else
                            $scope.role = 'Could not determine role';

                          if (autologinLocation) {
                            path = autologinLocation;
                          } else if ($scope.role.toLowerCase() == 'specialist') {
                            path = '/specialist/dash';
                            $scope.role = 'Specialist';
                          } else if ($scope.role.toLowerCase() == 'lead') {
                            path = '/lead/dash';
                            $scope.role = 'Lead';
                          } else if ($scope.role.toLowerCase() == 'administrator') {
                            path = '/admin/dash';
                            $scope.role = 'Administrator';
                          } else {
                            path = '/viewer/dash';
                            $scope.role = 'Viewer';
                          }

                          // add the
                          // user
                          // information
                          // to local
                          // storage
                          localStorageService.add('currentRole', $scope.role);

                          // broadcast
                          // the user
                          // information
                          // to
                          // rest of
                          // app
                          $rootScope.$broadcast('localStorageModule.notification.setRole', {
                            key : 'currentRole',
                            currentRole : $scope.role
                          });

                          $rootScope.glassPane--;

                          // redirect
                          // page
                          $location.path(path);

                        }).error(function(data, status, headers, config) {
                          $rootScope.glassPane--;
                          $rootScope.handleHttpError(data, status, headers, config);
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
            url : root_mapping + 'mapProject/metadata',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(response) {

            localStorageService.add('mapProjectMetadata', response);
            $rootScope.$broadcast('localStorageModule.notification.setMapProjectMetadata', {
              key : 'mapProjectMetadata',
              value : response
            });
          }).error(function(data, status, headers, config) {

            $rootScope.handleHttpError(data, status, headers, config);
          });

        });
      }

    };

    // function to change project from the header
    $scope.changeFocusProject = function(mapProject) {
      $scope.focusProject = mapProject;
      // update and broadcast the new focus project
      localStorageService.add('focusProject', $scope.focusProject);
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
        key : 'focusProject',
        focusProject : $scope.focusProject
      });

      // update the user preferences
      $scope.preferences.lastMapProjectId = $scope.focusProject.id;
      localStorageService.add('preferences', $scope.preferences);
      $rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {
        key : 'userPreferences',
        userPreferences : $scope.preferences
      });

    };

    $scope.goToHelp = function() {
      var path;
      if ($scope.page != 'mainDashboard') {
        path = 'help/' + $scope.page + 'Help.html';
      } else {
        path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
      }

      // redirect page
      $location.path(path);
    };

    // Controller logic

    // If we are not using auto-login, then clear the local cache
    if (!$location.path().endsWith('/autologin')) {

      // clear the local storage service
      localStorageService.clearAll();

      // set the user, role, focus project, and preferences to null
      // (i.e.
      // clear)
      // by broadcasting to rest of app
      $rootScope.$broadcast('localStorageModule.notification.setUser', {
        key : 'currentUser',
        currentUser : null
      });
      $rootScope.$broadcast('localStorageModule.notification.setRole', {
        key : 'currentRole',
        currentRole : null
      });
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
        key : 'focusProject',
        focusProject : null
      });
      $rootScope.$broadcast('localStorageModule.notification.setPreferences', {
        key : 'preferences',
        preferences : null
      });

      // initial values for pick-list
      $scope.roles = [ 'Viewer', 'Specialist', 'Lead', 'Administrator' ];
      $scope.role = $scope.roles[0];

    }

    // Otherwise, checked if we are logged in
    // If so, proceed to location, otherwise call 'goGuest'
    else {
      $scope.mapUser = localStorageService.get('currentUser');

      // If there is a user, attempt to log in
      if ($scope.mapUser) {
        $scope.userToken = localStorageService.get('userToken');
        // set default header to contain userToken
        $http.defaults.headers.common.Authorization = $scope.userToken;

        // Make a call to test if we're logged in and to get preferences
        $http({
          url : root_mapping + 'userPreferences/user/id/' + $scope.mapUser.userName,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          // set scope preferences object
          $scope.preferences = data;
          $scope.preferences.lastLogin = new Date().getTime();
          // If we are logged in, change focus project
          var mapProjects = localStorageService.get('mapProjects');
          var mapProject = localStorageService.get('mapProjects')[0];
          for (var i = 0; i < mapProjects.length; i++) {
            if ($routeParams.refSetId == mapProjects[i].refSetId) {
              mapProject = mapProjects[i];
              break;
            }
          }
          $scope.changeFocusProject(mapProject);

          // set location - should work for any autologin url
          $location.path($location.path().replace('/autologin', ''));

        }).error(function(data, status, headers, config) {

          // call go guest and set the focus project (via param?)
          $scope.goGuest($location.path().replace('/autologin', ''), $routeParams.refSetId);
        });

      } else {

        // call go guest and set the focus project (via param?)
        $scope.goGuest('record/conceptId/' + $routeParams.conceptId, $routeParams.refSetId);

      }

    }

  } ]);