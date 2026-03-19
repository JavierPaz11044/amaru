import pandas as pd
import amaru.config.config as config

class CleanDataset:
    config = None
    def __init__(self, path, path_config):
        self.path = path
        self.df = None
        self.metadata = None
        self.config = config.Config(path_config)

    def run(self, label_column=None):
        """
        Run the full cleaning pipeline once: load CSV, format columns,
        dropna, drop duplicates, and optionally format label column.
        Returns dataset and metadata with initial and final row/column counts.
        """
        # Load once and record initial dimensions
        self.df = pd.read_csv(self.path, index_col=False)
        initial_num_rows = self.df.shape[0]
        initial_num_columns = self.df.shape[1]
        features = self.config.features
        # Pipeline: format columns -> dropna -> drop duplicates -> optional label
        self._format_columns_dataset(selected_columns=features)
        self._clean_dropna_dataset()
        self._clean_drop_duplicates_dataset()
        self._change_name_columns()
        if label_column is not None:
            self._format_label_objective(label_column=label_column)

        # Final metadata with initial and final line counts
        self.metadata = {
            "initial_num_rows": initial_num_rows,
            "initial_num_columns": initial_num_columns,
            "final_num_rows": self.df.shape[0],
            "final_num_columns": self.df.shape[1],
        }
        return {"dataset": self.df, "metadata": self.metadata}

    def _format_columns_dataset(self, selected_columns=None):
        """Format column names: strip, lower, replace spaces and slash."""
        self.df = self.df.copy()
        self.df.columns = [col.strip() for col in self.df.columns]
        if selected_columns:
            self.df = self.df[selected_columns]
        self.df.columns = [col.lower().replace(" ", "_").replace("/", "-") for col in self.df.columns]

    def _clean_dropna_dataset(self):
        """Drop rows with any NaN."""
        self.df = self.df.dropna()

    def _clean_drop_duplicates_dataset(self):
        """Drop duplicate rows."""
        self.df = self.df.drop_duplicates()

    def _change_name_columns(self):
        self.df = self.df.rename(columns={
            "cwe_flag_count": "cwr_flag_count"
        })

    def _format_label_objective(self, label_column="label"):
        """Convert label column to binary (1 for BENIGN, 0 otherwise)."""
        benign_label = "BENIGN"
        max_label = self.df[label_column].max()
        label_category = 1 if max_label == benign_label else 0
        self.df[label_column] = label_category
