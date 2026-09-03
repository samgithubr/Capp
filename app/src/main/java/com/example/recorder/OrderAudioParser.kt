package com.example.recorder

import java.util.Locale

data class ParsedOrderItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double
)

data class ParsedOrderResult(
    val items: List<ParsedOrderItem>,
    val subtotal: Double,
    val tax: Double,
    val suggestedTip: Double,
    val total: Double,
    val summaryText: String
)

object OrderAudioParser {

    private val beverageCatalog = mapOf(
        "latte" to 5.50,
        "iced latte" to 5.75,
        "vanilla latte" to 6.00,
        "oat latte" to 6.25,
        "cold brew" to 5.00,
        "nitro cold brew" to 5.75,
        "cappuccino" to 5.25,
        "flat white" to 5.50,
        "americano" to 4.25,
        "iced americano" to 4.50,
        "espresso" to 3.75,
        "double espresso" to 4.50,
        "macchiato" to 4.75,
        "caramel macchiato" to 6.25,
        "mocha" to 6.00,
        "drip coffee" to 3.50,
        "pour over" to 5.50,
        "chai latte" to 5.50,
        "matcha latte" to 6.25,
        "croissant" to 4.50,
        "almond croissant" to 5.25,
        "chocolate croissant" to 5.00,
        "bagel" to 3.75,
        "muffin" to 4.00,
        "scone" to 4.25,
        "breakfast sandwich" to 8.50,
        "bacon sandwich" to 8.75
    )

    fun parseOrderText(inputText: String): ParsedOrderResult {
        val lower = inputText.lowercase(Locale.US)
        val itemsFound = mutableListOf<ParsedOrderItem>()

        // Look for keywords and numbers
        val words = lower.split(Regex("[,\\.\\s]+"))

        // Common number mappings
        val numberMap = mapOf(
            "a" to 1, "an" to 1, "one" to 1, "1" to 1,
            "two" to 2, "2" to 2, "three" to 3, "3" to 3,
            "four" to 4, "4" to 4, "five" to 5, "5" to 5
        )

        for ((bevName, basePrice) in beverageCatalog) {
            if (lower.contains(bevName)) {
                // Find quantity before this item
                val regex = Regex("(\\d+|one|two|three|four|five|a|an)\\s*(?:x|times)?\\s*(?:small|medium|large|tall|grande|venti|iced|hot)?\\s*${Regex.escape(bevName)}")
                val match = regex.find(lower)
                val quantity = if (match != null) {
                    val numWord = match.groupValues[1]
                    numberMap[numWord] ?: 1
                } else {
                    1
                }

                // Check if already covered by longer item name (e.g. vanilla latte vs latte)
                val isAlreadyCovered = itemsFound.any { it.name.contains(bevName, ignoreCase = true) }
                if (!isAlreadyCovered) {
                    val formattedName = bevName.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    itemsFound.add(ParsedOrderItem(name = formattedName, quantity = quantity, unitPrice = basePrice))
                }
            }
        }

        // If nothing matched, fallback to a standard Coffee Order
        if (itemsFound.isEmpty()) {
            itemsFound.add(
                ParsedOrderItem(
                    name = if (inputText.isNotBlank()) inputText.take(30) else "Craft Coffee Order",
                    quantity = 1,
                    unitPrice = 5.50
                )
            )
        }

        val subtotal = itemsFound.sumOf { it.quantity * it.unitPrice }
        val tax = Math.round(subtotal * 0.085 * 100.0) / 100.0 // 8.5% standard tax
        val tip = if (subtotal >= 15.0) 3.00 else 2.00
        val total = Math.round((subtotal + tax + tip) * 100.0) / 100.0

        val summary = itemsFound.joinToString(", ") { "${it.quantity}x ${it.name}" }

        return ParsedOrderResult(
            items = itemsFound,
            subtotal = subtotal,
            tax = tax,
            suggestedTip = tip,
            total = total,
            summaryText = summary
        )
    }

    val sampleVoiceTranscripts = listOf(
        "Hi, I'd like to place an order for 2 large Oat Lattes and an Almond Croissant for pickup in 15 minutes please.",
        "Could I order 1 Iced Caramel Macchiato and 1 Bacon Sandwich ready under Somnath?",
        "Good morning, I need 2 Cold Brew coffees and a Chocolate Croissant please.",
        "Can I order 1 Flat White, 1 Double Espresso, and a Blueberry Scone?",
        "Placing a phone order for 2 Cappuccinos and 1 Bagel with cream cheese."
    )
}
