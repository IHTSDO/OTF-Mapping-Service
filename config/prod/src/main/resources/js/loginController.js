// Navigation
mapProjectAppControllers.controller('LoginCtrl', [
  '$scope',
  'localStorageService',
  '$rootScope',
  '$location',
  '$http',
  '$routeParams',
  'appConfig',
  function($scope, localStorageService, $rootScope, $location, $http, $routeParams, appConfig) {
    $scope.appConfig = appConfig;
    $scope.page = 'login';
    $scope.mapUsers = [];
    $scope.userName = '';
    $scope.pending = true;

    // $rootScope.globalError = 'rootScopeGlobalError';
    $scope.globalError = $rootScope.globalError;

    // login button directs to next page based on role selected
    $scope.goGuest = function(autologinLocation, refSetId) {
      $scope.userName = 'guest';
      $scope.role = 'Viewer';
      $scope.password = 'guest';
      $scope.go(autologinLocation, refSetId);
    };

    // Login button, redirect to IMS
    $scope.login = function() {
      // This line requires maven filtering
      window.location.href = appConfig['security.handler.IMS.url'] + '/#/login?serviceReferer='
        + appConfig['base.url'] + '/index.html';
    }

    // / / login button directs to next page based on role selected
    $scope.go = function(autologinLocation, refSetId) {

      // / / reset the global error on log in attempt
      $rootScope.resetGlobalError();

      var path = '';

      // / / check that user has been selected
      if ($scope.userName == null) {
        alert('You must specify a user');
      } else if ($scope.password == null) {
        alert('You must enter a password');
      } else {

        // / / authenticate the user
        var query_url = root_security + 'authenticate/' + $scope.userName;

        // / / turn on the glass pane during login process/authentication
        // / / turned off at each error stage or before redirecting to
        // / / dashboards
        $rootScope.glassPane++;

        $http({
          url : query_url,
          dataType : 'json',
          data : $scope.password,
          method : 'POST',
          headers : {
            'Content-Type' : 'text/plain'
          // / / save userToken from authentication
          }
        }).success(
          function(data) {

            localStorageService.add('userToken', data);
            $scope.userToken = localStorageService.get('userToken');

            // / / set default header to contain userToken
            $http.defaults.headers.common.Authorization = $scope.userToken;

            // / / retrieve projects
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

                // / / retrieve users
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
                  // / / find the mapUser object
                  for (var i = 0; i < $scope.mapUsers.length; i++) {
                    if ($scope.mapUsers[i].userName === $scope.userName) {
                      $scope.mapUser = $scope.mapUsers[i];
                    }
                  }

                  // / / add the user information to
                  // / / local storage
                  localStorageService.add('currentUser', $scope.mapUser);

                  // / / broadcast the user
                  // / / information to rest of app
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

                    // / / retrieve the user preferences
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
                        // / / check for a
                        // / / last-visited
                        // / / project
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

                      // / / if project not
                      // / / found, set to first
                      // / / retrieved project
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

                          // / / add the
                          // / / user
                          // / / information
                          // / / to local
                          // / / storage
                          localStorageService.add('currentRole', $scope.role);

                          // / / broadcast
                          // / / the user
                          // / / information
                          // / / to
                          // / / rest of
                          // / / app
                          $rootScope.$broadcast('localStorageModule.notification.setRole', {
                            key : 'currentRole',
                            currentRole : $scope.role
                          });

                          $rootScope.glassPane--;

                          // / / redirect
                          // / / page
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

    // / / function to change project from the header
    $scope.changeFocusProject = function(mapProject) {
      $scope.focusProject = mapProject;
      // / / update and broadcast the new focus project
      localStorageService.add('focusProject', $scope.focusProject);
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
        key : 'focusProject',
        focusProject : $scope.focusProject
      });

      // / / update the user preferences
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

      // / / redirect page
      $location.path(path);
    };

    // / /
    // / / Initialize
    // / /

    // / / Controller logic

    // / / If we are not using auto-login, then clear the local cache
    if (!$location.path().endsWith('/autologin')) {

      // / / clear the local storage service
      localStorageService.clearAll();

      // / / set the user, role, focus project, and preferences to null
      // / / (i.e.
      // / / clear)
      // / / by broadcasting to rest of app
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

      // / / initial values for pick-list
      $scope.roles = [ 'Viewer', 'Specialist', 'Lead', 'Administrator' ];
      $scope.role = $scope.roles[0];

      // / / Need to call IMS/api/accounts
      // / / THis requires an nginx setup to redirect ims-api to
      // / ims.ihtsdotools.org
      $rootScope.glassPane++;
      $http.get('ims-api/account').then(
        // / / Success
        function(response) {

          // / / Call "go" function
          $scope.userName = response.data.login;
          $scope.password = JSON.stringify(response.data);
          $rootScope.glassPane--;
          $scope.go();

        },
        // / / Error

        function(data, status, headers, config) {

          if (response.status == '302' || response.status == 302) {
            
            $rootScope.glassPane--;

            //https://ims.ihtsdotools.org/#/login?serviceReferer=https:%2F%2Fauthoring.ihtsdotools.org%2F#%2Fhome
            var referer = $location.protocol() + '://' + $location.host();
            var redirectUrl = response.headers['Location'] + '?serviceReferer='
              + encodeURIComponent(referer);
            $location.path(redirectUrl);

          } else {
            // / / $rootScope.globalError = response;
            // / / Show login buttons
            $scope.pending = false;
            $rootScope.glassPane--;
          }
        });

    }

    // / / Otherwise, checked if we are logged in
    // / / If so, proceed to location, otherwise call 'goGuest'
    else {
      $scope.mapUser = localStorageService.get('currentUser');

      // / / If there is a user, attempt to log in
      if ($scope.mapUser) {
        $scope.userToken = localStorageService.get('userToken');
        // / / set default header to contain userToken
        $http.defaults.headers.common.Authorization = $scope.userToken;

        // / / Make a call to test if we're logged in and to get preferences
        $http({
          url : root_mapping + 'userPreferences/user/id/' + $scope.mapUser.userName,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          // / / set scope preferences object
          $scope.preferences = data;
          $scope.preferences.lastLogin = new Date().getTime();
          // / / If we are logged in, change focus project
          var mapProjects = localStorageService.get('mapProjects');
          var mapProject = localStorageService.get('mapProjects')[0];
          for (var i = 0; i < mapProjects.length; i++) {
            if ($routeParams.refSetId == mapProjects[i].refSetId) {
              mapProject = mapProjects[i];
              break;
            }
          }
          $scope.changeFocusProject(mapProject);

          // / / set location - should work for any autologin url
          $location.path($location.path().replace('/autologin', ''));

        }).error(function(data, status, headers, config) {

          // / / call go guest and set the focus project (via param?)
          $scope.goGuest($location.path().replace('/autologin', ''), $routeParams.refSetId);
        });

      } else {

        // / / call go guest and set the focus project (via param?)
        $scope.goGuest('record/conceptId/' + $routeParams.conceptId, $routeParams.refSetId);

      }

    }

  } ]);