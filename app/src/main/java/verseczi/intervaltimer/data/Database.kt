package verseczi.intervaltimer.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

/**
 * Created by verseczi on 8/15/2016.
 */

class Database(mContext: Context) {
    private var preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)

    // You have to set a default value for all of them in the defaultValue() function.

    val _ORIENTATION: String = "orientation"
    val _COORDINATEX: String = "CoordinateX"
    val _COORDINATEY: String = "CoordinateY"
    val _PACKAGENAME: String = "package_name"
    val _INTERVAL: String = "interval"
    val _IMGQTY: String = "imgqty"
    val _FPS: String = "fps"
    val _DELAYED: String = "delayed"
    val _DELAY: String = "delay"
    val _ENDLESS: String = "endless"
    val _CANCELLED: String = "endless"

    fun defaultValue(_key: String): Any {
        when(_key) {
            _ORIENTATION -> return 1
            _COORDINATEX -> return 20
            _COORDINATEY -> return 20
            _PACKAGENAME -> return "NO_NAME"
            _INTERVAL -> return 3
            _IMGQTY -> return 100
            _FPS -> return 30
            _DELAYED -> return true
            _DELAY -> return 10
            _ENDLESS -> return false
            _CANCELLED -> return false
        }
        return 0
    }

    var orientation: Int
        get() = import(_ORIENTATION) as Int
        set(value) = export(_ORIENTATION, value)

    var coordinateX: Int
        get() = import(_COORDINATEX) as Int
        set(value) = export(_COORDINATEX, value)

    var coordinateY: Int
        get() = import(_COORDINATEY) as Int
        set(value) = export(_COORDINATEY, value)

    var packageName: String
        get() = import(_PACKAGENAME) as String
        set(value) = export(_PACKAGENAME, value)

    var interval: Int
        get() = import(_INTERVAL) as Int
        set(value) = export(_INTERVAL, value)

    var imgQty: Int
        get() = import(_IMGQTY) as Int
        set(value) = export(_IMGQTY, value)

    var FPS: Int
        get() = import(_FPS) as Int
        set(value) = export(_FPS, value)

    var delayed: Boolean
        get() = import(_DELAYED) as Boolean
        set(value) = export(_DELAYED, value)

    var delay: Int
        get() = import(_DELAY) as Int
        set(value) = export(_DELAY, value)

    var endlessRepeat: Boolean
        get() = import(_ENDLESS) as Boolean
        set(value) = export(_ENDLESS, value)


    var cancelled: Boolean
        get() = import(_CANCELLED) as Boolean
        set(value) = export(_CANCELLED, value)


    fun export (_key: String, _value: Any){
        when(_value) {
            is String ->  return preferences.edit().putString(_key, _value).apply()
            is Int ->     return preferences.edit().putInt(_key, _value).apply()
            is Boolean -> return preferences.edit().putBoolean(_key, _value).apply()
            is Float ->   return preferences.edit().putFloat(_key, _value).apply()
            is Long ->    return preferences.edit().putLong(_key, _value).apply()
        }
    }

    fun import (_key: String): Any {
        val defaultValue = defaultValue(_key)
        when(defaultValue) {
            is String ->  return preferences.getString(_key, defaultValue)
            is Int ->     return preferences.getInt(_key, defaultValue)
            is Boolean -> return preferences.getBoolean(_key, defaultValue)
            is Float ->   return preferences.getFloat(_key, defaultValue)
            is Long ->    return preferences.getLong(_key, defaultValue)
        }
        return false
    }
}
