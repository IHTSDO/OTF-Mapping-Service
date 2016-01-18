'use strict';

var mapProjectAppDashboards = angular.module('mapProjectAppDashboards', [
  'adf', 'LocalStorageModule' ]);

mapProjectAppDashboards
  .controller(
    'ResolveConflictsDashboardCtrl',
    function($scope, $routeParams, $rootScope, $location, $http,
      localStorageService) {

      // model variable
      $scope.model = null;

      // On initialization, reset all values to null -- used to
      // ensure watch
      // functions work correctly
      $scope.mapProjects = null;
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.preferences = null;
      $scope.focusProject = null;
      $rootScope.globalError = '';

      // Used for Reload/Refresh purposes -- after setting to
      // null, get the
      // locally stored values
      $scope.mapProjects = localStorageService.get('mapProjects');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.preferences = localStorageService.get('preferences');
      $scope.focusProject = localStorageService.get('focusProject');

      $scope.page = 'resolveConflictsDashboard';

      // initialize the default model
      setDefaultModel();

      // on successful user retrieval, construct the dashboard
      $scope.$watch([ 'preferences' ], function() {

        console.debug('MainDashboard: Preferences loaded, models = ',
          $scope.preferences.dashboardModels);

        if ($scope.page in $scope.preferences.dashboardModels) {
          console.debug('  user defined model found');
          $scope.model = JSON
            .parse($scope.preferences.dashboardModels[$scope.page]);

        } else {
          console.debug('  using default model (no user-defined model)');
          $scope.model = $scope.defaultModel;
        }

        // calculate the number of widgets
        // available (used to display edit icon)
        var widgetCt = 0;

        // if model has rows defined
        if ($scope.model != null && $scope.model.hasOwnProperty('rows')) {

          console.debug('model has rows');

          // cycle over rows
          for (var i = 0; i < $scope.model.rows.length; i++) {

            // if row has columns defined
            if ($scope.model.rows[i].hasOwnProperty('columns')) {

              console.debug('row has columns');

              // cycle over columns
              for (var j = 0; j < $scope.model.rows[i].columns.length; j++) {

                // if column has widgets
                // defined
                if ($scope.model.rows[i].columns[j].hasOwnProperty('widgets')) {

                  console.debug('column has widgets');

                  // add the number of
                  // widgets to count
                  widgetCt += $scope.model.rows[i].columns[j].widgets.length;
                }
              }
            }
          }
        }
        $scope.model.widgetCount = widgetCt;
        console.debug('Widgets found: ', $scope.model.widgetCount);

      });

      // function to reset to the default model (called from page)
      $scope.resetModel = function() {
        console.debug('Main dashboard:   Reset to default model');

        console.debug('user defined models: ',
          $scope.preferences.dashboardModels);

        // splice working oddly here, clunky workaround
        var models = {};
        for ( var key in $scope.preferences.dashboardModels) {
          if (key != $scope.page)
            models[key] = $scope.preferences.dashboardModels[key];
        }

        $scope.preferences.dashboardModels = models;

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
            localStorageService.add('preferences', $scope.preferences);
            location.reload();
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

        console.debug('Revised preferences: ',
          $scope.preferences.dashboardModels);
      };

      console.debug('CONTROLLER MODEL');
      console.debug($scope.model);

      $scope
        .$on(
          'adfDashboardChanged',
          function(event, name, model) {
            console.debug('Dashboard change detected by mainDashboard', model);
            localStorageService.set(name, model);

            $scope.preferences.dashboardModels[$scope.page] = JSON
              .stringify($scope.model);
            localStorageService.add('preferences', $scope.preferences);

            console.debug('Models', $scope.preferences.dashboardModels);

            // update the user preferences
            $http({
              url : root_mapping + 'userPreferences/update',
              dataType : 'json',
              data : $scope.preferences,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            })
              .success(function(data) {
                // do nothing

              })
              .error(
                function(data) {
                  if (response.indexOf('HTTP Status 401') != -1) {
                    $rootScope.globalError = 'Authorization failed.  Please log in again.';
                    $location.path('/');
                  }
                });

          });

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject',
        function(event, parameters) {
          console
            .debug('RecordDashboardCtrl:  Detected change in focus project');

          var path = '';

          if ($scope.currentRole === 'Specialist') {
            path = '/specialist/dash';
          } else if ($scope.currentRole === 'Lead') {
            path = '/lead/dash';
          } else if ($scope.currentRole === 'Administrator') {
            path = '/admin/dash';
          } else if ($scope.currentRole === 'Viewer') {
            path = '/viewer/dash';
          }
          console.debug('redirecting to ' + path);
          $location.path(path);
        });

      function setDefaultModel() {
        // initialize the default model based on project
        // parameters
        $scope.defaultModel = {

          structure : '12/6-6/12',
          rows : [
            {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'compareRecords',
                  title : 'Compare Records'
                } ]
              } ]
            },
            { // new row

              columns : [
                {
                  class : 'col-md-6',
                  widgets : [ {
                    type : 'mapRecord',
                    config : {
                      recordId : $routeParams.recordId
                    },
                    title : 'Map Record'
                  }, {
                    type : 'recordSummary',
                    config : {
                      record : null
                    },
                    title : 'Record Summary'
                  } ]
                },
                {
                  class : 'col-md-6',
                  widgets : [
                    {
                      type : 'mapEntry',
                      config : {
                        entry : $scope.entry
                      },
                      title : 'Map Entry'
                    },
                    {
                      type : 'terminologyBrowser',
                      config : {
                        terminology : $scope.focusProject.destinationTerminology,
                        terminologyVersion : $scope.focusProject.destinationTerminologyVersion
                      },
                      title : $scope.focusProject.destinationTerminology
                        + ' Browser'

                    } ],
                } // end second column
              ]
            // end columns

            } ]
        // end second row

        };
      }
      ;

      $scope.logout = function() {

        $rootScope.glassPane++;
        $http(
          {
            url : root_security + 'logout/user/id/'
              + $scope.currentUser.userName,
            method : 'POST',
            headers : {
              'Content-Type' : 'text/plain'
            // save userToken from authentication
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $location.path('/');
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $location.path('/');
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // function to change project from the header
      $scope.changeFocusProject = function(mapProject) {
        $scope.focusProject = mapProject;
        console.debug('changing project to ' + $scope.focusProject.name);
        // update and broadcast the new focus project
        localStorageService.add('focusProject', $scope.focusProject);
        $rootScope.$broadcast(
          'localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

        // update the user preferences
        $scope.preferences.lastMapProjectId = $scope.focusProject.id;
        localStorageService.add('preferences', $scope.preferences);
        $rootScope.$broadcast(
          'localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

        // by default, changing the focus project means this
        // record comparison
        // is no longer valid
        // therefore, return to dashboard

      };

      $scope.$on('localStorageModule.notification.setMapProjects', function(
        event, parameters) {
        $scope.mapProjects = parameters.mapProjects;
      });

      $scope.goToHelp = function() {
        var path;
        if ($scope.page != 'mainDashboard') {
          path = 'help/' + $scope.page + 'Help.html';
        } else {
          path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
        }
        console.debug('go to help page ' + path);
        // redirect page
        $location.path(path);
      };
    });

mapProjectAppDashboards
  .controller(
    'FeedbackConversationsDashboardCtrl',
    function($scope, $routeParams, $rootScope, $location, localStorageService) {

      // model variable
      $scope.model = null;

      // On initialization, reset all values to null -- used to
      // ensure watch
      // functions work correctly
      $scope.mapProjects = null;
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.preferences = null;
      $scope.focusProject = null;
      $rootScope.globalError = '';

      // Used for Reload/Refresh purposes -- after setting to
      // null, get the
      // locally stored values
      $scope.mapProjects = localStorageService.get('mapProjects');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.preferences = localStorageService.get('preferences');
      $scope.focusProject = localStorageService.get('focusProject');

      $scope.page = 'feedbackConversationsDashboard';

      // initialize the default model
      setDefaultModel();

      // on successful user retrieval, construct the dashboard
      $scope.$watch([ 'preferences' ], function() {

        console.debug('MainDashboard: Preferences loaded, models = ',
          $scope.preferences.dashboardModels);

        if ($scope.page in $scope.preferences.dashboardModels) {
          console.debug('  user defined model found');
          $scope.model = JSON
            .parse($scope.preferences.dashboardModels[$scope.page]);

        } else {
          console.debug('  using default model (no user-defined model)');
          $scope.model = $scope.defaultModel;
        }

        // calculate the number of widgets
        // available (used to display edit icon)
        var widgetCt = 0;

        // if model has rows defined
        if ($scope.model != null && $scope.model.hasOwnProperty('rows')) {

          console.debug('model has rows');

          // cycle over rows
          for (var i = 0; i < $scope.model.rows.length; i++) {

            // if row has columns defined
            if ($scope.model.rows[i].hasOwnProperty('columns')) {

              console.debug('row has columns');

              // cycle over columns
              for (var j = 0; j < $scope.model.rows[i].columns.length; j++) {

                // if column has widgets
                // defined
                if ($scope.model.rows[i].columns[j].hasOwnProperty('widgets')) {

                  console.debug('column has widgets');

                  // add the number of
                  // widgets to count
                  widgetCt += $scope.model.rows[i].columns[j].widgets.length;
                }
              }
            }
          }
        }
        $scope.model.widgetCount = widgetCt;
        console.debug('Widgets found: ', $scope.model.widgetCount);

      });

      // function to reset to the default model (called from page)
      $scope.resetModel = function() {
        console.debug('Main dashboard:   Reset to default model');

        console.debug('user defined models: ',
          $scope.preferences.dashboardModels);

        // splice working oddly here, clunky workaround
        var models = {};
        for ( var key in $scope.preferences.dashboardModels) {
          if (key != $scope.page)
            models[key] = $scope.preferences.dashboardModels[key];
        }

        $scope.preferences.dashboardModels = models;

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
            localStorageService.add('preferences', $scope.preferences);
            location.reload();
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

        console.debug('Revised preferences: ',
          $scope.preferences.dashboardModels);
      };

      console.debug('CONTROLLER MODEL');
      console.debug($scope.model);

      $scope
        .$on(
          'adfDashboardChanged',
          function(event, name, model) {
            console.debug('Dashboard change detected by mainDashboard', model);
            localStorageService.set(name, model);

            $scope.preferences.dashboardModels[$scope.page] = JSON
              .stringify($scope.model);
            localStorageService.add('preferences', $scope.preferences);

            console.debug('Models', $scope.preferences.dashboardModels);

            // update the user preferences
            $http({
              url : root_mapping + 'userPreferences/update',
              dataType : 'json',
              data : $scope.preferences,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            })
              .success(function(data) {
                // do nothing

              })
              .error(
                function(data) {
                  if (response.indexOf('HTTP Status 401') != -1) {
                    $rootScope.globalError = 'Authorization failed.  Please log in again.';
                    $location.path('/');
                  }
                });

          });

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject',
        function(event, parameters) {
          console
            .debug('MapProjectWidgetCtrl: Detected change in focus project');

        });

      function setDefaultModel() {
        // initialize the default model based on project
        // parameters
        $scope.defaultModel = {

          structure : '12/6-6/12',
          rows : [ {
            columns : [ {
              class : 'col-md-12',
              widgets : [ {
                type : 'feedbackConversation',
                title : 'Feedback Conversation'
              } ]
            } ]
          } ]
        };
      }
      ;

      $scope.logout = function() {

        $rootScope.glassPane++;
        $http(
          {
            url : root_security + 'logout/user/id/'
              + $scope.currentUser.userName,
            method : 'POST',
            headers : {
              'Content-Type' : 'text/plain'
            // save userToken from authentication
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $location.path('/');
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $location.path('/');
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // function to change project from the header
      $scope.changeFocusProject = function(mapProject) {
        $scope.focusProject = mapProject;
        console.debug('changing project to ' + $scope.focusProject.name);

        // update and broadcast the new focus project
        localStorageService.add('focusProject', $scope.focusProject);
        $rootScope.$broadcast(
          'localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

        // update the user preferences
        $scope.preferences.lastMapProjectId = $scope.focusProject.id;
        localStorageService.add('preferences', $scope.preferences);
        $rootScope.$broadcast(
          'localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

      };

      $scope.$on('localStorageModule.notification.setMapProjects', function(
        event, parameters) {
        $scope.mapProjects = parameters.mapProjects;
      });

      $scope.goToHelp = function() {
        var path;
        if ($scope.page != 'mainDashboard') {
          path = 'help/' + $scope.page + 'Help.html';
        } else {
          path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
        }
        console.debug('go to help page ' + path);
        // redirect page
        $location.path(path);
      };
    });

mapProjectAppDashboards
  .controller(
    'dashboardCtrl',
    function($rootScope, $scope, $http, $location, localStorageService) {

      $scope.model = null;

      // On initialization, reset all values to null -- used to
      // ensure watch
      // functions work correctly
      $scope.mapProjects = null;
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.preferences = null;
      $scope.focusProject = null;

      // Used for Reload/Refresh purposes -- after setting to
      // null, get the
      // locally stored values
      $scope.mapProjects = localStorageService.get('mapProjects');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.preferences = localStorageService.get('preferences');
      $scope.focusProject = localStorageService.get('focusProject');

      // on project and user retrieval, retrieve the role
      $scope.$watch([ 'focusProject, currentUser' ], function() {

        // if both project and user retrieved
        if ($scope.focusProject != null && $scope.currentUser != null) {

          // retrieve role
          $http(
            {
              url : root_mapping + 'userRole/user/id/'
                + $scope.currentUser.userName + '/project/id/'
                + $scope.focusProject.id,
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(
            function(data) {

              var role = null;
              data = new String(data);

              // strip quotes off of data
              while (data.indexOf('\'') != -1) {
                data = data.replace('\'', '');
              }

              if (data === 'VIEWER')
                role = 'Viewer';
              else if (data === 'SPECIALIST')
                role = 'Specialist';
              else if (data === 'LEAD')
                role = 'Lead';
              else if (data === 'ADMINISTRATOR')
                role = 'Administrator';
              else
                role = 'Could not determine role';

              $scope.currentRole = role;
              localStorageService.add('currentRole', role);
              console.debug('Setting current role to: ', $scope.currentRole);

              if ($scope.model == null) {
                console
                  .debug('dashboardCtrl:  No model set, setting to default');
                setDefaultModel();
                $scope.model = $scope.defaultModel;
              }

            }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $location.path('/');
            $rootScope.handleHttpError(data, status, headers, config);
          });

        }
      });

      $scope.page = 'mainDashboard';
      $scope.isModelInitialized = false; // flag to determine
      // whether the model
      // has been successfully retrieved

      console.debug('in dashboardCtrl');

      // watch for preferences change
      $scope.parameters = null;
      $scope.$on('localStorageModule.notification.setUserPreferences',
        function(event, parameters) {

          console.debug('dashboardCtrl:  Detected change in preferences');
          console.debug(parameters);
          $scope.parameters = parameters;
        });

      $scope.userToken = localStorageService.get('userToken');

      $scope.$watch([ 'userToken' ], function() {
        $http.defaults.headers.common.Authorization = $scope.userToken;
      });

      // initialize the default model
      setDefaultModel();

      // on successful user retrieval, construct the dashboard
      $scope.$watch([ 'preferences' ], function() {

        console.debug('dashboardCtrl:  Preferences or role changed');

        if ($scope.preferences == null) {
          console.debug('  preferences not yet set, waiting');
          return;
        }

        if ($scope.currentRole == null) {
          console.debug('  role not yet set, waiting');
          return;
        }

        if ($scope.page in $scope.preferences.dashboardModels) {
          console.debug('  user defined model found');
          $scope.model = JSON
            .parse($scope.preferences.dashboardModels[$scope.page]);

        } else {

          console.debug('    setting default model based on role',
            $scope.currentRole);

        }
      });

      $scope.$watch('model', function() {

        console.debug('dashboardCtrl: Model Changed', $scope.model);
        if ($scope.model == null || $scope.model == undefined)
          return;

        // calculate the number of widgets
        // available (used to display edit icon)
        var widgetCt = 0;

        // if model has rows defined
        if ($scope.model != null && $scope.model.hasOwnProperty('rows')) {

          console.debug('model has rows');

          // cycle over rows
          for (var i = 0; i < $scope.model.rows.length; i++) {

            // if row has columns defined
            if ($scope.model.rows[i].hasOwnProperty('columns')) {

              console.debug('row has columns');

              // cycle over columns
              for (var j = 0; j < $scope.model.rows[i].columns.length; j++) {

                // if column has widgets
                // defined
                if ($scope.model.rows[i].columns[j].hasOwnProperty('widgets')) {

                  console.debug('column has widgets');

                  // add the number of
                  // widgets to count
                  widgetCt += $scope.model.rows[i].columns[j].widgets.length;
                }
              }
            }
          }
        }
        $scope.model.widgetCount = widgetCt;
        console.debug('Widgets found: ', $scope.model.widgetCount);

      });

      // function to reset to the default model (called from page)
      $scope.resetModel = function() {
        console.debug('Main dashboard:   Reset to default model');

        console.debug('user defined models: ',
          $scope.preferences.dashboardModels);

        // splice working oddly here, clunky workaround
        var models = {};
        for ( var key in $scope.preferences.dashboardModels) {
          if (key != $scope.page)
            models[key] = $scope.preferences.dashboardModels[key];
        }

        $scope.preferences.dashboardModels = models;

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
            localStorageService.add('preferences', $scope.preferences);
            location.reload();
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

        console.debug('Revised preferences: ',
          $scope.preferences.dashboardModels);
      };

      // function to set the default model (called on page load)
      function setDefaultModel() {

        console.debug('Setting the default dashboard based on role: '
          + $scope.currentRole);

        $scope.page = 'mainDashboard';

        /**
         * Viewer with user 'guest' has the following widgets: - MapProject
         */
        if (!$scope.currentRole
          || ($scope.currentRole === 'Viewer' && $scope.currentUser.userName === 'guest')) {
          console.debug('  Setting guest model');

          $scope.defaultModel = {

            structure : '12/6-6/12',
            rows : [ {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'mapProject',
                  config : {},
                  title : 'Map Project'
                } ]
              } ]
            } ]
          };
        }

        /**
         * Viewer (non-guest) has the following widgets: - MapProject MST wanted
         * this functionality disabled. else if (!$scope.currentRole ||
         * $scope.currentRole === 'Viewer') { console.debug(' Setting viewer
         * model');
         * 
         * $scope.defaultModel = {
         * 
         * structure : '12/6-6/12', rows : [ { columns : [ { class :
         * 'col-md-12', widgets : [ { type : 'mapProject', config : {}, title :
         * 'Map Project' } ] } ] }, { columns : [ { class : 'col-md-12', widgets : [ {
         * type : 'feedback', title : 'Feedback' } ] } ] } ] }; }
         */

        /**
         * Specialist has the following widgets: - MapProject - WorkAvailable -
         * AssignedList - EditedList
         */
        else if ($scope.currentRole === 'Specialist') {

          console.debug('  Setting specialist model');

          $scope.defaultModel = {

            structure : '12/6-6/12',
            rows : [ {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'mapProject',
                  config : {},
                  title : 'Map Project'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-6',
                widgets : [ {
                  type : 'workAvailable',
                  config : {},
                  title : 'Available Work'
                } ]
              }, {
                class : 'col-md-6',
                widgets : [ {
                  type : 'assignedList',
                  config : {},
                  title : 'Assigned Work'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'feedback',
                  title : 'Feedback'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'editedList',
                  title : 'Recently Edited'
                } ]
              } ]
            } ]
          };

          /**
           * Lead has the following widgets -MapProject - WorkAvailable -
           * AssignedList - EditedList
           */
        } else if ($scope.currentRole === 'Lead') {

          console.debug('  Setting lead model');

          $scope.defaultModel = {

            structure : '12/6-6/12',
            rows : [ {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'mapProject',
                  config : {},
                  title : 'Map Project'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-6',
                widgets : [ {
                  type : 'workAvailable',
                  config : {},
                  title : 'Available Work'
                } ]
              }, {
                class : 'col-md-6',
                widgets : [ {
                  type : 'assignedList',
                  config : {},
                  title : 'Assigned Work'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'feedback',
                  title : 'Feedback'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'editedList',
                  title : 'Recently Edited'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'report',
                  title : 'Reports'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'qaCheck',
                  title : 'QA Checks'
                } ]
              } ]

            } ]
          };

          console.debug($scope.defaultModel);

          /**
           * Admin has the following widgets - MapProject - MetadataList -
           * AdminTools
           */
        } else if ($scope.currentRole === 'Administrator') {
          console.debug('  Setting administrator model');
          $scope.defaultModel = {

            structure : '12/6-6/12',
            rows : [ {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'mapProject',
                  config : {},
                  title : 'Map Project'
                } ]
              } ]
            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'recordAdmin',
                  config : {},
                  title : 'Record Administration'
                } ]
              } ]

            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'applicationAdmin',
                  config : {},
                  title : 'Application Administration'
                } ]
              } ]

            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'report',
                  config : {},
                  title : 'Reports'
                } ]
              } ]

            }, {
              columns : [ {
                class : 'col-md-12',
                widgets : [ {
                  type : 'qaCheck',
                  title : 'QA Checks'
                } ]
              } ]
            } ]
          };

        } else {
          console.debug('  Invalid role detected');
        }
      }

      $scope
        .$on(
          'adfDashboardChanged',
          function(event, name, model) {
            console.debug('Dashboard change detected by mainDashboard', model);
            localStorageService.set(name, model);

            $scope.preferences.dashboardModels[$scope.page] = JSON
              .stringify($scope.model);
            localStorageService.add('preferences', $scope.preferences);

            console.debug('Models', $scope.preferences.dashboardModels);

            // update the user preferences
            $http({
              url : root_mapping + 'userPreferences/update',
              dataType : 'json',
              data : $scope.preferences,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            })
              .success(function(data) {
                // do nothing

              })
              .error(
                function(data) {
                  if (response.indexOf('HTTP Status 401') != -1) {
                    $rootScope.globalError = 'Authorization failed.  Please log in again.';
                    $location.path('/');
                  }
                });

          });

      $scope.logout = function() {

        $rootScope.glassPane++;
        $http(
          {
            url : root_security + 'logout/user/id/'
              + $scope.currentUser.userName,
            method : 'POST',
            headers : {
              'Content-Type' : 'text/plain'
            // save userToken from authentication
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $location.path('/');
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $location.path('/');
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // function to change project from the header
      $scope.changeFocusProject = function(mapProject) {
        $scope.focusProject = mapProject;
        console.debug('dashboardCtrl:  changing project to '
          + $scope.focusProject.name);

        // update and broadcast the new focus project
        localStorageService.add('focusProject', $scope.focusProject);
        $rootScope.$broadcast(
          'localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

        // update the user preferences
        $scope.preferences.lastMapProjectId = $scope.focusProject.id;
        localStorageService.add('preferences', $scope.preferences);
        $rootScope.$broadcast(
          'localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

        // get the role for this user and project
        console.debug('Retrieving role for ' + $scope.focusProject.name + ', '
          + $scope.currentUser.userName);
        $http(
          {
            url : root_mapping + 'userRole/user/id/'
              + $scope.currentUser.userName + '/project/id/'
              + $scope.focusProject.id,
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          console.debug('Role set to: ' + data);
          $scope.currentRole = data.substring(1, data.length - 1);
          if ($scope.currentRole.toLowerCase() == 'specialist') {
            $scope.currentRole = 'Specialist';
          } else if ($scope.currentRole.toLowerCase() == 'lead') {
            $scope.currentRole = 'Lead';
          } else if ($scope.currentRole.toLowerCase() == 'administrator') {
            $scope.currentRole = 'Administrator';
          } else {
            $scope.currentRole = 'Viewer';
          }
          localStorageService.add('currentRole', $scope.currentRole);
        }).then(function() {
          setTimeout(function() {
            location.reload();
          }, 100);
        });

      };

      $scope.$on('localStorageModule.notification.setMapProjects', function(
        event, parameters) {
        $scope.mapProjects = parameters.mapProjects;
      });

      $scope.goToHelp = function() {
        var path;
        if ($scope.page != 'mainDashboard') {
          path = 'help/' + $scope.page + 'Help.html';
        } else {
          path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
        }
        console.debug('go to help page ' + path);
        // redirect page
        $location.path(path);
      };
    });

mapProjectAppDashboards
  .controller(
    'MapRecordDashboardCtrl',
    function($scope, $rootScope, $http, $routeParams, $location,
      localStorageService) {

      $scope.model = null;

      // On initialization, reset all values to null -- used to
      // ensure watch
      // functions work correctly
      $scope.mapProjects = null;
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.preferences = null;
      $scope.focusProject = null;
      $rootScope.globalError = '';

      // Used for Reload/Refresh purposes -- after setting to
      // null, get the
      // locally stored values
      $scope.mapProjects = localStorageService.get('mapProjects');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.preferences = localStorageService.get('preferences');
      $scope.focusProject = localStorageService.get('focusProject');

      $scope.page = 'mapRecordDashboard';

      // initialize the default model
      setDefaultModel();

      // on successful user retrieval, construct the dashboard
      $scope.$watch([ 'preferences' ], function() {

        console.debug('MainDashboard: Preferences loaded, models = ',
          $scope.preferences.dashboardModels);

        if ($scope.page in $scope.preferences.dashboardModels) {
          console.debug('  user defined model found');
          $scope.model = JSON
            .parse($scope.preferences.dashboardModels[$scope.page]);

        } else {
          console.debug('  using default model (no user-defined model)');
          $scope.model = $scope.defaultModel;
        }

        // calculate the number of widgets
        // available (used to display edit icon)
        var widgetCt = 0;

        // if model has rows defined
        if ($scope.model != null && $scope.model.hasOwnProperty('rows')) {

          console.debug('model has rows');

          // cycle over rows
          for (var i = 0; i < $scope.model.rows.length; i++) {

            // if row has columns defined
            if ($scope.model.rows[i].hasOwnProperty('columns')) {

              console.debug('row has columns');

              // cycle over columns
              for (var j = 0; j < $scope.model.rows[i].columns.length; j++) {

                // if column has widgets
                // defined
                if ($scope.model.rows[i].columns[j].hasOwnProperty('widgets')) {

                  console.debug('column has widgets');

                  // add the number of
                  // widgets to count
                  widgetCt += $scope.model.rows[i].columns[j].widgets.length;
                }
              }
            }
          }
        }
        $scope.model.widgetCount = widgetCt;
        console.debug('Widgets found: ', $scope.model.widgetCount);

      });

      // function to reset to the default model (called from page)
      $scope.resetModel = function() {
        console.debug('Main dashboard:   Reset to default model');

        console.debug('user defined models: ',
          $scope.preferences.dashboardModels);

        // splice working oddly here, clunky workaround
        var models = {};
        for ( var key in $scope.preferences.dashboardModels) {
          if (key != $scope.page)
            models[key] = $scope.preferences.dashboardModels[key];
        }

        $scope.preferences.dashboardModels = models;

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
            localStorageService.add('preferences', $scope.preferences);
            location.reload();
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

        console.debug('Revised preferences: ',
          $scope.preferences.dashboardModels);
      };

      function setDefaultModel() {
        $scope.page = 'editDashboard';
        console.debug('Setting record dashboard model');
        console.debug($scope.model);

        $scope.defaultModel = {
          structure : '6-6',
          rows : [ {
            columns : [
              {
                class : 'col-md-6',
                widgets : [ {
                  type : 'mapRecord',
                  config : {
                    recordId : $routeParams.recordId
                  },
                  title : 'Map Record'
                }, {
                  type : 'recordSummary',
                  config : {
                    record : $scope.record
                  },
                  title : 'Record Summary'
                } ]
              },
              {
                class : 'col-md-6',
                widgets : [
                  {
                    type : 'mapEntry',
                    config : {
                      entry : $scope.entry
                    },
                    title : 'Map Entry'
                  },
                  {
                    type : 'terminologyBrowser',
                    config : {
                      terminology : $scope.focusProject.destinationTerminology,
                      terminologyVersion : $scope.focusProject.destinationTerminologyVersion
                    },
                    title : $scope.focusProject.destinationTerminology
                      + ' Terminology Browser'

                  } ]
              } // end second column
            ]
          // end columns
          } ]
        // end rows
        };

      }
      ;

      $scope
        .$on(
          'adfDashboardChanged',
          function(event, name, model) {
            console.debug('Dashboard change detected by mapRecordDashboard',
              model);
            localStorageService.set(name, model);

            $scope.preferences.dashboardModels[$scope.page] = JSON
              .stringify($scope.model);
            localStorageService.add('preferences', $scope.preferences);

            console.debug('Models', $scope.preferences.dashboardModels);

            // update the user preferences
            $http({
              url : root_mapping + 'userPreferences/update',
              dataType : 'json',
              data : $scope.preferences,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            })
              .success(function(data) {
                // do nothing

              })
              .error(
                function(data) {
                  if (response.indexOf('HTTP Status 401') != -1) {
                    $rootScope.globalError = 'Authorization failed.  Please log in again.';
                    $location.path('/');
                  }
                });

          });

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(
        event, name) {
        console.debug('MainDashboardCtrl:  Detected change in map projects');

        $scope.mapProjects = localStorageService.get('mapProjects');
      });

      $scope.logout = function() {

        $rootScope.glassPane++;
        $http(
          {
            url : root_security + 'logout/user/id/'
              + $scope.currentUser.userName,
            method : 'POST',
            headers : {
              'Content-Type' : 'text/plain'
            // save userToken from authentication
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $location.path('/');
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $location.path('/');
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // function to change project from the header
      $scope.changeFocusProject = function(mapProject) {
        $scope.focusProject = mapProject;
        console.debug('changing project to ' + $scope.focusProject.name);

        // update and broadcast the new focus project
        localStorageService.add('focusProject', $scope.focusProject);
        $rootScope.$broadcast(
          'localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

        // update the user preferences
        $scope.preferences.lastMapProjectId = $scope.focusProject.id;
        localStorageService.add('preferences', $scope.preferences);
        $rootScope.$broadcast(
          'localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

      };

      $scope.$on('localStorageModule.notification.setMapProjects', function(
        event, parameters) {
        $scope.mapProjects = parameters.mapProjects;
      });

      $scope.goToHelp = function() {
        var path;
        if ($scope.page != 'mainDashboard') {
          path = 'help/' + $scope.page + 'Help.html';
        } else {
          path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
        }
        console.debug('go to help page ' + path);
        // redirect page
        $location.path(path);
      };
    });

mapProjectAppDashboards
  .controller(
    'ProjectDetailsDashboardCtrl',
    function($rootScope, $scope, $http, $location, localStorageService) {

      // On initialization, reset all values to null -- used to
      // ensure watch
      // functions work correctly
      $scope.mapProjects = null;
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.preferences = null;
      $scope.focusProject = null;
      $rootScope.globalError = '';

      // Used for Reload/Refresh purposes -- after setting to
      // null, get the
      // locally stored values
      $scope.mapProjects = localStorageService.get('mapProjects');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.preferences = localStorageService.get('preferences');
      $scope.focusProject = localStorageService.get('focusProject');

      $scope.page = 'projectDetailsDashboard';

      console.debug('in projectDetailsDashboardCtrl');

      // watch for preferences change
      $scope.$on('localStorageModule.notification.setUserPreferences',
        function(event, parameters) {
          console.debug('dashboardCtrl:  Detected change in preferences');
          if (parameters.userPreferences != null
            && parameters.userPreferences != undefined) {
            $http({
              url : root_mapping + 'userPreferences/update',
              dataType : 'json',
              data : parameters.userPreferences,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });
          }
        });

      // must instantiate a default dashboard on call
      setModel();

      // on successful user retrieval, construct the dashboard
      $scope.$watch('currentRole', function() {
        setModel();
      });

      function setModel() {

        console.debug('Setting the dashboard based on role: '
          + $scope.currentRole);

        $scope.page = 'projectDetailsDashboard';
        $scope.model = {

          structure : '12/6-6/12',
          rows : [ {
            columns : [ {
              class : 'col-md-12',
              widgets : [ {
                type : 'projectDetails',
                config : {},
                title : 'Project Details'
              } ]
            } ]
          } ]
        };

      }

      $scope.$on('adfDashboardChanged', function(event, name, model) {
        console.debug('adfDashboardChanged in DashBoardCtrl');
        console.debug(event);
        console.debug(name);
        console.debug(model);
        $scope.model = model;
      });

      $scope.logout = function() {

        $rootScope.glassPane++;
        $http(
          {
            url : root_security + 'logout/user/id/'
              + $scope.currentUser.userName,
            method : 'POST',
            headers : {
              'Content-Type' : 'text/plain'
            // save userToken from authentication
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $location.path('/');
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $location.path('/');
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // function to change project from the header
      $scope.changeFocusProject = function(mapProject) {
        $scope.focusProject = mapProject;
        console.debug('changing project to ' + $scope.focusProject.name);

        // update and broadcast the new focus project
        localStorageService.add('focusProject', $scope.focusProject);
        $rootScope.$broadcast(
          'localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

        // update the user preferences
        $scope.preferences.lastMapProjectId = $scope.focusProject.id;
        localStorageService.add('preferences', $scope.preferences);
        $rootScope.$broadcast(
          'localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : $scope.preferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        })
          .success(function(data) {
          })
          .error(
            function(data) {
              if (response.indexOf('HTTP Status 401') != -1) {
                $rootScope.globalError = 'Authorization failed.  Please log in again.';
                $location.path('/');
              }
            });

      };

      $scope.$on('localStorageModule.notification.setMapProjects', function(
        event, parameters) {
        $scope.mapProjects = parameters.mapProjects;
      });

      $scope.goToHelp = function() {
        var path;
        if ($scope.page != 'mainDashboard') {
          path = 'help/' + $scope.page + 'Help.html';
        } else {
          path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
        }
        console.debug('go to help page ' + path);
        // redirect page
        $location.path(path);
      };
    });

mapProjectAppDashboards.controller('ProjectRecordsDashboardCtrl',
  function($rootScope, $scope, $http, $location, localStorageService) {

    // On initialization, reset all values to null -- used to ensure watch
    // functions work correctly
    $scope.mapProjects = null;
    $scope.currentUser = null;
    $scope.currentRole = null;
    $scope.preferences = null;
    $scope.focusProject = null;
    $rootScope.globalError = '';

    // Used for Reload/Refresh purposes -- after setting to null, get the
    // locally stored values
    $scope.mapProjects = localStorageService.get('mapProjects');
    $scope.currentUser = localStorageService.get('currentUser');
    $scope.currentRole = localStorageService.get('currentRole');
    $scope.preferences = localStorageService.get('preferences');
    $scope.focusProject = localStorageService.get('focusProject');

    $scope.page = 'projectRecordsDashboard';

    console.debug('in projectRecordsDashboardCtrl');

    // watch for preferences change
    $scope.$on('localStorageModule.notification.setUserPreferences', function(
      event, parameters) {
      console.debug('dashboardCtrl:  Detected change in preferences');
      if (parameters.userPreferences != null
        && parameters.userPreferences != undefined) {
        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : parameters.userPreferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
        }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
        });
      }
    });

    // must instantiate a default dashboard on call
    setModel();

    // on successful user retrieval, construct the dashboard
    $scope.$watch('currentRole', function() {
      setModel();
    });

    function setModel() {

      console.debug('Setting the dashboard based on role: '
        + $scope.currentRole);

      $scope.page = 'projectRecordsDashboard';
      $scope.model = {

        structure : '12/6-6/12',
        rows : [ {
          columns : [ {
            class : 'col-md-12',
            widgets : [ {
              type : 'projectRecords',
              config : {},
              title : 'Project Records'
            } ]
          } ]
        } ]
      };

    }

    $scope.$on('adfDashboardChanged', function(event, name, model) {
      console.debug('adfDashboardChanged in DashBoardCtrl');
      console.debug(event);
      console.debug(name);
      console.debug(model);
      $scope.model = model;
    });

    $scope.logout = function() {

      $rootScope.glassPane++;
      $http({
        url : root_security + 'logout/user/id/' + $scope.currentUser.userName,
        method : 'POST',
        headers : {
          'Content-Type' : 'text/plain'
        // save userToken from authentication
        }
      }).success(function(data) {
        $rootScope.glassPane--;
        $location.path('/');
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $location.path('/');
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    $scope.$on('localStorageModule.notification.setMapProjects', function(
      event, parameters) {
      $scope.mapProjects = parameters.mapProjects;
    });

    // function to change project from the header
    $scope.changeFocusProject = function(mapProject) {
      $scope.focusProject = mapProject;
      console.debug('changing project to ' + $scope.focusProject.name);

      // update and broadcast the new focus project
      localStorageService.add('focusProject', $scope.focusProject);
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject',
        {
          key : 'focusProject',
          focusProject : $scope.focusProject
        });

      // update the user preferences
      $scope.preferences.lastMapProjectId = $scope.focusProject.id;
      localStorageService.add('preferences', $scope.preferences);
      $rootScope.$broadcast(
        'localStorageModule.notification.setUserPreferences', {
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
      console.debug('go to help page ' + path);
      // redirect page
      $location.path(path);
    };
  });

mapProjectAppDashboards.controller('RecordConceptDashboardCtrl',
  function($rootScope, $scope, $http, $location, localStorageService) {

    // On initialization, reset all values to null -- used to ensure watch
    // functions work correctly
    $scope.mapProjects = null;
    $scope.currentUser = null;
    $scope.currentRole = null;
    $scope.preferences = null;
    $scope.focusProject = null;
    $rootScope.globalError = '';

    // Used for Reload/Refresh purposes -- after setting to null, get the
    // locally stored values
    $scope.mapProjects = localStorageService.get('mapProjects');
    $scope.currentUser = localStorageService.get('currentUser');
    $scope.currentRole = localStorageService.get('currentRole');
    $scope.preferences = localStorageService.get('preferences');
    $scope.focusProject = localStorageService.get('focusProject');

    $scope.page = 'recordConceptDashboard';

    console.debug('in recordConceptDashboardCtrl');

    $scope.$on('localStorageModule.notification.setMapProjects', function(
      event, parameters) {
      $scope.mapProjects = parameters.mapProjects;
    });

    // watch for preferences change
    $scope.$on('localStorageModule.notification.setUserPreferences', function(
      event, parameters) {
      console.debug('dashboardCtrl:  Detected change in preferences');
      if (parameters.userPreferences != null
        && parameters.userPreferences != undefined) {
        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : parameters.userPreferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
        }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
        });
      }
    });

    // must instantiate a default dashboard on call
    setModel();

    // on successful user retrieval, construct the dashboard
    $scope.$watch('currentRole', function() {
      setModel();
    });

    function setModel() {

      console.debug('Setting the dashboard based on role: '
        + $scope.currentRole);

      $scope.model = {

        structure : '12/6-6/12',
        rows : [ {
          columns : [ {
            class : 'col-md-12',
            widgets : [ {
              type : 'recordConcept',
              config : {},
              title : 'Record Concept'
            } ]
          } ]
        } ]
      };

    }

    $scope.$on('adfDashboardChanged', function(event, name, model) {
      console.debug('adfDashboardChanged in DashBoardCtrl');
      console.debug(event);
      console.debug(name);
      console.debug(model);
      $scope.model = model;
    });

    $scope.logout = function() {

      $rootScope.glassPane++;
      $http({
        url : root_security + 'logout/user/id/' + $scope.currentUser.userName,
        method : 'POST',
        headers : {
          'Content-Type' : 'text/plain'
        // save userToken from authentication
        }
      }).success(function(data) {
        $rootScope.glassPane--;
        $location.path('/');
      }).error(function(data, status, headers, config) {
        $rootScope.glassPane--;
        $location.path('/');
        $rootScope.handleHttpError(data, status, headers, config);
      });
    };

    // function to change project from the header
    $scope.changeFocusProject = function(mapProject) {
      $scope.focusProject = mapProject;
      console.debug('changing project to ' + $scope.focusProject.name);

      // update and broadcast the new focus project
      localStorageService.add('focusProject', $scope.focusProject);
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject',
        {
          key : 'focusProject',
          focusProject : $scope.focusProject
        });

      // update the user preferences
      $scope.preferences.lastMapProjectId = $scope.focusProject.id;
      localStorageService.add('preferences', $scope.preferences);
      $rootScope.$broadcast(
        'localStorageModule.notification.setUserPreferences', {
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
      console.debug('go to help page ' + path);
      // redirect page
      $location.path(path);
    };
  });

mapProjectAppDashboards.controller('IndexViewerDashboardCtrl',
  function($rootScope, $scope, $http, $location, localStorageService) {

    // On initialization, reset all values to null -- used to ensure watch
    // functions work correctly
    $scope.mapProjects = null;
    $scope.currentUser = null;
    $scope.currentRole = null;
    $scope.preferences = null;
    $scope.focusProject = null;
    $rootScope.globalError = '';

    // Used for Reload/Refresh purposes -- after setting to null, get the
    // locally stored values
    $scope.mapProjects = localStorageService.get('mapProjects');
    $scope.currentUser = localStorageService.get('currentUser');
    $scope.currentRole = localStorageService.get('currentRole');
    $scope.preferences = localStorageService.get('preferences');
    $scope.focusProject = localStorageService.get('focusProject');

    $scope.page = 'indexViewerDashboard';

    console.debug('in indexViewerDashboardCtrl');

    $scope.$on('localStorageModule.notification.setMapProjects', function(
      event, parameters) {
      $scope.mapProjects = parameters.mapProjects;
    });

    // watch for preferences change
    $scope.$on('localStorageModule.notification.setUserPreferences', function(
      event, parameters) {
      console.debug('dashboardCtrl:  Detected change in preferences');
      if (parameters.userPreferences != null
        && parameters.userPreferences != undefined) {
        $http({
          url : root_mapping + 'userPreferences/update',
          dataType : 'json',
          data : parameters.userPreferences,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
        }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
        });
      }
    });

    // must instantiate a default dashboard on call
    setModel();

    // on successful user retrieval, construct the dashboard
    $scope.$watch('currentRole', function() {
      setModel();
    });

    function setModel() {

      console.debug('Setting the dashboard based on role: '
        + $scope.currentRole);

      $scope.model = {

        structure : '12/6-6/12',
        rows : [ {
          columns : [ {
            class : 'col-md-12',
            widgets : [ {
              type : 'indexViewer',
              title : 'Index Viewer'
            } ]
          } ]
        } ]
      };

    }

    $scope.$on('adfDashboardChanged', function(event, name, model) {
      console.debug('adfDashboardChanged in DashBoardCtrl');
      console.debug(event);
      console.debug(name);
      console.debug(model);
      $scope.model = model;
    });

    // function to change project from the header
    $scope.changeFocusProject = function(mapProject) {
      $scope.focusProject = mapProject;
      console.debug('changing project to ' + $scope.focusProject.name);

      // update and broadcast the new focus project
      localStorageService.add('focusProject', $scope.focusProject);
      $rootScope.$broadcast('localStorageModule.notification.setFocusProject',
        {
          key : 'focusProject',
          focusProject : $scope.focusProject
        });

      // update the user preferences
      $scope.preferences.lastMapProjectId = $scope.focusProject.id;
      localStorageService.add('preferences', $scope.preferences);
      $rootScope.$broadcast(
        'localStorageModule.notification.setUserPreferences', {
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
      console.debug('go to help page ' + path);
      // redirect page
      $location.path(path);
    };
  });
