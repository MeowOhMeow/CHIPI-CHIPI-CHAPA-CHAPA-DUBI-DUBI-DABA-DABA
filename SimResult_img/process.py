import os
import shutil
import zipfile


# Function to unzip files and clean up
def unzip(zip_file, dest_dir):
    if not os.path.exists(dest_dir):
        os.makedirs(dest_dir)
    with zipfile.ZipFile(zip_file, "r") as zip_ref:
        zip_ref.extractall(dest_dir)
    files = os.listdir(dest_dir)
    # Delete all files except those ending with "_result.jpg" and starting with "Area"
    for file in files:
        if not file.endswith("_result.jpg") or not file.startswith("Area"):
            os.remove(os.path.join(dest_dir, file))


# Define the main directories
main_dir = os.path.join(os.path.dirname(__file__), "SimResult_img")
base_dir = os.path.join(os.path.dirname(__file__), "zips")
target_dir = main_dir

# Unzip files in the "zips" directory
zip_files = [
    os.path.join(base_dir, f) for f in os.listdir(base_dir) if f.endswith(".zip")
]
for zip_file in zip_files:
    dest_dir = os.path.join(target_dir, os.path.splitext(os.path.basename(zip_file))[0])
    unzip(zip_file, dest_dir)
    os.remove(zip_file)

print("Unzipped all zip files in the zips directory")

# Counter = the largest number of files in the main directory + 1
counter = 0
files = os.listdir(os.path.join(main_dir, "images"))
for file in files:
    if file.endswith(".jpg"):
        counter += 1
print(f"Counter: {counter}")

subdirs_to_remove = []

# Loop through the subdirectories
for subdir, _, files in os.walk(main_dir):
    if subdir != main_dir:  # Ignore the main directory itself
        if subdir.endswith("images"):
            continue
        elif subdir.endswith("labels"):
            continue
        subdirs_to_remove.append(subdir)
        for file in files:
            if file.endswith(".jpg"):
                # Construct the full file path
                old_file_path = os.path.join(subdir, file)

                # Construct the new file path
                new_file_name = f"{counter}.jpg"
                new_file_path = os.path.join(main_dir, "images", new_file_name)

                # Move and rename the file
                shutil.move(old_file_path, new_file_path)

                # Increment the counter
                counter += 1

# Remove the subdirectories
for subdir in subdirs_to_remove:
    os.rmdir(subdir)

print("All files have been moved and renamed successfully.")
