import yaml
from pathlib import Path


class Config:
    _instance = None
    _config = None
    def __init__(self, config_path: str = "config.yml"):
        if self._instance is None:
            self._instance = self
            self._load_config(config_path) 

    def _load_config(self, config_path: str):
        path = Path(config_path)

        if not path.exists():
            raise FileNotFoundError(f"Config file not found: {config_path}")

        with open(path, "r") as f:
            self._config = yaml.safe_load(f)

    @property
    def features(self):
        print("Accessing features from config", self._config)
        return self._config.get("features_unavailable", [])

    @property
    def features_available(self):
        return self._config.get("features", [])