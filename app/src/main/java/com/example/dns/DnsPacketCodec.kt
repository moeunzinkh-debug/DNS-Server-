package com.example.dns

import java.io.ByteArrayOutputStream
import java.net.InetAddress

data class Ipv4UdpPacket(
  val srcIp: ByteArray,
  val dstIp: ByteArray,
  val srcPort: Int,
  val dstPort: Int,
  val payload: ByteArray,
)

data class DnsQueryInfo(
  val transactionId: Int,
  val domain: String,
  val qtype: Int,
  val recordTypeName: String,
)

data class DnsResponseSummary(
  val rcode: Int,
  val rcodeName: String,
  val answers: List<String>,
)

object DnsPacketCodec {

  private const val PROTOCOL_UDP = 17

  fun parseIpv4UdpPacket(packet: ByteArray, length: Int): Ipv4UdpPacket? {
    if (length < 28 || length > packet.size) return null
    val version = (packet[0].toInt() ushr 4) and 0x0F
    if (version != 4) return null

    val ihl = (packet[0].toInt() and 0x0F) * 4
    if (ihl < 20 || length < ihl + 8) return null

    val protocol = packet[9].toInt() and 0xFF
    if (protocol != PROTOCOL_UDP) return null

    val ipTotalLength = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
    val effectiveLength = minOf(length, ipTotalLength)
    if (effectiveLength < ihl + 8) return null

    val srcIp = packet.copyOfRange(12, 16)
    val dstIp = packet.copyOfRange(16, 20)

    val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
    val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
    val udpLength = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
    if (udpLength < 8) return null

    val payloadStart = ihl + 8
    val availablePayload = effectiveLength - payloadStart
    val payloadLen = minOf(availablePayload, udpLength - 8)
    if (payloadLen < 12) return null

    val payload = packet.copyOfRange(payloadStart, payloadStart + payloadLen)
    return Ipv4UdpPacket(
      srcIp = srcIp,
      dstIp = dstIp,
      srcPort = srcPort,
      dstPort = dstPort,
      payload = payload,
    )
  }

  fun buildIpv4UdpResponsePacket(
    srcIp: ByteArray,
    srcPort: Int,
    dstIp: ByteArray,
    dstPort: Int,
    dnsPayload: ByteArray,
    ipId: Int = 1,
  ): ByteArray {
    val ipHeaderLen = 20
    val udpHeaderLen = 8
    val udpLen = udpHeaderLen + dnsPayload.size
    val totalLen = ipHeaderLen + udpLen
    val out = ByteArray(totalLen)

    // IPv4 Header
    out[0] = 0x45.toByte()
    out[1] = 0x00
    writeShort(out, 2, totalLen)
    writeShort(out, 4, ipId and 0xFFFF)
    writeShort(out, 6, 0x4000) // Don't fragment
    out[8] = 64.toByte() // TTL
    out[9] = PROTOCOL_UDP.toByte()
    // Checksum placeholder at 10..11
    System.arraycopy(srcIp, 0, out, 12, 4)
    System.arraycopy(dstIp, 0, out, 16, 4)

    val ipChecksum = computeIpChecksum(out, 0, ipHeaderLen)
    writeShort(out, 10, ipChecksum)

    // UDP Header
    writeShort(out, ipHeaderLen, srcPort)
    writeShort(out, ipHeaderLen + 2, dstPort)
    writeShort(out, ipHeaderLen + 4, udpLen)
    // UDP payload
    System.arraycopy(dnsPayload, 0, out, ipHeaderLen + udpHeaderLen, dnsPayload.size)

    val udpChecksum = computeUdpChecksum(srcIp, dstIp, out, ipHeaderLen, udpLen)
    writeShort(out, ipHeaderLen + 6, udpChecksum)

    return out
  }

  fun buildQueryPacket(domain: String, queryType: Int = 1, transactionId: Int = 0x1234): ByteArray {
    val cleanDomain = domain.trim().trimEnd('.')
    val baos = ByteArrayOutputStream()
    // Header (12 bytes)
    baos.write((transactionId ushr 8) and 0xFF)
    baos.write(transactionId and 0xFF)
    // Flags: standard query, recursion desired (0x0100)
    baos.write(0x01)
    baos.write(0x00)
    // QDCOUNT = 1
    baos.write(0x00)
    baos.write(0x01)
    // ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0
    repeat(6) { baos.write(0x00) }

    if (cleanDomain.isNotEmpty()) {
      for (label in cleanDomain.split('.')) {
        val bytes = label.toByteArray(Charsets.UTF_8)
        val len = minOf(bytes.size, 63)
        baos.write(len)
        baos.write(bytes, 0, len)
      }
    }
    baos.write(0x00) // root terminator

    // QTYPE
    baos.write((queryType ushr 8) and 0xFF)
    baos.write(queryType and 0xFF)
    // QCLASS = IN (1)
    baos.write(0x00)
    baos.write(0x01)

    return baos.toByteArray()
  }

  fun parseQueryInfo(dnsPayload: ByteArray): DnsQueryInfo {
    if (dnsPayload.size < 12) {
      return DnsQueryInfo(0, "unknown", 0, "UNKNOWN")
    }
    val txId = ((dnsPayload[0].toInt() and 0xFF) shl 8) or (dnsPayload[1].toInt() and 0xFF)
    val qdCount = ((dnsPayload[4].toInt() and 0xFF) shl 8) or (dnsPayload[5].toInt() and 0xFF)
    if (qdCount < 1) {
      return DnsQueryInfo(txId, "unknown", 0, "NONE")
    }

    val (domain, nextOffset) = readDomainName(dnsPayload, 12)
    val qtype =
      if (nextOffset + 2 <= dnsPayload.size) {
        ((dnsPayload[nextOffset].toInt() and 0xFF) shl 8) or
          (dnsPayload[nextOffset + 1].toInt() and 0xFF)
      } else {
        1
      }

    return DnsQueryInfo(
      transactionId = txId,
      domain = domain.ifBlank { "<root>" },
      qtype = qtype,
      recordTypeName = qtypeToString(qtype),
    )
  }

  fun parseResponseSummary(dnsPayload: ByteArray): DnsResponseSummary {
    if (dnsPayload.size < 12) {
      return DnsResponseSummary(-1, "MALFORMED", emptyList())
    }
    val rcode = dnsPayload[3].toInt() and 0x0F
    val rcodeName = rcodeToString(rcode)
    val qdCount = ((dnsPayload[4].toInt() and 0xFF) shl 8) or (dnsPayload[5].toInt() and 0xFF)
    val anCount = ((dnsPayload[6].toInt() and 0xFF) shl 8) or (dnsPayload[7].toInt() and 0xFF)

    var offset = 12
    repeat(qdCount) {
      if (offset >= dnsPayload.size) return DnsResponseSummary(rcode, rcodeName, emptyList())
      val (_, afterName) = readDomainName(dnsPayload, offset)
      offset = afterName + 4 // skip QTYPE + QCLASS
    }

    val answers = mutableListOf<String>()
    repeat(anCount) {
      if (offset >= dnsPayload.size) return DnsResponseSummary(rcode, rcodeName, answers)
      val (_, afterName) = readDomainName(dnsPayload, offset)
      offset = afterName
      if (offset + 10 > dnsPayload.size) return DnsResponseSummary(rcode, rcodeName, answers)
      val type = ((dnsPayload[offset].toInt() and 0xFF) shl 8) or (dnsPayload[offset + 1].toInt() and 0xFF)
      val rdLength =
        ((dnsPayload[offset + 8].toInt() and 0xFF) shl 8) or
          (dnsPayload[offset + 9].toInt() and 0xFF)
      offset += 10
      if (offset + rdLength > dnsPayload.size) return DnsResponseSummary(rcode, rcodeName, answers)

      when (type) {
        1 -> if (rdLength == 4) {
          val ipBytes = dnsPayload.copyOfRange(offset, offset + 4)
          answers.add(InetAddress.getByAddress(ipBytes).hostAddress ?: "")
        }
        28 -> if (rdLength == 16) {
          val ipBytes = dnsPayload.copyOfRange(offset, offset + 16)
          answers.add(InetAddress.getByAddress(ipBytes).hostAddress ?: "")
        }
        5, 2, 12 -> {
          val (cname, _) = readDomainName(dnsPayload, offset)
          if (cname.isNotBlank()) answers.add(cname)
        }
        16 -> {
          if (rdLength > 1) {
            val txtLen = dnsPayload[offset].toInt() and 0xFF
            val safeLen = minOf(txtLen, rdLength - 1)
            answers.add(String(dnsPayload, offset + 1, safeLen, Charsets.UTF_8))
          }
        }
      }
      offset += rdLength
    }

    return DnsResponseSummary(rcode, rcodeName, answers.filter { it.isNotBlank() })
  }

  private fun readDomainName(data: ByteArray, startOffset: Int): Pair<String, Int> {
    val sb = StringBuilder()
    var offset = startOffset
    var jumped = false
    var nextOffsetAfterName = startOffset
    val visitedOffsets = HashSet<Int>()

    while (offset < data.size) {
      if (!visitedOffsets.add(offset)) {
        break
      }
      val len = data[offset].toInt() and 0xFF
      if (len == 0) {
        if (!jumped) nextOffsetAfterName = offset + 1
        break
      }
      if ((len and 0xC0) == 0xC0) {
        if (offset + 1 >= data.size) break
        val pointer = ((len and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
        if (!jumped) {
          nextOffsetAfterName = offset + 2
        }
        jumped = true
        offset = pointer
      } else {
        offset++
        if (offset + len > data.size) break
        if (sb.isNotEmpty()) sb.append('.')
        sb.append(String(data, offset, len, Charsets.UTF_8))
        offset += len
        if (!jumped) nextOffsetAfterName = offset
      }
    }

    return sb.toString() to nextOffsetAfterName
  }

  private fun qtypeToString(qtype: Int): String =
    when (qtype) {
      1 -> "A"
      2 -> "NS"
      5 -> "CNAME"
      6 -> "SOA"
      12 -> "PTR"
      15 -> "MX"
      16 -> "TXT"
      28 -> "AAAA"
      33 -> "SRV"
      64 -> "SVCB"
      65 -> "HTTPS"
      255 -> "ANY"
      else -> "TYPE$qtype"
    }

  private fun rcodeToString(rcode: Int): String =
    when (rcode) {
      0 -> "NOERROR"
      1 -> "FORMERR"
      2 -> "SERVFAIL"
      3 -> "NXDOMAIN"
      4 -> "NOTIMP"
      5 -> "REFUSED"
      else -> "RCODE$rcode"
    }

  private fun writeShort(buf: ByteArray, offset: Int, value: Int) {
    buf[offset] = ((value ushr 8) and 0xFF).toByte()
    buf[offset + 1] = (value and 0xFF).toByte()
  }

  private fun computeIpChecksum(buf: ByteArray, offset: Int, length: Int): Int {
    var sum = 0L
    var i = offset
    while (i < offset + length) {
      val high = buf[i].toInt() and 0xFF
      val low = if (i + 1 < offset + length) buf[i + 1].toInt() and 0xFF else 0
      sum += (high shl 8) or low
      i += 2
    }
    while ((sum ushr 16) != 0L) {
      sum = (sum and 0xFFFF) + (sum ushr 16)
    }
    return sum.inv().toInt() and 0xFFFF
  }

  private fun computeUdpChecksum(
    srcIp: ByteArray,
    dstIp: ByteArray,
    packet: ByteArray,
    udpOffset: Int,
    udpLength: Int,
  ): Int {
    var sum = 0L
    // Pseudo-header: src IP + dst IP + zero + proto (17) + udpLength
    sum += ((srcIp[0].toInt() and 0xFF) shl 8) or (srcIp[1].toInt() and 0xFF)
    sum += ((srcIp[2].toInt() and 0xFF) shl 8) or (srcIp[3].toInt() and 0xFF)
    sum += ((dstIp[0].toInt() and 0xFF) shl 8) or (dstIp[1].toInt() and 0xFF)
    sum += ((dstIp[2].toInt() and 0xFF) shl 8) or (dstIp[3].toInt() and 0xFF)
    sum += PROTOCOL_UDP.toLong()
    sum += udpLength.toLong()

    var i = udpOffset
    while (i < udpOffset + udpLength) {
      val high = packet[i].toInt() and 0xFF
      val low = if (i + 1 < udpOffset + udpLength) packet[i + 1].toInt() and 0xFF else 0
      sum += (high shl 8) or low
      i += 2
    }
    while ((sum ushr 16) != 0L) {
      sum = (sum and 0xFFFF) + (sum ushr 16)
    }
    val checksum = sum.inv().toInt() and 0xFFFF
    return if (checksum == 0) 0xFFFF else checksum
  }
}
