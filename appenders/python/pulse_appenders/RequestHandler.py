from logging import handlers
import logging
import datetime
import json
from batch_requests import AutoBatchRequest


class RequestHandler(handlers.HTTPHandler):
    """
    Custom logging handler for the Pulse log collector
    Inherits HTTPHandler class
    """
    def __init__(self,*args):
        """
        initializes an object
        Args: hostname, url, Method
        Returns: RequestHandler Object
        """
        super(RequestHandler, self).__init__(*args)
        self.debug = False
        self.setFormatter(LogFormatter())
        self.auto_batch = AutoBatchRequest(self.host + self.url)
        
    def setDebug(self):
        self.debug = True
        self.auto_batch.debug = True

    def emit(self, record):
        """
        function which formats log record to json format
        Args: Log record
        Returns: http response
        """
        log_entry = self.format(record)
        try:
            self.auto_batch.add(log_entry)
        except Exception as e:
            if self.debug:
                print(e)


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
        Returns: Log record
        """
        data = {}

        data["category"] = record.name
        data["timestamp"] = datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')
        data["level"] = record.levelname
        data["message"] = record.msg
        data["threadName"] = record.threadName
        return data
