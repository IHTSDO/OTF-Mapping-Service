'use strict';


// Declare app level module
var mapProjectApp = angular.module('mapProjectApp', [
                                                     'ngRoute',
                                                     'mapProjectAppControllers'
                                                   ]);

mapProjectApp.config(['$routeProvider',
   function($routeProvider) {
	  
	  $routeProvider.when('/project/projects', {
		  templateUrl: 'partials/project-list.html', 
		  controller: 'MapProjectListCtrl'
	  });
	  
  	  $routeProvider.when('/lead/leads', {
  		  templateUrl: 'partials/lead-list.html',
  		  controller: 'MapLeadListCtrl'
	  });
  	  
	  $routeProvider.when('/specialist/specialists', {
		  templateUrl: 'partials/specialist-list.html',
		  controller: 'MapSpecialistListCtrl'
	  });
	  
	  $routeProvider.when('/record/records', {
		  templateUrl: 'partials/record-list.html',
	      controller: 'MapRecordListCtrl'
	  });
	  
	  $routeProvider.when('/advice/advices', {
		  templateUrl: 'partials/advice-list.html',
	      controller: 'MapAdviceListCtrl'
	  });
	  
	  // non functional at the moment
	  $routeProvider.when('/project/id/:projectId', {
  		  templateUrl: 'partials/project-detail.html', 
  		  controller: 'MapProjectDetailCtrl'
  	  });
	  
	// non functional at the moment
	  $routeProvider.when('/record/id/:recordId', {
  		  templateUrl: 'partials/record-detail.html', 
  		  controller: 'MapRecordDetailCtrl'
  	  });
	  
	  $routeProvider.otherwise({
	      redirectTo: '/'
	  });
   }]);
