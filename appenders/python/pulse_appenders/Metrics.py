import time
import requests
import json
from datetime import datetime,timezone

class Metrics:
    """
    url: Url for Pulsee metrics endpoint, like 'http://host:port/v1/metrics/<application_name>
    """
    def __init__(self, url):
        self.url = url

    def gauge_ts(self, tag, value, timestamp):
        """
        Log a gauge metric
        tag: tag of the gauge
        value: double value of the gauge
        timestamp: timestamp in microsesonds
        """
        data = {}
        data["metric"] = tag
        data["value"] = value
        data["timestamp"] = timestamp
        
        return self.post(data)

    def gauge(self, tag, value):
        """
        Log a gauge metric at the current timestamp microseconds
        tag: tag of the gauge
        value: double value of the gauge
        """
        self.gauge_ts(tag, value, self.unix_time_micros())

    def post(self, data):
        # TODO batch requests
        result = requests.post(self.url, json.dumps([data]),headers={"Content-type": "application/json"}).content
        if result != b'ok':
            print("failed to post metric")
            print(result)
        return result

    def unix_time_micros(self):
        dt = datetime.now()
        # TODO get actual microseconds
        return dt.replace(tzinfo=timezone.utc).timestamp() * 1000 * 1000
