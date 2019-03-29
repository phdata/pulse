from metrics import MetricWriter
import time
import random

log_collector_host="http://host:port"
pulse_app_name="pulse_app"
if __name__ == "__main__":
    num_metrics = 100000
    metric_writer = MetricWriter(log_collector_host + '/v1/metrics/' + pulse_app_name)
    metric_name="metric"
    for i in random.sample(range(0, num_metrics + 1), num_metrics):
        metric_writer.gauge(metric_name, i)
        print("wrote metric {0}={1}".format(metric_name, i))
        time.sleep(.1)
