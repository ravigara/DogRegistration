package com.example.dogregistration.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.ByteBuffer

/**
 * This class tells Room how to convert complex types (like Lists, Enums,
 * and FloatArrays) into simple types (like String or ByteArray)
 * that can be stored in an SQL database.
 */
class Converters {
    private val gson = Gson()

    // --- For FloatArray (Embedding) ---
    // Converts the FloatArray into a simple ByteArray
    @TypeConverter
    fun fromFloatArray(array: FloatArray?): ByteArray? {
        array ?: return null
        val buffer = ByteBuffer.allocate(array.size * 4) // 4 bytes per float
        buffer.asFloatBuffer().put(array)
        return buffer.array()
    }

    // Converts a ByteArray back into our FloatArray
    @TypeConverter
    fun toFloatArray(byteArray: ByteArray?): FloatArray? {
        byteArray ?: return null
        val buffer = ByteBuffer.wrap(byteArray)
        val floatBuffer = buffer.asFloatBuffer()
        val floatArray = FloatArray(floatBuffer.capacity())
        floatBuffer.get(floatArray)
        return floatArray
    }

    // --- For List<VaccinationRecord> ---
    // Converts the list into a JSON String
    @TypeConverter
    fun fromVaccinationList(list: List<VaccinationRecord>?): String? {
        return gson.toJson(list)
    }

    // Converts the JSON String back into a List
    @TypeConverter
    fun toVaccinationList(json: String?): List<VaccinationRecord>? {
        val type = object : TypeToken<List<VaccinationRecord>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- For Enums ---
    // Room will just store "MALE", "FEMALE", etc. as a String.
    @TypeConverter
    fun fromDogGender(gender: DogGender): String = gender.name
    @TypeConverter
    fun toDogGender(name: String): DogGender = DogGender.valueOf(name)

    @TypeConverter
    fun fromDogColor(color: DogColor): String = color.name
    @TypeConverter
    fun toDogColor(name: String): DogColor = DogColor.valueOf(name)

    @TypeConverter
    fun fromDogType(type: DogType): String = type.name
    @TypeConverter
    fun toDogType(name: String): DogType = DogType.valueOf(name)
}