# 크롬 브라우저 실행 옵션
사용 브라우저로 **크롬 브라우저**를 사용했으며,   
크롬 브라우저를 사용할 때 아래와 같은 **옵션**을 사용하여 브라우저를 실행  
```
(root mode)
google-chrome --allow-file-access-from-files --no-sandbox 
```

<br>

# 추가적인 플러그인 설치

<br>

Falcon Proxy  
```
https://chrome.google.com/webstore/detail/falcon-proxy/gchhimlnjdafdlkojbffdkogjhhkdepf?hl=ko
```

<br>

Allow-Control-Allow-Origin: *  
```
https://chrome.google.com/webstore/detail/allow-control-allow-origi/nlfbmbojpeacfghkpbjhddihlkkiljbi
```

<br>

**Allow-Control-Allow-Origin: * 플러그인에서** 아래 **헤더**를 추가해야 함
```
Access-Control-Allow-Headers: Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers
```
