package com.jakedlr.healthtracker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.jakedlr.healthtracker.data.db.HEALTH_DB_VERSION
import com.jakedlr.healthtracker.data.db.HealthDatabase
import com.jakedlr.healthtracker.domain.isSqliteHeader
import com.jakedlr.healthtracker.domain.sleepDate
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns the live database so [BackupRepository] can close it, swap its file, and open it again. */
interface DatabaseHolder {
    val database: HealthDatabase
    fun closeDatabase()
    fun reopenDatabase()
}

/** What a database file contains. */
data class DataSummary(
    val weighIns: Long,
    val foodItems: Long,
    val foodLogs: Long,
    val sleepEntries: Long,
    val exerciseLogs: Long,
    /** Earliest and latest day with weight, food, or sleep (sleep by its sleep date); null when empty. */
    val firstDay: LocalDate?,
    val lastDay: LocalDate?,
)

enum class InvalidBackup { NOT_SQLITE, NEWER_VERSION, UNKNOWN_VERSION, MISSING_TABLES, CORRUPT, UNREADABLE }

sealed interface InspectResult {
    /** Passed every check; [file] is a private copy ready for [BackupRepository.replaceWith]. */
    data class Valid(val file: File, val schemaVersion: Int, val summary: DataSummary) : InspectResult
    data class Invalid(val reason: InvalidBackup) : InspectResult
}

sealed interface ImportResult {
    data object Imported : ImportResult

    /** The imported file couldn't be opened; the previous data was put back. */
    data object FailedAndRestored : ImportResult
}

/**
 * Export and import of the whole database through user-picked locations (Storage Access
 * Framework). Nothing here touches the network or shared storage directly.
 */
class BackupRepository(
    context: Context,
    private val dbName: String,
    private val holder: DatabaseHolder,
    private val zone: ZoneId,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val dbFile: File get() = appContext.getDatabasePath(dbName)
    private val exportSnapshot get() = File(appContext.cacheDir, "$dbName.export-snapshot")
    private val importCandidate get() = File(appContext.cacheDir, "$dbName.import-candidate")

    /** Kept until the next import, so a failed import can be rolled back. Never leaves the app. */
    private val preImportBackup get() = File(appContext.filesDir, "$dbName.pre-import-backup")

    /**
     * Writes a consistent snapshot of the database to [destination] and returns what it contains.
     * VACUUM INTO produces a complete copy in one step (WAL contents included), so writes made
     * during the export can't leave a half-updated file.
     */
    suspend fun export(destination: Uri): DataSummary = withContext(io) {
        val snapshot = exportSnapshot
        snapshot.delete()
        vacuumInto(snapshot)
        try {
            val summary = readOnly(snapshot) { summarize(it) }
            checkNotNull(resolver.openOutputStream(destination, "wt")) { "Can't write to $destination" }.use { out ->
                snapshot.inputStream().use { it.copyTo(out) }
            }
            summary
        } finally {
            snapshot.delete()
        }
    }

    /** What the live database contains now. */
    suspend fun currentSummary(): DataSummary = withContext(io) {
        val db = holder.database.openHelper.readableDatabase
        summarize(
            queryLong = { sql -> db.query(sql).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null } },
            tables = setOf("weight_entry", "food_item", "food_log", "sleep_entry", "exercise_log"),
        )
    }

    /** Copies [source] into private storage and checks it thoroughly before anything is replaced. */
    suspend fun inspect(source: Uri): InspectResult = withContext(io) {
        val candidate = importCandidate
        candidate.delete()
        try {
            val input = resolver.openInputStream(source) ?: return@withContext InspectResult.Invalid(InvalidBackup.UNREADABLE)
            input.use { inp -> candidate.outputStream().use { inp.copyTo(it) } }
        } catch (e: Exception) {
            return@withContext InspectResult.Invalid(InvalidBackup.UNREADABLE)
        }
        validate(candidate).also { if (it is InspectResult.Invalid) candidate.delete() }
    }

    /**
     * Replaces all current data with [candidate] (from [inspect]). The current database is first
     * snapshotted to private storage; if the new file then fails to open or migrate, that snapshot
     * is restored, so the app never ends up without data.
     */
    suspend fun replaceWith(candidate: InspectResult.Valid): ImportResult = withContext(io) {
        val backup = preImportBackup
        backup.delete()
        vacuumInto(backup)

        holder.closeDatabase()
        installDatabaseFile(candidate.file)
        try {
            holder.reopenDatabase()
            verifyOpens()
            ImportResult.Imported
        } catch (e: Exception) {
            runCatching { holder.closeDatabase() }
            installDatabaseFile(backup)
            holder.reopenDatabase()
            verifyOpens()
            ImportResult.FailedAndRestored
        } finally {
            candidate.file.delete()
        }
    }

    /** Opening runs migrations and Room's schema check; any problem throws here. */
    private fun verifyOpens() {
        holder.database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM weight_entry").use { it.moveToFirst() }
    }

    private fun vacuumInto(target: File) {
        target.parentFile?.mkdirs()
        val path = target.absolutePath.replace("'", "''")
        holder.database.openHelper.writableDatabase.execSQL("VACUUM INTO '$path'")
    }

    /** Puts [source] in place of the (closed) database, removing any stale WAL/SHM files. */
    private fun installDatabaseFile(source: File) {
        val target = dbFile
        listOf(target, File("${target.path}-wal"), File("${target.path}-shm"), File("${target.path}-journal"))
            .forEach { it.delete() }
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }

    private fun validate(file: File): InspectResult {
        val header = ByteArray(16)
        val read = runCatching { file.inputStream().use { it.read(header) } }.getOrDefault(-1)
        if (read < header.size || !isSqliteHeader(header)) return InspectResult.Invalid(InvalidBackup.NOT_SQLITE)

        return try {
            readOnly(file) { db ->
                val version = db.version
                when {
                    version > HEALTH_DB_VERSION -> return@readOnly InspectResult.Invalid(InvalidBackup.NEWER_VERSION)
                    version < 1 -> return@readOnly InspectResult.Invalid(InvalidBackup.UNKNOWN_VERSION)
                }
                val ok = db.rawQuery("PRAGMA integrity_check", null).use { c -> c.moveToFirst() && c.getString(0) == "ok" }
                if (!ok) return@readOnly InspectResult.Invalid(InvalidBackup.CORRUPT)

                val tables = tableNames(db)
                if (!tables.containsAll(requiredTables(version))) {
                    return@readOnly InspectResult.Invalid(InvalidBackup.MISSING_TABLES)
                }
                InspectResult.Valid(file, version, summarize(db))
            }
        } catch (e: Exception) {
            InspectResult.Invalid(InvalidBackup.CORRUPT)
        }
    }

    private fun requiredTables(version: Int): Set<String> = buildSet {
        add("weight_entry")
        if (version >= 2) addAll(listOf("food_item", "food_log"))
        if (version >= 3) add("sleep_entry")
        if (version >= 4) addAll(listOf("exercise_category", "exercise", "exercise_log"))
    }

    private fun tableNames(db: SQLiteDatabase): Set<String> =
        db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }

    private fun <T> readOnly(file: File, block: (SQLiteDatabase) -> T): T =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use(block)

    private fun summarize(db: SQLiteDatabase): DataSummary = summarize(
        queryLong = { sql -> db.rawQuery(sql, null).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null } },
        tables = tableNames(db),
    )

    /** Counts and date span; tolerates older files that lack the newer tables. */
    private fun summarize(queryLong: (String) -> Long?, tables: Set<String>): DataSummary {
        fun count(table: String) = if (table in tables) queryLong("SELECT COUNT(*) FROM $table") ?: 0 else 0
        fun extreme(fn: String, table: String, column: String) =
            if (table in tables) queryLong("SELECT $fn($column) FROM $table") else null
        fun Long.localDate() = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

        val firsts = listOfNotNull(
            extreme("MIN", "weight_entry", "date")?.let(LocalDate::ofEpochDay),
            extreme("MIN", "food_log", "eaten_at")?.localDate(),
            extreme("MIN", "sleep_entry", "start_at")?.let { sleepDate(it, zone) },
        )
        val lasts = listOfNotNull(
            extreme("MAX", "weight_entry", "date")?.let(LocalDate::ofEpochDay),
            extreme("MAX", "food_log", "eaten_at")?.localDate(),
            extreme("MAX", "sleep_entry", "start_at")?.let { sleepDate(it, zone) },
        )
        return DataSummary(
            weighIns = count("weight_entry"),
            foodItems = count("food_item"),
            foodLogs = count("food_log"),
            sleepEntries = count("sleep_entry"),
            exerciseLogs = count("exercise_log"),
            firstDay = firsts.minOrNull(),
            lastDay = lasts.maxOrNull(),
        )
    }
}
