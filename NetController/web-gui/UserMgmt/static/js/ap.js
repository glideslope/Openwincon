
var app = angular.module('webApp', []);


app.config(['$interpolateProvider', '$httpProvider', function($interpolateProvider, $httpProvider) {
  $interpolateProvider.startSymbol('{[');
  $interpolateProvider.endSymbol(']}')

}]);


app.controller('apCtrl', ['$rootScope', '$scope', '$http', function($rootScope, $scope, $http) {
  $scope.hideform = true;
  $scope.hideformeNB = true;
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
  
  $scope.submit_form2 = {
    ssid: '',
    ap_ip: '',
    target_ifname: '',
    sudoer_id: '',
    sudoer_passwd:'',
    if_type: 'oai'
  };

  $scope.getList = function() {
    //$http.get("http://interest.snu.ac.kr:8000/api/qos/all")
      $http.get("http://147.47.208.159:8000/api/qos/all")
      .success(function(response) {
	console.log(response);
        $scope.aps = response;
      });
  };


  $scope.getList();

  $scope.editRecord = function(ap) {
    $scope.hideform = false;
    $scope.hideformeNB = true;
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
  
  $scope.editRecordeNB = function(ap) {
    $scope.hideform = true;
    $scope.hideformeNB = false;
    if (ap== 'new') {
      $scope.editeNB = true;
      $scope.incompleteeNB = true;
      $scope.modifiedeNB = false;

      $scope.submit_form2.id ='';

      $scope.submit_form2.ssid = '';
      $scope.submit_form2.ap_ip= '';
      $scope.submit_form2.target_ifname= '';
      $scope.submit_form2.sudoer_id= '';
      $scope.submit_form2.sudoer_passwd='';

    } else {
      $scope.editeNB = false;
      $scope.modifiedeNB = true;

      $scope.submit_form2.id =ap.id;

      $scope.submit_form2.ssid = ap.ssid; 
      $scope.submit_form2.ap_ip= ap.ap_ip;
      $scope.submit_form2.target_ifname=ap.target_ifname;
      $scope.submit_form2.sudoer_id=ap.sudoer_id;
      $scope.submit_form2.sudoer_passwd=ap.sudoer_passwd;

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

  $scope.submiteNB = function() {
    var _url = "http://147.47.208.159:8000/api/qos/" + $scope.submit_form2.d;
    var req = {
      method:'POST',
      url:_url,
      headers:{'Content-Type':undefined},
      data:$scope.submit_form2
    };

    if (!$scope.modifiedeNB) {
      req.method = 'PUT'; // temporary!
      $http(req).success(function(response) {
        $scope.getList();
        $scope.hideformeNB = true;
      });
    } else {
      $http(req).success(function(response) {
        $scope.getList();
        $scope.hideformeNB = true;
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


  $scope.$watch('submit_form2.ssid', function() {$scope.test2();});
  $scope.$watch('submit_form2.ap_ip', function() {$scope.test2();});
  $scope.$watch('submit_form2.target_ifname', function() {$scope.test2();});
  $scope.$watch('submit_form2.sudoer_id', function() {$scope.test2();});
  $scope.$watch('submit_form2.sudoer_passwd', function() {$scope.test2();});

  $scope.test2 = function() {
    $scope.incompleteeNB = false;
    if ($scope.submit_form2.ssid == "" ||
        $scope.submit_form2.ap_ip == "" ||
        $scope.submit_form2.target_ifname == "" ||
        $scope.submit_form2.sudoer_id == "" ||
        $scope.submit_form2.sudoer_passwd == "") {
      $scope.incompleteeNB = true;
    }
  };
}]);


