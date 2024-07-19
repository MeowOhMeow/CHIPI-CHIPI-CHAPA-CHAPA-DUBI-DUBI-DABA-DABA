from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import os
import time


# Load configuration from file
def load_config(filename):
    if not os.path.exists(filename):
        raise FileNotFoundError(f"Config file not found: {filename}")
    with open(filename, "r") as file:
        config = [line.strip() for line in file.readlines()]
    if len(config) != 3:
        raise ValueError("Config file must contain 3 lines")
    if not os.path.exists(config[2]):
        raise FileNotFoundError(f"Apk file not found: {config[2]}")
    return config


# Log in to the application
def login(driver: webdriver.Edge, config: list):
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


def upload_to_slot(driver: webdriver.Edge, slot_id: int, file_path: str):
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
    driver.find_element(By.XPATH, memo_input_xpath).send_keys("Meow, automated test")

    # Click start button
    start_button_xpath = (
        f"/html/body/div/div/main/div/div/div[2]/div[2]/div[{slot_id}]/div[2]/button[1]"
    )
    start_button = driver.find_element(By.XPATH, start_button_xpath)
    driver.execute_script("arguments[0].scrollIntoView(true);", start_button)

    WebDriverWait(driver, 10).until(
        EC.element_to_be_clickable((By.XPATH, start_button_xpath))
    )
    time.sleep(0.5)
    start_button.click()

    # Confirm simulation
    confirm_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[2]/div[3]/div[2]/div/div/div/form/div[2]/button[1]"
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, confirm_button_xpath))
    )
    driver.find_element(By.XPATH, confirm_button_xpath).click()

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
    available_slots = get_available_slots(driver)

    while available_slots:
        upload_to_slot(driver, available_slots[0], config[2])

        # Refresh and get available slots again
        driver.refresh()
        available_slots = get_available_slots(driver)


def remove_simulation(driver: webdriver.Edge):
    driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation/results")
    remove_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[3]/table/tbody/tr[1]/td[5]/button[2]"
    confirm_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[3]/div/div/div/form/div[2]/button[1]"
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, remove_button_xpath))
    )
    driver.find_element(By.XPATH, remove_button_xpath).click()
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, confirm_button_xpath))
    )
    driver.find_element(By.XPATH, confirm_button_xpath).click()
    time.sleep(0.5)

    WebDriverWait(driver, 10).until(
        EC.invisibility_of_element_located(
            (By.XPATH, "/html/body/div/div/main/div/div/div[2]/div[3]/div/div/div/form")
        )
    )


def download_files(driver: webdriver.Edge):
    log_file_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[4]/div/button[2]"
    image_file_xpath = "/html/body/div/div/main/div/div/div[2]/div/div[4]/div/button[4]"

    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, log_file_xpath))
    )

    log_button = driver.find_element(By.XPATH, log_file_xpath)
    image_button = driver.find_element(By.XPATH, image_file_xpath)

    driver.execute_script("arguments[0].scrollIntoView(true);", log_button)
    WebDriverWait(driver, 10).until(
        EC.element_to_be_clickable((By.XPATH, log_file_xpath))
    )
    time.sleep(0.5)
    log_button.click()
    time.sleep(0.5)
    # get status of the image button
    status = image_button.get_attribute("class")
    while "disabled" in status:
        time.sleep(0.5)
        status = image_button.get_attribute("class")
    image_button.click()
    time.sleep(0.5)
    status = image_button.get_attribute("class")
    while "disabled" in status:
        time.sleep(0.5)
        status = image_button.get_attribute("class")


def view_result_and_reupload(driver: webdriver.Edge, config: list):
    while True:
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
                driver.execute_script("arguments[0].scrollIntoView(true);", view_button)
                WebDriverWait(driver, 10).until(
                    EC.element_to_be_clickable((By.XPATH, view_button_xpath))
                )
                time.sleep(0.5)
                view_button.click()

                download_files(driver)
                remove_simulation(driver)

                driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation")
                upload_to_slot(driver, index, config[2])
                any_finished = True
                break

        if not any_finished:
            time.sleep(15)
        driver.refresh()


if __name__ == "__main__":
    current_dir = os.path.dirname(__file__)
    config = load_config(os.path.join(current_dir, "config.txt"))

    driver = webdriver.Edge()
    try:
        login(driver, config)
        start_simulation(driver, config)
        view_result_and_reupload(driver, config)
        # driver.get("https://d392k6hrcntwyp.cloudfront.net/simulation/results")
        # view_button_xpath = "/html/body/div/div/main/div/div/div[2]/div[3]/table/tbody/tr[1]/td[5]/button[1]"
        # WebDriverWait(driver, 10).until(
        #     EC.presence_of_element_located((By.XPATH, view_button_xpath))
        # )
        # driver.find_element(By.XPATH, view_button_xpath).click()
        # download_files(driver)
        # time.sleep(5)
    finally:
        driver.quit()
