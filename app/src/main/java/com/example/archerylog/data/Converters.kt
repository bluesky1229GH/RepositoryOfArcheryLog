package com.example.archerylog.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromLocationType(value: LocationType): String {
        return value.name
    }

    @TypeConverter
    fun toLocationType(value: String): LocationType {
        return LocationType.valueOf(value)
    }
}
