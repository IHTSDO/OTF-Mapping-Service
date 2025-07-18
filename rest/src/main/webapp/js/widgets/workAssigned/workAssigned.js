'use strict';

angular
  .module('mapProjectApp.widgets.workAssigned', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('workAssigned', {
      title : 'Assigned Work',
      description : 'Displays a list of assigned records',
      controller : 'workAssignedCtrl',
      templateUrl : 'js/widgets/workAssigned/workAssigned.html',
      edit : {}
    });
  })
  .controller(
    'workAssignedCtrl',
    function($scope, $rootScope, $http, $location, $uibModal, $timeout, localStorageService, utilService, appConfig, gpService) {

      // on initialization, explicitly assign to null and/or empty array
      $scope.currentUser = null;
      $scope.currentRole = null;
      $scope.focusProject = null;
      $scope.assignedTab = null;
      $scope.currentUserToken = null;
      $scope.preferences = null;
      $scope.assignedRecords = [];
      $scope.authorsList = [];

      // retrieve the necessary scope variables from local storage service
      $scope.currentUser = localStorageService.get('currentUser');
      $scope.currentRole = localStorageService.get('currentRole');
      $scope.focusProject = localStorageService.get('focusProject');
      $scope.currentUserToken = localStorageService.get('userToken');
      $scope.preferences = localStorageService.get('preferences');
      $scope.assignedTab = localStorageService.get('assignedTab');      
      $scope.tabs = localStorageService.get('assignedWorkTabs');

	  // MIMS Condition project wants default sort by preferred name
	  if ($scope.focusProject.projectSpecificAlgorithmHandlerClass == 'org.ihtsdo.otf.mapping.jpa.handlers.MIMSConditionToSnomedProjectSpecificAlgorithmHandler' ||
		  $scope.focusProject.projectSpecificAlgorithmHandlerClass == 'org.ihtsdo.otf.mapping.jpa.handlers.MIMSAllergyToSnomedProjectSpecificAlgorithmHandler') {
		$scope.defaultSortField = 'defaultPreferredName';
	  } else {
		$scope.defaultSortField = 'terminologyId';
	  }
      
      $scope.selectedTags = new Array();
      $scope.allTags = new Array();

      $scope.multiSelectSettings = {
        displayProp : 'tag',
        scrollableHeight : '150px',
        scrollable : true,
        showCheckAll : false,
        showUncheckAll : false
      };
      $scope.multiSelectCustomTexts = {
        buttonDefaultText : 'Select Tags'
      };

      if ($scope.tabs == null || $scope.tabs === '') {
        // tab variables
        $scope.tabs = [ {
          id : 0,
          title : 'Concepts',
          active : true,
          selection : null
        }, {
          id : 1,
          title : 'Conflicts',
          active : false,
          selection : null
        }, {
          id : 2,
          title : 'Review',
          active : false,
          selection : null
        }, {
          id : 3,
          title : 'By User',
          active : false,
          selection : null
        }, {
          id : 4,
          title : 'QA',
          active : false,
          selection : null
        } ];
      }
      
      // labels for QA filtering
      $scope.labelNames = [];

      // table sort fields - currently unused
      $scope.tableFields = [ {
        id : 0,
        title : 'id',
        sortDir : 'asc',
        sortOn : false
      } ];

      $scope.selected = {
        mapUserViewed : null
      };
      $scope.ownTab = true; // variable to track whether viewing own work or
      // other users work
      $scope.searchPerformed = false; // initialize variable to track
      // whether
      // search was performed

      $scope.sendFeedbackToAuthorEnabled = (appConfig["deploy.show.feedback.to.author.button"] === 'false') ? false : true;

      // function to change tab
      $scope.setTab = function(tabNumber) {
        if (tabNumber == null) {
          tabNumber = 0;
        }
        angular.forEach($scope.tabs, function(tab) {
          tab.active = (tab.id == tabNumber ? true : false);
        });

        // set flag for ByUser tab, i.e. whether viewing user's own work
        if (tabNumber == 3)
          $scope.ownTab = false;
        else
          $scope.ownTab = true;

        // add the tab to the local storage service for the next visit       
        $scope.preferences.lastAssignedTab = tabNumber;
        localStorageService.add('assignedTab', tabNumber);
        
        $scope.getRadio();
      };
            
      $scope.getRadio = function() {
    	// retrieve the user preferences
        $http({
            url : root_mapping + 'userPreferences/user/id/' + $scope.currentUser.userName,
            dataType : 'json',
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
        }).success(function(data) {
          $scope.preferences.lastAssignedRadio = localStorageService.get('assignedRadio');
                    
          $scope.assignedTypes.work = $scope.tabs[0].selection || 'ALL';
          $scope.assignedTypes.conflict = $scope.tabs[1].selection || 'ALL';
          $scope.assignedTypes.review = $scope.tabs[2].selection || 'ALL';
          $scope.assignedTypes.forUser = $scope.tabs[3].selection || 'ALL';
          $scope.assignedTypes.qa = $scope.tabs[4].selection || 'ALL';
          
          // update lists based on radio button selected when tab changes
          $scope.retrieveAssignedWork($scope.assignedWorkPage, null);
          $scope.retrieveAssignedQAWork(1, null);

          $scope.retrieveLabels();
		  $scope.retrieveTags();
          if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') { 
            $scope.retrieveAssignedReviewWork(1, null);
            $scope.retrieveAssignedConflicts(1, null);
            $scope.retrieveAssignedWorkForUser(1, $scope.selected?.mapUserViewed?.userName, null);
          }
        }); 
      }
      
      $scope.setRadio = function(tab, type) {
          // add the radio button to the local storage service for the next visit       
          $scope.preferences.lastAssignedRadio = type;
          localStorageService.add('assignedRadio', type);
          
          $scope.tabs[tab].selection = type;
          localStorageService.add('assignedWorkTabs', $scope.tabs);
          
          // update the user preferences
          $http({
            url : root_mapping + 'userPreferences/update',
            dataType : 'json',
            data : $scope.preferences,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            // do nothing

          }).error(function(data) {
            if (response.indexOf('HTTP Status 401') != -1) {
              $rootScope.globalError = 'Authorization failed.  Please log in again.';
              $location.path('/');
            }
          });
      }
      
      
      // pagination variables
      $scope.itemsPerPage = 10;
      $scope.assignedWorkPage = 1;
      $scope.assignedConflictsPage = 1;
      $scope.assignedReviewWorkPage = 1;
      $scope.assignedQAWorkPage = 1;
      $scope.assignedWorkForUserPage = 1;

      // query variables
      $scope.queryAssigned = null;
      $scope.queryConflict = null;
      $scope.queryReviewWork = null;
      $scope.queryQAWork = null;
      $scope.queryAssignedForUser = null;

      // work type filter variables
      $scope.assignedTypes = {};
     
      //sort direction
      var sortAscending = [];
      var sortField = [];
      
      $scope.getSortIndicator = function(table, field){
		if (sortField[table] !== field) return '';
		if (sortField[table] === field && sortAscending[table]) return '▴';
		if (sortField[table] === field && !sortAscending[table]) return '▾';
      };
      
    //sort field and get data
      $scope.setSortField = function(table, field) {
    	  sortAscending[table] = !sortAscending[table];
    	  sortField[table] = field;
    	  if (table === 'concepts') {
    		  $scope.retrieveAssignedWork(1, $scope.queryAssigned);
    	  } else if (table === 'conflicts') {
    		  $scope.retrieveAssignedConflicts(1, $scope.queryConflict);
    	  } else if (table === 'review') {
    		  $scope.retrieveAssignedReviewWork(1, $scope.queryReviewWork);
    	  } else if (table === 'user') {
    		  $scope.retrieveAssignedWorkForUser(1, $scope.selected.mapUserViewed.userName, 
    				  $scope.queryAssignedForUser);
    	  } else if (table === 'qa') {
    		  $scope.retrieveAssignedQAWork(1, $scope.queryQAWork);
    	  }
      };
      		
	  //UI Labels
	  $scope.getUILabel = function(label){
		if (utilService.checkIfProjectRelabels($scope.focusProject.refSetId, appConfig['deploy.ui.relabel.refsetids'])){
		  return utilService.getUILabel(label, appConfig['deploy.ui.relabel.' + $scope.focusProject.refSetId]);
		}
		else {
		  return label;
		}
	  }
		
      // watch for project change
      $scope.$on('localStorageModule.notification.setFocusProject', function(event, parameters) {
        $scope.focusProject = parameters.focusProject;
      });

      // watch for first retrieval of last tab for this session
      $scope.$watch('assignedTab', function() {
        // unidentified source is resetting the tab to 0 after initial load
        // introduced a brief timeout to ensure correct tab is picked
        setTimeout(function() {
          $scope.setTab($scope.assignedTab);
        }, 200);

      });

      // Event on assigned work being ready
      $scope.$on('workAvailableWidget.notification.assignWork', function(event, parameters) {
        // perform action based on notification parameters
        // Expect:
        // - assignUser: String, IHTSDO username (e.g. dmo, kli)
        // - assignType: String, either 'concept' or 'conflict' or
        // 'review' or 'qa'
        if ($scope.currentRole === 'Lead') {

          // if user name matches current user's user name, reload
          // work
          if (parameters.assignUser.userName === $scope.currentUser.userName) {

            if (parameters.assignType === 'concept') {

              // set the tab
              $scope.setTab(0);

              // retrieve the work
              $scope.retrieveAssignedWork($scope.assignedWorkPage, $scope.queryAssigned);

              // Conflicts
            } else if (parameters.assignType === 'conflict') {
              // set the tab
              $scope.setTab(1);

              // retrieve the work
              $scope.retrieveAssignedConflicts($scope.assignedConflictPage, $scope.queryConflict);

              // Review Work
            } else if (parameters.assignType === 'review') {
              // set the tab
              $scope.setTab(2);

              // retrieve the work
              $scope.retrieveAssignedReviewWork($scope.assignedReviewWorkPage,
                $scope.queryReviewWork);

            } else if (parameters.assignType === 'conceptsByUser') {

              // set the tab
              $scope.setTab(3);

              // set the user based on parameters
              angular.forEach($scope.mapUsers, function(mapUser) {
                if (mapUser.userName === parameters.assignUser.userName) {
                  $scope.selected.mapUserViewed = mapUser;
                }
              });

              // retrieve the work
              $scope.retrieveAssignedWorkForUser($scope.assignedWorkForUserPage,
                parameters.assignUser.userName, $scope.queryAssignedForUser);

              // QA Work
            } else if (parameters.assignType === 'qa') {
              // set the tab
              $scope.setTab(4);

              // retrieve the work
              $scope.retrieveAssignedQAWork($scope.assignedQAWorkPage, $scope.queryQAWork);
            }
          } else {

            // set the tab
            $scope.setTab(3);

            // set the user based on parameters
            angular.forEach($scope.mapUsers, function(mapUser) {
              if (mapUser.userName === parameters.assignUser.userName) {
                $scope.selected.mapUserViewed = mapUser;
              }
            });

            $scope.retrieveAssignedWorkForUser($scope.assignedWorkForUserPage,
              parameters.assignUser.userName, $scope.queryAssignedForUser);

          }

          // SPECIALIST TABS
        } else {

          if (parameters.assignType === 'concept') {

            // set the tab
            $scope.setTab(0);

            // retrieve the work
            $scope.retrieveAssignedWork($scope.assignedWorkPage, $scope.assignedWorkQuery);
          } else if (parameters.assignType === 'qa') {
            // set the tab
            $scope.setTab(4);

            // retrieve the work
            $scope.retrieveAssignedQAWork($scope.assignedQAWorkPage, $scope.assignedQAWorkQuery);
          }
        }
      });

      // on any change of relevant scope variables, retrieve work
      $scope.$watch([ 'focusProject', 'user', 'userToken', 'currentRole' ], function() {
        if ($scope.focusProject != null && $scope.currentUser != null
          && $scope.currentUserToken != null && $scope.currentRole != null) {
          $http.defaults.headers.common.Authorization = $scope.currentUserToken;
          
          $scope.getRadio();
          $scope.mapUsers = $scope.focusProject.mapSpecialist.concat($scope.focusProject.mapLead);
        }
      });

      $scope.retrieveAssignedConflicts = function(page, query) {

        // hard set the conflict PFS scope variables
        $scope.assignedConflictsPage = page;
        $scope.queryConflict = query;

        // ensure query is set to null if not specified
        if (query == undefined)
          query == null;

        // set global search performed varaiable based on query
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;
        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : (sortField['conflicts']) ? sortField['conflicts'] : $scope.defaultSortField,
          'ascending' : (sortAscending['conflicts']) ? sortAscending['conflicts'] : false,
          'queryRestriction' : $scope.assignedTypes.conflict
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedConflicts?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.assignedConflictsPage = page;
          $scope.assignedConflicts = data.searchResult;

          // set pagination
          $scope.numAssignedConflictsPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedConflicts = data.totalCount;

          // set title
          $scope.tabs[1].title = 'Conflicts (' + data.totalCount + ')';
          localStorageService.add('assignedWorkTabs', $scope.tabs);

        }).error(function(data, status, headers, config) {
          gpService.decrement();

          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedWork = function(page, pquery) {
        var query = pquery;
        // set the scope variables
        // this is necessary due to some frustrating non-functional two-way
        // binding
        $scope.assignedWorkPage = page;
        $scope.queryAssigned = query;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search input box if null
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;

        }

		// copy tag filter and add to query
		var pselectedTags = "";
		  for (var i = 0; i < $scope.selectedTags.length; i++) {
			for(var j = 0; j < $scope.allTags.length; j++) {
				if($scope.selectedTags[i].id == $scope.allTags[j].id){
					if(pselectedTags == ""){
						pselectedTags = 'tags:(\"' + $scope.allTags[j].tag + '\"';
					}
					else{
						pselectedTags = pselectedTags.concat(' OR "', $scope.allTags[j].tag,'\"');
					}
				}
			}
          }

		if (pselectedTags != "") {
			if (query != null && query != ""){
				query = query.concat(' AND ', pselectedTags,')');
			}
			else{
				query = pselectedTags + ')';
			}
		}

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : (sortField['concepts']) ? sortField['concepts'] : $scope.defaultSortField,
          'ascending' : (sortAscending['concepts']) ? sortAscending['concepts'] : false,
          'queryRestriction' : $scope.assignedTypes.work
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedConcepts?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.assignedWorkPage = page;
          $scope.assignedRecords = data.searchResult;

          // set pagination
          $scope.numAssignedRecordPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedRecords = data.totalCount;

          // set title
          $scope.tabs[0].title = 'Concepts (' + $scope.nAssignedRecords + ')';
          localStorageService.add('assignedWorkTabs', $scope.tabs);

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
       

      };

      $scope.retrieveLabels = function() {

        gpService.increment();
        $http({
          url : root_reporting + 'qaLabel/qaLabels/' + $scope.focusProject.id,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          gpService.decrement();
          for (var i = 0; i < data.searchResult.length; i++) {
            $scope.labelNames.push(data.searchResult[i].value);
          }

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveTags = function() {
        gpService.increment();
        $http({
          url : root_mapping + 'tags/' + $scope.focusProject.id,
          dataType : 'json',
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(function(data) {
          gpService.decrement();
	  		// 1. Initialize a temporary object to track seen tags.
		    //    The keys will be the 'tag' values, and the values can be anything (e.g., true, or the actual tag object).
		    const uniqueTagsMap = {};
		    // 2. Initialize a new array for the unique tags.
		    const newAllTagsArray = [];
		
		    for (var i = 0; i < data.searchResult.length; i++) {
		      const tagValue = data.searchResult[i].value;
		
		      // Check if we've already seen this tag value
		      if (!uniqueTagsMap[tagValue]) {
		        // If not, add it to our map and to the new array
		        uniqueTagsMap[tagValue] = true; // Mark as seen
		        newAllTagsArray.push({ id: i + 1, tag: tagValue });
		      }
		    }
		
		    // 3. Replace the old $scope.allTags with the newly generated unique array.
		    $scope.allTags = newAllTagsArray;
	
//          for (var i = 0; i < data.searchResult.length; i++) {
//			$scope.allTags.push({ id: i+1, tag: data.searchResult[i].value });
//          }
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.clearSelectedTags = function() {
		$scope.selectedTags = new Array();
	  };

      $scope.retrieveAssignedQAWork = function(page, pquery, type) {
        $scope.assignedQAWorkType = type;
        var query = pquery;
        // hard set the PFS variables
        $scope.assignedQAWorkPage = page;
        $scope.queryQAWork = query;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search input box if null
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;

        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : (sortField['qa']) ? sortField['qa'] : $scope.defaultSortField,
          'ascending' : (sortAscending['qa']) ? sortAscending['qa'] : false,
          'queryRestriction' : $scope.assignedTypes.qa
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedQAWork?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.assignedQAWorkPage = page;
          $scope.assignedQAWork = data.searchResult;

          // set pagination
          $scope.numAssignedQAWorkPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedQAWork = data.totalCount;

          // set title
          $scope.tabs[4].title = 'QA (' + $scope.nAssignedQAWork + ')';
          localStorageService.add('assignedWorkTabs', $scope.tabs);

          // set labels
          for (var i = 0; i < $scope.assignedQAWork.length; i++) {
            var concept = $scope.assignedQAWork[i];

            $scope.assignedQAWork[i].name = concept.value;
            $scope.assignedQAWork[i].labels = concept.value2.replace(/;/g, ' ');
          }

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedReviewWork = function(page, pquery) {
        var query = pquery;

        // hard set the PFS variables
        $scope.assignedReviewWorkPage = page;
        $scope.queryReviewWork = query;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search input box if null
        if (query == null) {
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;

        }
        
		// copy tag filter and add to query
		var pselectedTags = "";
		  for (var i = 0; i < $scope.selectedTags.length; i++) {
			for(var j = 0; j < $scope.allTags.length; j++) {
				if($scope.selectedTags[i].id == $scope.allTags[j].id){
					if(pselectedTags == ""){
						pselectedTags = 'tags:(\"' + $scope.allTags[j].tag + '\"';
					}
					else{
						pselectedTags = pselectedTags.concat(' OR "', $scope.allTags[j].tag,'\"');
					}
				}
			}
          }

		if (pselectedTags != "") {
			if (query != null && query != ""){
				query = query.concat(' AND ', pselectedTags,')');
			}
			else{
				query = pselectedTags + ')';
			}
		}

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : (sortField['review']) ? sortField['review'] : $scope.defaultSortField,
          'ascending' : (sortAscending['review']) ? sortAscending['review'] : false,
          'queryRestriction' : $scope.assignedTypes.review
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/assignedReviewWork?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.assignedReviewWorkPage = page;
          $scope.assignedReviewWork = data.searchResult;

          // set pagination
          $scope.numAssignedReviewWorkPages = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedReviewWork = data.totalCount;

          // set title
          $scope.tabs[2].title = 'Review (' + $scope.nAssignedReviewWork + ')';
          localStorageService.add('assignedWorkTabs', $scope.tabs);

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      $scope.retrieveAssignedWorkForUser = function(page, mapUserName, query) {

        // hard set the PFS variables
        $scope.assignedWorkForUserPage = page;
        $scope.queryAssignedForUser = query;

        // ensure query is set to null if undefined
        if (query == undefined)
          query = null;

        // reset the search box if query is null
        if (query == null) {
          $scope.queryAssignedForUser = null;
          $scope.searchPerformed = false;
        } else {
          $scope.searchPerformed = true;
        }

        // if no user specified, set to empty record set, with appropriate
        // pagination variables
        if (mapUserName == null) {
          $scope.assignedWorkForUserPage = 1;
          $scope.assignedRecordsForUser = {};

          // set pagination
          $scope.numAssignedRecordPagesForUser = 0;
          $scope.nAssignedRecordsForUser = 0;

          // set title
          $scope.tabs[3].title = 'By User';

          return;
        }

        // construct a paging/filtering/sorting object
        var pfsParameterObj = {
          'startIndex' : page == -1 ? -1 : (page - 1) * $scope.itemsPerPage,
          'maxResults' : page == -1 ? -1 : $scope.itemsPerPage,
          'sortField' : (sortField['user']) ? sortField['user'] : $scope.defaultSortField,
          'ascending' : (sortAscending['user']) ? sortAscending['user'] : false,
          'queryRestriction' : $scope.assignedTypes.forUser
        };

        gpService.increment();

        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + mapUserName + '/assignedConcepts?query=' + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          $scope.assignedWorkForUserPage = page;
          $scope.assignedRecordsForUser = data.searchResult;

          // set pagination
          $scope.numAssignedRecordPagesForUser = Math.ceil(data.totalCount / $scope.itemsPerPage);
          $scope.nAssignedRecordsForUser = data.totalCount;

          $scope.tabs[3].title = 'By User (' + data.totalCount + ')';
          localStorageService.add('assignedWorkTabs', $scope.tabs);

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      // on notification, update assigned work
      $scope.assignWork = function(newRecords) {

        $scope.retrieveAssignedWork($scope.assignedWorkPage);
        if ($scope.currentRole === 'Lead' || $scope.currentRole === 'Administrator') {
          $scope.retrieveAssignedConflicts($scope.assignedConflictsPage);
        }
      };

      // function to relinquish work (i.e. unassign the user)
      $scope.unassignWork = function(record, mapUser, workType) {
        // show a confirmation dialog if requested
        // NOTE: workflow status is contained in terminologyVersion for a
        // searchResult object
        if (record.terminologyVersion === 'EDITING_DONE'
          || record.terminologyVersion === 'REVIEW_RESOLVED'
          || record.terminologyVersion === 'QA_RESOLVED'
          || record.terminologyVersion === 'CONFLICT_RESOLVED') {
          var response = confirm('Are you sure you want to return finished work?  You will lose any work done.');
          if (response == false)
            return;
        }
        if (record.terminologyVersion.includes('IN_PROGRESS')) {
          var response = confirm('Are you sure you want to return in-progress work?  You will lose any work done.');
          if (response == false)
            return;
        }

        gpService.increment();
        $http(
          {
            url : root_workflow + 'unassign/project/id/' + $scope.focusProject.id + '/concept/id/'
              + record.terminologyId + '/user/id/' + mapUser.userName,
            dataType : 'json',
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

          // trigger reload of this type of work via broadcast
          // notification
          $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
            assignUser : mapUser,
            assignType : workType,
            resetFilters : false
          });

          $rootScope.$broadcast('workAssignedWidget.notification.unassignWork', {
            assignUser : mapUser,
            assignType : workType,
            resetFilters : false
          });

          // if this user is viewing their assigned concepts via the By User
          // tab, re-retrieve

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });
      };

      // Unassigns all currently viewed work (by tab, query, and workflow
      // status)
      // Parameters:
      // user: the map user to unassign (may not be the current user)
      // workType: the type of work, e.g. Concepts, Conflicts, Review...
      // workStatus: the currently selected workflow status, e.g. All, New,
      // Editing...
      // query: any text filter currently applied
      $scope.unassignAllWork = function(user, workType, workStatus, query) {

        if (confirm('Are you sure you want to return all displayed work?') == false)
          return;

        // get the full list of currently assigned work for this query and
        // workType
        gpService.increment();
        var pfsParameterObj = {
          'startIndex' : -1,
          'maxResults' : -1,
          'sortField' : $scope.defaultSortField,
          'queryRestriction' : workStatus
        };

        var workTypeText = null;
        switch (workType) {
        case 'concept':
          workTypeText = 'assignedConcepts';
          break;
        case 'conflict':
          workTypeText = 'assignedConflicts';
          break;
        case 'review':
          workTypeText = 'assignedReviewWork';
          break;
        case 'qa':
          workTypeText = 'assignedQAWork';
          break;
        }

        // retrieve the list of assigned work
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + user.userName + '/' + workTypeText + '?query='
              + encodeURIComponent(query ? query : ''),
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

          var terminologyIds = new Array();
          for (var i = 0; i < data.searchResult.length; i++) {
            terminologyIds.push(data.searchResult[i].terminologyId);
          }
          unassignBatch(user, terminologyIds, workType);

          gpService.decrement();

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      $scope.setOwnTab = function(ownTab) {
        $scope.ownTab = ownTab;
      };

      var unassignBatch = function(mapUser, terminologyIds, workType, workStatus) {

        gpService.increment();
        $http(
          {
            url : root_workflow + 'unassign/project/id/' + $scope.focusProject.id + '/user/id/'
              + mapUser.userName + '/batch',
            dataType : 'json',
            data : terminologyIds,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
          gpService.decrement();

        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        }).then(function() {

          // trigger reload of this type of work via broadcast
          // notification
          $rootScope.$broadcast('workAvailableWidget.notification.assignWork', {
            assignUser : mapUser,
            assignType : workType,
            assignWorkflowStatus : workStatus
          });

          $rootScope.$broadcast('workAssignedWidget.notification.unassignWork');

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

      $scope.goEditRecord = function(id) {
        var path = '/record/recordId/' + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditConflict = function(id) {
        var path = '/record/conflicts/' + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditReviewWork = function(id) {
        var path = '/record/review/' + id;
        // redirect page
        $location.path(path);
      };

      $scope.goEditQAWork = function(id) {
        var path = '/record/review/' + id;
        // redirect page
        $location.path(path);
      };

      // create JIRA issue ticket to send feedback to content author
      $scope.createJiraTicket = function(record) {
        // retrieve the list of authors
        gpService.increment();
        $http({
          url : root_mapping + 'authors/' + record.terminologyId,
          method : 'GET',
          headers : {
            'Content-Type' : 'application/json'
          }
        }).success(
          function(data) {
            gpService.decrement();

            // only put valid authors on list
            var searchResults = data.searchResult;
            $scope.authorsList = [];
            for (var i = 0; i < data.totalCount; i++) {
              if (searchResults[i].value != 'snowowl'
                && $scope.authorsList.indexOf(searchResults[i].value) == -1) {
                $scope.authorsList.push(searchResults[i].value);
              }
            }

            // Open modal that allows user to select author/recipient and compose jira issue
            $scope.openCreateJiraTicketModal(record);

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
          });
      };
      

      $scope.openCreateJiraTicketModal = function(record) {

        if (record == null) {
          return;
        }

        var modalInstance = $uibModal.open({
          templateUrl : 'js/widgets/workAssigned/createJiraTicket.html',
          controller : CreateJiraTicketModalCtrl,
          size : 'lg',
          resolve : {
            record : function() {
              return record;
            },
            project : function() {
              return $scope.focusProject;
            },
            user : function() {
              return $scope.currentUser;
            },
            authors : function() {
              return $scope.authorsList;
            }
          }
        });

        modalInstance.result.then(

        // called on Done clicked by user
        function() {
        })

      };
        

      var CreateJiraTicketModalCtrl = function($scope, $uibModalInstance, $q, user, project,
        record, authors) {

        $scope.user = user;
        $scope.project = project;
        $scope.record = record;
        $scope.authors = authors;
        $scope.selectedContentAuthor = $scope.authors[0];
        $scope.content = {};
        $scope.tinymceOptions = {
          menubar : false,
          statusbar : false,
          plugins : 'autolink link image charmap searchreplace lists paste',
          toolbar : 'undo redo | styleselect lists | bold italic underline strikethrough | charmap link image',
        }

        $scope.createJiraTicket = function() {
          $http(
            {
              url : root_mapping + 'jira/' + $scope.currentRecord.conceptId + '/'
                + $scope.selectedContentAuthor + '?messageText='
                + encodeURIComponent($scope.content.text ? $scope.content.text : ''),
              dataType : 'json',
              data : $scope.currentRecord,
              method : 'POST',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(function(data) {
            $uibModalInstance.close();

          }).error(function(data, status, headers, config) {
            $rootScope.handleHttpError(data, status, headers, config);
          });
        }
        
        // Load the current record
        $scope.loadRecord = function() {

          var deferred = $q.defer();

          gpService.increment();
          // perform the retrieval call
          $http({
            url : root_mapping + 'record/id/' + $scope.record.id,
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            gpService.decrement();
            // set scope record
            $scope.currentRecord = data;

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.error = 'Could not retrieve record';
            deferred.reject('Could not retrieve record');
          });

          return deferred.promise;
        };
        // get the  record
        $scope.loadRecord();
      }

      /**
       * Helper function to open Finish Or Publish modal (record in form of
       * search result from assigned list
       */
      $scope.finishOrPublish = function(searchResult) {

        // convert single search result into a single-element array
        var records = [];
        records.push(searchResult);

        $scope.openFinishOrPublishModal(records);
      };

      /**
       * Function to open finish or publish modal. Argument: workflowStatus: The
       * filter by which to retrieve records Must be a valid workflow status of
       * type *_IN_PROGRESS or *_RESOLVED
       */
      $scope.finishOrPublishBatch = function(workflowStatus) {

        // check arguments
        if (workflowStatus != null && workflowStatus.indexOf('_IN_PROGRESS') == -1
          && workflowStatus.indexOf('_RESOLVED') == -1) {
        }
        // determine type of work
        var apiWorkflowText;

        // determine the retrieval API text
        // i.e.. whether to call assignedConcepts,
        // assignedReviewWork, or assignedConcepts
        if (workflowStatus === 'CONFLICT_IN_PROGRESS' || workflowStatus === 'CONFLICT_RESOLVED')
          apiWorkflowText = 'assignedConflicts';
        else if (workflowStatus === 'REVIEW_IN_PROGRESS' || workflowStatus === 'REVIEW_RESOLVED')
          apiWorkflowText = 'assignedReviewWork';
        else if (workflowStatus === 'QA_IN_PROGRESS' || workflowStatus === 'QA_RESOLVED')
          apiWorkflowText = 'assignedQAWork';
        else if (workflowStatus === 'EDITING_IN_PROGRESS' || workflowStatus === 'EDITING_DONE')
          apiWorkflowText = 'assignedConcepts';
        else {
          return;
        }

        // construct a paging/filtering/sorting object based on work type
        var pfsParameterObj = {
          'startIndex' : -1,
          'maxResults' : -1,
          'sortField' : null,
          'queryRestriction' : workflowStatus
        };

        gpService.increment();
        // set based on specified workflow status
        $http(
          {
            url : root_workflow + 'project/id/' + $scope.focusProject.id + '/user/id/'
              + $scope.currentUser.userName + '/' + apiWorkflowText,
            dataType : 'json',
            data : pfsParameterObj,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {

          // if results found, open the modal
          if (data.searchResult.length > 0) {
            $scope.openFinishOrPublishModal(data.searchResult);
          }
          gpService.decrement();
        }).error(function(data, status, headers, config) {
          gpService.decrement();
          $rootScope.handleHttpError(data, status, headers, config);
        });

      };

      $scope.openFinishOrPublishModal = function(records) {

        if (records == null || records.length == 0) {
          return;
        }

        // NOTE: Record information is shoehorned into searchResult
        // workflow status is contained in terminologyVersion
        var workflowStatus = records[0].terminologyVersion;
        var modalInstance = $uibModal
          .open({
            templateUrl : 'js/widgets/workAssigned/workAssignedFinishOrPublish.html',
            controller : FinishOrPublishWorkModalCtrl,
            size : 'lg',
            resolve : {
              records : function() {
                return records;
              },
              project : function() {
                return $scope.focusProject;
              },
              user : function() {
                return $scope.currentUser;
              },
              action : function() {

                // catch simple workflow case
                if ($scope.focusProject.workflowType === 'SIMPLE_PATH') {
                  return 'publish';
                }

				// catch conflict and review case
                if ($scope.focusProject.workflowType === 'CONFLICT_AND_REVIEW_PATH') {
                  if (workflowStatus === 'CONFLICT_RESOLVED'){
						return 'finish';
					 }
				  if (workflowStatus === 'REVIEW_RESOLVED'){
						return 'publish';
					}
                }

                // otherwise, distinguish between lead work and specialist work
                return (workflowStatus === 'CONFLICT_RESOLVED'
                  || workflowStatus === 'REVIEW_RESOLVED' || workflowStatus === 'QA_RESOLVED') ? 'publish'
                  : 'finish';
              }
            }
          });

        modalInstance.result
          .then(

            // called on Done clicked by user
            function() {
              if (workflowStatus === 'CONFLICT_IN_PROGRESS'
                || workflowStatus === 'CONFLICT_RESOLVED') {
                $scope.retrieveAssignedConflicts(1, null);
              } else if (workflowStatus === 'REVIEW_IN_PROGRESS'
                || workflowStatus === 'REVIEW_RESOLVED') {
                if ($scope.currentRole === 'Lead') {
                  $scope.retrieveAssignedReviewWork(1, null);
                }
              } else if (workflowStatus === 'QA_IN_PROGRESS' || workflowStatus === 'QA_RESOLVED') {
                $scope.retrieveAssignedQAWork(1, null); // called
                // on
                // Done
              } else if (workflowStatus === 'EDITING_IN_PROGRESS'
                || workflowStatus === 'EDITING_DONE') {
                $scope.retrieveAssignedWork(1, null); // called
                // on
                // Done
              } else {
                return;
              }

              // called on Cancel/Escape, same functionality
            }, function() {
              if (workflowStatus === 'CONFLICT_IN_PROGRESS'
                || workflowStatus === 'CONFLICT_RESOLVED') {
                $scope.retrieveAssignedConflicts(1, null);
              } else if (workflowStatus === 'REVIEW_IN_PROGRESS'
                || workflowStatus === 'REVIEW_RESOLVED') {
                if ($scope.currentRole === 'Lead') {
                  $scope.retrieveAssignedReviewWork(1, null);
                }
              } else if (workflowStatus === 'QA_IN_PROGRESS' || workflowStatus === 'QA_RESOLVED') {
                $scope.retrieveAssignedQAWork(1, null);
              } else if (workflowStatus === 'EDITING_IN_PROGRESS'
                || workflowStatus === 'EDITING_DONE') {
                $scope.retrieveAssignedWork(1, null);
              } else {
                return;
              }
            });

      };

      var FinishOrPublishWorkModalCtrl = function($scope, $uibModalInstance, $q, user, project,
        records, action) {

        $scope.user = user;
        $scope.project = project;
        $scope.records = records;
        $scope.action = action;
        $scope.index = 1; // note this index is in range [1, n], where n is
        // the
        // number of records

        // set the display text based on action
        if (action === 'finish') {
          $scope.actionText = 'Finish';
        } else if (action === 'publish') {
          $scope.actionText = 'Publish';
        }

        // Select the next record in the records array
        $scope.selectNextRecord = function() {
          var deferred = $q.defer();
          $scope.index = $scope.index == $scope.records.length ? 1 : $scope.index + 1;
          $scope.loadRecord().then(function() {
            deferred.resolve();
          }, function() {
            deferred.reject();
          });
          return deferred.promise;

        };

        // Load the current record
        $scope.loadRecord = function() {

          var deferred = $q.defer();
          $scope.validationResult = null;

          // get id from list (note index is range [1,n], subtract one for
          // array
          // access)
          var recordId = $scope.records[$scope.index - 1].id;
          gpService.increment();
          // perform the retrieval call
          $http({
            url : root_mapping + 'record/id/' + recordId,
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
            function(data) {

              // do not close glass pane here, validate record first

              // set scope record
              $scope.currentRecord = data;

              // check if this record is still in progress, based on
              // requested
              // action
              if ($scope.action === 'publish') {

                // if in a publication state, this record has been
                // finished
                if ($scope.currentRecord.workflowStatus === 'READY_FOR_PUBLICATION'
                  || $scope.currentRecord.workflowStatus === 'PUBLISHED')
                  $scope.currentRecord.isFinished = true;
                else
                  $scope.currentRecord.isFinished = false;

              } else if ($scope.action === 'finish') {

                // if an *_IN_PROGRESS record, not finished
                if ($scope.currentRecord.workflowStatus === 'EDITING_IN_PROGRESS'
                  || $scope.currentRecord.workflowStatus === 'CONFLICT_IN_PROGRESS'
                  || $scope.currentRecord.workflowStatus === 'REVIEW_IN_PROGRESS'
                  || $scope.currentRecord.workflowStatus === 'QA_IN_PROGRESS') {
                  $scope.currentRecord.isFinished = false;
                }

                // If using "editing done" as a pre-step before review
                else if ($scope.currentRecord.workflowStatus === 'EDITING_DONE') {
                  $scope.currentRecord.isFinished = false;
                }

				// Handle conflict and review case
				else if ($scope.project.workflowType === 'CONFLICT_AND_REVIEW_PATH'){
					if(!($scope.currentRecord.workflowStatus === 'REVIEW_FINISHED' 
						|| $scope.currentRecord.workflowStatus === 'CONFLICT_FINISHED')){
							$scope.currentRecord.isFinished = false;
						}
				}

                // otherwise, this record has been finished/published
                // via this modal
                else {
                  $scope.currentRecord.isFinished = true;
                }
              }

              // validate the record
              $http({
                url : root_mapping + 'validation/record/validate',
                dataType : 'json',
                data : $scope.currentRecord,
                method : 'POST',
                headers : {
                  'Content-Type' : 'application/json'
                }
              }).success(function(data) {
                gpService.decrement();
                $scope.validationResult = data;
                deferred.resolve($scope.currentRecord);
              }).error(function(data, status, headers, config) {
                gpService.decrement();
                $scope.validationResult = null;
                $scope.recordError = 'Unexpected error reported by server.  Contact an admin.';
                $rootScope.handleHttpError(data, status, headers, config);
                deferred.reject('Map record failed validation');
              });

            }).error(function(data, status, headers, config) {
            gpService.decrement();
            $scope.error = 'Could not retrieve record';
            deferred.reject('Could not retrieve record');
          });

          return deferred.promise;
        };

        $scope.finishCurrentRecord = function() {

          if ($scope.validationResult.valid == true && $scope.currentRecord.isFinished == false) {
            finishRecord($scope.currentRecord).then(function(response) {

              // flag this record as finished
              $scope.currentRecord.isFinished = true;

              // if this was the only record, close the modal
              if ($scope.records.length == 1) {
                $scope.done();
              } else {
                $scope.selectNextRecord();
              }
            }, function(response) {
              $scope.error('Unexpected error finishing record.');
            }).then(function() {
            	// publish resolves related feedback, so feedback needs to be refreshed
            	if ($scope.action == 'publish') {
            	  $rootScope.$broadcast('feedbackWidget.notification.retrieveFeedback', {});
            	}
            });
          }
        };

        // helper function to return a promise resolved
        // or rejected when finish/publish call is complete
        function finishRecord(record) {
          var deferred = $q.defer();

          gpService.increment();

          $http({
            url : root_workflow + $scope.action, // api text is passed in
            // as
            // argument
            dataType : 'json',
            data : record,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(function(data) {
            gpService.decrement();
            deferred.resolve();

          }).error(function(data, status, headers, config) {
            gpService.decrement();
            deferred.reject();
          });

          return deferred.promise;

        }

        // function to finish the current record,
        // wait for completion, then continue
        function finishAllRecordsHelper() {

          // select the next record
          $scope.selectNextRecord().then(function() {
            if ($scope.validationResult.valid == true && $scope.currentRecord.isFinished == false) {

              // finish the record, then WAIT for the promise to resolve
              finishRecord($scope.currentRecord).then(
              // success function
              function() {
                $scope.recordsFinished++;

                // flag current record as finished
                $scope.currentRecord.isFinished = true;

                // publish resolves related feedback, so feedback needs to be refreshed
                if ($scope.action == 'publish') {
                	  $rootScope.$broadcast('feedbackWidget.notification.retrieveFeedback', {});
                }
                	
                // call the helper again if more records
                if ($scope.index < $scope.records.length)
                  finishAllRecordsHelper();

                // error function
              }, function() {
                $scope.recordsNotFinished++;

                // call the helper again if more records
                if ($scope.index < $scope.records.length)
                  finishAllRecordsHelper();

              });
            } else {

              // increment counter only if this record is not already
              // finished
              if ($scope.currentRecord.isFinished == false)
                $scope.recordsNotFinished++;

              // call the helper again if more records
              if ($scope.index < $scope.records.length)
                finishAllRecordsHelper();
            }
          });

        }

        $scope.finishAllRecords = function() {
          // set index to before the first record
          $scope.index = 0;

          // instantiate the reporting counters
          $scope.recordsFinished = 0;
          $scope.recordsNotFinished = 0;

          // call the sequential finishAllRecords helper function
          finishAllRecordsHelper();
          
        };

        $scope.done = function() {
          $uibModalInstance.close();
        };

        // get the first record
        $scope.loadRecord($scope.index);

      };
    });
