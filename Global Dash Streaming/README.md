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

- [50, 100, 150, 200, 250, 300, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000, 1050, 1100, 1150, 1200, 1250, 1300, 1350, 1400, 1450, 1500, 1550, 1600, 1650, 1700, 1750, 1800, 1850, 1900, 1950, 2000, 2500, 3000, 4000, 5000, 6000, 8000] kbps
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
