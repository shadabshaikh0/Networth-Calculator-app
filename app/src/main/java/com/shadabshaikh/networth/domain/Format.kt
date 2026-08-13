package com.shadabshaikh.networth.domain

import java.util.Locale
import kotlin.math.abs

/**
 * Group digits the Indian way — last three, then twos: 1,23,45,678.
 * Implemented explicitly (not via Locale) so it's identical on every device,
 * independent of the JVM's bundled locale data.
 */
private fun grouped(n: Long): String {
    val sign = if (n < 0) "-" else ""
    val digits = abs(n).toString()
    if (digits.length <= 3) return sign + digits
    val last3 = digits.substring(digits.length - 3)
    val rest = digits.substring(0, digits.length - 3)
    val head = StringBuilder()
    var i = rest.length
    while (i > 0) {
        val start = maxOf(0, i - 2)
        if (head.isNotEmpty()) head.insert(0, ",")
        head.insert(0, rest.substring(start, i))
        i = start
    }
    return "$sign$head,$last3"
}

/** Full rupee amount, e.g. "₹12,34,567". Mirrors the web's `inr`. */
fun inr(n: Long): String = "₹" + grouped(n)

/** 2 decimals, then trailing zeros (and a bare trailing dot) stripped: 1.50→"1.5", 2.00→"2". */
private fun trimDecimals(v: Double): String =
    String.format(Locale.US, "%.2f", v).replace(Regex("\\.?0+$"), "")

/** Compact rupee amount: "₹1.2 Cr" (≥1e7), "₹3.4 L" (≥1e5), else grouped. Mirrors `compact`. */
fun compact(n: Long): String {
    val a = abs(n)
    val sign = if (n < 0) "-" else ""
    return when {
        a >= 10_000_000L -> sign + "₹" + trimDecimals(a / 1e7) + " Cr"
        a >= 100_000L -> sign + "₹" + trimDecimals(a / 1e5) + " L"
        else -> sign + "₹" + grouped(a)
    }
}
