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

                        })
                        




mapProjectApp.config(['$routeProvider',
   function($routeProvider) {
	
      //////////////////////////////
	  // DASHBOARDS
	  //////////////////////////////
	  
	  $routeProvider.when('/specialist/dash', {
		  templateUrl: 'partials/dashboard.html',
		  controller: 'dashboardCtrl'
	  });
	  
	  $routeProvider.when('/lead/dash', {
		  templateUrl: 'partials/dashboard.html',
			  controller: 'dashboardCtrl'
	  });
	  
	  $routeProvider.when('/admin/dash', {
		  templateUrl: 'partials/dashboard.html',
		  controller: 'dashboardCtrl'
	  });
	
      //////////////////////////////
	  // MAPPING SERVICES
	  //////////////////////////////
	  
	  $routeProvider.when('/project/projects', {
		  templateUrl: 'partials/dashboard.html', 
		  controller: 'dashboardCtrl'
	  });
	  
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
	  
	  /* TODO: something like this to avoid hardcoding all help files below
	   * $routeProvider.when('/help/:helpFile', {
	      templateUrl: 'partials/doc/' + $routeParams.helpFile
  	  });*/
	  
	  $routeProvider.when('/help/LeadDashboardHelp.html', {
		  templateUrl: 'partials/doc/LeadDashboardHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/AdministratorDashboardHelp.html', {
		  templateUrl: 'partials/doc/AdministratorDashboardHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/conceptHelp.html', {
		  templateUrl: 'partials/doc/conceptHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/editDashboardHelp.html', {
		  templateUrl: 'partials/doc/editDashboardHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/loginHelp.html', {
		  templateUrl: 'partials/doc/loginHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/projectHelp.html', {
		  templateUrl: 'partials/doc/projectHelp.html'
	  });	  
	  $routeProvider.when('/help/recordsHelp.html', {
		  templateUrl: 'partials/doc/recordsHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/resolveConflictsDashboardHelp.html', {
		  templateUrl: 'partials/doc/resolveConflictsDashboardHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/SpecialistDashboardHelp.html', {
		  templateUrl: 'partials/doc/SpecialistDashboardHelp.html'
	  });	  
	  
	  $routeProvider.when('/help/ViewerDashboardHelp.html', {
		  templateUrl: 'partials/doc/ViewerDashboardHelp.html'
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
