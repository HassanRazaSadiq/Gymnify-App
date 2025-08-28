package com.example.gymapp

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri // Import Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth

object AppPreferences {
    private const val PREFS_NAME = "user_profile_prefs"
    private const val KEY_USERNAME = "user_username"
    private const val KEY_FULL_NAME = "user_full_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_GENDER = "user_gender"
    private const val KEY_AGE = "user_age"
    private const val KEY_HEIGHT_VALUE = "user_height_value"
    private const val KEY_HEIGHT_UNIT = "user_height_unit"
    private const val KEY_WEIGHT_VALUE = "user_weight_value"
    private const val KEY_WEIGHT_UNIT = "user_weight_unit"
    private const val KEY_NEEDS_ONBOARDING = "needs_onboarding"
    private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri" // Legacy key

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Profile Image URI ---
    fun saveProfileImageUri(context: Context, uriString: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        val key = "profile_image_uri_${'$'}userId"
        val editor = getPreferences(context).edit()
        if (uriString != null) {
            editor.putString(key, uriString)
        } else {
            editor.remove(key)
        }
        editor.apply()
    }

    fun getProfileImageUri(context: Context): Uri? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        val key = "profile_image_uri_${'$'}userId"
        val prefs = getPreferences(context)
        val uriString = prefs.getString(key, null)
        if (uriString != null) return Uri.parse(uriString)
        // Fallback to legacy key and migrate
        val legacy = prefs.getString(KEY_PROFILE_IMAGE_URI, null)
        if (legacy != null) {
            saveProfileImageUri(context, legacy)
            return Uri.parse(legacy)
        }
        return null
    }

    // --- Username ---
    fun saveUsername(context: Context, username: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    fun getUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_USERNAME, null)
    }

    // --- Full Name ---
    fun saveFullName(context: Context, fullName: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_FULL_NAME, fullName)
        editor.apply()
    }

    fun getFullName(context: Context): String? {
        return getPreferences(context).getString(KEY_FULL_NAME, null)
    }

    // --- Email ---
    fun saveEmail(context: Context, email: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_EMAIL, email)
        editor.apply()
    }

    fun getEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_EMAIL, null)
    }

    // --- Gender ---
    fun saveGender(context: Context, gender: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_GENDER, gender)
        editor.apply()
    }

    fun getGender(context: Context): String? {
        return getPreferences(context).getString(KEY_GENDER, null)
    }

    // --- Age ---
    fun saveAge(context: Context, age: Int) {
        val editor = getPreferences(context).edit()
        editor.putInt(KEY_AGE, age)
        editor.apply()
    }

    fun getAge(context: Context): Int {
        return getPreferences(context).getInt(KEY_AGE, 0)
    }

    // --- Height ---
    fun saveHeight(context: Context, value: Float, unit: String) {
        val editor = getPreferences(context).edit()
        editor.putFloat(KEY_HEIGHT_VALUE, value)
        editor.putString(KEY_HEIGHT_UNIT, unit)
        editor.apply()
    }

    fun getHeightValue(context: Context): Float {
        return getPreferences(context).getFloat(KEY_HEIGHT_VALUE, 0f)
    }

    fun getHeightUnit(context: Context): String? {
        return getPreferences(context).getString(KEY_HEIGHT_UNIT, null)
    }

    // --- Weight ---
    fun saveWeight(context: Context, value: Float, unit: String) {
        val editor = getPreferences(context).edit()
        editor.putFloat(KEY_WEIGHT_VALUE, value)
        editor.putString(KEY_WEIGHT_UNIT, unit)
        editor.apply()
    }

    fun getWeightValue(context: Context): Float {
        return getPreferences(context).getFloat(KEY_WEIGHT_VALUE, 0f)
    }

    fun getWeightUnit(context: Context): String? {
        return getPreferences(context).getString(KEY_WEIGHT_UNIT, null)
    }

    // --- Save/Update Entire Profile ---
    data class UserProfile(
        val username: String?,
        val fullName: String?,
        val email: String?,
        val gender: String?,
        val age: Int,
        val heightValue: Float,
        val heightUnit: String?,
        val weightValue: Float,
        val weightUnit: String?,
        val profileImageUri: String?,
        val exerciseRecords: List<ExerciseRecord> = emptyList()
    )

    fun saveUserProfile(context: Context, profile: UserProfile) {
        val editor = getPreferences(context).edit()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        android.util.Log.d("AppPreferences", "saveUserProfile uid=${'$'}userId profile=${'$'}profile")
        editor.putString("user_username_${'$'}userId", profile.username)
        editor.putString("user_full_name_${'$'}userId", profile.fullName)
        editor.putString("user_email_${'$'}userId", profile.email)
        editor.putString("user_gender_${'$'}userId", profile.gender)
        editor.putInt("user_age_${'$'}userId", profile.age)
        editor.putFloat("user_height_value_${'$'}userId", profile.heightValue)
        editor.putString("user_height_unit_${'$'}userId", profile.heightUnit)
        editor.putFloat("user_weight_value_${'$'}userId", profile.weightValue)
        editor.putString("user_weight_unit_${'$'}userId", profile.weightUnit)
        // Save profile image URI per user
        val profileImageKey = "profile_image_uri_${'$'}userId"
        editor.putString(profileImageKey, profile.profileImageUri)
        // Save exercise records as JSON per user
        val gson = Gson()
        editor.putString("user_exercise_records_${'$'}userId", gson.toJson(profile.exerciseRecords))
        editor.apply()
    }

    fun getUserProfile(context: Context): UserProfile {
        val prefs = getPreferences(context)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        android.util.Log.d("AppPreferences", "getUserProfile uid=${'$'}userId")
        val gson = Gson()
        val json = prefs.getString("user_exercise_records_${'$'}userId", null)
        val type = object : TypeToken<List<ExerciseRecord>>() {}.type
        val exerciseRecords: List<ExerciseRecord> = if (json != null) gson.fromJson(json, type) else emptyList()
        val profileImageUri = prefs.getString("profile_image_uri_${'$'}userId", null)
        var profile = UserProfile(
            prefs.getString("user_username_${'$'}userId", null),
            prefs.getString("user_full_name_${'$'}userId", null),
            prefs.getString("user_email_${'$'}userId", null),
            prefs.getString("user_gender_${'$'}userId", null),
            prefs.getInt("user_age_${'$'}userId", 0),
            prefs.getFloat("user_height_value_${'$'}userId", 0f),
            prefs.getString("user_height_unit_${'$'}userId", null),
            prefs.getFloat("user_weight_value_${'$'}userId", 0f),
            prefs.getString("user_weight_unit_${'$'}userId", null),
            profileImageUri,
            exerciseRecords
        )
        // If per-user profile looks empty, migrate from legacy globals
        val looksEmpty = (profile.username == null && profile.fullName == null && profile.email == null &&
                profile.gender == null && profile.age == 0 && profile.heightValue == 0f && profile.weightValue == 0f && profile.profileImageUri == null)
        if (looksEmpty) {
            val legacyProfile = UserProfile(
                prefs.getString(KEY_USERNAME, null),
                prefs.getString(KEY_FULL_NAME, null),
                prefs.getString(KEY_EMAIL, null),
                prefs.getString(KEY_GENDER, null),
                prefs.getInt(KEY_AGE, 0),
                prefs.getFloat(KEY_HEIGHT_VALUE, 0f),
                prefs.getString(KEY_HEIGHT_UNIT, null),
                prefs.getFloat(KEY_WEIGHT_VALUE, 0f),
                prefs.getString(KEY_WEIGHT_UNIT, null),
                prefs.getString(KEY_PROFILE_IMAGE_URI, null),
                exerciseRecords
            )
            val anyLegacy = (legacyProfile.username != null || legacyProfile.fullName != null || legacyProfile.email != null ||
                    legacyProfile.gender != null || legacyProfile.age != 0 || legacyProfile.heightValue != 0f || legacyProfile.weightValue != 0f || legacyProfile.profileImageUri != null)
            if (anyLegacy) {
                saveUserProfile(context, legacyProfile)
                profile = legacyProfile
            }
        }
        android.util.Log.d("AppPreferences", "loaded profile for uid=${'$'}userId -> ${'$'}profile")
        return profile
    }

    // --- Exercise Records ---
    private const val KEY_EXERCISE_RECORDS = "user_exercise_records"

    data class ExerciseRecord(
        val name: String,
        val timestamp: Long,
        val reps: Int,
        val durationSeconds: Int = 0
    )

    fun addExerciseRecord(context: Context, record: ExerciseRecord) {
        val profile = getUserProfile(context)
        val updatedRecords = profile.exerciseRecords.toMutableList().apply { add(record) }
        val updatedProfile = profile.copy(exerciseRecords = updatedRecords)
        saveUserProfile(context, updatedProfile)
    }

    fun getExerciseRecords(context: Context): List<ExerciseRecord> {
        val gson = Gson()
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_EXERCISE_RECORDS, null)
        val type = object : TypeToken<List<ExerciseRecord>>() {}.type
        return if (json != null) gson.fromJson(json, type) else emptyList()
    }

    fun setExerciseRecords(context: Context, records: List<ExerciseRecord>) {
        val gson = Gson()
        val json = gson.toJson(records)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_EXERCISE_RECORDS, json).apply()
    }

    // New: set exercise records for current user profile (preferred)
    fun setCurrentUserExerciseRecords(context: Context, records: List<ExerciseRecord>) {
        val profile = getUserProfile(context)
        saveUserProfile(context, profile.copy(exerciseRecords = records))
    }

    // --- Onboarding ---
    fun setNeedsOnboarding(context: Context, needsOnboarding: Boolean) {
        val editor = getPreferences(context).edit()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        editor.putBoolean("needs_onboarding_${'$'}userId", needsOnboarding)
        editor.apply()
    }

    fun getNeedsOnboarding(context: Context): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        return getPreferences(context).getBoolean("needs_onboarding_${'$'}userId", false)
    }

    fun clearCurrentUserData(context: Context) {
        val prefs = getPreferences(context)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        val keys = prefs.all.keys
        val editor = prefs.edit()
        keys.forEach { key ->
            if (key.endsWith("_${'$'}userId") ||
                key == "profile_image_uri_${'$'}userId" ||
                key == "user_exercise_records_${'$'}userId" ||
                key == "needs_onboarding_${'$'}userId"
            ) {
                editor.remove(key)
            }
        }
        editor.apply()
        android.util.Log.d("AppPreferences", "Cleared data for uid=${'$'}userId")
    }
}
