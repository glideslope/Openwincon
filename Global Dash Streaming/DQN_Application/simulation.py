import math
import numpy as np
import random

# UE와 AP 사이 정보
SIZE_INFO = 3
CONST_CONNECTABLE = 0
CONST_AVAILABLE = 1
CONST_REQUEST = 2

# Time slot 인덱스
REMAIN_TIMESLOT = 0

# Bandwidth 정보
BW_AVG = 2000
BW_STD = 500

class Simulation:
	def __init__(self, data):
		self.NUM_UE = data['NUM_UE']
		self.NUM_AP = data['NUM_AP']
		self.PERCENT_CONNECT = data['PERCENT_CONNECT']
		self.VAL_TIMESLOT = data['VAL_TIMESLOT']
		
		self.list_rate = data['LIST_RATE']
		self.NUM_RATE = data['NUM_RATE']
		
		"""
		PSNR 계산
		"""
		self.list_PSNR = []
		for rate in self.list_rate:
			self.list_PSNR.append(_get_PSNR(rate))


	def reset(self):
		
		# state 및 구성 정보 초기화

		self.state = np.zeros((self.NUM_UE + 1, self.NUM_AP))
		self.info = np.zeros((self.NUM_UE, self.NUM_AP, SIZE_INFO))
		
		# 연결 가능한 UE-AP 및 이용 가능한 자원 설정
	
		# UE-AP 연결
		for i in range(self.NUM_UE):
			sum = 0
			for j in range(self.NUM_AP):
			
				# 일정확률(초기 입력 값)로 UE와 AP 연결
				percent = np.random.randint(100)
				if percent < self.PERCENT_CONNECT:
					self.info[i][j][CONST_CONNECTABLE] = 1
					self.info[i][j][CONST_AVAILABLE] = _get_random_bandwidth()

				sum += self.info[i][j][CONST_CONNECTABLE]
		
			# 만일 AP와 연결 가능한 UE가 없을 경우
			if sum == 0:
				ap = np.random.randint(self.NUM_AP)
				self.info[i][ap][CONST_CONNECTABLE] = 1
				self.info[i][ap][CONST_AVILABLE] = _get_random_bandwidth()
				
		# AP-UE 연결
		for j in range(self.NUM_AP):
			sum = 0
			for i in range(self.NUM_UE):
		
				# 일정확률(초기 입력 값)로 UE와 AP 연결
				percent = np.random.randint(100)
				if percent < self.PERCENT_CONNECT:
					self.info[i][j][CONST_CONNECTABLE] = 1
					self.info[i][j][CONST_AVAILABLE] = _get_random_bandwidth()

				sum += self.info[i][j][CONST_CONNECTABLE]
			
			# 만일 UE와 연결 가능한 AP가 없을 경우
			if sum == 0:
				ue = np.random.randint(self.NUM_UE)
				self.info[ue][j][CONST_CONNECTABLE] = 1
				self.info[ue][j][CONST_AVILABLE] = _get_random_bandwidth()
	
		for i in range(self.NUM_UE):
			request = np.random.randint(len(self.list_rate))
			for j in range(self.NUM_AP):
				if self.info[i][j][CONST_CONNECTABLE] == 1:
					self.info[i][j][CONST_REQUEST] = request

	def make_state(self):
		
		# 요구하는 bitrate를 다 받는다고 가정하고 state에 timeslot 값 삽입
		for ue in range(self.NUM_UE):
			for ap in range(self.NUM_AP):
				if self.info[ue][ap][CONST_CONNECTABLE]:
					request_index = int(self.info[ue][ap][CONST_REQUEST])
					request_bitrate = self.list_rate[request_index]
					self.state[ue + 1][ap] = request_bitrate / self.info[ue][ap][CONST_AVAILABLE]

	def step(self, ue, action):

		# UE와 AP 사이의 연결이 불가능하면
		if self.info[ue][action][CONST_CONNECTABLE] == 0 or self.info[ue][action][CONST_AVAILABLE] == 0:
			return 0, True

		self.state[REMAIN_TIMESLOT][action] += self.state[ue + 1][action]
		
		numerator = 0
		denominator = 0
		for ap in range(self.NUM_AP):
			numerator += self.state[REMAIN_TIMESLOT][ap]
			denominator += self.NUM_AP * (self.state[REMAIN_TIMESLOT][ap] * self.state[REMAIN_TIMESLOT][ap])
		numerator = numerator * numerator
		return numerator / denominator, False
		
def _get_PSNR(rate):
	return 6.4157 * math.log10(rate) + 22.27
	
def _get_random_bandwidth():
	bandwidth = random.gauss(BW_AVG, BW_STD)
	if bandwidth < 0:
		bandwidth = -bandwidth
	return bandwidth
