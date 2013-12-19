
function SecondCtrl($scope, $http) {
	$http({method: 'GET', url: 'http://localhost:8080/mapping-rest/mapping/project/projects'}).
	success(function(response) {
		$scope.mapProjects = response.mapProjects;
    	$scope.data = {message: "success"}; 
        console.log('success');  
        console.log(response);
    }).
    error(function(response) {
    	$scope.data = {message: "error"};
        console.log('failed');  
        console.log(response);
    });      
    
} 