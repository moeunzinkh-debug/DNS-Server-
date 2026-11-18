package com.example.data

import com.example.model.DnsProvider

object DefaultDnsProviders {
  val builtInProviders: List<DnsProvider> =
    listOf(
      DnsProvider(
        id = "google",
        name = "Google",
        hostname = "dns.google",
        primaryIpv4 = "8.8.8.8",
        secondaryIpv4 = "8.8.4.4",
        primaryIpv6 = "2001:4860:4860::8888",
        secondaryIpv6 = "2001:4860:4860::8844",
        taglineKhmer = "លឿន និងស្ថេរភាពខ្ពស់បំផុត",
        taglineEnglish = "Fastest global anycast & highest stability",
        features = listOf("DNS-over-TLS", "DNSSEC", "Global Anycast"),
        accentColorHex = 0xFF3B82F6,
      ),
      DnsProvider(
        id = "quad9",
        name = "Quad9",
        hostname = "dns.quad9.net",
        primaryIpv4 = "9.9.9.9",
        secondaryIpv4 = "149.112.112.112",
        primaryIpv6 = "2620:fe::fe",
        secondaryIpv6 = "2620:fe::9",
        taglineKhmer = "លឿន + ការពារមេរោគល្អ",
        taglineEnglish = "Fast + strong malware & phishing blocking",
        features = listOf("Malware Block", "Zero Logging", "DNSSEC"),
        accentColorHex = 0xFF16A34A,
      ),
      DnsProvider(
        id = "cloudflare",
        name = "Cloudflare",
        hostname = "one.one.one.one",
        primaryIpv4 = "1.1.1.1",
        secondaryIpv4 = "1.0.0.1",
        primaryIpv6 = "2606:4700:4700::1111",
        secondaryIpv6 = "2606:4700:4700::1001",
        taglineKhmer = "លឿនខ្លាំង ឯកជនភាពល្អ",
        taglineEnglish = "Ultra-low latency & privacy-first resolver",
        features = listOf("Ultra Fast", "Privacy Audited", "DNSSEC"),
        accentColorHex = 0xFFDC2626,
      ),
      DnsProvider(
        id = "dns_sb",
        name = "DNS.SB",
        hostname = "dns.sb",
        primaryIpv4 = "185.222.222.222",
        secondaryIpv4 = "45.11.45.11",
        primaryIpv6 = "2a09::",
        secondaryIpv6 = "2a11::",
        taglineKhmer = "លឿនណាស់ និងឯកជនភាពខ្ពស់",
        taglineEnglish = "Very fast & strict no-logging privacy",
        features = listOf("No Logging", "DNS-over-TLS", "DNSSEC"),
        accentColorHex = 0xFF7C3AED,
      ),
      DnsProvider(
        id = "adguard",
        name = "AdGuard",
        hostname = "dns.adguard.com",
        primaryIpv4 = "94.140.14.14",
        secondaryIpv4 = "94.140.15.15",
        primaryIpv6 = "2a10:50c0::ad1:ff",
        secondaryIpv6 = "2a10:50c0::ad2:ff",
        taglineKhmer = "លឿន + បិទការផ្សាយពាណិជ្ជកម្មបាន",
        taglineEnglish = "Fast + blocks ads, trackers & analytics",
        features = listOf("Ad Blocking", "Tracker Block", "DNS-over-TLS"),
        accentColorHex = 0xFF06B6D4,
      ),
    )

  fun findById(id: String, customProviders: List<DnsProvider> = emptyList()): DnsProvider {
    return (builtInProviders + customProviders).firstOrNull { it.id == id }
      ?: builtInProviders.first()
  }
}
