import requests
from logging import handlers
from LogFormatter import LogFormatter

class RequestsHandler(handlers.HTTPHandler):
    """
    Custom loggin handler
    Inherits HTTPHandler class
    """
    def __init__(self,*args):
        """
        initializes an object
        Args: hostname, url, Method
        Returns: RequestsHandler Object
        """
        super(RequestsHandler, self).__init__(*args)

    def emit(self, record):
        """
        function which formats log record to json format
        Args: Log record
        Returns: http response
        """
        self.setFormatter(LogFormatter())
        log_entry = self.format(record)
        return requests.post(self.host+self.url, log_entry,headers={"Content-type": "application/json"}).content
