'use strict';

var mapProjectAppDirectives = angular.module('mapProjectAppDirectives', []);

// ///////////////////////////////////////////////////
// Directives:
// ///////////////////////////////////////////////////
mapProjectAppDirectives.directive('otfMapRecordNarrow', function($sce, utilService) {

  return {

    replace : false,
    restrict : 'AE',
    templateUrl : 'partials/otf-map-record-narrow.html',
    scope : {
      record : '=',
      project : '=',
      showTitle : '='
    },
    link : function(scope, iElement, iAttrs, ctrl) {

      // Wire notes
      if (scope.project) {
        scope.notes = utilService.getNotes(scope.project.id);
      }

      // function to return trusted html code (for tooltip
      // content)
      scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };
    }
  };
});

mapProjectAppDirectives.directive('otfMapRecordWide', function($sce, utilService) {

  return {

    replace : false,
    restrict : 'AE',
    templateUrl : 'partials/otf-map-record-wide.html',
    scope : {
      record : '=',
      project : '=',
      showTitle : '='
    },
    link : function(scope, iElement, iAttrs, ctrl) {

      // Wire notes
      if (scope.project) {
        scope.notes = utilService.getNotes(scope.project.id);
      }
      // function to return trusted html code (for tooltip
      // content)
      scope.to_trusted = function(html_code) {
        return $sce.trustAsHtml(html_code);
      };
    }
  };
});

angular.module('dynamicSortableTree', []).directive('dynamicSortableTree',
  [ '$compile', function($compile) {
    'use strict';
    return {
      restrict : 'E',
      require : '^ngModel',
      scope : true,
      link : function(scope, element, attrs, ngModel) {
        var ngModelItem = scope.$eval(attrs.ngModel);
        scope.ngModelItem = ngModelItem;

        var getView = scope.$eval(attrs.dynamicSortableView);
        if (getView && typeof getView === 'function') {
          var templateUrl = getView(ngModelItem);
          if (templateUrl) {
            element.html('<div ng-include src="' + templateUrl + '"></div>');
          }

          $compile(element.contents())(scope);
        }
      }
    };
  } ]);

mapProjectAppDirectives.directive('draggable', function() {
  return function(scope, element) {
    // this gives us the native JS object
    var el = element[0];

    el.draggable = true;

    el.addEventListener('dragstart', function(e) {
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('Text', this.id);
      this.classList.add('drag');
      return false;
    }, false);

    el.addEventListener('dragend', function(e) {
      this.classList.remove('drag');
      return false;
    }, false);
  };
});

mapProjectAppDirectives.directive('droppable', function() {
  return {
    scope : {
      drop : '&',
      bin : '='
    },
    link : function(scope, element) {
      // again we need the native object
      var el = element[0];

      el.addEventListener('dragover', function(e) {
        e.dataTransfer.dropEffect = 'move';
        // allows us to drop
        if (e.preventDefault)
          e.preventDefault();
        this.classList.add('over');
        return false;
      }, false);

      el.addEventListener('dragenter', function(e) {
        this.classList.add('over');
        return false;
      }, false);

      el.addEventListener('dragleave', function(e) {
        this.classList.remove('over');
        return false;
      }, false);

      el.addEventListener('drop', function(e) {
        // Stops some browsers from redirecting.
        if (e.stopPropagation)
          e.stopPropagation();

        this.classList.remove('over');

        var binId = this.id;
        var item = document.getElementById(e.dataTransfer.getData('Text'));
        this.appendChild(item);
        // call the passed drop function
        scope.$apply(function(scope) {
          var fn = scope.drop();
          if ('undefined' !== typeof fn) {
            fn(item.id, binId);
          }
        });

        return false;
      }, false);
    }
  };
});

// tree search result directive
mapProjectAppDirectives.directive('treeSearchResult',
  [
    '$q',
    '$sce',
    function($q, $sce) {
      return {
        restrict : 'A',
        scope : {

          // set search results if viewing trees for search
          searchResults : '=',

          // pass parameters for styling (e.g. extension highlighting)
          parameters : '=',

          // callbacks functions from parent scope
          callbacks : '='
        },
        templateUrl : 'partials/treeSearchResult.html',
        link : function(scope, element, attrs) {
          // page sizes
          scope.pageSizeSibling = 10;

          // comparator to use when adding siblings
          // NOTE: Do not apply to top-level. These are pre-sorted by back-end
          // with no problems as per children
          // NOTE: Unclear where, but rendering children is removing applied sort
          // from back-end. Thus, force new sibling calls to re-sort to ensure
          // proper display.
          // NOTE: ui-tree-node does not particularly like orderBy in ng-repeat,
          // with nodes model values disconnected from their display slot (i.e.
          // clicking to expand one node will actually expand another)
          var sortComparator = null;
          scope
            .$watch('searchResults',
              function() {

                if (scope.searchResults && scope.searchResults.length > 0) {
                  // if ICD9 or ICD10, sort by terminologyId; otherwise, by name
                  var sortField = scope.searchResults[0].terminology.toLowerCase()
                    .startsWith('icd') ? 'terminologyId' : 'defaultPreferredName';
                  sortComparator = function(a, b) {
                    if (a[sortField] < b[sortField]) {
                      return -1;
                    } else {
                      return 1;
                    }
                    return 0;
                  }
                 }
              });

          // computed tooltip html for derived labels
          // NOTE: Must not be null or empty string, or uib-tooltip-html
          // will not properly register the first mouseover event
          scope.labelTooltipHtml = "&nbsp;";

          function concatSiblings(tree, siblings) {

            var existingIds = tree.map(function(item) {
              return item.terminologyId;
            });

            var newSiblings = tree.concat(siblings.filter(function(sibling) {
              return existingIds.indexOf(sibling.terminologyId) == -1;
            }));

            newSiblings.sort(sortComparator);

            return newSiblings;
          }

          // retrieves the children for a node (from DOM)
          scope.getTreeChildrenFromTree = function(nodeScope) {
            var tree = nodeScope.$modelValue;
            scope.getTreeChildren(tree).then(function(children) {
              tree.children = concatSiblings(tree.children, children);
            });
          };

          scope.selectConcept = function(tree) {
            scope.callbacks.selectConcept(tree);
          }

          // retrieves children for a node (not from DOM)
          scope.getTreeChildren = function(tree) {

            var deferred = $q.defer();

            if (!tree) {
              utilService.setError('getChildren called with null node');
              deferred.resolve([]);
            }

            // get the next page of children based on start index of current
            // children length
            // NOTE: Offset by 1 to incorporate the (possibly) already loaded item

            scope.callbacks.getTreeChildren(tree, scope.metadata.terminology.organizingClassType,
              tree.children.length - 1).then(function(data) {
              deferred.resolve(data.trees);
            }, function(error) {
              utilService.setError('Unexpected error retrieving children');
              deferred.resolve([]);
            });

            return deferred.promise;
          };

          // toggles a node (from DOM)
          scope.toggleTree = function(nodeScope) {
            var tree = nodeScope.$modelValue;

            // if not expanded, expand
            if (!nodeScope.collapsed) {

              // get children if not already present
              if (tree.children.length == 0 && tree.childrenCount > 0) {
                scope.callbacks.getTreeChildren(tree).then(function(children) {
                  tree.children = concatSiblings(tree.children, children);
                });

              } else {
                nodeScope.toggle();
              }
            }

            // otherwise, collapse
            else {
              nodeScope.toggle();
            }
          };

          // returns the display icon for a node (from DOM)
          scope.getTreeNodeIcon = function(nodeScope) {
            var tree = nodeScope.$modelValue;

            // NOTE: This is redundant, leaf icon is set directly in html
            if (tree.childrenCount == 0) {
              return 'glyphicon-leaf';
            }

            // if collapsed or unloaded
            else if (nodeScope.collapsed || (tree.childrenCount > 0 && tree.children.length == 0)) {
              return 'glyphicon-chevron-right';
            }

            // if formally collapsed or less than sibling page size retrieved
            // children, return plus sign
            else if (tree.children.length != tree.childrenCount
              && tree.children.length < scope.pageSizeSibling) {
              return 'glyphicon-chevron-down';
            }

            // otherwise, return minus sign
            else if (!nodeScope.collapsed) {
              return 'glyphicon-chevron-down';
            }

            // if no matches, return a ? because something is seriously wrong
            else {
              return 'glyphicon-question-sign';
            }

          };

          scope.truncate = function(string, plength) {
            var length = plength;
            if (length == null)
              length = 150;
            if (string.length > length)
              return string.slice(0, length - 3);
            else
              return string;
          };

          scope.truncated = function(string, plength) {
            var length = plength;
            if (length == null)
              length = 150;
            if (string.length > length)
              return true;
            else
              return false;
          };

          scope.isMatchingNode = function(tree) {
            return scope.parameters.query
              && scope.parameters.query.toLowerCase() === tree.terminologyId;
          }

        }
      };
    } ]);
