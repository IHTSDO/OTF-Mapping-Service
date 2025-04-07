'use strict';

angular
  .module('mapProjectApp.widgets.mapEntry', [ 'adf.provider' ])
  .config(function(dashboardProvider) {
    dashboardProvider.widget('mapEntry', {
      title : 'Map Entry',
      description : 'Edit module for a map entry',
      controller : 'mapEntryWidgetCtrl',
      templateUrl : 'js/widgets/mapEntry/mapEntry.html',
      edit : {}
    });
  })
  .controller(
    'mapEntryWidgetCtrl',
    [
      '$scope',
      '$window',
      '$rootScope',
      '$q',
      '$http',
      '$routeParams',
      '$uibModal',
      '$location',
      '$anchorScroll',
      'localStorageService',
      'utilService',
      'appConfig',
      'gpService',
      function($scope, $window, $rootScope, $q, $http, $routeParams, $uibModal,
        $location, $anchorScroll, localStorageService, utilService, appConfig, gpService) {

        // for this widget, the only local storage service variable used is
        // user
        // token
        $scope.userToken = localStorageService.get('userToken');
		$scope.additionalMapEntryInfoOrderingMap = utilService.processOrderingInfo(appConfig['deploy.additional.map.entry.info.ordering']);
		$scope.additionalMapEntryInfoHidingMap = utilService.processHidingInfo(appConfig['deploy.additional.map.entry.info.hiding']);

        // watch for entry change
        $scope.$on('mapRecordWidget.notification.changeSelectedEntry',
          function(event, parameters) {
            console.debug(
              '  => on mapRecordWidget.notification.changeSelectedEntry = ',
              parameters.record, parameters.entry);
            $scope.entry = parameters.entry;
            $scope.record = parameters.record;
            $scope.project = parameters.project;

            // get the allowable advices
            $scope.allowableAdvices = getAllowableAdvices(parameters.entry,
              parameters.project.mapAdvice);
            sortByKey($scope.allowableAdvices, 'detail');
            $scope.allowableMapRelations = getAllowableRelations(
              parameters.entry, parameters.project.mapRelation);

			// identify any additional map entry information for this project
			$scope.additionalMapEntryFields = getAdditionalMapEntryFields($scope.project.additionalMapEntryInfo);	

			// initialize additional map entry info, if needed
			if($scope.additionalMapEntryFields.length > 0 && $scope.entry['additionalMapEntryInfo'] === undefined){
				$scope.entry['additionalMapEntryInfo'] = [];
			}

			// determine which additional map entries are available for the current entry
			$scope.allowableMapEntryInfos = {};
			for(var i = 0; i < $scope.additionalMapEntryFields.length; i++){
				$scope.allowableMapEntryInfos[$scope.additionalMapEntryFields[i].name] = 
				$scope.getAllowableMapEntryInfosForField($scope.entry, $scope.additionalMapEntryFields[i].name);
				sortByKey($scope.allowableMapEntryInfos[$scope.additionalMapEntryFields[i].name],'value');
			}


            // set the rule to null if a non-rule-based project
            // added to catch any badly constructed rules from other widgets
            if (!$scope.project.ruleBased) {
              $scope.entry.rule = null;
            }

            // compute relation and advice IFF a target or entry has
            // been set attempt to autocompute the map relation, then update the
            // entry
            // $scope.computeParameters(false);

          });

        // watch for entry deletion from map record page
        $scope
          .$on(
            'mapRecordWidget.notification.deleteSelectedEntry',
            function(event, parameters) {
              console.debug(
                ' => on mapRecordWidget.notification.deleteSelectedEntry = ',
                parameters.entry);
              // if the currently viewed entry is the one being viewed,
              // clear the displayed entry
              if (($scope.entry.localId && $scope.entry.localId == parameters.entry.localId)
                || ($scope.entry.id && $scope.entry.id == parameters.entry.id)) {
                $scope.entry = null;
              }

            });

        // local variables
        $scope.isTargetOpen = true;
        $scope.isParametersOpen = true;
        $scope.localErrorRule = '';

        $scope.$watch('userToken', function() {
          $http.defaults.headers.common.Authorization = $scope.userToken;
        });

        // ///////////////////////////////////////
        // Save, Cancel, and Delete Functions //
        // //////////////////////////////////////

        // broadcasts an update from the map entry to the map record widget
        function updateEntry(entry) {
          console.debug(
            'broadcast mapEntryWidget.notification.modifySelectedEntry = ',
            entry);
          $rootScope.$broadcast(
            'mapEntryWidget.notification.modifySelectedEntry', {
              action : 'save',
              entry : angular.copy(entry),
              record : $scope.record,
              project : $scope.project
            });
        }

        $scope.setTarget = function(targetCode) {
          
          $scope.getValidTargetError = '';

          // if target code is empty, compute parameters and return
          if (!targetCode) {
            $scope.entry.targetId = '';
            $scope.entry.targetName = 'No target';
            $scope.computeParameters(true);
            return;
          }

          gpService.increment();
          $http(
            {
              url : root_mapping + 'project/id/' + $scope.project.id
                + '/concept/isValid' + '?terminologyId=' + encodeURIComponent(targetCode),
              method : 'GET',
              headers : {
                'Content-Type' : 'application/json'
              }
            }).success(
            function(data) {
              gpService.decrement();
              console.debug('  valid target = ', data)
              // if target found and valid
              if (data) {
				
				if ($scope.entry.targetName != data.defaultPreferredName) {
					$scope.clearDataOnChange($scope.entry);
				}

                $scope.entry.targetId = data.terminologyId;
                $scope.entry.targetName = data.defaultPreferredName;
                
                // make sure that the mapEntry is up to date before computing advice and relations
                for (var i = 0; i < $scope.record.mapEntry.length; i++) {
                    var entry = $scope.record.mapEntry[i];
                    // Use the scoped entry if the local id matches or if the actual id
                    // matches
                    if (matchingEntry(entry, $scope.entry)) {
                      $scope.record.mapEntry[i].targetId = $scope.entry.targetId;
                      $scope.record.mapEntry[i].targetName = $scope.entry.targetName;
                    } 
                }
                
                // attempt to autocompute the map relation, then update the
                // entry
                $scope.computeParameters(false);
                

	          // get the allowable advices and relations
	          $scope.allowableAdvices = getAllowableAdvices($scope.entry,
	            $scope.project.mapAdvice);
	          sortByKey($scope.allowableAdvices, 'detail');

                $scope.allowableMapRelations = getAllowableRelations($scope.entry,
                    $scope.project.mapRelation);

				for(var i = 0; i < $scope.additionalMapEntryFields.length; i++){
					$scope.allowableMapEntryInfos[$scope.additionalMapEntryFields[i].name] = 
					$scope.getAllowableMapEntryInfosForField($scope.entry, $scope.additionalMapEntryFields[i].name);
					sortByKey($scope.allowableMapEntryInfos[$scope.additionalMapEntryFields[i].name],'value');
				}

              } else {
                $scope.getValidTargetError = targetCode
                  + ' is not a valid target';
                $scope.entry.targetName = null;
              }

            }).error(function(data, status, headers, config) {
            gpService.decrement();

            $rootScope.handleHttpError(data, status, headers, config);
          });
        }; // end scope.setTarget

        // watch for concept selection from terminology browser
        $scope.$on('terminologyBrowser.selectConcept', function(event,
          parameters) {
          console.debug(' => on terminologyBrowser.selectConcept = ',
            parameters.concept);
          // get the relative position of the inside of the map entry widget

          var rect = document.getElementById('mapEntryWidgetTop')
            .getBoundingClientRect();

          // scroll to (mapEntry left, mapEntry top + scroll offset -
          // header/widget header width)
          $window.scrollTo(rect.left, rect.top + window.pageYOffset - 90);

          $scope.entry.targetId = parameters.concept.terminologyId;
          $scope.entry.targetName = parameters.concept.defaultPreferredName;

          // clear the relation and advices
          $scope.entry.mapRelation = null;
          $scope.entry.mapAdvice = [];

          // attempt to autocompute the map relation and map advices, then update the
          // entry
          //$scope.computeParameters(false);
          // compute parameters will get called from setTarget()
          // best to call setTarget first so that advices are computed based on correct targetId
          $scope.setTarget($scope.entry.targetId);
          
        });

        $scope.clearTargetConcept = function(entry) {
          entry.targetId = null;
          entry.targetName = null;
          entry.mapRelation = null;
          entry.mapAdvice = [];
		  entry['additionalMapEntryInfo'] = [];

          console
            .debug('broadcast mapEntryWidget.notification.clearTargetConcept');
          $rootScope
            .$broadcast('mapEntryWidget.notification.clearTargetConcept');

          // get the allowable advices and relations
          $scope.allowableAdvices = getAllowableAdvices($scope.entry,
            $scope.project.mapAdvice);
          sortByKey($scope.allowableAdvices, 'detail');
          $scope.allowableMapRelations = getAllowableRelations($scope.entry,
            $scope.project.mapRelation);

			$scope.allowableMapEntryInfos = {};
			for(var i = 0; i < $scope.additionalMapEntryFields.length; i++){
				$scope.allowableMapEntryInfos[$scope.additionalMapEntryFields[i].name] = 
				$scope.getAllowableMapEntryInfosForField($scope.entry, $scope.additionalMapEntryFields[i].name);
				sortByKey($scope.allowableMapEntryInfos[$scope.additionalMapEntryFields[i].name],'value');
			}

          // if project rule based, reset rule to TRUE
          if ($scope.project.ruleBased) {
            entry.rule = 'TRUE';
          }

          // attempt to autocompute the map relation, then update the entry
          $scope.computeParameters(false);

          // update the entry
          updateEntry($scope.entry);
          
          $scope.getValidTargetError = '';
          $scope.mapRelationInput = '';
        };

        function computeRelation(entry) {
          var deferred = $q.defer();

          // ensure mapRelation is deserializable
          if (!entry.mapRelation) {
            entry.mapRelation = null;
          }

          // Fake the ID of this entry with -1 id, copy, then set it back
          // This is hacky, but we do not have a good way to send 2 objects
          // and the entry may not have an id yet because it could be new.
          var copy = angular.copy($scope.record);
          // Find the matching localId and replace it and set the id to -1
          for (var i = 0; i < copy.mapEntry.length; i++) {
            if (entry.localId == copy.mapEntry[i].localId) {
              var entryCopy = angular.copy(entry);
              entryCopy.id = -1;
              copy.mapEntry.splice(i, 1, entryCopy);
            }
          }

          gpService.increment();
          $http({
            url : root_mapping + 'relation/compute',
            dataType : 'json',
            data : copy,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          })
            .success(
              function(data) {
                console.debug('  relation = ', data);
                if (data) {

                  // get the allowable advices and relations
                  $scope.allowableAdvices = getAllowableAdvices(entry,
                    $scope.project.mapAdvice);
                  sortByKey($scope.allowableAdvices, 'detail');
                  $scope.allowableMapRelations = getAllowableRelations(entry,
                    $scope.project.mapRelation);

                  if (data.isComputed) {
                    entry.mapRelation = data;
                  } else {
                    for (var i = 0; i < $scope.allowableMapRelations.length; i++) {
                      var relation = $scope.allowableMapRelations[i];
                      if (relation.id == data.id) {
                        entry.mapRelation = relation;
                        break;
                      }
                    }
                  }

                  gpService.decrement();

                  // return the promise
                  deferred.resolve(entry);
                } else {
                  gpService.decrement();
                  deferred.resolve(entry);
                }
              }).error(function(data, status, headers, config) {
              gpService.decrement();
              $rootScope.handleHttpError(data, status, headers, config);

              // reject the promise
              deferred.reject();

            });

          return deferred.promise;
        }

        // Computes advices (if any) for each entry, then update entry
        function computeAdvices(record) {
          for (var i = 0; i < record.mapEntry.length; i++) {
            var entry = record.mapEntry[i];
            // Use the scoped entry if the local id matches or if the actual id
            // matches
            if (matchingEntry(entry, $scope.entry)) {
              entry = $scope.entry;
            }
            // pass the record entry, use the scoped one
            // if it is the entry currently being edited
            computeAdvice(entry, i).then(
            // Success
            function(data) {
              // Update the entry
              if (data) {
                updateEntry(data);
              }
            });
          }
        }

        // Computes map advice
        function computeAdvice(entry, index) {
          var deferred = $q.defer();

          // ensure mapAdvice is deserializable
          if (!entry.mapAdvice) {
            entry.mapAdvice = [];
          }

          gpService.increment();

          // var entryIsScopeEntry = matchingEntry(entry, $scope.entry);

          // Replace in the record the entry being edited
          // so the changes are reflected. All other entries
          // are sync'd with map record display.
          // var copy = angular.copy($scope.record);
          // var entryCopy = angular.copy(entry);
          // entryCopy.id = -1;
          // copy.mapEntry.splice(index, 1, entryCopy);
          //
          // // Also need to replace the scope record with the edited
          // // one in cases where we are checking other entries
          // for (var i = 0; i < copy.mapEntry.length; i++) {
          // // if localId or Id matches $scope record, replace it
          // if (matchingEntry(copy.mapEntry[i], $scope.entry)) {
          // var entryCopy2 = angular.copy($scope.entry);
          // if (entryIsScopeEntry) {
          // entryCopy2.id = -1;
          // }
          // copy.mapEntry.splice(i, 1, entryCopy2);
          // break;
          // }
          // }

          
          $http({
            url : root_mapping + 'advice/compute/' + index,
            dataType : 'json',
            data : $scope.record,
            method : 'POST',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
            function(data) {
              console.debug('  advice = ', data);
              if (data) {
                entry.mapAdvice = data.mapAdvice;
                // get the allowable advices and relations for this entry
                if (matchingEntry(entry, $scope.entry)) {
                  $scope.allowableAdvices = getAllowableAdvices(entry,
                    $scope.project.mapAdvice);
                  sortByKey($scope.allowableAdvices, 'detail');
                  $scope.allowableMapRelations = getAllowableRelations(entry,
                    $scope.project.mapRelation);
                }
              }
              gpService.decrement();
              deferred.resolve(entry);

            }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);
            deferred.reject();
          });

          return deferred.promise;
        }

        // Determine if entrys match on id
        function matchingEntry(entry1, entry2) {
          if (entry1.id == null) {
            return entry1.localId == entry2.localId;
          } else {
            return entry1.id == entry2.id;
          }
        }

        // ////////////////////////////////////
        // Rule Modal Constructor Functions //
        // ////////////////////////////////////

        // scope level function to open the modal constructor
        $scope.openRuleConstructor = function() {

          // clear any error regarding rule construction
          $scope.localErrorRule = '';

          var modalInstance = $uibModal.open({
            templateUrl : 'partials/rule-modal.html',
            controller : RuleConstructorModalCtrl,
            resolve : {
              presetAgeRanges : function() {
                return angular.copy($scope.project.mapAgeRange);
              },
              entry : function() {
                return angular.copy($scope.entry);
              }
            }
          });

          modalInstance.result
            .then(
            // Success
            function(lrule) {
              var rule = lrule;
              // set to true if rule returned with no value and display an
              // error
              if (!rule) {
                rule = 'TRUE';
                $scope.localErrorRule = 'Invalid rule constructed, setting rule to TRUE';
              }

              $scope.entry.rule = rule;
              $scope.entry.ruleSummary = $scope.getRuleSummary($scope.entry);

              // compute relation and advice (if any), then update entry
              $scope.computeParameters(false);
            });
        };

        // Returns a summary string for the entry rule type
        $scope.getRuleSummary = function(entry) {

          var ruleSummary = '';

          // first, rule summary
          if ($scope.project.ruleBased) {
            if (entry.rule.toUpperCase().indexOf('TRUE') != -1)
              ruleSummary += '[TRUE] ';
            else if (entry.rule.toUpperCase().indexOf('FEMALE') != -1)
              ruleSummary += '[FEMALE] ';
            else if (entry.rule.toUpperCase().indexOf('MALE') != -1)
              ruleSummary += '[MALE] ';
            else if (entry.rule.toUpperCase().indexOf('AGE') != -1) {

              var lowerBound = entry.rule.match(/(>= \d+ [a-zA-Z]*)/);
              var upperBound = entry.rule.match(/(< \d+ [a-zA-Z]*)/);

              ruleSummary += '[AGE ';

              if (lowerBound != null && lowerBound != ''
                && lowerBound.length > 0) {
                ruleSummary += lowerBound[0];
                if (upperBound != null && upperBound != ''
                  && upperBound.length > 0)
                  ruleSummary += ' AND ';
              }
              if (upperBound != null && upperBound != ''
                && upperBound.length > 0)
                ruleSummary += upperBound[0];

              ruleSummary += '] ';
            }
          }

          return ruleSummary;

        };

        // controller for the modal
        var RuleConstructorModalCtrl = function($scope, $http,
          $uibModalInstance, presetAgeRanges, entry) {

          $scope.ruleError = '';
          $scope.customAgeRange = {
            'name' : '',
            'lowerValue' : '',
            'lowerInclusive' : 'false',
            'lowerUnits' : 'years',
            'upperValue' : '',
            'upperInclusive' : 'false',
            'upperUnits' : 'years'
          };
          $scope.presetAgeRanges = presetAgeRanges;

          initializePresetAgeRanges();

          $scope.ruleCategories = [ 'TRUE', 'Gender - Male', 'Gender - Female',
            'Age - At Onset (Custom range)', 'Age - At Onset (Preset range)' ];

          if (entry != null && entry.rule != null) {
            if (entry.rule.indexOf('Male') > -1)
              $scope.ruleCategory = 'Gender - Male';
            else if (entry.rule.indexOf('Female') > -1)
              $scope.ruleCategory = 'Gender - Female';
            else if (entry.rule.indexOf('Custom') > -1)
              $scope.ruleCategory = 'Age - At Onset (Custom range)';
            else if (entry.rule.indexOf('Preset') > -1)
              $scope.ruleCategory = 'Age - At Onset (Preset range)';
            else
              $scope.ruleCategory = 'TRUE';
          } else
            $scope.ruleCategory = 'TRUE';

          $scope.rule = entry.rule;

          $scope.saveRule = function() {
            $uibModalInstance.close($scope.rule, $scope.ruleSummary);
          };

          $scope.cancelRule = function() {
            $uibModalInstance.dismiss('cancel');
          };

          // alter the rule category, and construct
          $scope.changeRuleCategory = function(ruleCategory) {

            $scope.ageRange = null;
            $scope.constructRule(ruleCategory, null);
          };

          // construct actual text of rule based on category and age range (if
          // supplied)
          $scope.constructRule = function(ruleCategory, ageRange) {
            // clear the rule
            $scope.rule = '';

            // clear the rule error
            $scope.ruleError = '';

            // if a true rule
            if (ruleCategory === 'TRUE') {
              $scope.rule = 'TRUE';
            }

            // if a male gender rule
            else if (ruleCategory === 'Gender - Male') {
              $scope.rule = 'IFA 248153007 | Male (finding) |';
            }

            // if a female gender rule
            else if (ruleCategory === 'Gender - Female') {
              $scope.rule = 'IFA 248152002 | Female (finding) |';
            }

            // if an age range rule
            else if (ruleCategory === 'Age - At Onset (Custom range)'
              || ruleCategory === 'Age - At Onset (Preset range)') {

              // if age range not yet specified, do not construct rule
              if (ageRange == null || ageRange == undefined)
                return;

              // determine if lower and upper values are complete by checking
              // for null values
              var lowerValueValid = ageRange.lowerValue != '-1'
                && ageRange.lowerValue != undefined
                && ageRange.lowerValue != null && ageRange.lowerValue != '';
              var upperValueValid = ageRange.upperValue != '-1'
                && ageRange.upperValue != undefined
                && ageRange.lowerValue != null && ageRange.upperValue != '';

              // stop if neither value has been fully specified
              if (!lowerValueValid && !upperValueValid)
                return;

              // initialize calculated values (in days)
              var lowerValue = -1;
              var upperValue = -1;

              // calculate lower value based on units and verify greater than
              // zero
              if (lowerValueValid) {

                switch (ageRange.lowerUnits) {
                case 'days':
                  lowerValue = parseFloat(ageRange.lowerValue, 10);
                  break;
                case 'months':
                  lowerValue = parseFloat(ageRange.lowerValue, 10) * 30;
                  break;
                case 'years':
                  lowerValue = parseFloat(ageRange.lowerValue, 10) * 365;
                  break;
                default:
                  $scope.ruleError += 'Unexpected error determining lower units\n';
                }

                if (lowerValue <= 0) {
                  $scope.ruleError = 'Lower bound value must be greater than zero\n';
                  return;
                }
              }
              if (upperValueValid) {

                switch (ageRange.upperUnits) {
                case 'days':
                  upperValue = parseFloat(ageRange.upperValue, 10);
                  break;
                case 'months':
                  upperValue = parseFloat(ageRange.upperValue, 10) * 30;
                  break;
                case 'years':
                  upperValue = parseFloat(ageRange.upperValue, 10) * 365;
                  break;
                case 'year':
                  upperValue = parseFloat(ageRange.upperValue, 10) * 365;
                  break;
                default:
                  $scope.ruleError += 'Unexpected error determining upper units\n';
                }

                if (upperValue <= 0) {
                  $scope.ruleError = 'Upper bound value must be greater than zero\n';
                  return;
                }
              }

              // if both specified, check that upper value is greater than
              // lower value
              if (lowerValueValid && upperValueValid
                && lowerValue >= upperValue) {
                $scope.ruleError += 'Upper bound value must be greater than lower bound value';
              }

              // base text for both lower and upper value sections
              var ruleText = 'IFA 445518008 | Age at onset of clinical finding (observable entity)';

              if (lowerValueValid) {
                $scope.rule += ruleText + ' | '
                  + (ageRange.lowerInclusive === "true" ? '>=' : '>') + ' '
                  + parseFloat(ageRange.lowerValue, 10).toFixed(1) + ' '
                  + ageRange.lowerUnits;
              }

              if (lowerValueValid && upperValueValid) {
                $scope.rule += ' AND ';
              }

              if (upperValueValid) {
                $scope.rule += ruleText + ' | '
                  + (ageRange.upperInclusive === "true" ? '<=' : '<') + ' '
                  + parseFloat(ageRange.upperValue, 10).toFixed(1) + ' '
                  + ageRange.upperUnits;

              }
            } else
              $scope.rule = null;
          };

          $scope.constructRuleAgeHelper = function(ruleCategory, ageRange) {
            $scope.constructRule($scope.ruleCategory);
          };

          function initializePresetAgeRanges() {

            // set the preset age range strings
            for (var i = 0; i < $scope.presetAgeRanges.length; i++) {
              var presetAgeRangeStr = $scope.presetAgeRanges[i].name + ', ';

              if ($scope.presetAgeRanges[i].lowerValue != null
                && $scope.presetAgeRanges[i].lowerValue != '-1') {
                presetAgeRangeStr += ($scope.presetAgeRanges[i].lowerInclusive ? '>='
                  : '>')
                  + ' '
                  + $scope.presetAgeRanges[i].lowerValue
                  + ' '
                  + $scope.presetAgeRanges[i].lowerUnits;
              }

              if ($scope.presetAgeRanges[i].lowerValue != null
                && $scope.presetAgeRanges[i].lowerValue != '-1'
                && $scope.presetAgeRanges[i].upperValue != null
                && $scope.presetAgeRanges[i].upperValue != '-1') {

                presetAgeRangeStr += ' and ';
              }

              if ($scope.presetAgeRanges[i].upperValue != null
                && $scope.presetAgeRanges[i].upperValue != '-1') {

                presetAgeRangeStr += ($scope.presetAgeRanges[i].upperInclusive ? '<='
                  : '<')
                  + ' '
                  + $scope.presetAgeRanges[i].upperValue
                  + ' '
                  + $scope.presetAgeRanges[i].upperUnits;
              }

              $scope.presetAgeRanges[i].stringName = presetAgeRangeStr;
            }

          }

        };

        // /////////////////////
        // Advice functions ///
        // /////////////////////

        // validates and adds advice to a map entry
        $scope.addEntryAdvice = function(entry, advice) {

          // check if advice valid
          if (advice == '') {
            $scope.errorAddAdvice = 'Advice cannot be empty';
          } else if (advice == null) {
            $scope.errorAddAdvice = 'This advice is not found in allowable advices for this project';
          } else {
            $scope.errorAddAdvice = '';

            // check if this advice is already present
            var advicePresent = false;
            for (var i = 0; i < entry.mapAdvice.length; i++) {
              if (advice.id === entry.mapAdvice[i].id)
                advicePresent = true;
            }

            if (advicePresent) {
              $scope.errorAddAdvice = 'This advice ' + advice.detail
                + ' is already attached to this entry';
            } else {
              $scope.entry['mapAdvice'].push(advice);
              $scope.adviceInput = '?';
            }
          }

          updateEntry($scope.entry);
        };

        // removes advice from a map entry
        $scope.removeEntryAdvice = function(entry, advice) {

          var confirmRemove = true;
          if (advice.isComputed) {
            confirmRemove = confirm('The advice you are removing was automatically computed for this entry.  Are you sure you want to do this?');
          }

          if (confirmRemove) {
            entry.mapAdvice = removeJsonElement(entry.mapAdvice, advice);

		  // add the advice to the list of options, if not already present
          var advicePresent = false;
          for (var j = 0; j < $scope.allowableAdvices.length; j++) {
	            if ($scope.allowableAdvices[j].id === advice.id)
	              advicePresent = true;
		  }
          // add advice if not already present
          if (!advicePresent)
            $scope.allowableAdvices.push(advice);

          updateEntry($scope.entry);
          }

        };

        // ///////////////////////
        // Relation functions ///
        // ///////////////////////

        $scope.selectMapRelation = function() {
          // compute advices
          computeAdvices($scope.record);
        };

        $scope.clearMapRelation = function(mapRelation) {
          $scope.entry.mapRelation = null;

          $scope.computeParameters(false);
        };

        $scope.setNullTarget = function() {
          // open glass pane (setNullTarget1)
          gpService.increment();
          $scope.entry.targetId = '';
          $scope.entry.targetName = 'No target';
          $scope.computeParameters(true);
          // close glass pane (setNullTarget1)
          gpService.decrement();
        };

        /**
         * Function to compute relation and advice for an entry Parameter:
         * ignoreNullValues false: if both target and relation are null, do
         * nothing true: make computation API calls regardless of target and
         * relation
         */
        $scope.computeParameters = function(ignoreNullValues) {

          // either target or relation must be non-null to compute
          // relation/advice
          if ($scope.entry.targetId || $scope.entry.mapRelation
            || ignoreNullValues) {

            computeRelation($scope.entry).then(
            // Success
            function() {
              computeAdvices($scope.record);
            });

            // set these to null for consistency
          } else {
            $scope.entry.targetId = null;
            $scope.entry.mapRelation = null;
          }
        };

		// Check if the project requires clearing out of other information on a change
		// like advices, additional map entry info, etc.
        $scope.clearDataOnChange = function(entry) {

          var deferred = $q.defer();

          gpService.increment();
          
          $http({
            url : root_mapping + 'clearDataOnChange/' + $scope.project.id,
            method : 'GET',
            headers : {
              'Content-Type' : 'application/json'
            }
          }).success(
            function(data) {
              if (data) {
			    gpService.decrement();
				if(data === true){
				  $scope.clearAdditionalMapEntryInfosForEntry(entry);
				}
				

                // return the promise
                deferred.resolve(entry);
              } else {
                gpService.decrement();
                deferred.resolve(entry);	
			  }
            }).error(function(data, status, headers, config) {
            gpService.decrement();
            $rootScope.handleHttpError(data, status, headers, config);

            // reject the promise
            deferred.reject();
          });

          return deferred.promise;
        };

        // ///////////////////////////////////////////
        // Functions for additional map entry infos///
        // //////////////////////////////////////////

        $scope.getAllowableMapEntryInfosForField = function(entry, specifiedField) {

          var allowableMapEntryInfos = [];

          if (entry!= null && specifiedField != null) {

			// Identify all map entry infos for the specified field
            for (var i = 0; i < $scope.project.additionalMapEntryInfo.length; i++) {
			  var field = $scope.project.additionalMapEntryInfo[i].field;
	
			  if(field === specifiedField){
				// check that this map entry info is not already present on the entry
			    var mapEntryInfoPresent = false;
				 if(entry.additionalMapEntryInfo != null){
					for(var j = 0; j < entry.additionalMapEntryInfo.length; j++){
					  if(entry.additionalMapEntryInfo[j].name === $scope.project.additionalMapEntryInfo[i].name){
						mapEntryInfoPresent = true;
					  }
				    }
				  }

				// add map entry info if not already present
              	if (!mapEntryInfoPresent)
                  allowableMapEntryInfos.push($scope.project.additionalMapEntryInfo[i]);
              	}
			  }
		  	}

		  return allowableMapEntryInfos;
		}
		
		
		$scope.getAssignedMapEntryInfosForField = function(entry, specifiedField){
          var mapEntryInfos = [];

          if (specifiedField != null && entry != null && entry.additionalMapEntryInfo != null) {

            for (var i = 0; i < entry.additionalMapEntryInfo.length; i++) {
			  var field = entry.additionalMapEntryInfo[i].field;
	
			  if(field === specifiedField){
			  	mapEntryInfos.push(entry.additionalMapEntryInfo[i]);
			  }
		  	}
		  }

		  return mapEntryInfos;			
		}

        // validates and adds additional Map Entry Info to a map entry
        $scope.addEntryAdditionalMapEntryInfo = function(entry, additionalMapEntryInfo) {

            // check if this additionalMapEntryInfo is already present
            var additionalMapEntryInfoPresent = false;
			if(entry.additionalMapEntryInfo != null){
              for (var i = 0; i < entry.additionalMapEntryInfo.length; i++) {
                if (additionalMapEntryInfo.id === entry.additionalMapEntryInfo[i].id)
                  additionalMapEntryInfoPresent = true;
              }				
			}

            if (additionalMapEntryInfoPresent) {
              $scope.errorAddAdditionalMapEntryInfo = 'This map entry info ' + additionalMapEntryInfo.value
                + ' is already attached to this entry';
            } else {
              $scope.entry['additionalMapEntryInfo'].push(additionalMapEntryInfo);
              $scope.additionalMapEntryInfoInput = '?';
            }

			$scope.allowableMapEntryInfos[additionalMapEntryInfo.field] = 
				$scope.getAllowableMapEntryInfosForField(entry, additionalMapEntryInfo.field);


          updateEntry($scope.entry);

          // compute advices
          computeAdvices($scope.record);

        };

        // removes additional map entry info from a map entry
        $scope.removeAdditionalMapEntryInfo = function(entry, additionalMapEntryInfo) {

            entry.additionalMapEntryInfo = removeJsonElement(entry.additionalMapEntryInfo, additionalMapEntryInfo);

			$scope.allowableMapEntryInfos[additionalMapEntryInfo.field] = 
				$scope.getAllowableMapEntryInfosForField(entry, additionalMapEntryInfo.field);

          updateEntry($scope.entry);

        };

		$scope.showAdditionalMapEntryInfo = function(entry, field) {
			if($scope.additionalMapEntryInfoHidingMap == null){
				return true;
			}
			else if($scope.additionalMapEntryInfoHidingMap.get(field.name) == entry.mapGroup){
				return false;
			}
			else{
				return true;
			}
		}
		
		$scope.clearAdditionalMapEntryInfosForEntry = function(entry) {
			entry['additionalMapEntryInfo'] = [];

			$scope.allowableMapEntryInfos[additionalMapEntryInfo.field] = 
				$scope.getAllowableMapEntryInfosForField(entry, additionalMapEntryInfo.field);

			
			updateEntry($scope.entry);
		}

        // Function for MapAdvice and MapRelations, returns allowable lists
        // based
        // on null target and element properties
        function getAllowableAdvices(entry, advices) {

          var allowableAdvices = [];

          // if target is null (i.e. not valid or empty), return empty list
          if (entry.targetId != null) {
            var nullTarget = !entry.targetId;

            for (var i = 0; i < advices.length; i++) {

              // do not add computed advices
              if (!advices[i].isComputed) {

                // if empty target and allowable for null target OR
                // if valid target and not allowable for null target
                if ((nullTarget && advices[i].isAllowableForNullTarget)
                  || (!nullTarget && !advices[i].isAllowableForNullTarget)) {

                  // check that this advice is not already present on the
                  // entry
                  var advicePresent = false;
                  for (var j = 0; j < entry.mapAdvice.length; j++) {
                    if (entry.mapAdvice[j].id === advices[i].id)
                      advicePresent = true;
                  }

                  // add advice if not already present
                  if (!advicePresent)
                    allowableAdvices.push(advices[i]);
                }
              }
            }
          }

          return allowableAdvices;
        }

        function getAdditionalMapEntryFields(additionalMapEntryInfo) {

          var additionalMapEntryFields = [];

          if (additionalMapEntryInfo != null) {

            for (var i = 0; i < additionalMapEntryInfo.length; i++) {
			  var field = {};
			  field.name = additionalMapEntryInfo[i].field;
	
			  var fieldAlreadyAdded = false;
			  for(var j = 0; j < additionalMapEntryFields.length; j++){
				if (additionalMapEntryFields[j].name === field.name){
					fieldAlreadyAdded = true;
					break;
				}
			  }
	
			  if(!fieldAlreadyAdded){
				additionalMapEntryFields.push(field);
			  }
			}
		  }

		 addOrderIds(additionalMapEntryFields, $scope.additionalMapEntryInfoOrderingMap);

		  return additionalMapEntryFields;
		}

        // Allowable MapRelations
        // based on null target and element properties
        function getAllowableRelations(entry, relations) {
          var allowableRelations = [];

          // if the target is null (i.e. neither valid or empty)
          // return an empty list, otherwise calculate
          if (entry.targetId != null) {

            var nullTarget = !entry.targetId;

            for (var i = 0; i < relations.length; i++) {

              if (!relations[i].isComputed) {

                if ((!entry.targetId && relations[i].isAllowableForNullTarget)
                  || (entry.targetId && !relations[i].isAllowableForNullTarget)) {

                  allowableRelations.push(relations[i]);
                }
              }
            }
          }
          return allowableRelations;
        }

		//UI Labels
		$scope.getUILabel = function(label){
		  if (utilService.checkIfProjectRelabels($scope.project.refSetId, appConfig['deploy.ui.relabel.refsetids'])){
			return utilService.getUILabel(label, appConfig['deploy.ui.relabel.' + $scope.project.refSetId]);
		  }
		  else {
			return label;
		  }
		}

        // sort and return an array by string key
        function sortByKey(array, key) {
          return array.sort(function(a, b) {
            var x = a[key];
            var y = b[key];
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
          });
        }

        function addOrderIds(additionalMapEntryFields, orderIdInfoMap) {
          for (var i = 0; i < additionalMapEntryFields.length; i++) {
			if (orderIdInfoMap.has(additionalMapEntryFields[i].name)){
				additionalMapEntryFields[i].orderId = orderIdInfoMap.get(additionalMapEntryFields[i].name);
			}
			else{
				additionalMapEntryFields[i].orderId = 0;
			}
          }

			return additionalMapEntryFields;
        }

        function removeJsonElement(array, elem) {
          var newArray = [];
          for (var i = 0; i < array.length; i++) {
            if (array[i].id != elem.id) {
              newArray.push(array[i]);
            }
          }
          return newArray;
        }

      } ]);
