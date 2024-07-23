package com.example.myapp.prefs

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsHelper (context: Context){
    private val PREFS_NAME = "HomeFragmentPrefs"
    private val TAB_POSITION_KEY = "selected_tab_position"

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Save tab position
    fun saveSelectedTabPosition(position: Int) {
        sharedPreferences.edit().putInt(TAB_POSITION_KEY, position).apply()
    }

    // Get saved tab position
    fun getSavedTabPosition(): Int {
        return sharedPreferences.getInt(TAB_POSITION_KEY, 0)
    }
}