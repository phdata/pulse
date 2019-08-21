package io.phdata.pulse.logcollector

import java.net.ServerSocket

object ServiceUtil {

  /**
   * Find an open port
   * @return integer of an open ephemeral port
   */
  def getNextPort: Int = {
    val ss   = new ServerSocket(0)
    val port = ss.getLocalPort
    ss.close()
    port
  }

}
