import logging
import datetime
import json
from pytz import utc


class StandardFormatter(logging.Formatter):
    """
    A Formatter Class that created a JSON record format for Pulse.
    """

    def __init__(self):
        """
        Initializes StandardFormatter
        """
        logging.Formatter.__init__(self)

    def format(self, record):
        """
        Covert LogRecord to JSON.

        :param record: :class:`LogRecord` to format
        :type record: logging.LogRecord
        :returns: JSON formatted string
        :rtype: str
        """
        data = dict()

        data["category"] = record.name
        data["timestamp"] = datetime.datetime.utcnow().replace(tzinfo=utc).strftime('%Y-%m-%dT%H:%M:%SZ')
        data["level"] = record.levelname
        data["message"] = record.msg
        data["threadName"] = record.threadName

        return json.dumps(data)
