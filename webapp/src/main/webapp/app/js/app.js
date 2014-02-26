'use strict';


// Declare app level module
var mapProjectApp = angular.module('mapProjectApp', [
                                                     'ngRoute',
                                                     'mapProjectAppControllers'
                                                   ]);



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
		  templateUrl: 'partials/specialist-dash.html', 
		  controller: 'SpecialistDashCtrl'
	  });
	  
	  $routeProvider.when('/lead/dash', {
		  templateUrl: 'partials/lead-dash.html', 
		  controller: 'LeadDashCtrl'
	  });
	  
	  $routeProvider.when('/admin/dash', {
		  templateUrl: 'partials/admin-dash.html', 
		  controller: 'AdminDashCtrl'
	  });
	
      //////////////////////////////
	  // MAPPING SERVICES
	  //////////////////////////////
	  
	  $routeProvider.when('/project/projects', {
		  templateUrl: 'partials/project-list.html', 
		  controller: 'MapProjectListCtrl'
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
		  templateUrl: 'partials/record-detail.html',
		  controller: 'MapRecordDetailCtrl'
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
