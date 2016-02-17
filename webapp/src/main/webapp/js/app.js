'use strict';

// Declare app level module
var mapProjectApp = angular.module(
  'mapProjectApp',
  [ 'ngRoute', 'mapProjectAppControllers', 'adf', 'mapProjectApp.widgets.metadataList',
    'mapProjectApp.widgets.mapProject', 'mapProjectApp.widgets.mapRecord',
    'mapProjectApp.widgets.mapEntry', 'mapProjectApp.widgets.assignedList',
    'mapProjectApp.widgets.editedList', 'mapProjectApp.widgets.workAvailable',
    'mapProjectApp.widgets.terminologyBrowser', 'mapProjectApp.widgets.compareRecords',
    'mapProjectApp.widgets.projectDetails', 'mapProjectApp.widgets.projectRecords',
    'mapProjectApp.widgets.recordConcept', 'mapProjectApp.widgets.recordSummary',
    'mapProjectApp.widgets.recordAdmin', 'mapProjectApp.widgets.feedback',
    'mapProjectApp.widgets.feedbackConversation', 'mapProjectApp.widgets.applicationAdmin',
    'mapProjectApp.widgets.report', 'mapProjectApp.widgets.qaCheck',
    'mapProjectApp.widgets.indexViewer', 'LocalStorageModule', 'ngCookies', 'ui.tinymce',
    'angularjs-dropdown-multiselect', 'angularFileUpload', 'ui.tree' ]).value('prefix', '').config(
  function(dashboardProvider) {

    dashboardProvider.structure('6-6', {
      rows : [ {
        columns : [ {
          styleClass : 'col-md-6'
        }, {
          styleClass : 'col-md-6'
        } ]
      } ]
    }).structure('4-8', {
      rows : [ {
        columns : [ {
          styleClass : 'col-md-4',
          widgets : []
        }, {
          styleClass : 'col-md-8',
          widgets : []
        } ]
      } ]
    }).structure('12/4-4-4', {
      rows : [ {
        columns : [ {
          styleClass : 'col-md-12'
        } ]
      }, {
        columns : [ {
          styleClass : 'col-md-4'
        }, {
          styleClass : 'col-md-4'
        }, {
          styleClass : 'col-md-4'
        } ]
      } ]
    }).structure('12/6-6', {
      rows : [ {
        columns : [ {
          styleClass : 'col-md-12'
        } ]
      }, {
        columns : [ {
          styleClass : 'col-md-6'
        }, {
          styleClass : 'col-md-6'
        } ]
      } ]
    }).structure('12/6-6/12', {
      rows : [ {
        columns : [ {
          styleClass : 'col-md-12'
        } ]
      }, {
        columns : [ {
          styleClass : 'col-md-6'
        }, {
          styleClass : 'col-md-6'
        } ]
      }, {
        columns : [ {
          styleClass : 'col-md-12'
        } ]
      } ]
    });

  });

mapProjectApp.config([ '$routeProvider', function($routeProvider) {

  // ////////////////////////////
  // DASHBOARDS
  // ////////////////////////////

  $routeProvider.when('/:role/dash', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'dashboardCtrl'
  });

  // ////////////////////////////
  // MAPPING SERVICES
  // ////////////////////////////

  $routeProvider.when('/project/records', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'ProjectRecordsDashboardCtrl'
  });

  $routeProvider.when('/project/details', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'ProjectDetailsDashboardCtrl'
  });

  $routeProvider.when('/record/conceptId/:conceptId', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'RecordConceptDashboardCtrl'
  });

  $routeProvider.when('/record/conceptId/:conceptId/autologin', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'LoginCtrl'
  });

  $routeProvider.when('/conversation/recordId/:recordId', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'FeedbackConversationsDashboardCtrl'
  });

  $routeProvider.when('/record/recordId/:recordId', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'MapRecordDashboardCtrl'
  });

  $routeProvider.when('/record/conflicts/:recordId', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'ResolveConflictsDashboardCtrl'
  });

  $routeProvider.when('/record/review/:recordId', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'ResolveConflictsDashboardCtrl'
  });

  $routeProvider.when('/index/viewer', {
    templateUrl : 'partials/otf-dashboard.html',
    controller : 'IndexViewerDashboardCtrl'
  });

  // ////////////////////////////
  // HELP PAGES
  // ////////////////////////////

  $routeProvider.when('/help/:type', {
    templateUrl : function(params) {
      return 'partials/doc/' + params.type;
    }
  });

  // /////////////////////////////
  // HOME and ERROR ROUTES
  // /////////////////////////////

  $routeProvider.when('/', {
    templateUrl : 'partials/login.html',
    controller : 'LoginCtrl'
  });

  $routeProvider.otherwise({
    redirectTo : 'partials/error.html'
  });
} ]);

mapProjectApp.directive('indexViewerPage', function($http, $templateCache, $compile, utilService) {

  return {
    scope : false,
    restrict : 'AE',
    link : function(scope, elem, attrs) {
      
      console.debug('scope', scope);
      console.debug('tUrl', scope.tUrl);

      function init() {
        
        console.debug('Index Viewer init: ' + attrs.template);

        // get the html template from attributes
        var templatePath = attrs.template;

        if (!templatePath) {
          utilService.handleError("Index Viewer page template not defined");
        }

        // get the template from the cache
        $http.get(templatePath, {
          cache : $templateCache
        }).success(function(response) {
          // append the template to the element
          var contents = elem.html(response).contents();

          // compile the template
          $compile(contents)(scope);

          console.debug('compiled');
        }).error(
          function() {
            utilService.handleError("Index Viewer page template not found in cache: "
              + templatePath);
          });
      }

      scope.$watch(attrs.template, function() {
        console.debug('template', attrs.template);
        init();
      }, true);

    }
  };
});
