
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
}).controller('mapEntryWidgetCtrl', function($scope, $rootScope, $http, $routeParams, $modal, localStorageService){

	// watch for entry change
	$scope.$on('mapRecordWidget.notification.changeSelectedEntry', function(event, parameters) { 	
		console.debug("MapEntryWidget: Detected change in selected entry");
		console.debug(parameters);
		$scope.entry = parameters.entry;
		$scope.record = parameters.record;
		$scope.project = parameters.project;

		// get the allowable advices
		$scope.allowableAdvices = getAllowableElements(parameters.entry, parameters.project.mapAdvice);
		sortByKey($scope.allowableAdvices, 'detail');
		$scope.allowableMapRelations = getAllowableElements(parameters.entry, parameters.project.mapRelation);
		});	

	// local variables
	$scope.isTargetOpen = true;
	$scope.isParametersOpen = true;



	////////////////////////////////////////
	// Save, Cancel, and Delete Functions //
	////////////////////////////////////////
	$scope.saveEntry = function() {

		console.debug("MapEntryWidget: saveEntry()");

		$rootScope.$broadcast(
				'mapEntryWidget.notification.modifySelectedEntry',
				{
					action: 'save', 
					entry: angular.copy($scope.entry), 
					record: $scope.record, 
					project: $scope.project
				}
		);  

		$scope.entry = null;

	};

	$scope.cancelEntry = function() {

		console.debug("MapEntryWidget: cancelEntry()");
		$scope.entry = null;
	};

	// delete an entry (after user confirmation)
	$scope.deleteEntry = function() {

		console.debug("MapEntryWidget: deleteEntry()");

		var confirmDelete = confirm("Are you sure you want to delete this entry?");
		if (confirmDelete == true) {

			$rootScope.$broadcast(
					'mapEntryWidget.notification.modifySelectedEntry',
					{
						action: 'delete', 
						entry: angular.copy($scope.entry), 
						record: $scope.record, 
						project: $scope.project
					}
			);

			$scope.entry = null;
		}
	};

	/////////////////////////////////////////
	// Target Concept Search/Set Functions //
	/////////////////////////////////////////
	$scope.retrieveTargetConcepts = function(query) {

		// execute query for concepts
		$http({
			url: root_content + "concept/query/" + query,
			dataType: "json",
			method: "GET",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {

			console.debug(data);

			// eliminate concepts that don't match target terminology 
			$scope.targetConcepts = [];

			for (var i = 0; i < data.count; i++) {
				if (data.searchResult[i].terminology === $scope.project.destinationTerminology &&
						data.searchResult[i].terminologyVersion === $scope.project.destinationTerminologyVersion) {

					$scope.targetConcepts.push(data.searchResult[i]);
				};
			};

			$scope.statusRetrieveTargetConcepts = $scope.targetConcepts.length == 0 ? 'No concepts found': '';

		}).error(function(data) {
			$scope.errorRetrieveTargetConcepts = "Failed to retrieve entries";
		});
	};

	$scope.resetTargetConcepts = function() {
		console.debug("resetTargetConcepts() called");
		$scope.queryTarget = "";
		$scope.targetConcepts = [];
	};

	$scope.selectTargetConcept = function(entry, target) {
		console.debug("selectTargetConcept() called");
		console.debug(target);
		entry.targetId = target.terminologyId;
		entry.targetName = target.value;
		$scope.resetTargetConcepts();
		computeRelation(entry);
		
	};
	
	// watch for concept selection from terminology browser
	$scope.$on('terminologyBrowser.selectConcept', function(event, parameters) { 	
		console.debug("MapEntryWidget: Detected selectConcept from terminologyBrowser");
		console.debug(parameters);
		
		$scope.entry.targetId = parameters.concept.terminologyId;
		$scope.entry.targetName = parameters.concept.defaultPreferredName;
		computeRelation($scope.entry);
	});	
	
	
	
	$scope.clearTargetConcept = function(entry) {
		console.debug("clearTargetConcept() called");
		entry.targetId = null;
		entry.targetName = null;
		computeRelation(entry);
	};
	
	function computeRelation(entry) {
		$http({
			url: root_mapping + "relation/compute",
			dataType: "json",
			data: entry,
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			}	
		}).success(function(data) {

			console.debug(data);

			entry.mapRelation = data;
			
		}).error(function(data) {
			$scope.errorCreateRecord = "Failed to retrieve entries";
		});
		
		// get the allowable advices and relations
		$scope.allowableAdvices = getAllowableElements(entry, $scope.project.mapAdvice);
		sortByKey($scope.allowableAdvices, 'detail');
		$scope.allowableMapRelations = getAllowableElements(entry, $scope.project.mapRelation);
		
	}

	//////////////////////////////////////
	// Rule Modal Constructor Functions //
	//////////////////////////////////////

	// scope level function to open the modal constructor
	$scope.openRuleConstructor = function() {

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
			$scope.entry.rule = rule;
			$scope.entry.ruleSummary = $scope.getRuleSummary($scope.entry);
		});
	};

	// Returns a summary string for the entry rule type
	$scope.getRuleSummary = function(entry) {
		
		var ruleSummary = "";
		
		// first, rule summary
		if ($scope.project.ruleBased == true) {
			if (entry.rule.toUpperCase().indexOf("FEMALE") != -1) ruleSummary += "[FEMALE]";
			else if (entry.rule.toUpperCase().indexOf("MALE") != -1) ruleSummary += "[MALE]";
			else if (entry.rule.toUpperCase().indexOf("AGE") != -1) {

				// find lower and upper bounds matching the pattern:
				// > [number] [units] OR >= [number] [units]
				var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/ );
				var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/ );
				
				console.debug(lowerBound);
				console.debug(upperBound);

				ruleSummary += '[AGE ';
				
				if (lowerBound != null && lowerBound.length > 0) {
					ruleSummary += lowerBound[0];
					
					// if also an upper bound, add an AND separator
					if (upperBound.length > 0) ruleSummary += ' AND ';
				}
				if (upperBound != null && upperBound.length > 0) ruleSummary += upperBound[0];
				
				ruleSummary += ']';				
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
		
		$scope.entry = entry;  
	};

	/////////////////////////
	// Relation functions ///
	/////////////////////////
	
	$scope.selectMapRelation = function(mapRelation) {
		
		$scope.entry.mapRelation = mapRelation;
	};
	
	$scope.clearMapRelation = function(mapRelation) {
		$scope.entry.mapRelation = null;
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
	
					elements[i].displayName = (elements[i].abbreviation === 'none' ? '' : elements[i].abbreviation + ": ") + elements[i].name;
					
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
