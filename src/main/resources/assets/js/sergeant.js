'use strict';
console.log("starting up");
var sergeantApp = angular.module('sergeantApp',[]);

sergeantApp.controller ('JobController', function($scope,$http,$timeout) {
  $scope.jobs = {}
  $scope.status = null;
  $scope.update = function() {
    $http({method: 'GET', url: 'rest/job'}).
    success(function(data,status,headers,config) {
      $scope.jobs = data;
      $timeout($scope.update, 5000);
      $scope.status = null;
    }).
    error(function(data,status,headers,config) {
      $scope.status = "failed to get services"
      $timeout($scope.update, 1000);
    });
  };
  // Kick it off
  $scope.update();});

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
