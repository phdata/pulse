# noinspection PyCompatibility
from queue import Queue
import threading
import logging
import sys
import json

import requests


# noinspection PyPep8Naming
class PulseBatcher(object):
    """
    Class to handle batching and submission of Metrics and LogRecords to the
    Pulse Log Collector
    """

    def __init__(self, endpoint, capacity=1000, threadCount=1, logger=None):
        """
        Initializes PulseBatcher using the REST API endpoint, buffer capacity
        and thread count.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param capacity: Number of records to buffer before flushing.
                         Defaults to 1000.
        :type capacity: int
        :param threadCount: Number of threads to handle post requests.
        :type threadCount: int
        :param logger: :class:`logging.Logger` object for debug logging.
                       If none is provided, a StreamHandler will be created to
                       log to sys.stdout. It is recommended that you provide a
                       logger if you plan to use more than one instance of this
                       class.
        :type logger: logging.Logger
        :rtype: PulseBatcher
        """

        self.capacity = capacity
        self.endpoint = endpoint
        self.debug = False
        self.buffer = list()
        self.logger = logger

        # Initialize Threading
        self.thread_count = threadCount
        self.queue = Queue()
        self.threads = list()
        for i in range(self.thread_count):
            thread = threading.Thread(target=self.__threadWorker)
            thread.start()
            self.threads.append(thread)

        if not self.logger:
            self.logger = logging.getLogger(__name__)
            handler = logging.StreamHandler(sys.stdout)
            fmt = "[%(asctime)s] " \
                  "%(levelname)s " \
                  "[%(name)s.%(funcName)s:%(lineno)d:%(threadName)s] " \
                  "%(message)s"
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

    def __threadWorker(self):
        """
        Method is used for threading API put requests so that they are
        non-blocking.
        """
        while True:
            buffer = self.queue.get()
            if buffer is None:
                self.logger.debug("Terminating thread")
                break
            try:
                self.logger.debug("Posting items to API endpoint")
                resp = requests.post(self.endpoint,
                                     json.dumps(buffer),
                                     headers={"Content-type":
                                              "application/json"})
                self.logger.debug("Status: [%s] %s" %
                                  (resp.status_code, resp.text))
            except requests.exceptions.RequestException:
                self.logger.error("---Posting Error---")
                self.logger.error("---Failed Items Printed Below---")
                for item in buffer:
                    self.logger.info(json.dumps(item))
            self.queue.task_done()

    def shouldFlush(self, item):
        """
        Check if the buffer is full.

        :rtype: bool
        """
        # Item parameter is only applicable for the method overriding in the
        # PulseHandler class
        #
        # Note: This method overrides MemoryHandler.shouldFlush when inherited
        # by PulseHandler
        return len(self.buffer) >= self.capacity

    def flush(self):
        """
        Flush items from buffer and clear buffer.
        """
        # Note: This method overrides MemoryHandler.flush when inherited by
        # PulseHandler
        self.logger.debug("Flushing items from buffer")
        self.queue.put(list(self.buffer), block=False)
        self.buffer.clear()

    def handle(self, item):
        """
        Add item to buffer and flush when appropriate.

        :param item: Item to send to Pulse
        :type item: dict or logging.LogRecord
        """
        # Note: This method overrides MemoryHandler.handle when inherited by
        # PulseHandler
        self.logger.debug("Buffering item")
        # Append metric to buffer and if buffer is at capacity and flush.
        self.buffer.append(item)
        if self.shouldFlush(item):
            self.flush()

    def close(self):
        """
        Flush remaining metrics and terminate all threads
        """
        # Note: This method overrides MemoryHandler.close when inherited by
        # PulseHandler
        self.logger.debug("Cleaning up class")
        self.flush()
        for i in range(self.thread_count):
            self.queue.put(None)
        for t in self.threads:
            t.join()
