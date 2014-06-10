var DemoCtrl = function($scope, localStorageService) {
    
  localStorageService.clearAll();

  $scope.$watch('localStorageDemo', function(value){
    localStorageService.add('localStorageDemo',value);
    $scope.localStorageDemoValue = localStorageService.get('localStorageDemo');
  });

  $scope.storageType = 'Local storage';

  if (!localStorageService.isSupported()) {
    $scope.storageType = 'Cookie';
  }

};
