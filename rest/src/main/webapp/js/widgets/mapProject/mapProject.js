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
    function($scope, $http, $rootScope, $location, $uibModal, localStorageService, appConfig, gpService) {
      
      // get the local storage variables
      $scope.project = localStorageService.get('focusProject');
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.userToken = localStorageService.get('userToken');
      
      var deployBrowserLabel = appConfig['deploy.terminology.browser.label']; 
      
      $scope.project.terminologyButtonText =
        (!($scope.project.sourceTerminology == 'SNOMEDCT' || $scope.project.sourceTerminology.startsWith('SNOMEDCT_')))
            ? (deployBrowserLabel == null || typeof deployBrowserLabel == 'undefined' || deployBrowserLabel === '' )
                  ? $scope.project.sourceTerminology
                  : deployBrowserLabel
            : (deployBrowserLabel == null || typeof deployBrowserLabel == 'undefined' || deployBrowserLabel === '' ) 
                  ? $scope.project.destinationTerminology
                  : deployBrowserLabel;
      
                   
                  
      // flag indicating if index viewer is available for dest terminology
      $scope.indexViewerExists = false;
      
      //must be disabled in config file, otherwise enabled even if key not in config
      $scope.showDeltaIsEnabled = (appConfig['deploy.mapproject.showdelta.button.enabled'] === 'true') ? true : false; 

      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        $scope.project = parameters.focusProject;
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
          gpService.increment();
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
              gpService.decrement();
            }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });

          } else {
            gpService.decrement();

          }
        }
      };

      $scope.generateTestingStateForKLININ = function() {
        gpService.increment();

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
            gpService.decrement();
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.generateTestingStateForBHEKRE = function() {
        gpService.increment();
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
            gpService.decrement();
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.showDelta = function() {

        var modalInstance = $uibModal.open({
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
        gpService.increment();
        $http({
          url : root_workflow + 'project/id/' + $scope.project.id + '/compute',
          dataType : 'json',
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          gpService.decrement();
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // Compute names
      $scope.computeNames = function() {
        gpService.increment();
        $http({
          url : root_mapping + 'project/id/' + $scope.project.id + '/names',
          dataType : 'json',
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          gpService.decrement();
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      var ShowDeltaModalCtrl = function($scope, $http, $uibModalInstance, terminology, version) {

        $scope.pageSize = 10;
        $scope.terminology = terminology; // used
        // for
        // title

        $scope.close = function() {
          $uibModalInstance.close();
        };

        $scope.getConcepts = function(page, filter) {
          gpService.increment();
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
            gpService.decrement();

            $scope.concepts = data.searchResult;
            $scope.nConcepts = data.totalCount;
            $scope.numConceptPages = Math.ceil(data.totalCount / $scope.pageSize);

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.concepts = [];
            // $rootScope.handleHttpError(data, status, headers,
            // config);
          });

        };
        $scope.getConcepts(1, null);
      };

      $scope.openConceptBrowser = function() {
        var myWindow = null;

        if (appConfig['deploy.snomed.browser.force'] === 'true') {
         myWindow = window.open(appConfig['deploy.snomed.browser.url']); 
        }
        else {
          if ($scope.currentUser.userName === 'guest')
            myWindow = window.open(appConfig['deploy.snomed.browser.url.base']);
          else if ($scope.project.sourceTerminology === 'SNOMEDCT_US') 
            myWindow = window.open(appConfig['deploy.snomed.dailybuild.url.base']+appConfig['deploy.snomed.dailybuild.url.us']);
          else
            myWindow = window.open(appConfig['deploy.snomed.dailybuild.url.base']+appConfig['deploy.snomed.dailybuild.url.other']);
        }
        myWindow.focus();
      };

      $scope.openTerminologyBrowser = function(browserRequest){
        var browserUrl = appConfig['deploy.terminology.browser.url'];
        if (browserUrl == null || browserUrl === "")
        {
          var currentUrl = window.location.href;
          var baseUrl = currentUrl.substring(0, currentUrl.indexOf('#') + 1);
          var browserUrl = baseUrl + '/terminology/browser';
          
          localStorageService.add('browserRequest', browserRequest);
        }
        
        var myWindow = window.open(browserUrl, browserRequest + 'terminologyBrowserWindow?browserRequest');
        myWindow.focus();
      }

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

    });
