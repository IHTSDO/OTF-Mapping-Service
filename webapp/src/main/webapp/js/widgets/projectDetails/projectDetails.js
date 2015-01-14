'use strict';

angular
  .module('mapProjectApp.widgets.projectDetails', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('projectDetails', {
      title : 'Project Details',
      description : 'Displays details for a specific map project.',
      templateUrl : 'js/widgets/projectDetails/projectDetails.html',
      controller : 'projectDetailsCtrl',
      resolve : {
        data : function(projectDetailsService, config) {
          if (!config.terminology) {
            return 'SNOMEDCT';
          }
          return config.terminology;
        }
      },
      edit : {}
    });
  })
  .service('projectDetailsService', function($q, $http) {
    return {
      get : function(terminology) {
        var deferred = $q.defer();
        $http({
          url : root_metadata + "metadata/terminology/id/" + terminology,
          dataType : "json",
          method : "GET",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(function(data) {
          if (data) {
            data.terminology = terminology;
            deferred.resolve(data);
          } else {
            deferred.reject();
          }
        }).error(function(data, status, headers, config) {

          $rootScope.handleHttpError(data, status, headers, config);
          deferred.reject();
        });
        return deferred.promise;
      }
    };
  })
  // filter out users/entities who are already on the selected list
  // since this is used to return list of potential entities to add for
  // selection
  .filter('elementFilter', function() {
    return function(elementList, elementsToCheck) {
      var out = [];
      if (elementList == undefined || elementsToCheck == undefined)
        return out;
      for (var i = 0; i < elementList.length; i++) {
        var found = false;
        for (var j = 0; j < elementsToCheck.length; j++) {
          if (elementList[i].name === elementsToCheck[j].name) {
            found = true;
            break;
          }
        }
        if (found == false)
          out.push(elementList[i]);
      }
      return out;
    };
  })

  .controller(
    'projectDetailsCtrl',
    [
      '$scope',
      '$http',
      '$sce',
      '$rootScope',
      '$location',
      'localStorageService',
      '$upload',
      function($scope, $http, $sce, $rootScope, $location, localStorageService,
        $upload) {

        $scope.page = 'project';

        $scope.currentRole = localStorageService.get('currentRole');
        $scope.currentUser = localStorageService.get('currentUser');
        $scope.focusProject = localStorageService.get('focusProject');
        $scope.mapProjects = localStorageService.get("mapProjects");
        $scope.mapUsers = localStorageService.get('mapUsers');

        $scope.focusProjectBeforeChanges = {};
        $scope.focusProjectBeforeChanges = angular.copy($scope.focusProject);

        $scope.editModeEnabled = false;
        $scope.reportDefinitions = new Array();
        $scope.qaCheckDefinitions = new Array();

        $scope.allowableMapTypes = [ {
          displayName : 'Extended Map',
          name : 'ExtendedMap'
        }, {
          displayName : 'Complex Map',
          name : 'ComplexMap'
        }, {
          displayName : 'Simple Map',
          name : 'SimpleMap'
        } ];
        $scope.allowableMapRelationStyles = [ {
          displayName : 'Map Category Style',
          name : 'MAP_CATEGORY_STYLE'
        }, {
          displayName : 'Relationship Style',
          name : 'RELATIONSHIP_STYLE'
        } ];
        $scope.allowableWorkflowTypes = [ {
          displayName : 'Conflict Project',
          name : 'CONFLICT_PROJECT'
        }, {
          displayName : 'Review Project',
          name : 'REVIEW_PROJECT'
        } ];

        // watch for focus project change
        $scope.$on('localStorageModule.notification.setFocusProject', function(
          event, parameters) {
          console
            .debug("MapProjectDetailCtrl: Detected change in focus project");
          $scope.focusProject = parameters.focusProject;
        });

        $scope.userToken = localStorageService.get('userToken');

        $scope.$watch([ 'focusProject', 'userToken' ], function() {

          if ($scope.focusProject != null && $scope.userToken != null) {
          }
          $http.defaults.headers.common.Authorization = $scope.userToken;
          $scope.go();
        });

        $scope.go = function() {

          console.debug('Formatting project details');

          $http({
            url : root_mapping + "advice/advices",
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
            function(data) {
              $scope.mapAdvices = data.mapAdvice;
              localStorageService.add('mapAdvices', data.mapAdvice);
              $rootScope.$broadcast(
                'localStorageModule.notification.setMapAdvices', {
                  key : 'mapAdvices',
                  mapAdvices : data.mapAdvices
                });
              $scope.allowableMapAdvices = localStorageService
                .get('mapAdvices');
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_mapping + "relation/relations",
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
            function(data) {
              $scope.mapRelations = data.mapRelation;
              localStorageService.add('mapRelations', data.mapRelation);
              $rootScope.$broadcast(
                'localStorageModule.notification.setMapRelations', {
                  key : 'mapRelations',
                  mapRelations : data.mapRelations
                });
              $scope.allowableMapRelations = localStorageService
                .get('mapRelations');
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_mapping + "principle/principles",
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
            function(data) {
              $scope.mapPrinciples = data.mapPrinciple;
              localStorageService.add('mapPrinciples', data.mapPrinciple);
              $rootScope.$broadcast(
                'localStorageModule.notification.setMapPrinciples', {
                  key : 'mapPrinciples',
                  mapPrinciples : data.mapPrinciples
                });
              $scope.allowableMapPrinciples = localStorageService
                .get('mapPrinciples');
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_mapping + "ageRange/ageRanges",
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
            function(data) {
              $scope.mapAgeRanges = data.mapAgeRange;
              localStorageService.add('mapAgeRanges', data.mapAgeRange);
              $rootScope.$broadcast(
                'localStorageModule.notification.setMapAgeRanges', {
                  key : 'mapAgeRanges',
                  mapAgeRanges : data.mapAgeRanges
                });
              $scope.allowableMapAgeRanges = localStorageService
                .get('mapAgeRanges');
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_reporting + "definition/definitions",
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
            function(data) {
              // $scope.reportDefinitions
              // = data.reportDefinition;
              for (var i = 0; i < data.reportDefinition.length; i++) {
                $scope.reportDefinitions.push(data.reportDefinition[i]);
              }
              localStorageService.add('reportDefinitions',
                $scope.reportDefinitions);
              $rootScope.$broadcast(
                'localStorageModule.notification.setMapRelations', {
                  key : 'reportDefinitions',
                  reportDefinitions : $scope.reportDefinitions
                });
              $scope.allowableReportDefinitions = localStorageService
                .get('reportDefinitions');
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          $http({
            url : root_reporting + "qaCheckDefinition/qaCheckDefinitions",
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
            function(data) {
              for (var i = 0; i < data.reportDefinition.length; i++) {
                    $scope.qaCheckDefinitions.push(data.reportDefinition[i]);
              }
              localStorageService.add('qaCheckDefinitions',
                data.reportDefinition);
              $rootScope.$broadcast(
                'localStorageModule.notification.setQACheckDefinitions', {
                  key : 'qaCheckDefinitions',
                  qaCheckDefinitions : $scope.qaCheckDefinitions
                });
              $scope.allowableQACheckDefinitions = localStorageService
                .get('qaCheckDefinitions');
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });

          // find selected elements from the allowable
          // lists
          $scope.selectedMapType = $scope.getSelectedMapType();
          $scope.selectedMapRelationStyle = $scope
            .getSelectedMapRelationStyle();
          $scope.selectedWorkflowType = $scope.getSelectedWorkflowType();

          /*
           * // determine if this project has a principles document if
           * ($scope.focusProject.destinationTerminology == "ICD10") {
           * $scope.focusProject.mapPrincipleDocumentPath = "doc/";
           * $scope.focusProject.mapPrincipleDocument =
           * "ICD10_MappingPersonnelHandbook.docx";
           * $scope.focusProject.mapPrincipleDocumentName = "Mapping Personnel
           * Handbook"; } else { $scope.focusProject.mapPrincipleDocument =
           * null; }
           */

          // set the scope maps
          $scope.scopeMap = {};
          $scope.scopeExcludedMap = {};

          // set pagination variables
          $scope.pageSize = 5;
          $scope.maxSize = 5;
          $scope.getPagedAdvices(1);
          $scope.getPagedRelations(1);
          $scope.getPagedPrinciples(1);
          $scope.getPagedScopeConcepts(1);
          $scope.getPagedScopeExcludedConcepts(1);
          $scope.getPagedReportDefinitions(1);
          // need to initialize selected qa check definitions since they are persisted in the
          // report definition array
          $scope.focusProject.qaCheckDefinition = new Array();
          for (var i = 0; i < $scope.focusProject.reportDefinition.length; i++) {
        	  if ($scope.focusProject.reportDefinition[i].qacheck == true)
        		  $scope.focusProject.qaCheckDefinition.push($scope.focusProject.reportDefinition[i]);
          }
          $scope.getPagedQACheckDefinitions(1);
          $scope.orderProp = 'id';
        };

        $scope.goMapRecords = function() {
          console.debug($scope.role);

          var path = "/project/records";
          // redirect page
          $location.path(path);
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

        $scope.getPagedAdvices = function(page, filter) {
          console.debug('getPagedAdvices', filter);
          $scope.adviceFilter = filter;
          $scope.pagedAdvice = $scope.sortByKey($scope.focusProject.mapAdvice,
            'id').filter(containsAdviceFilter);
          $scope.pagedAdviceCount = $scope.pagedAdvice.length;
          $scope.pagedAdvice = $scope.pagedAdvice.slice((page - 1)
            * $scope.pageSize, page * $scope.pageSize);
        };

        $scope.getPagedRelations = function(page, filter) {
          $scope.relationFilter = filter;
          $scope.pagedRelation = $scope.sortByKey(
            $scope.focusProject.mapRelation, 'id').filter(
            containsRelationFilter);
          $scope.pagedRelationCount = $scope.pagedRelation.length;
          $scope.pagedRelation = $scope.pagedRelation.slice((page - 1)
            * $scope.pageSize, page * $scope.pageSize);
        };

        $scope.getPagedPrinciples = function(page, filter) {
          $scope.principleFilter = filter;
          $scope.pagedPrinciple = $scope.sortByKey(
            $scope.focusProject.mapPrinciple, 'id').filter(
            containsPrincipleFilter);
          $scope.pagedPrincipleCount = $scope.pagedPrinciple.length;
          $scope.pagedPrinciple = $scope.pagedPrinciple.slice((page - 1)
            * $scope.pageSize, page * $scope.pageSize);

          console.debug($scope.pagedPrinciple);
        };

        $scope.getPagedReportDefinitions = function(page, filter) {
          $scope.reportDefinitionFilter = filter;
          $scope.pagedReportDefinition = $scope.sortByKey(
            $scope.focusProject.reportDefinition, 'id').filter(
            containsReportDefinitionFilter);
          $scope.pagedReportDefinitionCount = $scope.pagedReportDefinition.length;
          $scope.pagedReportDefinition = $scope.pagedReportDefinition.slice(
            (page - 1) * $scope.pageSize, page * $scope.pageSize);

          console.debug("pagedReportDefinition", $scope.pagedReportDefinition);
        };
        
        $scope.getPagedQACheckDefinitions = function(page, filter) {
            $scope.qaCheckDefinitionFilter = filter;
            $scope.pagedQACheckDefinition = $scope.sortByKey(
            		$scope.focusProject.qaCheckDefinition, 'id').filter(
              containsQACheckDefinitionFilter);
            $scope.pagedQACheckDefinitionCount = $scope.pagedQACheckDefinition.length;
            $scope.pagedQACheckDefinition = $scope.pagedQACheckDefinition.slice(
              (page - 1) * $scope.pageSize, page * $scope.pageSize);

            console.debug("pagedQACheckDefinition", $scope.pagedQACheckDefinition);
          };

        $scope.getPagedScopeConcepts = function(page) {
          console.debug("Called paged scope concept for page " + page);

          // construct a paging/filtering/sorting object
          var pfsParameterObj = {
            "startIndex" : page == -1 ? -1 : (page - 1) * $scope.pageSize,
            "maxResults" : page == -1 ? -1 : $scope.pageSize,
            "sortField" : '',
            "queryRestriction" : ''
          };

          $rootScope.glassPane++;

          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeConcepts",
              dataType : "json",
              data : pfsParameterObj,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.pagedScopeConcept = data.searchResult;
            $scope.pagedScopeConceptCount = data.totalCount;
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        $scope.getPagedScopeExcludedConcepts = function(page, filter) {
          console.debug("Called paged scope concept for page " + page);

          // construct a paging/filtering/sorting object
          var pfsParameterObj = {
            "startIndex" : page == -1 ? -1 : (page - 1) * $scope.pageSize,
            "maxResults" : page == -1 ? -1 : $scope.pageSize,
            "sortField" : '',
            "queryRestriction" : ''
          };

          $rootScope.glassPane++;

          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeExcludedConcepts",
              dataType : "json",
              data : pfsParameterObj,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.pagedScopeExcludedConcept = data.searchResult;
            $scope.pagedScopeExcludedConceptCount = data.totalCount;
          }).error(function(data, status, headers, config) {
            $rootScoped.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // functions to reset the filter and retrieve
        // unfiltered results

        $scope.resetAdviceFilter = function() {
          $scope.adviceFilter = "";
          $scope.getPagedAdvices(1);
        };

        $scope.resetRelationFilter = function() {
          $scope.relationFilter = "";
          $scope.getPagedRelations(1);
        };

        $scope.resetPrincipleFilter = function() {
          $scope.principleFilter = "";
          $scope.getPagedPrinciples(1);
        };

        $scope.resetScopeConceptFilter = function() {
          $scope.scopeConceptFilter = "";
          $scope.getPagedScopeConcepts(1);
        };

        $scope.resetReportDefinitionFilter = function() {
          $scope.reportDefinitionFilter = "";
          $scope.getPagedReportDefinitions(1);
        };
        
        $scope.resetQACheckDefinitionFilter = function() {
            $scope.qaCheckDefinitionFilter = "";
            $scope.getPagedQACheckDefinitions(1);
          };

        $scope.resetScopeExcludedConceptFilter = function() {
          $scope.scopeExcludedConceptFilter = "";
          $scope.getPagedScopeExcludedConcepts(1);
        };

        // element-specific functions for filtering
        // don't want to search id or objectId

        function containsAdviceFilter(element) {

          console.debug("Checking advice: ", $scope.adviceFilter);

          // check if advice filter is empty
          if ($scope.adviceFilter === "" || $scope.adviceFilter == null)
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
        ;

        function containsRelationFilter(element) {

          console.debug("Checking relation: ", $scope.relationFilter);

          // check if relation filter is empty
          if ($scope.relationFilter === "" || $scope.relationFilter == null)
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
        ;

        function containsPrincipleFilter(element) {

          // check if principle filter is empty
          if ($scope.principleFilter === "" || $scope.principleFilter == null)
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
        ;

        function containsScopeConceptFilter(element) {

          // check if scopeConcept filter is empty
          if ($scope.scopeConceptFilter === ""
            || $scope.scopeConceptFilter == null)
            return true;

          // otherwise check if upper-case scopeConcept
          // filter matches upper-case element name or
          // detail
          if (element.scopeConceptId.toString().toUpperCase().indexOf(
            $scope.scopeConceptFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.name.toString().toUpperCase().indexOf(
            $scope.scopeConceptFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function containsScopeExcludedConceptFilter(element) {

          // check if scopeConcept filter is empty
          if ($scope.scopeExcludesConceptFilter === ""
            || $scope.scopeExcludesConceptFilter == null)
            return true;

          // otherwise check if upper-case scopeConcept
          // filter matches upper-case element name or
          // detail
          if (element.scopeExcludesConceptId.toString().toUpperCase().indexOf(
            $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1)
            return true;
          if (element.name.toString().toUpperCase().indexOf(
            $scope.scopeExcludesConceptFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }

        function containsReportDefinitionFilter(element) {

          console.debug("Checking report definition: ",
            $scope.reportDefinitionFilter);

          // check if reportDefinition filter is empty
          if ($scope.reportDefinitionFilter === ""
            || $scope.reportDefinitionFilter == null)
            return true;

          // otherwise check if upper-case
          // reportDefinition filter
          // matches upper-case element name or detail
          if (element.name.toString().toUpperCase().indexOf(
            $scope.reportDefinitionFilter.toString().toUpperCase()) != -1)
            return true;

          // otherwise return false
          return false;
        }
        ;
        
        function containsQACheckDefinitionFilter(element) {

            console.debug("Checking qa check definition: ",
              $scope.qaCheckDefinitionFilter);

            // check if qaCheckDefinition filter is empty
            if ($scope.qaCheckDefinitionFilter === ""
              || $scope.qaCheckDefinitionFilter == null)
              return true;

            // otherwise check if upper-case
            // qaCheckDefinition filter
            // matches upper-case element name or detail
            if (element.name.toString().toUpperCase().indexOf(
              $scope.qaCheckDefinitionFilter.toString().toUpperCase()) != -1)
              return true;

            // otherwise return false
            return false;
          }
          ;

        // helper function to sort a JSON array by field

        $scope.sortByKey = function sortById(array, key) {
          return array.sort(function(a, b) {
            var x = a[key];
            var y = b[key];
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
          });
        };

        // function to change project from the header
        $scope.changeFocusProject = function(mapProject) {
          $scope.focusProject = mapProject;
          console.debug("changing project to " + $scope.focusProject.name);

          // update and broadcast the new focus project
          localStorageService.add('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });

          // update the user preferences
          $scope.preferences.lastMapProjectId = $scope.focusProject.id;
          localStorageService.add('preferences', $scope.preferences);
          $rootScope.$broadcast(
            'localStorageModule.notification.setUserPreferences', {
              key : 'userPreferences',
              userPreferences : $scope.preferences
            });

        };

        $scope.goToHelp = function() {
          var path;
          if ($scope.page != 'mainDashboard') {
            path = "help/" + $scope.page + "Help.html";
          } else {
            path = "help/" + $scope.currentRole + "DashboardHelp.html";
          }
          console.debug("go to help page " + path);
          // redirect page
          $location.path(path);
        };

        $scope.isEmailViewable = function(email) {
          if (email.indexOf("ihtsdo.org") > -1) {
            return true;
          } else {
            return false;
          }
        };

        $scope.toggleEditMode = function() {
          if ($scope.editModeEnabled == true) {
            $scope.editModeEnabled = false;
            $scope.updateMapProject();
          } else {
            $scope.editModeEnabled = true;
          }
        };

        $scope.getSelectedMapRelationStyle = function() {
          console.debug("in getSelectedMapRelationStyle");
          for (var j = 0; j < $scope.allowableMapRelationStyles.length; j++) {
            if ($scope.focusProject.mapRelationStyle === $scope.allowableMapRelationStyles[j].name)
              return $scope.allowableMapRelationStyles[j];
          }
          return null;
        };

        $scope.selectMapRelationStyle = function() {
          // update and broadcast the updated focus
          // project
          $scope.focusProject.mapRelationStyle = $scope.selectedMapRelationStyle.name;
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.updateMapProject();
        };

        $scope.getSelectedMapType = function() {
          console.debug("in getSelectedMapType");
          for (var j = 0; j < $scope.allowableMapTypes.length; j++) {
            if ($scope.focusProject.mapRefsetPattern === $scope.allowableMapTypes[j].name)
              return $scope.allowableMapTypes[j];
          }
          return null;
        };

        $scope.selectMapType = function() {
          $scope.focusProject.mapType = $scope.selectedMapType.name;
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.updateMapProject();
        };

        $scope.getSelectedWorkflowType = function() {
          console.debug("in getSelectedWorkflowType");
          for (var j = 0; j < $scope.allowableWorkflowTypes.length; j++) {
            if ($scope.focusProject.workflowType === $scope.allowableWorkflowTypes[j].name)
              return $scope.allowableWorkflowTypes[j];
          }
          return null;

        };

        $scope.selectWorkflowType = function() {
          $scope.focusProject.workflowType = $scope.selectedWorkflowType.name;
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.updateMapProject();
        };
        
        $scope.deleteAdministrator = function(administrator) {
          console.debug("in deleteAdministrator");
          for (var j = 0; j < $scope.focusProject.mapAdministrator.length; j++) {
            if (administrator.userName === $scope.focusProject.mapAdministrator[j].userName) {
              $scope.focusProject.mapAdministrator.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
        };
        
        $scope.addAdministrator = function(user) {
          console.debug("in addAdministrator");
          for (var i = 0; i < $scope.focusProject.mapLead.length; i++) {
            if ($scope.focusProject.mapLead[i].name == user.name) {
              confirm("User "
                + user.name
                + " is already a Map Lead.\nUser cannot have more than one role.");
              return;
            }
          }
          for (var i = 0; i < $scope.focusProject.mapSpecialist.length; i++) {
            if ($scope.focusProject.mapSpecialist[i].name == user.name) {
              confirm("User "
                + user.name
                + " is already a Map Specialist.\nUser cannot have more than one role.");
              return;
            }
          }
          $scope.focusProject.mapAdministrator.push(user);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
        };

        $scope.deleteLead = function(lead) {
          console.debug("in deleteLead");
          for (var j = 0; j < $scope.focusProject.mapLead.length; j++) {
            if (lead.userName === $scope.focusProject.mapLead[j].userName) {
              $scope.focusProject.mapLead.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
        };

        $scope.addLead = function(user) {
          console.debug("in addLead");
          for (var i = 0; i < $scope.focusProject.mapAdministrator.length; i++) {
            if ($scope.focusProject.mapAdministrator[i].name == user.name) {
              confirm("User "
                + user.name
                + " is already a Map Administrator.\nUser cannot have more than one role.");
              return;
            }
          }
          for (var i = 0; i < $scope.focusProject.mapSpecialist.length; i++) {
            if ($scope.focusProject.mapSpecialist[i].name == user.name) {
              confirm("User "
                + user.name
                + " is already a Map Specialist.\nUser cannot have more than one role.");
              return;
            }
          }
          $scope.focusProject.mapLead.push(user);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
        };

        $scope.deleteSpecialist = function(specialist) {
          console.debug("in deleteSpecialist");
          for (var j = 0; j < $scope.focusProject.mapSpecialist.length; j++) {
            if (specialist.userName === $scope.focusProject.mapSpecialist[j].userName) {
              $scope.focusProject.mapSpecialist.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
        };

        $scope.addSpecialist = function(user) {
          console.debug("in addSpecialist");
          for (var i = 0; i < $scope.focusProject.mapLead.length; i++) {
            if ($scope.focusProject.mapLead[i].name == user.name) {
              confirm("User "
                + user.name
                + " is already a Map Lead.\nUser cannot have more than one role.");
              return;
            }
          }
          for (var i = 0; i < $scope.focusProject.mapAdministrator.length; i++) {
            if ($scope.focusProject.mapAdministrator[i].name == user.name) {
              confirm("User "
                + user.name
                + " is already a Map Administrator.\nUser cannot have more than one role.");
              return;
            }
          }
          $scope.focusProject.mapSpecialist.push(user);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
        };

        $scope.deleteAdvice = function(advice) {
          console.debug("in deleteAdvice");
          for (var j = 0; j < $scope.focusProject.mapAdvice.length; j++) {
            if (advice.name === $scope.focusProject.mapAdvice[j].name) {
              $scope.focusProject.mapAdvice.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.pageAdvice = 1;
          $scope.resetAdviceFilter();
          $scope.updateMapProject();
        };

        $scope.addAdvice = function(advice) {
          console.debug("in addAdvice");
          $scope.focusProject.mapAdvice.push(advice);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.resetAdviceFilter();
          $scope.updateMapProject();
        };

        $scope.updateAdvice = function(advice) {
          console.debug("in updateAdvice");
          $http({
            url : root_mapping + "advice/update",
            dataType : "json",
            data : advice,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          })
            .success(function(data) {
              console.debug("success to updateMapAdvice");
            })
            .error(function(data, status, headers, config) {
              $scope.recordError = "Error updating map advice.";
              $rootScope.handleHttpError(data, status, headers, config);
            })
            .then(
              function(data) {
                $http({
                  url : root_mapping + "advice/advices",
                  dataType : "json",
                  method : "GET",
                  headers : {
                    "Content-Type" : "application/json"
                  }
                })
                  .success(
                    function(data) {
                      $scope.mapAdvices = data.mapAdvice;
                      for (var j = 0; j < $scope.focusProject.mapAdvice.length; j++) {
                        if (advice.id === $scope.focusProject.mapAdvice[j].id) {
                          $scope.focusProject.mapAdvice[j] = advice;
                        }
                      }
                      localStorageService.add('mapAdvices', data.mapAdvice);
                      $rootScope.$broadcast(
                        'localStorageModule.notification.setMapAdvices', {
                          key : 'mapAdvices',
                          mapAdvices : data.mapAdvices
                        });
                      $scope.allowableMapAdvices = localStorageService
                        .get('mapAdvices');

                      // update
                      // and
                      // broadcast
                      // the
                      // updated
                      // focus
                      // project
                      localStorageService.add('focusProject',
                        $scope.focusProject);
                      $rootScope.$broadcast(
                        'localStorageModule.notification.setFocusProject', {
                          key : 'focusProject',
                          focusProject : $scope.focusProject
                        });

                      $scope.updateMapProject();

                    }).error(function(data, status, headers, config) {
                    $rootScope.handleHttpError(data, status, headers, config);
                  });

              });
        };

        $scope.submitNewMapAdvice = function(mapAdviceName, mapAdviceDetail,
          allowableForNullTarget, isComputed) {
          console.debug("in submitNewMapAdvice");
          var obj = {
            "name" : mapAdviceName,
            "detail" : mapAdviceDetail,
            "isAllowableForNullTarget" : allowableForNullTarget,
            "isComputed" : isComputed
          };

          $http({
            url : root_mapping + "advice/add",
            dataType : "json",
            data : obj,
            method : "PUT",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
            console.debug("success to addMapAdvice");
          }).error(function(data, status, headers, config) {
            $scope.recordError = "Error adding new map advice.";
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(
            function(data) {
              $http({
                url : root_mapping + "advice/advices",
                dataType : "json",
                method : "GET",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(
                function(data) {
                  $scope.mapAdvices = data.mapAdvice;
                  $scope.resetAdviceFilter();
                  localStorageService.add('mapAdvices', data.mapAdvice);
                  $rootScope.$broadcast(
                    'localStorageModule.notification.setMapAdvices', {
                      key : 'mapAdvices',
                      mapAdvices : data.mapAdvices
                    });
                  $scope.allowableMapAdvices = localStorageService
                    .get('mapAdvices');
                }).error(function(data, status, headers, config) {
                $rootScope.handleHttpError(data, status, headers, config);
              });

            });
        };

        $scope.deleteRelation = function(relation) {
          console.debug("in deleteRelation");
          for (var j = 0; j < $scope.focusProject.mapRelation.length; j++) {
            if (relation.name === $scope.focusProject.mapRelation[j].name) {
              $scope.focusProject.mapRelation.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.resetRelationFilter();
          $scope.updateMapProject();
        };

        $scope.addRelation = function(relation) {
          console.debug("in addRelation");
          $scope.focusProject.mapRelation.push(relation);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.resetRelationFilter();
          $scope.updateMapProject();
        };

        $scope.submitNewMapRelation = function(mapRelationName,
          mapRelationAbbreviation, mapRelationTerminologyId,
          allowableForNullTarget, isComputed) {
          console.debug("in submitNewMapRelation for application");
          var obj = {
            "terminologyId" : mapRelationTerminologyId,
            "name" : mapRelationName,
            "abbreviation" : mapRelationAbbreviation,
            "isAllowableForNullTarget" : allowableForNullTarget,
            "isComputed" : isComputed
          };
          $http({
            url : root_mapping + "relation/add",
            dataType : "json",
            data : obj,
            method : "PUT",
            headers : {
              "Content-Type" : "application/json"
            }
          })
            .success(function(data) {
              console.debug("success to addMapRelation to application");
            })
            .error(
              function(data, status, headers, config) {
                $scope.recordError = "Error adding new map relation for the application.";
                $rootScope.handleHttpError(data, status, headers, config);
              }).then(
              function(data) {
                $http({
                  url : root_mapping + "relation/relations",
                  dataType : "json",
                  method : "GET",
                  headers : {
                    "Content-Type" : "application/json"
                  }
                }).success(
                  function(data) {
                    $scope.mapRelations = data.mapRelation;
                    $scope.resetRelationFilter();
                    localStorageService.add('mapRelations', data.mapRelation);
                    $rootScope.$broadcast(
                      'localStorageModule.notification.setMapRelations', {
                        key : 'mapRelations',
                        mapRelations : data.mapRelations
                      });
                    $scope.allowableMapRelations = localStorageService
                      .get('mapRelations');
                  }).error(function(data, status, headers, config) {
                  $rootScope.handleHttpError(data, status, headers, config);
                });

              });
        };

        $scope.deleteReportDefinition = function(reportDefinition) {
          console.debug("in deleteReportDefinition");
          for (var j = 0; j < $scope.focusProject.reportDefinition.length; j++) {
            if (reportDefinition.name === $scope.focusProject.reportDefinition[j].name) {
              $scope.focusProject.reportDefinition.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.resetReportDefinitionFilter();
          $scope.updateMapProject();
        };
        
        $scope.deleteQACheckDefinition = function(qaCheckDefinition) {
            console.debug("in deleteQACheckDefinition");
            for (var j = 0; j < $scope.focusProject.qaCheckDefinition.length; j++) {
              if (qaCheckDefinition.name === $scope.focusProject.qaCheckDefinition[j].name) {
                $scope.focusProject.qaCheckDefinition.splice(j, 1);
              }
            }
            // update and broadcast the updated focus
            // project
            localStorageService.set('focusProject', $scope.focusProject);
            $rootScope.$broadcast(
              'localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });
            $scope.resetQACheckDefinitionFilter();
            $scope.updateMapProject();
          };

        $scope.addReportDefinition = function(reportDefinition) {
          console.debug("in addReportDefinition", reportDefinition);
          $scope.focusProject.reportDefinition.push(reportDefinition);
          console.debug($scope.focusProject.reportDefinition);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.resetReportDefinitionFilter();
          $scope.updateMapProject();
        };
        
        $scope.addQACheckDefinition = function(qaCheckDefinition) {
            console.debug("in addQACheckDefinition", qaCheckDefinition);
            $scope.focusProject.qaCheckDefinition.push(qaCheckDefinition);
            console.debug($scope.focusProject.qaCheckDefinition);
            // update and broadcast the updated focus
            // project
            localStorageService.set('focusProject', $scope.focusProject);
            $rootScope.$broadcast(
              'localStorageModule.notification.setFocusProject', {
                key : 'focusProject',
                focusProject : $scope.focusProject
              });
            $scope.resetQACheckDefinitionFilter();
            $scope.updateMapProject();
          };

        $scope.deletePrinciple = function(principle) {
          console.debug("in deletePrinciple");
          for (var j = 0; j < $scope.focusProject.mapPrinciple.length; j++) {
            if (principle.name === $scope.focusProject.mapPrinciple[j].name) {
              $scope.focusProject.mapPrinciple.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.resetPrincipleFilter();
          $scope.updateMapProject();
        };

        $scope.addPrinciple = function(principle) {
          console.debug("in addPrinciple");
          $scope.focusProject.mapPrinciple.push(principle);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.resetPrincipleFilter();
          $scope.updateMapProject();
        };

        $scope.updatePrinciple = function(principle) {
          console.debug("in  updatePrinciple");
          $http({
            url : root_mapping + "principle/update",
            dataType : "json",
            data : principle,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          })
            .success(function(data) {
              console.debug("success to updateMapPrinciple");
            })
            .error(function(data, status, headers, config) {
              $scope.recordError = "Error updating map principle.";
              $rootScope.handleHttpError(data, status, headers, config);
            })
            .then(
              function(data) {
                $http({
                  url : root_mapping + "principle/principles",
                  dataType : "json",
                  method : "GET",
                  headers : {
                    "Content-Type" : "application/json"
                  }
                })
                  .success(
                    function(data) {

                      $scope.mapPrinciples = data.mapPrinciple;
                      for (var j = 0; j < $scope.focusProject.mapPrinciple.length; j++) {
                        if (principle.id === $scope.focusProject.mapPrinciple[j].id) {
                          $scope.focusProject.mapPrinciple[j] = principle;
                        }
                      }
                      localStorageService.add('mapPrinciples',
                        data.mapPrinciple);
                      $rootScope.$broadcast(
                        'localStorageModule.notification.setMapPrinciples', {
                          key : 'mapPrinciples',
                          mapPrinciples : data.mapPrinciples
                        });
                      $scope.allowableMapPrinciples = localStorageService
                        .get('mapPrinciples');

                      // update
                      // and
                      // broadcast
                      // the
                      // updated
                      // focus
                      // project
                      localStorageService.add('focusProject',
                        $scope.focusProject);
                      $rootScope.$broadcast(
                        'localStorageModule.notification.setFocusProject', {
                          key : 'focusProject',
                          focusProject : $scope.focusProject
                        });

                      $scope.updateMapProject();
                    }).error(function(data, status, headers, config) {
                    $rootScope.handleHttpError(data, status, headers, config);
                  });

              });
        };

        $scope.submitNewMapPrinciple = function(mapPrincipleName,
          mapPrincipleId, mapPrincipleDetail, mapPrincipleSectionRef) {
          console.debug("in submitNewMapPrinciple");
          var obj = {
            "name" : mapPrincipleName,
            "principleId" : mapPrincipleId,
            "detail" : mapPrincipleDetail,
            "sectionRef" : mapPrincipleSectionRef
          };
          $http({
            url : root_mapping + "principle/add",
            dataType : "json",
            data : obj,
            method : "PUT",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
            console.debug("success to addMapPrinciple");
          }).error(function(data, status, headers, config) {
            $scope.recordError = "Error adding new map principle.";
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(
            function(data) {
              $http({
                url : root_mapping + "principle/principles",
                dataType : "json",
                method : "GET",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(
                function(data) {
                  $scope.mapPrinciples = data.mapPrinciple;
                  localStorageService.add('mapPrinciples', data.mapPrinciple);
                  $rootScope.$broadcast(
                    'localStorageModule.notification.setMapPrinciples', {
                      key : 'mapPrinciples',
                      mapPrinciples : data.mapPrinciples
                    });
                  $scope.allowableMapPrinciples = localStorageService
                    .get('mapPrinciples');
                }).error(function(data, status, headers, config) {
                $rootScope.handleHttpError(data, status, headers, config);
              });

            });
        };

        $scope.deleteAgeRange = function(ageRange) {
          console.debug("in deleteAgeRange");
          for (var j = 0; j < $scope.focusProject.mapAgeRange.length; j++) {
            if (ageRange.name === $scope.focusProject.mapAgeRange[j].name) {
              $scope.focusProject.mapAgeRange.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.updateMapProject();
        };

        $scope.addAgeRange = function(ageRange) {
          console.debug("in addAgeRange");
          $scope.focusProject.mapAgeRange.push(ageRange);
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.updateMapProject();
        };

        $scope.submitNewMapAgeRange = function(name, lowerInclusive,
          lowerUnits, lowerValue, upperInclusive, upperUnits, upperValue) {
          console.debug("in submitNewMapAgeRange");
          var obj = {
            "lowerInclusive" : true,
            "lowerUnits" : lowerUnits,
            "lowerValue" : lowerValue,
            "name" : name,
            "upperInclusive" : true,
            "upperUnits" : upperUnits,
            "upperValue" : upperValue
          };
          $http({
            url : root_mapping + "ageRange/add",
            dataType : "json",
            data : obj,
            method : "PUT",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
            console.debug("success to addMapAgeRange");
          }).error(function(data, status, headers, config) {
            $scope.recordError = "Error adding new map age range.";
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(
            function(data) {
              $http({
                url : root_mapping + "ageRange/ageRanges",
                dataType : "json",
                method : "GET",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(
                function(data) {
                  $scope.mapAgeRanges = data.mapAgeRange;
                  localStorageService.add('mapAgeRanges', data.mapAgeRange);
                  $rootScope.$broadcast(
                    'localStorageModule.notification.setMapAgeRanges', {
                      key : 'mapAgeRanges',
                      mapAgeRanges : data.mapAgeRanges
                    });
                  $scope.allowableMapAgeRanges = localStorageService
                    .get('mapAgeRanges');
                }).error(function(data, status, headers, config) {
                $rootScope.handleHttpError(data, status, headers, config);
              });

            });
        };

        $scope.submitNewErrorMessage = function(message) {
          var localErrorMessages = $scope.focusProject.errorMessages;
          localErrorMessages.push(message);
          $scope.focusProject.errorMessages = localErrorMessages;
          $scope.updateMapProject();
        };

        $scope.deleteErrorMessage = function(message) {
          console.debug("in deleteErrorMessage ");
          for (var j = 0; j < $scope.focusProject.errorMessages.length; j++) {
            if (message === $scope.focusProject.errorMessages[j]) {
              $scope.focusProject.errorMessages.splice(j, 1);
            }
          }
          // update and broadcast the updated focus
          // project
          localStorageService.set('focusProject', $scope.focusProject);
          $rootScope.$broadcast(
            'localStorageModule.notification.setFocusProject', {
              key : 'focusProject',
              focusProject : $scope.focusProject
            });
          $scope.updateMapProject();
        };
        
        //////////////////////////////////////////////
        // Scope Include Concept Addition/Removal
        //////////////////////////////////////////////
        
        // remove a single concept (using the [x] button)
        $scope.removeScopeIncludedConcept = function(scopeConcept, currentPage) {
          // TODO: recalculate workflow
          console.debug("in removeScopeIncludedConcept");
          $rootScope.glassPane++;

          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeConcepts/remove",
              dataType : "json",
              data : scopeConcept.terminologyId,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.getPagedScopeConcepts(currentPage);
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        };
        
        // remove a single/batch of excluded concepts
        $scope.removeScopeIncludedConcepts = function(scopeConceptsUnsplit) {
          console.debug("in removeScopeIncludedConcepts");
          $rootScope.glassPane++;

          var scopeConcepts = scopeConceptsUnsplit.split(/,\s*|\s+/);
          
          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeConcepts/remove",
              dataType : "json",
              data : scopeConcepts,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.getPagedScopeConcepts(1);
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // submit a single/batch of concepts for addition
        $scope.submitNewScopeIncludedConcepts = function(scopeConceptsUnsplit) {
          console.debug("in submitNewScopeIncludedConcept");

          $rootScope.glassPane++;
          
          var scopeConcepts = scopeConceptsUnsplit.split(/,\s*|\s+/);

          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeConcepts/add",
              dataType : "json",
              data : scopeConcepts,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.getPagedScopeConcepts(1);
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });

        };
        
        //////////////////////////////////////////////
        // Scope Exclude Concept Addition/Removal
        //////////////////////////////////////////////

     // remove a single concept (using the [x] button)
        $scope.removeScopeExcludedConcept = function(scopeExcludedConcept, currentPage) {
          // TODO: recalculate workflow
          console.debug("in removeScopeExcludedConcept");
          $rootScope.glassPane++;

          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeExcludedConcepts/remove",
              dataType : "json",
              data : scopeExcludedConcept.terminologyId,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.getPagedScopeExcludedConcepts(currentPage);
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        };
        
        // remove a single/batch of excluded concepts
        $scope.removeScopeExcludedConcepts = function(scopeExcludedConceptsUnsplit) {
          console.debug("in removeScopeExcludedConcepts");
          $rootScope.glassPane++;

          var scopeExcludedConcepts = scopeExcludedConceptsUnsplit.split(/,\s*|\s+/);
          
          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeExcludedConcepts/remove",
              dataType : "json",
              data : scopeExcludedConcepts,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.getPagedScopeExcludedConcepts(1);
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        // submit a single/batch of concepts for addition
        $scope.submitNewScopeExcludedConcepts = function(scopeExcludedConceptsUnsplit) {
          console.debug("in submitNewScopeExcludedConcept");

          $rootScope.glassPane++;
          
          var scopeExcludedConcepts = scopeExcludedConceptsUnsplit.split(/,\s*|\s+/);

          $http(
            {
              url : root_mapping + "project/id/" + $scope.focusProject.id
                + "/scopeExcludedConcepts/add",
              dataType : "json",
              data : scopeExcludedConcepts,
              method : "POST",
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(function(data) {
            $rootScope.glassPane--;
            $scope.getPagedScopeExcludedConcepts(1);
          }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });

        };
        
        ///////////////////////////////////////
        // Model reset, clears all filters
        ///////////////////////////////////////

        $scope.resetModel = function() {
          console.debug("in resetModel");
          angular.copy($scope.focusProjectBeforeChanges, $scope.focusProject);

          $scope.resetAdviceFilter();
          $scope.resetRelationFilter();
          $scope.resetPrincipleFilter();
          $scope.resetScopeConceptFilter();
          $scope.resetScopeExcludedConceptFilter();
          $scope.resetReportDefinitionFilter();
        };

        $scope.updateMapProject = function() {
        	
          for (var k = 0; k < $scope.focusProject.reportDefinition.length; k++) {
          	  if ($scope.focusProject.reportDefinition[k].qacheck == true) {
                    $scope.focusProject.reportDefinition.splice(k, 1);
          	  }
          }
          for (var j = 0; j < $scope.focusProject.qaCheckDefinition.length; j++) {
          	  $scope.focusProject.reportDefinition.push($scope.focusProject.qaCheckDefinition[j]);
          }
        	
          $http({
            url : root_mapping + "project/update",
            dataType : "json",
            data : $scope.focusProject,
            method : "POST",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
            function(data) {
              console.debug("success to updateMapProject");
              
              localStorageService.set('focusProject', $scope.focusProject);
              $rootScope.$broadcast(
                'localStorageModule.notification.setFocusProject', {
                  key : 'focusProject',
                  focusProject : $scope.focusProject
                });
            }).error(function(data, status, headers, config) {
            $scope.recordError = "Error updating map project.";
            $rootScope.handleHttpError(data, status, headers, config);
          });
        };

        $scope.onFileSelect = function($files) {
          // $files: an array of files selected, each file
          // has name, size, and type.
          for (var i = 0; i < $files.length; i++) {
            var $file = $files[i];
            $rootScope.glassPane++;
            $upload.upload({
              url : root_mapping + "upload/" + $scope.focusProject.id,
              file : $file,
              progress : function(e) {
              } 
            }).error(function(data, status, headers, config) {
                // file is not uploaded
                // successfully
                console.log("error", data);
                $scope.recordError = "Error updating map project.";
                $rootScope.handleHttpError(data, status, headers, config);
                $rootScope.glassPane--;
            }).success(function(data) {
              // file is uploaded
              // successfully
              confirm("The mapping principle handbook file upload is complete.");
              $rootScope.glassPane--;
            });
          }
        };

      } ]);
