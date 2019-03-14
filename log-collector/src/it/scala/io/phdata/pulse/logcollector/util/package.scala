/* Copyright 2018 phData Inc. */

package io.phdata.pulse.logcollector

import java.net.ServerSocket

package object util {

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
