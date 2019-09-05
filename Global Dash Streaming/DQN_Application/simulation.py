import math
import numpy as np
import random

# UE와 AP 사이 정보
SIZE_INFO = 4
CONST_CONNECTABLE = 0
CONST_AVAILABLE = 1
CONST_REQUEST = 2
CONST_SUPPORT = 3

# Time slot 인덱스
SUM_TIMESLOT = 0

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
			self.list_PSNR.append(self.get_PSNR(rate))


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
					# 처음에는 요구하는 대로 다 받는다고 가정
					self.info[i][j][CONST_SUPPORT] = request

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

		self.state[SUM_TIMESLOT][action] += self.state[ue + 1][action]
		
		numerator = 0
		denominator = 0
		for ap in range(self.NUM_AP):
			numerator += self.state[SUM_TIMESLOT][ap]
			denominator += self.NUM_AP * (self.state[SUM_TIMESLOT][ap] * self.state[SUM_TIMESLOT][ap])
		numerator = numerator * numerator
		return numerator / denominator, False

	def adjust_bitrate(self, ap, connection):
		dic_ue = {}
		for ue in connection:
			dic_ue[ue] = self.info[ue][ap][CONST_SUPPORT]
		while True:

			# 가장 큰 bitrate 재조정		
			greedy_ue = sorted(dic_ue, key = lambda ue: dic_ue[ue], reverse = True)[0]
			dic_ue[greedy_ue] -= 1
			self.info[greedy_ue][ap][CONST_SUPPORT] -= 1
		
			# Timeslot 재계산
			self.state[SUM_TIMESLOT][ap] = 0
			for ue in connection:
				support_index = int(self.info[ue][ap][CONST_SUPPORT])
				support_bitrate = self.list_rate[support_index]
				self.state[SUM_TIMESLOT][ap] += support_bitrate / self.info[ue][ap][CONST_AVAILABLE]

			# 허용된 Timeslot 보다 작으면 루프 종료
			if self.state[SUM_TIMESLOT][ap] <= self.VAL_TIMESLOT:
				break

	def get_PSNR(self, rate):
		return 6.4157 * math.log10(rate) + 22.27
	
def _get_random_bandwidth():
	bandwidth = random.gauss(BW_AVG, BW_STD)
	if bandwidth < 0:
		bandwidth = -bandwidth
	return bandwidth
