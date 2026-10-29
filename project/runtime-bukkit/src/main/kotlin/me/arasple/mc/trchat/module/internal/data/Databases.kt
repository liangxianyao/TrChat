package me.arasple.mc.trchat.module.internal.data

import me.arasple.mc.trchat.api.event.CustomDatabaseEvent
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.util.print
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.function.disablePlugin
import taboolib.expansion.Database
import taboolib.expansion.playerDatabase
import taboolib.expansion.setupPlayerDatabase
import taboolib.module.database.HostPostgreSQL
import java.sql.SQLException
import java.time.LocalDate
import java.util.UUID

//lateinit var globalPDC: DataContainer

@PlatformSide(Platform.BUKKIT)
object Databases {

    private const val DAILY_TABLE = "trchat_global_daily_limit"

    enum class DailyLimitResult { ALLOWED, EXCEEDED, UNAVAILABLE }

    @Awake(LifeCycle.ENABLE)
    fun init() {
        try {
            when (val type = Settings.conf.getString("Database.Method")?.uppercase()) {
                "LOCAL", "SQLITE", null -> setupPlayerDatabase()
                "SQL", "MYSQL" -> setupPlayerDatabase(
                    Settings.conf.getConfigurationSection("Database.SQL")!!,
                    Settings.conf.getString("Database.SQL.table")!! + "_v2"
                )
                "POSTGRESQL", "POSTGRES" -> playerDatabase = Database(
                    TypePostgreSQL(
                        HostPostgreSQL(Settings.conf.getConfigurationSection("Database.SQL")!!),
                        Settings.conf.getString("Database.SQL.table")!! + "_v2"
                    )
                )
                else -> {
                    val event = CustomDatabaseEvent(type)
                    event.call()
                    playerDatabase = event.database ?: error("Unsupported database type: $type")
                }
            }
            playerDatabase!!.dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS $DAILY_TABLE (" +
                            "player_uuid VARCHAR(36) PRIMARY KEY, date_key CHAR(10) NOT NULL, sent_count INTEGER NOT NULL)"
                    )
                }
            }
//            globalPDC = DataContainer(".", playerDatabase!!)
        } catch (t: Throwable) {
            t.print("Failed to load database! Plugin will be disabled.")
            disablePlugin()
        }
    }

    /** 条件更新保证多个后端共享数据库时不会超出每日额度。 */
    fun reserveDailyMessage(uuid: UUID, date: LocalDate, limit: Int): DailyLimitResult {
        if (limit < 0) return DailyLimitResult.ALLOWED
        if (limit == 0) return DailyLimitResult.EXCEEDED
        val day = date.toString()
        return try {
            playerDatabase!!.dataSource.connection.use { connection ->
                try {
                    connection.prepareStatement(
                        "INSERT INTO $DAILY_TABLE (player_uuid, date_key, sent_count) VALUES (?, ?, 1)"
                    ).use { insert ->
                        insert.setString(1, uuid.toString())
                        insert.setString(2, day)
                        insert.executeUpdate()
                    }
                    return DailyLimitResult.ALLOWED
                } catch (e: SQLException) {
                    if (!e.sqlState.orEmpty().startsWith("23") && e.errorCode != 19) throw e
                }
                connection.prepareStatement(
                    "UPDATE $DAILY_TABLE SET date_key = ?, sent_count = 1 WHERE player_uuid = ? AND date_key < ?"
                ).use { reset ->
                    reset.setString(1, day)
                    reset.setString(2, uuid.toString())
                    reset.setString(3, day)
                    if (reset.executeUpdate() == 1) return DailyLimitResult.ALLOWED
                }
                connection.prepareStatement(
                    "UPDATE $DAILY_TABLE SET sent_count = sent_count + 1 " +
                        "WHERE player_uuid = ? AND date_key = ? AND sent_count < ?"
                ).use { increment ->
                    increment.setString(1, uuid.toString())
                    increment.setString(2, day)
                    increment.setInt(3, limit)
                    if (increment.executeUpdate() == 1) return DailyLimitResult.ALLOWED
                }
                DailyLimitResult.EXCEEDED
            }
        } catch (e: Exception) {
            e.print("Failed to update global channel daily limit")
            DailyLimitResult.UNAVAILABLE
        }
    }

}
