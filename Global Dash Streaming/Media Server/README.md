# Media Server
python 폴더  
- web_server.py 실행 (여기서는 list_device.csv 수정할 필요 없음)

<br>

html 폴더
- js 폴더에 필요한 아래의 라이브러리 설치

<br>

media 폴더  
- mpd 파일 저장
- mp4 파일 저장
- 인코딩 된 m4s 파일 저장 (아래 사항 참고)

<br>

# 추가적인 라이브러리 설치

<br>

video.js
```
https://unpkg.com/video.js/dist/video.js
```
videojs-dash.js
```
https://unpkg.com/videojs-contrib-dash/dist/videojs-dash.js
```
dash.all.debug.js
```
//cdn.dashjs.org/latest/dash.all.debug.js
```

<br>

# 영상 인코딩
- 필요 소프트웨어

<br>

FFmpeg
```
https://www.ffmpeg.org/
```
MP4Box
```
https://gpac.wp.imt.fr/tag/mp4box/
```

<br>

- 원본 -> mp4로 변환하기
```
ffmpeg -i <원본 파일> -c:v libx264 -preset ultrafast -qp 0 -pix_fmt yuv420p -movflags +faststart <mp4 파일>

ex)
ffmpeg -i bunny.y4m -c:v libx264 -preset ultrafast -qp 0 -pix_fmt yuv420p -movflags +faststart bunny.mp4
```

<br>

- 특정 비트레이트로 인코딩
```
ffmpeg -i <mp4 파일> -b:v <비트레이트> <해당 비트레이트로 인코딩된 mp4파일>

ex)
ffmpeg -i bunny.mp4 -b:v 200K bunny_200K.mp4
```

<br>

- DASH 세그먼트 만들기
```
MP4Box -dash <재생 시간(ms)> -profile live -out dash.mp4 <특정 비트레이트로 인코딩된 mp4파일> <특정 비트레이트로 인코딩된 mp4파일> ...

ex)
MP4Box -dash 2000 -profile live -out dash.mp4 bunny_200K.mp4 bunny_300K.mp4 bunny_400K.mp4
```