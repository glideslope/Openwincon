# Global DASH Streaming  

<br>

# Play Order  
SDN Aplication -> Media Server -> AP -> DASH Client

<br>

# list_device.csv   
각 요소의 IP 및 port 정보를 담은 csv 파일    
서버의 경우 "xxx.xxx.xxx.xxx"로 되어 있는데 이를 해당 서버 IP 주소에 맞게 수정해야함   

<br>

# Test Setting  
3개의 비디오 사용  

 - Big Buck Bunny
 - Elephants Dream
 - Sintel
    <br>
    인코딩 bitrate

- [200, 400, 600, 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800, 3000] kbps
  <br>


<br>

Bandwidth 모델
- ![404 Not Found](_image/formula_bandwidth.png?raw=true)

<br>

# Reference   
- Dash Example
```
http://www-itec.uni-klu.ac.at/dash/js/dash-js/dashtest-ibmff.html  
```
- Videos
```
https://media.xiph.org/
```
- SDN Application Example
```
http://uni2u.tistory.com/44 
```
- Python Simple Web Server
```
https://gist.github.com/bradmontgomery/2219997
```
