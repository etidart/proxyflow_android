package io.github.etidart.proxyflow_android

import kotlin.math.min

object SubnetSplitter {
    fun coverExceptMany(parentCidr: String, excludeIps: Collection<String>): List<Cidr> {
        val (netIp, prefix) = parseCidr(parentCidr)
        val net = netIp and mask(prefix)
        val size = 1L shl (32 - prefix)
        val end = net + size - 1

        val excluded = excludeIps
            .asSequence()
            .map(::parseIp)
            .filter { it in net..end }
            .distinct()
            .sorted()
            .toList()

        if (excluded.isEmpty()) return listOf(Cidr(net, prefix))

        val out = ArrayList<Cidr>()
        var cursor = net

        for (x in excluded) {
            if (cursor < x) out += intervalToCidrs(cursor, x - 1)
            cursor = x + 1
        }
        if (cursor <= end) out += intervalToCidrs(cursor, end)

        return out
    }

    fun contains(parentCidr: String, ip: String): Boolean {
        val (netIp, prefix) = parseCidr(parentCidr)
        return (parseIp(ip) and mask(prefix)) == (netIp and mask(prefix))
    }

    data class Cidr(val network: Long, val prefix: Int) {
        val ip: String get() = longToIp(network)
        val text: String get() = "$ip/$prefix"
        override fun toString(): String = text
    }

    private fun intervalToCidrs(startIn: Long, end: Long): List<Cidr> {
        var start = startIn
        val out = ArrayList<Cidr>()

        while (start <= end) {
            // Largest block aligned at `start`
            val align = if (start == 0L) (1L shl 32)
            else (start and -start)

            // Largest power-of-two block that fits before `end`
            val maxByLen = java.lang.Long.highestOneBit(end - start + 1)

            val blockSize = minOf(align, maxByLen)
            val prefix = 32 - java.lang.Long.numberOfTrailingZeros(blockSize)

            out += Cidr(start, prefix)
            start += blockSize
        }
        return out
    }

    fun parseIp(ip: String): Long {
        val parts = ip.split(".")
        require(parts.size == 4) { "Invalid IPv4: $ip" }
        var r = 0L
        for (p in parts) {
            val o = p.toIntOrNull() ?: throw IllegalArgumentException("Invalid octet: $p")
            require(o in 0..255) { "Octet out of range: $p" }
            r = (r shl 8) or o.toLong()
        }
        return r
    }

    fun parseCidr(cidr: String): Pair<Long, Int> {
        val slash = cidr.indexOf('/')
        require(slash > 0) { "Invalid CIDR: $cidr" }
        val ip = parseIp(cidr.substring(0, slash))
        val prefix = cidr.substring(slash + 1).toIntOrNull()
            ?: throw IllegalArgumentException("Invalid prefix in $cidr")
        require(prefix in 0..32) { "Prefix out of range: $prefix" }
        return ip to prefix
    }

    fun longToIp(n: Long): String =
        (24 downTo 0 step 8).joinToString(".") { sh ->
            ((n shr sh) and 0xFF).toString()
        }

    fun mask(prefix: Int): Long =
        if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
}