'use strict';
angular
  .module('mapProjectApp.widgets.mapProject', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('mapProject', {
      title : 'Map Project Details',
      description : 'Map Project Details',
      controller : 'MapProjectWidgetCtrl',
      templateUrl : 'js/widgets/mapProject/mapProject.html',
      edit : {}
    });
  })
  .controller(
    'MapProjectWidgetCtrl',
    function($scope, $http, $rootScope, $location, $modal, localStorageService) {

      // get the local storage variables
      $scope.project = localStorageService.get('focusProject');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.userToken = localStorageService.get('userToken');

      // flag indicating if index viewer is available for dest terminology
      $scope.indexViewerExists = false;

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        console.debug('on localStorageModule.notification.setFocusProject');
        $scope.project = parameters.focusProject;
        console.debug($scope.project);
      });

      // the only local storage variable required for this app is
      // userToken
      $scope.$watch('userToken', function() {
        $http.defaults.headers.common.Authorization = $scope.userToken;
        setIndexViewerStatus();
      });

      $scope.goProjectDetails = function() {
        var path = '/project/details';
        // redirect page
        $location.path(path);
      };

      $scope.goMapRecords = function() {
        var path = '/project/records';
        // redirect page
        $location.path(path);
      };

      $scope.generateTestData = function() {

        if ($scope.nConflicts == undefined || $scope.nConflicts == null) {
          alert('You must specify the number of conflicts to be generated.');
        } else {
          $rootScope.glassPane++;
          var confirmGenerate = confirm('Are you sure you want to generate test data?');
          if (confirmGenerate == true) {
            // call the generate API
            $http(
              {
                url : root_workflow + 'project/id/' + $scope.project.id
                  + '/generateConflicts/maxConflicts/' + $scope.nConflicts,
                dataType : 'json',
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
              $rootScope.glassPane--;
            }).error(function(data, status, headers, config) {
              $rootScope.glassPane--;
              $rootScope.handleHttpError(data, status, headers, config);
            });

          } else {
            $rootScope.glassPane--;

          }
        }
      };

      $scope.generateTestingStateForKLININ = function() {
        $rootScope.glassPane++;

        var confirmGenerate = confirm('Are you sure you want to generate the clean mapping user testing state?');
        if (confirmGenerate == true) {
          // call the generate API
          $http(
            {
              url : root_workflow + 'project/id/' + $scope.project.id
                + '/generateTestingStateKLININ',
              dataType : 'json',
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            $rootScope.glassPane--;
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.generateTestingStateForBHEKRE = function() {
        $rootScope.glassPane++;
        var confirmGenerate = confirm('Are you sure you want to generate the clean mapping user testing state?');
        if (confirmGenerate == true) {
          // call the generate API
          $http(
            {
              url : root_workflow + 'project/id/' + $scope.project.id
                + '/generateTestingStateBHEKRE',
              dataType : 'json',
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            $rootScope.glassPane--;
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.showDelta = function() {

        var modalInstance = $modal.open({
          templateUrl : 'partials/delta-concepts.html',
          controller : ShowDeltaModalCtrl,
          resolve : {
            terminology : function() {
              return $scope.project.sourceTerminology;
            },
            version : function() {
              return $scope.project.sourceTerminologyVersion;
            }
          }
        });

        modalInstance.result.then(function() {
          // do nothing, placeholder
        });

      };

      // Compute Workflow
      $scope.computeWorkflow = function() {
        console.debug('compute workflow');
        $rootScope.glassPane++;
        $http({
          url : root_workflow + 'project/id/' + $scope.project.id + '/compute',
          dataType : 'json',
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          $rootScope.glassPane--;
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      }

      var ShowDeltaModalCtrl = function($scope, $http, $modalInstance, terminology, version) {

        $scope.pageSize = 10;
        $scope.terminology = terminology; // used
        // for
        // title

        $scope.close = function() {
          $modalInstance.close();
        };

        $scope.getConcepts = function(page, filter) {
          $rootScope.glassPane++;
          var pfsParameterObj = {
            'startIndex' : page == -1 ? -1 : (page - 1) * $scope.pageSize,
            'maxResults' : page == -1 ? -1 : $scope.pageSize,
            'sortField' : null,
            'queryRestriction' : filter
          };
          $http({
            url : root_content + 'terminology/id/' + terminology + '/' + version + '/delta',
            dataType : 'json',
            method : 'POST',
            data : pfsParameterObj,
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $rootScope.glassPane--;

            $scope.concepts = data.searchResult;
            $scope.nConcepts = data.totalCount;
            $scope.numConceptPages = Math.ceil(data.totalCount / $scope.pageSize);

          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;
            $scope.concepts = [];
            // $rootScope.handleHttpError(data, status, headers,
            // config);
          });

        };
        $scope.getConcepts(1, null);
      };

      $scope.openConceptBrowser = function() {
        if ($scope.currentUser.userName === 'guest')
          var myWindow = window.open('http://browser.ihtsdotools.org/index.html?perspective=full'
            + '&edition=en-edition'
            + '&server=https://browser-aws-1.ihtsdotools.org/&langRefset=900000000000509007'
            + '&acceptLicense=true');
        else
          var myWindow = window
            .open(
              'http://dailybuild.ihtsdotools.org/index.html?perspective=full&diagrammingMarkupEnabled=true&acceptLicense=true',
              'browserWindow');
        myWindow.focus();
      };

      // Open index viewer
      $scope.openIndexViewer = function() {
        var currentUrl = window.location.href;
        var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
        var newUrl = baseUrl + '/index/viewer';
        var myWindow = window.open(newUrl, 'indexViewerWindow');
        myWindow.focus();
      };

      // Set index viewer status
      function setIndexViewerStatus() {
        $http(
          {
            url : root_content + 'index/' + $scope.project.destinationTerminology + '/'
              + $scope.project.destinationTerminologyVersion,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          if (data.searchResult.length > 0) {
            $scope.indexViewerExists = true;
          } else {
            $scope.indexViewerExists = false;
          }
        }).error(function(data, status, headers, config) {
          $scope.indexViewerExists = false;
        });
      }
      ;
    });
