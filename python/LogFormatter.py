import logging
import time
import json

class LogFormatter(logging.Formatter):
    """
    Custom log formatter
    inherits Formatter class
    """
    def __init__(self, task_name=None):
        self.task_name = task_name
        super(LogFormatter, self).__init__()

    def format(self, record):
        """
        Formats record to json
        Args: Log Record
        Returns: Json formatted log record
        """
        data = {}

        data["category"] = record.name
        data["timestamp"] = time.time()
        data["level"] = record.levelname
        data["message"] = record.msg
        data["threadName"] = record.threadName
        return json.dumps(data)