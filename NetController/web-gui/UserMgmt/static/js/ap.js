
var app = angular.module('webApp', []);
var serverip = '147.47.208.159:8000'


app.config(['$interpolateProvider', '$httpProvider', function($interpolateProvider, $httpProvider) {
  $interpolateProvider.startSymbol('{[');
  $interpolateProvider.endSymbol(']}')

}]);


app.controller('apCtrl', ['$rootScope', '$scope', '$http', function($rootScope, $scope, $http) {
  $scope.hideSliceForm = true;
  $scope.hideQoSForm = true;
  $scope.options = {
    devices:['wired', 'wireless']
  };

  $scope.submit_form = {
    ssid: '',
    id: '',
    passwd: '',
    rate:''
  };

  $scope.getList = function() {
      $http.get(serverip + "/api/slice")
      .success(function(response) {
        $scope.aps = response;
      });
  };


  //$scope.getList();

  $scope.editRecord = function(ap) {
    $scope.hideSliceForm = false;
    if (ap== 'new') {
      $scope.editSlice = true;
      $scope.incompleteSlice = true;
      $scope.modifiedSlice = false;

      $scope.submit_form.id ='';
      $scope.submit_form.ssid = '';
      $scope.submit_form.passwd = '';
      $scope.submit_form.rate = '';

    } else {
      $scope.editSlice = false;
      $scope.modifiedSlice = true;

      $scope.submit_form.id =ap.id;

      $scope.submit_form.ssid = ap.ssid; 
      $scope.submit_form.passwd = ap.passwd;
      $scope.submit_form.rate = ap.rate;
    }

  };

  $scope.editQoS = function(ap) {
    $scope.hideQoSForm = false;
    if (ap== 'new') {
      $scope.editSlice = true;
      $scope.incompleteSlice = true;
      $scope.modifiedSlice = false;

      $scope.submit_form.ssid = '';
      $scope.submit_form.rate = '';
    }
  };

  $scope.delRecord = function(ap) {
    var _url = serverip + "/api/slice";
    var req = {
      method:'DELETE',
      url:_url,
      headers:{'Content-Type':undefined},
      data:$scope.submit_form
    };

    $http(req).success(function(response) {
      $scope.getList();
      $scope.hideSliceForm = true;
    });
  };

  $scope.submitSlice = function() {
    var _url = serverip + "/api/qos/" + $scope.submit_form.d;
    var req = {
      method:'POST',
      url:_url,
      headers:{'Content-Type':undefined},
      data:$scope.submit_form
    };

    if ($scope.modifiedSlice) {
      req.method = 'PUT'; // temporary!
      $http(req).success(function(response) {
        $scope.getList();
        $scope.hideSliceForm = true;
      });
    } else {
      $http(req).success(function(response) {
        $scope.getList();
        $scope.hideSliceForm = true;
      });
    }
  };


  $scope.$watch('submit_form.ssid', function() {$scope.test();});
  $scope.$watch('submit_form.id', function() {$scope.test();});
  $scope.$watch('submit_form.passwd', function() {$scope.test();});
  $scope.$watch('submit_form.rate', function() {$scope.test();});
  

  $scope.test = function() {
    $scope.incompleteSlice = false;
    if ($scope.submit_form.ssid == "" ||
        $scope.submit_form.id == "" ||
        $scope.submit_form.passwd == "" ||
        $scope.submit_form.rate == "") {
      $scope.incompleteSlice = true;
    }
  };



}]);


