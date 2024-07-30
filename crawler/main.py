from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select
import os
import time
import glob
import shutil
import datetime
import json
import playsound
import pandas as pd
import traceback


current_dir = os.path.dirname(__file__)
if not os.path.exists(os.path.join(current_dir, "htmls")):
    os.makedirs(os.path.join(current_dir, "htmls"))
if not os.path.exists(os.path.join(current_dir, "images")):
    os.makedirs(os.path.join(current_dir, "images"))
if not os.path.exists(os.path.join(current_dir, "results")):
    os.makedirs(os.path.join(current_dir, "results"))
if not os.path.exists(os.path.join(current_dir, "logs")):
    os.makedirs(os.path.join(current_dir, "logs"))


# Load configuration from file
def load_config(filename):
    if not os.path.exists(filename):
        raise FileNotFoundError(f"Config file not found: {filename}")
    with open(filename, "r", encoding="utf-8") as file:
        config = [line.strip() for line in file.readlines()]
    if len(config) != 8:
        raise ValueError(
            "Config file must contain 8 lines: "
            "account ID, "
            "password, "
            "apk file path, "
            "memo, "
            "difficulty, "
            "download path, "
            "sim times, "
            "del sims on site"
        )
    if not os.path.exists(config[2]):
        raise FileNotFoundError(f"Apk file not found: {config[2]}")
    if not os.path.exists(config[5]):
        raise FileNotFoundError(f"Download path not found: {config[5]}")
    difficulties = ("Easy", "Normal", "Hard", "Very Hard")
    config[4] = difficulties[int(config[4])]
    config[6] = int(config[6])
    config[7] = bool(int(config[7]))
    return config


previous_log_in_time = 0


# Log in to the application
def login(driver: webdriver.Edge, config: list):
    global previous_log_in_time

    driver.get("https://d392k6hrcntwyp.cloudfront.net/user-auth")

    # Wait for account ID and password fields to be present
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located(
            (By.XPATH, "/html/body/div/div/main/div/div/div/div[1]/input[1]")
        )
    )

    # Enter account ID and password
    driver.find_element(
        By.XPATH, "/html/body/div/div/main/div/div/div/div[1]/input[1]"
    ).send_keys(config[0])
    driver.find_element(
        By.XPATH, "/html/body/div/div/main/div/div/div/div[1]/input[2]"
    ).send_keys(config[1])
    driver.find_element(
        By.XPATH, "/html/body/div/div/main/div/div/div/div[2]/button"
    ).click()

    # Wait until login is successful
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located(
            (By.XPATH, "/html/body/div/div/div[2]/nav/div[3]/div[2]/a")
        )
    )
    driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation")

    previous_log_in_time = time.time()


# Get available slots
def get_available_slots(driver: webdriver.Edge):
    try:
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.XPATH, "/html/body/input[1]"))
        )
    except:
        return []

    time.sleep(0.5)
    result = []
    slot_status_elements = driver.find_elements(By.CLASS_NAME, "slot-status")

    for index, element in enumerate(slot_status_elements, start=1):
        if element.text == "Available":
            result.append(index)

    return result


def upload_to_slot(driver: webdriver.Edge, slot_id: int, config: list):
    file_path = config[2]
    memo = config[3]
    difficulty = config[4]
    driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation")
    # Upload file
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, "/html/body/input[1]"))
    )
    driver.find_element(By.XPATH, "/html/body/input[1]").send_keys(file_path)

    # Enter memo
    memo_input_xpath = f"/html/body/div/div/main/div/div/div[2]/div[2]/div[{slot_id}]/div[1]/div[6]/input"
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, memo_input_xpath))
    )
    driver.find_element(By.XPATH, memo_input_xpath).send_keys(memo)

    dropdown_xpath = f"/html/body/div/div/main/div/div/div[2]/div[2]/div[{slot_id}]/div[1]/div[5]/select"
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, dropdown_xpath))
    )

    # Locate the dropdown element
    dropdown = Select(driver.find_element(By.XPATH, dropdown_xpath))

    # Select the simulator level by visible text (adjust the text as needed)
    dropdown.select_by_visible_text(difficulty)

    time.sleep(0.5)

    # Click start button
    start_button_xpath = (
        f"/html/body/div/div/main/div/div/div[2]/div[2]/div[{slot_id}]/div[2]/button[1]"
    )
    start_button = driver.find_element(By.XPATH, start_button_xpath)
    driver.execute_script("arguments[0].click();", start_button)

    # Confirm simulation
    confirm_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[2]/div[3]/div[2]/div/div/div/form/div[2]/button[1]"
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, confirm_button_xpath))
    )
    confirm_button = driver.find_element(By.XPATH, confirm_button_xpath)
    driver.execute_script("arguments[0].click();", confirm_button)

    # Wait until the modal is closed
    WebDriverWait(driver, 600).until(
        EC.invisibility_of_element_located(
            (
                By.XPATH,
                "/html/body/div/div/main/div/div/div[2]/div[2]/div[3]/div[2]/div",
            )
        )
    )


# Start simulation in available slots
def start_simulation(driver: webdriver.Edge, config: list):
    driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation")
    available_slots = get_available_slots(driver)
    counter = 0
    while available_slots and counter < config[6]:
        upload_to_slot(driver, available_slots[0], config)

        # Refresh and get available slots again
        driver.refresh()
        available_slots = get_available_slots(driver)
        counter += 1


def remove_simulation(driver: webdriver.Edge):
    driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation/results")
    remove_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[3]/table/tbody/tr[1]/td[5]/button[2]"
    confirm_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[3]/div/div/div/form/div[2]/button[1]"
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, remove_button_xpath))
    )
    remove_button = driver.find_element(By.XPATH, remove_button_xpath)
    driver.execute_script("arguments[0].click();", remove_button)
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, confirm_button_xpath))
    )
    confirm_button = driver.find_element(By.XPATH, confirm_button_xpath)
    driver.execute_script("arguments[0].click();", confirm_button)
    time.sleep(0.5)


data: list = []


def pt_to_float(pt: str) -> float:
    return float(pt[:-3])


def convert_to_second(time: str) -> float:
    time = time.split(":")
    return float(time[0]) * 60 + float(time[1])


def append_info(driver: webdriver.Edge):
    global data

    total_score_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[2]/span[7]"
    game_time_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[2]/span[9]"
    recognition_score_xpath = (
        "/html/body/div/div/main/div/div/div[2]/div/div[2]/span[23]"
    )
    target_score_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[2]/span[25]"

    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, total_score_xpath))
    )

    total_score = pt_to_float(driver.find_element(By.XPATH, total_score_xpath).text)
    game_time = convert_to_second(driver.find_element(By.XPATH, game_time_xpath).text)
    recognition_score = pt_to_float(
        driver.find_element(By.XPATH, recognition_score_xpath).text
    )
    target_score = pt_to_float(driver.find_element(By.XPATH, target_score_xpath).text)

    data.append(
        {
            "total score": total_score,
            "game time": game_time,
            "time score": total_score - recognition_score - target_score,
        }
    )


def download_files(driver: webdriver.Edge, index: int, html_folder: str):
    global current_dir

    status_value_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[1]/span[2]"
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, status_value_xpath))
    )
    status_value = driver.find_element(By.XPATH, status_value_xpath).text
    if status_value != "Finished":
        print("Simulation failed")
        return False

    log_file_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[4]/div/button[2]"
    image_file_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[4]/div/button[4]"

    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, log_file_xpath))
    )

    log_button = driver.find_element(By.XPATH, log_file_xpath)
    image_button = driver.find_element(By.XPATH, image_file_xpath)

    driver.execute_script("arguments[0].click();", log_button)
    time.sleep(0.5)
    # get status of the image button
    status = image_button.get_attribute("class")
    while "disabled" in status:
        time.sleep(0.5)
        status = image_button.get_attribute("class")
    time.sleep(0.5)
    driver.execute_script("arguments[0].click();", image_button)
    time.sleep(0.5)
    status = image_button.get_attribute("class")
    while "disabled" in status:
        time.sleep(0.5)
        status = image_button.get_attribute("class")
    time.sleep(0.5)

    if index != -1:
        append_info(driver)

    source = driver.page_source
    with open(
        os.path.join(html_folder, f"simulation_{index}.html"),
        "w",
        encoding="utf-8",
    ) as file:
        file.write(source)

    return True


idx = 0


def view_result_and_reupload(driver: webdriver.Edge, config: list, html_folder: str):
    global idx, previous_log_in_time
    while idx < config[6] - 3:
        if time.time() - previous_log_in_time > 3600:
            login(driver, config)
        driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation")

        # Wait until the slots are loaded
        WebDriverWait(driver, 10).until(
            EC.presence_of_all_elements_located((By.CLASS_NAME, "slot-status"))
        )

        # Locate all slot elements by class name
        slots = driver.find_elements(By.CLASS_NAME, "slot-status")

        any_finished = False
        # Check each slot's status and find the start button for the ready slot
        for index, slot in enumerate(slots, start=1):
            slot_status = slot.text.strip().lower()

            if slot_status == "finished":
                view_button_xpath = f"/html/body/div/div/main/div/div/div[2]/div[2]/div[{index}]/div[2]/button[3]"
                view_button = driver.find_element(By.XPATH, view_button_xpath)
                driver.execute_script("arguments[0].click();", view_button)

                has_successed = download_files(driver, idx, html_folder)
                idx += 1
                if has_successed and config[7]:
                    remove_simulation(driver)

                upload_to_slot(driver, index, config)
                any_finished = True
                break

        if not any_finished:
            time.sleep(15)


def rename_and_move_files(download_folder, images_folder, results_folder, start_time):
    image_files = glob.glob(os.path.join(download_folder, "*DebugImages*.zip"))
    log_files = glob.glob(os.path.join(download_folder, "*results.zip"))
    image_files.sort(key=os.path.getmtime)
    log_files.sort(key=os.path.getmtime)
    image_files = [file for file in image_files if os.path.getmtime(file) > start_time]
    log_files = [file for file in log_files if os.path.getmtime(file) > start_time]
    for i in range(len(image_files)):
        new_name = f"image_{i}_{int(os.path.getmtime(image_files[i]))}.zip"
        shutil.move(image_files[i], os.path.join(images_folder, new_name))
    for i in range(len(log_files)):
        new_name = f"result_{i}_{int(os.path.getmtime(log_files[i]))}.zip"
        shutil.move(log_files[i], os.path.join(results_folder, new_name))


def wait_till_all_finished(html_folder: str):
    global idx, previous_log_in_time

    run = True
    while run:
        if time.time() - previous_log_in_time > 3600:
            login(driver, config)
        driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation")

        WebDriverWait(driver, 10).until(
            EC.presence_of_all_elements_located((By.CLASS_NAME, "slot-status"))
        )

        slots = driver.find_elements(By.CLASS_NAME, "slot-status")

        run = False

        any_finished = False

        for index, slot in enumerate(slots, start=1):
            slot_status = slot.text.strip().lower()
            run |= slot_status == "in progress"

            if slot_status == "finished":
                view_button_xpath = f"/html/body/div/div/main/div/div/div[2]/div[2]/div[{index}]/div[2]/button[3]"
                view_button = driver.find_element(By.XPATH, view_button_xpath)
                driver.execute_script("arguments[0].click();", view_button)

                has_successed = download_files(driver, idx, html_folder)
                idx += 1
                if has_successed and config[7]:
                    remove_simulation(driver)

                any_finished = True
                run = True
                break

        if not any_finished:
            time.sleep(15)


def remove_not_used_files(download_folder, start_time, html_folder):
    global current_dir

    os.remove(os.path.join(html_folder, "simulation_-1.html"))
    # remove files in download folder from start_time to now
    image_files = glob.glob(os.path.join(download_folder, "*DebugImages.zip"))
    log_files = glob.glob(os.path.join(download_folder, "*results.zip"))
    for image_file in image_files:
        if os.path.getmtime(image_file) > start_time:
            os.remove(image_file)
    for log_file in log_files:
        if os.path.getmtime(log_file) > start_time:
            os.remove(log_file)


def save_logs_to_csv(log_folder):
    global data

    df = pd.DataFrame(data)
    df.to_csv(os.path.join(log_folder, "result.csv"), index=False)


if __name__ == "__main__":
    start = time.time()
    config = load_config(os.path.join(current_dir, "config.txt"))
    print("Config:", config)
    time_stamp = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    html_folder = os.path.join(current_dir, "htmls", time_stamp)
    images_folder = os.path.join(current_dir, "images", time_stamp)
    results_folder = os.path.join(current_dir, "results", time_stamp)
    log_folder = os.path.join(current_dir, "logs", time_stamp)
    os.makedirs(html_folder)
    os.makedirs(images_folder)
    os.makedirs(results_folder)
    os.makedirs(log_folder)
    with open(os.path.join(current_dir, "log.json"), "w") as file:
        json.dump(
            {
                "download_folder": config[5],
                "html_folder": html_folder,
                "images_folder": images_folder,
                "results_folder": results_folder,
                "start_time": start,
            },
            file,
        )

    driver = webdriver.Edge()
    try:
        print("Logging in")
        login(driver, config)
        driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation/results")
        view_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[3]/table/tbody/tr[1]/td[5]/button[1]"
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.XPATH, view_button_xpath))
        )
        driver.find_element(By.XPATH, view_button_xpath).click()
        download_files(driver, -1, html_folder)
        input("Press Enter to continue...")

        print("Starting simulation")
        start_simulation(driver, config)
        remove_not_used_files(config[5], start, html_folder)
        print("Viewing results and reuploading")
        view_result_and_reupload(driver, config, html_folder)
        print("Waiting for all simulations to finish")
        wait_till_all_finished(html_folder)
        print("All simulations finished")
        # driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation/results")
        # view_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[3]/table/tbody/tr[1]/td[5]/button[1]"
        # WebDriverWait(driver, 10).until(
        #     EC.presence_of_element_located((By.XPATH, view_button_xpath))
        # )
        # driver.find_element(By.XPATH, view_button_xpath).click()
        # download_files(driver)
        # remove_simulation(driver)
        # time.sleep(5)
    except Exception as e:
        print("Error:")
        traceback.print_exc()
    finally:
        driver.quit()

    print("Renaming and moving files")
    rename_and_move_files(
        config[5],
        images_folder,
        results_folder,
        start,
    )
    save_logs_to_csv(log_folder)
    print("Done")
    playsound.playsound(os.path.join(current_dir, "sakana.wav"), True)
