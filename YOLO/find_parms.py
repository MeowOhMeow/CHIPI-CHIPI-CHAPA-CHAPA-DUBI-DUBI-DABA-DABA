from ultralytics import YOLO
import os
from collections import defaultdict
from tqdm import tqdm
import numpy as np
import matplotlib.pyplot as plt
import json


def cal_acc(model, conf, iou, images, labels, batch_size=128):
    correct = 0

    for i in range(0, len(images), batch_size):
        # Predict in batches
        results = model.predict(
            images[i : i + batch_size], conf=conf, iou=iou, verbose=False
        )

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
            object_classes = result.boxes.cls.to("cpu").tolist()

            for item in object_classes:
                item_counts[item] -= 1

            if all(count == 0 for count in item_counts.values()):
                correct += 1

    return correct / len(images)


if os.path.exists("YOLO/config.json") == False:
    print("Config file not found")
    with open("YOLO/config.json", "w") as f:
        json.dump(
            {
                "model_path": "YOLO/models/train8.pt",
                "images_dir": "YOLO/data/images",
                "labels_dir": "YOLO/data/labels",
                "batch_size": 128,
                "conf_start": 0.1,
                "conf_end": 1,
                "conf_count": 10,
                "iou_start": 0.1,
                "iou_end": 1,
                "iou_count": 10,
            },
            f,
            indent=4,
        )
    print("Default config file created")
with open("YOLO/config.json") as f:
    config = json.load(f)

model = YOLO(config["model_path"])

if not os.path.exists(config["images_dir"]) or not os.path.exists(config["labels_dir"]):
    print("Images or labels directory not found")
    exit()
images = [
    os.path.join(config["images_dir"], p)
    for p in sorted(os.listdir(config["images_dir"]))
]

labels = [
    os.path.join(config["labels_dir"], p)
    for p in sorted(os.listdir(config["labels_dir"]))
]
print(f"Found {len(images)} images and {len(labels)} labels")


batch_size = config["batch_size"]


# testing parmeters
conf_range = np.arange(config["conf_start"], config["conf_end"], config["conf_count"])
iou_range = np.arange(config["iou_start"], config["iou_end"], config["iou_count"])

best_acc = 0
best_conf = 0
best_iou = 0
results = np.array([], dtype=float)

for conf in tqdm(conf_range, position=0, leave=False):
    for iou in tqdm(iou_range, position=1, leave=False):
        acc = cal_acc(model, conf, iou, images, labels, batch_size)
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

# plot heatmap
fig, ax = plt.subplots()
cax = ax.matshow(results, cmap="hot")
fig.colorbar(cax)
plt.xticks(range(len(iou_range)), iou_range)
plt.yticks(range(len(conf_range)), conf_range)
plt.xlabel("IOU")
plt.ylabel("Confidence")
plt.title("Accuracy heatmap")
# plt.show()
plt.savefig("YOLO/heatmap.png")
