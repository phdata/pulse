import requests
import logging
import sys


# noinspection PyPep8Naming
class PulseHandler(logging.handlers.MemoryHandler):
    """
    A class which sends records to the Pulse log collector.
    """

    def __init__(self, endpoint, capacity=0, flushLevel=logging.ERROR):
        """
        Initializes PulseHandler using the REST API endpoint. By default, no buffering
        is done.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param capacity: Number of records to buffer before flushing.
        :type capacity: int
        :param flushLevel: Log level at which to to flush the buffer.
        :rtype: PulseHandler
        """
        logging.handlers.MemoryHandler.__init__(self, capacity, flushLevel)
        self.endpoint = endpoint
        self.debug = False
        
    def setDebug(self):
        self.debug = True

    def handle(self, buffer):
        """
        Emit the buffered records, depending on the filters added to the handler.

        :param buffer: List of records to send to Pulse
        :type buffer: list(logging.LogRecord)
        """
        filtered_buffer = [rec for rec in buffer if self.filter(rec)]
        if len(filtered_buffer) > 0:
            self.acquire()
            try:
                self.emit(filtered_buffer)
            finally:
                self.release()

    def flush(self):
        """
        Flush records from buffer and clear buffer.
        """
        self.acquire()
        try:
            self.handle(self.buffer)
            self.buffer.clear()
        finally:
            self.release()

    def emit(self, buffer):
        """
        Send the record to the Pulse log collector as a JSON formatted string.

        :param buffer: List of records to send to Pulse
        :type buffer: list(logging.LogRecord)
        """
        records = [self.format(rec) for rec in buffer]

        try:
            requests.post(self.endpoint, records, headers={"Content-type": "application/json"})
        except requests.exceptions.RequestException as e:
            self.handleError(records)
            if self.debug:
                sys.stderr.write(e)

    def handleError(self, records):
        """
        Used to log information about a failed posting of a batch of records.

        :param records: List of records from the failed batch
        :type records: list(logging.LogRecord)
        """
        sys.stderr.write('---Logging error---\n')

        for record in records:
            sys.stderr.write('Message: %r\nArguments: %s\n' % (record.msg, record.args))
