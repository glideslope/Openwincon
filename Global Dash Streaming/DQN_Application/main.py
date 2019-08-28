"""
Reference: https://github.com/golbin/TensorFlow-Tutorials
"""
import sys

import utility as Util

"""
학습 모드
"""
def train_simulation(data):
	print("Training mode")

"""
재생 모드
"""
def test_simulation(data):
	print("Test mode")

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