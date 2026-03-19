import dotenv
dotenv.load_dotenv()
import random
import os
import pathlib
import sys


def print_progress_bar(current: int, total: int, filename: str = "", bar_width: int = 30) -> None:
    """
    Print a simple progress bar to stdout and refresh the same line.
    This is dependency-free so it can be reused in other modules.
    """
    if total <= 0:
        return
    ratio = current / total
    filled = int(bar_width * ratio)
    bar = "#" * filled + "-" * (bar_width - filled)
    sys.stdout.write(f"\r[{bar}] {current}/{total} | {filename}")
    sys.stdout.flush()
    if current >= total:
        sys.stdout.write("\n")
def get_random_path():
    base_path = os.getenv("BASE_PATH_DATA_RAW")
    raw_data_ = pathlib.Path(base_path)
    list_csv_files = list(raw_data_.rglob("*.csv"))
    position_random = random.randrange(0, len(list_csv_files)) 
    random_csv_file = list_csv_files[position_random]
    return random_csv_file
