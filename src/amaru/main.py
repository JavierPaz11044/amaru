import json
import shutil
import amaru.config.config as config
import amaru.data_processing.clean_dataset as clean_dataset
import amaru.data_processing.feature_selection as feature_selection
import dotenv
import os
import pathlib
import pandas as pd

from amaru.logger import Logger
dotenv.load_dotenv()
path_config = os.getenv("PATH_CONFIG")
path_raw_data = os.getenv("BASE_PATH_DATA_RAW")
path_cleaned = os.getenv("BASE_PATH_DATA_CLEANED")
path_output = os.getenv("PATH_OUTPUT")
base_path = os.getenv("BASE_PATH_DATASET")
"""
Create and replace the folder in the raw data path
"""
def _create_and_replace_raw_data_folder(name_folder):
    path_dataset_folder = pathlib.Path(base_path) / name_folder
    path_dataset_folder.mkdir(parents=True, exist_ok=True)
    shutil.rmtree(path_dataset_folder, ignore_errors=True)
    path_dataset_folder.mkdir(parents=True, exist_ok=True)
    return path_dataset_folder

def clean_dataset_all_files():
    name_folder = "cleaned"
    path_dataset_folder = _create_and_replace_raw_data_folder(name_folder)
    logger = Logger(name="clean_dataset_all_files")
    list_csv_files = list(pathlib.Path(path_raw_data).rglob("*.csv"))
    logger.info(f"Found {len(list_csv_files)} files to clean")
    map_acum_metadata = {}
    for file in list_csv_files:
        logger.info(f"Cleaning dataset {file}")
        clean_dataset_instance = clean_dataset.CleanDataset(file, path_config)
        clean_dataset_instance.run(label_column="label")
        map_acum_metadata[file] = clean_dataset_instance.metadata
        logger.info(f"Cleaned dataset {file} with {clean_dataset_instance.metadata}")
        file_name = file.name
        path_file = path_dataset_folder / file_name
        clean_dataset_instance.df.to_csv(path_file, index=False)
    logger.info(f"Final metadata: {map_acum_metadata}")
    return map_acum_metadata

def feature_selection_all_files():
    """Run feature selection on cleaned data and persist selected feature column names."""
    logger = Logger(name="feature_selection_all_files")
    if not path_cleaned:
        logger.error("BASE_PATH_DATA_CLEANED is not set in environment")
        return None
    fs = feature_selection.FeatureSelection(path_cleaned=path_cleaned)
    result = fs.run()
    selected = result["selected_features"]
    meta = result["metadata"]
    logger.info(
        "Feature selection: initial_columns=%s, dropped_by_correlation=%s, dropped_by_variance=%s, final_num_features=%s",
        meta["initial_num_columns"],
        meta["dropped_by_correlation"],
        meta["dropped_by_variance"],
        meta["final_num_features"],
    )
    logger.info("Selected features: %s", selected)
    # Persist selected features to output folder (path from env PATH_OUTPUT)
    if not path_output:
        logger.error("PATH_OUTPUT is not set in environment")
        return result
    out_dir = pathlib.Path(path_output)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / "selected_features.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump({"selected_features": selected, "metadata": meta}, f, indent=2)
    logger.info("Wrote selected features to %s", out_file)
    return result