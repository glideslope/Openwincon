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
	pass

"""
재생
"""
def play_simulation(data):
	print("Test mode")
	pass

if __name__ == "__main__":

	if len(sys.argv) != 2:
		print("Help > python main.py <mode>")
		print("Help > <mode> can be 'train' or 'test'") 
	else:
		input = sys.argv[1]
		if input == "train":
			pass
			
		elif input == "test":
			pass
		else:
			print("Check your input")