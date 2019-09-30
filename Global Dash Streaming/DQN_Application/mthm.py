import copy
EMPTY = -1

def greedy(n, m, p, w, c, y):
	z = 0
	c_ = [0] * m

	for ap in range(m):
		c_[ap] = c[ap]
		for ue in range(n):
			if y[ue] == EMPTY and w[ue] <= c_[ap]:
				y[ue] = ap
				c_[ap] -= w[ue]
				z += p[ue]
	return y

def rearrange(n, m, p, w, c, y):
	z = 0
	c_ = copy.deepcopy(c)
	ap = 0

	for ue in range(n - 1, -1 , -1):
		if y[ue] == EMPTY:
			continue
		temp = []
		for k in range(0, ap - 1):
			temp.append(k)
		for k in range(ap, m):
			temp.append(k)
		temp.sort()
		l = EMPTY
		for k in temp:
			if w[ue] <= c_[k]:
				l = k
				break
		if l == EMPTY:
			y[ue] = EMPTY
		else:
			y[ue] = l
			c_[l] -= w[ue]
			z += p[ue]
			if l < m:
				ap = l +1
			else:
				ap = 0
	return c_, z

def improve_1(n, m, p, w, c, y, c_, z):
	for ue in range(n):
		if y[ue] == EMPTY:
			continue
		for k in range(ue + 1, n):
			if y[k] != y[ue] and y[k] > EMPTY:
				if w[ue] > w[k]:
					h = ue
					l = k
				else:
					h = k
					l = ue
				d = w[h] - w[l]

				temp = []
				for t in range(n):
					if y[t] == EMPTY:
						temp.append(w[t])
				temp.sort()

				if d <= c_[y[l]] and c_[y[h]] + d > temp[0]:
					temp = []
					for t in range(n):
						if y[t] == EMPTY and w[t] <= c_[y[h]] + d:
							temp.append(t)
					temp.sort(reverse = True)
					t = temp[0]
					c_[y[h]] += (d - w[t])
					c_[y[l]] -= d
					y[t] = y[h]
					y[h] = y[l]
					y[l] = y[t]
					z += p[t]
	return z

def improve_2(n, m, p, w, c, y, c_, z):
	for ue in range(n - 1, -1 , -1):
		if y[ue] == EMPTY:
			continue
		c__ = c_[y[ue]] + w[ue]
		temp = set()
		for k in range(n):
			if y[k] == EMPTY and w[k] <= c__:
				temp.add(k)
				c__ -= w[k]

		sum = 0
		for t in temp:
			sum += p[t]
		if sum > p[ue]:
			for t in temp:
				y[t] = y[ue]
			c_[y[ue]] = c__
			y[ue] = EMPTY
			z += (sum - p[ue])
	return z				

def mthm(n, m, p, w, c):
	y = [EMPTY] * n

	greedy(n, m, p, w, c, y)
	c_, z = rearrange(n, m, p, w, c, y)
	z = improve_1(n, m, p, w, c, y, c_, z)
	z = improve_2(n, m, p, w, c, y, c_, z)

	return z, y

"""
n = 9
m = 2
p = [80, 20, 60, 40, 60, 60, 65, 25, 30]
w = [40, 10, 40, 30, 50, 50, 55, 25, 40]
c= [100, 150]
"""