import math
import numpy as np
import random
import copy
import timeit
from mkp.algorithms import mtm
from mthm import *

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
	
		for ue in range(self.NUM_UE):
			# 요구 bitrate
			request_index = np.random.randint(len(self.list_rate))
			for ap in range(self.NUM_AP):
				if self.info[ue][ap][CONST_CONNECTABLE] == 1:
					self.info[ue][ap][CONST_REQUEST] = request_index
					available_bitrate = self.list_rate[request_index]
					
					# 처음에는 요구하는 대로 다 받는다고 가정, 하지만 available bandwidth 보다 요구하는 것이 클 경우는 조정
					while available_bitrate > self.info[ue][ap][CONST_AVAILABLE]:
						# 최소 bitrate는 받도록
						if request_index == 0:
							break

						request_index -= 1
						available_bitrate = self.list_rate[request_index]

					self.info[ue][ap][CONST_SUPPORT] = request_index

	def make_state(self):
		
		# 요구하는 bitrate를 다 받는다고 가정하고 state에 timeslot 값 삽입
		for ue in range(self.NUM_UE):
			for ap in range(self.NUM_AP):
				if self.info[ue][ap][CONST_CONNECTABLE]:
					# 이미 초기화 할때 avilable bandwidth 고려한 bitrate 조정을 함
					available_index = int(self.info[ue][ap][CONST_SUPPORT])
					available_bitrate = self.list_rate[available_index]
					self.state[ue + 1][ap] = available_bitrate / self.info[ue][ap][CONST_AVAILABLE]

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

	def solve_random(self):
		start = timeit.default_timer()

		timeslot = []
		for j in range(self.NUM_AP):
			timeslot.append(self.VAL_TIMESLOT)

		info = copy.deepcopy(self.info)
		self.solution_random = 0

		for i in range(self.NUM_UE):
			list_connectable = []
			for j in range(self.NUM_AP):
				list_connectable.append(j)
			
			random.shuffle(list_connectable)
			ap = list_connectable[0]

			max_index = int(info[i][ap][CONST_SUPPORT])
			# 최대 이용가능한 bitrate 찾아보기
			for k in range(max_index, -1 , -1):
				rate = self.list_rate[k]
				if timeslot[ap] >= rate / info[i][ap][CONST_AVAILABLE]:
					max_index = k
					break

			if max_index == 0:
				index = 0
			else:
				index = np.random.randint(max_index)
			rate = self.list_rate[index]
			timeslot[ap] -= rate / info[i][ap][CONST_AVAILABLE]
			self.solution_random += self.list_PSNR[index]
			
		return self.solution_random, (timeit.default_timer() - start)

	def solve_greedy(self):
		start = timeit.default_timer()

		list_timeslot = []
		list_priority = []

		# Timeslot에 따른 우선순위
		for ap in range(self.NUM_AP):
			list_timeslot.append([self.VAL_TIMESLOT, ap])

		info = copy.deepcopy(self.info)

		# 비트레이트에 따른 우선 순위
		for ue in range(self.NUM_UE):
			list_priority.append([int(info[ue][0][CONST_REQUEST]), ue])

		self.solution_greedy = 0
		list_priority.sort(reverse = True)

		# 비트레이트 우선순위에 따라 넣음
		for ue in list_priority:
			list_timeslot.sort(reverse = True)

			ap = None
			for temp in list_timeslot:

				# 연결 불가능한 경우
				if info[ue[1]][temp[1]][CONST_CONNECTABLE] == 0:
					continue

				ap = temp
				break

			# 최대 이용가능한 bitrate 찾아보기
			for k in range(ue[0], -1, -1):
				rate = self.list_rate[k]

				# 이용가능한 bandwidth 보다 클 경우 다른 bitrate 사용, 적어도 최소 비트레이트는 보장해주기				
				if (rate > info[ue[1]][ap[1]][CONST_AVAILABLE]) and (k != 0):
					continue

				timeslot = rate / info[ue[1]][ap[1]][CONST_AVAILABLE]
				if ap[0] - timeslot >= 0:
					ap[0] -= timeslot
					self.solution_greedy += self.list_PSNR[k]
					break


		return self.solution_greedy, (timeit.default_timer() - start)

	def solve_mtm(self):
		start = timeit.default_timer()

		info = copy.deepcopy(self.info)
	
		# AP가 client에게 줄 수 있는 평균 bandwidth를 통해 capacity 구하기
		list_capacity = []
		for ap in range(self.NUM_AP):
			sum = 0
			for ue in range(self.NUM_UE):
				sum += info[ue][ap][CONST_AVAILABLE]

			list_capacity.append(int((self.VAL_TIMESLOT * sum) / self.NUM_UE))
		
		list_connection = None
			
		list_value = []
		list_weight = []	
		for ue in range(self.NUM_UE):
			rate_index = int(info[ue][0][CONST_REQUEST])
			rate = self.list_rate[rate_index]
			list_value.append(self.list_PSNR[rate_index])
			list_weight.append(rate)

		# 가방 안에 물건 다 못 넣으면 finish 변수 False 값으로
		finish = True
		# 가방 안에 물건 다 못 들어갈 경우 따로 구분
		list_priority = []
		# 타임 슬롯 계산 위한 변수
		list_timeslot = []
		for ap in range(self.NUM_AP):
			list_timeslot.append([self.VAL_TIMESLOT, ap])
		try:
			sum_psnr, list_connection, back, _ = mtm(list_value, list_weight, list_capacity)
		except Exception as e:
			print(e)

		if list_connection == None:
			list_connection = [-1] * self.NUM_UE

		for ue, ap in enumerate(list_connection):
			# 가방에 물건 다 못 담는 경우
			if ap == -1:
				list_priority.append([int(info[ue][0][CONST_REQUEST]), ue])
				finish = False
			else:
				rate = self.list_rate[int(info[ue][ap][CONST_REQUEST])]
				timeslot = rate / info[ue][ap][CONST_AVAILABLE]
				list_timeslot[ap][0] -= timeslot

		# 가방에 안 담긴 물건들 넣기
		if finish == False:
				list_priority.sort(reverse = True)
				list_timeslot.sort(reverse = True)
				for priority in list_priority:
					ue = priority[1]
					ap = list_timeslot[0][1]
					rate = self.list_rate[int(info[ue][0][CONST_REQUEST])]
					timeslot = rate / info[ue][ap][CONST_AVAILABLE]
					list_timeslot[ap][0] -= timeslot 
					list_connection[ue] = ap
					list_timeslot.sort(reverse = True)

		# Timslot 체크
		set_problem = set()
		list_timeslot = [self.VAL_TIMESLOT] * self.NUM_AP
		for ue, ap in enumerate(list_connection):
			rate = self.list_rate[int(info[ue][ap][CONST_SUPPORT])]
			timeslot = rate / info[ue][ap][CONST_AVAILABLE]
			list_timeslot[ap] -= timeslot
			if list_timeslot[ap] < 0:
				set_problem.add(ap)

		for j in set_problem:
			# 높은 비트레이트를 가진 UE부터 rate 줄이도록 담는 리스트
			list_priority = []
			for ue, ap in enumerate(list_connection):

				#문제가 없는 AP의 경우는 지나침
				if j != ap:
					continue
				list_priority.append([int(info[ue][ap][CONST_SUPPORT]), ue])

			# 큰 비트레이트를 가진 UE부터 비트레이트 줄여가기
			while True:
				list_priority.sort(reverse = True)
				total_timeslot = self.VAL_TIMESLOT
				# 비트레이트 한단계 낮춤
				first = list_priority[0]
				first[0] -= 1
				info[first[1]][j][CONST_SUPPORT] = first[0]
				for ue in list_priority:
					rate = self.list_rate[ue[0]]
					timeslot = rate / info[ue[1]][j][CONST_AVAILABLE]
					total_timeslot -= timeslot
				if total_timeslot >= 0:
					break
			
		# PSNR 합 구함
		self.solution_mtm = 0		
		for ue, ap in enumerate(list_connection):
			rate_index = int(info[ue][ap][CONST_SUPPORT])
			self.solution_mtm += self.list_PSNR[rate_index]

		return self.solution_mtm, (timeit.default_timer() - start)
	

	def solve_mthm(self):
		start = timeit.default_timer()

		info = copy.deepcopy(self.info)	

		# AP가 client에게 줄 수 있는 평균 bandwidth를 통해 capacity 구하기
		list_capacity = []
		for ap in range(self.NUM_AP):
			sum = 0
			for ue in range(self.NUM_UE):
				sum += info[ue][ap][CONST_AVAILABLE]

			list_capacity.append(int((self.VAL_TIMESLOT * sum) / self.NUM_UE))
		
		list_connection = None
			
		list_value = []
		list_weight = []	
		for ue in range(self.NUM_UE):
			rate_index = int(info[ue][0][CONST_REQUEST])
			rate = self.list_rate[rate_index]
			list_value.append(self.list_PSNR[rate_index])
			list_weight.append(rate)

		# 가방 안에 물건 다 못 넣으면 finish 변수 False 값으로
		finish = True
		# 가방 안에 물건 다 못 들어갈 경우 따로 구분
		list_priority = []
		# 타임 슬롯 계산 위한 변수
		list_timeslot = []
		for ap in range(self.NUM_AP):
			list_timeslot.append([self.VAL_TIMESLOT, ap])
		try:
			sum_psnr, list_connection = mthm(self.NUM_UE, self.NUM_AP, list_value, list_weight, list_capacity)
		except Exception as e:
			print(e)

		if list_connection == None:
			list_connection = [-1] * self.NUM_UE

		for ue, ap in enumerate(list_connection):
			# 가방에 물건 다 못 담는 경우
			if ap == -1:
				list_priority.append([int(info[ue][0][CONST_REQUEST]), ue])
				finish = False
			else:
				rate = self.list_rate[int(info[ue][ap][CONST_REQUEST])]
				timeslot = rate / info[ue][ap][CONST_AVAILABLE]
				list_timeslot[ap][0] -= timeslot 

		# 가방에 안 담긴 물건들 넣기
		if finish == False:
				list_priority.sort(reverse = True)
				list_timeslot.sort(reverse = True)
				for priority in list_priority:
					ue = priority[1]
					ap = list_timeslot[0][1]
					rate = self.list_rate[int(info[ue][0][CONST_REQUEST])]
					timeslot = rate / info[ue][ap][CONST_AVAILABLE]
					list_timeslot[ap][0] -= timeslot 
					list_connection[ue] = ap
					list_timeslot.sort(reverse = True)
			

		# Timslot 체크
		set_problem = set()
		list_timeslot = [self.VAL_TIMESLOT] * self.NUM_AP
		for ue, ap in enumerate(list_connection):
			rate = self.list_rate[int(info[ue][ap][CONST_SUPPORT])]
			timeslot = rate / info[ue][ap][CONST_AVAILABLE]
			list_timeslot[ap] -= timeslot
			if list_timeslot[ap] < 0:
				set_problem.add(ap)

		for j in set_problem:
			# 높은 비트레이트를 가진 UE부터 rate 줄이도록 담는 리스트
			list_priority = []
			for ue, ap in enumerate(list_connection):

				#문제가 없는 AP의 경우는 지나침
				if j != ap:
					continue
				list_priority.append([int(info[ue][ap][CONST_SUPPORT]), ue])

			# 큰 비트레이트를 가진 UE부터 비트레이트 줄여가기
			while True:
				list_priority.sort(reverse = True)
				total_timeslot = self.VAL_TIMESLOT
				# 비트레이트 한단계 낮춤
				first = list_priority[0]
				first[0] -= 1
				info[first[1]][j][CONST_SUPPORT] = first[0]
				for ue in list_priority:
					rate = self.list_rate[ue[0]]
					timeslot = rate / info[ue[1]][j][CONST_AVAILABLE]
					total_timeslot -= timeslot
				if total_timeslot >= 0:
					break

		# PSNR 합 구함
		self.solution_mthm = 0		
		for ue, ap in enumerate(list_connection):
			rate_index = int(info[ue][ap][CONST_SUPPORT])
			self.solution_mthm += self.list_PSNR[rate_index]

		return self.solution_mthm, (timeit.default_timer() - start)
	

	def solve_optimal(self):
		start = timeit.default_timer()

		timeslot = []
		for j in range(self.NUM_AP):
			timeslot.append(self.VAL_TIMESLOT)
		
		info = copy.deepcopy(self.info)
		self.solution_optimal = 0

		self._dfs(0, info, timeslot, 0)

		return self.solution_optimal, (timeit.default_timer() - start)
		
	def _dfs(self, ue, info, timeslot, PSNR):
		if ue == self.NUM_UE:
			if PSNR > self.solution_optimal:
				self.solution_optimal = PSNR
			return

		else:
			for j in range(self.NUM_AP):
				# 연결 불가능한 경우는 제외
				if info[ue][j][CONST_CONNECTABLE] == 0:
					continue
				
				origin_timeslot = timeslot[j]
				origin_support = info[ue][j][CONST_SUPPORT]

				for k in range(self.NUM_RATE):
					
					# 요구하는 비트레이트보다 클때는 건너 뛰기
					if k > info[ue][j][CONST_REQUEST]:
						break

					info[ue][j][CONST_SUPPORT] = k
					support_rate = self.list_rate[k]
					timeslot[j] -= support_rate / self.info[ue][j][CONST_AVAILABLE]
					if timeslot[j] < 0:
						info[ue][j][CONST_SUPPORT] = origin_support
						timeslot[j] = origin_timeslot
						break
					
					else:
						self._dfs(ue + 1, info, timeslot, PSNR + self.list_PSNR[k])				
						info[ue][j][CONST_SUPPORT] = origin_support
						timeslot[j] = origin_timeslot

def _get_random_bandwidth():
	bandwidth = random.gauss(BW_AVG, BW_STD)
	if bandwidth < 0:
		bandwidth = -bandwidth
	return bandwidth
