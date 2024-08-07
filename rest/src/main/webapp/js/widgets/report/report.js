"use strict";

angular
  .module("mapProjectApp.widgets.report", ["adf.provider"])
  .config(function (dashboardProvider) {
    dashboardProvider.widget("report", {
      title: "Reports",
      description: "Widget for viewing and generating reports",
      controller: "reportCtrl",
      templateUrl: "js/widgets/report/report.html",
      edit: {},
    });
  })
  .controller(
    "reportCtrl",
    function (
      $scope,
      $rootScope,
      $http,
      $location,
      $uibModal,
      $sce,
      localStorageService,
      gpService,
      appConfig
    ) {
      // initialize as empty to indicate still initializing
      // database connection
      $scope.appConfig = appConfig;
      $scope.currentUser = localStorageService.get("currentUser");
      $scope.currentRole = localStorageService.get("currentRole");
      $scope.focusProject = localStorageService.get("focusProject");

      // select options
      $scope.reportSelected = null;

      $scope.definitionEditing = null;
      $scope.isAddingDefinition = false;

      $scope.resultTypes = ["CONCEPT", "MAP_RECORD"];
      $scope.availableRoles = ["VIEWER", "SPECIALIST", "LEAD", "ADMINISTRATOR"];
      $scope.queryTypes = ["SQL", "HQL", "LUCENE"];
      $scope.timePeriods = ["DAILY", "WEEKLY", "MONTHLY"];

      // value field parsing
      $scope.valueFields = [];

      // pagination variables
      $scope.itemsPerPage = 10;

      // watch for project change
      $scope.$on(
        "localStorageModule.notification.setFocusProject",
        function (event, parameters) {
          $scope.focusProject = parameters.focusProject;
        }
      );

      // on any change of focusProject, set headers
      $scope.currentUserToken = localStorageService.get("userToken");
      $scope.$watch(["focusProject", "currentUser", "userToken"], function () {
        if (
          $scope.focusProject != null &&
          $scope.currentUser != null &&
          $scope.currentUserToken != null
        ) {
          $http.defaults.headers.common.Authorization = $scope.currentUserToken;

          // retrieve the definitions
          $scope.definitions = $scope.focusProject.reportDefinition.filter(
            function (item) {
              if ($scope.currentRole == "Specialist") {
                return (
                  item.roleRequired == "SPECIALIST" ||
                  item.roleRequired == "VIEWER"
                );
              } else if ($scope.currentRole == "Lead") {
                return (
                  item.roleRequired == "LEAD" ||
                  item.roleRequired == "SPECIALIST" ||
                  item.roleRequired == "VIEWER"
                );
              }
              return true;
            }
          );

          $scope.definitions.sort();

          if ($scope.currentRole == "Lead") {
            $scope.getReports(1, null, null);
          }
        }
      });

      $scope.getReports = function (page, pdefinition, queryReport) {
        console.debug("*** Get reports", page, pdefinition, queryReport);
        var definition = pdefinition;
        // force reportType to null if undefined or blank string
        if (definition == undefined || definition === "") definition = null;
        // construct a PFS object
        var pfsParameterObj = {
          startIndex: (page - 1) * $scope.itemsPerPage,
          maxResults: $scope.itemsPerPage,
          sortField: null,
          queryRestriction: null,
        };

        // gpService.increment();

        // construct the url based on whether report type is
        // null
        var url =
          root_reporting +
          "report/reports/project/id/" +
          $scope.focusProject.id +
          (definition == null ? "" : "/definition/id/" + definition.id);

        // obtain the reports
        $http({
          url: url,
          dataType: "json",
          data: pfsParameterObj,
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        })
          .success(function (data) {
            // gpService.decrement();
            $scope.reports = data.report;
            // set paging parameters
            $scope.nReports = data.totalCount;
            $scope.nReportPages = Math.ceil(
              data.totalCount / $scope.itemsPerPage
            );
          })
          .error(function (data, status, headers, config) {
            // gpService.decrement();
            $scope.reports = null;
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      $scope.viewReport = function (report) {
        console.debug("view report", $scope.focusProject, report);

        // retrieve the report
        gpService.increment();
        // obtain the record
        $http
          .get(
            root_reporting +
              "report/project/id/" +
              $scope.focusProject.id +
              "/" +
              report.id
          )
          .success(function (report) {
            gpService.decrement();

            initializeCollapsed(report); // set the collapses
            // to true
            $scope.reportDisplayed = report; // set the displayed
            // report
          })
          .error(function (data, status, headers, config) {
            gpService.decrement();
            $scope.generatedReport = null;
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      // function to return trusted html code (for advice content)
      $scope.to_trusted = function (html_code) {
        return $sce.trustAsHtml(html_code);
      };

      $scope.generateReport = function (definition) {
        gpService.increment();
        // obtain the record
        $http({
          url:
            root_reporting +
            "report/generate/project/id/" +
            $scope.focusProject.id +
            "/user/id/" +
            $scope.currentUser.userName,
          dataType: "json",
          data: definition,
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        })
          .success(function (data) {
            gpService.decrement();
            $scope.generatedReport = data;
          })
          .error(function (data, status, headers, config) {
            gpService.decrement();
            $scope.generatedReport = null;
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      $scope.toggleResultItems = function (reportResult) {
        // if open, simply close
        if (reportResult.isCollapsed == false) {
          reportResult.isCollapsed = true;

          // if closed, re-open and get result items if
          // necessary
        } else {
          reportResult.isCollapsed = false;
          if (reportResult.reportResultItems == null) {
            $scope.getResultItems(reportResult, reportResult.page);
          }
        }
      };
      // if closed, open
      $scope.getResultItems = function (reportResult, page) {
        gpService.increment();
        // construct a PFS object
        var pfsParameterObj = {
          startIndex: (page - 1) * $scope.itemsPerPage,
          maxResults: $scope.itemsPerPage,
          sortField: null,
          queryRestriction: null,
        };

        // obtain the reports
        $http({
          url: root_reporting + "reportResult/id/" + reportResult.id + "/items",
          dataType: "json",
          data: pfsParameterObj,
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        })
          .success(function (data) {
            gpService.decrement();
            reportResult.reportResultItems = data.reportResultItem;
            reportResult.page = page;
            reportResult.nPages = Math.ceil(
              reportResult.ct / $scope.itemsPerPage
            );

            return reportResult;
          })
          .error(function (data, status, headers, config) {
            gpService.decrement();
            reportResult.reportResultItems = null;
            $rootScope.handleHttpError(data, status, headers, config);
            return null;
          });
      };

      var initializeCollapsed = function (report) {
        for (var i = 0; i < report.results.length; i++) {
          report.results[i].isCollapsed = true;
          report.results[i].reportResultItems = null;
          report.results[i].page = 1;
          report.results[i].nPages = Math.ceil(
            report.results[i].ct / $scope.itemsPerPage
          );
        }
      };

      $scope.generateNewReport = function (reportDefinition) {
        gpService.increment();
        // obtain the record
        $http({
          url:
            root_reporting +
            "report/generate/project/id/" +
            $scope.focusProject.id +
            "/user/id/" +
            $scope.currentUser.userName,
          method: "POST",
          dataType: "json",
          data: reportDefinition,
          headers: {
            "Content-Type": "application/json",
          },
        })
          .success(function (data) {
            gpService.decrement();
            $scope.viewReport(data);
            $scope.getReports(1, null, null);
            $scope.definitionMsg = "Successfully saved definition";
          })
          .error(function (data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      $scope.addReportDefinition = function () {
        var definition = {
          id: null,
          name: "(New Report)",
          query: null,
          queryType: null,
          resultType: null,
          roleRequired: null,
          isDiffReport: false,
          timePeriod: null,
        };

        $scope.definitionEditing = definition;
        $scope.isAddingDefinition = true;
      };

      $scope.showRunReport = function (reportDefinition) {
        var show = false;
        if (
		  reportDefinition != null && appConfig["deploy.reports.allowed"].split(",").includes(reportDefinition.name)
        ) {
          show = true;
        }
        return show;
      };

      $scope.runReport = function (reportDefinition) {
        gpService.increment();
        $http({
          url: root_reporting + "report/execute",
          method: "POST",
          dataType: "json",
          data: reportDefinition.name,
          headers: {
            "Content-Type": "application/json",
          },
        })
          .success(function (data) {
            gpService.decrement();
            $scope.definitionMsg = "Successfully run report";
			  window
              .alert("Report has successfully started, and the output will be emailed when completed.");
          })
          .error(function (data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      $scope.deleteReport = function (
        report,
        page,
        selectedDefinition,
        queryReport
      ) {
        if (confirm("Are you sure that you want to delete a report?") == false)
          return;

        $http({
          url: root_reporting + "report/delete",
          dataType: "json",
          data: report,
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
          },
        })
          .success(function (data) {
            $scope.getReports(page, selectedDefinition, queryReport);
          })
          .error(function (data, status, headers, config) {
            $scope.recordError = "Error deleting map report from application.";
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      $scope.exportReport = function (report) {
        gpService.increment();
        $http({
          url: root_reporting + "report/export/" + report.id,
          dataType: "json",
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          },
          responseType: "arraybuffer",
        })
          .success(function (data) {
            $scope.definitionMsg = "Successfully exported report";
            var blob = new Blob([data], {
              type: "application/vnd.ms-excel",
            });
            // hack to download store a file having its URL
            var fileURL = URL.createObjectURL(blob);
            var a = document.createElement("a");
            a.href = fileURL;
            a.target = "_blank";
            a.download = getReportFileName(report);
            document.body.appendChild(a);
            gpService.decrement();
            a.click();
          })
          .error(function (data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };

      var getReportFileName = function (report) {
        var date = new Date(report.timestamp)
          .toISOString()
          .slice(0, 10)
          .replace(/-/g, "");
        return report.name + "." + date + ".xls";
      };
    }
  );
