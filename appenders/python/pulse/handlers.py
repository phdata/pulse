import requests
import logging
import sys
from logging.handlers import MemoryHandler
import json


# noinspection PyPep8Naming
class PulseHandler(MemoryHandler):
    """
    A class which sends records to the Pulse log collector. Note that records
    are converted to JSON upon posting. You must provide a
    :class:`logging.Formatter` that returns the record as a dict.
    """

    def __init__(self, endpoint, capacity=0, flushLevel=logging.ERROR):
        """
        Initializes PulseHandler using the REST API endpoint. By default,
        no buffering is done.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param capacity: Number of records to buffer before flushing.
        :type capacity: int
        :param flushLevel: Log level at which to to flush the buffer.
        :rtype: PulseHandler
        """
        MemoryHandler.__init__(self, capacity, flushLevel)
        self.endpoint = endpoint
        self.debug = False
        
    def setDebug(self):
        """
        Set debug mode for this handler.
        """
        self.debug = True

    def setFormatter(self, fmt):
        """
        Set the formatter for this handler.

        :param fmt: Formatter to apply to log records.
        :type fmt: logging.Formatter
        """
        # Check if formatter provided returns a dictionary
        rec = logging.LogRecord("x", 0, "x", 1, "x", [], None)
        if not isinstance(fmt.format(rec), dict):
            raise ValueError("Invalid LogFormatter: You must use a formatter " +
                             "that produces a dictionary.")

        self.formatter = fmt

    def emit(self, buffer):
        """
        Emit the buffered records, depending on the filters added to the
        handler.

        :param buffer: List of records to send to Pulse.
        :type buffer: list(logging.LogRecord)
        """
        # Filter and format records in buffer
        ff_buffer = [
            self.format(record)
            for record in buffer
            if self.filter(record)
        ]

        # If there are records left after filtering, post to Pulse Log Collector
        #
        # TODO(jtbirdsell): Should implement some sort of retry for timeouts
        if len(ff_buffer) > 0:
            self.acquire()
            try:
                requests.post(self.endpoint, json.dumps(ff_buffer),
                              headers={"Content-type": "application/json"})
            except requests.exceptions.RequestException as e:
                self.handleError(ff_buffer)
                if self.debug:
                    sys.stderr.write(e)
            finally:
                self.release()

    def flush(self):
        """
        Flush records from buffer and clear buffer.
        """
        # Emit records in buffer, then clear buffer.
        #
        # TODO(jtbirdsell): Not convinced that acquiring IO lock is necessary
        self.acquire()
        try:
            self.emit(self.buffer)
            self.buffer.clear()
        finally:
            self.release()

    def handle(self, record):
        """
        Add record to buffer and flush when appropriate.

        :param record: Record to send to Pulse
        :type record: logging.LogRecord
        """
        # Append record to buffer and if buffer is at capacity and flush.
        #
        # Note: This is the entry point from logging.Logger
        self.buffer.append(record)
        if self.shouldFlush(record):
            self.flush()

    def handleError(self, records):
        """
        Used to log information about a failed posting of a batch of records.

        :param records: List of records from the failed batch
        :type records: list(logging.LogRecord)
        """
        # Write records from failed batch to stderr
        sys.stderr.write('---Logging error---\n')

        for record in records:
            sys.stderr.write('Message: %r\nArguments: %s\n' % (record.msg,
                                                               record.args))
