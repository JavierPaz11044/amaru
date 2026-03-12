import pandas as pd


class CleanDataset:
    def __init__(self, path):
        self.path = path
        self.df = None
        self.metadata = None

    def run(self, label_column=None):
        """
        Run the full cleaning pipeline once: load CSV, format columns,
        dropna, drop duplicates, and optionally format label column.
        Returns dataset and metadata with initial and final row/column counts.
        """
        # Load once and record initial dimensions
        self.df = pd.read_csv(self.path)
        initial_num_rows = self.df.shape[0]
        initial_num_columns = self.df.shape[1]

        # Pipeline: format columns -> dropna -> drop duplicates -> optional label
        self._format_columnas_dataset()
        self._clean_dropna_dataset()
        self._clean_drop_duplicates_dataset()
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

    def _format_columnas_dataset(self):
        """Format column names: strip, lower, replace spaces and slash."""
        self.df = self.df.copy()
        self.df.columns = [
            col.strip().lower().replace(" ", "_").replace("/", "-")
            for col in self.df.columns
        ]

    def _clean_dropna_dataset(self):
        """Drop rows with any NaN."""
        self.df = self.df.dropna()

    def _clean_drop_duplicates_dataset(self):
        """Drop duplicate rows."""
        self.df = self.df.drop_duplicates()

    def _format_label_objective(self, label_column="label"):
        """Convert label column to binary (1 for BENIGN, 0 otherwise)."""
        benign_label = "BENIGN"
        max_label = self.df[label_column].max()
        label_category = 1 if max_label == benign_label else 0
        self.df[label_column] = label_category
