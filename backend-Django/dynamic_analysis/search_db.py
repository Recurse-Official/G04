import sqlite3
import dynamic_analysis.constants as constants 
def search_apk_in_database(apk_name , db_path = constants.DB_PATH):

    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        # Query to search for the APK by package_name
        query = """
        SELECT * 
        FROM syscalls
        WHERE package_name = ?
        """
        
        # Execute the query
        cursor.execute(query, (apk_name,))
        
        # Fetch results
        results = cursor.fetchall()

        # Close the connection
        conn.close()

        return results

    except sqlite3.Error as e:
        print(f"Database error: {e}")
        return []

# Example usage

apk_name = "bmthx.god102409paperi"

results = search_apk_in_database(apk_name=apk_name)

if results:
    print("Search Results:")
    print(results)
else:
    print("No matching APK found in the database.")
