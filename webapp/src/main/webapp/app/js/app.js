'use strict';


// Declare app level module
var mapProjectApp = angular.module('mapProjectApp', ['ngRoute',
                                                     'mapProjectAppControllers',                                                
                                                     'adf',  
                                                     'mapProjectApp.widgets.mapProjectList', 
                                                     'mapProjectApp.widgets.metadataList',
                                                     'mapProjectApp.widgets.mapProject',
                                                     'mapProjectApp.widgets.mapRecord',
                                                     'mapProjectApp.widgets.mapEntry',  
                                                     'mapProjectApp.widgets.assignedList', 
                                                     'mapProjectApp.widgets.editedList',  
                                                     'mapProjectApp.widgets.workAvailable',
                                                     'mapProjectApp.widgets.terminologyBrowser',
                                                     'mapProjectApp.widgets.compareRecords',
                                                     'LocalStorageModule',
                                                     'ngCookies'/*,
                                                     'textAngular'*/
                        ])
                        .value('prefix', '')
                        .config(function (dashboardProvider) {
                          dashboardProvider
                            .structure('6-6', {
                              rows: [{
                                columns: [{
                                  class: 'col-md-6'
                                }, {
                                  class: 'col-md-6'
                                }]
                              }]
                            })
                            .structure('4-8', {
                              rows: [{
                                columns: [{
                                  class: 'col-md-4',
                                  widgets: []
                                }, {
                                  class: 'col-md-8',
                                  widgets: []
                                }, {
                                  class: 'col-md-4',
                                  widgets: []
                                }]
                                
                              }]
                            })
                            .structure('12/4-4-4', {
                              rows: [{
                                columns: [{
                                  class: 'col-md-12'
                                }]
                              }, {
                                columns: [{
                                  class: 'col-md-4'
                                }, {
                                  class: 'col-md-4'
                                }, {
                                  class: 'col-md-4'
                                }]
                              }]
                            })
                            .structure('12/6-6/12', {
                              rows: [{
                                columns: [{
                                  class: 'col-md-12'
                                }]
                              }, {
                                columns: [{
                                  class: 'col-md-6'
                                }, {
                                  class: 'col-md-6'
                                }]
                              }, {
                                columns: [{
                                  class: 'col-md-12'
                                }]
                              }]
                            });

                        });
                        

mapProjectApp.config(['$routeProvider',
   function($routeProvider) {
	
      //////////////////////////////
	  // DASHBOARDS
	  //////////////////////////////
	  
	  $routeProvider.when('/:role/dash', {
		  templateUrl: 'partials/dashboard.html',
		  controller: 'dashboardCtrl'
	  });
	
      //////////////////////////////
	  // MAPPING SERVICES
	  //////////////////////////////
	  
	  $routeProvider.when('/project/records', {
		  templateUrl: 'partials/project-records.html',
	      controller: 'MapProjectRecordCtrl'
	  });

	  $routeProvider.when('/project/details', {
  		  templateUrl: 'partials/project-detail.html', 
  		  controller: 'MapProjectDetailCtrl'
  	  });
	  
	  $routeProvider.when('/record/conceptId/:conceptId', {
			templateUrl: 'partials/record-concept.html',
			controller: 'RecordConceptListCtrl'
	  });
	  
	  $routeProvider.when('/record/recordId/:recordId', {
		  templateUrl: 'partials/record-dashboard.html',
		  controller: 'MapRecordDashboardCtrl'
	  });
	  
	  $routeProvider.when('/record/conflicts/:recordId', {
		  templateUrl: 'partials/record-dashboard.html',
		  controller: 'ResolveConflictsDashboardCtrl'
	  });
		

	  
      //////////////////////////////
	  // HELP PAGES
	  //////////////////////////////
	  
	  $routeProvider.when('/help/:type', {
		  templateUrl: function(params){ return 'partials/doc/' + params.type; }
	  });
	  
	  
	  ///////////////////////////////
	  // HOME and ERROR ROUTES
	  ///////////////////////////////
	  
	  $routeProvider.when('/', {
		  templateUrl: 'partials/login.html',
  		  controller: 'LoginCtrl'
	  });
	  
	  $routeProvider.otherwise({
	      redirectTo: 'partials/error.html'
	  });
   }]);
