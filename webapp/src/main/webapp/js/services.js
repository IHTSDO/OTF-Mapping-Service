'use strict';

// TODO Test or remove later
mapProjectApp.service('indexViewerService', [ 'rootScope', function($rootScope, $templateCache) {

  this.addTemplate = function(name, template) {
    // n/a
  };

  this.getTemplate = function(name) {
    // n/a
  };
} ]);

// Util service
mapProjectApp
  .service(
    'utilService',
    [
      '$rootScope',
      '$window',
      '$location',
      '$anchorScroll',
      '$http',
      function($rootScope, $window, $location, $anchorScroll, $http) {
        console.debug('configure utilService');
        // declare the error
        this.error = {
          message : null,
          longMessage : null,
          expand : false
        };

        // tinymce options
        this.tinymceOptions = {
          menubar : false,
          statusbar : false,
          plugins : 'autolink link image charmap searchreplace lists paste',
          toolbar : 'undo redo | styleselect lists | bold italic underline strikethrough | charmap link image',
          forced_root_block : ''
        };

        // terminology Notes
        var notes = {};

        // Sets the error
        this.setError = function(message) {
          $rootScope.globalError = message;
        };

        // Clears the error
        this.clearError = function() {
          $rootScope.resetGlobalError();
        };

        // Handle error message
        function handleError(message) {
          this.handleError(message);
        }

        this.handleError = function(message) {
          if (message && message.length > 120) {
            $rootScope.globalError = 'Unexpected error, click the icon to view attached full error';
            $rootScope.globalLongError = message;
          } else {
            $rootScope.globalError = message;
          }
          // handle no message
          if (!$rootScope.globalError) {
            $rootScope.globalError = 'Unexpected server side error.';
          }
          // If authtoken expired, relogin
          if ($rootScope.globalError && $rootScope.globalError.indexOf('AuthToken') != -1) {
            // Reroute back to login page with 'auth token has expired' message
            $location.path('/');
          } else {
            // scroll to top of page
            $window.scrollTo(0, 0);
          }
        };

        // look up the 'terminology notes' for the map project
        // Mechanism for asterisk/dagger in ICD10
        this.initializeTerminologyNotes = function(projectId) {
          // Skip if no auth header yet
          if ($http.defaults.headers.common.Authorization) {
            $rootScope.glassPane++;
            $http.get(root_mapping + 'mapProject/' + projectId + '/notes').then(
            // Success
            function(response) {
              var list = {};
              for (var i = 0; i < response.data.keyValuePair.length; i++) {
                var entry = response.data.keyValuePair[i];
                list[entry.key] = entry.value;
              }
              notes[projectId] = list;
              $rootScope.glassPane--;
            },
            // Error
            function(response) {
              $rootScope.glassPane--;
              handleError(response.data);
            });
          }
        };

        // Get notes for this project id
        // Mechanism for asterisk/dagger in ICD10
        this.getNotes = function(projectId) {
          return notes[projectId];
        };

        // Prep query
        this.prepQuery = function(query) {
          if (!query) {
            return '';
          }

          // Add a * to the filter if set and doesn't contain a :
          if (query.indexOf('(') == -1 && query.indexOf(':') == -1 && query.indexOf('"') == -1) {
            var query2 = query.concat('*');
            return encodeURIComponent(query2);
          }
          return encodeURIComponent(query);
        };

        // Prep pfs filter
        this.prepPfs = function(pfs) {
          if (!pfs) {
            return {};
          }

          // Add a * to the filter if set and doesn't contain a :
          if (pfs.queryRestriction && pfs.queryRestriction.indexOf(':') == -1
            && pfs.queryRestriction.indexOf('"') == -1) {
            var pfs2 = angular.copy(pfs);
            pfs2.queryRestriction += '*';
            return pfs2;
          }
          return pfs;
        };

        // Convert date to a string
        this.toDate = function(lastModified) {
          var date = new Date(lastModified);
          var year = '' + date.getFullYear();
          var month = '' + (date.getMonth() + 1);
          if (month.length == 1) {
            month = '0' + month;
          }
          var day = '' + date.getDate();
          if (day.length == 1) {
            day = '0' + day;
          }
          var hour = '' + date.getHours();
          if (hour.length == 1) {
            hour = '0' + hour;
          }
          var minute = '' + date.getMinutes();
          if (minute.length == 1) {
            minute = '0' + minute;
          }
          var second = '' + date.getSeconds();
          if (second.length == 1) {
            second = '0' + second;
          }
          return year + '-' + month + '-' + day + ' ' + hour + ':' + minute + ':' + second;
        };

        // Convert date to a short string
        this.toShortDate = function(lastModified) {
          var date = new Date(lastModified);
          var year = '' + date.getFullYear();
          var month = '' + (date.getMonth() + 1);
          if (month.length == 1) {
            month = '0' + month;
          }
          var day = '' + date.getDate();
          if (day.length == 1) {
            day = '0' + day;
          }
          return year + '-' + month + '-' + day;
        };

        // Convert date to a simple string
        this.toSimpleDate = function(lastModified) {
          var date = new Date(lastModified);
          var year = '' + date.getFullYear();
          var month = '' + (date.getMonth() + 1);
          if (month.length == 1) {
            month = '0' + month;
          }
          var day = '' + date.getDate();
          if (day.length == 1) {
            day = '0' + day;
          }
          return year + month + day;
        };

        // Table sorting mechanism
        this.setSortField = function(table, field, paging) {
          paging[table].sortField = field;
          // reset page number too
          paging[table].page = 1;
          // handles null case also
          if (!paging[table].ascending) {
            paging[table].ascending = true;
          } else {
            paging[table].ascending = false;
          }
          // reset the paging for the correct table
          for ( var key in paging) {
            if (paging.hasOwnProperty(key)) {
              if (key == table)
                paging[key].page = 1;
            }
          }
        };

        // Return up or down sort chars if sorted
        this.getSortIndicator = function(table, field, paging) {
          if (paging[table].ascending == null) {
            return '';
          }
          if (paging[table].sortField == field && paging[table].ascending) {
            return '▴';
          }
          if (paging[table].sortField == field && !paging[table].ascending) {
            return '▾';
          }
        };

        // Helper to get a paged array with show/hide flags
        // and filtered by query string
        this.getPagedArray = function(array, paging, pageSize) {
          var newArray = new Array();

          // if array blank or not an array, return blank list
          if (array == null || array == undefined || !Array.isArray(array)) {
            return newArray;
          }

          newArray = array;

          // apply sort if specified
          if (paging.sortField) {
            // if ascending specified, use that value, otherwise use false
            newArray.sort(this.sortBy(paging.sortField, paging.ascending));
          }

          // apply filter
          if (paging.filter) {
            newArray = this.getArrayByFilter(newArray, paging.filter);
          }

          // apply active status filter
          if (paging.typeFilter) {
            newArray = this.getArrayByActiveStatus(newArray, paging.typeFilter);
          }

          // get the page indices
          var fromIndex = (paging.page - 1) * pageSize;
          var toIndex = Math.min(fromIndex + pageSize, array.length);

          // slice the array
          var results = newArray.slice(fromIndex, toIndex);

          // add the total count before slicing
          results.totalCount = newArray.length;

          return results;
        };

        // function for sorting an array by (string) field and direction
        this.sortBy = function(field, reverse) {

          // key: function to return field value from object
          var key = function(x) {
            return x[field];
          };

          // convert reverse to integer (1 = ascending, -1 =
          // descending)
          reverse = !reverse ? 1 : -1;

          return function(a, b) {
            return a = key(a), b = key(b), reverse * ((a > b) - (b > a));
          };
        };

        // Get array by filter text matching terminologyId or name
        this.getArrayByFilter = function(array, filter) {
          var newArray = [];

          for ( var object in array) {

            if (this.objectContainsFilterText(array[object], filter)) {
              newArray.push(array[object]);
            }
          }
          return newArray;
        };

        // Get array by filter on conceptActive status
        this.getArrayByActiveStatus = function(array, filter) {
          var newArray = [];

          for ( var object in array) {

            if (array[object].conceptActive && filter == 'Active') {
              newArray.push(array[object]);
            } else if (!array[object].conceptActive && filter == 'Retired') {
              newArray.push(array[object]);
            } else if (array[object].conceptActive && filter == 'All') {
              newArray.push(array[object]);
            }
          }
          return newArray;
        };

        // Returns true if any field on object contains filter text
        this.objectContainsFilterText = function(object, filter) {

          if (!filter || !object)
            return false;

          for ( var prop in object) {
            var value = object[prop];
            // check property for string, note this will cover child elements
            if (value && value.toString().toLowerCase().indexOf(filter.toLowerCase()) != -1) {
              return true;
            }
          }

          return false;
        };

        // Finds the object in a list by the field
        this.findBy = function(list, obj, field) {

          // key: function to return field value from object
          var key = function(x) {
            return x[field];
          };

          for (var i = 0; i < list.length; i++) {
            if (key(list[i]) == key(obj)) {
              return list[i];
            }
          }
          return null;
        };

      } ]);

// Glass pane service
mapProjectApp.service('gpService', [ '$rootScope', function($rootScope) {
  console.debug('configure gpService');

  // model: $rootScope.glassPane;

  this.isGlassPaneSet = function() {
    return $rootScope.glassPane > 0;
  };

  this.isGlassPaneNegative = function() {
    return $rootScope.glassPane < 0;
  };

  // Increments glass pane counter
  this.increment = function() {
    $rootScope.glassPane++;
  };

  // Decrements glass pane counter
  this.decrement = function(message) {
    $rootScope.glassPane--;
  };

} ]);
