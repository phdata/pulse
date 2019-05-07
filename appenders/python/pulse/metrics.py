# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# Including Apache 2 license because unix_time_micros method was pulled
# from Apache Kudu project.

import requests
import json
from datetime import datetime
from pytz import utc
import six
import sys
from queue import Queue
import threading
import logging


# noinspection PyPep8Naming
class MetricWriter:
    """
    A class which sends metrics to the Pulse Log Collector.
    """

    def __init__(self, endpoint, capacity=1000, threadCount=1):
        """
        Initializes a MetricWriter using the provided endpoint and optional
        buffer capacity.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param capacity: The maximum number of metrics to buffer before
                                flushing. If no value is provided, each metric
                                will be submitted one at a time. Defaults to
                                1000.
        :param threadCount: Number of threads to handle post requests.
        :type threadCount: int
        :rtype: MetricWriter
        """
        self.endpoint = endpoint
        self.buffer_capacity = capacity
        self.buffer = list()
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

    def gaugeTimestamp(self, tag, value, timestamp, frmt="%Y-%m-%dT%H:%M:%S.%f"):
        """
        Log a gauge metric at the provided timestamp.

        :param tag: Tag or name of the gauge
        :type tag: str
        :param value: Value of the gauge.
        :type value: float
        :param timestamp: Timestamp at which to log the gauge metric.
        :type timestamp: str or :class:`datetime.datetime`
        :param frmt: Required if a string timestamp is provided
                     Uses the C strftime() function, see strftime(3)
                     documentation.
        :type frmt: str
        """
        self.logger.debug("Constructing metric data")
        data = dict()
        data["metric"] = tag
        data["value"] = value
        data["timestamp"] = self.unixTimeMicros(timestamp, frmt)
        
        self.handle(data)

    def gauge(self, tag, value):
        """
        Log a gauge metric at the current timestamp.

        :param tag: Tag or name of the gauge
        :type tag: str
        :param value: Value of the gauge.
        :type value: float
        :rtype: requests.Response
        """
        self.gaugeTimestamp(tag, value, datetime.utcnow())

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
                self.logger.debug("Posting metrics to API endpoint")
                self.logger.debug(json.dumps(buffer))
                resp = requests.post(self.endpoint,
                                     json.dumps(buffer),
                                     headers={"Content-type": "application/json"})
                self.logger.debug("Status: [%s] %s" % (resp.status_code, resp.text))
            except requests.exceptions.RequestException:
                self.logger.error("---Posting Error---")
                self.logger.error("---Failed Metrics Printed Below---")
                for metric in buffer:
                    self.logger.info(json.dumps(metric))
            self.logger.debug("Queue task completed")
            self.queue.task_done()

    def shouldFlush(self):
        """
        Check if the buffer is full.

        :rtype: bool
        """
        return len(self.buffer) >= self.buffer_capacity

    def flush(self):
        """
        Flush metrics from buffer and clear buffer.
        """
        self.logger.debug("Flushing metrics from buffer")
        self.queue.put(list(self.buffer), block=False)
        self.buffer.clear()

    def handle(self, metric):
        """
        Add metric to buffer and flush when appropriate.

        :param metric: Metric to send to Pulse
        :type metric: dict
        """
        self.logger.debug("Buffering metric")
        # Append metric to buffer and if buffer is at capacity and flush.
        self.buffer.append(metric)
        if self.shouldFlush():
            self.flush()

    def close(self):
        """
        Flush remaining metrics and terminate all threads
        """
        self.logger.debug("Cleaning up class")
        self.flush()
        for i in range(self.thread_count):
            self.queue.put(None)
        for t in self.threads:
            t.join()

    @staticmethod
    def unixTimeMicros(timestamp, frmt="%Y-%m-%dT%H:%M:%S.%f"):
        """
        Convert incoming datetime value to a integer representing
        the number of microseconds since the unix epoch

        :param timestamp: If a string is provided, a format must be provided as
                          well. A tuple may be provided in place of the
                          timestamp with a string value and a format. This is
                          useful for predicates and setting values where this
                          method is indirectly called. Timezones provided in the
                          string are not supported at this time. UTC unless
                          provided in a datetime object.
        :type timestamp: str or :class:`datetime.datetime`
        :param frmt: Required if a string timestamp is provided
                     Uses the C strftime() function, see strftime(3)
                     documentation.
        :type frmt: str
        :rtype: int
        """
        # Validate input
        if isinstance(timestamp, datetime):
            pass
        elif isinstance(timestamp, six.string_types):
            timestamp = datetime.strptime(timestamp, frmt)
        elif isinstance(timestamp, tuple):
            timestamp = datetime.strptime(timestamp[0], timestamp[1])
        else:
            raise ValueError("Invalid timestamp type. " +
                             "You must provide a datetime.datetime or a string")

        # If datetime has a valid timezone assigned, convert it to UTC.
        if timestamp.tzinfo and timestamp.utcoffset():
            timestamp = timestamp.astimezone(utc)
        # If datetime has no timezone, it is assumed to be UTC
        else:
            timestamp = timestamp.replace(tzinfo=utc)

        # Return the unixtime_micros for the provided datetime and locale.
        # Avoid timedelta.total_seconds() for py2.6 compatibility.
        td = timestamp - datetime.fromtimestamp(0, utc)
        td_micros = td.microseconds + (td.seconds + td.days * 24 * 3600) * 10**6

        return int(td_micros)
