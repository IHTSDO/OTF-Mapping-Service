// Refset Service
tsApp.service('refsetService', [
  '$http',
  '$rootScope',
  '$q',
  'Upload',
  'gpService',
  'utilService',
  'projectService',
  function($http, $rootScope, $q, Upload, gpService, utilService, projectService) {
    console.debug('configure refsetService');

    // Clear error
    utilService.clearError();

    // broadcasts a refset change
    this.fireRefsetChanged = function(refset) {
      $rootScope.$broadcast('refset:refsetChanged', refset);
    };

    // get refset revision
    this.getRefsetRevision = function(refsetId, date) {
      console.debug('getRefsetRevision');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + refsetId + '/' + date).then(
      // success
      function(response) {
        console.debug('  refset revision = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // find members of refset revision
    this.findRefsetRevisionMembersForQuery = function(refsetId, date, pfs) {
      console.debug('findRefsetRevisionMembersForQuery');
      var deferred = $q.defer();

      gpService.increment();
      $http.post(refsetUrl + refsetId + '/' + date + '/members', utilService.prepPfs(pfs)).then(
      // success
      function(response) {
        console.debug('  members = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get refset for id
    this.refset = function(refsetId) {
      console.debug('getRefset');
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + '/' + refsetId).then(
      // success
      function(response) {
        console.debug('  refset = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get refset member for id
    this.getMember = function(memberId) {
      console.debug('getMember');
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'member/' + memberId).then(
      // success
      function(response) {
        console.debug('  member = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get refsets for project
    this.getRefsetsForProject = function(projectId) {
      console.debug('getRefsetsForProject');
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'refsets/' + projectId).then(
      // success
      function(response) {
        console.debug('  projects = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // Finds refsets based on pfs parameter and query
    this.findRefsetsForQuery = function(query, pfs) {
      console.debug('findRefsetsForQuery', query, pfs);
      // Setup deferred
      var deferred = $q.defer();

      gpService.increment();
      $http.post(refsetUrl + 'refsets?query=' + utilService.prepQuery(query),
        utilService.prepPfs(pfs)).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // Adds refset members resolved from expression
    this.addRefsetMembersForExpression = function(refset, expression) {

      console.debug('addRefsetmembersForExpression', refset, expression);
      // Setup deferred
      var deferred = $q.defer();

      gpService.increment();
      $http.put(refsetUrl + 'members/add?refsetId=' + refset.id, expression, {
        headers : {
          'Content-type' : 'text/plain'
        }
      }).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // Removes refset members resolved from expression
    this.removeRefsetMembersForExpression = function(refset, expression) {

      console.debug('removeRefsetmembersForExpression', refset, expression);
      // Setup deferred
      var deferred = $q.defer();

      gpService.increment();
      $http['delete'](
        refsetUrl + 'members/remove?refsetId=' + refset.id + '&expression='
          + encodeURIComponent(expression)).then(
      // success
      function(response) {
        // empty response
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // add refset
    this.addRefset = function(refset) {
      console.debug('addRefset');
      var deferred = $q.defer();

      // Add refset
      gpService.increment();
      $http.put(refsetUrl + 'add', refset).then(
      // success
      function(response) {
        console.debug('  refset = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // clone refset
    this.cloneRefset = function(projectId, refset) {
      console.debug('cloneRefset');
      var deferred = $q.defer();

      // Clone refset
      gpService.increment();
      $http.put(refsetUrl + 'clone?projectId=' + projectId, refset).then(
      // success
      function(response) {
        console.debug('  refset = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // update refset
    this.updateRefset = function(refset) {
      console.debug('updateRefset');
      var deferred = $q.defer();

      gpService.increment();
      $http.post(refsetUrl + 'update', refset).then(
      // success
      function(response) {
        console.debug('  refset = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // remove refset
    this.removeRefset = function(refsetId) {
      console.debug('removeRefset');
      var deferred = $q.defer();

      gpService.increment();
      $http['delete'](refsetUrl + 'remove/' + refsetId + '?cascade=true').then(
      // success
      function(response) {
        // empty response
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // add refset member
    this.addRefsetMember = function(member) {
      console.debug('addRefsetMember');
      var deferred = $q.defer();

      gpService.increment();
      $http.put(refsetUrl + 'member/add', member).then(
      // success
      function(response) {
        console.debug('  refset member = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // remove refset member
    this.removeRefsetMember = function(memberId) {
      console.debug('removeRefsetMember');
      var deferred = $q.defer();

      // remove refset member
      gpService.increment();
      $http['delete'](refsetUrl + 'member/remove/' + memberId).then(
      // success
      function(response) {
        // empty response
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // remove refset member
    this.removeAllRefsetMembers = function(refsetId) {
      console.debug('removeAllRefsetMembers');
      var deferred = $q.defer();

      // remove refset member
      gpService.increment();
      $http['delete'](refsetUrl + 'member/remove/all/' + refsetId).then(
      // success
      function(response) {
        console.debug('  remove refset members = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // find members for refset
    this.findRefsetMembersForQuery = function(refsetId, query, pfs) {
      console.debug('findRefsetMembersForQuery');
      var deferred = $q.defer();

      // find members
      gpService.increment();
      $http.post(
        refsetUrl + 'members?refsetId=' + refsetId + '&query=' + utilService.prepQuery(query),
        utilService.prepPfs(pfs)).then(
      // success
      function(response) {
        console.debug('  members = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // add refset inclusion
    this.addRefsetInclusion = function(member, staged) {
      console.debug('addRefsetInclusion');
      var deferred = $q.defer();

      // Add refset inclusion
      gpService.increment();
      $http.put(refsetUrl + 'inclusion/add?staged=' + staged, member).then(
      // success
      function(response) {
        console.debug('  inclusion = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // add refset exclusion
    this.addRefsetExclusion = function(refset, conceptId, staged) {
      console.debug('addRefsetExclusion', refset, conceptId, staged);
      var deferred = $q.defer();

      // Add refset exclusion
      gpService.increment();
      $http.put(
        refsetUrl + 'exclusion/add/' + refset.id + '?&refsetId=' + refset.id + '&staged=' + staged,
        conceptId, {
          headers : {
            'Content-type' : 'text/plain'
          }
        }).then(
      // success
      function(response) {
        console.debug('  exclusion = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // remove refset inclusion
    this.removeRefsetExclusion = function(memberId) {
      console.debug('removeRefsetExclusion');
      var deferred = $q.defer();

      // Remove refset inclusion
      gpService.increment();
      $http['delete'](refsetUrl + 'exclusion/remove/' + memberId).then(
      // success
      function(response) {
        // empty response
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get import refset handlers
    this.getImportRefsetHandlers = function() {
      console.debug('getImportRefsetHandlers');
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'import/handlers').then(
      // success
      function(response) {
        console.debug('  import handlers = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get export refset handlers
    this.getExportRefsetHandlers = function() {
      console.debug('getExportRefsetHandlers');
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'export/handlers').then(
      // success
      function(response) {
        console.debug('  export handlers = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // compare refsets
    this.compareRefsets = function(refsetId1, refsetId2) {
      console.debug('compareRefsets');
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'compare?refsetId1=' + refsetId1 + '&refsetId2=' + refsetId2, {
        headers : {
          'Content-type' : 'text/plain'
        }
      }).then(
      // success
      function(response) {
        console.debug('  compare refsets = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // find members in common
    this.findMembersInCommon = function(reportToken, query, pfs, conceptActive) {
      console.debug('findMembersInCommon', query, pfs);
      // Setup deferred
      var deferred = $q.defer();

      // Make POST call
      gpService.increment();
      $http.post(
        refsetUrl + 'common/members' + '?reportToken=' + reportToken
          + (conceptActive != null ? '&conceptActive=' + conceptActive : '') + '&query='
          + utilService.prepQuery(query), utilService.prepPfs(pfs)).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // find members in diff
    this.getDiffReport = function(reportToken) {
      console.debug('getDiffReport');
      // Setup deferred
      var deferred = $q.defer();

      // Make POST call
      gpService.increment();
      $http.get(refsetUrl + 'diff/members?reportToken=' + reportToken).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // release report
    this.releaseReportToken = function(reportToken) {
      console.debug('releaseReportToken');
      // Setup deferred
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'release/report?reportToken=' + reportToken).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // optimize the refset definition
    this.optimizeDefinition = function(refsetId) {
      console.debug('optimizeDefinition');
      // Setup deferred
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'optimize/' + refsetId).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get definition for refset id
    this.extrapolateDefinition = function(refsetId) {
      console.debug('extrapolateDefinition');
      // Setup deferred
      var deferred = $q.defer();

      gpService.increment();
      $http.get(refsetUrl + 'definition/' + refsetId).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get list of old regular members from diff report
    this.getOldRegularMembers = function(reportToken, query, pfs, conceptActive) {
      console.debug('getOldRegularMembers');
      // Setup deferred
      var deferred = $q.defer();

      // Make POST call
      gpService.increment();
      $http.post(
        refsetUrl + 'old/members?reportToken=' + reportToken
          + (conceptActive != null ? '&conceptActive=' + conceptActive : '') + '&query='
          + utilService.prepQuery(query), utilService.prepPfs(pfs)).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get list of new regular members from diff report
    this.getNewRegularMembers = function(reportToken, query, pfs, conceptActive) {
      console.debug('getNewRegularMembers');
      // Setup deferred
      var deferred = $q.defer();

      // Make POST call
      gpService.increment();
      $http.post(
        refsetUrl + 'new/members?reportToken=' + reportToken
          + (conceptActive != null ? '&conceptActive=' + conceptActive : '') + '&query='
          + utilService.prepQuery(query), utilService.prepPfs(pfs)).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // get refset types
    this.getRefsetTypes = function() {
      console.debug('getRefsetTypes');
      var deferred = $q.defer();

      // Get refset types
      gpService.increment();
      $http.get(refsetUrl + 'types').then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
      });
      return deferred.promise;
    };

    this.beginRedefinition = function(refsetId, definition) {
      console.debug('beginRedefinition');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(
        refsetUrl + 'redefinition/begin?refsetId=' + refsetId + '&newDefinition='
          + encodeURIComponent(definition)).then(
      // success
      function(response) {
        console.debug('  refset revision = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.finishRedefinition = function(refsetId) {
      console.debug('finishRedefinition');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + 'redefinition/finish?refsetId=' + refsetId).then(
      // success
      function(response) {
        console.debug('  finish refset redefinition = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.cancelRedefinition = function(refsetId) {
      console.debug('cancelRedefinition');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + 'redefinition/cancel?refsetId=' + refsetId).then(
      // success
      function(response) {
        console.debug('  cancel refset redefinition = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.resumeRedefinition = function(refsetId) {
      console.debug('resumeRedefinition');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + 'redefinition/resume?refsetId=' + refsetId).then(
      // success
      function(response) {
        console.debug('  resume refset redefinition = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.getExportRefsetHandlers = function() {
      console.debug('getExportRefsetHandlers');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + 'export/handlers').then(
      // success
      function(response) {
        console.debug('  export refset handlers = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.exportDefinition = function(refset, handlerId, extension) {
      console.debug('exportDefinition');
      gpService.increment();
      $http.get(refsetUrl + 'export/definition?refsetId=' + refset.id + '&handlerId=' + handlerId)
        .then(
        // Success
        function(response) {
          var blob = new Blob([ response.data ], {
            type : ''
          });

          // fake a file URL and download it
          var fileURL = URL.createObjectURL(blob);
          var a = document.createElement('a');
          a.href = fileURL;
          a.target = '_blank';
          a.download = 'definition.' + refset.terminologyId + '.' + extension;
          document.body.appendChild(a);
          gpService.decrement();
          a.click();
          window.URL.revokeObjectURL(fileURL);

        },
        // Error
        function(response) {
          utilService.handleError(response);
          gpService.decrement();
        });
    };

    this.exportMembers = function(refset, handlerId, extension) {
      console.debug('exportMembers');
      gpService.increment();
      $http.get(refsetUrl + 'export/members?refsetId=' + refset.id + '&handlerId=' + handlerId)
        .then(
        // Success
        function(response) {
          var blob = new Blob([ response.data ], {
            type : ''
          });

          // fake a file URL and download it
          var fileURL = URL.createObjectURL(blob);
          var a = document.createElement('a');
          a.href = fileURL;
          a.target = '_blank';
          a.download = 'members.' + refset.terminologyId + '.' + extension;
          document.body.appendChild(a);
          gpService.decrement();
          a.click();

        },
        // Error
        function(response) {
          utilService.handleError(response);
          gpService.decrement();
        });
    };

    // Begin import members - if validation is result, OK to proceed.
    this.beginImportMembers = function(refsetId, handlerId) {
      console.debug('begin import members');
      var deferred = $q.defer();
      gpService.increment();
      $http.get(refsetUrl + 'import/begin?refsetId=' + refsetId + '&handlerId=' + handlerId).then(
      // success
      function(response) {
        console.debug('  validation result = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // Cancel import members
    this.cancelImportMembers = function(refsetId) {
      console.debug('cancel import members');
      var deferred = $q.defer();
      gpService.increment();
      $http.get(refsetUrl + 'import/cancel?refsetId=' + refsetId).then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // Finish import members - if validation is result, OK to proceed.
    this.finishImportMembers = function(refsetId, handlerId, file) {
      console.debug('finish import members');
      var deferred = $q.defer();
      gpService.increment();
      Upload.upload({
        url : refsetUrl + 'import/finish?refsetId=' + refsetId + '&handlerId=' + handlerId,
        data : {
          file : file
        }
      }).then(
      // Success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);

      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      },
      // event
      function(evt) {
        var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
        console.debug('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
      });
      return deferred.promise;
    };

    // import definition
    this.importDefinition = function(refsetId, handlerId, file) {
      console.debug('finish import members');
      var deferred = $q.defer();
      gpService.increment();
      Upload.upload({
        url : refsetUrl + 'import/definition?refsetId=' + refsetId + '&handlerId=' + handlerId,
        data : {
          file : file
        }
      }).then(
      // Success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);

      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      },
      // event
      function(evt) {
        var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
        console.debug('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
      });
      return deferred.promise;
    };

    this.beginMigration = function(refsetId, terminology, version) {
      console.debug('beginMigration');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(
        refsetUrl + 'migration/begin?refsetId=' + refsetId + '&newTerminology=' + terminology
          + '&newVersion=' + version).then(
      // success
      function(response) {
        console.debug('  refset revision = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.finishMigration = function(refsetId) {
      console.debug('finishMigration');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + 'migration/finish?refsetId=' + refsetId).then(
      // success
      function(response) {
        console.debug('  finish refset migration = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.cancelMigration = function(refsetId) {
      console.debug('cancelMigration');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + 'migration/cancel?refsetId=' + refsetId).then(
      // success
      function(response) {
        console.debug('  cancel refset migration = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.resumeMigration = function(refsetId) {
      console.debug('resumeMigration');
      var deferred = $q.defer();

      // get refset revision
      gpService.increment();
      $http.get(refsetUrl + 'migration/resume?refsetId=' + refsetId).then(
      // success
      function(response) {
        console.debug('  resume refset migration = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.addRefsetNote = function(refsetId, note) {
      console.debug('add refset note', refsetId, note);
      var deferred = $q.defer();

      // Add refset
      gpService.increment();
      $http.put(refsetUrl + 'add/note?refsetId=' + refsetId, note, {
        headers : {
          'Content-type' : 'text/plain'
        }
      }).then(
      // success
      function(response) {
        console.debug('  note = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.removeRefsetNote = function(refsetId, noteId) {
      console.debug('remove refset note', refsetId, noteId);
      var deferred = $q.defer();

      gpService.increment();
      $http['delete'](refsetUrl + '/remove/note?refsetId=' + refsetId + '&noteId=' + noteId).then(
      // success
      function(response) {
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.addRefsetMemberNote = function(refsetId, memberId, note) {
      console.debug('add member note', refsetId, memberId, note);
      var deferred = $q.defer();

      // Add refset
      gpService.increment();
      $http.put(refsetUrl + 'member/add/note?refsetId=' + refsetId + '&memberId=' + memberId, note,
        {
          headers : {
            'Content-type' : 'text/plain'
          }
        }).then(
      // success
      function(response) {
        console.debug('  note = ', response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    this.removeRefsetMemberNote = function(memberId, noteId) {
      console.debug('remove member note', memberId, noteId);
      var deferred = $q.defer();

      gpService.increment();
      $http['delete'](refsetUrl + '/member/remove/note?memberId=' + memberId + '&noteId=' + noteId)
        .then(
        // success
        function(response) {
          // empty response
          gpService.decrement();
          deferred.resolve(response.data);
        },
        // error
        function(response) {
          utilService.handleError(response);
          gpService.decrement();
          deferred.reject(response.data);
        });
      return deferred.promise;
    };

    // get the progress of the name/status member lookup process
    this.getLookupProgress = function(refsetId) {
      console.debug('getLookupProgress');
      // Setup deferred
      var deferred = $q.defer();

      $http.get(refsetUrl + 'lookup/status?refsetId=' + refsetId, {
        headers : {
          'Content-type' : 'text/plain'
        }
      }).then(
      // success
      function(response) {
        console.debug('  output = ', response.data);
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // start lookup of member names/statuses
    this.startLookup = function(refsetId) {
      console.debug('startLookup');
      var deferred = $q.defer();

      // get refset revision
      $http.get(refsetUrl + 'lookup/start?refsetId=' + refsetId).then(
      // success
      function(response) {
        console.debug('  start lookup names = ', response.data);
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        deferred.reject(response.data);
      });
      return deferred.promise;
    };

    // checks if expression is valid
    this.isExpressionValid = function(expression, terminology, version) {
      console.debug('isExpressionValid');
      var deferred = $q.defer();
      
      // Get project roles
      gpService.increment();
      $http.post(refsetUrl + 'expression/valid?terminology=' + terminology + '&version=' + version,
        expression, {
          headers : {
            'Content-type' : 'text/plain'
          }
        }).then(
      // success
      function(response) {
        console.debug('  expression valid = ' + response.data);
        gpService.decrement();
        deferred.resolve(response.data);
      },
      // error
      function(response) {
        utilService.handleError(response);
        gpService.decrement();
        deferred.reject(response.data);
      });
      return deferred.promise;
    };
    // end
  } ]);
