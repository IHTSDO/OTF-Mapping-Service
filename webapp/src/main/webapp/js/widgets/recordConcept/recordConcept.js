'use strict';

angular
  .module('mapProjectApp.widgets.recordConcept', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('recordConcept', {
      title : 'Record Concept',
      description : 'Displays concept for map record.',
      controller : 'recordConceptCtrl',
      templateUrl : 'js/widgets/recordConcept/recordConcept.html',
      edit : {}
    });
  })
  .controller(
    'recordConceptCtrl',
    function($scope, $rootScope, $http, $routeParams, $location, $modal,
      localStorageService, $sce) {

      // scope variables
      $scope.page = 'concept';
      $scope.error = ""; // initially empty
      $scope.conceptId = $routeParams.conceptId;
      $scope.recordsInProject = [];
      $scope.recordsNotInProject = [];
      $scope.historicalRecords = [];
      $scope.recordsInProjectNotFound = false; // set to true after record
      // retrieval returns no records
      // for focus project

      $scope.focusProject = null;
      $scope.mapProjects = null;

      // retrieve cached values
      $scope.focusProject = localStorageService.get("focusProject");
      $scope.mapProjects = localStorageService.get("mapProjects");
      $scope.currentUser = localStorageService.get("currentUser");
      $scope.currentRole = localStorageService.get("currentRole");
      $scope.preferences = localStorageService.get("preferences");
      $scope.userToken = localStorageService.get('userToken');

      // watch for changes to focus project
      $scope.$on('localStorageModule.notification.setFocusProject', function(
        event, parameters) {
        console
          .debug("RecordConceptListCtrl:  Detected change in focus project");
        $scope.focusProject = parameters.focusProject;
        $scope.filterRecords();
      });

      // once focus project, user token, and map projects retrieved,
      // retrieve
      // the concept and records
      $scope.$watch([ 'focusProject', 'userToken', 'mapProjects' ], function() {

        // need both focus project and user token set before
        // executing main
        // functions
        if ($scope.focusProject != null && $scope.userToken != null
          && $scope.mapProjects != null) {
          $http.defaults.headers.common.Authorization = $scope.userToken;
          console.debug($scope.mapProjects);
          $scope.go();
        }
      });

      $scope.go = function() {

        $scope.recordsInProjectNotFound = false;

        console.debug("RecordConceptCtrl:  Focus Project change");

        // find concept based on source terminology
        $http(
          {
            url : root_content + "concept/id/"
              + $scope.focusProject.sourceTerminology + "/"
              + $scope.focusProject.sourceTerminologyVersion + "/"
              + $routeParams.conceptId,
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(
          function(data) {
            $scope.concept = data;
            setTitle($scope.focusProject.sourceTerminology,
              $routeParams.conceptId, $scope.concept.defaultPreferredName);
            $scope.getRecordsForConcept();
            $scope.findUnmappedDescendants();

            // find children based on source terminology
            $http(
              {
                url : root_content + "concept/id/"
                  + $scope.focusProject.sourceTerminology + "/"
                  + $scope.focusProject.sourceTerminologyVersion + "/"
                  + $routeParams.conceptId + "/children",
                dataType : "json",
                method : "GET",
                headers : {
                  "Content-Type" : "application/json"
                }
              }).success(function(data) {
              console.debug(data);
              $scope.concept.children = data.searchResult;

            }).error(function(data, status, headers, config) {
              $rootScope.handleHttpError(data, status, headers, config);
            });
          }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.goProjectDetails = function() {
        console.debug("Redirecting to project details view");
        $location.path("/project/details");
      };

      $scope.goMapRecords = function() {
        console.debug("Redirecting to project records view");
        $location.path("/project/records");
      };

      // function to return trusted html code (for tooltip content)
      $scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };

      $scope.getRecordsForConcept = function() {
        // retrieve all records with this concept id
        $http({
          url : root_mapping + "record/concept/id/" + $routeParams.conceptId,
          dataType : "json",
          method : "GET",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(function(data) {
          $scope.records = data.mapRecord;
          $scope.filterRecords();
        }).error(function(data, status, headers, config) {
          $rootScope.handleHttpError(data, status, headers, config);
        }).then(function() {

          $scope.getRecordsForConceptHistorical();

          // check relation style flags
          if ($scope.focusProject.mapRelationStyle === "MAP_CATEGORY_STYLE") {
            applyMapCategoryStyle();
          }

          if ($scope.focusProject.mapRelationStyle === "RELATIONSHIP_STYLE") {
            applyRelationshipStyle();
          }

          console.debug('VALIDATING RECORDS');
          validateRecords();
        });
      };

      // function to retrieve validation results
      function validateRecords() {

        console.debug('VALIDATING RECORDS FOR ROLE ' + $scope.currentRole);

        if ($scope.currentRole != 'Administrator'
          && $scope.currentRole != 'Lead' && $scope.currentRole != 'Specialist')
          return;

        console.debug('Cycling over records', $scope.records);

        for (var i = 0; i < $scope.records.length; i++) {

          console.debug('record ' + i, $scope.records[i]);

          validateRecord($scope.records[i]);

        }
      }

      function validateRecord(record) {

        $rootScope.glassPane++;
        $http({
          url : root_mapping + "validation/record/validate",
          dataType : "json",
          data : record,
          method : "POST",
          headers : {
            "Content-Type" : "application/json"
          }
        }).success(function(data) {
          $rootScope.glassPane--;
          console.debug("validation results:", data);
          record.errors = data.errors;
          record.warnings = data.warnings;
          console.debug(record);
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $rootScope.handleHttpError(data, status, headers, config);
        });

      }

      $scope.getRecordsForConceptHistorical = function() {
        // retrieve all records with this concept id
        $http(
          {
            url : root_mapping + "record/concept/id/" + $routeParams.conceptId
              + "/project/id/" + $scope.focusProject.id + "/historical",
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          })
          .success(
            function(data) {
              $scope.historicalRecords = data.mapRecord;
              // remove records that are already displayed in focus
              // project
              // section
              for (var i = $scope.historicalRecords.length; i--;) {
                var found = false;
                for (var j = 0; j < $scope.recordsInProject.length; j++) {
                  if ($scope.historicalRecords[i].id == $scope.recordsInProject[j].id)
                    found = true;
                }
                if (found == true) {
                  $scope.historicalRecords.splice(i, 1);
                }
              }
            }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          }).then(function() {
            // check relation style flags
            /*
             * if ($scope.focusProject.mapRelationStyle ===
             * "MAP_CATEGORY_STYLE") { applyMapCategoryStyle(); }
             * 
             * if ($scope.focusProject.mapRelationStyle ===
             * "RELATIONSHIP_STYLE") { applyRelationshipStyle(); }
             */
          });
      };

      $scope.displayToViewer = function(record) {
        if ($scope.currentRole === 'Viewer'
          && record.workflowStatus === 'READY_FOR_PUBLICATION') {
          return false;
        } else
          return true;
      };

      $scope.filterRecords = function() {
        $scope.recordsInProject = [];
        $scope.recordsNotInProject = [];

        console.debug("Filtering records (" + $scope.records.length + ")");
        for (var i = 0; i < $scope.records.length; i++) {
          if ($scope.records[i].mapProjectId === $scope.focusProject.id) {
            $scope.recordsInProject.push($scope.records[i]);
          } else if ($scope.getProject($scope.records[i])) {
            var project = $scope.getProject($scope.records[i]);
            var projectPublic = project.public;
            if ($scope.currentUser.name == 'Administrator'
              || $scope.currentUser.name == 'Lead'
              || $scope.currentUser.name == 'Specialist'
              || ($scope.currentUser.name == 'Guest' && projectPublic == true)) {
              var projectExists = false;
              for (var j = 0; j < $scope.recordsNotInProject.length; j++) {

                if ($scope.recordsNotInProject[j][0].mapProjectId === $scope.records[i].mapProjectId) {
                  console.debug("Found match for "
                    + $scope.records[i].mapProjectId);
                  $scope.recordsNotInProject[j].push($scope.records[i]);
                  projectExists = true;
                }
              }
              if (!projectExists) {
                var newArray = [];
                newArray.push($scope.records[i]);
                $scope.recordsNotInProject.push(newArray);
              }
            }
          }
        }

        console.debug($scope.recordsInProject.length + " records in project "
          + $scope.focusProject.name);

        for (var i = 0; i < $scope.recordsNotInProject.length; i++)
          console.debug($scope.recordsNotInProject[i].length
            + " records in project "
            + $scope.recordsNotInProject[i][0].mapProjectId);

        // if no records for this project found, set flag
        if ($scope.recordsInProject.length == 0) {
          $scope.recordsInProjectNotFound = true;
        } else {
          $scope.recordsInProjectNotFound = false;
        }
      };

      $scope.isEditable = function(record) {

        if (($scope.currentRole === 'Specialist'
          || $scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator')
          && (record.workflowStatus === 'PUBLISHED' || record.workflowStatus === 'READY_FOR_PUBLICATION')) {

          return true;

        } else if ($scope.currentUser.userName === record.owner.userName) {
          return true;
        } else
          return false;
      };

      $scope.editRecord = function(record) {

        console.debug("EditRecord()");
        console.debug(record);

        // check if this record is assigned to the user and not in a
        // publication
        // ready state
        if (record.owner.userName === $scope.currentUser.userName
          && record.workflowStatus != 'PUBLISHED'
          && record.workflowStatus != 'READY_FOR_PUBLICATION') {

          // if a conflict or review record record, go to conflict
          // resolution
          // page
          if (record.workflowStatus === 'CONFLICT_NEW'
            || record.workflowStatus === 'CONFLICT_IN_PROGRESS') {
            $location.path("/record/conflicts/" + record.id);
          }

          else if (record.workflowStatus === 'REVIEW_NEW'
            || record.workflowStatus === 'REVIEW_IN_PROGRESS') {
            $location.path("/record/review/" + record.id);
          }

          else if (record.workflowStatus === 'QA_NEW'
            || record.workflowStatus === 'QA_IN_PROGRESS') {
            $location.path("/record/review/" + record.id);
          }

          // otherwise go to the edit page
          else
            $location.path("/record/recordId/" + record.id);

          // otherwise, assign this record along the FIX_ERROR_PATH
        } else {

          // assign the record along the FIX_ERROR_PATH
          $rootScope.glassPane++;

          // remove advices if this is a RELATIONSHIP_STYLE project (these
          // are
          // used to render relation names)
          if ($scope.focusProject.mapRelationStyle === "RELATIONSHIP_STYLE") {
            for (var i = 0; i < record.mapEntry.length; i++)
              record.mapEntry[i].mapAdvice = [];
          }

          console.debug("Edit record clicked, assigning record if necessary");
          $http(
            {
              url : root_workflow + "assignFromRecord/user/id/"
                + $scope.currentUser.userName,
              method : "POST",
              dataType : 'json',
              data : record,
              headers : {
                "Content-Type" : "application/json"
              }
            }).success(
            function(data) {
              console.debug('Assignment successful');
              $http(
                {
                  url : root_workflow + "record/project/id/"
                    + $scope.focusProject.id + "/concept/id/"
                    + record.conceptId + "/user/id/"
                    + $scope.currentUser.userName,
                  method : "GET",
                  dataType : 'json',
                  data : record,
                  headers : {
                    "Content-Type" : "application/json"
                  }
                }).success(function(data) {
                console.debug(data);
                $rootScope.glassPane--;

                // open the record edit view
                $location.path("/record/recordId/" + data.id);
              }).error(function(data, status, headers, config) {
                $rootScope.glassPane--;

                $rootScope.handleHttpError(data, status, headers, config);
              });

            }).error(function(data, status, headers, config) {
            $rootScope.glassPane--;

            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
      };

      $scope.getProject = function(record) {
        for (var i = 0; i < $scope.mapProjects.length; i++) {
          if ($scope.mapProjects[i].id == record.mapProjectId) {
            return $scope.mapProjects[i];
          }
        }
        return null;
      };

      $scope.getProjectFromName = function(name) {
        for (var i = 0; i < $scope.mapProjects.length; i++) {
          if ($scope.mapProjects[i].name === name) {
            return $scope.mapProjects[i];
          }
        }
        return null;
      };

      $scope.getProjectName = function(record) {

        for (var i = 0; i < $scope.mapProjects.length; i++) {
          if ($scope.mapProjects[i].id == record.mapProjectId) {
            return $scope.mapProjects[i].name;
          }
        }
        return null;
      };

      $scope.findUnmappedDescendants = function() {

        $http(
          {
            url : root_mapping + "concept/id/" + $scope.concept.terminologyId
              + "/" + "unmappedDescendants/project/id/"
              + $scope.focusProject.id,
            dataType : "json",
            method : "GET",
            headers : {
              "Content-Type" : "application/json"
            }
          }).success(function(data) {
          if (data.count > 0)
            $scope.unmappedDescendantsPresent = true;
          $scope.concept.unmappedDescendants = data.searchResult;
        }).error(function(data, status, headers, config) {

          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // given a record, retrieves associated project's ruleBased flag
      $scope.getRuleBasedForRecord = function(record) {
        var project = $scope.getProject(record);
        return project.ruleBased;

      };

      function applyMapCategoryStyle() {

        // Cycle over all entries. If targetId is blank, show relationName
        // as
        // the target name
        for (var i = 0; i < $scope.records.length; i++) {
          for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {

            if ($scope.records[i].mapEntry[j].targetId === "") {
              $scope.records[i].mapEntry[j].targetName = "\""
                + $scope.records[i].mapEntry[j].relationName + "\"";

            }
          }
        }
      }
      ;

      function applyRelationshipStyle() {
        // Cycle over all entries. Add the relation name to the advice list
        for (var i = 0; i < $scope.records.length; i++) {
          for (var j = 0; j < $scope.records[i].mapEntry.length; j++) {
            if ($scope.records[i].mapEntry[j].targetId === "") {
              // get the object for easy handling
              var jsonObj = $scope.records[i].mapEntry[j].mapAdvice;

              console.debug("Relation",
                $scope.records[i].mapEntry[j].mapRelation);

              var relationAsAdvice = {
                "id" : "0",
                "name" : $scope.records[i].mapEntry[j].mapRelation.abbreviation,
                "detail" : $scope.records[i].mapEntry[j].mapRelation.name,
                "objectId" : "0"
              };

              console.debug(relationAsAdvice);
              // add the serialized advice
              jsonObj.push(relationAsAdvice);
              $scope.records[i].mapEntry[j].mapAdvice = jsonObj;
            }
          }
        }
      }
      ;

      // change the focus project to the project associated with a specified
      // record
      $scope.changeFocusProjectByRecord = function(record) {

        console.debug("changeFocusProjectByRecord:  record project id = "
          + record.mapProjectId);

        console.debug($scope.mapProjects);
        for (var i = 0; i < $scope.mapProjects.length; i++) {
          console.debug("  comparing to project id = "
            + $scope.mapProjects[i].id);
          if ($scope.mapProjects[i].id = record.mapProjectId) {

            $scope.changeFocusProject($scope.mapProjects[i]);
            break;
          }
        }
      };

      $scope.logout = function() {
        $rootScope.glassPane++;
        $http(
          {
            url : root_security + "logout/user/id/"
              + $scope.currentUser.userName,
            method : "POST",
            headers : {
              "Content-Type" : "text/plain"
            // save userToken from authentication
            }
          }).success(function(data) {
          $rootScope.glassPane--;
          $location.path("/");
        }).error(function(data, status, headers, config) {
          $rootScope.glassPane--;
          $location.path("/");
          $rootScope.handleHttpError(data, status, headers, config);
        });

      }
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

        $http({
          url : root_mapping + "userPreferences/update",
          dataType : "json",
          data : $scope.preferences,
          method : "POST",
          headers : {
            "Content-Type" : "application/json"
          }
        })
          .success(function(data) {
          })
          .error(
            function(data) {
              if (response.indexOf("HTTP Status 401") != -1) {
                $rootScope.globalError = "Authorization failed.  Please log in again.";
                $location.path("/");
              }
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

      function setTitle(terminology, conceptId, defaultPreferredName) {
        $scope.model.title = terminology + " Concept " + conceptId + ": "
          + defaultPreferredName;

      }
      ;

      // opens SNOMED CT browser
      $scope.getBrowserUrl = function() {
        if ($scope.currentUser.userName === 'guest')
          return "http://browser.ihtsdotools.org/index.html?perspective=full&conceptId1="
            + $scope.conceptId
            + "&edition=en-edition&release=v"
            + $scope.focusProject.sourceTerminologyVersion
            + "&server=https://browser-aws-1.ihtsdotools.org/&langRefset=900000000000509007";
        else
          return "http://dailybuild.ihtsdotools.org/index.html?perspective=full&conceptId1="
            + $scope.conceptId
            + "&diagrammingMarkupEnabled=true&acceptLicense=true";
      };

      $scope.openConceptBrowser = function() {
        window.open($scope.getBrowserUrl(), "browserWindow");
      };

      

      $scope.openViewerFeedbackModal = function(lrecord, currentUser) {

        console.debug("openViewerFeedbackModal with ", lrecord, currentUser);

        var modalInstance = $modal
          .open({
            templateUrl : 'js/widgets/projectRecords/projectRecordsViewerFeedback.html',
            controller : ViewerFeedbackModalCtrl,
            resolve : {
              record : function() {
                return lrecord;
              },
              currentUser : function() {
                return currentUser;
              }
            }
          });

      };

      var ViewerFeedbackModalCtrl = function($scope, $modalInstance, record) {

        console.debug("Entered modal control", record);

        $scope.record = record;
        $scope.project = localStorageService.get('focusProject');
        $scope.currentUser = localStorageService.get('currentUser');
        $scope.returnRecipients = $scope.project.mapLead;
        $scope.feedbackInput = '';

        $scope.sendFeedback = function(record, feedbackMessage, name, email) {
          console.debug("Sending feedback email", record);

          if (feedbackMessage == null || feedbackMessage == undefined
            || feedbackMessage === '') {
            window.alert("The feedback field cannot be blank. ");
            return;
          }

          if ($scope.currentUser.userName === 'guest'
            && (name == null || name == undefined || name === ''
              || email == null || email == undefined || email === '')) {
            window.alert("Name and email must be provided.");
            return;
          }

          if ($scope.currentUser.userName === 'guest'
            && validateEmail(email) == false) {
            window.alert("Invalid email address provided.");
            return;
          }

          var sList = [ name, email, record.conceptId, record.conceptName, $scope.project.refSetId,
            feedbackMessage ];

          $rootScope.glassPane++;
          $http({
            url : root_workflow + "message",
            dataType : "json",
            method : "POST",
            data : sList,
            headers : {
              "Content-Type" : "application/json"
            }

          }).success(function(data) {
            console.debug("success to sendFeedbackEmail.");
            $rootScope.glassPane--;
            $modalInstance.close();
          }).error(function(data, status, headers, config) {
            $modalInstance.close();
            $scope.recordError = "Error sending feedback email.";
            $rootScope.glassPane--;
            $rootScope.handleHttpError(data, status, headers, config);
          });

        };

        $scope.cancel = function() {
          $modalInstance.dismiss('cancel');
        };

        function validateEmail(email) {
          var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
          return re.test(email);
        }

        $scope.tinymceOptions = {

          menubar : false,
          statusbar : false,
          plugins : "autolink autoresize link image charmap searchreplace lists paste",
          toolbar : "undo redo | styleselect lists | bold italic underline strikethrough | charmap link image",

          setup : function(ed) {

            // added to fake two-way binding from the html
            // element
            // noteInput is not accessible from this javascript
            // for some reason
            ed.on('keyup', function(e) {
              $scope.tinymceContent = ed.getContent();
              $scope.$apply();
            });
          }
        };
      };      
    });
