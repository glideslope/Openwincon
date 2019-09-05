"""
Reference: https://github.com/golbin/TensorFlow-Tutorials
"""
import tensorflow as tf
import sys

import utility as Util
from simulation import Simulation
from model import DQN

# 최대 에피소드 갯수
MAX_EPISODE = 10000

# 네트워크 업데이트 주기
INTERVAL_UPDATE = 1000

# 랜덤 액션 조정 수치
DELTA_EPSILON = 1000

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
		simulation.reset()
		simulation.make_state()
	# 학습 종료
	
	
"""
재생 모드
"""
def test_simulation(data):
	print("Test mode")
	session = tf.Session()

	simulation = Simulation(data)

	saver = tf.train.Saver()
	ckeckpoint = tf.train.get_checkpoint_state('model')
	saver.restore(session, ckeckpoint.model_checkpoint_path)

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