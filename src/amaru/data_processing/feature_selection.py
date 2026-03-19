"""
Feature selection pipeline: correlation, variance threshold, and optional mutual information.
Returns the list of selected feature column names (columns that remain / can be used).
Based on the feature_selection.ipynb notebook.
"""

import pathlib
from typing import List, Optional

import numpy as np
import pandas as pd
from sklearn.feature_selection import VarianceThreshold, mutual_info_classif
from sklearn.model_selection import train_test_split


class FeatureSelection:
    """
    Runs the same feature selection process as the notebook:
    union CSVs -> drop timestamp -> drop by correlation -> drop by variance -> filter by MI.
    Result is the list of selected feature column names (for X, excluding label).
    """

    def __init__(self, path_cleaned: str):
        self.path_cleaned = pathlib.Path(path_cleaned)
        self.selected_features: List[str] = []
        self.metadata: dict = {}

    def run(
        self,
        correlation_threshold: float = 0.9,
        variance_threshold: float = 0.01,
        mi_threshold: float = 0.006,
        use_mi_filter: bool = True,
        label_column: str = "label",
        test_size: float = 0.2,
        random_state: int = 42,
    ) -> dict:
        """
        Run the full feature selection pipeline. Returns selected feature names and metadata.

        Returns:
            dict with "selected_features" (list of column names to use) and "metadata".
        """
        # Load and union all cleaned CSVs
        df_with_label = self._union_all_files_with_label()
        df_with_label = df_with_label.drop(columns="timestamp", errors="ignore")
        initial_num_columns = df_with_label.shape[1]

        # Columns to drop by Spearman correlation
        columns_to_drop_correlation = self._get_columns_to_drop_by_correlation(
            df_with_label, threshold=correlation_threshold
        )

        # Columns to drop by variance (on data without label)
        df_without_label = self._union_all_files_without_label()
        df_without_label = df_without_label.drop(columns="timestamp", errors="ignore")
        columns_to_drop_variance = self._get_columns_to_drop_by_variance(
            df_without_label, threshold=variance_threshold
        )

        total_columns_to_drop = list(
            set(columns_to_drop_correlation + columns_to_drop_variance)
        )
        df_with_label = df_with_label.drop(columns=total_columns_to_drop, errors="ignore")
        df_with_label = df_with_label.drop(columns="timestamp", errors="ignore")

        columns_after_drops = [c for c in df_with_label.columns if c != label_column]
        num_after_corr_var = len(columns_after_drops)

        if use_mi_filter and len(columns_after_drops) > 0:
            X = df_with_label.drop(columns=label_column)
            y = df_with_label[label_column]
            X_train, _, y_train, _ = train_test_split(
                X, y, test_size=test_size, random_state=random_state
            )
            mi_scores = mutual_info_classif(
                X_train.values, np.array(y_train), random_state=random_state
            )
            mi_series = pd.Series(mi_scores, index=X.columns)
            selected = mi_series[mi_series > mi_threshold].index.tolist()
            self.selected_features = selected
        else:
            self.selected_features = columns_after_drops

        self.metadata = {
            "initial_num_columns": initial_num_columns,
            "dropped_by_correlation": len(columns_to_drop_correlation),
            "dropped_by_variance": len(columns_to_drop_variance),
            "num_after_correlation_and_variance": num_after_corr_var,
            "final_num_features": len(self.selected_features),
            "selected_features": self.selected_features,
        }
        return {
            "selected_features": self.selected_features,
            "metadata": self.metadata,
        }

    def _union_all_files_with_label(self) -> pd.DataFrame:
        """Union all CSV files under path_cleaned, keeping the label column."""
        list_files = list(self.path_cleaned.rglob("*.csv"))
        if not list_files:
            return pd.DataFrame()
        return pd.concat([pd.read_csv(f, index_col=False) for f in list_files], ignore_index=True)

    def _union_all_files_without_label(self) -> pd.DataFrame:
        """Union all CSV files under path_cleaned, without the label column."""
        list_files = list(self.path_cleaned.rglob("*.csv"))
        if not list_files:
            return pd.DataFrame()
        return pd.concat(
            [pd.read_csv(f, index_col=False).drop(columns=["label"], errors="ignore") for f in list_files],
            ignore_index=True,
        )

    @staticmethod
    def _get_correlation_matrix_spearman(df: pd.DataFrame) -> pd.DataFrame:
        return df.corr(method="spearman")

    @staticmethod
    def _get_columns_to_drop_by_correlation(
        df: pd.DataFrame, threshold: float = 0.9
    ) -> List[str]:
        corr = FeatureSelection._get_correlation_matrix_spearman(df)
        upper = corr.where(np.triu(np.ones(corr.shape), k=1).astype(bool))
        return [col for col in upper.columns if any(upper[col] > threshold)]

    @staticmethod
    def _get_columns_to_drop_by_variance(
        df: pd.DataFrame, threshold: float = 0.01
    ) -> List[str]:
        selector = VarianceThreshold(threshold=threshold)
        selector.fit(df)
        return df.columns[~selector.get_support()].tolist()
