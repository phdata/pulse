import logging
import atexit
from logging.handlers import MemoryHandler

from pulse.batcher import PulseBatcher


# noinspection PyPep8Naming
class PulseHandler(PulseBatcher, MemoryHandler):
    """
    A class which sends records to the Pulse log collector. Note that records
    are converted to JSON upon posting. You must provide a
    :class:`logging.Formatter` that returns the record as a dict.
    """
    # Note: Method Resolution Order (MRO):
    #       self -> PulseBatcher -> MemoryHandler -> object
    # Some methods in this class override methods in the order noted above

    def __init__(self, endpoint, capacity=1000, flushLevel=logging.ERROR,
                 threadCount=1, logger=None):
        """
        Initializes PulseHandler using the REST API endpoint, buffer capacity,
        buffer flush level and thread count.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param capacity: Number of records to buffer before flushing.
                         Defaults to 1000.
        :type capacity: int
        :param flushLevel: Log level at which to to flush the buffer.
        :type flushLevel: Log Level
        :param threadCount: Number of threads to handle post requests.
        :type threadCount: int
        :param logger: :class:`logging.Logger` object for debug logging. If none
                       is provided, a StreamHandler will be created to log to
                       sys.stdout. It is recommended that you provide a
                       logger if you plan to use more than one instance of this
                       class.
        :type logger: logging.Logger
        :rtype: PulseHandler
        """
        PulseBatcher.__init__(self, endpoint, capacity, threadCount, logger)
        MemoryHandler.__init__(self, capacity, flushLevel)
        # Cleanup when Python terminates
        atexit.register(self.close)

    def setFormatter(self, fmt):
        """
        Set the formatter for this handler.

        :param fmt: Formatter to apply to log records.
        :type fmt: logging.Formatter
        """
        # Note: Overrides MemoryHandler.setFormatter
        #
        # Check if formatter provided returns a dictionary
        rec = logging.LogRecord("x", 0, "x", 1, "x", [], None)
        if not isinstance(fmt.format(rec), dict):
            self.logger.error("Invalid LogFormatter")
            raise ValueError("Invalid LogFormatter: You must use a formatter " +
                             "that produces a dictionary.")

        self.formatter = fmt

    def shouldFlush(self, record):
        """
        Check for buffer full or a record at the flushLevel or higher.
        """
        # Note: Calling class method directly to avoid MRO
        #
        # Note: Overrides PulseBatcher.shouldFlush which overrides
        # MemoryHandler.shouldFlush
        #       when inherited by this class
        return MemoryHandler.shouldFlush(self, record)

    def flush(self):
        """
        Flush items from buffer and clear buffer.
        """
        # Format records before flushing. This couldn't be done sooner because
        # of the LogLevel check
        # in the shouldFlush method.
        #
        # Note: Overrides PulseBatcher.flush which overrides MemoryHandler.flush
        # when inherited by this class
        self.logger.debug("Applying format to log record")

        self.buffer = [
            self.format(record)
            for record in self.buffer
        ]

        PulseBatcher.flush(self)

    def handle(self, record):
        """
        Add item to buffer and flush when appropriate.

        :param record: Record to send to Pulse
        :type record: logging.LogRecord
        """
        # Note: Overrides PulseBatcher.handle which overrides
        # MemoryHandler.handle when inherited by this class
        self.logger.debug("Applying filter to log record")
        if self.filter(record):
            super(PulseHandler, self).handle(record)

    def close(self):
        """
        Flush remaining records and terminate all threads
        """
        # Note: Overrides PulseBatcher.close which overrides MemoryHandler.close
        # when inherited by this class
        MemoryHandler.close(self)
        PulseBatcher.close(self)
