/*
 * The MIT License
 * 
 * Copyright (c) 2013, Sebastian Sdorra
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * @ngdoc directive
 * @name adf.directive:adfDashboard
 * @element div
 * @restrict ECA
 * @scope
 * @description
 * 
 * `adfDashboard` is a directive which renders the dashboard with all its 
 * components. The directive requires a name attribute. The name of the
 * dashboard can be used to store the model.
 */


'use strict';

angular.module('adf')
  .directive('adfDashboard', function($rootScope, $log, $modal, dashboard, localStorageService){
	
	  
    function fillStructure(model, columns, counter){
      angular.forEach(model.rows, function(row){
        angular.forEach(row.columns, function(column){
          if (!column.widgets){
            column.widgets = [];
          }
          if ( counter < columns.length ){
            angular.forEach(columns[counter].widgets, function(widget){
              column.widgets.push(widget);
            });
            counter++;
          }
        });
      });
      return counter;
    }
    
    function readColumns(model){
      var columns = [];
      angular.forEach(model.rows, function(row){
        angular.forEach(row.columns, function(col){
          columns.push(col);
        });
      });
      return columns;
    }
            
    function changeStructure(model, structure){
      var columns = readColumns(model);
      model.rows = structure.rows;
      var counter = 0;
      while ( counter < columns.length ){
        counter = fillStructure(model, columns, counter);
      }
    }

    return {
      replace: true,
      restrict: 'EA',
      transclude : false,
      scope: {
        structure: '@',
        name: '@',
        adfModel: '='
      },
      controller: function($scope){
    	  
	    $scope.currentUser = localStorageService.get('currentUser');
		$scope.currentRole = localStorageService.get('currentRole');
    		  
    	  
        // sortable options for drag and drop
        $scope.sortableOptions = {
          connectWith: ".column",
          handle: ".fa-arrows",
          cursor: 'move',
          tolerance: 'pointer',
          placeholder: 'placeholder',
          forcePlaceholderSize: true,
          opacity: 0.4
        };
    	
        var name = $scope.name;
        var model = $scope.adfModel;
        console.debug('DASHBOARD.JS MODEL 1:');
        console.debug(model);
        if ( ! model || ! model.rows ){
          var structureName = $scope.structure;
          var structure = dashboard.structures[structureName];
          if (structure){
            if (model){
              model.rows = angular.copy(structure).rows;
            } else {
              model = angular.copy(structure);
            }
            model.structure = structureName;
          } else {
            $log.error( 'could not find structure ' + structureName);
          }
        } 
        
        console.debug('DASHBOARD.JS MODEL 2:');
        console.debug(model);
        
        if (model) {
          if (!model.title){
            model.title = 'Dashboard';
          }
          $scope.model = model;
        } else {
        	alert("model " + model);
          $log.error('could not find or create model');
        }

        // edit mode
        $scope.editMode = false;
        $scope.editClass = "";

        $scope.toggleEditMode = function(){
        
        	console.debug('toggleEditMode');
        	
          $scope.editMode = ! $scope.editMode;
          if ($scope.editClass === ""){
            $scope.editClass = "edit";
          } else {
            $scope.editClass = "";
          }
          if (!$scope.editMode){
            $rootScope.$broadcast('adfDashboardChanged', name, model);
          }
        };
        
        // edit dashboard settings
        $scope.editDashboardDialog = function(){
          var editDashboardScope = $scope.$new();
          editDashboardScope.structures = dashboard.structures;
          var instance = $modal.open({
            scope: editDashboardScope,
            templateUrl: './partials/dashboard-edit.html'
          });
          $scope.changeStructure = function(name, structure){
            $log.info('change structure to ' + name);
            changeStructure(model, structure);
          };
          editDashboardScope.closeDialog = function(){
            instance.close();
            editDashboardScope.$destroy();
          };
        };

        // add widget dialog
        $scope.addWidgetDialog = function(){
          var addScope = $scope.$new();
          addScope.widgets = dashboard.widgets;
          var opts = {
            scope: addScope,
            templateUrl: './partials/widget-add.html'
          };
          var instance = $modal.open(opts);
          addScope.addWidget = function(widget){
        	var w = {
              type: widget,
              config: {}
            };
            addScope.model.rows[0].columns[0].widgets.unshift(w);
            instance.close();

            addScope.$destroy();
          };
          addScope.isWidgetInRole = function(widget){
        	  if ($scope.name === 'default') {
	        	  if (widget == 'mapProjectList' && $scope.currentRole.value >= 1) { //$rootScope.role.value >= 1) {
	            	  return true;
	              } else if (widget == 'metadataList' && $scope.currentRole.value > 1) { //$rootScope.role.value >= 3) {
	        	      return true;
	          	  } else if (widget == 'mapProject') {
	          		  return true;
	          	  } else if (widget == 'assignedList' && $scope.currentRole.value > 1) {
	          		  return true;
	          	  } else if (widget == 'editedList' && $scope.currentRole.value > 1) {
	          		  return true;
	          	  } else if (widget == 'compareRecords' && $scope.currentRole.value > 1) {
	          		  return true;
	          	  }
	          	  else if (widget = "availableWork") return true;
	        	  return false;
        	  } else if ($scope.name === 'mappingDashboard') {
        		  if (widget == 'mapRecord' && $scope.currentRole.value >= 1) { //$rootScope.role.value >= 1) {
	            	  return true;
	              } else if (widget == 'mapEntry' && $scope.currentRole.value > 1) { //$rootScope.role.value >= 3) {
	        	      return true;
	          	  } 
        	  }
          };
          addScope.closeDialog = function(){
            instance.close();
            addScope.$destroy();
          };
        };
      },
      link: function ($scope, $element, $attr) {
        // pass attributes to scope
    	  console.debug('attributes passed to scope:')
    	  console.debug($attr.name);
    	  console.debug($attr.structure);
    	  console.debug($attr.adfModel);
        $scope.name = $attr.name;
        $scope.structure = $attr.structure;
        $scope.adfModel = $attr.adfModel;
      }/*,
      templateUrl: './partials/dashboard.html'*/
    	 
    };
  });