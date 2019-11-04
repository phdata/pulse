package io.phdata.pulse.log;


import org.apache.log4j.Logger;


public class SLF4j {
    public static void main(String[] args) {

        Logger logger = Logger.getLogger(SLF4j.class);

        final String message = "Hello logging!";
        logger.trace(message);
        logger.debug(message);
        logger.info(message);
        logger.warn(message);
        logger.error(message);
    }
}
