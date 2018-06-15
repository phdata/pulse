import logging.config
import sys
import time
from RequestsHandler import RequestsHandler
"""
Logger Example in python
Creates loggers which takes custom handler created
Takes 2 arguments number of Events and Sleep time
setting level to info and posting a log message
"""
def main(argv,logger):
    if (len(argv) >2):
        numEvents = argv[1]
        sleepTime = argv[2]
    else:
        numEvents = 10000
        sleepTime = 1
    while (numEvents > 0):
        logger.info("This is an info message")
        time.sleep(sleepTime)
        if (numEvents%300 == 0):
            logger.error("This is an error message")
        numEvents=numEvents-1


if __name__ == "__main__":
    logger = logging.getLogger('mylogger')
    http_handler = RequestsHandler('http://0.0.0.0:9005','/event/python_app','POST')
    logger.addHandler(http_handler)
    logger.setLevel(logging.INFO)
    main(sys.argv[1:],logger)
