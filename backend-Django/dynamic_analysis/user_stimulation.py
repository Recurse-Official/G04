import subprocess
import random
import time

def simulate_user_events(event_count=5000):
    """
    Simulates user events like taps, swipes, and scrolls using adb commands.

    Parameters:
    - event_count (int): The number of user events to simulate. Default is 5000.
    """
    try:
        # Screen dimensions (Modify as per your device resolution)
        screen_width = 1080
        screen_height = 1920

        # Loop through the number of events
        for i in range(event_count):
            event_type = random.choice(["tap", "swipe", "scroll", "key"])

            if event_type == "tap":
                # Random tap on the screen
                x = random.randint(0, screen_width)
                y = random.randint(0, screen_height)
                command = f"adb shell input tap {x} {y}"
                print(f"Simulating tap at ({x}, {y})")
            
            elif event_type == "swipe":
                # Random swipe from one point to another
                x1 = random.randint(0, screen_width)
                y1 = random.randint(0, screen_height)
                x2 = random.randint(0, screen_width)
                y2 = random.randint(0, screen_height)
                duration = random.randint(100, 1000)  # Duration in ms
                command = f"adb shell input swipe {x1} {y1} {x2} {y2} {duration}"
                print(f"Simulating swipe from ({x1}, {y1}) to ({x2}, {y2}) over {duration} ms")
            
            elif event_type == "scroll":
                # Simulate scrolling (swipe upward or downward)
                x = random.randint(screen_width // 4, 3 * screen_width // 4)
                y1 = random.randint(screen_height // 2, screen_height)
                y2 = random.randint(0, screen_height // 2)
                duration = random.randint(100, 1000)  # Duration in ms
                command = f"adb shell input swipe {x} {y1} {x} {y2} {duration}"
                print(f"Simulating scroll from ({x}, {y1}) to ({x}, {y2}) over {duration} ms")
            
            elif event_type == "key":
                # Random key events (like BACK or HOME)
                key_event = random.choice(["KEYCODE_BACK", "KEYCODE_HOME", "KEYCODE_APP_SWITCH"])
                command = f"adb shell input keyevent {key_event}"
                print(f"Simulating key event: {key_event}")

            # Execute the command
            subprocess.run(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

            # Random delay between events to simulate real usage
            time.sleep(random.uniform(0.1, 0.5))

        print("Event simulation completed successfully!")
    except Exception as e:
        print(f"An error occurred: {e}")

# Example usage
if __name__ == "__main__":
    simulate_user_events(event_count=5000)