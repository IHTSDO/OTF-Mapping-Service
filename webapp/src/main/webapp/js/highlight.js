/**
 * Wraps the
 * 
 * @param text
 *          {string} haystack to search through
 * @param search
 *          {string} needle to search for
 * @param [caseSensitive]
 *          {boolean} optional boolean to use case-sensitive searching
 */
angular.module('ui.highlight', []).filter('highlight', function(highlight) {
  return function(text, search, caseSensitive) {
    if (search || angular.isNumber(search)) {
      var ltext = text.toString();
      var lsearch = search.toString();
      if (caseSensitive) {
        return ltext.split(lsearch).join('<span class="ui-match">' + lsearch + '</span>');
      } else {
        return ltext.replace(new RegExp(lsearch, 'gi'), '<span class="ui-match">$&</span>');
      }
    } else {
      return text;
    }
  };
});
