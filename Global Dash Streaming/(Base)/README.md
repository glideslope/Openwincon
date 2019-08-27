# pom.xml 만들기   

mvn archetype:generate -Dfilter=org.onosproject: -DarchetypeGroupId=org.onosproject -DarchetypeArtifactId=onos-bundle-archetype -DarchetypeVersion=**Version**  
<br>

예시)  
```
mvn archetype:generate -Dfilter=org.onosproject: -DarchetypeGroupId=org.onosproject -DarchetypeArtifactId=onos-bundle-archetype -DarchetypeVersion=1.13.0
```

<br>

Define value for property 'groupId': **Group**  

Define value for property 'artifactId': **App**  

**ETC:** `Enter`  
<br>

예시)
```
Define value for property 'groupId': kr.ac.postech 
Define value for property 'artifactId': app
<Enter>...
```
<br>

# 프로젝트 컴파일하기   

일반적인 경우)  
```
mvn clean install
```

테스트 코드가 없을 때)  
```
mvn clean install -Dmaven.test.skip=true
```

<br>

# 어플리케이션 설치 및 실행    

(어플리케이션은 **ONOS 콘솔**에서 설치되어야 함)  
bundle:install mvn:**Group**/**App**/1.0-SNAPSHOT  
start **App**  
<br>

예시)  
```
bundle:install mvn:kr.ac.postech/app/1.0-SNAPSHOT
start app
```
