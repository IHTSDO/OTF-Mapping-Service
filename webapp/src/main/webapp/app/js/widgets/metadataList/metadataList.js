/*
 * The MIT License
 * 
 * Copyright (c) 2013, Sebastian Sdorra
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

'use strict';

angular.module('mapProjectApp.widgets.metadataList', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('metadataList', {
        title: 'Metadata',
        description: 'Display metadata for a terminology',
        templateUrl: 'js/widgets/metadataList/metadataList.html',
        controller: 'metadataCtrl',
        resolve: {
          data: function(metadataService, config){
            if (config.terminology){
              return metadataService.get(config.terminology);
            } else {
              return metadataService.get('SNOMEDCT');
            }
          }
        },
        edit: {
          templateUrl: 'js/widgets/metadataList/edit.html'
        }
      });
  })
  .service('metadataService', function($q, $http){
    return {
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
	                deferred.resolve(data);
	              } else {
	                deferred.reject();
	              }
		  }).error(function() {
              deferred.reject();
			  $scope.errorMetadata = "Error retrieving all metadata";
		 });
        return deferred.promise;
      }
    };
  })
  .controller('metadataCtrl', function($scope, data){
    $scope.data = data;
    $scope.keyValuePairLists = data.keyValuePairList;
  });