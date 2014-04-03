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
                          
                          if (!model) { // lead or higher privledge
                            // set default model for demo purposes
                        	model = {
                        			  
                        	  structure: "12/6-6/12",
                        	  rows: [{
                                  columns: [{
                                    class: 'col-md-12',
                                    widgets: [{
	                                      type: "mapProject",
	                                      config: {},
	                                      title: "Map Project"
	                                  }]
                                  }]
                                }, {
                                  columns: [{
                                    class: 'col-md-6',
                                    widgets: [{
	                                      type: "workAvailable",
	                                      config: {},
	                                      title: "Available Work"
	                                  }]
                                  }, {
                                    class: 'col-md-6',
                                    widgets: [{
	                                      type: "assignedList",
	                                      config: {},
	                                      title: "Assigned to Me"
	                                  }]
                                  }]
                                }
                                
                                , {
                                  columns: [{
                                    class: 'col-md-12',
                                    widgets: [{
	                                      type: "editedList",
	                                      title: "Recently Edited"
	                                  }]
                                  }]
                                }, {
                                    columns: [{
                                        class: 'col-md-12',
                                        widgets: [{
	                                      type: "metadataList",
	                                      config: {
	                                          terminology: "SNOMEDCT"
	                                      },
	                                      title: "Metadata"
	                                  }]
                                  }]
                                }]
                        	  
	
                            };
                          } else if (!model) { // viewer or specialist
                              // set default model for demo purposes
                              model = {
                                structure: "6-6",                          
                              rows: [{
                            	  columns: [{
                                      class: 'col-md-6',
                                      widgets: [{
  	                                      type: "terminologyBrowser",
  	                                      config: {
  	                                          terminology: "ICD10",
  	                                          terminologyVersion: "2010"                	 
  	                                      },
  	                                      title: "ICD10 Browser"
  	                                  }]
                                    }]
                                  }]
                                }
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
	  
	  $routeProvider.when('/record/recordId/:recordId', {
		  templateUrl: 'partials/record-dashboard.html',
		  controller: 'MapRecordDashboardCtrl'
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
