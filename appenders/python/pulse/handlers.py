import requests
import logging
import sys
from logging.handlers import MemoryHandler
import json
from queue import Queue
import threading


# noinspection PyPep8Naming
class PulseHandler(MemoryHandler):
    """
    A class which sends records to the Pulse log collector. Note that records
    are converted to JSON upon posting. You must provide a
    :class:`logging.Formatter` that returns the record as a dict.
    """

    def __init__(self, endpoint, capacity=1000, flushLevel=logging.ERROR, threadCount=1):
        """
        Initializes PulseHandler using the REST API endpoint. By default,
        no buffering is done.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param capacity: Number of records to buffer before flushing. Defaults to 1000.
        :type capacity: int
        :param flushLevel: Log level at which to to flush the buffer.
        :type flushLevel: Log Level
        :param threadCount: Number of threads to handle post requests.
        :type threadCount: int
        :rtype: PulseHandler
        """
        super(PulseHandler, self).__init__(capacity, flushLevel)
        self.endpoint = endpoint
        self.debug = False

        # Initialize Threading
        self.thread_count = threadCount
        self.queue = Queue()
        self.threads = list()
        for i in range(self.thread_count):
            thread = threading.Thread(target=self.__threadWorker)
            thread.start()
            self.threads.append(thread)

        # Initialize Logging
        self.logger = logging.getLogger(__name__)
        handler = logging.StreamHandler(sys.stdout)
        fmt = "[%(asctime)s] %(levelname)s [%(name)s.%(funcName)s:%(lineno)d:%(threadName)s] %(message)s"
        handler.setFormatter(logging.Formatter(fmt))
        handler.setLevel(logging.INFO)
        self.logger.addHandler(handler)
        self.logger.setLevel(logging.INFO)
        
    def setDebug(self):
        """
        Set debug mode for MetricWriter.
        """
        self.debug = True
        self.logger.setLevel(logging.DEBUG)
        for handler in self.logger.handlers:
            handler.setLevel(logging.DEBUG)

    def setFormatter(self, fmt):
        """
        Set the formatter for this handler.

        :param fmt: Formatter to apply to log records.
        :type fmt: logging.Formatter
        """
        # Check if formatter provided returns a dictionary
        rec = logging.LogRecord("x", 0, "x", 1, "x", [], None)
        if not isinstance(fmt.format(rec), dict):
            self.logger.error("Invalid LogFormatter")
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
        # Filter records in buffer
        self.logger.debug("Filtering records in buffer")
        filtered_buffer = [
            record
            for record in buffer
            if self.filter(record)
        ]

        # If there are records left after filtering, post to Pulse Log Collector
        self.logger.debug("Putting record buffer into queue")
        if len(filtered_buffer) > 0:
            self.queue.put(filtered_buffer, block=False)

    def __threadWorker(self):
        """
        Method is used for threading API put requests so that they are non-blocking.
        """
        while True:
            buffer = self.queue.get()
            if buffer is None:
                self.logger.debug("Terminating thread")
                break
            try:
                self.logger.debug("Posting records to API endpoint")
                requests.post(self.endpoint,
                              json.dumps([self.format(record) for record in buffer]),
                              headers={"Content-type": "application/json"})
            except requests.exceptions.RequestException:
                self.logger.error("---Posting Error---")
                self.logger.error("---Failed LogRecords Printed Below---")
                for record in buffer:
                    self.logger.handle(record)
            self.queue.task_done()

    def flush(self):
        """
        Flush records from buffer and clear buffer.
        """
        # Emit records in buffer, then clear buffer.
        self.logger.debug("Flushing log records")
        self.emit(self.buffer)
        self.buffer.clear()

    def handle(self, record):
        """
        Add record to buffer and flush when appropriate.

        :param record: Record to send to Pulse
        :type record: logging.LogRecord
        """
        # Append record to buffer and if buffer is at capacity and flush.
        #
        # Note: This is the entry point from logging.Logger
        self.logger.debug("Buffering log record")
        self.buffer.append(record)
        if self.shouldFlush(record):
            self.flush()

    def close(self):
        """
        Flush remaining records and terminate all threads
        """
        self.logger.debug("Cleaning up class")
        super(PulseHandler, self).close()
        for i in range(self.thread_count):
            self.queue.put(None)
        for t in self.threads:
            t.join()
