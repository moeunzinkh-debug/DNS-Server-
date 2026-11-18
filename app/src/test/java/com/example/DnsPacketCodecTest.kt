package com.example

import com.example.data.DefaultDnsProviders
import com.example.dns.DnsPacketCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsPacketCodecTest {

  @Test
  fun `build and parse DNS query roundtrip`() {
    val query = DnsPacketCodec.buildQueryPacket("dns.adguard.com", queryType = 1, transactionId = 0x4321)
    val parsed = DnsPacketCodec.parseQueryInfo(query)
    assertEquals(0x4321, parsed.transactionId)
    assertEquals("dns.adguard.com", parsed.domain)
    assertEquals("A", parsed.recordTypeName)
  }

  @Test
  fun `build and parse IPv4 UDP DNS packet roundtrip`() {
    val srcIp = byteArrayOf(10, 8, 0, 1)
    val dstIp = byteArrayOf(10, 8, 0, 2)
    val dnsPayload = DnsPacketCodec.buildQueryPacket("one.one.one.one", queryType = 28, transactionId = 0x1122)

    val rawPacket =
      DnsPacketCodec.buildIpv4UdpResponsePacket(
        srcIp = srcIp,
        srcPort = 53,
        dstIp = dstIp,
        dstPort = 51234,
        dnsPayload = dnsPayload,
        ipId = 77,
      )

    val parsedPacket = DnsPacketCodec.parseIpv4UdpPacket(rawPacket, rawPacket.size)
    assertNotNull(parsedPacket)
    assertArrayEquals(srcIp, parsedPacket!!.srcIp)
    assertArrayEquals(dstIp, parsedPacket.dstIp)
    assertEquals(53, parsedPacket.srcPort)
    assertEquals(51234, parsedPacket.dstPort)
    assertArrayEquals(dnsPayload, parsedPacket.payload)

    val queryInfo = DnsPacketCodec.parseQueryInfo(parsedPacket.payload)
    assertEquals("one.one.one.one", queryInfo.domain)
    assertEquals("AAAA", queryInfo.recordTypeName)
  }

  @Test
  fun `built in providers contain all five requested resolvers`() {
    val hostnames = DefaultDnsProviders.builtInProviders.map { it.hostname }
    assertEquals(
      listOf("dns.google", "dns.quad9.net", "one.one.one.one", "dns.sb", "dns.adguard.com"),
      hostnames,
    )
  }
}
