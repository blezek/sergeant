'use strict';
console.log("starting up");
var sergeantApp = angular.module('sergeantApp',['ui.bootstrap']);

//We already have a limitTo filter built-in to angular,
//let's make a startFrom filter
sergeantApp.filter('startFrom', function() {
    return function(input, start) {
        start = +start; //parse to int
        // console.log("Starting at " + start, input )
        return input.slice(start);
    }
});

sergeantApp.controller ('JobController', function($scope,$http,$timeout) {
  $scope.jobs = {}
  $scope.status = null;
  $scope.pageSize = 25;
  $scope.currentPage = 1;
  $scope.numberOfJobs = 0;
  $scope.numberOfPages = function() {
    return Math.ceil($scope.jobs.length/$scope.pageSize);
  }

  // Cleanup
  $scope.cleanup = function(job) {
    $http({method: "DELETE", url: "rest/job/" + job.uuid}).
    success(function(data,status,headers,config){
      for(var i = 0; i < $scope.jobs.length; i++) {
        if ( $scope.jobs[i].uuid == job.uuid ) {
          $scope.jobs.splice(i,1)
        }
      }
    }).
    error( function(data,status,headers,config) {
      bootbox.alert("Failed to delete " + job.endPoint + ": " + status);
    });
  };

  $scope.cleanupAll = function() {
    for(var i = 0; i < $scope.jobs.length; i++) {
      var job = $scope.jobs[i];
      if ( job.status == "done") {
        $scope.cleanup(job);
      }
    }
  };


  $scope.update = function() {
    $http({method: 'GET', url: 'rest/job'}).
    success(function(data,status,headers,config) {
      $scope.jobs = data;
      $scope.numberOfJobs = $scope.jobs.length;
      $timeout($scope.update, 5000);
      $scope.status = null;
    }).
    error(function(data,status,headers,config) {
      $scope.status = "failed to get services"
      $timeout($scope.update, 1000);
    });
  };
  // Kick it off
  $scope.update();


});

  sergeantApp.controller ('ServiceController', function($scope,$http,$timeout) {
    $scope.services = {}
    $scope.status = null;
    $scope.update = function() {
      $http({method: 'GET', url: 'rest/service'}).
      success(function(data,status,headers,config) {
        $scope.services = data;
        $timeout($scope.update, 5000);
        $scope.status = null;
      }).
      error(function(data,status,headers,config) {
        $scope.status = "failed to get services"
        $timeout($scope.update, 1000);
      });
    };
    // Kick it off
    $scope.update();
  });
