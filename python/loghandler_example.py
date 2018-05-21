import logging.config
from RequestsHandler import RequestsHandler
"""
Logger Example in python
Creates loggers which takes custom handler created
setting level to Debug and posting a log message 'hai'
"""

logger = logging.getLogger('mylogger')
http_handler = RequestsHandler('http://0.0.0.0:9000','/log?application=python_app','POST')
logger.addHandler(http_handler)
logger.setLevel(logging.DEBUG)


logger.debug("hai")
