'use strict';

var mapProjectAppServices = angular.module('mapProjectAppServices', []);

/* Services */
mapProjectAppServices.service('utilityService', function($q, $http, localStorageService){

		/*
		get: function(terminology){
			var deferred = $q.defer();
			$http({
				url: root_metadata + "all/" + terminology,
				dataType: "json",
				method: "GET",
				headers: {
					"Content-Type": "application/json"
				}
			}).success(function(data) {
				if (data){
					data.terminology = terminology;
					deferred.resolve(data);
				} else {
					deferred.reject();
				}
			}).error(function() {
				deferred.reject();
			});
			return deferred.promise;
		}*/
})

