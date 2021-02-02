// Navigation
mapProjectAppControllers.controller('LoginCtrl', [
  '$scope',
  'localStorageService',
  '$rootScope',
  '$location',
  '$http',
  '$routeParams',
  'appConfig',
  'gpService',
  function ($scope, localStorageService, $rootScope, $location, $http, $routeParams, appConfig, gpService) {
    $scope.appConfig = appConfig;
    $scope.page = 'login';
    $scope.mapUsers = [];
    $scope.userName = '';
    $scope.pending = true;

    // $rootScope.globalError = 'rootScopeGlobalError';
    $scope.globalError = $rootScope.globalError;

    // login button directs to next page based on role selected
    $scope.goGuest = function (autologinLocation, refSetId) {
      $scope.userName = 'guest';
      $scope.role = 'Viewer';
      $scope.applicationRole = 'Viewer';
      $scope.password = 'guest';
      $scope.go(autologinLocation, refSetId);
    };

    $scope.parseJwt = function (token) {
      var base64Url = token.split('.')[1];
      var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      var jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
      return JSON.parse(jsonPayload);
    };

    // Login button, redirect to security.handler.OAUTH2.url
    $scope.login = function () {

      var url = appConfig['security.handler.OAUTH2.url.authorize']
      	+ '?client_id=' + appConfig['security.handler.OAUTH2.client_id']
      	+ '&response_type=' + appConfig['security.handler.OAUTH2.response_type']
      	+ '&redirect_uri=' + appConfig['security.handler.OAUTH2.redirect_uri']
      	+ '&response_mode=' + appConfig['security.handler.OAUTH2.response_mode']
      	+ '&scope=' + appConfig['security.handler.OAUTH2.scope']

      console.debug("url =", url);
      window.location.href = url;
    }

    // login button directs to next page based on role selected
    $scope.go = function (autologinLocation, refSetId) {

      // reset the global error on log in attempt
      $rootScope.resetGlobalError();

      var path = '';
      $scope.pending = false;

      // set default header to contain userToken
      $http.defaults.headers.common.Authorization = $scope.userToken;

      // retrieve projects
      console.debug('retrieving map projects ');
      $http({
        url: root_mapping + 'project/projects',
        dataType: 'json',
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }

      }).success(function (data) {

        localStorageService.add('mapProjects', data.mapProject);
        $rootScope.$broadcast('localStorageModule.notification.setMapProjects', {
          key: 'mapProjects',
          mapProjects: data.mapProject
        });
        $scope.mapProjects = data.mapProject;
      }).error(function (data, status, headers, config) {
        gpService.decrement();
        $rootScope.handleHttpError(data, status, headers, config);
      }).then(
        function (data) {

          console.debug('retrieving users');

          // retrieve users
          $http({
            url: root_mapping + 'user/users',
            dataType: 'json',
            method: 'GET',
            headers: {
              'Content-Type': 'application/json'
            }
          }).success(function (data) {
            // reconstruct emails for ihtsdo.gov users - privacy caution
            // others will remain as 'Private email'
            for (var i = 0; i < data.mapUser.length; i++) {
              if (data.mapUser[i].email != 'Private email') {
                data.mapUser[i].email = data.mapUser[i].email + '@ihtsdo.gov';
              }
            }

            $scope.mapUsers = data.mapUser;
            localStorageService.add('mapUsers', data.mapUser);
            $rootScope.$broadcast('localStorageModule.notification.setMapUsers', {
              key: 'mapUsers',
              mapUsers: data.mapUsers
            });
            // find the mapUser object
            for (var i = 0; i < $scope.mapUsers.length; i++) {
              if ($scope.mapUsers[i].userName === $scope.userName) {
                $scope.mapUser = $scope.mapUsers[i];
              }
            }

            // add the user information to local storage
            localStorageService.add('currentUser', $scope.mapUser);

            // broadcast the user information to rest of app
            $rootScope.$broadcast('localStorageModule.notification.setUser', {
              key: 'currentUser',
              currentUser: $scope.mapUser
            });
          }).error(function (data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(
            function (data) {

              console.debug('retrieving user preferences');

              // retrieve the user preferences
              $http({
                url: root_mapping + 'userPreferences/user/id/' + $scope.userName,
                dataType: 'json',
                method: 'GET',
                headers: {
                  'Content-Type': 'application/json'
                }
              }).success(function (data) {

                console.debug('getting focus project ' + $scope.mapProjects);

                $scope.preferences = data;
                $scope.preferences.lastLogin = new Date().getTime();
                localStorageService.add('preferences', $scope.preferences);
                localStorageService.add('assignedTab', $scope.preferences.lastAssignedTab);
                localStorageService.add('assignedRadio', $scope.preferences.lastAssignedRadio);

                // if user is a guest, set a default project to avoid confusion to
                // the users if previous guest exited on non-default project
                if ($scope.userName == 'guest') {
                  for (var i = 0; i < $scope.mapProjects.length; i++) {
                    if ($scope.mapProjects[i].name.indexOf('SNOMEDCT_US') > 0
                      && $scope.mapProjects[i].name.indexOf('ICD10CM') > 0) {
                      $scope.focusProject = $scope.mapProjects[i];
                      break;
                    } else if ($scope.mapProjects[i].name.indexOf('SNOMEDCT') > 0
                      && $scope.mapProjects[i].name.indexOf('ICD11') > 0) {
                      $scope.focusProject = $scope.mapProjects[i];
                      break;
                    } else if ($scope.mapProjects[i].name.indexOf('SNOMEDCT') > 0
                      && $scope.mapProjects[i].name.indexOf('ICD10') > 0) {
                      $scope.focusProject = $scope.mapProjects[i];
                      break;
                    } else {
                      $scope.focusProject = $scope.mapProjects[0];
                    }
                  }
                } else if (typeof refSetId === 'undefined') {
                  // check for a last-visited project
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

                // if project not found, set to first retrieved project
                if ($scope.focusProject == null) {
                  $scope.focusProject = $scope.mapProjects[0];
                }

                localStorageService.add('focusProject', $scope.focusProject);
                localStorageService.add('userPreferences', $scope.preferences);
                $rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {
                  key: 'userPreferences',
                  preferences: $scope.preferences
                });
                $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                  key: 'focusProject',
                  focusProject: $scope.focusProject
                });

              }).error(function (data, status, headers, config) {
                gpService.decrement();
                $rootScope.handleHttpError(data, status, headers, config);

              }).then(
                function (data) {

                  $http(
                    {
                      url: root_mapping + 'userRole/user/id/' + $scope.userName,
                      dataType: 'json',
                      method: 'GET',
                      headers: {
                        'Content-Type': 'application/json'
                      }
                    }).success(function (data) {

                      $scope.applicationRole = data.replace(/"/g, '');
                      if ($scope.applicationRole === 'VIEWER')
                        $scope.applicationRole = 'Viewer';
                      else if ($scope.applicationRole === 'ADMINISTRATOR')
                        $scope.applicationRole = 'Administrator';
                      else
                        $scope.role = 'Viewer';

                      // add the user information to local storage
                      localStorageService.add('applicationRole', $scope.applicationRole);

                    }).error(function (data, status, headers, config) {
                      gpService.decrement();
                      $rootScope.handleHttpError(data, status, headers, config);
                    }).then(
                      function (data) {

                        $http(
                          {
                            url: root_mapping + 'userRole/user/id/' + $scope.userName
                              + '/project/id/' + $scope.focusProject.id,
                            dataType: 'json',
                            method: 'GET',
                            headers: {
                              'Content-Type': 'application/json'
                            }
                          }).success(function (data) {

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
                              $scope.role = 'Viewer';

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

                            // add the user information to local storage
                            localStorageService.add('currentRole', $scope.role);

                            // broadcast the user information to rest of app
                            $rootScope.$broadcast('localStorageModule.notification.setRole', {
                              key: 'currentRole',
                              currentRole: $scope.role
                            });

                            gpService.decrement();

                            // redirect page
                            $location.path(path);

                          }).error(function (data, status, headers, config) {
                            gpService.decrement();
                            $rootScope.handleHttpError(data, status, headers, config);
                          });
                      });
                });
            });
        });
    };

    // function to change project from the header
    $scope.changeFocusProject = function (mapProject) {
      $scope.focusProject = mapProject;
      // update and broadcast the new focus project
      localStorageService.add('focusProject', $scope.focusProject);
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
        key: 'focusProject',
        focusProject: $scope.focusProject
      });

      // update the user preferences
      $scope.preferences.lastMapProjectId = $scope.focusProject.id;
      localStorageService.add('preferences', $scope.preferences);
      $rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {
        key: 'userPreferences',
        userPreferences: $scope.preferences
      });

    };

    $scope.goToHelp = function () {
      var path;
      if ($scope.page != 'mainDashboard') {
        path = 'help/' + $scope.page + 'Help.html';
      } else {
        path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
      }

      // redirect page
      $location.path(path);
    };

    // / /
    // Initialize
    // / /

    // Controller logic

    // If we are not using auto-login, then clear the local cache
    console.debug($location.path());
    console.debug($routeParams.token);

    if (!$location.path().includes('autologin')) {

      // clear the local storage service
      localStorageService.clearAll();

      // set the user, role, focus project, and preferences to null
      // (i.e. clear) by broadcasting to rest of app
      $rootScope.$broadcast('localStorageModule.notification.setUser', {
        key: 'currentUser',
        currentUser: null
      });
      $rootScope.$broadcast('localStorageModule.notification.setRole', {
        key: 'currentRole',
        currentRole: null
      });
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
        key: 'focusProject',
        focusProject: null
      });
      $rootScope.$broadcast('localStorageModule.notification.setPreferences', {
        key: 'preferences',
        preferences: null
      });

      // initial values for pick-list
      $scope.roles = ['Viewer', 'Specialist', 'Lead', 'Administrator'];
      $scope.role = $scope.roles[0];
      
      $scope.pending = false;

    }

    // Otherwise, checked if we are logged in
    // If so, proceed to location, otherwise call 'goGuest'
    else {
	  
      $scope.userToken = $routeParams.token;
      $scope.authToken = $routeParams.token;
      $scope.password = $routeParams.token;
      var t = $scope.parseJwt($routeParams.token);
      $scope.userName = t.upn.toLowerCase();
      $scope.go();
      $scope.pending = true;

    }

  }]);