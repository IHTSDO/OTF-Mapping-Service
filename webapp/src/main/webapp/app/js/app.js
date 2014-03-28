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
                                                     'mapProjectApp.widgets.workAvailable',
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
                        .controller('dashboardCtrl', function ($rootScope, $scope, localStorageService) {
                          var name = 'default';
                          var model = localStorageService.get(name);
                          
                          var currentUser = localStorageService.get('currentUser');
                          var currentRole = localStorageService.get('currentRole');
                          
                          if (!model && currentRole.value >= 3) { // lead or higher privledge
                            // set default model for demo purposes
                            model = {
                                structure: "4-8",                          
	                            rows: [{
	                                columns: [
	                                {
	                                  class: 'col-md-4',
	                                  widgets: [{
	                                      type: "mapProject",
	                                      config: {},
	                                      title: "Map Project"
	                                  }]
	                                }, {
	                                  class: 'col-md-8',
	                                  widgets: [{
	                                      type: "workAvailable",
	                                      config: {},
	                                      title: "Available Work"
	                                  }]
	                                }]
	                              }, // end row 1
	                              
	                              { // begin row 2
                            	   columns : [
	                               {
	                                  class: 'col-md-8',
	                                  widgets: [{
	                                      type: "metadataList",
	                                      config: {
	                                          terminology: "SNOMEDCT"
	                                      },
	                                      title: "Metadata"
	                                  }]
	                               }]
	                              } // end row 2
                               ] // end rows
                            } // end model
                          } else if (!model) { // viewer or specialist
                              // set default model for demo purposes
                              model = {
                                structure: "4-8",                          
                              rows: [{
                                  columns: [{
                                    class: 'col-md-4',
                                    widgets: [{
                                        type: "mapProjectList",
                                        config: {},
                                        title: "Map Projects"
                                    }]
                                  }]
                                }]
                              };
                            }
                          $scope.name = name;
                          $scope.model = model;

                          $scope.$on('adfDashboardChanged', function (event, name, model) {
                            localStorageService.set(name, model);
                          });
                        });




mapProjectApp.config(['$routeProvider',
   function($routeProvider) {
	
      //////////////////////////////
	  // DASHBOARDS
	  //////////////////////////////
	
	  $routeProvider.when('/sorttest', {
		  controller: 'SortTestCtrl',
		  templateUrl: 'sorttest.html'
	  });
	  
	  $routeProvider.when('/specialist/dash', {
		  templateUrl: 'partials/project-list.html'
	  });
	  
	  $routeProvider.when('/lead/dash', {
		  templateUrl: 'partials/project-list.html'
	  });
	  
	  $routeProvider.when('/admin/dash', {
		  templateUrl: 'partials/project-list.html'
	  });
	
      //////////////////////////////
	  // MAPPING SERVICES
	  //////////////////////////////
	  
	  $routeProvider.when('/project/projects', {
		  templateUrl: 'partials/project-list.html'//, 
		  //controller: 'MapProjectListCtrl'
	  });
	  
	  $routeProvider.when('/record/projectId/:projectId', {
		  templateUrl: 'partials/project-records.html',
	      controller: 'MapProjectRecordCtrl'
	  });

	  $routeProvider.when('/project/id/:projectId', {
  		  templateUrl: 'partials/project-detail.html', 
  		  controller: 'MapProjectDetailCtrl'
  	  });
	  
	  $routeProvider.when('/record/conceptId/:conceptId', {
			templateUrl: 'partials/record-concept.html',
			controller: 'RecordConceptListCtrl'
	  });
	  
	  $routeProvider.when('/record/conceptId/:conceptId/create', {
		  templateUrl: 'partials/record-create.html',
		  controller: 'RecordCreateCtrl'
	  });
	  
	  $routeProvider.when('/record/recordId/:recordId', {
		  templateUrl: 'partials/record-dashboard.html',
		  controller: 'MapRecordDashboardCtrl'
	  });
		
	  
	  //////////////////////////////
	  // CONTENT SERVICES
	  //////////////////////////////
	  
	  
	  
	  
	  //////////////////////////////
	  // QUERY SERVICES
	  //////////////////////////////

	  //////////////////////////////
	  // METADATA SERVICES
	  //////////////////////////////

	  
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
