'use strict';

var mapProjectAppDirectives = angular.module('mapProjectAppDirectives', []);

/////////////////////////////////////////////////////
//Directives:
/////////////////////////////////////////////////////
mapProjectAppDirectives.directive('otfMapRecordNarrow', function($sce) {
	  

		return {

		replace : false,
		restrict : 'AE',
		templateUrl : 'partials/otf-map-record-narrow.html',
		scope : {
			record : '=',
			project : '=',
			showTitle : '='
		},
		link: function(scope, iElement, iAttrs, ctrl) {
			// function to return trusted html code (for tooltip
			// content)
			scope.to_trusted = function(html_code) {
				console.debug("otfMapRecord: to_trusted", $sce.trustAsHtml(html_code));
				return $sce.trustAsHtml(html_code);
			};
		}
	};
});

mapProjectAppDirectives.directive('otfMapRecordWide', function($sce) {
	  

	return {

	replace : false,
	restrict : 'AE',
	templateUrl : 'partials/otf-map-record-wide.html',
	scope : {
		record : '=',
		project : '=',
		showTitle : '='
	},
	link: function(scope, iElement, iAttrs, ctrl) {
		// function to return trusted html code (for tooltip
		// content)
		scope.to_trusted = function(html_code) {
			console.debug("otfMapRecord: to_trusted", $sce.trustAsHtml(html_code));
			return $sce.trustAsHtml(html_code);
		};
	}
};
});

mapProjectAppDirectives.directive('draggable', function() {
  return function(scope, element) {
    // this gives us the native JS object
    var el = element[0];
    
    el.draggable = true;
    
    el.addEventListener(
      'dragstart',
      function(e) {
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('Text', this.id);
        this.classList.add('drag');
        return false;
      },
      false
    );
    
    el.addEventListener(
      'dragend',
      function(e) {
        this.classList.remove('drag');
        return false;
      },
      false
    );
  };
});

mapProjectAppDirectives.directive('droppable', function() {
  return {
    scope: {
      drop: '&',
      bin: '='
    },
    link: function(scope, element) {
      // again we need the native object
      var el = element[0];
      
      el.addEventListener(
        'dragover',
        function(e) {
          e.dataTransfer.dropEffect = 'move';
          // allows us to drop
          if (e.preventDefault) e.preventDefault();
          this.classList.add('over');
          return false;
        },
        false
      );
      
      el.addEventListener(
        'dragenter',
        function(e) {
          this.classList.add('over');
          return false;
        },
        false
      );
      
      el.addEventListener(
        'dragleave',
        function(e) {
          this.classList.remove('over');
          return false;
        },
        false
      );
      
      el.addEventListener(
        'drop',
        function(e) {
          // Stops some browsers from redirecting.
          if (e.stopPropagation) e.stopPropagation();
          
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
        },
        false
      );
    }
  };
});






