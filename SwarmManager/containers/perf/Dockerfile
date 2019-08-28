FROM python:3

RUN pip install psutil
RUN pip install requests

WORKDIR "/home"
RUN mkdir -p /home/result

ADD perf.py /home

CMD ["python", "perf.py"]
