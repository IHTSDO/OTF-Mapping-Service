'use strict';

angular
  .module('mapProjectApp.widgets.applicationAdmin', [ 'adf.provider' ])
  .config(
    function(dashboardProvider) {
      dashboardProvider
        .widget(
          'applicationAdmin',
          {
            title : 'Application Administration',
            description : 'Provides for the addition, deletion and updating of application level metadata.',
            templateUrl : 'js/widgets/applicationAdmin/applicationAdmin.html',
            controller : 'applicationAdminCtrl',
            resolve : {},
            edit : {}
          });
    })
  .controller(
    'applicationAdminCtrl',
    [
      '$scope',
      '$http',
      '$sce',
      '$rootScope',
      '$location',
      'localStorageService',
      '$q',
      'gpService',
      function($scope, $http, $sce, $rootScope, $location, localStorageService, $q, gpService) {

        $scope.page = 'project';

        $scope.currentRole = localStorageService.get('currentRole');
        $scope.currentUser = localStorageService.get('currentUser');
        $scope.focusProject = localStorageService.get('focusProject');
        $scope.mapProjects = localStorageService.get('mapProjects');
        $scope.mapUsers = localStorageService.get('mapUsers');
        $scope.mapProjectMetadata = localStorageService.get('mapProjectMetadata');
        $scope.newAllowableForNullTarget = false;
        $scope.newIsComputed = false;
        $scope.newRelationAllowableForNullTarget = false;
        $scope.newRelationIsComputed = false;
        $scope.newAgeRangeUpperInclusive = false;
        $scope.newAgeRangeLowerInclusive = false;
        $scope.newMapProjectPublished = false;
        $scope.newMapProjectRuleBased = false;
        $scope.newMapProjectGroupStructure = false;
        $scope.newMapProjectScopeDescendantsFlag = false;
        $scope.newMapProjectScopeExcludedDescendantsFlag = false;
        $scope.newMapProjectPublic = false;
        $scope.newMapProjectPropagationFlag = false;
        $scope.newMapProjectPropagationThreshold = null;
        $scope.newHandler;

        $scope.terminologyVersionPairs = new Array();
        $scope.terminologyVersionPairCount = 0;
        $scope.terminologyVersionScopeTriplets = new Array();
        $scope.mapProjectMetadataPairs = new Array();

        $scope.termLoadData = new Array();
        $scope.termLoadVersions = new Array();
        $scope.termLoadScopes = new Array();
        $scope.termLoadAwsZipFileName = '';
        $scope.termLoadVersionFileNameMap = new Map();
        
        $scope.downloadedGmdnVersions = new Array();
        
        var editingPerformed = new Array();
        var previousUserPage = 1;
        var previousAdvicePage = 1;
        var previousPrinciplePage = 1;
        var previousRelationPage = 1;
        var previousReportDefinitionPage = 1;
        var previousQaDefinitionPage = 1;

        $scope.allowableMapTypes = new Array();
        $scope.allowableMapRelationStyles = new Array();
        $scope.allowableWorkflowTypes = new Array();
        $scope.defaultProjectSpecificAlgorithmHandler = '(No default found)';

        $scope.testReportSuccess = false; // flag for
        // whether new
        // report has
        // passed
        // testing
        $scope.testReportError = null; // error returned
        // for report not
        // passing test
        $scope.testQaSuccess = false;
        $scope.testQaError = null;

        // instantiate the new report definition with default
        // success/error flags
        $scope.newDefinition = {
          'testReportSuccess' : false,
          'testReportError' : null
        };

        $scope.newQaDefinition = {
          'testQaSuccess' : false,
          'testQaError' : null
        };

        // enumerate the report definition field pick lists
        $scope.definitionQueryTypes = [ 'NONE', 'SQL', 'HQL', 'LUCENE' ];
        $scope.definitionResultTypes = [ 'CONCEPT', 'MAP_RECORD' ];
        $scope.definitionRoles = [ 'VIEWER', 'SPECIALIST', 'LEAD', 'ADMINISTRATOR' ];
        $scope.definitionTimePeriods = [ 'DAILY', 'WEEKLY', 'MONTHLY', 'ANNUALLY' ];
        $scope.definitionFrequencies = [ 'DAILY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
          'FRIDAY', 'SATURDAY', 'SUNDAY', 'FIRST_OF_MONTH', 'MID_MONTH', 'LAST_OF_MONTH',
          'ON_DEMAND' ];

        $scope.userApplicationRoles = [ 'VIEWER', 'ADMINISTRATOR' ];

        //get list of type in $scope.terminologyFiles?
        $scope.terminologyInputTypes = [ 'GMDN', 'SNOMED CT' ];
        
        // Event for focus project change
        $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
          $scope.focusProject = parameters.focusProject;
        });

        // Event for map project metadata change
        $scope.$on('localStorageModule.notification.setMapProjectMetadata', function(event,
          parameters) {
          $scope.mapProjectMetadata = parameters.value;
          initializeMapProjectMetadata();
          // force the gui to update the
          // select pick-lists after
          // metadata is loaded
          $scope.mapProjects = null;
          $scope.mapProjects = localStorageService.get('mapProjects');
        });

        $scope.userToken = localStorageService.get('userToken');

        // Watch for focus project change
        $scope.$watch([ 'focusProject', 'userToken' ], function() {
          if ($scope.focusProject != null && $scope.userToken != null) {
            $http.defaults.headers.common.Authorization = $scope.userToken;
            $scope.go();
          }
        });

        // Load metadata once focus project is ready
        $scope.go = function() {
          // reload the application's Terminologies
          reloadTerminologies();

          // initialize map project metadata variables
          initializeMapProjectMetadata();

          $http({
            url : root_mapping + 'user/users',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
        // reconstruct emails for ihtsdo.gov users - privacy caution
            // others will remain as 'Private email'
        for (var i = 0; i < data.mapUser.length; i++) {
              if (data.mapUser[i].email != 'Private email') {
                data.mapUser[i].email = data.mapUser[i].email + '@ihtsdo.gov';
              }
            }
            $scope.mapUsers = data.mapUser;
            localStorageService.add('mapUsers', data.mapUser);
            $rootScope.$broadcast('localStorageModule.notification.setMapUsers', {
              key : 'mapUsers',
              mapUsers : data.mapUsers
            });
            $scope.allowableMapUsers = localStorageService.get('mapUsers');
            $scope.getPagedUsers(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_mapping + 'advice/advices',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapAdvices = data.mapAdvice;
            localStorageService.add('mapAdvices', data.mapAdvice);
            $rootScope.$broadcast('localStorageModule.notification.setMapAdvices', {
              key : 'mapAdvices',
              mapAdvices : data.mapAdvices
            });
            $scope.allowableMapAdvices = localStorageService.get('mapAdvices');
            $scope.getPagedAdvices(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_mapping + 'relation/relations',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapRelations = data.mapRelation;
            localStorageService.add('mapRelations', data.mapRelation);
            $rootScope.$broadcast('localStorageModule.notification.setMapRelations', {
              key : 'mapRelations',
              mapRelations : data.mapRelations
            });
            $scope.allowableMapRelations = localStorageService.get('mapRelations');
            $scope.getPagedRelations(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_mapping + 'principle/principles',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapPrinciples = data.mapPrinciple;
            localStorageService.add('mapPrinciples', data.mapPrinciple);
            $rootScope.$broadcast('localStorageModule.notification.setMapPrinciples', {
              key : 'mapPrinciples',
              mapPrinciples : data.mapPrinciples
            });
            $scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
            $scope.getPagedPrinciples(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_mapping + 'ageRange/ageRanges',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapAgeRanges = data.mapAgeRange;
            localStorageService.add('mapAgeRanges', data.mapAgeRange);
            $rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges', {
              key : 'mapAgeRanges',
              mapAgeRanges : data.mapAgeRanges
            });
            $scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_reporting + 'definition/definitions',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.reportDefinitions = data.reportDefinition;
            localStorageService.add('reportDefinitions', data.reportDefinition);
            $rootScope.$broadcast('localStorageModule.notification.setReportDefinitions', {
              key : 'reportDefinitions',
              reportDefinitions : data.reportDefinitions
            });
            $scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');
            $scope.getPagedReportDefinitions(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_reporting + 'qaCheckDefinition/qaCheckDefinitions',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.qaCheckDefinitions = data.reportDefinition;
            localStorageService.add('qaCheckDefinitions', data.reportDefinition);
            $rootScope.$broadcast('localStorageModule.notification.setQaDefinitions', {
              key : 'qaCheckDefinitions',
              qaCheckDefinitions : data.qaCheckDefinitions
            });
            $scope.allowableQaDefinitions = localStorageService.get('qaCheckDefinitions');
            $scope.getPagedQaDefinitions(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          // set pagination variables
          $scope.pageSize = 5;
          $scope.maxSize = 5;

          $scope.orderProp = 'id';
        };

        // function to return trusted html code (for tooltip
        // content)
        $scope.to_trusted = function(html_code) {
          return $sce.trustAsHtml(html_code);
        };

        // /////////////////////////////////////////////////////////////
        // Functions to display and filter advices and
        // principles
        // NOTE: This is a workaround due to pagination
        // issues
        // /////////////////////////////////////////////////////////////

        // get paged functions
        // - sorts (by id) filtered elements
        // - counts number of filtered elmeents
        // - returns artificial page via slice
        $scope.getPagedUsers = function(page, filter) {
          if ($scope.userInEditingPerformed() == true) {
            if (confirm('You have unsaved changes.\n\n Are you sure that you want to switch pages?') == false) {
              $scope.pageUser = previousUserPage;
              return;
            }
          }
          $scope.userFilter = filter;
          $scope.pagedUser = $scope.sortByKey($scope.mapUsers, 'id').filter(containsUserFilter);
          $scope.pagedUserCount = $scope.pagedUser.length;
          $scope.pagedUser = $scope.pagedUser.slice((page - 1) * $scope.pageSize, page
            * $scope.pageSize);
          previousUserPage = page;
        };

        $scope.getPagedAdvices = function(page, filter) {
          if ($scope.adviceInEditingPerformed() == true) {
            if (confirm('You have unsaved changes.\n\n Are you sure that you want to switch pages?') == false) {
              $scope.pageAdvice = previousAdvicePage;
              return;
            }
          }
          $scope.adviceFilter = filter;
          $scope.pagedAdvice = $scope.sortByKey($scope.mapAdvices, 'id').filter(
            containsAdviceFilter);
          $scope.pagedAdviceCount = $scope.pagedAdvice.length;
          $scope.pagedAdvice = $scope.pagedAdvice.slice((page - 1) * $scope.pageSize, page
            * $scope.pageSize);
          previousAdvicePage = page;
        };

        $scope.getPagedRelations = function(page, filter) {
          if ($scope.relationInEditingPerformed() == true) {
            if (confirm('You have unsaved changes.\n\n Are you sure that you want to switch pages?') == false) {
              $scope.pageRelation = previousRelationPage;
              return;
            }
          }
          $scope.relationFilter = filter;
          $scope.pagedRelation = $scope.sortByKey($scope.mapRelations, 'id').filter(
            containsRelationFilter);
          $scope.pagedRelationCount = $scope.pagedRelation.length;
          $scope.pagedRelation = $scope.pagedRelation.slice((page - 1) * $scope.pageSize, page
            * $scope.pageSize);
          previousRelationPage = page;
        };

        $scope.getPagedPrinciples = function(page, filter) {
          if ($scope.principleInEditingPerformed() == true) {
            if (confirm('You have unsaved changes.\n\n Are you sure that you want to switch pages?') == false) {
              $scope.pagePrinciple = previousPrinciplePage;
              return;
            }
          }
          $scope.principleFilter = filter;
          $scope.pagedPrinciple = $scope.sortByKey($scope.mapPrinciples, 'id').filter(
            containsPrincipleFilter);
          $scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
          $scope.pagedPrinciple = $scope.pagedPrinciple.slice((page - 1) * $scope.pageSize, page
            * $scope.pageSize);
          previousPrinciplePage = page;
        };

        $scope.getPagedReportDefinitions = function(page, filter) {
          if ($scope.reportDefinitionInEditingPerformed() == true) {
            if (confirm('You have unsaved changes.\n\n Are you sure that you want to switch pages?') == false) {
              $scope.pageReportDefinition = previousReportDefinitionPage;
              return;
            }
          }
          $scope.reportDefinitionFilter = filter;
          $scope.pagedReportDefinition = $scope.sortByKey($scope.reportDefinitions, 'name').filter(
            containsReportDefinitionFilter);
          $scope.pagedReportDefinitionCount = $scope.pagedReportDefinition.length;
          $scope.pagedReportDefinition = $scope.pagedReportDefinition.slice((page - 1)
            * $scope.pageSize, page * $scope.pageSize);
          previousReportDefinitionPage = page;
        };

        $scope.getPagedQaDefinitions = function(page, filter) {
          if ($scope.qaCheckDefinitionInEditingPerformed() == true) {
            if (confirm('You have unsaved changes.\n\n Are you sure that you want to switch pages?') == false) {
              $scope.pageQaDefinition = previousQaDefinitionPage;
              return;
            }
          }
          $scope.qaCheckDefinitionFilter = filter;
          $scope.pagedQaDefinition = $scope.sortByKey($scope.qaCheckDefinitions, 'id').filter(
            containsQaDefinitionFilter);
          $scope.pagedQaDefinitionCount = $scope.pagedQaDefinition.length;
          $scope.pagedQaDefinition = $scope.pagedQaDefinition.slice((page - 1) * $scope.pageSize,
            page * $scope.pageSize);
          previousQaDefinitionPage = page;
        };

        $scope.getPagedTerminologies = function(terminology, filter) {
          $scope.userFilter = filter;
          $scope.pagedUser = $scope.sortByKey($scope.mapUsers, 'id').filter(containsUserFilter);
          $scope.pagedUserCount = $scope.pagedUser.length;
          $scope.pagedUser = $scope.pagedUser.slice((page - 1) * $scope.pageSize, page
            * $scope.pageSize);
          previousUserPage = page;
        };        
        
        // functions to reset the filter and retrieve
        // unfiltered results
        $scope.resetUserFilter = function() {
          $scope.userFilter = '';
          $scope.getPagedUsers(1);
        };

        $scope.resetAdviceFilter = function() {
          $scope.adviceFilter = '';
          $scope.getPagedAdvices(1);
        };

        $scope.resetRelationFilter = function() {
          $scope.relationFilter = '';
          $scope.getPagedRelations(1);
        };

        $scope.resetPrincipleFilter = function() {
          $scope.principleFilter = '';
          $scope.getPagedPrinciples(1);
        };

        $scope.resetReportDefinitionFilter = function() {
          $scope.reportDefinitionFilter = '';
          $scope.getPagedReportDefinitions(1);
        };

        $scope.resetQaDefinitionFilter = function() {
          $scope.qaCheckDefinitionFilter = '';
          $scope.getPagedQaDefinitions(1);
        };

        // element-specific functions for filtering
        // do not want to search id or objectId

        function containsUserFilter(element) {

          // check if user filter is empty
          if ($scope.userFilter === '' || $scope.userFilter == null)
            return true;

          // otherwise check if upper-case user filter
          // matches upper-case element name, user name, or email
          if (element.userName.toString().toUpperCase().indexOf(
            $scope.userFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.name.toString().toUpperCase().indexOf(
            $scope.userFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.email.toString().toUpperCase().indexOf(
            $scope.userFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.applicationRole.toString().toUpperCase().indexOf(
            $scope.userFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function containsAdviceFilter(element) {

          // check if advice filter is empty
          if ($scope.adviceFilter === '' || $scope.adviceFilter == null)
            return true;

          // otherwise check if upper-case advice filter
          // matches upper-case element name or detail
          if (element.detail.toString().toUpperCase().indexOf(
            $scope.adviceFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.name.toString().toUpperCase().indexOf(
            $scope.adviceFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function containsReportDefinitionFilter(element) {
          // check if advice filter is empty
          if ($scope.reportDefinitionFilter === '' || $scope.reportDefinitionFilter == null)
            return true;

          // otherwise check if upper-case report
          // definition filter matches upper-case element
          // name or detail
          if (element.name.toString().toUpperCase().indexOf(
            $scope.reportDefinitionFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function containsQaDefinitionFilter(element) {

          // check if advice filter is empty
          if ($scope.qaCheckDefinitionFilter === '' || $scope.qaCheckDefinitionFilter == null)
            return true;

          // otherwise check if upper-case report
          // definition filter matches upper-case element
          // name or detail
          if (element.name.toString().toUpperCase().indexOf(
            $scope.qaCheckDefinitionFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function containsRelationFilter(element) {

          // check if relation filter is empty
          if ($scope.relationFilter === '' || $scope.relationFilter == null)
            return true;

          // otherwise check if upper-case relation filter
          // matches upper-case element name or detail
          if (element.terminologyId.toString().toUpperCase().indexOf(
            $scope.relationFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.name.toString().toUpperCase().indexOf(
            $scope.relationFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function containsPrincipleFilter(element) {

          // check if principle filter is empty
          if ($scope.principleFilter === '' || $scope.principleFilter == null)
            return true;

          // otherwise check if upper-case principle
          // filter matches upper-case element name or
          // detail
          if (element.principleId.toString().toUpperCase().indexOf(
            $scope.principleFilter.toString().toUpperCase()) != -1)
            return true;
          // if (
          // element.detail.toString().toUpperCase().indexOf(
          // $scope.principleFilter.toString().toUpperCase())
          // != -1) return true;
          if (element.name.toString().toUpperCase().indexOf(
            $scope.principleFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.sectionRef.toString().toUpperCase().indexOf(
            $scope.principleFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function reportDefinitionUsedInProjects(definition) {
          for (var i = 0; i < $scope.mapProjects.length; i++) {
            for (var j = 0; j < $scope.mapProjects[i].reportDefinition.length; j++) {
              if ($scope.mapProjects[i].reportDefinition[j].id == definition.id)
                return true;
            }
          }
          return false;
        }
        
        function reloadTerminologies() {
          
          var deferred = $q.defer();

          $http({
            url : root_metadata + 'terminology/terminologies',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
            function(data) {
              $scope.terminologyVersionPairs = new Array();
              for (var i = 0; i < data.keyValuePairList.length; i++) {
                for (var j = 0; j < data.keyValuePairList[i].keyValuePair.length; j++) {
                  $scope.terminologyVersionPairs.push(data.keyValuePairList[i].keyValuePair[j].key
                    + ' ' + data.keyValuePairList[i].keyValuePair[j].value);
                }
              }
              $scope.terminologyVersionPairCount = $scope.terminologyVersionPairs.length;
              
              deferred.resolve();
              
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
            deferred.reject();            
          });

          return deferred.promise;          
        }
        
        function getDownloadedGmdnVersions() {
          
          var deferred = $q.defer();

          $http({
            url : root_metadata + 'terminology/gmdn',
            dataType : 'text/plain',
            method : 'GET'
          }).success(
            function(data) {
              $scope.downloadedGmdnVersions = new Array();
              var downloadedVersionArray = data.split(';');
              for (var i = 0; i < downloadedVersionArray.length; i++) {
                $scope.downloadedGmdnVersions.push(downloadedVersionArray[i]);
                }            
              deferred.resolve();
              
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
            deferred.reject();            
          });

          return deferred.promise;          
        }        
        
        function initializeMapProjectMetadata() {
          if ($scope.mapProjectMetadata != null) {
            for (var i = 0; i < $scope.mapProjectMetadata.keyValuePairList.length; i++) {
              if ($scope.mapProjectMetadata.keyValuePairList[i].name == 'Map Refset Patterns') {
                for (var j = 0; j < $scope.mapProjectMetadata.keyValuePairList[i].keyValuePair.length; j++) {
                  $scope.allowableMapTypes
                    .push($scope.mapProjectMetadata.keyValuePairList[i].keyValuePair[j]);
                }
              }
              if ($scope.mapProjectMetadata.keyValuePairList[i].name == 'Relation Styles') {
                for (var j = 0; j < $scope.mapProjectMetadata.keyValuePairList[i].keyValuePair.length; j++) {
                  $scope.allowableMapRelationStyles
                    .push($scope.mapProjectMetadata.keyValuePairList[i].keyValuePair[j]);
                }
              }
              if ($scope.mapProjectMetadata.keyValuePairList[i].name == 'Workflow Types') {
                for (var j = 0; j < $scope.mapProjectMetadata.keyValuePairList[i].keyValuePair.length; j++) {
                  $scope.allowableWorkflowTypes
                    .push($scope.mapProjectMetadata.keyValuePairList[i].keyValuePair[j]);
                }
              }

              if ($scope.mapProjectMetadata.keyValuePairList[i].name == 'Project Specific Handlers') {
                for (var j = 0; j < $scope.mapProjectMetadata.keyValuePairList[i].keyValuePair.length; j++) {
                  if ($scope.mapProjectMetadata.keyValuePairList[i].keyValuePair[j].key === 'default') {
                    $scope.defaultProjectSpecificAlgorithmHandler = $scope.mapProjectMetadata.keyValuePairList[i].keyValuePair[j].value;
                  }
                }
              }
            }
            $scope.newMapProjectMapType = $scope.allowableMapTypes[0];
            $scope.newMapRelationStyle = $scope.allowableMapRelationStyles[0];
            $scope.newWorkflowType = $scope.allowableWorkflowTypes[0];
            $scope.newHandler = $scope.defaultProjectSpecificAlgorithmHandler;
          }
        }

        // helper function to sort a JSON array by field
        $scope.sortByKey = function sortById(array, key) {
          return array.sort(function(a, b) {
            var x, y;
            // if a number
            if (!isNaN(parseInt(a[key]))) {

              x = a[key];
              y = b[key];

            } else {

              x = new String(a[key]).toUpperCase();
              y = new String(b[key]).toUpperCase();

            }

            if (x < y)
              return -1;
            if (x > y)
              return 1;
            return 0;
          });
        };

        $scope.getSourceVersion = function(project) {
          return project.sourceTerminology + ' ' + project.sourceTerminologyVersion;
        };

        $scope.getDestinationVersion = function(project) {
          return project.destinationTerminology + ' ' + project.destinationTerminologyVersion;
        };

        $scope.getMapType = function(project) {
          for (var i = $scope.allowableMapTypes.length; i--;) {
            if ($scope.allowableMapTypes[i].key === project.mapRefsetPattern ||
            $scope.allowableMapTypes[i].key == project.mapRefsetPattern.key)
              return $scope.allowableMapTypes[i];
          }
        };

        $scope.getWorkflowType = function(project) {
          for (var i = $scope.allowableWorkflowTypes.length; i--;) {
            if ($scope.allowableWorkflowTypes[i].key === project.workflowType ||
            $scope.allowableWorkflowTypes[i].key == project.workflowType.key)
              return $scope.allowableWorkflowTypes[i];
          }
        };

        $scope.getMapRelationStyle = function(project) {
          for (var i = $scope.allowableMapRelationStyles.length; i--;) {
            if ($scope.allowableMapRelationStyles[i].key === project.mapRelationStyle ||
            $scope.allowableMapRelationStyles[i].key == project.mapRelationStyle.key)
              return $scope.allowableMapRelationStyles[i];
          }
        };

        $scope.getHandler = function(project) {
          for (var i = $scope.handlers.length; i--;) {
            if ($scope.handlers[i] === project.projectSpecificAlgorithmHandlerClass)
              return $scope.handlers[i];
          }
        };

        $scope.setEditingPerformed = function(component) {
          editingPerformed.push(component);
        };

        $scope.isComponentChanged = function(component) {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i] === component) {
              return true;
            }
          }
          return false;
        };

        function removeComponentFromArray(arr, component) {
          // remove component
          for (var i = arr.length; i--;) {
            if (arr[i] === component) {
              arr.splice(i, 1);
            }
          }
        }

        // indicates if any unsaved user
        $scope.userInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].name != null && editingPerformed[i].isComputed != null) {
              return true;
            }
          }
          return false;
        };

        // indicates if any unsaved advice
        $scope.adviceInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].name != null && editingPerformed[i].isComputed != null) {
              return true;
            }
          }
          return false;
        };

        // reverts advice to last saved state
        $scope.revertUnsavedAdvices = function() {

          // clear advice from editingPerformed
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].name != null && editingPerformed[i].isComputed != null) {
              editingPerformed.splice(i, 1);
            }
          }
          // get last saved state of advice
          $http({
            url : root_mapping + 'advice/advices',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapAdvices = data.mapAdvice;
            localStorageService.add('mapAdvices', data.mapAdvice);
            $rootScope.$broadcast('localStorageModule.notification.setMapAdvices', {
              key : 'mapAdvices',
              mapAdvices : data.mapAdvices
            });
            $scope.allowableMapAdvices = localStorageService.get('mapAdvices');
            $scope.getPagedAdvices(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // indicates if any unsaved principle
        $scope.principleInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].principleId != null) {
              return true;
            }
          }
          return false;
        };

        // reverts principle to last saved state
        $scope.revertUnsavedPrinciples = function() {

          // clear principle from editingPerformed
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].principleId != null) {
              editingPerformed.splice(i, 1);
            }
          }
          // get last saved state of principle
          $http({
            url : root_mapping + 'principle/principles',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapPrinciples = data.mapPrinciple;
            localStorageService.add('mapPrinciples', data.mapPrinciple);
            $rootScope.$broadcast('localStorageModule.notification.setMapPrinciples', {
              key : 'mapPrinciples',
              mapPrinciples : data.mapPrinciples
            });
            $scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
            $scope.getPagedPrinciples(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // indicates if any unsaved relation
        $scope.relationInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].terminologyId != null) {
              return true;
            }
          }
          return false;
        };

        // reverts relation to last saved state
        $scope.revertUnsavedRelations = function() {

          // clear relation from editingPerformed
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].terminologyId != null) {
              editingPerformed.splice(i, 1);
            }
          }
          // get last saved state of relation
          $http({
            url : root_mapping + 'relation/relations',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapRelations = data.mapRelation;
            localStorageService.add('mapRelations', data.mapRelation);
            $rootScope.$broadcast('localStorageModule.notification.setMapRelations', {
              key : 'mapRelations',
              mapRelations : data.mapRelations
            });
            $scope.allowableMapRelations = localStorageService.get('mapRelations');
            $scope.getPagedRelations(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // indicates if any unsaved ageRange
        $scope.ageRangeInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].lowerUnits != null) {
              return true;
            }
          }
          return false;
        };

        // reverts ageRange to last saved state
        $scope.revertUnsavedAgeRanges = function() {

          // clear ageRange from editingPerformed
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].lowerUnits != null) {
              editingPerformed.splice(i, 1);
            }
          }
          // get last saved state of ageRange
          $http({
            url : root_mapping + 'ageRange/ageRanges',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapAgeRanges = data.mapAgeRange;
            localStorageService.add('mapAgeRanges', data.mapAgeRange);
            $rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges', {
              key : 'mapAgeRanges',
              mapAgeRanges : data.mapAgeRanges
            });
            $scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');
            $scope.getPagedAgeRanges(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // indicates if any unsaved reportDefinition
        $scope.reportDefinitionInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].qacheck == false) {
              return true;
            }
          }
          return false;
        };

        // indicates if any unsaved qaCheckDefinition
        $scope.qaCheckDefinitionInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].qacheck == true) {
              return true;
            }
          }
          return false;
        };

        // reverts an unsaved Report or QA Check definition
        // 1) Removes from edited list
        // 2) Replaces scope object with REST-retrieved object
        $scope.revertUnsavedReportDefinition = function(definition) {
          // get last saved state of reportDefinitions
          $http({
            url : root_reporting + 'definition/id/' + definition.id,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
            function(data) {

              // remove this definition from the editing performed
              // array
              for (var i = editingPerformed.length; i--;) {
                if (editingPerformed[i].id == definition.id
                  && editingPerformed[i].name == definition.name) {
                  editingPerformed.splice(i, 1);
                }

              }

              // replace this definition with the new data
              if (data.qacheck == false) {
                for (var i = 0; i < $scope.pagedReportDefinition.length; i++) {
                  if ($scope.pagedReportDefinition[i].id == data.id) {
                    $scope.pagedReportDefinition[i] = data;
                  }
                }
              } else {
                for (var i = 0; i < $scope.pagedQaDefinition.length; i++) {
                  if ($scope.pagedQaDefinition[i].id == data.id) {
                    $scope.pagedQaDefinition[i] = data;
                  }
                }
              }

            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // reverts reportDefinition to last saved state
        $scope.revertUnsavedReportDefinitions = function() {

          // clear reportDefinition from editingPerformed
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].qacheck == false) {
              editingPerformed.splice(i, 1);
            }
          }
          // get last saved state of reportDefinitions
          $http({
            url : root_reporting + 'definition/definitions',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.reportDefinitions = data.reportDefinition;
            localStorageService.add('reportDefinitions', data.reportDefinition);
            $rootScope.$broadcast('localStorageModule.notification.setReportDefinitions', {
              key : 'reportDefinitions',
              reportDefinitions : data.reportDefinitions
            });
            $scope.allowableMapReportDefinitions = localStorageService.get('reportDefinitions');
            $scope.getPagedReportDefinitions(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // reverts qaCheckDefinition to last saved state
        $scope.revertUnsavedQaDefinitions = function() {

          // clear qaCheckDefinition from editingPerformed
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].qacheck == true) {
              editingPerformed.splice(i, 1);
            }
          }
          // get last saved state of reportDefinitions
          $http({
            url : root_reporting + 'qaCheckDefinition/qaCheckDefinitions',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.qaCheckDefinitions = data.reportDefinition;
            localStorageService.add('qaCheckDefinitions', data.qaCheckDefinition);
            $rootScope.$broadcast('localStorageModule.notification.setQaDefinitions', {
              key : 'qaCheckDefinitions',
              qaCheckDefinitions : data.qaCheckDefinitions
            });
            $scope.allowableMapQaDefinitions = localStorageService.get('qaCheckDefinitions');
            $scope.getPagedQaDefinitions(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // indicates if any unsaved project
        $scope.projectInEditingPerformed = function() {
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].sourceTerminology != null) {
              return true;
            }
          }
          return false;
        };

        // reverts project to last saved state
        $scope.revertUnsavedProjects = function() {

          // clear project from editingPerformed
          for (var i = editingPerformed.length; i--;) {
            if (editingPerformed[i].sourceTerminology != null) {
              editingPerformed.splice(i, 1);
            }
          }
          // get last saved state of project
          $http({
            url : root_mapping + 'project/projects',
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.mapProjects = data.mapProject;
            localStorageService.add('mapProjects', data.mapProject);
            $rootScope.$broadcast('localStorageModule.notification.setMapProjects', {
              key : 'mapProjects',
              mapProjects : data.mapProjects
            });
            $scope.allowableMapProjects = localStorageService.get('mapProjects');
            $scope.getPagedProjects(1, '');
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // function to change project from the header
        $scope.changeFocusProject = function(mapProject) {
          $scope.focusProject = mapProject;
          // update and broadcast the new focus project
          localStorageService.add('focusProject', $scope.focusProject);
          $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });

          // update the user preferences
          $scope.preferences.lastMapProjectId = $scope.focusProject.id;
          localStorageService.add('preferences', $scope.preferences);
          $rootScope.$broadcast('localStorageModule.notification.setUserPreferences', {
            key : 'userPreferences',
            userPreferences : $scope.preferences
          });

        };

        $scope.goToHelp = function() {
          var path;
          if ($scope.page != 'mainDashboard') {
            path = 'help/' + $scope.page + 'Help.html';
          } else {
            path = 'help/' + $scope.currentRole + 'DashboardHelp.html';
          }

          // redirect page
          $location.path(path);
        };

        $scope.deleteUser = function(user) {

          if (confirm('Are you sure that you want to delete a map user?') == false)
            return;

          $http({
            url : root_mapping + 'user/delete',
            dataType : 'json',
            data : user,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error deleting map user from application.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'user/users',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            })
            // update the users on success
            // no need to update project as if user is
            // attached, delete will fail
            .success(function(data) {
              // reconstruct emails for ihtsdo.gov users - privacy caution
              // others will remain as 'Private email'
              for (var i = 0; i < data.mapUser.length; i++) {
                  if (data.mapUser[i].email != 'Private email') {
                    data.mapUser[i].email = data.mapUser[i].email + '@ihtsdo.gov';
                  }
              }
              $scope.mapUsers = data.mapUser;
              $scope.resetUserFilter();

              localStorageService.add('mapUsers', data.mapUser);
              $rootScope.$broadcast('localStorageModule.notification.setMapUsers', {
                key : 'mapUsers',
                mapUsers : data.mapUsers
              });
              $scope.allowableMapUsers = localStorageService.get('mapUsers');

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.deleteAdvice = function(advice) {

          if (confirm('Are you sure that you want to delete a map advice?') == false)
            return;

          $http({
            url : root_mapping + 'advice/delete',
            dataType : 'json',
            data : advice,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error deleting map advice from application.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'advice/advices',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapAdvices = data.mapAdvice;
              $scope.resetAdviceFilter();
              for (var j = 0; j < $scope.focusProject.mapAdvice.length; j++) {
                if (advice.id === $scope.focusProject.mapAdvice[j].id) {
                  $scope.focusProject.mapAdvice[j] = advice;
                  
                }
              }
              
              localStorageService.add('mapAdvices', data.mapAdvice);
              $rootScope.$broadcast('localStorageModule.notification.setMapAdvices', {
                key : 'mapAdvices',
                mapAdvices : data.mapAdvices
              });
              $scope.allowableMapAdvices = localStorageService.get('mapAdvices');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // Update user
        $scope.updateUser = function(user) {
          $http({
            url : root_mapping + 'user/update',
            dataType : 'json',
            data : user,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            removeComponentFromArray(editingPerformed, user);
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error updating map user.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'user/users',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              // reconstruct emails for ihtsdo.gov users - privacy caution
              // others will remain as 'Private email'
              for (var i = 0; i < data.mapUser.length; i++) {
                  if (data.mapUser[i].email != 'Private email') {
                    data.mapUser[i].email = data.mapUser[i].email + '@ihtsdo.gov';
                  }
              }
              $scope.mapUsers = data.mapUser;
              for (var j = 0; j < $scope.focusProject.mapSpecialist.length; j++) {
                if (user.id === $scope.focusProject.mapSpecialist[j].id) {
                  $scope.focusProject.mapSpecialist[j] = user;
                }
              }
              for (var j = 0; j < $scope.focusProject.mapLead.length; j++) {
                if (user.id === $scope.focusProject.mapLead[j].id) {
                  $scope.focusProject.mapLead[j] = user;
                }
              }
              for (var j = 0; j < $scope.focusProject.mapAdministrator.length; j++) {
                if (user.id === $scope.focusProject.mapAdministrator[j].id) {
                  $scope.focusProject.mapAdministrator[j] = user;
                }
              }
              localStorageService.add('mapUsers', data.mapUser);
              $rootScope.$broadcast('localStorageModule.notification.setMapUsers', {
                key : 'mapUsers',
                mapUsers : data.mapUsers
              });
              $scope.allowableMapUsers = localStorageService.get('mapUsers');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.updateAdvice = function(advice) {
          $http({
            url : root_mapping + 'advice/update',
            dataType : 'json',
            data : advice,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            removeComponentFromArray(editingPerformed, advice);
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error updating map advice.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'advice/advices',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapAdvices = data.mapAdvice;
              for (var j = 0; j < $scope.focusProject.mapAdvice.length; j++) {
                if (advice.id === $scope.focusProject.mapAdvice[j].id) {
                  $scope.focusProject.mapAdvice[j] = advice;
                }
              }
              localStorageService.add('mapAdvices', data.mapAdvice);
              $rootScope.$broadcast('localStorageModule.notification.setMapAdvices', {
                key : 'mapAdvices',
                mapAdvices : data.mapAdvices
              });
              $scope.allowableMapAdvices = localStorageService.get('mapAdvices');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.submitNewMapUser = function(mapUserName, mapUserFullName, mapUserEmail,
          mapUserApplicationRole) {
        	
          // have user confirm that an IMS account exists for new user
          if (!confirm("The user submitted here must already have an IMS account.  Please confirm an IMS account exists for this user before proceeding.")) {
          	return;
          } 	
        	
          var obj = {
            'userName' : mapUserName,
            'name' : mapUserFullName,
            'email' : mapUserEmail,
            'applicationRole' : mapUserApplicationRole
          };

          gpService.increment();

          $http({
            url : root_mapping + 'user/add',
            dataType : 'json',
            data : obj,
            method : 'PUT',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error adding new map user.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'user/users',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              // reconstruct emails for ihtsdo.gov users - privacy caution
              // others will remain as 'Private email'
              for (var i = 0; i < data.mapUser.length; i++) {
                  if (data.mapUser[i].email != 'Private email') {
                    data.mapUser[i].email = data.mapUser[i].email + '@ihtsdo.gov';
                  }
              }
              $scope.mapUsers = data.mapUser;
              $scope.resetUserFilter();
              localStorageService.add('mapUsers', data.mapUser);
              $rootScope.$broadcast('localStorageModule.notification.setMapUsers', {
                key : 'mapUsers',
                mapUsers : data.mapUsers
              });
              $scope.allowableMapUsers = localStorageService.get('mapUsers');
              gpService.decrement();
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
              gpService.decrement();
            });

          });
        };

        $scope.submitNewMapAdvice = function(mapAdviceName, mapAdviceDetail,
          allowableForNullTarget, isComputed) {
          var obj = {
            'name' : mapAdviceName,
            'detail' : mapAdviceDetail,
            'isAllowableForNullTarget' : allowableForNullTarget,
            'isComputed' : isComputed
          };

          $http({
            url : root_mapping + 'advice/add',
            dataType : 'json',
            data : obj,
            method : 'PUT',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error adding new map advice.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'advice/advices',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapAdvices = data.mapAdvice;
              $scope.resetAdviceFilter();
              localStorageService.add('mapAdvices', data.mapAdvice);
              $rootScope.$broadcast('localStorageModule.notification.setMapAdvices', {
                key : 'mapAdvices',
                mapAdvices : data.mapAdvices
              });
              $scope.allowableMapAdvices = localStorageService.get('mapAdvices');
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // Delete map relation
        $scope.deleteRelation = function(relation) {
          // confirm delete
          if (confirm('Are you sure that you want to delete a map relation?') == false)
            return;

          $http({
            url : root_mapping + 'relation/delete',
            dataType : 'json',
            data : relation,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error deleting map relation from application.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'relation/relations',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapRelations = data.mapRelation;
              $scope.resetRelationFilter();
              for (var j = 0; j < $scope.focusProject.mapRelation.length; j++) {
                if (relation.id === $scope.focusProject.mapRelation[j].id) {
                  $scope.focusProject.mapRelation[j] = relation;
                }
              }
              localStorageService.add('mapRelations', data.mapRelation);
              $rootScope.$broadcast('localStorageModule.notification.setMapRelations', {
                key : 'mapRelations',
                mapRelations : data.mapRelations
              });
              $scope.allowableMapRelations = localStorageService.get('mapRelations');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.updateRelation = function(relation) {
          $http({
            url : root_mapping + 'relation/update',
            dataType : 'json',
            data : relation,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            removeComponentFromArray(editingPerformed, relation);
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error updating map relation.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'relation/relations',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapRelations = data.mapRelation;
              for (var j = 0; j < $scope.focusProject.mapRelation.length; j++) {
                if (relation.id === $scope.focusProject.mapRelation[j].id) {
                  $scope.focusProject.mapRelation[j] = relation;
                }
              }
              localStorageService.add('mapRelations', data.mapRelation);
              $rootScope.$broadcast('localStorageModule.notification.setMapRelations', {
                key : 'mapRelations',
                mapRelations : data.mapRelations
              });
              $scope.allowableMapRelations = localStorageService.get('mapRelations');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.submitNewMapRelation = function(mapRelationName, mapRelationAbbreviation,
          mapRelationTerminologyId, allowableForNullTarget, isComputed) {
          var obj = {
            'terminologyId' : mapRelationTerminologyId,
            'name' : mapRelationName,
            'abbreviation' : mapRelationAbbreviation,
            'isAllowableForNullTarget' : allowableForNullTarget,
            'isComputed' : isComputed
          };
          $http({
            url : root_mapping + 'relation/add',
            dataType : 'json',
            data : obj,
            method : 'PUT',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error adding new map relation for the application.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'relation/relations',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapRelations = data.mapRelation;
              $scope.resetRelationFilter();
              localStorageService.add('mapRelations', data.mapRelation);
              $rootScope.$broadcast('localStorageModule.notification.setMapRelations', {
                key : 'mapRelations',
                mapRelations : data.mapRelations
              });
              $scope.allowableMapRelations = localStorageService.get('mapRelations');
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.updatePrinciple = function(principle) {
          $http({
            url : root_mapping + 'principle/update',
            dataType : 'json',
            data : principle,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            removeComponentFromArray(editingPerformed, principle);
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error updating map principle.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'principle/principles',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {

              $scope.mapPrinciples = data.mapPrinciple;
              for (var j = 0; j < $scope.focusProject.mapPrinciple.length; j++) {
                if (principle.id === $scope.focusProject.mapPrinciple[j].id) {
                  $scope.focusProject.mapPrinciple[j] = principle;
                }
              }
              localStorageService.add('mapPrinciples', data.mapPrinciple);
              $rootScope.$broadcast('localStorageModule.notification.setMapPrinciples', {
                key : 'mapPrinciples',
                mapPrinciples : data.mapPrinciples
              });
              $scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // Delete map principle
        $scope.deletePrinciple = function(principle) {
          if (confirm('Are you sure that you want to delete a map principle?') == false)
            return;

          $http({
            url : root_mapping + 'principle/delete',
            dataType : 'json',
            data : principle,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error deleting map principle from application.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'principle/principles',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapPrinciples = data.mapPrinciple;
              $scope.resetPrincipleFilter();
              for (var j = 0; j < $scope.focusProject.mapPrinciple.length; j++) {
                if (principle.id === $scope.focusProject.mapPrinciple[j].id) {
                  $scope.focusProject.mapPrinciple[j] = principle;
                }
              }
              localStorageService.add('mapPrinciples', data.mapPrinciple);
              $rootScope.$broadcast('localStorageModule.notification.setMapPrinciples', {
                key : 'mapPrinciples',
                mapPrinciples : data.mapPrinciples
              });
              $scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // Add new map principle
        $scope.submitNewMapPrinciple = function(mapPrincipleName, mapPrincipleId,
          mapPrincipleDetail, mapPrincipleSectionRef) {
          var obj = {
            'name' : mapPrincipleName,
            'principleId' : mapPrincipleId,
            'detail' : mapPrincipleDetail,
            'sectionRef' : mapPrincipleSectionRef
          };
          $http({
            url : root_mapping + 'principle/add',
            dataType : 'json',
            data : obj,
            method : 'PUT',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // n/a
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error adding new map principle.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'principle/principles',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapPrinciples = data.mapPrinciple;
              localStorageService.add('mapPrinciples', data.mapPrinciple);
              $rootScope.$broadcast('localStorageModule.notification.setMapPrinciples', {
                key : 'mapPrinciples',
                mapPrinciples : data.mapPrinciples
              });
              $scope.allowableMapPrinciples = localStorageService.get('mapPrinciples');
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // Delete age range
        $scope.deleteAgeRange = function(ageRange) {
          if (confirm('Are you sure that you want to delete an age range?') == false)
            return;

          for (var j = 0; j < $scope.focusProject.mapAgeRange.length; j++) {
            if (ageRange.name === $scope.focusProject.mapAgeRange[j].name) {
              $scope.focusProject.mapAgeRange.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
            key : 'focusProject',
            focusProject : $scope.focusProject
          });
          $scope.updateMapProject($scope.focusProject);
        };

        // Update age range
        $scope.updateAgeRange = function(ageRange) {
          $http({
            url : root_mapping + 'ageRange/update',
            dataType : 'json',
            data : ageRange,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            removeComponentFromArray(editingPerformed, ageRange);
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error updating age range.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'ageRange/ageRanges',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {

              $scope.mapAgeRanges = data.mapAgeRange;
              for (var j = 0; j < $scope.focusProject.mapAgeRange.length; j++) {
                if (ageRange.id === $scope.focusProject.mapAgeRange[j].id) {
                  $scope.focusProject.mapAgeRange[j] = ageRange;
                }
              }
              localStorageService.add('mapAgeRanges', data.mapAgeRange);
              $rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges', {
                key : 'mapAgeRanges',
                mapAgeRanges : data.mapAgeRanges
              });
              $scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // Add new age range
        $scope.submitNewMapAgeRange = function(name, lowerInclusive, lowerUnits, lowerValue,
          upperInclusive, upperUnits, upperValue) {
          var obj = {
            'lowerInclusive' : true,
            'lowerUnits' : lowerUnits,
            'lowerValue' : lowerValue,
            'name' : name,
            'upperInclusive' : true,
            'upperUnits' : upperUnits,
            'upperValue' : upperValue
          };
          $http({
            url : root_mapping + 'ageRange/add',
            dataType : 'json',
            data : obj,
            method : 'PUT',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // make the record pristine
            $scope.ageRangeForm.$setPristine();
            $scope.name = '';
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error adding new map age range.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_mapping + 'ageRange/ageRanges',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.mapAgeRanges = data.mapAgeRange;
              localStorageService.add('mapAgeRanges', data.mapAgeRange);
              $rootScope.$broadcast('localStorageModule.notification.setMapAgeRanges', {
                key : 'mapAgeRanges',
                mapAgeRanges : data.mapAgeRanges
              });
              $scope.allowableMapAgeRanges = localStorageService.get('mapAgeRanges');
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // function to return array of non-difference report
        // definitions
        // used to set the report name for difference
        // reports
        $scope.getNonDiffReportDefinitionNames = function() {

          var arr = [];

          // if report definitions not yet retrieved, return empty array
          if ($scope.reportDefinitions == null)
            return arr;

          for (var i = 0; i < $scope.reportDefinitions.length; i++) {
            if ($scope.reportDefinitions[i].diffReport == false)
              arr.push($scope.reportDefinitions[i].name);
          }
          return arr;
        };

        // Delete report definition
        $scope.deleteReportDefinition = function(reportDefinition) {
          if (confirm('Are you sure that you want to delete a map report definition?') == false)
            return;

          if (reportDefinitionUsedInProjects(reportDefinition) == true
            && confirm('This report definition is active in at least one project.\nAre you'
              + ' still sure that you want to delete a map report definition? \nAll reports'
              + ' with this report definition type will be deleted as well!') == false)
            return;

          gpService.increment();

          $http({
            url : root_reporting + 'definition/delete',
            dataType : 'json',
            data : reportDefinition,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            gpService.decrement();
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error deleting map reportDefinition from application.';
            $rootScope.handleHttpError(data, status, headers, config);
            gpService.decrement();
          }).then(function(data) {
            $http({
              url : root_reporting + 'definition/definitions',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.reportDefinitions = data.reportDefinition;
              $scope.resetReportDefinitionFilter();
              localStorageService.add('reportDefinitions', data.reportDefinition);
              $rootScope.$broadcast('localStorageModule.notification.setReportDefinitions', {
                key : 'reportDefinitions',
                reportDefinitions : data.reportDefinitions
              });
              $scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');

              // update and broadcast the updated focus project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              //$scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.validateReportDefinition = function(definition) {

          // initial report is null
          var testReportError = '';

          // check all parameters
          if (definition.name == null || definition.name === '')
            testReportError += 'You must specify a report name.\n';
          if (definition.roleRequired == null)
            testReportError += 'You must specify the required role.\n';
          if (definition.resultType == null)
            testReportError += 'You must specify the result type.\n';
          if (definition.frequency == null)
            testReportError += 'You must specify the report frequency\n';

          // check diff report parameters
          if (definition.diffReport == null) {
            testReportError += 'You must specify whether this report is a difference report\n';
          } else if (definition.diffReport == true) {
            if (definition.timePeriod == null)
              testReportError += 'You must specify the time period over which the difference report is calculated\n';
            if (definition.diffReportDefinitionName == null)
              testReportError += 'You must specify the report definition from which the difference report is calculated\n';

          } else if (definition.diffReport == false) {
            if (definition.queryType == null)
              testReportError += 'You must specify the query type\n';
            if (definition.queryType != 'NONE'
              && (definition.query == null || definition.query === ''))
              testReportError += 'You must specify a query\n';
          }

          if (testReportError != '')
            window.alert(testReportError);

          // return true if no errors found, false if any errors found
          return testReportError === '';

        };

        // helper function to set the new report definition test success
        // variable to false
        $scope.setTestReportSuccess = function(flag) {
          $scope.testReportSuccess = flag;
        };

        /**
         * Function to test a report definition
         */
        $scope.testReportDefinition = function(definition) {
          definition.testReportSuccess = false;
          definition.testReportErrors = null;

          // if validation returns an error, simply return
          if ($scope.validateReportDefinition(definition) != true)
            return;

          gpService.increment();

          $http(
            {
              url : root_reporting + 'report/test/project/id/' + $scope.focusProject.id
                + '/user/id/' + $scope.currentUser.userName,
              dataType : 'json',
              data : definition,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            gpService.decrement();
            definition.testReportSuccess = true
            definition.testReportError = null;
            // NOTE: Do not handle this as normal http error
            // instead set a local error variable
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            definition.testReportSuccess = false;
            definition.testReportError = data.replace(/"/g, '');

          });

        };

        $scope.updateReportDefinition = function(definition) {

          // if validation returns an error, simply return
          if ($scope.validateReportDefinition(definition) != true)
            return;

          definition.testReportSuccess = null;
          definition.testReportErrors = null;

          gpService.increment();

          $http({
            url : root_reporting + 'definition/update',
            dataType : 'json',
            data : definition,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            removeComponentFromArray(editingPerformed, definition);
          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error updating map reportDefinition.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_reporting + 'definition/definitions',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.reportDefinitions = data.reportDefinition;
              if ($scope.reportDefinition) {
                for (var j = 0; j < $scope.focusProject.reportDefinition.length; j++) {
                  if (reportDefinition.id === $scope.focusProject.reportDefinition[j].id) {
                    $scope.focusProject.reportDefinition[j] = reportDefinition;
                  }
                }
              }
              localStorageService.add('reportDefinitions', data.reportDefinition);
              $rootScope.$broadcast('localStorageModule.notification.setReportDefinitions', {
                key : 'reportDefinitions',
                reportDefinitions : data.reportDefinitions
              });
              $scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            }).then(function() {
              gpService.decrement();
            });

          });
        };

        $scope.submitNewReportDefinition = function(definition) {

          // if validation returns an error, simply return
          if ($scope.validateReportDefinition(definition) != true)
            return;
          gpService.increment();
          $http({
            url : root_reporting + 'definition/add',
            dataType : 'json',
            data : definition,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.newReportAdded = true;

          }).error(function(data, status, headers, config) {
            $scope.newReportAdded = false;
            definition.testReportSuccess = null;
            definition.testReportError = 'Error adding report';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_reporting + 'definition/definitions',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.reportDefinitions = data.reportDefinition;
              $scope.resetReportDefinitionFilter();
              localStorageService.add('reportDefinitions', data.reportDefinition);
              $rootScope.$broadcast('localStorageModule.notification.setReportDefinitions', {
                key : 'reportDefinitions',
                reportDefinitions : data.reportDefinitions
              });
              $scope.allowableReportDefinitions = localStorageService.get('reportDefinitions');
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            }).then(function() {
              gpService.decrement();
            });

          });
        };

        $scope.deleteQaDefinition = function(qaCheckDefinition) {
          if (confirm('Are you sure that you want to delete a map QA Check Definition?') == false)
            return;

          gpService.increment();

          $http({
            url : root_reporting + 'definition/delete',
            dataType : 'json',
            data : qaCheckDefinition,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            gpService.decrement();

          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error deleting map qaCheckDefinition from application.';
            $rootScope.handleHttpError(data, status, headers, config);
            gpService.decrement();

          }).then(function(data) {
            $http({
              url : root_reporting + 'qaCheckDefinition/qaCheckDefinitions',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.qaCheckDefinitions = data.reportDefinition;
              $scope.resetQaDefinitionFilter();
              localStorageService.add('qaCheckDefinitions', data.qaCheckDefinition);
              $rootScope.$broadcast('localStorageModule.notification.setQaDefinitions', {
                key : 'reportDefinitions',
                qaCheckDefinitions : data.qaCheckDefinitions
              });
              $scope.allowableQaDefinitions = localStorageService.get('qaCheckDefinitions');

              // update and broadcast theupdated focus  project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        // function to allow a user to test whether qa check
        // successfully runs before
        // officially adding it
        $scope.testQaDefinition = function(definition) {

          // set the unused fields for qa definition
          definition.diffReport = false;
          definition.timePeriod = null;
          definition.frequency = 'ON_DEMAND';

          // if validation returns an error, simply return
          if ($scope.validateReportDefinition(definition) != true)
            return;

          gpService.increment();

          $http(
            {
              url : root_reporting + 'report/test/project/id/' + $scope.focusProject.id
                + '/user/id/' + $scope.currentUser.userName,
              dataType : 'json',
              data : definition,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            gpService.decrement();
            definition.testQaSuccess = true;
            definition.testQaError = null;

            // NOTE: Do not handle this
            // as normal http error
            // instead set a local error
            // variable
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            definition.testQaSuccess = false;
            definition.testQaError = data.replace(/"/g, '');
          });

        };

        $scope.updateQaDefinition = function(definition) {

          // set the unused fields for qa definition
          definition.diffReport = false;
          definition.timePeriod = null;
          definition.frequency = 'ON_DEMAND';

          // if validation returns an error, simply return
          if ($scope.validateReportDefinition(definition) != true)
            return;

          definition.testQaSuccess = null;
          definition.testQaErrors = null;

          gpService.increment();
          $http({
            url : root_reporting + 'definition/update',
            dataType : 'json',
            data : definition,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            removeComponentFromArray(editingPerformed, definition);

          }).error(function(data, status, headers, config) {
            $scope.recordError = 'Error updating map qaCheckDefinition.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_reporting + 'qaCheckDefinition/qaCheckDefinitions',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(

            function(data) {
              gpService.decrement();
              $scope.qaCheckDefinitions = data.reportDefinition;
              for (var j = 0; j < $scope.focusProject.reportDefinition.length; j++) {
                if (definition.id === $scope.focusProject.reportDefinition[j].id) {
                  $scope.focusProject.reportDefinition[j] = definition;
                }
              }
              localStorageService.add('qaCheckDefinitions', data.reportDefinition);
              $rootScope.$broadcast('localStorageModule.notification.setQaDefinitions', {
                key : 'qaCheckDefinitions',
                qaCheckDefinitions : data.qaCheckDefinitions
              });
              $scope.allowableQaDefinitions = localStorageService.get('qaCheckDefinitions');

              // update
              // and
              // broadcast
              // the
              // updated
              // focus
              // project
              localStorageService.add('focusProject', $scope.focusProject);
              $rootScope.$broadcast('localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });

              $scope.updateMapProject($scope.focusProject);

            }).error(

            function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        $scope.submitNewQaDefinition = function(definition) {
          // set the unused fields for qa definition
          definition.diffReport = false;
          definition.timePeriod = null;
          definition.frequency = 'ON_DEMAND';
          definition.qacheck = true;

          // if validation returns an error, simply return
          if ($scope.validateReportDefinition(definition) != true)
            return;

          $http({
            url : root_reporting + 'definition/add',
            dataType : 'json',
            data : definition,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.newQaReportAdded = true;

          }).error(function(data, status, headers, config) {
            $scope.testQaSuccess = false;
            $scope.testQaError = 'Error adding new map qa check Definition.';
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {
            $http({
              url : root_reporting + 'qaCheckDefinition/qaCheckDefinitions',
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              $scope.qaCheckDefinitions = data.reportDefinition;
              $scope.resetQaDefinitionFilter();
              localStorageService.add('qaCheckDefinitions', data.qaCheckDefinition);
              $rootScope.$broadcast('localStorageModule.notification.setQaDefinitions', {
                key : 'qaCheckDefinitions',
                qaCheckDefinitions : data.qaCheckDefinitions
              });
              $scope.allowableQaDefinitions = localStorageService.get('qaCheckDefinitions');
            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });

          });
        };

        /**
         * Helper function to update map project from the list of existing
         * projects. Required as terminology/version are rendered as single
         * strings instead of individual fields
         */
        $scope.updateMapProjectFromList = function(project) {
          var projectCopy =angular.copy(project);
          // get source and version and dest and version
          var src = project.sourceTerminologyAndVersion.split(' ');
          projectCopy.sourceTerminology = src[0];
          projectCopy.sourceTerminologyVersion = src[1];
          var res = project.destinationTerminologyAndVersion.split(' ');
          projectCopy.destinationTerminology = res[0];
          projectCopy.destinationTerminologyVersion = res[1];
          projectCopy.workflowType = project.workflowType.key;
          projectCopy.mapRelationStyle = project.mapRelationStyle.key;
          projectCopy.mapRefsetPattern = project.mapRefsetPattern.key;

          $scope.updateMapProject(projectCopy);
        };

        /**
         * Function to update a map project via REST call and update the cached
         * projects
         */
        $scope.updateMapProject = function(project) {

          var deferred = $q.defer();

          $http({
            url : root_mapping + 'project/update',
            dataType : 'json',
            data : project,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function() {
   
            // update the cached project list
            for (var i = 0; i < $scope.mapProjects.length; i++) {
              if ($scope.mapProjects[i].id == project.id) {
                $scope.mapProjects[i] = project;
              }
            }
            // update the local cache
            localStorageService.add('mapProjects', $scope.mapProjects);

            deferred.resolve();

          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
            deferred.reject();
          });

          return deferred.promise;
        };

        $scope.deleteMapProject = function(project) {

          if (confirm('ARE YOU ABSOLUTELY SURE?\n\n  Deleting a project is final and cannot be undone.') == false)
            return;
          
          project.workflowType = project.workflowType.key;
          project.mapRelationStyle = project.mapRelationStyle.key;
          project.mapRefsetPattern = project.mapRefsetPattern.key;

          gpService.increment();
          $http({
            url : root_mapping + 'project/delete',
            method : 'DELETE',
            dataType : 'json',
            data : project,
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            gpService.decrement();

            $scope.successMsg = 'Successfully deleted project ' + project.id;

            var mapProjects = [];
            for (var i = 0; i < $scope.mapProjects.length; i++) {
              if ($scope.mapProjects[i].id != project.id) {
                mapProjects.push($scope.mapProjects[i]);
              }
            }
            $scope.mapProjects = mapProjects;

            localStorageService.add('mapProjects', $scope.mapProjects);

            // broadcast change
            $rootScope.$broadcast('localStorageModule.notification.setMapProjects', {
              key : 'mapProjects',
              mapProjects : $scope.mapProjects
            });

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        $scope.submitNewMapProject = function(newMapProjectName, newMapProjectSourceVersion,
          newMapProjectDestinationVersion, newMapProjectRefSetId, newMapProjectModuleId,
          newMapProjectPublished,newMapProjectRuleBased, newMapProjectGroupStructure, newMapProjectPublic,
          newMapProjectScopeDescendantsFlag, newMapProjectScopeExcludedDescendantsFlag,
          newMapProjectMapType, newWorkflowType, newMapRelationStyle, 
          newHandler,newMapProjectPropagationFlag,newMapProjectPropagationThreshold) {

          var errors = '';
          if (newMapProjectSourceVersion == null) {
            errors += 'You must specify a source terminology and version.\n';
          }
          if (newMapProjectDestinationVersion == null) {
            errors += 'You must specify a destination terminology and version.\n';
          }
          if (newMapProjectRefSetId == null) {
            errors += 'You must specify a ref set id.\n';
          }
          if (newMapProjectModuleId == null) {
              errors += 'You must specify a module id.\n';
            }
          if (newMapProjectName == null) {
            errors += 'You must specify a project name.\n';
          }
          if (newMapProjectPropagationFlag == true && newMapProjectPropagationThreshold == null) {
            errors += 'You must specify the propagation threshold for a map project using propagation';
          }

          for (var i = 0; i < $scope.mapProjects.length; i++) {
            if ($scope.mapProjects[i].refSetId === newMapProjectRefSetId)
              errors += 'The refset id specified must be unique, but is used by project '
                + $scope.mapProjects[i].name;
          }

          if (errors.length > 0) {
            alert(errors);
            return;
          }

          for (var i = 0; i < $scope.mapProjects.length; i++) {
              if ($scope.mapProjects[i].moduleId === newMapProjectModuleId){
                  var r = confirm('Module id ' + newMapProjectModuleId + ' is already used by project '
                          + $scope.mapProjects[i].name + '.\nDo you want to proceed?');
                  if (r == true) {
                    break;
                } else {
                return;
                }
              }
            }          
          
          // get source and version and dest and version
          var res = newMapProjectSourceVersion.split(' ');
          var newMapProjectSource = res[0];
          var newMapProjectSourceVersion = res[1];
          res = newMapProjectDestinationVersion.split(' ');
          var newMapProjectDestination = res[0];
          var newMapProjectDestinationVersion = res[1];
          var newMapProjectRefSetName = '';

          // check ref set id

          // get the refsetid name
          $http(
            {
              url : root_content + 'concept/id/' + newMapProjectSource + '/'
                + newMapProjectSourceVersion + '/' + newMapProjectRefSetId,
              dataType : 'json',
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            newMapProjectRefSetName = data.defaultPreferredName;
          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function(data) {

            var project = {
              'name' : newMapProjectName,
              'sourceTerminology' : newMapProjectSource,
              'sourceTerminologyVersion' : newMapProjectSourceVersion,
              'destinationTerminology' : newMapProjectDestination,
              'destinationTerminologyVersion' : newMapProjectDestinationVersion,
              'refSetId' : newMapProjectRefSetId,
              'moduleId' : newMapProjectModuleId,
              'refSetName' : newMapProjectRefSetName,
              'published' : newMapProjectPublished,
              'ruleBased' : newMapProjectRuleBased,
              'groupStructure' : newMapProjectGroupStructure,
              'mapRefsetPattern' : newMapProjectMapType.key,
              'workflowType' : newWorkflowType.key,
              'mapRelationStyle' : newMapRelationStyle.key,
              'public' : newMapProjectPublic,
              'projectSpecificAlgorithmHandlerClass' : newHandler,
              'scopeDescendantsFlag' : newMapProjectScopeDescendantsFlag,
              'scopeExcludedDescendantsFlag' : newMapProjectScopeExcludedDescendantsFlag,
              'mapPrincipleSourceDocumentName' : '',
              'propagatedFlag' : newMapProjectPropagationFlag,
              'propagationDescendantThreshold' : newMapProjectPropagationThreshold
            };

            if ($scope.checkRefSetId(project) == false) {
              alert('The ref set id you provided is not unique.');
              return;
            }

            gpService.increment();

            $http({
              url : root_mapping + 'project/add',
              method : 'PUT',
              dataType : 'json',
              data : project,
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
              gpService.decrement();

              // set
              // the
              // admin
              // project
              // to
              // response
              var newProject = data;

              $scope.successMsg = 'Successfully added project ' + newProject.id;

              $http({
                url : root_mapping + 'project/update',
                dataType : 'json',
                data : newProject,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                // add to local projects and to cache
                $scope.mapProjects.push(newProject);
                localStorageService.add('mapProjects', $scope.mapProjects);

                // broadcast change
                $rootScope.$broadcast('localStorageModule.notification.setMapProjects', {
                  key : 'mapProjects',
                  mapProjects : $scope.mapProjects
                });

              }).error(function(data, status, headers, config) {
                $scope.recordError = 'Error updating map project.';
                $rootScope.handleHttpError(data, status, headers, config);
              });

            }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);
            });
          });
        };

        $scope.checkRefSetId = function(project) {
          for (var i = 0; i < $scope.mapProjects.length; i++) {
            if ($scope.mapProjects[i].id != project.id
              && $scope.mapProjects[i].refSetId === project.refSetId) {
              return false;
            }
          }
          return true;
        };
        
        $scope.checkModuleId = function(project) {
            for (var i = 0; i < $scope.mapProjects.length; i++) {
              if ($scope.mapProjects[i].id != project.id
                && $scope.mapProjects[i].moduleId === project.moduleId) {
                return false;
              }
            }
            return true;
          };

        // Upload file
        $scope.onFileSelect = function($files) {
          // $files: an array of files selected, each file
          // has name, size, and type.
          for (var i = 0; i < $files.length; i++) {
            var $file = $files[i];
            // $upload.upload({
            // url : root_mapping + 'upload/' + $scope.focusProject.id,
            // file : $file,
            // progress : function(e) {
            // // n/a
            // }
            // }).then(function(data, status, headers, config) {
            // // file is uploaded successfully
            // });
          }
        };
        
        $scope.deleteTerminology = function(terminologyVersion) {

          if (confirm('Are you sure that you want to delete ' + terminologyVersion + '?') == false)
            return;
          
          gpService.increment();

          var termVerArray = terminologyVersion.split(' ');
          var terminology = termVerArray[0];
          var version = termVerArray[1];
          
          $http({
            url : root_content + 'terminology/' + terminology + '/' + version,
            method : 'DELETE',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            //Reload terminology metadata
            var promise = reloadTerminologies();
            promise.then(function(data){
              gpService.decrement();
            });
          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.recordError = 'Error deleting terminology from application.';
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };        
    
        $scope.downloadTerminologyGmdn = function() {
          gpService.increment();
         
          // download the latest version of gmdn from SFTP   
          $http({
            url : root_content + 'terminology/download/gmdn',
            method : 'POST',
            }).success(function(data) {
              //Reload downloaded gmdn version metadata
              var promise = getDownloadedGmdnVersions();
              promise.then(function(data){
                gpService.decrement();
              });
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        //hold select list for terminologies and versions.
        $scope.termLoad = {};
        $scope.termLoad.terminology = '';
        $scope.termLoad.version = '';
        $scope.termLoad.scope = '';
        
        $scope.handleTerminologySelection = function(terminology) {
          if (terminology == 'GMDN'){
            getDownloadedGmdnVersions();
            return;
          }

          gpService.increment();

          // download the latest version of gmdn from SFTP
          $http({
            url : root_content + 'terminology/versions/' + terminology,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            $scope.termLoadVersions = new Array();
            $scope.termLoadData = new Array();
            $scope.termLoadVersionFileNameMap = new Map();
            $scope.termLoad.version = ''; //reset
            $scope.termLoad.scope = ''; // reset
            
            for (var i = 0; i < data.TerminologyVersion.length; i++) {
                if ($scope.termLoadVersions.indexOf(data.TerminologyVersion[i].version) < 0) {
                    $scope.termLoadVersions.push(data.TerminologyVersion[i].version);
            	}
                if (terminology != 'SNOMED CT')
                    $scope.termLoadVersionFileNameMap.set(data.TerminologyVersion[i].version, data.TerminologyVersion[i].awsZipFileName)
                else
                    $scope.termLoadData.push(data.TerminologyVersion[i]);

            } 
            
            gpService.decrement();

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        $scope.handleVersionSelection = function(terminology, version) {
          if (version) {
              $scope.termLoadScopeFileNameMap = new Map();
              $scope.termLoad.scope = ''; // reset
              $scope.termLoadScopes = new Array();
              
              if (terminology == 'SNOMED CT') {
                for (var i = 0; i < $scope.termLoadData.length; i++) {
                	if ($scope.termLoadData[i].version == version) {
                		$scope.termLoadScopes.push($scope.termLoadData[i].scope);
                		$scope.termLoadScopeFileNameMap.set($scope.termLoadData[i].scope, $scope.termLoadData[i].awsZipFileName)
                	}
                }
            } else {
              // Not SNOMED.  So simply define the awsZipFileName
              $scope.termLoadAwsZipFileName = $scope.termLoadVersionFileNameMap.get(version);
            }
          }
        }
        
        $scope.handleScopeSelection = function(terminology, version, scope) {
          if (terminology != 'SNOMED CT')
            return;
         
          // Only valid for SNOMED usage of SCOPE.  
          $scope.termLoadAwsZipFileName = $scope.termLoadScopeFileNameMap.get(scope);
        }
        
        // terminology/load/aws/{terminology}
        $scope.loadTerminologyAws = function(terminology, version, scope) {
          gpService.increment();

          var warnings = '';
          for (var i = 0; i < $scope.terminologyVersionPairs.length; i++) {
            console.log("check", warnings);
            var loadVersion = version.replace(' ', '');
            var terminologyVersionPair = $scope.terminologyVersionPairs[i];
            if (terminology != 'SNOMED CT') {
              if(terminologyVersionPair == terminology.replace(' ', '') + ' ' + loadVersion){
                warnings += terminology + ' ' + loadVersion + ' is already loaded in the application.\nWould you like to reload the terminology?';
                break;
              }
            } else {
              // For SNOMED & it's scope
              loadVersion = loadVersion + (scope == 'Alpha' || scope == 'Beta' ? '_' + scope : '');
              if(terminologyVersionPair == terminology.replace(' ', '') + ' ' + loadVersion){
                warnings += terminology + ' ' + loadVersion + ' is already loaded in the application.\nWould you like to reload the terminology?';
                break;
              }
            }
          }
          
          console.log("warnings", warnings);
          
          if (warnings.length > 0  && !confirm(warnings)) {
            gpService.decrement();
            return;
          }
          
          // load the version of gmdn into the application   
          if (isRf2Terminology(terminology)) {
            loadTerminologyAwsRf2Snapshot(terminology, loadVersion, scope);
          }
          reloadTerminologies();
          gpService.decrement();          
        };
        
        
        function isRf2Terminology(terminology) {
          return terminology == 'SNOMED CT' || 
            terminology == 'ICNP Diagnoses' || 
            terminology == 'ICNP Interventions' ||
            terminology == 'ICPC';
        }
        
        // terminology/load/gmdn
        $scope.loadTerminologyGmdn = function(gmdnVersion) {
          gpService.increment();

          var errors = '';
          for (var i = 0; i < $scope.terminologyVersionPairs.length; i++) {
            var terminologyVersionPair = $scope.terminologyVersionPairs[i];
            if(terminologyVersionPair == 'GMDN ' + gmdnVersion){
              errors += 'GMDN ' + gmdnVersion + ' is already loaded in the application.\n';
              break;
            }
          }

          if (errors.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          // load the version of gmdn into the application   
          $http({
            url : root_content + 'terminology/load/gmdn/' + gmdnVersion,
            data : 'GENERATE',
            method : 'PUT',
            headers : {
              'Content-Type' : 'text/plain'
            }
            }).success(function(data) {
              //Reload terminology metadata
              var promise = reloadTerminologies();
              promise.then(function(data){
                gpService.decrement();
              });
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };
        
        
        // load load Map Record Rf2 Complex Map
        $scope.loadMapRecordRf2ComplexMap = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          var queryString = '';
          if (loadTerminology.memberFlag) 
            queryString += "memberFlag=" + loadTerminology.memberFlag;
          
          if (queryString !== '') 
            queryString = "&" + queryString;
          
          if (loadTerminology.recordFlag) 
            queryString += "recordFlag=" + loadTerminology.recordFlag;
          
          if (queryString !== '') 
            queryString = "&" + queryString;
          
          if (loadTerminology.workflowStatus) 
            queryString += "workflowStatus" + loadTerminology.workflowStatus;
          
          if (queryString !== '') 
            queryString = "&" + queryString;
          
          // rest call   
          $http({
            url: root_content + "map/record/rf2/complex" + queryString,
            data: loadTerminology.inputFileOrDirectory,
            method: "PUT",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };
        
        
        // load Map Record Rf2 Simple Map
        $scope.loadMapRecordRf2SimpleMap = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          var queryString = '';
          if (loadTerminology.memberFlag) 
            queryString += "memberFlag=" + loadTerminology.memberFlag;
          
          if (queryString !== '') 
            queryString = "&" + queryString;          
          
          if (loadTerminology.recordFlag) 
            queryString += "recordFlag=" + loadTerminology.recordFlag;
          
          if (queryString !== '') 
            queryString = "&" + queryString;
          
          if (loadTerminology.workflowStatus) 
            queryString += "workflowStatus" + loadTerminology.workflowStatus;
          
          if (queryString !== '') 
            queryString = "&" + queryString;
          
          // rest call   
          $http({
            url: root_content + "map/record/rf2/simple" + queryString,
            data: loadTerminology.inputFileOrDirectory,
            method: "PUT",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };
        
        // load terminology Claml
        $scope.loadTerminologyClaml = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          // rest call   
          $http({
            url: root_content + "terminology/load/claml/" 
              + loadTerminology.terminology + "/" + loadTerminology.version,
            data: loadTerminology.inputFileOrDirectory,
            method: "PUT",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        }; 
        
        
        // remove Map Record
        $scope.removeMapRecord = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          // rest call   
          $http({
            url: root_content + "map/record/" + loadTerminology.refsetId,
            method: "DELETE",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        }; 
        
        
        // remove terminology
        $scope.removeTerminology = function(removeTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          // rest call   
          $http({
            url: root_content + "terminology/" 
              + removeTerminology.terminology + "/" + removeTerminology.version,
            method: "DELETE",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        }; 
        
        // load terminology Rf2 delta
        $scope.loadTerminologyRf2Delta = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          // rest call   
          $http({
            url: root_content + "terminology/load/rf2/delta/" 
              + loadTerminology.terminology + "/" + loadTerminology.lastPublicationDate,
            data: loadTerminology.inputFileOrDirectory,
            method: "PUT",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        }; 
        
        // load terminology Rf2 snapshot
        function loadTerminologyAwsRf2Snapshot(terminology, version, scope) {
          gpService.increment();

          var errors = '';
          for (var i = 0; i < $scope.terminologyVersionPairs.length; i++) {
            var terminologyVersionPair = $scope.terminologyVersionPairs[i];
            if (terminology == 'SNOMED CT') {
              if(terminologyVersionPair == (terminology.replace(' ', '') + ' ' + version.replace(' ', ''))){
                errors += terminology + ' ' + version + ' is already loaded in the application.\n';
                break;
              }
            } else {
              // For SNOMED & it's scope
            }
          }

          console.log("errors", errors);
          
          if (errors.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          var queryString = '?';
          queryString += "awsZipFileName=" + $scope.termLoadAwsZipFileName;
          queryString += "&treePositions=true&sendNotification=true";
          
          // rest call
          $http({
            url: root_content + "terminology/load/aws/rf2/snapshot/" 
              + terminology + "/" + version + "/" + queryString,
            method: "PUT",
            data: null, 
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              gpService.decrement();
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        }; 
        
        
        // load terminology simple
        $scope.loadTerminologySimple = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          // rest call   
          $http({
            url: root_content + "terminology/load/simple/" + terminology + "/" + version,
            data: loadTerminology.inputFileOrDirectory,
            method: "PUT",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };
        
        
        // reload terminology Rf2 snapshot
        $scope.reloadTerminologyRf2Snapshot = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          var queryString = '';
          if (loadTerminology.treePositions) 
            queryString += "treePositions=" + loadTerminology.treePositions;
          
          if (queryString !== '') 
            queryString = "&" + queryString;
          
          if (loadTerminology.sendNotification) 
            queryString += "sendNotification=" + loadTerminology.sendNotification;
          
          if (queryString !== '') 
            queryString = "&" + queryString;

          
          // rest call   
          $http({
            url: root_content + "terminology/reload/rf2/snapshot/" 
              + loadTerminology.terminology + "/" + loadTerminology.version + queryString,
            data: loadTerminology.inputFileOrDirectory,
            method: "PUT",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };
        
        // reload map record
        $scope.reloadMapRecord = function(loadTerminology) {
          gpService.increment();

          var errors = '';

          if (error.length > 0) {
            alert(errors);
            gpService.decrement();
            return;
          }
          
          var queryString = '';
          if (loadTerminology.memberFlag) 
            queryString += "treePositions=" + loadTerminology.treePositions;
          
          if (queryString !== '') 
            queryString = "&" + queryString;
          
          if (loadTerminology.recordFlag) 
            queryString += "sendNotification=" + loadTerminology.sendNotification;
          
          if (queryString !== '') 
            queryString = "&" + queryString;

          
          // rest call   
          $http({
            url: root_content + "map/record/reload/" + refsetId + queryString,
            data: loadTerminology.inputFileOrDirectory,
            method: "PUT",
            headers: { 'Content-Type' : 'text/plain' }
            }).success(function(data) {
              //nothing
            }).error(function(data, status, headers, config) {
            gpService.decrement();          
            $rootScope.handleHttpError(data, status, headers, config);
          });

        }; 
        

} ]);
