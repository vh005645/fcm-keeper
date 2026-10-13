package com.fcmkeeper.iqoo

import android.content.Context

object Prefs {
    private const val FILE = "fcm_keeper"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_BEAT = "last_beat"
    private const val KEY_NEXT_BEAT = "next_beat"
    private const val KEY_INTERVAL = "interval_min"
    private const val KEY_CONN = "conn_state"
    private const val KEY_LAST_RECONNECT = "last_reconnect"
    private const val KEY_STATUS_CODE = "status_code"
    private const val KEY_STATUS_ERROR = "status_error"
    private const val KEY_BIND = "bind_gms"

    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean(KEY_ENABLED, v).apply()

    fun lastBeat(ctx: Context): Long = p(ctx).getLong(KEY_LAST_BEAT, 0L)
    fun setLastBeat(ctx: Context, v: Long) = p(ctx).edit().putLong(KEY_LAST_BEAT, v).apply()

    fun nextBeat(ctx: Context): Long = p(ctx).getLong(KEY_NEXT_BEAT, 0L)
    fun intervalMin(ctx: Context): Int = p(ctx).getInt(KEY_INTERVAL, 0)
    fun setNext(ctx: Context, nextWall: Long, intervalMin: Int) =
        p(ctx).edit().putLong(KEY_NEXT_BEAT, nextWall).putInt(KEY_INTERVAL, intervalMin).apply()

    /** 1 = connected, 0 = disconnected, -1 = unknown. */
    fun connState(ctx: Context): Int = p(ctx).getInt(KEY_CONN, -1)
    fun setConnState(ctx: Context, v: Int) = p(ctx).edit().putInt(KEY_CONN, v).apply()

    /** Raw status code reported by GMS (null if it could not be read), and the reason if so. */
    fun statusCode(ctx: Context): String? = p(ctx).getString(KEY_STATUS_CODE, null)
    fun statusError(ctx: Context): String? = p(ctx).getString(KEY_STATUS_ERROR, null)
    fun setStatus(ctx: Context, code: String?, error: String?) =
        p(ctx).edit().putString(KEY_STATUS_CODE, code).putString(KEY_STATUS_ERROR, error).apply()

    fun lastReconnect(ctx: Context): Long = p(ctx).getLong(KEY_LAST_RECONNECT, 0L)
    fun setLastReconnect(ctx: Context, v: Long) = p(ctx).edit().putLong(KEY_LAST_RECONNECT, v).apply()

    /** Whether to keep the experimental bound connection to GMS (default on). */
    fun bindEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_BIND, true)
    fun setBindEnabled(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean(KEY_BIND, v).apply()
}
