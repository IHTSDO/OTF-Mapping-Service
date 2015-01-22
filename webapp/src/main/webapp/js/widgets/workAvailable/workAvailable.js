'use strict';

angular
		.module('mapProjectApp.widgets.workAvailable', [ 'adf.provider' ])
		.config(function(dashboardProvider) {

			dashboardProvider.widget('workAvailable', {
				title : 'Concepts',
				description : 'Module to assign work to users',
				controller : 'workAvailableWidgetCtrl',
				templateUrl : 'js/widgets/workAvailable/workAvailable.html',
				resolve : {},
				edit : {}
			});
		})

		.controller(
				'workAvailableWidgetCtrl',
				function($scope, $rootScope, $http, $routeParams, $modal, $location,
						localStorageService) {

					// local variables
					$scope.batchSizes = [ 100, 50, 25, 10, 5 ];
					$scope.batchSize = $scope.batchSizes[2];
					$scope.batchSizeConflict = $scope.batchSizes[4];
					$scope.batchSizeReview = $scope.batchSizes[4];

					// pagination variables
					$scope.itemsPerPage = 10;
					$scope.availableWorkPage = 1;
					$scope.availableConflictsPage = 1;
					$scope.availableReviewPage = 1;
					$scope.availableQAPage = 1;

					// initial titles
					$scope.availableWorkTitle = "Concepts";
					$scope.availableConflictsTitle = "Conflicts";
					$scope.availableReviewWorkTitle = "Review";
					$scope.availableQAWorkTitle = "QA";

					// retrieve focus project, current user, and current role
					$scope.focusProject = localStorageService.get('focusProject');
					$scope.currentUser = localStorageService.get('currentUser');
					$scope.currentRole = localStorageService.get('currentRole');
					$scope.userToken = localStorageService.get('userToken');
					$scope.availableTab = localStorageService.get('availableTab');
					$scope.isConceptListOpen = false;
					$scope.queryAvailable = null;
					
					// innitialize the search fields
					$scope.queryAvailableWork = null;
					$scope.queryAvailableReview = null;
					$scope.queryAvailableConflict = null;
					$scope.queryAvailableQaWork = null;
					$scope.queryAvailableWorkForUser = null;

					// intiialize the user list
					$scope.mapUsers = {};

					// tab variables, defaults to first active tab?
					$scope.tabs = [ {
						id : 0,
						title : 'Concepts',
						active : false
					}, {
						id : 1,
						title : 'Conflicts',
						active : false
					}, {
						id : 2,
						title : 'Review',
						active : false
					}, {
						id : 3,
						title : 'QA',
						active : false
					} ];

					// labels for QA filtering
					$scope.labelNames = [];

					// watch for project change and modify the local variable if necessary
					// coupled with $watch below, this avoids premature work fetching
					$scope
							.$on(
									'localStorageModule.notification.setFocusProject',
									function(event, parameters) {
										console
												.debug("WorkAvailableCtrl:  Detected change in focus project");
										$scope.focusProject = parameters.focusProject;
									});

					// on unassign notification, refresh the available work widget
					$scope
							.$on(
									'assignedListWidget.notification.unassignWork',
									function(event, parameters) {
										console
												.debug("WorkAvailableCtrl:  Detected unassign work notification");
										console.debug($scope.queryAvailableWork, $scope.queryAvailableConflicts);
										$scope.retrieveAvailableWork(1, $scope.queryAvailableWork);
										$scope.retrieveAvailableQAWork(1, $scope.queryAvailableQaWork);
										if ($scope.currentRole === 'Lead'
												|| $scope.currentRole === 'Admin') {
											$scope
													.retrieveAvailableConflicts(1, $scope.queryAvailableConflicts);
											$scope
													.retrieveAvailableReviewWork(1, $scope.queryAvailableReviewWork);
										}
									});

					// on computation of workflow, refresh the available work widget
					$scope
							.$on(
									'mapProjectWidget.notification.workflowComputed',
									function(event, parameters) {
										console
												.debug("WorkAvailableCtrl:  Detected recomputation of workflow");
										$scope.retrieveAvailableWork($scope.availableWorkPage);
										$scope.retrieveAvailableQAWork($scope.availableQAWorkPage);
										if ($scope.currentRole === 'Lead'
												|| $scope.currentRole === 'Admin') {
											$scope
													.retrieveAvailableConflicts($scope.availableConflictsPage);
											$scope
													.retrieveAvailableReviewWork($scope.availableReviewWorkPage);
										}
									});

					// on creation of qa work, refresh the available work widget
					$scope.$on('qaCheckWidget.notification.qaWorkCreated', function(
							event, parameters) {
						console.debug("WorkAvailableCtrl:  Detected new qa work");
						$scope.retrieveAvailableQAWork($scope.availableQAWorkPage);
					});

					// watch for first retrieval of last tab for this session
					$scope.$watch('availableTab', function() {
						console.debug('availableTab retrieved', $scope.availableTab);

						// unidentified source is resetting the tab to 0 after initial load
						// introduced a brief timeout to ensure correct tab is picked
						setTimeout(function() {
							$scope.setTab($scope.availableTab);
						}, 200);

					});

					$scope.setTab = function(tabNumber) {
						if (tabNumber == null)
							tabNumber = 0;
						console.debug('Setting tab', tabNumber);
						angular.forEach($scope.tabs, function(tab) {
							tab.active = (tab.id == tabNumber ? true : false);
						});
						localStorageService.add('availableTab', tabNumber);

					};

					$scope.addAssignment = function(name) {
						alert(name);
						return;
					};

					// on retrieval, set the user drop-down lists to the current user
					$scope.$watch([ 'currentUser' ], function() {
						console.debug('user changed');
						$scope.assignedMapUser = $scope.currentUser;
						$scope.assignedMapLead = $scope.currentUser;
					});

					// on retrieval of either focus project or user token, try to retrieve
					// work
					$scope
							.$watch(
									[ 'focusProject', 'userToken', 'currentUser', 'currentRole' ],
									function() {
										console
												.debug('workAvailableWidget:  scope project changed!');

										// both variables must be non-null
										if ($scope.focusProject != null && $scope.userToken != null
												&& $scope.currentUser != null
												&& $scope.currentRole != null) {

											// set the authorization header
											$http.defaults.headers.common.Authorization = $scope.userToken;

											// construct the list of users
											$scope.mapUsers = $scope.focusProject.mapSpecialist
													.concat($scope.focusProject.mapLead);
											console.debug('Project Users:');
											console.debug($scope.projectUsers);

											$scope.retrieveLabels();
											$scope.retrieveAvailableWork($scope.availableWorkPage);
											$scope
													.retrieveAvailableQAWork($scope.availableQAWorkPage);
											if ($scope.currentRole === 'Lead'
													|| $scope.currentRole === 'Admin') {
												$scope
														.retrieveAvailableConflicts($scope.availableConflictsPage);
												$scope
														.retrieveAvailableReviewWork($scope.availableReviewWorkPage);
											}
										}
									});

					$scope.retrieveLabels = function() {
						console.debug('workAvailableCtrl: Retrieving labels');

						$rootScope.glassPane++;
						$http({
							url : root_reporting + "qaLabel/qaLabels",
							dataType : "json",
							method : "GET",
							headers : {
								"Content-Type" : "application/json"
							}
						}).success(function(data) {
							console.debug("Success in getting qa labels.");

							$rootScope.glassPane--;
							for (var i = 0; i < data.searchResult.length; i++) {
								$scope.labelNames.push(data.searchResult[i].value);
							}
						}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;
							$rootScope.handleHttpError(data, status, headers, config);
						});
					};

					$scope.retrieveAvailableConflicts = function(page, query, user) {
						console.debug('workAvailableCtrl: Retrieving available conflicts');

						// clear local conflict error message
						$scope.errorConflict = null;

						// if user not supplied, assume current user
						if (user == null || user == undefined)
							user = $scope.currentUser;

						// clear the existing work
						$scope.availableConflicts = null;

						// set query to null if undefined
						if (query == undefined)
							query = null;

						// if null query, reset the search field
						if (query == null)
							$scope.queryAvailable = null;

						// construct a paging/filtering/sorting object
						var pfsParameterObj = {
							"startIndex" : (page - 1) * $scope.itemsPerPage,
							"maxResults" : $scope.itemsPerPage,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;

						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/user/id/" + user.userName + "/query/"
											+ (query == null ? "null" : encodeURIComponent(query))
											+ "/availableConflicts",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								}).success(
								function(data) {
									$rootScope.glassPane--;

									console.debug("Retrieve conflicts", data);

									$scope.availableConflicts = data.searchResult;

									// set pagination
									$scope.nAvailableConflicts = data.totalCount;
									$scope.numAvailableConflictsPages = Math.ceil(data.totalCount
											/ $scope.itemsPerPage);

									// set title
									$scope.tabs[1].title = "Conflicts (" + data.totalCount + ")";
								}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;

							$rootScope.handleHttpError(data, status, headers, config);
						});
					};

					// get a page of available work
					$scope.retrieveAvailableWork = function(page, query, user) {
						console.debug('workAvailableCtrl: Retrieving available work');

						// clear local error
						$scope.error = null;

						// if user not supplied, assume current user
						if (user == null || user == undefined)
							user = $scope.currentUser;

						// clear the existing work
						$scope.availableWork = null;

						// set query to null if undefined
						if (query == undefined)
							query = null;

						// if null query, reset the search field
						if (query == null)
							$scope.queryAvailable = null;

						// construct a paging/filtering/sorting object
						var pfsParameterObj = {
							"startIndex" : (page - 1) * $scope.itemsPerPage,
							"maxResults" : $scope.itemsPerPage,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;

						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/user/id/" + user.userName + "/query/"
											+ (query == null ? "null" : encodeURIComponent(query)) + "/availableConcepts",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								}).success(
								function(data) {
									$rootScope.glassPane--;

									console.debug(data);

									$scope.availableWork = data.searchResult;

									// set pagination
									$scope.nAvailableWork = data.totalCount;
									$scope.numAvailableWorkPages = Math.ceil(data.totalCount
											/ $scope.itemsPerPage);

									// set title
									$scope.tabs[0].title = "Concepts (" + data.totalCount + ")";
									console.debug($scope.numAvailableWorkPages);
									$scope.availableCount = data.totalCount;
									console.debug(data.totalCount);

								}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;
							$rootScope.handleHttpError(data, status, headers, config);
						});
					};

					$scope.retrieveAvailableQAWork = function(page, query) {
						console.debug('workAvailableCtrl: Retrieving available qa work');

						// clear local error
						$scope.error = null;

						// clear the existing work
						$scope.availableQAWork = null;

						// set query to null if undefined
						if (query == undefined)
							query = null;

						// if null query, reset the search field
						if (query == null)
							$scope.queryAvailable = null;

						// construct a paging/filtering/sorting object
						// if page is null, get all results
						var pfsParameterObj = {
							"startIndex" : (page - 1) * $scope.itemsPerPage,
							"maxResults" : $scope.itemsPerPage,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;

						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/query/" + (query == null ? "null" : encodeURIComponent(query))
											+ "/availableQAWork",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								}).success(
								function(data) {
									$rootScope.glassPane--;

									console.debug(data);

									$scope.availableQAWork = data.searchResult;

									// set pagination
									$scope.nAvailableQAWork = data.totalCount;
									$scope.numAvailableQAWorkPages = Math.ceil(data.totalCount
											/ $scope.itemsPerPage);

									// set title
									$scope.tabs[3].title = "QA (" + data.totalCount + ")";
									console.debug($scope.numAvailableQAWorkPages);

									// set labels
									for (var i = 0; i < $scope.availableQAWork.length; i++) {
										var concept = $scope.availableQAWork[i];

										$scope.availableQAWork[i].name = concept.value;
										$scope.availableQAWork[i].labels = concept.value2.replace(
												/;/g, ' ');
									}

								}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;
							$rootScope.handleHttpError(data, status, headers, config);
						});
					};

					$scope.removeQaWork = function(conceptId, query, page) {
						console.debug('workAvailableCtrl: Removing qa work for '
								+ conceptId);

						$rootScope.glassPane++;

						// clear local error
						$scope.error = null;

						// call unassign for the QA user
						// TODO: Get QA user name from cached metadata or a separate REST
						// call
						$http(
								{
									url : root_workflow + "unassign/project/id/"
											+ $scope.focusProject.id + "/concept/id/" + conceptId
											+ "/user/id/qa",
									dataType : "json",
									data : null,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								}).success(function(data) {

							$scope.retrieveAvailableQAWork(page, query)

							$rootScope.glassPane--;

						}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;
							$rootScope.handleHttpError(data, status, headers, config);
						});
					};

					$scope.removeAllQaWork = function(query) {
						console.debug('workAvailableCtrl: Removing available qa work');

						$rootScope.glassPane++;

						// clear local error
						$scope.error = null;

						// clear the existing work
						$scope.availableQAWork = null;

						// set query to null if undefined
						if (query == undefined)
							query = null;

						// if null query, reset the search field
						if (query == null)
							$scope.queryAvailable = null;

						// construct a blank paging/filtering/sorting object
						// unnecessary construction, but left in for possible future use
						var pfsParameterObj = {
							"startIndex" : -1,
							"maxResults" : -1,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						// first, get the currently available work (refresh)
						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/query/" + (query == null ? "null" : encodeURIComponent(query))
											+ "/availableQAWork",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								}).success(
								function(data) {

									console.debug(data);

									var workToUnassign = [];
									for (var i = 0; i < data.searchResult.length; i++) {
										workToUnassign.push(data.searchResult[i].terminologyId);
									}

									$http(
											{
												// TODO Get qa user name from either previously
												// retrieved metadata
												// or a separate REST call
												url : root_workflow + "unassign/project/id/"
														+ $scope.focusProject.id + "/user/id/qa/batch",
												dataType : "json",
												data : workToUnassign,
												method : "POST",
												headers : {
													"Content-Type" : "application/json"
												}
											}).success(function(data) {

										$scope.retrieveAvailableQAWork(1, query);

										$rootScope.glassPane--;

									}).error(function(data, status, headers, config) {
										$rootScope.glassPane--;
										$rootScope.handleHttpError(data, status, headers, config);
									});

								}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;
							$rootScope.handleHttpError(data, status, headers, config);
						});
					};

					$scope.retrieveAvailableReviewWork = function(page, query, user) {
						console
								.debug('************* workAvailableCtrl: Retrieving available review work');

						// clear local review error message
						$scope.errorReview = null;

						// if user not supplied, assume current user
						if (user == null || user == undefined)
							user = $scope.currentUser;

						// clear the existing work
						$scope.availableReviewWork = null;

						// set query to null if undefined
						if (query == undefined)
							query = null;

						// if null query, reset the search field
						if (query == null)
							$scope.queryAvailable = null;

						// construct a paging/filtering/sorting object
						var pfsParameterObj = {
							"startIndex" : (page - 1) * $scope.itemsPerPage,
							"maxResults" : $scope.itemsPerPage,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;

						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/user/id/" + user.userName + "/query/"
											+ (query == null ? "null" : encodeURIComponent(query))
											+ "/availableReviewWork",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								}).success(
								function(data) {
									$rootScope.glassPane--;

									console.debug("Retrieve reviews", data);

									$scope.availableReviewWork = data.searchResult;

									// set pagination
									$scope.nAvailableReviewWork = data.totalCount;
									$scope.numAvailableReviewWorkPages = Math
											.ceil(data.totalCount / $scope.itemsPerPage);

									// set title
									$scope.tabs[2].title = "Review (" + data.totalCount + ")";
								}).error(function(data, status, headers, config) {
							$rootScope.glassPane--;

							$rootScope.handleHttpError(data, status, headers, config);
						});
					};

					/**
					 * assign a single concept to the current user Arguments: -
					 * trackingRecord: the full search result object representing the
					 * tracking record - mapUser: the full user object representing the
					 * user to assign to - query: the query for this type of work, passed
					 * in to ensure correct refresh of available work - workType: the type
					 * of work ('concept', 'conflict', 'review', 'qa'), used for
					 * broadcasting assignment query passed in to ensure correct retrieval
					 * of work
					 */
					$scope.assignWork = function(trackingRecord, mapUser, query,
							workType, workPage) {

						console.debug('assignWork called');
						console.debug(trackingRecord);
						console.debug(mapUser);
						console.debug(query);
						console.debug(workType);
						console.debug(workPage);
						;
						// doublecheck map user and query, assign default values if
						// necessary
						if (mapUser == null)
							mapUser = $scope.currentUser;
						if (query == undefined)
							query = null;

						$rootScope.glassPane++;

						$http(
								{
									url : root_workflow + "assign/project/id/"
											+ $scope.focusProject.id + "/concept/id/"
											+ trackingRecord.terminologyId + "/user/id/"
											+ mapUser.userName,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {
											$rootScope.glassPane--;
											$rootScope.$broadcast(
													'workAvailableWidget.notification.assignWork', {
														assignUser : mapUser,
														assignType : workType
													});

											console.debug($scope.availableWorkPage,
													$scope.availableConflictsPage,
													$scope.availableReviewWorkPage,
													$scope.availableQAWorkPage);
											if (workType == 'concept') {
												$scope.retrieveAvailableWork(workPage, query, mapUser);
											} else if (workType === 'conflict') {
												$scope.retrieveAvailableConflicts(workPage, query,
														mapUser);
											} else if (workType === 'review') {
												$scope.retrieveAvailableReviewWork(workPage, query,
														mapUser);
											} else if (workType === 'qa') {
												$scope
														.retrieveAvailableQAWork(workPage, query, mapUser);
											}
										}).error(function(data, status, headers, config) {
									$rootScope.glassPane--;
									$rootScope.handleHttpError(data, status, headers, config);
								});

					};

					// assign a batch of records to the current user
					$scope.assignBatch = function(mapUser, batchSize, query) {
						
						console.debug("workAvailable, assignBatch:", mapUser, batchSize, query);

						// set query to null string if not provided
						if (query == undefined)
							query == null;

						if (mapUser == null || mapUser == undefined) {
							$scope.error = "Work recipient must be selected from list.";
							return;
						}
						;

						if (batchSize > $scope.availableCount) {
							$scope.error = "Batch size is greater than available number of concepts.";
							return;
						} else {
							$scope.error = null;
						}

						// construct a paging/filtering/sorting object
						var pfsParameterObj = {
							"startIndex" : ($scope.availableWorkPage - 1)
									* $scope.itemsPerPage,
							"maxResults" : batchSize,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;
						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/user/id/" + mapUser.userName + "/query/"
											+ (query == null ? 'null' : query) + "/availableConcepts",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {

											console
													.debug("Claim batch:  Checking against viewed concepts");

											var trackingRecords = data.searchResult;
											var conceptListValid = true;

											console.debug(trackingRecords);
											console.debug($scope.availableWork);

											// if user is assigning to self, check that first result
											// matches
											// first displayed result
											if ($scope.currentUser.userName === mapUser.userName) {
												for (var i = 0; i < $scope.itemsPerPage
														&& i < batchSize; i++) {
													console.debug(trackingRecords[i]);
													console.debug($scope.availableWork[i]);
													if (trackingRecords[i].id != $scope.availableWork[i].id) {
														retrieveAvailableWork($scope.availableWorkPage,
																query);
														alert("The list of available concepts has changed.  Please check the refreshed list and try again");
														conceptListValid = false;
													}
												}
											}

											if (conceptListValid == true) {
												console.debug("Claiming batch of size: " + batchSize);

												var terminologyIds = [];
												for (var i = 0; i < trackingRecords.length; i++) {

													terminologyIds.push(trackingRecords[i].terminologyId);
													console.debug('  -> Concept '
															+ trackingRecords[i].terminologyId);
												}

												console.debug("Calling batch assignment API");

												$http(
														{
															url : root_workflow + "assignBatch/project/id/"
																	+ $scope.focusProject.id + "/user/id/"
																	+ mapUser.userName,
															dataType : "json",
															data : terminologyIds,
															method : "POST",
															headers : {
																"Content-Type" : "application/json"
															}
														})
														.success(
																function(data) {
																	$rootScope.glassPane--;

																	// notify other widgets of work assignment
																	$rootScope
																			.$broadcast(
																					'workAvailableWidget.notification.assignWork',
																					{
																						assignUser : mapUser,
																						assignType : 'concept'
																					});

																	// refresh the available work list
																	$scope.retrieveAvailableWork(1, query,
																			mapUser);
																})
														.error(
																function(data, status, headers, config) {
																	$rootScope.glassPane--;

																	$rootScope.handleHttpError(data, status,
																			headers, config);
																	console
																			.debug("Could not retrieve available work when assigning batch.");
																});
											} else {
												console.debug("Unexpected error in assigning batch");
											}
										}).error(function(data, status, headers, config) {
									$rootScope.glassPane--;

									$rootScope.handleHttpError(data, status, headers, config);
								});

					};

					// assign a batch of conflicts to the current user
					$scope.assignBatchConflict = function(mapUser, batchSize, query) {

						console.debug("workAvailable, assignBatchConflict", mapUser, batchSize, query);
						
						// set query to null string if not provided
						if (query == undefined)
							query == null;

						if (mapUser == null || mapUser == undefined) {
							$scope.errorConflict = "Work recipient must be selected from list.";
							return;
						}
						

						// construct a paging/filtering/sorting object
						var pfsParameterObj = {
							"startIndex" : ($scope.availableWorkPage - 1)
									* $scope.itemsPerPage,
							"maxResults" : batchSize,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;
						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/user/id/" + mapUser.userName + "/query/"
											+ (query == null ? 'null' : query)
											+ "/availableConflicts",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {

											console
													.debug("Claim batch:  Checking against viewed conflicts");

											var trackingRecords = data.searchResult;
											var conceptListValid = true;

											console.debug(trackingRecords);
											console.debug($scope.availableConflicts);

											// if user is viewing conflicts, confirm that the returned
											// batch
											// matches the displayed conflicts
											if ($scope.currentUser.userName === mapUser.userName) {
												for (var i = 0; i < $scope.itemsPerPage
														&& i < batchSize && i < $scope.availableConflicts; i++) {
													console.debug(trackingRecords[i]);
													console.debug($scope.availableWork[i]);
													if (trackingRecords[i].id != $scope.availableWork[i].id) {
														retrieveAvailableWork($scope.availableWorkPage,
																query);
														alert("The available conflicts list has changed since loading.  Please review the new available conflicts and try again.");
														$scope.isConceptListOpen = false;
														conceptListValid = false;
													}
												}
											}

											if (conceptListValid == true) {
												console.debug("Claiming conflict batch of size: "
														+ batchSize);

												var terminologyIds = [];
												for (var i = 0; i < trackingRecords.length; i++) {

													terminologyIds.push(trackingRecords[i].terminologyId);
													console.debug('  -> Conflict '
															+ trackingRecords[i].terminologyId);
												}

												console.debug("Calling batch assignment API");

												$http(
														{
															url : root_workflow + "assignBatch/project/id/"
																	+ $scope.focusProject.id + "/user/id/"
																	+ mapUser.userName,
															dataType : "json",
															data : terminologyIds,
															method : "POST",
															headers : {
																"Content-Type" : "application/json"
															}
														})
														.success(
																function(data) {
																	$rootScope.glassPane--;

																	// broadcast the work assignment
																	$rootScope
																			.$broadcast(
																					'workAvailableWidget.notification.assignWork',
																					{
																						assignUser : mapUser,
																						assignType : 'conflict'
																					});

																	// refresh the displayed list of conflicts
																	$scope.retrieveAvailableConflicts(1, query,
																			mapUser);
																})
														.error(
																function(data, status, headers, config) {
																	$rootScope.glassPane--;

																	$rootScope.handleHttpError(data, status,
																			headers, config);
																	console
																			.debug("Could not retrieve available work when assigning batch.");
																});
											} else {
												$rootScope.glassPane--;
												console.debug("Unexpected error in assigning batch");
											}
										}).error(function(data, status, headers, config) {
									$rootScope.glassPane--;

									$rootScope.handleHttpError(data, status, headers, config);
								});

					};

					// assign a batch of review work to the current user
					$scope.assignBatchReview = function(mapUser, batchSize, query) {

						// set query to null string if not provided
						if (query == undefined)
							query == null;

						if (mapUser == null || mapUser == undefined) {
							$scope.errorReview = "Work recipient must be selected from list.";
							return;
						}

						// construct a paging/filtering/sorting object
						var pfsParameterObj = {
							"startIndex" : ($scope.availableWorkPage - 1)
									* $scope.itemsPerPage,
							"maxResults" : batchSize,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;
						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/user/id/" + mapUser.userName + "/query/"
											+ (query == null ? 'null' : query)
											+ "/availableReviewWork",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {

											console
													.debug("Claim batch:  Checking against viewed review work");

											var trackingRecords = data.searchResult;
											var conceptListValid = true;

											// if user is viewing conflicts, confirm that the returned
											// batch
											// matches the displayed conflicts
											if ($scope.currentUser.userName === mapUser.userName) {
												for (var i = 0; i < $scope.itemsPerPage
														&& i < batchSize && i < $scope.availableReviewWork; i++) {

													if (trackingRecords[i].id != $scope.availableReviewWork[i].id) {
														retrieveAvailableWork($scope.availableWorkPage,
																query);
														alert("The list of available review work has changed.  Please check the refreshed list and try again.");
														$scope.isConceptListOpen = false;
														conceptListValid = false;
													}
												}
											}

											if (conceptListValid == true) {
												console.debug("Claiming review work batch of size: "
														+ batchSize);

												var terminologyIds = [];
												for (var i = 0; i < trackingRecords.length; i++) {

													terminologyIds.push(trackingRecords[i].terminologyId);
													console.debug('  -> Review '
															+ trackingRecords[i].terminologyId);
												}

												console.debug("Calling batch assignment API");

												$http(
														{
															url : root_workflow + "assignBatch/project/id/"
																	+ $scope.focusProject.id + "/user/id/"
																	+ mapUser.userName,
															dataType : "json",
															data : terminologyIds,
															method : "POST",
															headers : {
																"Content-Type" : "application/json"
															}
														})
														.success(
																function(data) {
																	$rootScope.glassPane--;

																	// broadcast the work assignment
																	$rootScope
																			.$broadcast(
																					'workAvailableWidget.notification.assignWork',
																					{
																						assignUser : mapUser,
																						assignType : 'review'
																					});

																	// refresh the displayed list of conflicts
																	$scope.retrieveAvailableReviewWork(1, query,
																			mapUser);
																})
														.error(
																function(data, status, headers, config) {
																	$rootScope.glassPane--;

																	$rootScope.handleHttpError(data, status,
																			headers, config);
																	console
																			.debug("Could not retrieve available review work when assigning batch.");
																});
											} else {
												$rootScope.glassPane--;
												console
														.debug("Unexpected error in assigning review batch");
											}
										}).error(function(data, status, headers, config) {
									$rootScope.glassPane--;

									$rootScope.handleHttpError(data, status, headers, config);
								});

					};

					// assign a batch of qa work to the current user
					$scope.assignBatchQA = function(mapUser, batchSize, query) {

						// set query to null string if not provided
						if (query == undefined)
							query == null;

						if (mapUser == null || mapUser == undefined) {
							$scope.errorReview = "Work recipient must be selected from list.";
							return;
						}

						// construct a paging/filtering/sorting object
						var pfsParameterObj = {
							"startIndex" : ($scope.availableQAWorkPage - 1)
									* $scope.itemsPerPage,
							"maxResults" : batchSize,
							"sortField" : 'sortKey',
							"queryRestriction" : null
						};

						$rootScope.glassPane++;
						$http(
								{
									url : root_workflow + "project/id/" + $scope.focusProject.id
											+ "/query/" + (query == null ? 'null' : query)
											+ "/availableQAWork",
									dataType : "json",
									data : pfsParameterObj,
									method : "POST",
									headers : {
										"Content-Type" : "application/json"
									}
								})
								.success(
										function(data) {

											console
													.debug("Claim batch:  Checking against viewed qa work");

											var trackingRecords = data.searchResult;
											var conceptListValid = true;

											// if user is viewing conflicts, confirm that the returned
											// batch
											// matches the displayed conflicts
											if ($scope.currentUser.userName === mapUser.userName) {
												for (var i = 0; i < $scope.itemsPerPage
														&& i < batchSize && i < $scope.availableQAWork; i++) {

													if (trackingRecords[i].id != $scope.availableQAWork[i].id) {
														retrieveAvailableQAWork($scope.availableQAWorkPage,
																query);
														alert("The list of available QA work has changed.  Please check the refreshed list and try again.");
														$scope.isConceptListOpen = false;
														conceptListValid = false;
													}
												}
											}

											if (conceptListValid == true) {
												console.debug("Claiming qa work batch of size: "
														+ batchSize);

												var terminologyIds = [];
												for (var i = 0; i < trackingRecords.length; i++) {

													terminologyIds.push(trackingRecords[i].terminologyId);
													console.debug('  -> Review '
															+ trackingRecords[i].terminologyId);
												}

												console.debug("Calling batch assignment API");

											
												$http(
														{
															url : root_workflow + "assignBatch/project/id/"
																	+ $scope.focusProject.id + "/user/id/"
																	+ mapUser.userName,
															dataType : "json",
															data : terminologyIds,
															method : "POST",
															headers : {
																"Content-Type" : "application/json"
															}
														})
														.success(
																function(data) {
																	$rootScope.glassPane--;

																	// broadcast the work assignment
																	$rootScope
																			.$broadcast(
																					'workAvailableWidget.notification.assignWork',
																					{
																						assignUser : mapUser,
																						assignType : 'qa'
																					});

																	// refresh the displayed list of qa items
																	$scope.retrieveAvailableQAWork(1, query,
																			mapUser);
																})
														.error(
																function(data, status, headers, config) {
																	$rootScope.glassPane--;

																	$rootScope.handleHttpError(data, status,
																			headers, config);
																	console
																			.debug("Could not retrieve available qa work when assigning batch.");
																});
											} else {
												console.debug("Unexpected error in assigning qa batch");
												$rootScope.glassPane--;
											}
										}).error(function(data, status, headers, config) {
									$rootScope.glassPane--;

									$rootScope.handleHttpError(data, status, headers, config);
								});

					};

					// remove an element from an array by key
					Array.prototype.removeElement = function(elem) {

						// field to switch on
						var idType = 'id';

						var array = new Array();
						$.map(this, function(v, i) {
							if (v[idType] != elem[idType])
								array.push(v);
						});

						this.length = 0; // clear original array
						this.push.apply(this, array); // push all elements except the one we
						// want to delete
					};

					// sort and return an array by string key
					function sortByKey(array, key) {
						return array.sort(function(a, b) {
							var x = a[key];
							var y = b[key];
							return ((x < y) ? -1 : ((x > y) ? 1 : 0));
						});
					}
					;

				});
