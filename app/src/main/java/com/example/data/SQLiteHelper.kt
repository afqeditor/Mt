package com.example.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

object SQLiteHelper {

    data class TableInfo(
        val name: String,
        val columns: List<String>
    )

    data class RowData(
        val rowid: Long?,
        val values: Map<String, String>
    )

    suspend fun getTables(dbFile: File): List<TableInfo> = withContext(Dispatchers.IO) {
        var db: SQLiteDatabase? = null
        val tables = mutableListOf<TableInfo>()
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null)
            cursor.use {
                while (it.moveToNext()) {
                    val tableName = it.getString(0)
                    tables.add(TableInfo(tableName, getTableColumns(db, tableName)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        tables
    }

    private fun getTableColumns(db: SQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("PRAGMA table_info(\"$tableName\")", null)
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (nameIndex != -1) {
                        columns.add(it.getString(nameIndex))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return columns
    }

    suspend fun getTableData(dbFile: File, tableName: String): Pair<List<String>, List<RowData>> = withContext(Dispatchers.IO) {
        var db: SQLiteDatabase? = null
        val rows = mutableListOf<RowData>()
        var columns = listOf<String>()
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            columns = getTableColumns(db, tableName)
            
            // Try selecting rowid to easily support editing later
            val hasRowid = try {
                db.rawQuery("SELECT rowid FROM \"$tableName\" LIMIT 1", null).use { true }
            } catch (e: Exception) {
                false
            }

            val query = if (hasRowid) {
                "SELECT rowid, * FROM \"$tableName\" LIMIT 200"
            } else {
                "SELECT * FROM \"$tableName\" LIMIT 200"
            }

            val cursor = db.rawQuery(query, null)
            cursor.use { c ->
                val columnNames = c.columnNames.toList()
                val isRowidIncluded = hasRowid && columnNames.firstOrNull()?.equals("rowid", ignoreCase = true) == true
                
                while (c.moveToNext()) {
                    val rowValues = mutableMapOf<String, String>()
                    var rowidVal: Long? = null
                    
                    if (isRowidIncluded) {
                        rowidVal = c.getLong(0)
                    }

                    val startIdx = if (isRowidIncluded) 1 else 0
                    for (i in startIdx until c.columnCount) {
                        val colName = columnNames[i]
                        val value = try {
                            when (c.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> "[NULL]"
                                Cursor.FIELD_TYPE_BLOB -> "[BLOB - ${c.getBlob(i).size} bytes]"
                                else -> c.getString(i) ?: ""
                            }
                        } catch (e: Exception) {
                            "[ERROR]"
                        }
                        rowValues[colName] = value
                    }
                    rows.add(RowData(rowidVal, rowValues))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        Pair(columns, rows)
    }

    suspend fun updateCell(
        dbFile: File,
        tableName: String,
        columnName: String,
        newValue: String,
        rowid: Long?,
        fallbackConditions: Map<String, String>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            if (rowid != null) {
                val stmt = db.compileStatement("UPDATE \"$tableName\" SET \"$columnName\" = ? WHERE rowid = ?")
                stmt.bindString(1, newValue)
                stmt.bindLong(2, rowid)
                stmt.executeUpdateDelete()
                true
            } else if (!fallbackConditions.isNullOrEmpty()) {
                val whereClause = StringBuilder()
                val args = mutableListOf<String>()
                args.add(newValue)
                
                fallbackConditions.forEach { (col, valStr) ->
                    if (whereClause.isNotEmpty()) whereClause.append(" AND ")
                    whereClause.append("\"$col\" = ?")
                    args.add(valStr)
                }
                
                db.execSQL("UPDATE \"$tableName\" SET \"$columnName\" = ? WHERE $whereClause", args.toTypedArray())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.close()
        }
    }

    suspend fun exportTableToCSV(dbFile: File, tableName: String, exportDir: File): File? = withContext(Dispatchers.IO) {
        val csvFile = File(exportDir, "${dbFile.nameWithoutExtension}_${tableName}.csv")
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val columns = getTableColumns(db, tableName)
            val cursor = db.rawQuery("SELECT * FROM \"$tableName\"", null)
            
            cursor.use { c ->
                FileWriter(csvFile).use { writer ->
                    // Write header
                    writer.write(columns.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    
                    while (c.moveToNext()) {
                        val row = mutableListOf<String>()
                        for (i in 0 until c.columnCount) {
                            val value = try {
                                when (c.getType(i)) {
                                    Cursor.FIELD_TYPE_NULL -> ""
                                    Cursor.FIELD_TYPE_BLOB -> "[BLOB]"
                                    else -> c.getString(i) ?: ""
                                }
                            } catch (e: Exception) {
                                ""
                            }
                            row.add("\"${value.replace("\"", "\"\"")}\"")
                        }
                        writer.write(row.joinToString(",") + "\n")
                    }
                }
            }
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            db?.close()
        }
    }
}
