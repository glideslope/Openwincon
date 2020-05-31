
var app = angular.module('webApp', []);
var serverip = 'http://147.47.208.159:8000'


app.config(['$interpolateProvider', '$httpProvider', function($interpolateProvider, $httpProvider) {
  $interpolateProvider.startSymbol('{[');
  $interpolateProvider.endSymbol(']}')

}]);


app.controller('qosCtrl', ['$rootScope', '$scope', '$http', function($rootScope, $scope, $http) {
  $scope.hideSliceForm = true;
  $scope.hideQoSForm = true;
  $scope.options = {
    devices:['wired', 'wireless']
  };

  $scope.submit_form = {
    ssid: '',
    ip: '',
    id: '',
    passwd: '',
    rate:''
  };

  $scope.submit_form2 = {
    ssid: '',
    ip: '',
    rate: ''
  };

  $scope.getList = function() {
      $http.get(serverip + "/api/slice")
      .success(function(response) {
	console.log(response.slices);
        $scope.aps = response.slices;
      });
  };


  $scope.getList();

  $scope.editRecord = function(ap) {
    $scope.hideSliceForm = false;
    $scope.hideQoSForm = true;
    if (ap== 'new') { // Create
      $scope.inputSlice = true;
      $scope.incompleteSlice = true;
      $scope.modifiedSlice = false;

      $scope.submit_form.id ='';
      $scope.submit_form.ip ='';
      $scope.submit_form.ssid = '';
      $scope.submit_form.passwd = '';
      $scope.submit_form.rate = '';

    } else { // Edit
      $scope.inputSlice = false;
      $scope.modifiedSlice = true;

      $scope.submit_form.id = ap.id;
      $scope.submit_form.ip = ap.ip;
      $scope.submit_form.ssid = ap.ssid; 
      $scope.submit_form.passwd = ap.passwd;
      $scope.submit_form.rate = ap.rate;
    }

  };

  $scope.editQoS = function(ap) {
    $scope.hideSliceForm = true;
    $scope.hideQoSForm = false;
    if (ap== 'new') {
      $scope.inputQoS = true;
      $scope.incompleteQoS = true;
      $scope.modifiedQoS = false;

      $scope.submit_form2.ssid = '';
      $scope.submit_form2.ip = '';
      $scope.submit_form2.rate = '';
    } else {
      $scope.inputQoS = false;
      $scope.modifiedQoS = true;

      $scope.submit_form2.ssid = ap.ssid;
      $scope.submit_form2.ip = ap.ip;
      $scope.submit_form2.rate = ap.rate;
    }

  };

  $scope.delRecord = function(ap) {
    var _url = serverip + "/api/slice";
    var req = {
      method:'DELETE',
      url:_url,
      headers:{'Content-Type':undefined},
      params:{'ssid':ap.ssid, 'ip':ap.ip, 'id':ap.id}
    };

    $http(req).success(function(response) {
      $scope.getList();
      $scope.hideSliceForm = true;
    });
  };

  $scope.submitSlice = function() {
    var _url = serverip + "/api/slice";
    var req = {
      method:'POST',
      url:_url,
      headers:{'Content-Type':undefined},
      params:$scope.submit_form
    };

    if (!$scope.modifiedSlice) {
      req.method = 'PUT';
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

  $scope.submitQoS = function() {
    var _url = serverip + "/api/sliceqos";
    var req = {
      method:'POST',
      url:_url,
      headers:{'Content-Type':undefined},
      params:$scope.submit_form2
    };

    if (!$scope.modifiedQoS) {
      req.method = 'PUT';
      $http(req).success(function(response) {
	$scope.getList();
	$scope.hideQoSForm = true;
      });
    } else {
      $http(req).success(function(response) {
        $scope.getList();
        $scope.hideQoSForm = true;
      });
    }
  };

  $scope.$watch('submit_form.ssid', function() {$scope.test();});
  $scope.$watch('submit_form.id', function() {$scope.test();});
  $scope.$watch('submit_form.ip', function() {$scope.test();});
  $scope.$watch('submit_form.passwd', function() {$scope.test();});
  $scope.$watch('submit_form2.ssid', function() {$scope.test2();});
  $scope.$watch('submit_form2.ip', function() {$scope.test2();});
  $scope.$watch('submit_form2.rate', function() {$scope.test2();});

  

  $scope.test = function() {
    $scope.incompleteSlice = false;
    if ($scope.submit_form.ssid == "" ||
	$scope.submit_form.ip == "" ||
        $scope.submit_form.id == "" ||
        $scope.submit_form.passwd == "") {
      $scope.incompleteSlice = true;
    }
  };

  $scope.test2 = function() {
    $scope.incompleteQoS = false;
    if ($scope.submit_form2.ssid == "" ||
	$scope.submit_form2.ip == "" ||
	$scope.submit_form2.rate == "") {
      $scope.incompleteQoS = true;
    }
  };

}]);


