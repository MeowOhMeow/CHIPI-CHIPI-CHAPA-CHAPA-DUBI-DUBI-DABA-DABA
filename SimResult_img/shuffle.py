import os
import shutil
import random

current_dir = os.path.dirname(__file__)
base_dir = os.path.join(current_dir, "SimResult_img")

# Define the paths
images_dir = os.path.join(base_dir, "images")
labels_dir = os.path.join(base_dir, "labels")
train_images_dir = os.path.join(base_dir, "train", "images")
train_labels_dir = os.path.join(base_dir, "train", "labels")
valid_images_dir = os.path.join(base_dir, "val", "images")
valid_labels_dir = os.path.join(base_dir, "val", "labels")

# Create train and valid directories
os.makedirs(train_images_dir, exist_ok=True)
os.makedirs(train_labels_dir, exist_ok=True)
os.makedirs(valid_images_dir, exist_ok=True)
os.makedirs(valid_labels_dir, exist_ok=True)

# Get a list of all image files
image_files = [f for f in os.listdir(images_dir) if f.endswith(".jpg")]

# Set the split ratio
split_ratio = 0.8

# Shuffle the list of files
random.shuffle(image_files)

# Calculate the split index
split_index = int(len(image_files) * split_ratio)

# Split the files into train and valid sets
train_files = image_files[:split_index]
valid_files = image_files[split_index:]


# Function to move files
def move_files(files, src_img_dir, src_lbl_dir, dest_img_dir, dest_lbl_dir):
    for file in files:
        img_src = os.path.join(src_img_dir, file)
        lbl_src = os.path.join(src_lbl_dir, file.replace(".jpg", ".txt"))
        img_dest = os.path.join(dest_img_dir, file)
        lbl_dest = os.path.join(dest_lbl_dir, file.replace(".jpg", ".txt"))
        if not os.path.exists(lbl_src):
            print(f"Label file does not exist for {file}. Skipping...")
            continue

        shutil.copy(img_src, img_dest)
        if os.path.exists(lbl_src):
            shutil.copy(lbl_src, lbl_dest)


# Move the train files
move_files(train_files, images_dir, labels_dir, train_images_dir, train_labels_dir)

# Move the valid files
move_files(valid_files, images_dir, labels_dir, valid_images_dir, valid_labels_dir)

print("Files have been successfully split into train and valid sets.")
