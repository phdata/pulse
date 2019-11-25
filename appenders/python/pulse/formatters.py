import logging
import datetime
import socket
import os
from pytz import utc


class PulseFormatter(logging.Formatter):
    """
    A Formatter Class that creates a condensed dictionary for Pulse.
    """

    def __init__(self):
        self.PULSE_DEBUG = os.getenv('PULSE_DEBUG', False)
        try:
            self.hostname = socket.gethostname()
            if not self.hostname and self.PULSE_DEBUG:
                logging.debug('No HostName received via socket')
        except Exception:
            if self.PULSE_DEBUG:
                logging.error('Failed to get hostname')


    def format(self, record):
        """
        Condenses LogRecord to smaller Dictionary.

        :param record: :class:`LogRecord` to format
        :type record: logging.LogRecord
        :returns: Condensed dictionary
        :rtype: dict
        """
        data = dict()

        data["category"] = record.name
        data["timestamp"] = datetime.datetime.utcnow()\
            .replace(tzinfo=utc)\
            .strftime('%Y-%m-%dT%H:%M:%SZ')
        data["level"] = record.levelname
        data["message"] = record.msg
        data["threadName"] = record.threadName
        data["hostname"] = self.hostname
        
        return data
