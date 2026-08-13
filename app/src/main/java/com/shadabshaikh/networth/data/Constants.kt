package com.shadabshaikh.networth.data

import com.shadabshaikh.networth.model.CategoryDef
import com.shadabshaikh.networth.model.Item
import com.shadabshaikh.networth.model.Member

/**
 * App-wide constants ported verbatim from the web app's `src/constants.ts`.
 * Kept free of Android/Compose imports so it stays pure and unit-testable.
 * Category colors are hex strings; the theme palette lives in ui/theme.
 */

val ASSET_CATS: List<CategoryDef> = listOf(
    CategoryDef("bonds", "Bonds & fixed income", "#D5B475", "M5 4h13v16l-3-2-3 2-3-2-1 1zM8 8h7M8 12h5"),
    CategoryDef("stocks", "Stocks & mutual funds", "#417CF1", "M4 16l4-5 3 3 5-8 4 5M4 20h16"),
    CategoryDef("cash", "Cash & bank", "#19AA4D", "M3 8h18v10H3zM3 8l2-3h14l2 3M15 13h3"),
    CategoryDef("epf", "EPF / PPF / retirement", "#6FCAFF", "M12 3l7 3v5c0 4-3 7-7 8-4-1-7-4-7-8V6z"),
    CategoryDef("vehicles", "Vehicles", "#FC823A", "M4 13l2-5h12l2 5M3 13h18v4H3zM7 17v2M17 17v2"),
    CategoryDef("gold", "Gold", "#FBC450", "M4 14h6v5H4zM14 14h6v5h-6zM9 8h6v5H9z"),
    CategoryDef("realestate", "Real estate", "#A964F7", "M4 11l8-6 8 6M6 10v9h12v-9M10 19v-5h4v5"),
    CategoryDef("other_a", "Other assets", "#B1B1B1", "M5 5h6v6H5zM13 5h6v6h-6zM5 13h6v6H5zM13 13h6v6h-6z"),
)

val LIAB_CATS: List<CategoryDef> = listOf(
    CategoryDef("home", "Home loan", "#D8645D", "M4 11l8-6 8 6M6 10v9h12v-9M10 19v-5h4v5"),
    CategoryDef("carloan", "Car / vehicle loan", "#FE817B", "M4 13l2-5h12l2 5M3 13h18v4H3zM7 17v2M17 17v2"),
    CategoryDef("personal", "Personal loan", "#F877D2", "M12 12a4 4 0 100-8 4 4 0 000 8zM5 20c0-4 3-6 7-6s7 2 7 6"),
    CategoryDef("creditcard", "Credit card debt", "#E142BC", "M3 6h18v12H3zM3 10h18M7 15h4"),
    CategoryDef("education", "Education loan", "#FFB48C", "M3 9l9-4 9 4-9 4zM7 11v5c0 2 10 2 10 0v-5"),
    CategoryDef("other_l", "Other liabilities", "#979797", "M5 5h6v6H5zM13 5h6v6h-6zM5 13h6v6H5zM13 13h6v6h-6z"),
)

val DEFAULT_MEMBERS: List<Member> = listOf(
    Member("self", "You", "Self", "#D5B475"),
    Member("spouse", "Priya", "Spouse", "#6FCAFF"),
    Member("parent", "Dad", "Father", "#A964F7"),
)

val MEMBER_COLORS: List<String> =
    listOf("#D5B475", "#6FCAFF", "#A964F7", "#FC823A", "#8BF1A7", "#F877D2", "#FBC450", "#5EE0C0")

val RELATIONS: List<String> =
    listOf("Spouse", "Father", "Mother", "Son", "Daughter", "Sibling", "Partner", "HUF", "Other")

/** Asset categories reachable quickly in an emergency (everything else is "locked":
 *  epf, realestate, vehicles, other_a). */
val LIQUID_CATS: Set<String> = setOf("cash", "stocks", "gold", "bonds")

/** ₹ per gram (indicative defaults). */
val DEFAULT_RATES: Map<String, Long> = mapOf("gold" to 7250L, "silver" to 92L)

val SEED_ASSETS: List<Item> = listOf(
    Item("a1", "Bond fund SIP", "bonds", 250000L, "self"),
    Item("a2", "Zerodha equity", "stocks", 680000L, "self"),
    Item("a3", "Parag Parikh Flexi", "stocks", 320000L, "self"),
    Item("a4", "HDFC savings", "cash", 145000L, "self"),
    Item("a5", "EPF balance", "epf", 410000L, "self"),
    Item("a6", "Honda City", "vehicles", 550000L, "self"),
    Item("a7", "Sovereign Gold Bond", "gold", 180000L, "spouse"),
    Item("a8", "2BHK, Pune", "realestate", 8500000L, "self"),
    Item("a9", "SBI mutual funds", "stocks", 540000L, "spouse"),
    Item("a10", "PPF account", "epf", 820000L, "spouse"),
    Item("a11", "Ancestral land", "realestate", 6200000L, "parent"),
    Item("a12", "Gold jewellery", "gold", 950000L, "parent"),
)

val SEED_LIAB: List<Item> = listOf(
    Item("l1", "HDFC home loan", "home", 5200000L, "self"),
    Item("l2", "Car loan", "carloan", 320000L, "self"),
    Item("l3", "HDFC Regalia", "creditcard", 45000L, "self"),
    Item("l4", "Personal loan", "personal", 180000L, "spouse"),
)
