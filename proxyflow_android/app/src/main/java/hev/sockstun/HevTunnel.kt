package hev.sockstun

object HevTunnel {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    /**
     * Starts the tunnel.
     * @param configPath Absolute path to the YAML configuration file.
     * @param fd The raw file descriptor of the TUN interface.
     */
    external fun TProxyStartService(configPath: String, fd: Int)

    /**
     * Stops the tunnel gracefully.
     */
    external fun TProxyStopService()

    /**
     * Retrieves traffic statistics.
     * @return A LongArray containing [tx_packets, tx_bytes, rx_packets, rx_bytes].
     */
    external fun TProxyGetStats(): LongArray?
}