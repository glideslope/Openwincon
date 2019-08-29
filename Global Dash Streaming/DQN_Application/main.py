"""
Reference: https://github.com/golbin/TensorFlow-Tutorials
"""
import tensorflow as tf
import sys

import utility as Util
from simulation import Simulation

"""
학습 모드
"""
def train_simulation(data):
	print("Training mode")
	session = tf.Session()

	simulation = Simulation(data)

	saver = tf.train.Saver()

	fileWriter = tf.summary.FileWriter('logs', session.graph)

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