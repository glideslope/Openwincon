"""
Reference: https://github.com/golbin/TensorFlow-Tutorials
"""
import tensorflow as tf
import numpy as np
import sys

import utility as Util
from simulation import Simulation
from model import DQN

# 최대 에피소드 갯수
MAX_EPISODE = 10000

# 트레이닝 주기
INTERVAL_TRAINING = 4

# 네트워크 업데이트 주기
INTERVAL_UPDATE = 1000

# 데이터 어느정도 쌓이면 학습 시작
THRESH_OBSERVE = 100

# 랜덤 액션 조정 수치
DELTA_EPSILON = 1000

# 최대 테스트 횟수
MAX_TEST = 1

# UE와 AP 사이 정보
SIZE_INFO = 4
CONST_CONNECTABLE = 0
CONST_AVAILABLE = 1
CONST_REQUEST = 2
CONST_SUPPORT = 3

# Time slot 인덱스
SUM_TIMESLOT = 0

"""
학습 모드
"""
def train_simulation(data):
	print("Training mode")
	session = tf.Session()

	simulation = Simulation(data)
	network = DQN(session, data)

	rewards = tf.placeholder(tf.float32, [None])
	tf.summary.scalar('reward average / episode', tf.reduce_mean(rewards))

	saver = tf.train.Saver()
	session.run(tf.global_variables_initializer())

	writer = tf.summary.FileWriter('logs', session.graph)
	summary = tf.summary.merge_all()

	# 네트워크 초기화
	network.update_target_network()

	epsilon = 1.0
	time = 0
	
	# 학습 시작
	for episode in range(MAX_EPISODE):
		total_reward = 0
		list_reward = []

		before_reward = 0

		simulation.reset()
		simulation.make_state()

		network.init_state(simulation.state)

		# UE 차례로 AP에 할당
		for ue in range(data['NUM_UE']):
			
			if np.random.rand() < epsilon:
				action = np.random.randint(data['NUM_AP'])
			else:
				action = network.get_action()
			
			epsilon -= 1 / DELTA_EPSILON

			fairness, error = simulation.step(ue, action)
			reward = fairness - before_reward
			before_reward = fairness

			total_reward += reward

			if error:
				network.remember(simulation.state, action, reward, True)
			else:
				network.remember(simulation.state, action, reward, (ue == (data['NUM_UE'] - 1)))

			if time > THRESH_OBSERVE and (time % INTERVAL_TRAINING == 0):
				network.train()

			if time % INTERVAL_UPDATE == 0:
				network.update_target_network()
	
			time += 1

			if error:
				break	

		list_reward.append(total_reward)
		print(episode, total_reward)

		if episode % 10 == 0:
			result = session.run(summary, feed_dict={rewards: list_reward})
			writer.add_summary(result, time)
			list_reward = []

		if episode % 100 == 0:
			saver.save(session, 'model/dqn.ckpt', global_step = time)
			
			
	# 학습 종료
	
	
"""
재생 모드
"""
def test_simulation(data):
	print("Test mode")
	session = tf.Session()

	simulation = Simulation(data)
	network = DQN(session, data)

	saver = tf.train.Saver()
	ckeckpoint = tf.train.get_checkpoint_state('model')
	saver.restore(session, ckeckpoint.model_checkpoint_path)

	# 테스트 시작
	for episode in range(MAX_TEST):

		list_connection = [[] for i in range(data['NUM_AP'])]

		total_reward = 0
	
		before_reward = 0

		simulation.reset()
		simulation.make_state()

		network.init_state(simulation.state)

		# UE 차례로 AP에 할당
		for ue in range(data['NUM_UE']):
			
			action = network.get_action()
			list_connection[action].append(ue)			

			fairness, error = simulation.step(ue, action)
			reward = fairness - before_reward
			before_reward = fairness

			total_reward += reward

			if error:
				network.remember(simulation.state, action, reward, True)
			else:
				network.remember(simulation.state, action, reward, (ue == (data['NUM_UE'] - 1)))

			if error:
				break
	
		print()
		print("Fairness:", total_reward)
		print()
		print("== Before adjustment ==")
		for ap in range(data['NUM_AP']):
			print("AP %d Timeslot: %.2f" % (ap, simulation.state[SUM_TIMESLOT][ap]))
			print("Connection:", end = " ")
			for ue in list_connection[ap]:
				print("UE %d(%dkbps)" % (ue, data['LIST_RATE'][int(simulation.info[ue][ap][CONST_REQUEST])]), end = " ")
			print()
		
			# AP에 할당된 Timeslot이 허용 Timeslot보다 넘치는 경우
			if simulation.state[SUM_TIMESLOT][ap] > data['VAL_TIMESLOT']:
				simulation.adjust_bitrate(ap, list_connection[ap])
		
		print()

		total_psnr = 0
		print("== After adjustment ==")
		for ap in range(data['NUM_AP']):
			print("AP %d Timeslot: %.2f" % (ap, simulation.state[SUM_TIMESLOT][ap]))
			print("Connection:", end = " ")
			for ue in list_connection[ap]:
				support_index = int(simulation.info[ue][ap][CONST_SUPPORT])
				support_rate = data['LIST_RATE'][support_index]
				total_psnr += simulation.get_PSNR(support_rate)
				print("UE %d(%dkbps)" % (ue, support_rate), end = " ")
			print()
		print()
		print("average PSNR: %.2f" % (total_psnr / data['NUM_UE']))

	# 테스트 종료


if __name__ == "__main__":

	if len(sys.argv) != 2:
		print("Help > python main.py <mode>")
		print("Help > <mode> can be 'train' or 'test'") 
	else:
		input = sys.argv[1]
		if input == "train":
			data = Util.initialize_data()
			train_simulation(data)
			
		elif input == "test":
			data = Util.initialize_data()
			test_simulation(data)
			
		else:
			print("Check your input")