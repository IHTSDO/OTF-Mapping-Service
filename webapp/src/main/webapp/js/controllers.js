'use strict';

var mapProjectAppControllers = angular.module('mapProjectAppControllers', [ 'ui.bootstrap',
  'mapProjectAppDirectives', 'mapProjectAppDashboards' ]);

var root_url = '/mapping-rest/';
var root_mapping = root_url + 'mapping/';
var root_content = root_url + 'content/';
var root_metadata = root_url + 'metadata/';
var root_workflow = root_url + 'workflow/';
var root_security = root_url + 'security/';
var root_reporting = root_url + 'reporting/';

mapProjectAppControllers.config(function(localStorageServiceProvider) {

  localStorageServiceProvider.setPrefix('mapping').setStorageType('sessionStorage').setNotify(true,
    true)
});

mapProjectAppControllers
  .run(function($rootScope, $window, $http, $location, utilService) {

    // global function to handle any type of error. Currently only
    // specifically implemented for authorization failures.
    $rootScope.handleHttpError = function(data, status, headers, config) {
      // $rootScope.globalError = data.replace(/"/g, '');
      utilService.handleError(data.replace(/"/g, ''));
      if (status == '401') {
        $location.path('/');
      }
      $window.scrollTo(0, 0);
    };

    // global function to handle an error that returns user to dashboard
    // currently used for improper viewing of records in editing
    $rootScope.handleReturnToDashboardError = function(errorString, currentRole) {
      $rootScope.globalError = errorString;
      $location.path('/' + currentRole.toLowerCase() + '/dash');
      $window.scrollTo(0, 0);

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

          // request to prevent reload (currently indexViewer.js)
          else if ($rootScope.preventSingleReload == true) {

            // prevent the default and set the flag to false
            event.preventDefault();
            $rootScope.preventSingleReload = false;
          }
        }

      );

  });
