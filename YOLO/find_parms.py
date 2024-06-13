from ultralytics import YOLO
import os
import json
from collections import defaultdict


def cal_acc(model, images, labels, batch_size=128):
    correct = 0

    for i in range(0, len(images), batch_size):
        # Predict in batches
        results = model(images[i : i + batch_size], verbose=False)

        # Prepare labels and counts
        batch_labels = labels[i : i + batch_size]
        batch_item_counts = []

        for label in batch_labels:
            item_counts = defaultdict(int)
            with open(label) as f:
                for line in f:
                    class_id = int(line.split()[0])
                    item_counts[class_id] += 1
            batch_item_counts.append(item_counts)

        # Compare results with the actual counts
        for result, item_counts in zip(results, batch_item_counts):
            object_classes = result.boxes.cls.to('cpu').tolist()

            for item in object_classes:
                item_counts[item] -= 1

            if all(count == 0 for count in item_counts.values()):
                correct += 1

    return correct / len(images)


model = YOLO("YOLO/models/train8.pt")

images = [
    os.path.join("YOLO/data/images", p) for p in sorted(os.listdir("YOLO/data/images"))
]

labels = [
    os.path.join("YOLO/data/labels", p) for p in sorted(os.listdir("YOLO/data/labels"))
]
batch_size = 128

# acc = cal_acc(model, images, labels, batch_size)
# print(f"Accuracy: {acc}")


from tqdm import tqdm
import numpy as np

# testing parmeters
conf_range = [conf / 100 for conf in range(10, 91, 10)]
iou_range = [iou / 100 for iou in range(10, 91, 10)]

best_acc = 0
best_conf = 0
best_iou = 0
results = np.array([], dtype=float)

for conf in tqdm(conf_range, position=0, leave=False):
    for iou in tqdm(iou_range, position=1, leave=False):
        model.conf = conf
        model.iou = iou
        acc = cal_acc(model, images, labels, batch_size)
        results = np.append(results, acc)
        if acc > best_acc:
            best_acc = acc
            best_conf = conf
            best_iou = iou

print(f"Best accuracy: {best_acc}")
print(f"Best conf: {best_conf}")
print(f"Best iou: {best_iou}")

results = results.reshape(len(conf_range), len(iou_range))
print(results)

import matplotlib.pyplot as plt

# plot heatmap
fig, ax = plt.subplots()
cax = ax.matshow(results, cmap="hot")
fig.colorbar(cax)
plt.xticks(range(len(iou_range)), iou_range)
plt.yticks(range(len(conf_range)), conf_range)
plt.xlabel("IOU")
plt.ylabel("Confidence")
plt.title("Accuracy heatmap")
plt.show()
