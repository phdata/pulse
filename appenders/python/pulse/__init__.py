#   _______       __
#  |   _   .--.--|  .-----.-----.
#  |.  1   |  |  |  |__ --|  -__|
#  |.  ____|_____|__|_____|_____|
#  |:  |
#  |::.|
#  `---'

import logging.handlers

from pulse.version import (
    __version__, __author__, __author_email__,
    __description__, __license__, __title__, __url__
)
from pulse.formatters import PulseFormatter
from pulse.handlers import PulseHandler
from pulse.metrics import MetricWriter


# Publish class to the `logging.handlers` namespace so that it can be used in
# a logging config file.
logging.handlers.PulseHandler = PulseHandler
