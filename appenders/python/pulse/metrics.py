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


class MetricWriter:
    """
    A class which sends metrics to the Pulse Log Collector.
    """

    def __init__(self, endpoint, buffer_capacity=0):
        """
        Initializes a MetricWriter using the provided endpoint and optional
        buffer capacity.

        :param endpoint: The REST API endpoint for the Pulse Log Collector
        :type endpoint: str
        :param buffer_capacity: The maximum number of metrics to buffer before
                                flushing. If no value is provided, each metric
                                will be submitted one at a time.
        :rtype: MetricWriter
        """
        self.endpoint = endpoint
        self.buffer_capacity = buffer_capacity
        self.buffer = []

    def gauge_ts(self, tag, value, timestamp, frmt="%Y-%m-%dT%H:%M:%S.%f"):
        """
        Log a gauge metric at the provided timestamp. Returns the response from
        the post request.

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
        :rtype: requests.Response
        """
        data = dict()
        data["metric"] = tag
        data["value"] = value
        data["timestamp"] = self.unix_time_micros(timestamp, frmt)
        
        return self.post(data)

    def gauge(self, tag, value):
        """
        Log a gauge metric at the current timestamp. Returns the response from
        the post request.

        :param tag: Tag or name of the gauge
        :type tag: str
        :param value: Value of the gauge.
        :type value: float
        :rtype: requests.Response
        """
        return self.gauge_ts(tag, value, datetime.utcnow())

    def post(self, data):
        """
        Post metric to REST API endpoint for Pulse Log Collector.

        :param data: Metric data to be logged
        :type data: dict
        :rtype: requests.Response
        """
        self.buffer.append(data)
        result = None

        if len(self.buffer) >= self.buffer_capacity:
            try:
                result = requests.post(
                            self.endpoint,
                            json.dumps(self.buffer),
                            headers={"Content-type": "application/json"}
                )
            except requests.exceptions.RequestException as e:
                sys.stderr.write(e)

        return result

    @staticmethod
    def unix_time_micros(timestamp, frmt="%Y-%m-%dT%H:%M:%S.%f"):
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
