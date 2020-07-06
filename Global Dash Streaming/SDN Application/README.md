# SDN Application
java 폴더  
- list_allow.csv에서 허용할 UE와 AP의 MAC 등록 (xx:xx:xx:xx:xx:xx를 해당 MAC 주소로 변경)
- list_allow.csv에서 group은 같은 device를 나타냄 (multipath이므로 한 device당 두개의 MAC 주소를 가지는데 이를 묶기 위함)

<br>

csv 폴더

- PSNR 계산에 필요한 csv 파일 저장

<br>

# 실행 환경  
- 리눅스에서만 실행 가능  

<br>

# pom.xml 만들기   

mvn archetype:generate -Dfilter=org.onosproject: -DarchetypeGroupId=org.onosproject -DarchetypeArtifactId=onos-bundle-archetype -DarchetypeVersion=**Version**  
<br>

Example)  
```
mvn archetype:generate -Dfilter=org.onosproject: -DarchetypeGroupId=org.onosproject -DarchetypeArtifactId=onos-bundle-archetype -DarchetypeVersion=1.13.0
```

<br>

Define value for property 'groupId': **Group**  

Define value for property 'artifactId': **App**  

**ETC:** `Enter`  
<br>

Example)
```
Define value for property 'groupId': kr.ac.postech 
Define value for property 'artifactId': app
<Enter>...
```
<br>

# Project 컴파일 하기  

Normal)  
```
mvn clean install
```

No Test Code)  
```
mvn clean install -Dmaven.test.skip=true
```
<br>

# Application 설치 및 실행하기  

(Application can be installed **with ONOS console**)  
bundle:install mvn:**Group**/**App**/1.0-SNAPSHOT  
start **App**  
<br>

Example)  
```
bundle:install mvn:kr.ac.postech/app/1.0-SNAPSHOT
start app
```
×
Drag and Drop
The image will be downloaded