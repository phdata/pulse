# Pulse Logging - Python

## Installation
Installing package via pip

```bash
pip install pulse-logging
```
Alternatively you could install by running the setup script.

```bash
python setup.py install
```

## Usage
#### Log Appender

```python
import logging
from pulse import PulseHandler
from pulse import PulseFormatter

pulse_handler = PulseHandler("http://host.com:9001/v2/events/app")
pulse_handler.setFormatter(PulseFormatter())
pulse_handler.setLevel(logging.WARNING)
logger = logging.getLogger(__name__)
logger.setLevel(logging.WARNING)
logger.addHandler(pulse_handler)

try:
    raise ValueError
except ValueError as e:
    logger.exception("Bad stuff")
```

##### Logging Configuration File Example

`logging.ini` file contents
```
[loggers]
keys=root

[handlers]
keys=pulse_handler

[formatters]
keys=pulse_formatter

[logger_root]
level=WARNING
handlers=pulse_handler

[handler_pulse_handler]
class=handlers.PulseHandler
level=WARNING
formatter=pulse_formatter
args=("http://host.com:9001/v2/events/app")

[formatter_pulse_formatter]
class=pulse.PulseFormatter

```

`main.py` file contents
```python
import logging
from logging.config import fileConfig

fileConfig("logging.ini")
logger = logging.getLogger()

try:
    raise ValueError
except ValueError as e:
    logger.exception("Bad stuff")

```


#### Writing Metrics

```python
from pulse import MetricWriter

writer = MetricWriter("http://host.com:9001/v1/metrics", "kudu_table_name")
writer.gauge("key1", "r2", 0.952)

writer.close()
```
