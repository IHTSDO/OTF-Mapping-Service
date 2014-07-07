
'use strict';

angular.module('mapProjectApp.widgets.mapEntry', ['adf.provider'])
.config(function(dashboardProvider){
	dashboardProvider
	.widget('mapEntry', {
		title: 'Map Entry',
		description: 'Edit module for a map entry',
		controller: 'mapEntryWidgetCtrl',
		templateUrl: 'js/widgets/mapEntry/mapEntry.html',
		edit: {}
	});
}).controller('mapEntryWidgetCtrl', function($scope, $rootScope, $q, $http, $routeParams, $modal, $location, localStorageService){

	// watch for entry change
	$scope.$on('mapRecordWidget.notification.changeSelectedEntry', function(event, parameters) { 	
		console.debug("MapEntryWidget: Detected change in selected entry");
		console.debug(parameters);
		$scope.entry = parameters.entry;
		$scope.record = parameters.record;
		$scope.project = parameters.project;
		
		console.debug("MapEntryWidget: mapRecord = ", $scope.record);

		// get the allowable advices
		$scope.allowableAdvices = getAllowableElements(parameters.entry, parameters.project.mapAdvice);
		sortByKey($scope.allowableAdvices, 'detail');
		$scope.allowableMapRelations = getAllowableElements(parameters.entry, parameters.project.mapRelation);
		});	
	
	// watch for entry deletion from map record page
	$scope.$on('mapRecordWidget.notification.deleteSelectedEntry', function(event, parameters) { 	
		console.debug("MapEntryWidget: Detected delete notification from MapRecordWidget");
		console.debug(parameters);
		
		// if the currently viewed entry is the one being viewed, clear the displayed entry
		if (($scope.entry.localId === parameters.entry.localId && $scope.entry.localId != null && $scope.entry.localId != "") 
				|| ($scope.entry.id === parameters.entry.id && $scope.entry.id != null && $scope.entry.id != "")) {
			$scope.entry = null;
		}
		
	});
	

	// local variables
	$scope.isTargetOpen = true;
	$scope.isParametersOpen = true;
	$scope.localErrorRule = "";
	
	$scope.userToken = localStorageService.get('userToken');
	$scope.$watch('userToken', function() {
		
		$http.defaults.headers.common.Authorization = $scope.userToken;
		
	});



	/////////////////////////////////////////
	// Save, Cancel, and Delete Functions //
	////////////////////////////////////////
	
	// broadcasts an update from the map entry to the map record widget
	function updateEntry() {
		$rootScope.$broadcast(
				'mapEntryWidget.notification.modifySelectedEntry',
				{
					action: 'save', 
					entry: angular.copy($scope.entry), 
					record: $scope.record, 
					project: $scope.project
				}
		);
	}
	
	
	// watch for concept selection from terminology browser
	$scope.$on('terminologyBrowser.selectConcept', function(event, parameters) { 	
		console.debug("MapEntryWidget: Detected selectConcept from terminologyBrowser");
		console.debug(parameters);
		
		$scope.entry.targetId = parameters.concept.terminologyId;
		$scope.entry.targetName = parameters.concept.defaultPreferredName;
		
		// get the allowable advices and relations
		$scope.allowableAdvices = getAllowableElements($scope.entry, $scope.project.mapAdvice);
		sortByKey($scope.allowableAdvices, 'detail');
		$scope.allowableMapRelations = getAllowableElements($scope.entry, $scope.project.mapRelation);
		
		// clear the relation and advices
		$scope.entry.mapRelation = null;
		$scope.entry.mapAdvice = [];
		
		// attempt to autocompute the map relation, then update the entry
		computeRelation($scope.entry).then(function() {
			console.debug('Relation computed');
			computeAdvice($scope.entry).then(function() {
				console.debug('Advice computed');
				updateEntry();
			});
		});
	});	
	
	
	
	$scope.clearTargetConcept = function(entry) {
		console.debug("clearTargetConcept() called");
		entry.targetId = null;
		entry.targetName = null;
		entry.mapRelation = null;
		entry.mapAdvice = [];
		
		// get the allowable advices and relations
		$scope.allowableAdvices = getAllowableElements($scope.entry, $scope.project.mapAdvice);
		sortByKey($scope.allowableAdvices, 'detail');
		$scope.allowableMapRelations = getAllowableElements($scope.entry, $scope.project.mapRelation);
		
		updateEntry();	
	};
	
	function computeRelation(entry) {
		
		var deferred = $q.defer();
		
		// ensure mapRelation is deserializable
		if (entry.mapRelation === '' || entry.mapRelation === undefined) entry.mapRelation = null;
		
		$rootScope.glassPane++;
		$http({
			url: root_mapping + "relation/compute",
			dataType: "json",
			data: entry,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {

			$rootScope.glassPane--;
			if (data) {
				
				entry.mapRelation = data;
				
				console.debug("MapRelation computed: ", entry.mapRelation);
				
				// get the allowable advices and relations
				$scope.allowableAdvices = getAllowableElements(entry, $scope.project.mapAdvice);
				sortByKey($scope.allowableAdvices, 'detail');
				$scope.allowableMapRelations = getAllowableElements(entry, $scope.project.mapRelation);
			
				// return the promise
				deferred.resolve(entry);
			} else {
				console.debug("No map relation computed for this entry");
				deferred.resolve(entry);
			}
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);			
		  
		    // reject the promise
		    deferred.reject();
			
		});
		
		return deferred.promise;
	}
	
	function computeAdvice(entry) {
		
		var deferred = $q.defer();
		
		// ensure mapAdvice is deserializable
		if (entry.mapAdvice === '' || entry.mapAdvice === undefined) entry.mapAdvice = [];
		
		$rootScope.glassPane++;
		
		$http({
			url: root_mapping + "advice/compute",
			dataType: "json",
			data: entry,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {
			$rootScope.glassPane--;
			
			if (data) {
				
				entry.mapAdvice = data.mapAdvice;
				
				console.debug("Map advices computed: ", entry.mapAdvice);
				
				// get the allowable advices and relations
				$scope.allowableAdvices = getAllowableElements(entry, $scope.project.mapAdvice);
				sortByKey($scope.allowableAdvices, 'detail');
				$scope.allowableMapRelations = getAllowableElements(entry, $scope.project.mapRelation);
			
				// return the promise
				deferred.resolve(entry);
			} else {
				console.debug("No map advice computed");
				deferred.resolve(entry);
			}
		}).error(function(data, status, headers, config) {
		    $rootScope.glassPane--;

		    $rootScope.handleHttpError(data, status, headers, config);			
		  
		    // reject the promise
		    deferred.reject();
			
		});	
		
		return deferred.promise;
	}

	//////////////////////////////////////
	// Rule Modal Constructor Functions //
	//////////////////////////////////////

	// scope level function to open the modal constructor
	$scope.openRuleConstructor = function() {
		
		// clear any error regarding rule construction
		$scope.localErrorRule = "";

		var modalInstance = $modal.open({
			templateUrl: 'partials/rule-modal.html',
			controller: RuleConstructorModalCtrl,
			resolve: {
				presetAgeRanges: function() {
					return angular.copy($scope.project.mapAgeRange);
				},
				entry: function() {
					return angular.copy($scope.entry);
				}
			}
		});

		modalInstance.result.then(function(rule) {
			
			// set to true if rule returned with no value and display an error
			if (rule == null || rule == undefined || rule === '') {
				rule = 'TRUE';
				$scope.localErrorRule = "Invalid rule constructed, setting rule to TRUE";
			}
			
			$scope.entry.rule = rule;
			$scope.entry.ruleSummary = $scope.getRuleSummary($scope.entry);
			
			// clear relation and advice
			$scope.entry.mapRelation = null;
			$scope.entry.mapAdvice = [];
			
			// compute relation and advice (if any), then update entry
			computeRelation($scope.entry).then(function() {
				computeAdvice($scope.entry).then(function() {
					updateEntry();
				});
			});
		});
	};

	// Returns a summary string for the entry rule type
	$scope.getRuleSummary = function(entry) {
		
		var ruleSummary = "";
		
		// first, rule summary
		if ($scope.project.ruleBased == true) {
			if (entry.rule.toUpperCase().indexOf("TRUE") != -1) ruleSummary += "[TRUE] ";
			else if (entry.rule.toUpperCase().indexOf("FEMALE") != -1) ruleSummary += "[FEMALE] ";
			else if (entry.rule.toUpperCase().indexOf("MALE") != -1) ruleSummary += "[MALE] ";
			else if (entry.rule.toUpperCase().indexOf("AGE") != -1) {

				
				var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/ );
				var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/ );

				ruleSummary += '[AGE ';
				
				if (lowerBound != null && lowerBound != '' && lowerBound.length > 0) {
					ruleSummary += lowerBound[0];
					if (upperBound != null && upperBound != '' && upperBound.length > 0) ruleSummary += ' AND ';
				}
				if (upperBound != null && upperBound != '' && upperBound.length > 0) ruleSummary += upperBound[0];
				
				ruleSummary += '] ';				
			}
		}
		
		return ruleSummary;
			
	};

	// controller for the modal
	var RuleConstructorModalCtrl = function($scope, $http, $modalInstance, presetAgeRanges, entry) {

		$scope.ageRange={"name":"" , "lowerValue":"", "lowerInclusive":"", "lowerUnits":"", 
				"upperValue":"", "upperInclusive":"", "upperUnits":""},

		$scope.presetAgeRanges = presetAgeRanges;

		initializePresetAgeRanges();

		console.debug($scope.presetAgeRanges);
		console.debug(entry.rule);
		
		$scope.ruleCategories = ['TRUE', 'Gender - Male', 'Gender - Female', 'Age - Chronological', 'Age - At Onset'];
		
		
		if (entry != null && entry.rule != null) {
			if (entry.rule.indexOf('Male') > -1)
			  $scope.ruleCategory = 'Gender - Male';
			else if (entry.rule.indexOf('Female') > -1)
			  $scope.ruleCategory = 'Gender - Female';
			else if (entry.rule.indexOf('chronological') > -1)
			  $scope.ruleCategory = 'Age - Chronological';
			else if (entry.rule.indexOf('onset') > -1)
			  $scope.ruleCategory = 'Age - At Onset';
			else
			  $scope.ruleCategory = 'TRUE';
		} else 
		    $scope.ruleCategory = 'TRUE'; 

		$scope.saveRule = function() {
			$modalInstance.close($scope.rule, $scope.ruleSummary);
		};

		$scope.cancelRule = function() {
			$modalInstance.dismiss('cancel');
		};

		$scope.changeRuleCategory = function(ruleCategory) {

			$scope.ageRange = null;
			$scope.constructRule(ruleCategory, null);
		};

		$scope.constructRule = function(ruleCategory, ageRange) {

			$scope.rule = "";

			if (ruleCategory === "TRUE") {
				$scope.rule = "TRUE";
			}

			else if (ruleCategory === "Gender - Male") {
				$scope.rule = "IFA 248153007 | Male (finding) |";
			}

			else if (ruleCategory === "Gender - Female") {
				$scope.rule = "IFA 248152002 | Female (finding) |";
			}

			else if (ageRange != null) {

				if (ruleCategory === "Age - Chronological" || ruleCategory === "Age - At Onset") {

					var ruleText = (ruleCategory === "Age - Chronological") ?
							"IFA 424144002 | Current chronological age (observable entity)" :
								"IFA 445518008 | Age at onset of clinical finding (observable entity)"	;


					if (ageRange.lowerValue != "-1") {
						$scope.rule += ruleText
						+  " | " + (ageRange.lowerInclusive == true ? ">=" : ">") + " "
						+  ageRange.lowerValue + " "
						+  ageRange.lowerUnits;
					}

					if (ageRange.lowerValue != "-1" && ageRange.upperValue != "-1")
						$scope.rule += " AND ";

					if (ageRange.upperValue != "-1") {
						$scope.rule += ruleText
						+  " | " + (ageRange.upperInclusive == true ? "<=" : "<") + " "
						+  ageRange.upperValue + " "
						+  ageRange.upperUnits;
					}			
				}
			} else $scope.rule = null;
		};

		$scope.constructRuleAgeHelper = function(ruleCategory, ageRange) {
			$scope.constructRule($scope.ruleCategory);
		};

		function initializePresetAgeRanges() {  

			// set the preset age range strings
			for (var i = 0; i < $scope.presetAgeRanges.length; i++) {
				var presetAgeRangeStr = $scope.presetAgeRanges[i].name + ", ";

				if ($scope.presetAgeRanges[i].lowerValue != null && $scope.presetAgeRanges[i].lowerValue != "-1") {
					presetAgeRangeStr += ($scope.presetAgeRanges[i].lowerInclusive == true ? ">=" : ">") + " "
					+  $scope.presetAgeRanges[i].lowerValue + " "
					+  $scope.presetAgeRanges[i].lowerUnits;
				}

				if ($scope.presetAgeRanges[i].lowerValue != null && $scope.presetAgeRanges[i].lowerValue != "-1" &&
						$scope.presetAgeRanges[i].upperValue != null && $scope.presetAgeRanges[i].upperValue != "-1") {

					presetAgeRangeStr += " and ";
				}

				if ($scope.presetAgeRanges[i].upperValue != null && $scope.presetAgeRanges[i].upperValue != "-1") {

					presetAgeRangeStr += ($scope.presetAgeRanges[i].upperInclusive == true ? "<=" : "<") + " "
					+  $scope.presetAgeRanges[i].upperValue + " "
					+  $scope.presetAgeRanges[i].upperUnits;
				}

				$scope.presetAgeRanges[i].stringName = presetAgeRangeStr;
			};
		};

	};


	///////////////////////
	// Advice functions ///
	///////////////////////

	// validates and adds advice to a map entry
	$scope.addEntryAdvice = function(entry, advice) {

		console.debug('ADDING ADVICE');
		// check if advice valid
		if (advice == '') {
			$scope.errorAddAdvice = "Advice cannot be empty";
		} else if (advice == null) {
			$scope.errorAddAdvice = "This advice is not found in allowable advices for this project";
		} else {
			$scope.errorAddAdvice = "";

			// check if this advice is already present
			var advicePresent = false;
			for (var i = 0; i < entry.mapAdvice.length; i++) {
				if (advice.id === entry.mapAdvice[i].id) advicePresent = true;
			}

			if (advicePresent == true) {
				$scope.errorAddAdvice = "This advice " + advice.detail + " is already attached to this entry";
			} else {
				$scope.entry['mapAdvice'].push(advice);
				$scope.adviceInput = "?";
			}
		}
		
			updateEntry();	
	};

	// removes advice from a map entry
	$scope.removeEntryAdvice = function(entry, advice) {	
		
		console.debug('Removing advice:');
		console.debug(advice);
		console.debug('Advices found:');
		for (var i = 0; i < entry['mapAdvice'].length; i++) {
			console.debug(entry['mapAdvice'][i]);
		}
		
		entry.mapAdvice = removeJsonElement(entry.mapAdvice, advice);
		
		console.debug('Advices after removal:');
		for (var i = 0; i < entry['mapAdvice'].length; i++) {
			console.debug(entry['mapAdvice'][i]);
		}
		
			updateEntry();	
		
		
	};

	/////////////////////////
	// Relation functions ///
	/////////////////////////
	
	$scope.selectMapRelation = function(mapRelation) {		
		$scope.entry.mapRelation = mapRelation;
		
		// clear advice on relation change
		$scope.entry.mapAdvice = [];
		
		// compute advice (if any), then update entry
		computeAdvice($scope.entry).then(function() {
			updateEntry();	
		});
		
	};
	
	$scope.clearMapRelation = function(mapRelation) {
		$scope.entry.mapRelation = null;
		
		// compute advice (if any), then update entry
		computeAdvice($scope.entry).then(function() {
			updateEntry();	
		});
	};


	// Function for MapAdvice and MapRelations, returns allowable lists based on null target and element properties
	function getAllowableElements(entry, elements) {
		
		console.debug('called allowable elements');
		
		var nullTarget = entry.targetId == null || entry.targetId === "";
		var allowableElements = [];

		if (nullTarget == true) console.debug('NULL TARGET');
		
		for (var i = 0; i < elements.length; i++) {
			
			if (elements[i].isComputed == false) {
			
				if ( (nullTarget == true && elements[i].isAllowableForNullTarget == true) ||
						(nullTarget == false && elements[i].isAllowableForNullTarget == false) ) {
	
					elements[i].displayName = (elements[i].abbreviation === 'none' ? elements[i].name : elements[i].abbreviation )  ;
					
					allowableElements.push(elements[i]);
			}
			}
		}
		
		return allowableElements;
	};
	

	// sort and return an array by string key
	function sortByKey(array, key) {
		return array.sort(function(a, b) {
			var x = a[key]; var y = b[key];
			return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
	};
	
	function removeJsonElement(array, elem) {
		
		console.debug("Removing element");
		var newArray = [];
		for (var i = 0; i < array.length; i++) {
			if (array[i].id != elem.id) {
				console.debug("Pushing element " + array[i].id);
				newArray.push(array[i]);
			}
		}
		
		console.debug("After remove, before return:")
		console.debug(newArray)
		return newArray;
	}
	
});
