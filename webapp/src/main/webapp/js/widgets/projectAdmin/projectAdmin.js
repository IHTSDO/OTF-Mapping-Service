angular
  .module('mapProjectApp.widgets.projectAdmin', [ 'adf.provider' ])
  .config(
    function(dashboardProvider) {
      dashboardProvider
        .widget(
          'projectAdmin',
          {
            title : 'Map Project Administrative Tools',
            description : 'In browser administrative tools to modify map projects outside of the normal workflow process',
            controller : 'projectAdminCtrl',
            templateUrl : 'js/widgets/projectAdmin/projectAdmin.html',
            edit : {}
          });
    })
  .controller(
    'projectAdminCtrl',
    function($scope, $rootScope, $http, $location, localStorageService) {

      $scope.user = localStorageService.get('currentUser');
      $scope.project = localStorageService.get('focusProject');
      $scope.mapProjects = localStorageService.get('mapProjects');
      $scope.role = localStorageService.get('currentRole');

      // local variables to display visual cues for editing
      $scope.editingPerformed = false;
      $scope.successMsg = null;
      $scope.errorMsg = null;

      // local variable to display visual cue for duplicate id
      $scope.isDuplicateRefSetId = false;

      $scope.userToken = localStorageService.get('userToken');
      $scope.$watch([ 'focusProject', 'userToken' ], function() {
        console.debug('editedListCtrl:  Detected project set/change');

        if ($scope.focusProject != null && $scope.userToken != null) {

          $http.defaults.headers.common.Authorization = $scope.userToken;

        }
      });

      $scope.getAdminProject = function(id) {

        $scope.adminProject = null;
        $scope.successMsg = null;
        $scope.errorMsg = null;

        $rootScope.glassPane++;
        $http({
          url : root_mapping + "project/id/" + id,
          method : "GET",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(function(data) {
          $rootScope.glassPane--;
          console.debug("Successfully retrieved project " + data.name);
          $scope.adminProject = data;

          // check for duplicate ref set id
          $scope.checkRefSetId();

        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          console.debug("ERROR GETTING PROJECT");
          $scope.errorMsg = "Could not retrieve map project";
        });
      };

      $scope.setEditingPerformed = function() {
        $scope.editingPerformed = true;
        $scope.successMsg = null;
        $scope.errorMsg = null;
      };

      $scope.checkRefSetId = function() {
        $scope.isDuplicateRefSetId = false;
        for (var i = 0; i < $scope.mapProjects.length; i++) {
          if ($scope.mapProjects[i].id != $scope.adminProject.id
            && $scope.mapProjects[i].refSetId === $scope.adminProject.refSetId) {
            $scope.isDuplicateRefSetId = true;
          }

        }
      };

      $scope.updateAdminProject = function() {

        if ($scope.isDuplicateRefSetId == true) {
          alert("You cannot update the project while its ref set id matches that of another project.");
          return;
        }

        if ($scope.adminProject.refSetId == null
          || $scope.adminProject.refSetId.length == 0) {
          alert("You must specify a unique ref set id");
          return;
        }

        if (confirm("ARE YOU ABSOLUTELY SURE?\n\n  Updating a project can have significant implications for any existing mappings.") == false)
          return;

        $rootScope.glassPane++;
        $http({
          url : root_mapping + "project/update",
          method : "POST",
          dataType : "json",
          data : $scope.project,
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(
          function(data) {
            $rootScope.glassPane--;

            $scope.successMsg = 'Successfully updated project '
              + $scope.adminProject.id;

            var mapProjects = [];
            for (var i = 0; i < $scope.mapProjects.length; i++) {
              if ($scope.mapProjects[i].id != $scope.adminProject.id) {
                mapProjects.push($scope.mapProjects[i]);
              } else {
                mapProjects.push($scope.adminProject);
              }
            }
            $scope.mapProjects = mapProjects;

            localStorageService.add('mapProjects', $scope.mapProjects);

            // broadcast change
            $rootScope.$broadcast(
              'localStorageModule.notification.setMapProjects', {
                key : 'mapProjects',
                mapProjects : $scope.mapProjects
              });

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

        $scope.editingPerformed = false;
      };

      $scope.copyAdminProject = function() {

        $rootScope.glassPane++;

        $scope.adminProject.id = null;
        $scope.adminProject.refSetId = null;
        $scope.adminProject.refSetName = null;
        $http({
          url : root_mapping + "project/add",
          method : "PUT",
          dataType : "json",
          data : $scope.adminProject,
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(
          function(data) {
            $rootScope.glassPane--;

            // set the admin project to response
            $scope.adminProject = data;

            // check ref set id
            $scope.checkRefSetId();

            $scope.successMsg = 'Successfully added project '
              + $scope.adminProject.id;

            // add to local projects and to cache
            $scope.mapProjects.push(data);
            localStorageService.add('mapProjects', $scope.mapProjects);

            // broadcast change
            $rootScope.$broadcast(
              'localStorageModule.notification.setMapProjects', {
                key : 'mapProjects',
                mapProjects : $scope.mapProjects
              });

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.deleteAdminProject = function(id) {

        if (confirm("ARE YOU ABSOLUTELY SURE?\n\n  Deleting a project requires recomputing workflow and rerunning indexes, and may cause workflow problems for other projects.") == false)
          return;

        $rootScope.glassPane++;
        $http({
          url : root_mapping + "project/delete",
          method : "DELETE",
          dataType : "json",
          data : $scope.adminProject,
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(
          function(data) {
            $rootScope.glassPane--;

            $scope.successMsg = 'Successfully deleted project '
              + $scope.adminProject.id;

            var mapProjects = [];
            for (var i = 0; i < $scope.mapProjects.length; i++) {
              if ($scope.mapProjects[i].id != $scope.adminProject.id) {
                mapProjects.push($scope.mapProjects[i]);
              }
            }
            $scope.mapProjects = mapProjects;

            localStorageService.add('mapProjects', $scope.mapProjects);

            // broadcast change
            $rootScope.$broadcast(
              'localStorageModule.notification.setMapProjects', {
                key : 'mapProjects',
                mapProjects : $scope.mapProjects
              });

            // clear the viewed project
            $scope.adminProject = null;

          }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

    });
