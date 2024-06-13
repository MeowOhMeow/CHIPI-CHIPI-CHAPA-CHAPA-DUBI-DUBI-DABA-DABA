import matplotlib.pyplot as plt
import numpy as np
# from parameters_set import data_original
# from parameters_set_sharpen import data_sharpen
# from parameters_set_105_0609 import data_105
from parameters_set_105_0609 import data_105

# test 1: 6/8前的照片
# data = data_original()
# test 2: 6/12，105張照片
data = data_105()

all_sets = [getattr(data, f"set{i}") for i in range(0, 106)]
# 要跳过的集合索引
# skip_indices = {6, 12, 52, 64, 66, 69, 72, 73, 80, 83, 85}
skip_indices = {}

count = 0
point = (0.41, 0.71)
intersection = all_sets[0]
for i, s in enumerate(all_sets[1:], start=1):
    if i in skip_indices:
        print(i)
        continue
    is_in_intersection = point in all_sets[i]
    if(is_in_intersection == True):
        count = count + 1
    intersection &= s
print(count)

# intersection = (data.set1 & data.set2 & data.set3 & data.set4 & data.set5 & data.set6 & data.set7 
#                 & data.set8 & data.set9 & data.set10 & data.set11 & data.set12 & data.set13 & data.set14  
#                 & data.set15 & data.set16 & data.set17 & data.set18 & data.set19 & data.set20 & data.set21
#                 & data.set22 & data.set23 & data.set24 & data.set25 & data.set26 & data.set27 & data.set28
#                 & data.set29 & data.set30 & data.set31 & data.set32 & data.set33 & data.set34 & data.set35 
#                 & data.set36 & data.set37 & data.set38 & data.set39 & data.set40 & data.set41 & data.set42
#                 & data.set43 & data.set44 & data.set45 & data.set46 & data.set47 & data.set48 & data.set49
#                 & data.set50 & data.set51 & data.set52 & data.set53 & data.set54 & data.set55 & data.set56
#                 & data.set57 & data.set58 & data.set59 & data.set60 & data.set61 & data.set62 & data.set63
#                 & data.set64 
#                 )
# intersection_1 = (data.set6)
# intersection_1 = (data.set52)
# intersection_1 = (data.set64)
# intersection_1 = (data.set66)
# intersection_1 = (data.set69)
# intersection_1 = (data.set72)
# intersection_1 = (data.set73)
# intersection_1 = (data.set80)
# intersection_1 = (data.set83)
# intersection_1 = (data.set85)

# 确定 (0.2, 0.6) 是否在交集中
point = (0.41, 0.71)
is_in_intersection = point in intersection
print(f"Point {point} is in intersection: {is_in_intersection}")

# 设置 x 和 y 轴的范围和间隔
plt.figure(figsize=(8, 6))
x = np.arange(0, 1.1, 0.1)
y = np.arange(0, 1.1, 0.1)
plt.xticks(x)
plt.yticks(y)
plt.xlim(0, 1.0)
plt.ylim(0, 1.0)

# 绘制网格
plt.grid(True)

# 绘制交集点
for point in intersection:
    plt.scatter(point[0], point[1], color='red', s=20)  # 将坐标缩放到 0-1 范围
# for point in intersection_1:
#     plt.scatter(point[0], point[1], color='blue', s=15)  # 将坐标缩放到 0-1 范围
# for point in intersection_2:
#     plt.scatter(point[0], point[1], color='black', s=5)  # 将坐标缩放到 0-1 范围
# for point in intersection_3:
#     plt.scatter(point[0], point[1], color='green', s=10)  # 将坐标缩放到 0-1 范围

# 添加标题和标签
# plt.title('Valid Pairs of 64 sets')
# plt.title('Valid Pairs of 64 sets + set66 + set67 + set68')
# plt.title('Valid Pairs of fine pictures in set0~19')
plt.title('Valid Pairs of set85')
plt.xlabel('confThreshold')
plt.ylabel('nmsThreshold')

# 显示图形
plt.show()