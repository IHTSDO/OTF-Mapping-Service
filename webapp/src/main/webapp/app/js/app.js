'use strict';


// Declare app level module
var mapProjectApp = angular.module('mapProjectApp', [
                                                     'ngRoute',
                                                     'mapProjectAppControllers'
                                                   ]);

mapProjectApp.config(['$routeProvider',
   function($routeProvider) {
	
	
      //////////////////////////////
	  // MAPPING SERVICES
	  //////////////////////////////
	  
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
	  
	  $routeProvider.when('/record/id/:recordId', {
		  templateUrl: 'partials/record-detail.html',
	      controller: 'MapRecordDetailCtrl'
	  });
	  
	  $routeProvider.when('/advice/advices', {
		  templateUrl: 'partials/advice-list.html',
	      controller: 'MapAdviceListCtrl'
	  });
	  
	  $routeProvider.when('/project/id/:projectId', {
  		  templateUrl: 'partials/project-detail.html', 
  		  controller: 'MapProjectDetailCtrl'
  	  });
	  
	  $routeProvider.when('/record/id/:recordId', {
  		  templateUrl: 'partials/record-detail.html', 
  		  controller: 'MapRecordDetailCtrl'
  	  });
	  
	  $routeProvider.when('/principle/id/:principleId', {
			templateUrl: 'partials/principle-detail.html',
			controller: 'MapPrincipleDetailCtrl'
		});
		
	  
	  //////////////////////////////
	  // CONTENT SERVICES
	  //////////////////////////////
	  
	  $routeProvider.when('/concept/concepts', {
		  templateUrl: 'partials/concept-list.html',
		  controller: 'ConceptListCtrl'
	  });
	  
	  
	  $routeProvider.when('/concept/:terminology/:version/id/:conceptId', {
  		  templateUrl: 'partials/concept-detail.html', 
  		  controller: 'ConceptDetailCtrl'
  	  });
	  
	  
	  
	  //////////////////////////////
	  // QUERY SERVICES
	  //////////////////////////////
	  $routeProvider.when('/concept/query', {
  		  templateUrl: 'partials/query-partial.html', 
  		  controller: 'QueryCtrl'
  	  });
	  
	  //////////////////////////////
	  // METADATA SERVICES
	  //////////////////////////////
	  $routeProvider.when('/metadata', {
	  	templateUrl: 'partials/metadata-detail.html', 
		  controller: 'MetadataCtrl'
	  });
	  
	  ///////////////////////////////
	  // HOME and ERROR ROUTES
	  ///////////////////////////////
	  
	  $routeProvider.when('/', {
		  templateUrl: 'partials/home.html'
	  });
	  
	  $routeProvider.otherwise({
	      redirectTo: 'partials/error.html'
	  });
   }]);
