import matplotlib.pyplot as plt
import numpy as np
from collections import defaultdict
import matplotlib.colors as mcolors
from parameters_set_105_0609 import data_105
# from parameters_set_245_0612 import data_245

# 将所有集合合并到一个大集合
data = data_105()
# data = data_245()
# 将所有集合合并到一个大集合
all_points = []
for i in range(0, 106):  # 假设有 105 个集合
    set_name = f'set{i}'
    all_points.extend(getattr(data, set_name))

# 计算每个 (x, y) 对的出现次数
counts = defaultdict(int)
for point in all_points:
    counts[point] += 1
print(counts)

# 计算每个 (x, y) 对的概率
total_points = 105
# total_points = 245
print(total_points)
probabilities = {point: count / total_points for point, count in counts.items()}

# 创建 x, y 和 颜色数据
x, y, colors = [], [], []
for (point, prob) in probabilities.items():
    x.append(point[0])
    y.append(point[1])
    colors.append(prob)
    if(prob == 1.0):
        print(point[0])
        print(point[1])

# 将概率映射到颜色范围
norm = mcolors.Normalize(vmin=min(colors), vmax=max(colors))
# print(max(colors))
# norm = mcolors.Normalize(vmin=0, vmax=1)
cmap = plt.get_cmap('nipy_spectral') # nipy_spectral
mappable = plt.cm.ScalarMappable(norm=norm, cmap=cmap)
color_values = mappable.to_rgba(colors)

# 绘制图形
plt.figure(figsize=(10, 8))
plt.scatter(x, y, c=color_values, s=30)
plt.colorbar(mappable, label='Probability', ax=plt.gca()) #, ticks=[]).set_ticklabels([]
plt.xlabel('confThresold')
plt.ylabel('nmsThresold')
plt.title('Probability Distribution of (confThresold, nmsThresold) Pairs\nVersion1, 105sets')
plt.grid(True)
plt.show()
