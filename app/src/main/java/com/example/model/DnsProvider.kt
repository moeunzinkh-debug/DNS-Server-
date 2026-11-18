package com.example.model

enum class ThemeMode {
  DARK,
  LIGHT,
  SYSTEM,
}

enum class LanguageMode {
  BILINGUAL,
  KHMER,
  ENGLISH,
}

data class DnsProvider(
  val id: String,
  val name: String,
  val hostname: String,
  val primaryIpv4: String,
  val secondaryIpv4: String = "",
  val primaryIpv6: String = "",
  val secondaryIpv6: String = "",
  val taglineKhmer: String,
  val taglineEnglish: String,
  val features: List<String>,
  val accentColorHex: Long,
  val isCustom: Boolean = false,
)
