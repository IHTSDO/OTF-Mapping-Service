'use strict';

angular
		.module('mapProjectApp.widgets.report', [ 'adf.provider' ])
		.config(function(dashboardProvider) {
			dashboardProvider.widget('report', {
				title : 'Reports',
				description : 'Widget for viewing and generating reports',
				controller : 'reportCtrl',
				templateUrl : 'js/widgets/report/report.html',
				edit : {}
			});
		})
		.controller(
				'reportCtrl',
				function($scope, $rootScope, $http, $location, $modal, $sce,
						localStorageService) {

					// initialize as empty to indicate still initializing
					// database connection
					$scope.currentUser = localStorageService.get('currentUser');
					$scope.currentRole = localStorageService.get('currentRole');
					$scope.focusProject = localStorageService
							.get('focusProject');

					// select options
					$scope.reportSelected = null;
		
					$scope.definitionEditing = null;
					$scope.isAddingDefinition = false;

					$scope.resultTypes = [ 'CONCEPT', 'MAP_RECORD' ];
					$scope.availableRoles = [ 'VIEWER', 'SPECIALIST', 'LEAD',
							'ADMINISTRATOR' ];
					$scope.queryTypes = [ 'SQL', 'HQL', 'LUCENE' ];
					$scope.timePeriods = [ 'DAILY', 'WEEKLY', 'MONTHLY' ];

					// value field parsing
					$scope.valueFields = [];

					// pagination variables
					$scope.itemsPerPage = 10;

					// watch for project change
					$scope
							.$on(
									'localStorageModule.notification.setFocusProject',
									function(event, parameters) {
										console
												.debug("MapProjectWidgetCtrl:  Detected change in focus project");
										$scope.focusProject = parameters.focusProject;
									});

					// on any change of focusProject, set headers
					$scope.currentUserToken = localStorageService
							.get('userToken');
					$scope
							.$watch(
									[ 'focusProject', 'currentUser',
											'userToken' ],
									function() {

										if ($scope.focusProject != null
												&& $scope.currentUser != null
												&& $scope.currentUserToken != null) {
											$http.defaults.headers.common.Authorization = $scope.currentUserToken;

											// retrieve the definitions
											$scope.definitions = $scope.focusProject.reportDefinition;

											console.debug(
													"Report definitions: ",
													$scope.definitions);

											// retrieve the first page of
											// reports
											$scope.getReports(1, null, null);
										}
									});

					$scope.getReports = function(page, definition, queryReport) {

						// force reportType to null if undefined or blank string
						if (definition == undefined || definition === '')
							definition = null;

						console.debug("getReports", page, definition,
								queryReport);

						// construct a PFS object
						var pfsParameterObj = {
							"startIndex" : (page - 1) * $scope.itemsPerPage,
							"maxResults" : $scope.itemsPerPage,
							"sortField" : null,
							"queryRestriction" : null
						};

						$rootScope.glassPane++;

						// construct the url based on whether report type is
						// null
						var url = root_reporting
								+ "report/reports/project/id/"
								+ $scope.focusProject.id
								+ (definition == null ? "" : "/definition/id/"
										+ definition.id);

						console.debug("getReports URL", url);

						// obtain the reports
						$http({
							url : url,
							dataType : "json",
							data : pfsParameterObj,
							method : "POST",
							headers : {
								"Content-Type" : "application/json"
							}
						}).success(
								function(data) {
									$rootScope.glassPane--;
									$scope.reports = data.report;

									// set paging parameters
									$scope.nReports = data.totalCount;
									$scope.nReportPages = Math
											.ceil(data.totalCount
													/ $scope.itemsPerPage);

									console.debug("Pagination variables",
											$scope.nReports,
											$scope.nReportPages);
								}).error(
								function(data, status, headers, config) {
									$rootScope.glassPane--;
									$scope.reports = null;
									$rootScope.handleHttpError(data, status,
											headers, config);
								});
					}

					$scope.viewReport = function(report) {
						initializeCollapsed(report); // set the collapses
						// to true
						$scope.reportDisplayed = report; // set the displayed
						// report
					};

					$scope.generateReport = function(definition) {

						$rootScope.glassPane++;
						console.debug("Definition", definition);
						// obtain the record
						$http(
								{
									url : root_reporting
											+ "report/generate/project/id/"
											+ $scope.focusProject.id
											+ "/user/id/"
											+ $scope.currentUser.userName
											+ "/type/" + definition,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								}).success(function(data) {
							$rootScope.glassPane--;
							$scope.generatedReport = data;
						}).error(
								function(data, status, headers, config) {
									$rootScope.glassPane--;
									$scope.generatedReport = null;
									$rootScope.handleHttpError(data, status,
											headers, config);
								});
					};

					$scope.toggleResultItems = function(reportResult) {

						console.debug("Toggling report result, id = "
								+ reportResult.id);
						// if open, simply close
						if (reportResult.isCollapsed == false) {
							console.debug("--> Closing");
							reportResult.isCollapsed = true;

							// if closed, re-open and get result items if
							// necessary
						} else {
							console.debug("--> Opening");

							reportResult.isCollapsed = false;
							if (reportResult.reportResultItems == null) {
								console.debug("--> Retrieving new page");
								reportResult = $scope.getResultItems(
										reportResult, reportResult.page);

							}

						}
					};
					// if closed, open
					$scope.getResultItems = function(reportResult, page) {

						$rootScope.glassPane++;

						// construct a PFS object
						var pfsParameterObj = {
							"startIndex" : (page - 1) * $scope.itemsPerPage,
							"maxResults" : $scope.itemsPerPage,
							"sortField" : null,
							"queryRestriction" : null
						};

						// obtain the reports
						$http(
								{
									url : root_reporting + "reportResult/id/"
											+ reportResult.id + "/items",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {
											$rootScope.glassPane--;
											reportResult.reportResultItems = data.reportResultItem;
											reportResult.page = page;
											reportResult.nPages = Math
													.ceil(reportResult.ct
															/ $scope.itemsPerPage);

											return reportResult;
										})
								.error(
										function(data, status, headers, config) {
											$rootScope.glassPane--;
											reportResult.reportResultItems = null;
											$rootScope.handleHttpError(data,
													status, headers, config);
											return null;
										});
					}

					$scope.getItemUrl = function(reportResultItem) {

						console.debug("Getting item url", reportResultItem)

						switch (reportResultItem.resultType) {
						case 'CONCEPT':
							return '/record/conceptId/' + resultType.itemId;
						case 'MAP_RECORD':
							return '/record/conceptId/' + resultType.itemId;
						default:
							return null;

						}
					};

					var initializeCollapsed = function(report) {
						for (var i = 0; i < report.results.length; i++) {
							report.results[i].isCollapsed = true;
							report.results[i].reportResultItems = null;
							report.results[i].page = 1;
							report.results[i].nPages = Math
									.ceil(report.results[i].ct
											/ $scope.itemsPerPage);
						}
					};

					$scope.saveDefinition = function(definition) {

						$rootScope.glassPane++;
						console.debug("Definition", definition);
						// add or update based on whether definition has a
						// hibernate id
						$http(
								{
									url : root_reporting
											+ "definition/"
											+ (definition.id != null ? "update"
													: "add"),
									method : "POST",
									dataType : "json",
									data : definition,
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {
											$rootScope.glassPane--;
											$scope.definitionMsg = "Successfully saved definition";

											// if new report, and selected to
											// add to project, update project
											if ($scope.isAddingDefinition == true
													&& $scope.addDefinitionToProject == true) {
												
												console.debug("Also adding to project");
												$rootScope.glassPane++;

												// add this definition to
												// locally cached project
												$scope.focusProject.reportDefinition
														.push(definition);
												
												

												// update the project
												$http(
														{
															url : root_mapping
																	+ "project/update",
															method : "POST",
															dataType : "json",
															data : $scope.focusProject,
															headers : {
																"Content-Type" : "application/json"
															}
														})
														.success(
																function(data) {
																	$rootScope.glassPane--;
																	$scope.definitionMsg = "Successfully updated project";
																})
														.error(
																function(
																		data,
																		status,
																		headers,
																		config) {
																	$rootScope.glassPane--;
																	$rootScope
																			.handleHttpError(
																					data,
																					status,
																					headers,
																					config);
																});
											}

											$scope.isAddingDefinition = false;
										})
								.error(
										function(data, status, headers, config) {
											$rootScope.glassPane--;
											$rootScope.handleHttpError(data,
													status, headers, config);
										});
					};

					// @Path("/report/generate/project/id/{projectId}/user/id/{userName}")

					$scope.generateNewReport = function(reportDefinition) {
						$rootScope.glassPane++;

						// obtain the record
						$http(
								{
									url : root_reporting
											+ "report/generate/project/id/"
											+ $scope.focusProject.id
											+ "/user/id/"
											+ $scope.currentUser.userName,
									method : "POST",
									dataType : "json",
									data : reportDefinition,
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {
											$rootScope.glassPane--;
											$scope.reportDisplayed = data;
											$scope.definitionMsg = "Successfully saved definition";
										})
								.error(
										function(data, status, headers, config) {
											$rootScope.glassPane--;
											$rootScope.handleHttpError(data,
													status, headers, config);
										});
					};

					$scope.saveNewReport = function(report) {
						$rootScope.glassPane++;

						// obtain the record
						$http({
							url : root_reporting + "report/add",
							method : "POST",
							dataType : "json",
							data : report,
							headers : {
								"Content-Type" : "application/json"
							}
						}).success(function(data) {
							$rootScope.glassPane--;
							$scope.reportDisplayed = data;
							$scope.definitionMsg = "Successfully saved report";
						}).error(
								function(data, status, headers, config) {
									$rootScope.glassPane--;
									$rootScope.handleHttpError(data, status,
											headers, config);
								});

					};

					$scope.exportReport = function(report) {
						alert("Export function still in development");
					};

					$scope.addReportDefinition = function() {

						console.debug("Adding new report definition");

						var definition = {
							"id" : null,
							"name" : "(New Report)",
							"query" : null,
							"queryType" : null,
							"resultType" : null,
							"roleRequired" : null,
							"isDiffReport" : false,
							"timePeriod" : null
						};

						$scope.definitionEditing = definition;
						$scope.isAddingDefinition = true;

					};

				});
