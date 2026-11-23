package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.RoutesCatalog
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

fun buildPackingExcelFileName(
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
  now: Date = Date(),
): String {
  val routeSlug = when {
    snapshot.selectedRoute != null -> snapshot.selectedRoute.id
    selectedRouteId == RoutesCatalog.UNALLOCATED -> "Unassigned"
    else -> "All_Routes"
  }.replace(Regex("""[^A-Za-z0-9_-]"""), "_")
  val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(now)
  return "Homely_Packing_${routeSlug}_$dateTag.xlsx"
}

fun buildPackingExcelBytes(
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
  now: Date = Date(),
): ByteArray {
  val sharedStrings = SharedStringsTable()
  val sheet1Xml = buildSummaryAndShopsSheetXml(snapshot, selectedRouteId, now, sharedStrings)
  val sheet2Xml = buildFlatShopTableSheetXml(snapshot, sharedStrings)
  val sharedStringsXml = sharedStrings.buildXml()

  val out = ByteArrayOutputStream()
  ZipOutputStream(out).use { zip ->
    fun putXml(path: String, xml: String) {
      zip.putNextEntry(ZipEntry(path))
      zip.write(xml.toByteArray(Charsets.UTF_8))
      zip.closeEntry()
    }

    putXml("[Content_Types].xml", buildContentTypesXml())
    putXml("_rels/.rels", buildRootRelsXml())
    putXml("xl/workbook.xml", buildWorkbookXml())
    putXml("xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
    putXml("xl/styles.xml", buildStylesXml())
    putXml("xl/sharedStrings.xml", sharedStringsXml)
    putXml("xl/worksheets/sheet1.xml", sheet1Xml)
    putXml("xl/worksheets/sheet2.xml", sheet2Xml)
  }
  return out.toByteArray()
}

fun writePackingExcelToCache(
  context: Context,
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
  now: Date = Date(),
): Pair<File, Uri> {
  val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
  val file = File(exportDir, buildPackingExcelFileName(snapshot, selectedRouteId, now))
  file.writeBytes(buildPackingExcelBytes(snapshot, selectedRouteId, now))
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  return file to uri
}

fun exportPackingExcel(
  context: Context,
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
  preferOpenViewer: Boolean,
): Result<File> = runCatching {
  val (file, uri) = writePackingExcelToCache(context, snapshot, selectedRouteId)
  val title = "Homely by Homie Packing List (.xlsx)"

  if (preferOpenViewer) {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, XLSX_MIME_TYPE)
      clipData = ClipData.newRawUri(title, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      if (context !is Activity) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
    val viewers = context.packageManager.queryIntentActivities(
      viewIntent,
      PackageManager.MATCH_DEFAULT_ONLY,
    )
    if (viewers.isNotEmpty()) {
      viewers.forEach { info ->
        runCatching {
          context.grantUriPermission(
            info.activityInfo.packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
      }
      if (viewers.size == 1) {
        context.startActivity(viewIntent)
      } else {
        val chooser = Intent.createChooser(viewIntent, title).apply {
          clipData = ClipData.newRawUri(title, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        }
        context.startActivity(chooser)
      }
      return@runCatching file
    }
  }

  val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = XLSX_MIME_TYPE
    putExtra(Intent.EXTRA_STREAM, uri)
    putExtra(Intent.EXTRA_SUBJECT, title)
    putExtra(
      Intent.EXTRA_TEXT,
      "Homely by Homie Packing List (${snapshot.totalShopsCount} shops, ${snapshot.totalOrderedPacks} packets)",
    )
    clipData = ClipData.newRawUri(title, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  val chooser = Intent.createChooser(shareIntent, title).apply {
    clipData = ClipData.newRawUri(title, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (context !is Activity) {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }
  context.startActivity(chooser)
  file
}

private fun buildContentTypesXml(): String = buildString {
  append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
  append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n")
  append("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n")
  append("  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n")
  append("  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n")
  append("  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n")
  append("  <Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n")
  append("  <Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n")
  append("  <Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>\n")
  append("</Types>")
}

private fun buildRootRelsXml(): String = buildString {
  append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
  append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n")
  append("  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n")
  append("</Relationships>")
}

private fun buildWorkbookXml(): String = buildString {
  append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
  append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
  append("  <fileVersion appName=\"xl\" lastEdited=\"7\" lowestEdited=\"7\" rupBuild=\"27425\"/>\n")
  append("  <bookViews><workbookView xWindow=\"0\" yWindow=\"0\" windowWidth=\"24000\" windowHeight=\"15000\"/></bookViews>\n")
  append("  <sheets>\n")
  append("    <sheet name=\"Packing List\" sheetId=\"1\" r:id=\"rId1\"/>\n")
  append("    <sheet name=\"Shop Orders\" sheetId=\"2\" r:id=\"rId2\"/>\n")
  append("  </sheets>\n")
  append("  <calcPr calcId=\"191029\"/>\n")
  append("</workbook>")
}

private fun buildWorkbookRelsXml(): String = buildString {
  append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
  append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n")
  append("  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n")
  append("  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>\n")
  append("  <Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n")
  append("  <Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>\n")
  append("</Relationships>")
}

private fun buildStylesXml(): String = buildString {
  append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
  append("<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
  append("  <fonts count=\"5\">\n")
  append("    <font><sz val=\"11\"/><color rgb=\"FF1F2937\"/><name val=\"Calibri\"/></font>\n")
  append("    <font><b/><sz val=\"14\"/><color rgb=\"FF0D472A\"/><name val=\"Calibri\"/></font>\n")
  append("    <font><b/><sz val=\"11\"/><color rgb=\"FF1B5E20\"/><name val=\"Calibri\"/></font>\n")
  append("    <font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>\n")
  append("    <font><sz val=\"10\"/><color rgb=\"FF4B5563\"/><name val=\"Calibri\"/></font>\n")
  append("  </fonts>\n")
  append("  <fills count=\"5\">\n")
  append("    <fill><patternFill patternType=\"none\"/></fill>\n")
  append("    <fill><patternFill patternType=\"gray125\"/></fill>\n")
  append("    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8F5E9\"/></patternFill></fill>\n")
  append("    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1B5E20\"/></patternFill></fill>\n")
  append("    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF1F8E9\"/></patternFill></fill>\n")
  append("  </fills>\n")
  append("  <borders count=\"2\">\n")
  append("    <border><left/><right/><top/><bottom/><diagonal/></border>\n")
  append("    <border>\n")
  append("      <left style=\"thin\"><color rgb=\"FFD1D5DB\"/></left>\n")
  append("      <right style=\"thin\"><color rgb=\"FFD1D5DB\"/></right>\n")
  append("      <top style=\"thin\"><color rgb=\"FFD1D5DB\"/></top>\n")
  append("      <bottom style=\"thin\"><color rgb=\"FFD1D5DB\"/></bottom>\n")
  append("      <diagonal/>\n")
  append("    </border>\n")
  append("  </borders>\n")
  append("  <cellStyleXfs count=\"1\">\n")
  append("    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/>\n")
  append("  </cellStyleXfs>\n")
  append("  <cellXfs count=\"7\">\n")
  append("    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>\n")
  append("    <xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n")
  append("    <xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>\n")
  append("    <xf numFmtId=\"0\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>\n")
  append("    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>\n")
  append("    <xf numFmtId=\"0\" fontId=\"2\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>\n")
  append("    <xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>\n")
  append("  </cellXfs>\n")
  append("  <cellStyles count=\"1\">\n")
  append("    <cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/>\n")
  append("  </cellStyles>\n")
  append("</styleSheet>")
}

private class SharedStringsTable {
  private val strings = mutableListOf<String>()
  private val indexMap = LinkedHashMap<String, Int>()
  var totalReferences = 0
    private set

  fun intern(value: String): Int {
    totalReferences++
    return indexMap.getOrPut(value) {
      val idx = strings.size
      strings.add(value)
      idx
    }
  }

  fun buildXml(): String = buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
    append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
    append(totalReferences)
    append("\" uniqueCount=\"")
    append(strings.size)
    append("\">\n")
    strings.forEach { text ->
      append("  <si><t xml:space=\"preserve\">")
      append(escapeXml(text))
      append("</t></si>\n")
    }
    append("</sst>")
  }
}

private class SheetRowsBuilder(
  private val sharedStrings: SharedStringsTable,
  private val colCount: Int,
) {
  private val rows = StringBuilder()
  private val merges = mutableListOf<String>()
  var currentRow = 1
    private set

  fun addBlankRow() {
    rows.append("    <row r=\"").append(currentRow).append("\"/>\n")
    currentRow++
  }

  fun addMergedBannerRow(text: String, style: Int) {
    val r = currentRow
    val cells = (0 until colCount).map { idx ->
      SheetCell.Text(if (idx == 0) text else "", style = style)
    }
    addRow(cells)
    if (colCount > 1) {
      merges.add("A$r:${colLetter(colCount - 1)}$r")
    }
  }

  fun addRow(cells: List<SheetCell>) {
    val r = currentRow++
    rows.append("    <row r=\"").append(r).append("\">")
    cells.forEachIndexed { colIdx, cell ->
      val ref = "${colLetter(colIdx)}$r"
      when (cell) {
        is SheetCell.Text -> {
          val strIdx = sharedStrings.intern(cell.value)
          rows.append("<c r=\"").append(ref).append("\" t=\"s\" s=\"").append(cell.style)
            .append("\"><v>").append(strIdx).append("</v></c>")
        }
        is SheetCell.Number -> {
          rows.append("<c r=\"").append(ref).append("\" s=\"").append(cell.style).append("\"><v>")
            .append(cell.value)
            .append("</v></c>")
        }
        is SheetCell.FormulaNumber -> {
          rows.append("<c r=\"").append(ref).append("\" s=\"").append(cell.style).append("\"><f>")
            .append(escapeXml(cell.formula))
            .append("</f><v>").append(cell.cachedValue).append("</v></c>")
        }
      }
    }
    rows.append("</row>\n")
  }

  fun dimensionRef(): String {
    val lastRow = (currentRow - 1).coerceAtLeast(1)
    return "A1:${colLetter(colCount - 1)}$lastRow"
  }

  fun buildRowsXml(): String = rows.toString()

  fun buildMergeCellsXml(): String {
    if (merges.isEmpty()) return ""
    return buildString {
      append("  <mergeCells count=\"").append(merges.size).append("\">\n")
      merges.forEach { ref ->
        append("    <mergeCell ref=\"").append(ref).append("\"/>\n")
      }
      append("  </mergeCells>\n")
    }
  }

  private fun colLetter(index: Int): String {
    var n = index
    val sb = StringBuilder()
    while (n >= 0) {
      sb.append(('A' + (n % 26)))
      n = (n / 26) - 1
    }
    return sb.reverse().toString()
  }
}

private sealed class SheetCell(val style: Int) {
  class Text(val value: String, style: Int = 4) : SheetCell(style)
  class Number(val value: Long, style: Int = 4) : SheetCell(style)
  class FormulaNumber(val formula: String, val cachedValue: Long, style: Int = 5) : SheetCell(style)
}

private fun buildSummaryAndShopsSheetXml(
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
  now: Date,
  sharedStrings: SharedStringsTable,
): String {
  val b = SheetRowsBuilder(sharedStrings = sharedStrings, colCount = 5)
  val generatedAt = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US).format(now)
  val routeLabel = when {
    snapshot.selectedRoute != null -> "${snapshot.selectedRoute.name} (${snapshot.selectedRoute.id})"
    selectedRouteId == RoutesCatalog.UNALLOCATED -> "Unassigned Queue"
    else -> "All Active Routes"
  }

  // Title
  b.addMergedBannerRow("HOMELY BY HOMIE — VEHICLE PACKING LIST", style = 1)
  b.addMergedBannerRow("Generated: $generatedAt  |  Route: $routeLabel", style = 6)
  b.addBlankRow()

  // Section 1: Vehicle Details
  b.addMergedBannerRow("VEHICLE DETAILS", style = 2)
  val r = snapshot.selectedRoute
  b.addRow(
    listOf(
      SheetCell.Text("Vehicle", style = 5),
      SheetCell.Text(r?.vehicle ?: "Combined Dispatch Fleet", style = 4),
      SheetCell.Text("Driver", style = 5),
      SheetCell.Text(r?.driverName ?: "Fleet Team", style = 4),
      SheetCell.Text(r?.driverPhone.orEmpty(), style = 4),
    ),
  )
  b.addRow(
    listOf(
      SheetCell.Text("Route", style = 5),
      SheetCell.Text(routeLabel, style = 4),
      SheetCell.Text("Staging Bay", style = 5),
      SheetCell.Text(r?.stagingBay ?: "Central Warehouse Hub", style = 4),
      SheetCell.Text(r?.dispatchWindow ?: "Same-day", style = 4),
    ),
  )
  b.addRow(
    listOf(
      SheetCell.Text("Total Shops", style = 5),
      SheetCell.Number(snapshot.totalShopsCount.toLong(), style = 4),
      SheetCell.Text("Total Packets", style = 5),
      SheetCell.Number(snapshot.totalOrderedPacks.toLong(), style = 4),
      SheetCell.Text(String.format(Locale.US, "Est. Weight: %.1f kg", snapshot.payloadWeightKg), style = 4),
    ),
  )
  b.addBlankRow()

  // Section 2: Overall Count of Different Packets
  b.addMergedBannerRow("OVERALL PACKET COUNT", style = 2)
  if (snapshot.productionBySize.isNotEmpty()) {
    b.addRow(
      listOf(
        SheetCell.Text("Packet Size", style = 3),
        SheetCell.Text("Variant Count", style = 3),
        SheetCell.Text("Total Packets", style = 3),
        SheetCell.Text("Unit", style = 3),
        SheetCell.Text("", style = 3),
      ),
    )
    snapshot.productionBySize.forEach { target ->
      b.addRow(
        listOf(
          SheetCell.Text("${target.packSize} Packs", style = 4),
          SheetCell.Number(target.skuCount.toLong(), style = 4),
          SheetCell.Number(target.orderedPacks.toLong(), style = 4),
          SheetCell.Text("pk", style = 4),
          SheetCell.Text("", style = 4),
        ),
      )
    }
    b.addBlankRow()
  }

  val allPacketLines = snapshot.categoryGroups.flatMap { it.items }
  b.addRow(
    listOf(
      SheetCell.Text("Snack / Packet Name", style = 3),
      SheetCell.Text("Pack Size", style = 3),
      SheetCell.Text("Total Quantity", style = 3),
      SheetCell.Text("SKU Code", style = 3),
      SheetCell.Text("Shop Drops", style = 3),
    ),
  )
  if (allPacketLines.isEmpty()) {
    b.addMergedBannerRow("No packets queued for this selection", style = 4)
  } else {
    val firstSkuRow = b.currentRow
    allPacketLines.forEach { sku ->
      b.addRow(
        listOf(
          SheetCell.Text(sku.productName, style = 4),
          SheetCell.Text(sku.packSize, style = 4),
          SheetCell.Number(sku.orderedPacks.toLong(), style = 4),
          SheetCell.Text(sku.productCode, style = 4),
          SheetCell.Number(sku.allocatedDropCount.toLong(), style = 4),
        ),
      )
    }
    val lastSkuRow = b.currentRow - 1
    b.addRow(
      listOf(
        SheetCell.Text("TOTAL PACKETS", style = 5),
        SheetCell.Text("", style = 5),
        SheetCell.FormulaNumber(
          formula = "SUM(C$firstSkuRow:C$lastSkuRow)",
          cachedValue = allPacketLines.sumOf { it.orderedPacks }.toLong(),
          style = 5,
        ),
        SheetCell.Text("pk", style = 5),
        SheetCell.Text("", style = 5),
      ),
    )
  }
  b.addBlankRow()

  // Section 3: Shop-by-Shop Order Details
  b.addMergedBannerRow("SHOP ORDER DETAILS (${snapshot.shopSlips.size} SHOPS)", style = 2)
  b.addRow(
    listOf(
      SheetCell.Text("Shop / Packet Name", style = 3),
      SheetCell.Text("Code / Size", style = 3),
      SheetCell.Text("Packets Qty", style = 3),
      SheetCell.Text("Stop & Order #", style = 3),
      SheetCell.Text("Status / Note", style = 3),
    ),
  )
  if (snapshot.shopSlips.isEmpty()) {
    b.addMergedBannerRow("No shop orders in this route filter", style = 4)
  } else {
    snapshot.shopSlips.forEachIndexed { shopIndex, slip ->
      val shopQty = slip.items.sumOf { it.quantity }.takeIf { it > 0 } ?: slip.order.itemCount
      val stopLabel = if (slip.deliveryStopNumber > 0) "Stop #${slip.deliveryStopNumber}" else "Unassigned"
      b.addRow(
        listOf(
          SheetCell.Text("${shopIndex + 1}. ${slip.order.shopName}", style = 5),
          SheetCell.Text(slip.order.shopCode, style = 5),
          SheetCell.Number(shopQty.toLong(), style = 5),
          SheetCell.Text("$stopLabel • ${slip.order.orderCode}", style = 5),
          SheetCell.Text(if (slip.isPacked) "Packed" else "Pending", style = 5),
        ),
      )
      if (slip.items.isNotEmpty()) {
        slip.items.forEachIndexed { idx, line ->
          b.addRow(
            listOf(
              SheetCell.Text("   • ${line.productName}", style = 4),
              SheetCell.Text(line.packSize, style = 4),
              SheetCell.Number(line.quantity.toLong(), style = 4),
              SheetCell.Text(line.productCode, style = 4),
              SheetCell.Text(if (idx == 0) slip.order.note else "", style = 4),
            ),
          )
        }
      } else {
        b.addRow(
          listOf(
            SheetCell.Text("   • ${slip.order.itemsSummary}", style = 4),
            SheetCell.Text("Standard", style = 4),
            SheetCell.Number(slip.order.itemCount.toLong(), style = 4),
            SheetCell.Text(slip.order.orderCode, style = 4),
            SheetCell.Text(slip.order.note, style = 4),
          ),
        )
      }
    }
  }

  return buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
    append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
    append("  <sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>\n")
    append("  <dimension ref=\"").append(b.dimensionRef()).append("\"/>\n")
    append("  <sheetViews><sheetView tabSelected=\"1\" workbookViewId=\"0\"/></sheetViews>\n")
    append("  <sheetFormatPr defaultRowHeight=\"15\"/>\n")
    append("  <cols>\n")
    append("    <col min=\"1\" max=\"1\" width=\"32\" customWidth=\"1\"/>\n")
    append("    <col min=\"2\" max=\"2\" width=\"18\" customWidth=\"1\"/>\n")
    append("    <col min=\"3\" max=\"3\" width=\"15\" customWidth=\"1\"/>\n")
    append("    <col min=\"4\" max=\"4\" width=\"24\" customWidth=\"1\"/>\n")
    append("    <col min=\"5\" max=\"5\" width=\"24\" customWidth=\"1\"/>\n")
    append("  </cols>\n")
    append("  <sheetData>\n")
    append(b.buildRowsXml())
    append("  </sheetData>\n")
    append(b.buildMergeCellsXml())
    append("  <pageMargins left=\"0.7\" right=\"0.7\" top=\"0.75\" bottom=\"0.75\" header=\"0.3\" footer=\"0.3\"/>\n")
    append("  <pageSetup orientation=\"portrait\" paperSize=\"9\" fitToWidth=\"1\" fitToHeight=\"0\"/>\n")
    append("</worksheet>")
  }
}

private fun buildFlatShopTableSheetXml(
  snapshot: PackingDashboardSnapshot,
  sharedStrings: SharedStringsTable,
): String {
  val b = SheetRowsBuilder(sharedStrings = sharedStrings, colCount = 9)
  b.addRow(
    listOf(
      SheetCell.Text("Stop #", style = 3),
      SheetCell.Text("Shop Name", style = 3),
      SheetCell.Text("Shop Code", style = 3),
      SheetCell.Text("Order #", style = 3),
      SheetCell.Text("Route", style = 3),
      SheetCell.Text("Packet Name", style = 3),
      SheetCell.Text("Pack Size", style = 3),
      SheetCell.Text("Quantity (pk)", style = 3),
      SheetCell.Text("Status", style = 3),
    ),
  )
  snapshot.shopSlips.forEach { slip ->
    val stopNum = if (slip.deliveryStopNumber > 0) slip.deliveryStopNumber.toLong() else slip.packOrderNumber.toLong()
    val routeName = slip.route?.name ?: "Unassigned"
    val status = if (slip.isPacked) "Packed" else "Pending"
    if (slip.items.isNotEmpty()) {
      slip.items.forEach { line ->
        b.addRow(
          listOf(
            SheetCell.Number(stopNum, style = 4),
            SheetCell.Text(slip.order.shopName, style = 4),
            SheetCell.Text(slip.order.shopCode, style = 4),
            SheetCell.Text(slip.order.orderCode, style = 4),
            SheetCell.Text(routeName, style = 4),
            SheetCell.Text(line.productName, style = 4),
            SheetCell.Text(line.packSize, style = 4),
            SheetCell.Number(line.quantity.toLong(), style = 4),
            SheetCell.Text(status, style = 4),
          ),
        )
      }
    } else {
      b.addRow(
        listOf(
          SheetCell.Number(stopNum, style = 4),
          SheetCell.Text(slip.order.shopName, style = 4),
          SheetCell.Text(slip.order.shopCode, style = 4),
          SheetCell.Text(slip.order.orderCode, style = 4),
          SheetCell.Text(routeName, style = 4),
          SheetCell.Text(slip.order.itemsSummary, style = 4),
          SheetCell.Text("Standard", style = 4),
          SheetCell.Number(slip.order.itemCount.toLong(), style = 4),
          SheetCell.Text(status, style = 4),
        ),
      )
    }
  }

  return buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
    append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
    append("  <dimension ref=\"").append(b.dimensionRef()).append("\"/>\n")
    append("  <sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>\n")
    append("  <sheetFormatPr defaultRowHeight=\"15\"/>\n")
    append("  <cols>\n")
    append("    <col min=\"1\" max=\"1\" width=\"9\" customWidth=\"1\"/>\n")
    append("    <col min=\"2\" max=\"2\" width=\"26\" customWidth=\"1\"/>\n")
    append("    <col min=\"3\" max=\"3\" width=\"13\" customWidth=\"1\"/>\n")
    append("    <col min=\"4\" max=\"4\" width=\"16\" customWidth=\"1\"/>\n")
    append("    <col min=\"5\" max=\"5\" width=\"24\" customWidth=\"1\"/>\n")
    append("    <col min=\"6\" max=\"6\" width=\"24\" customWidth=\"1\"/>\n")
    append("    <col min=\"7\" max=\"7\" width=\"11\" customWidth=\"1\"/>\n")
    append("    <col min=\"8\" max=\"8\" width=\"14\" customWidth=\"1\"/>\n")
    append("    <col min=\"9\" max=\"9\" width=\"12\" customWidth=\"1\"/>\n")
    append("  </cols>\n")
    append("  <sheetData>\n")
    append(b.buildRowsXml())
    append("  </sheetData>\n")
    append("  <pageMargins left=\"0.7\" right=\"0.7\" top=\"0.75\" bottom=\"0.75\" header=\"0.3\" footer=\"0.3\"/>\n")
    append("</worksheet>")
  }
}

private fun escapeXml(input: String): String {
  if (input.isEmpty()) return ""
  val sb = StringBuilder(input.length)
  for (ch in input) {
    when (ch) {
      '&' -> sb.append("&amp;")
      '<' -> sb.append("&lt;")
      '>' -> sb.append("&gt;")
      '"' -> sb.append("&quot;")
      '\'' -> sb.append("&apos;")
      else -> if (ch >= ' ' || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch)
    }
  }
  return sb.toString()
}
