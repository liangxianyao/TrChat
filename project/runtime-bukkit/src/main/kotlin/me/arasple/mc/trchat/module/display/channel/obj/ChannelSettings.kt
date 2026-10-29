package me.arasple.mc.trchat.module.display.channel.obj

import me.arasple.mc.trchat.module.internal.script.Condition
import java.time.ZoneId
import java.time.Clock
import java.time.LocalDate
import java.util.Locale

data class DailyLimitSettings(
    val defaultLimit: Int,
    val groups: Map<String, Int>,
    val timeZone: ZoneId
) {
    init {
        require(defaultLimit >= -1) { "Default daily limit must be >= -1" }
        require(groups.values.all { it >= -1 }) { "Group daily limits must be >= -1" }
    }

    fun limitFor(primaryGroup: String?): Int = groups[primaryGroup?.lowercase(Locale.ROOT)] ?: defaultLimit

    fun date(clock: Clock = Clock.systemUTC()): LocalDate = LocalDate.now(clock.withZone(timeZone))
}

/**
 * @author ItsFlicker
 * @since 2022/2/5 13:25
 */
class ChannelSettings(
    val joinPermission: String = "",
    val listenPermission: String = "",
    val speakCondition: Condition = Condition.EMPTY,
    val alwaysListen: Boolean = true,
    val isPrivate: Boolean = false,
    val range: ChannelRange = ChannelRange(ChannelRange.Type.ALL, -1),
    val proxy: Boolean = false,
    val forceProxy: Boolean = false,
    val doubleTransfer: Boolean = true,
    val ports: List<Int> = emptyList(),
    val disabledFunctions: List<String> = emptyList(),
    val filterBeforeSending: Boolean = false,
    val sendToDiscord: Boolean = true,
    val receiveFromDiscord: Boolean = true,
    val discordChannel: String = "",
    val dailyLimit: DailyLimitSettings? = null
)
