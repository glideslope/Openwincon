
var app = angular.module('webApp', []);


app.config(['$interpolateProvider', '$httpProvider', function($interpolateProvider, $httpProvider) {
  $interpolateProvider.startSymbol('{[');
  $interpolateProvider.endSymbol(']}')

}]);


app.controller('qosCtrl', ['$rootScope', '$scope', '$http', function($rootScope, $scope, $http) {
  $scope.hideform = true;
  $scope.options = {
    devices:['wired', 'wireless']
  };

  $scope.submit_form = {
    ssid: '',
    ap_ip: '',
    target_ifname: '',
    sudoer_id: '',
    sudoer_passwd:''
  };

  $scope.getList = function() {
    //$http.get("http://interest.snu.ac.kr:8000/api/qos/all")
      $http.get("http://147.47.208.159:8000/api/qos/all")
      .success(function(response) {
        $scope.aps = response;
      });
  };


  $scope.getList();

  $scope.editRecord = function(ap) {
    $scope.hideform = false;
    if (ap== 'new') {
      $scope.edit = true;
      $scope.incomplete = true;
      $scope.modified = false;

      $scope.submit_form.id ='';

      $scope.submit_form.ssid = '';
      $scope.submit_form.ap_ip= '';
      $scope.submit_form.target_ifname= '';
      $scope.submit_form.sudoer_id= '';
      $scope.submit_form.sudoer_passwd='';

    } else {
      $scope.edit = false;
      $scope.modified = true;

      $scope.submit_form.id =ap.id;

      $scope.submit_form.ssid = ap.ssid; 
      $scope.submit_form.ap_ip= ap.ap_ip;
      $scope.submit_form.target_ifname=ap.target_ifname;
      $scope.submit_form.sudoer_id=ap.sudoer_id;
      $scope.submit_form.sudoer_passwd=ap.sudoer_passwd;

    }

  };

  $scope.delRecord = function(ap) {
    //var _url = "http://interest.snu.ac.kr:8000/api/qos/" + ap.id;
    var _url = "http://147.47.208.159:8000/api/qos/" + ap.ssid;
      $http.delete(_url).success(function(response) {
        $scope.getList();
        $scope.hideform = true;
      });
  };

  $scope.submit = function() {
    //var _url = "http://interest.snu.ac.kr:8000/api/qos/" + $scope.submit_form.d;
    var _url = "http://147.47.208.159:8000/api/qos/" + $scope.submit_form.d;
    var req = {
      method:'POST',
      url:_url,
      headers:{'Content-Type':undefined},
      data:$scope.submit_form
    };

    if (!$scope.modified) {
      req.method = 'PUT'; // temporary!
      $http(req).success(function(response) {
        $scope.getList();
        $scope.hideform = true;
      });
    } else {
      $http(req).success(function(response) {
        $scope.getList();
        $scope.hideform = true;
      });
    }
  };


  $scope.$watch('submit_form.ssid', function() {$scope.test();});
  $scope.$watch('submit_form.ap_ip', function() {$scope.test();});
  $scope.$watch('submit_form.target_ifname', function() {$scope.test();});
  $scope.$watch('submit_form.sudoer_id', function() {$scope.test();});
  $scope.$watch('submit_form.sudoer_passwd', function() {$scope.test();});
  

  $scope.test = function() {
    $scope.incomplete = false;
    if ($scope.submit_form.ssid == "" ||
        $scope.submit_form.ap_ip == "" ||
        $scope.submit_form.target_ifname == "" ||
        $scope.submit_form.sudoer_id == "" ||
        $scope.submit_form.sudoer_passwd == "") {
      $scope.incomplete = true;
    }
  };



}]);


