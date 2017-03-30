'use strict';

/* Filters */

mapProjectApp.filter('highlight', function($sce) {
  return function(text, phrase) {
    // console.debug("higlight", text, phrase);
    var htext = text;
    if (text && phrase) {
      htext = text.replace(new RegExp('(' + phrase.replace(/"/g, '') + ')', 'gi'),
        '<span class="highlighted">$1</span>');
    }
    return $sce.trustAsHtml(htext);
  };
});

mapProjectApp.filter('highlightLabelFor', function($sce) {
  return function(text, phrase) {
    // console.debug("higlightLabelFor", text, phrase);
    var htext = text;
    if (text && phrase) {
      htext = text.replace(new RegExp('(' + phrase.replace(/"/g, '') + ')', 'gi'),
        '<span style="background-color:#e0ffe0;">$1</span>');
    }
    return $sce.trustAsHtml(htext);
  };
});

mapProjectApp.filter('highlightLabel', function($sce) {
  return function(text, phrase) {
    // console.debug("higlightLabel", text, phrase);
    var htext = text;
    if (text && phrase) {
      htext = text.replace(new RegExp('(' + phrase.replace(/"/g, '') + ')', 'gi'),
        '<span style="background-color:#e0e0ff;">$1</span>');
    }
    return $sce.trustAsHtml(htext);
  };
});