import os
import glob
import shutil
import time
import json


def rename_and_move_files(download_folder, images_folder, results_folder, start_time):
    image_files = glob.glob(os.path.join(download_folder, "*DebugImages*.zip"))
    log_files = glob.glob(os.path.join(download_folder, "*results.zip"))
    image_files.sort(key=os.path.getmtime)
    log_files.sort(key=os.path.getmtime)
    image_files = [file for file in image_files if os.path.getmtime(file) > start_time]
    log_files = [file for file in log_files if os.path.getmtime(file) > start_time]
    for i in range(len(image_files)):
        new_name = f"image_{i}.zip"
        shutil.move(image_files[i], os.path.join(images_folder, new_name))
    for i in range(len(log_files)):
        new_name = f"result_{i}.zip"
        shutil.move(log_files[i], os.path.join(results_folder, new_name))


if __name__ == "__main__":
    current_dir = os.path.dirname(os.path.realpath(__file__))
    with open(os.path.join(current_dir, "log.json"), "r") as f:
        log = json.load(f)
    start_time = log["start_time"]
    download_folder = log["download_folder"]
    images_folder = log["images_folder"]
    results_folder = log["results_folder"]
    rename_and_move_files(download_folder, images_folder, results_folder, start_time)
