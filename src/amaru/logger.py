"""
Logger class to handle and format application logs with a consistent style.
Uses Python's standard logging module under the hood.
"""

import logging
import sys
from datetime import datetime
from typing import Optional


class Logger:
    """Handles formatted logging with configurable level, format, and optional file output."""

    # Default format: timestamp | level | message
    DEFAULT_FORMAT = "%(asctime)s | %(levelname)-8s | %(message)s"
    DEFAULT_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

    def __init__(
        self,
        name: Optional[str] = None,
        level: int = logging.INFO,
        log_format: Optional[str] = None,
        date_format: Optional[str] = None,
        filepath: Optional[str] = None,
    ):
        """
        Initialize the logger.

        Args:
            name: Logger name (e.g. module or app name). If None, uses root-like name.
            level: Minimum level to log (e.g. logging.DEBUG, logging.INFO).
            log_format: Format string for log records. Uses DEFAULT_FORMAT if None.
            date_format: Format for timestamps. Uses DEFAULT_DATE_FORMAT if None.
            filepath: If set, also write logs to this file.
        """
        self._name = name or "amaru"
        self._log_format = log_format or self.DEFAULT_FORMAT
        self._date_format = date_format or self.DEFAULT_DATE_FORMAT

        self._logger = logging.getLogger(self._name)
        self._logger.setLevel(level)
        self._logger.handlers.clear()

        formatter = logging.Formatter(self._log_format, datefmt=self._date_format)

        # Console handler
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(level)
        console_handler.setFormatter(formatter)
        self._logger.addHandler(console_handler)

        # Optional file handler
        if filepath:
            file_handler = logging.FileHandler(filepath, encoding="utf-8")
            file_handler.setLevel(level)
            file_handler.setFormatter(formatter)
            self._logger.addHandler(file_handler)

    def debug(self, message: str, *args, **kwargs) -> None:
        """Log a debug message."""
        self._logger.debug(message, *args, **kwargs)

    def info(self, message: str, *args, **kwargs) -> None:
        """Log an info message."""
        self._logger.info(message, *args, **kwargs)

    def warning(self, message: str, *args, **kwargs) -> None:
        """Log a warning message."""
        self._logger.warning(message, *args, **kwargs)

    def error(self, message: str, *args, **kwargs) -> None:
        """Log an error message."""
        self._logger.error(message, *args, **kwargs)

    def exception(self, message: str, *args, **kwargs) -> None:
        """Log an error message and include exception traceback (call from except block)."""
        self._logger.exception(message, *args, **kwargs)

    def set_level(self, level: int) -> None:
        """Set the minimum logging level for this logger and its handlers."""
        self._logger.setLevel(level)
        for handler in self._logger.handlers:
            handler.setLevel(level)
