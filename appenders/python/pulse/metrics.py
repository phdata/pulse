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

from datetime import datetime
import atexit

from pytz import utc
import six

from pulse.batcher import PulseBatcher


# noinspection PyPep8Naming
class MetricWriter(PulseBatcher):
    """
    A class which sends metrics to the Pulse Log Collector.
    """

    def __init__(self, endpoint, table, capacity=1000,
                 threadCount=1, logger=None):
        """
        Initializes a MetricWriter using the provided endpoint, table and
        optional buffer capacity.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param table: Kudu table to store metrics in
        :type table: str
        :param capacity: The maximum number of metrics to buffer before
                                flushing. If no value is provided, each metric
                                will be submitted one at a time. Defaults to
                                1000.
        :param threadCount: Number of threads to handle post requests.
        :type threadCount: int
        :param logger: :class:`logging.Logger` object for debug logging. If
                       none is provided, a StreamHandler will be created to log
                       to sys.stdout. It is recommended that you provide a
                       logger if you plan to use more than one instance of this
                       class.
        :type logger: logging.Logger
        :rtype: MetricWriter
        """
        PulseBatcher.__init__(self, endpoint, capacity, threadCount, logger)
        self.table = table
        # Cleanup when Python terminates
        atexit.register(self.close)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    def gaugeTimestamp(self, key, tag, value, timestamp,
                       frmt="%Y-%m-%dT%H:%M:%S.%f"):
        """
        Gauge metric at the provided timestamp.

        :param key: Key or ID of the gauge
        :type key: int or str
        :param tag: Tag or name of the gauge
        :type tag: str
        :param value: Value of the gauge.
        :type value: float
        :param timestamp: Timestamp at which to gauge the metric.
                          If timezone is not provided, UTC is assumed.
        :type timestamp: str or :class:`datetime.datetime`
        :param frmt: Required if a string timestamp is provided
                     Uses the C strftime() function, see strftime(3)
                     documentation.
        :type frmt: str
        """
        self.logger.debug("Constructing metric data")
        data = dict()
        data["key"] = key
        data["tag"] = tag
        data["value"] = value
        data["ts"] = self.unixTimeMicros(timestamp, frmt)

        self.handle(data)

    def gauge(self, key, tag, value):
        """
        Gauge metric at the current timestamp.

        :param key: Key or ID of the gauge
        :type key: int or str
        :param tag: Tag or name of the gauge
        :type tag: str
        :param value: Value of the gauge.
        :type value: float
        :rtype: requests.Response
        """
        self.gaugeTimestamp(key, tag, value, datetime.utcnow())

    def flush(self):
        """
        Flush items from buffer and clear buffer.
        """
        # Overrides PulseBatcher.flush
        self.buffer = {"table_name": self.table, "payload": self.buffer}
        self.queue.put(dict(self.buffer), block=False)
        self.buffer = list()

    @staticmethod
    def unixTimeMicros(timestamp, frmt="%Y-%m-%dT%H:%M:%S.%f"):
        """
        Convert incoming datetime value to a integer representing
        the number of microseconds since the unix epoch

        :param timestamp: If a string is provided, a format must be provided as
                          well. A tuple may be provided in place of the
                          timestamp with a string value and a format. This is
                          useful for predicates and setting values where this
                          method is indirectly called. Timezones provided in
                          the string are not supported at this time. UTC unless
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
                             "You must provide a datetime or a string")

        # If datetime has a valid timezone assigned, convert it to UTC.
        if timestamp.tzinfo and timestamp.utcoffset():
            timestamp = timestamp.astimezone(utc)
        # If datetime has no timezone, it is assumed to be UTC
        else:
            timestamp = timestamp.replace(tzinfo=utc)

        # Return the unixtime_micros for the provided datetime and locale.
        # Avoid timedelta.total_seconds() for py2.6 compatibility.
        td = timestamp - datetime.fromtimestamp(0, utc)
        td_micros = td.microseconds + (
                td.seconds + td.days * 24 * 3600
        ) * 10 ** 6

        return int(td_micros)
