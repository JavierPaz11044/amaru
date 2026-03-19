"""
Prepare dataset for analysis: read cleaned CSVs, keep only columns from config
(features-metadata + features_by_selection), and write to the path-to-process folder.
"""

from pathlib import Path
from typing import List

import pandas as pd
from amaru.utils import print_progress_bar


class BalanceDataset:
    """
    For each CSV in the cleaned path: keep only columns listed in config
    (features_metadata + features_by_selection), drop the rest, and write to path_to_process.
    """

    def __init__(self, path_cleaned: str, path_to_process: str, config):
        self.path_cleaned = Path(path_cleaned)
        self.path_to_process = Path(path_to_process)
        self.config = config

    def run(self) -> dict:
        """
        Process all CSVs in path_cleaned and write filtered CSVs to path_to_process.
        Returns metadata (e.g. list of files processed).
        """
        columns_to_keep = self._columns_to_keep()
        self.path_to_process.mkdir(parents=True, exist_ok=True)
        list_files = list(self.path_cleaned.rglob("*.csv"))
        processed = []
        total_files = len(list_files)
        for idx, file_path in enumerate(list_files, start=1):
            print_progress_bar(idx, total_files, filename=file_path.name)
            df = pd.read_csv(file_path, index_col=False)
            # Keep only columns that exist in the CSV and are in our config list
            existing = [c for c in columns_to_keep if c in df.columns]
            df_filtered = df[existing]
            out_path = self.path_to_process / file_path.name
            df_filtered.to_csv(out_path, index=False)
            processed.append({"source": str(file_path), "destination": str(out_path), "columns_kept": len(existing)})
        return {"columns_to_keep": columns_to_keep, "processed": processed, "num_files": len(processed)}

    def _columns_to_keep(self) -> List[str]:
        """Union of features_metadata and features_by_selection from config."""
        meta = self.config.features_metadata or []
        selection = self.config.features_by_selection or []
        return list(dict.fromkeys(meta + selection))
