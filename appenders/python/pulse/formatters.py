import logging
import datetime
from pytz import utc


class PulseFormatter(logging.Formatter):
    """
    A Formatter Class that creates a condensed dictionary for Pulse.
    """

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

        return data
