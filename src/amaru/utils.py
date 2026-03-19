import dotenv
dotenv.load_dotenv()
import random
import os
import pathlib
def get_random_path():
    base_path = os.getenv("BASE_PATH_DATA_RAW")
    raw_data_ = pathlib.Path(base_path)
    list_csv_files = list(raw_data_.rglob("*.csv"))
    position_random = random.randrange(0, len(list_csv_files)) 
    random_csv_file = list_csv_files[position_random]
    return random_csv_file
