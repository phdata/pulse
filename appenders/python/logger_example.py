import logging.config
import sys
import time
from RequestHandler import RequestHandler

log_collector_host = "http://host:port"
application_name = "pulse_app"

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
    logger = logging.getLogger('pulse-logger')
    logger.setLevel(logging.INFO)

    # Create a console handler so we can see what's going on
    ch = logging.StreamHandler()
    ch.setLevel(logging.INFO)
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    ch.setFormatter(formatter)

    # Create the http handler
    http_handler = RequestHandler(log_collector_host,'/v2/event/' + application_name, 'POST')
    # Turn on debugging so we can see errors (bad url, unresponsive server, etc.)
    http_handler.setDebug()
    logger.addHandler(http_handler)
    logger.addHandler(ch)
    main(sys.argv[1:],logger)
