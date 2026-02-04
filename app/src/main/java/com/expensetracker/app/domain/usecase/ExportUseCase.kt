package com.expensetracker.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) {

    suspend fun exportToExcel(context: Context, uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val expenses = expenseRepository.getAllExpensesSync()
            val categories = categoryRepository.getAllCategories().first()
            val categoriesMap = categories.associateBy { it.id }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                Workbook(outputStream, "ExpenseTracker", "1.0").use { workbook ->
                    val sheet = workbook.newWorksheet("Expenses")

                    // Header row
                    val headers = listOf("Date", "Type", "Category", "Subcategory", "Amount", "Note")
                    headers.forEachIndexed { col, header ->
                        sheet.value(0, col, header)
                        sheet.style(0, col).bold().set()
                    }

                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                    // Data rows
                    expenses.forEachIndexed { index, expense ->
                        val row = index + 1
                        val categoryName = expense.categoryId?.let { categoriesMap[it]?.name } ?: ""
                        val subcategoryName = expense.subcategoryId?.let { categoriesMap[it]?.name } ?: ""

                        sheet.value(row, 0, expense.date.format(dateFormatter))
                        sheet.value(row, 1, expense.type.name)
                        sheet.value(row, 2, categoryName)
                        sheet.value(row, 3, subcategoryName)
                        sheet.value(row, 4, expense.amount)
                        sheet.value(row, 5, expense.note)
                    }

                    // Set column widths
                    sheet.width(0, 12.0)  // Date
                    sheet.width(1, 10.0)  // Type
                    sheet.width(2, 15.0)  // Category
                    sheet.width(3, 15.0)  // Subcategory
                    sheet.width(4, 12.0)  // Amount
                    sheet.width(5, 30.0)  // Note
                }
            }
        }
    }

    suspend fun exportToPdf(context: Context, uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val expenses = expenseRepository.getAllExpensesSync()
            val categories = categoryRepository.getAllCategories().first()
            val categoriesMap = categories.associateBy { it.id }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)

                // Title
                document.add(
                    Paragraph("Expense Report")
                        .setFontSize(24f)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20f)
                )

                // Summary
                val totalIncome =
                    expenses.filter { it.type == com.expensetracker.app.data.local.entity.TransactionType.INCOME }
                        .sumOf { it.amount }
                val totalExpense =
                    expenses.filter { it.type == com.expensetracker.app.data.local.entity.TransactionType.EXPENSE }
                        .sumOf { it.amount }

                document.add(
                    Paragraph("Total Income: $${String.format("%.2f", totalIncome)}").setFontSize(
                        12f
                    )
                )
                document.add(
                    Paragraph("Total Expenses: $${String.format("%.2f", totalExpense)}").setFontSize(
                        12f
                    )
                )
                document.add(
                    Paragraph(
                        "Balance: $${
                            String.format(
                                "%.2f",
                                totalIncome - totalExpense
                            )
                        }"
                    ).setFontSize(12f).setMarginBottom(20f)
                )

                // Table with 6 columns now (added Subcategory)
                val table =
                    Table(UnitValue.createPercentArray(floatArrayOf(1.5f, 1f, 1.5f, 1.5f, 1f, 2.5f)))
                        .useAllAvailableWidth()

                // Header
                listOf("Date", "Type", "Category", "Subcategory", "Amount", "Note").forEach { header ->
                    table.addHeaderCell(
                        Cell().add(Paragraph(header).setBold())
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    )
                }

                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

                expenses.forEach { expense ->
                    val categoryName = expense.categoryId?.let { categoriesMap[it]?.name } ?: ""
                    val subcategoryName = expense.subcategoryId?.let { categoriesMap[it]?.name } ?: ""

                    table.addCell(expense.date.format(dateFormatter))
                    table.addCell(expense.type.name)
                    table.addCell(categoryName)
                    table.addCell(subcategoryName)
                    table.addCell("$${String.format("%.2f", expense.amount)}")
                    table.addCell(expense.note)
                }

                document.add(table)
                document.close()
            }
        }
    }
}
