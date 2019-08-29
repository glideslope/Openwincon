import tkinter
from tkinter import ttk
from tkinter import messagebox

# 비트레이트
LIST_RATE = [50, 400, 800, 1200, 2000]

"""
입력 콘솔
"""
def initialize_data():

	box = tkinter.Tk()
	box.title("Data Input")

	tkinter.Label(box, text="UE 수를 입력하시오").pack()
	NUM_UE = tkinter.StringVar(box, value='3')
	ttk.Entry(box, textvariable = NUM_UE).pack()

	tkinter.Label(box, text="AP 수를 입력하시오").pack()
	NUM_AP = tkinter.StringVar(box, value='2')
	ttk.Entry(box, textvariable = NUM_AP).pack()

	tkinter.Label(box, text="UE - AP의 연결률을 입력하시오(%)").pack()
	PERCENT_CONNECT = tkinter.StringVar(box, value='100')
	ttk.Entry(box, textvariable = PERCENT_CONNECT).pack()

	tkinter.Label(box, text="Timeslot 값을 입력하시오").pack()
	VAL_TIMESLOT = tkinter.StringVar(box, value='1.0')
	ttk.Entry(box, textvariable = VAL_TIMESLOT).pack()

	ret = {}
	
	def parse_data():
		try:
			# 입력으로 받는 것들
			ret['NUM_UE'] = int(NUM_UE.get())
			ret['NUM_AP'] = int(NUM_AP.get())
			ret['PERCENT_CONNECT'] = int(PERCENT_CONNECT.get())
			ret['VAL_TIMESLOT'] = float(VAL_TIMESLOT.get())

			if ret['NUM_UE'] <= 0:
				messagebox.showerror("Error", "Please check your input")
			elif ret['NUM_AP'] <= 0:
				messagebox.showerror("Error", "Please check your input")
			elif ret['PERCENT_CONNECT'] <= 0 or ret['PERCENT_CONNECT'] > 100:
				messagebox.showerror("Error", "Please check your input")
			elif ret['VAL_TIMESLOT'] <= 0:
				messagebox.showerror("Error", "Please check your input")
			else:
				box.destroy()
		except ValueError:
			messagebox.showerror("Error", "Please check your input")

	action = ttk.Button(box, text="Enter", command = parse_data)
	action.pack()
	box.mainloop()

	# 전역 변수로 지정한 것들
	ret['LIST_RATE'] = LIST_RATE
	ret['NUM_RATE'] = len(LIST_RATE)
	
	return ret