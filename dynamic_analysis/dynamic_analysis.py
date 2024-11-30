import subprocess
import time
import os
import constants  


def install_apk(path_to_apk):
    result = subprocess.run(["adb", "install", "-g", path_to_apk], capture_output=True, text=True)
    # print(result)
    if result.returncode == 0:
        logging.info("APK installation : Success")
        return True
    else:
        logging.error(f"APK installation Failure for : {path_to_apk}")
        return False


def package_list():
    try:
        package_list_output = subprocess.check_output(["adb", "shell", "pm", "list", "packages", "-3"]).decode("utf-8")
        pkg_list = [line.split("package:")[1] for line in package_list_output.splitlines() if line.startswith("package:")]
        return pkg_list
    except Exception as e:
        time.sleep(2)
        package_list()


# Uninstall passed apk
def uninstall_apk(package_name,times = 0):
    result = subprocess.run(["adb", "-s","emulator-5556","uninstall", package_name], capture_output=True, text=True)
    if result.returncode == 0:
        # print("APK uninstallation : Success")
        return True
    else:
        if times < 3:
            logging.error("APK uninstallation : Failure , Trying once again")
            time.sleep(5)
            uninstall_apk(package_name,times+1)
        else:
            logging.error(f"Couldn't  Uninstall : {package_name} ")

# Force stop passed apk (for stopping strace)
def close_apk(package_name):
    subprocess.run(["adb", "-s","emulator-5556","shell", "am", "force-stop", package_name])

# Open apk and wait for it to load
def open_apk(package_name,count = 0):
    subprocess.run(["adb", "-s","emulator-5556","shell", "monkey", "-p",package_name,"-c", "android.intent.category.LAUNCHER", "1"])
    if count == 0:
        time.sleep(30)
    else:
        time.sleep(15)

# Main function
def dyn_analysis(apk,source = constants.SOURCE ):
    db_path = constants.DB_PATH  #"syscalls.db"   Path to the SQLite database
    if source == "directory":
        apk_file_path = os.path.join(constants.APK_DIRECTORY, apk)
        print(f"Starting dynamic analysis for : {apk_file_path}")
        pkg_list_before = package_list()  # Retrieve package list before installing apk
        x = install_apk(apk_file_path)
        pkg_list_after = package_list()  # Retrieve package list after installing apk
        apk_name = ""
        for i in pkg_list_after:
            if i not in pkg_list_before:
                apk_name = i  # Package name is the difference between the two lists
        print(apk_name)
        open_apk(apk_name)
        r = uninstall_apk(apk_name)
        if r:
            logging.info(f"Dynamic Analysis Success for :{apk_file_path}")


if __name__ == "__main__":
    apk = "Give apk name in given folder :"
    dyn_analysis(apk=apk)
