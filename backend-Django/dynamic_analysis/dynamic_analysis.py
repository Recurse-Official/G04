import subprocess
import time
import os
import sqlite3
from datetime import datetime
import multiprocessing
import hashlib
import logging
import json
import dynamic_analysis.constants as constants;  # Importing the constants module

# setup_logging()

# Function for installing apk into connected device
def install_apk(command):
    print("Called")
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode == 0:
        # print("APK installation : Success")
        return True
    else:
        logging.error(f"APK installation Failure for : {path_to_apk}")
        return False

# Retrieve list of installed apks in connected device
def package_list():
    package_list_output = subprocess.check_output(["adb", "shell", "pm", "list", "packages", "-3"]).decode("utf-8")
    pkg_list = [line.split("package:")[1] for line in package_list_output.splitlines() if line.startswith("package:")]
    return pkg_list

# Perform monkey testing on passed apk, close apk after monkey testing is performed with some added delay
def monkey_testing(package_name):
    monkey_command = f"adb shell monkey -p {package_name} -v {constants.MONKEY_EVENTS} --throttle {constants.MONKEY_THROTTLE}"
    
    # print("Monkey testing and strace: started")
    monkey_process = subprocess.Popen(monkey_command, stdout=subprocess.DEVNULL, shell=True)
    time.sleep(constants.MONKEY_DURATION)
    close_apk(package_name)
    time.sleep(5)

# Attach strace to passed pid
def strace(pid, package_name):
    strace_command = f"adb shell su -c '/data/local/tmp/strace -c -f -o /sdcard/logs.txt -p {pid}'"
    strace_process = subprocess.Popen(strace_command, stdout=subprocess.DEVNULL, shell=True)

# Pull the file containing logs logged by strace from device to machine
def pull_logs_txt(path, package_name):
    subprocess.run(["adb", "pull", "/sdcard/logs.txt", f"{path}/{package_name}.txt"])
    # print("Pulled logs txt")

# Uninstall passed apk
def uninstall_apk(package_name,times = 0):
    result = subprocess.run(["adb", "uninstall", package_name], capture_output=True, text=True)
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
    subprocess.run(f"adb shell am force-stop {package_name}")

# Open apk and wait for it to load
def open_apk(package_name,count = 0):
    subprocess.run(f"adb shell monkey -p {package_name} -c android.intent.category.LAUNCHER 1")
    if count == 0:
        time.sleep(30)
    else:
        time.sleep(15)

# Retrieve pid of passed apk
def get_pid(package_name, i=0):
    pid_result = subprocess.run(f"adb shell pidof {package_name}", capture_output=True, text=True)
    if pid_result.returncode == 0:
        return pid_result.stdout.strip()
    else:
        if i == 3:
            logging.error(f"Failed to retrieve PID for {package_name}")
            uninstall_apk(package_name)
            return None
        else:
            # Recursive call with increased retry count
            open_apk(package_name,count=1)
            return get_pid(package_name, i + 1)
        
def get_sha256(file_path):
    sha256_hash = hashlib.sha256()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

# Add data to the SQL database
def add_to_db(path, package_name, sha,db_path,first_seen = datetime.now()):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Read the contents of the text file
    with open(f"{path}/{package_name}.txt", 'r') as file:
        p = file.readlines()

    # Extract syscall data
    if len(p)>0:
        syscall_data = {}
        for i in p:
            l = [j for j in i.split()]
            syscall_data[l[-1]] = l[3]

        syscall_data.pop('syscall')
        syscall_data.pop('----------------')
        syscall_data.pop('total')
        if '...>' in syscall_data.keys():
            syscall_data.pop('...>')

        # Create table if it doesn't exist
        cursor.execute("CREATE TABLE IF NOT EXISTS syscalls (package_name TEXT,  SHA256 TEXT , First_seen TEXT)")

        # Add columns for new syscalls if they don't exist
        for syscall in syscall_data.keys():
            # Check if the column already exists
            cursor.execute(f"PRAGMA table_info(syscalls)")
            columns = cursor.fetchall()
            if not any(column[1] == syscall for column in columns):
                cursor.execute(f"ALTER TABLE syscalls ADD COLUMN {syscall} INTEGER DEFAULT 0")

        # Insert data into the table
        columns = ['package_name', 'SHA256','First_seen'] + list(syscall_data.keys())
        fs_json = json.dumps(first_seen.strftime("%Y-%m-%dT%H:%M:%SZ"))
        values = [package_name, sha,fs_json] + list(syscall_data.values())
        placeholders = ', '.join('?' * len(values))
        cursor.execute(f"INSERT INTO syscalls ({', '.join(columns)}) VALUES ({placeholders})", values)

        conn.commit()
        conn.close()
        print("Data added to database : Success")
    else:
        pass

# Main function
def dyn_analysis(apk_file_path, files):
    db_path = constants.DB_PATH  #"syscalls.db"   Path to the SQLite database
    if len(files) == 1:
        command = ["adb", "install", "-t", files[0]]
    else:
        space_separated_paths = " ".join(str(path) for path in files)
        command = ["adb", "install-multiple", space_separated_paths]

    print(f"Starting dynamic analysis for : {apk_file_path}")
    pkg_list_before = package_list()  # Retrieve package list before installing apk
    if(install_apk(command)):
        sha = get_sha256(apk_file_path)
        pkg_list_after = package_list()  # Retrieve package list after installing apk
        apk_name = ""
        for i in pkg_list_after:
            if i not in pkg_list_before:
                apk_name = i  # Package name is the difference between the two lists
        print(apk_name)
        open_apk(apk_name)
        pid = get_pid(apk_name)
        if pid:
            # Name processes
            p1 = multiprocessing.Process(target=monkey_testing, args=[apk_name])
            p2 = multiprocessing.Process(target=strace, args=[pid, apk_name])
            time.sleep(1)

            # Start processes
            p1.start()
            p2.start()
            time.sleep(10)

            p1.join()
            p2.join()
            pull_logs_txt(constants.LOGS_DIRECTORY, apk_name)
            time.sleep(1)
            add_to_db(constants.LOGS_DIRECTORY, apk_name, sha,db_path)
            r = uninstall_apk(apk_name)
            if r:
                logging.info(f"Dynamic Analysis Success for :{apk_file_path}")
                return apk_name
    else:
        print("APK Installatio : Failed , Verify if apk s not corrupted ..  ")
        return None


if __name__ == "__main__":
    connected_devices  = subprocess.run(["adb","devices"],capture_output=True,text = True)
    l = connected_devices.stdout.splitlines() 
    l.pop(0)
    if len(l) > 1:
        devices_access = []
        for i in range(len(l)-1):
            devices_access.append(l[i].split())
        if len(devices_access)>1:
            logging.error("Cannot proceed further : Multiple devices connected")
        elif devices_access[0][1] != "device":
            logging.error("Cannot proceed further : Device Unauthorized")
        else:
            logging.info("Proceeding with real device")
            apk = input("Give apk name : ")
            dyn_analysis(apk=apk)
            time.sleep(20)
            subprocess.run(['adb','kill-server'])
    else:
        logging.info("No device found . Launching Emulator ...")
        emu_process = subprocess.Popen([constants.EMULATOR_LOCATION,"-avd",constants.EMULATOR_NAME,"-no-window"])
        logging.info(f"Emulator launched : {constants.EMULATOR_NAME}")
        time.sleep(30)
        apk = input("Give apk name : ")
        dyn_analysis(apk=apk)
        time.sleep(20)
        emu_process.terminate()
        time.sleep(20)
        logging.info(f"Closing Emulator : {constants.EMULATOR_NAME}")
        subprocess.run(["adb","emu","kill"])
        time.sleep(20)
        subprocess.run(['adb','kill-server'])
