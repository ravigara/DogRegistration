package com.example.dogregistration.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// Enums (DogType, DogGender, DogColor) are unchanged...
enum class DogType(val displayName: String) {
    PET("Pet"),
    STREET("Street Dog"),
    ADOPTED("Adopted")
}
enum class DogGender {
    Male, Female, None
}
enum class DogColor(val displayName: String) {
    NONE("None"),
    BLACK("Black"),
    WHITE("White"),
    BROWN("Brown"),
    GOLDEN("Golden"),
    CREAM("Cream"),
    GREY("Grey"),
    RED("Red")
}

// VaccinationRecord is unchanged...
data class VaccinationRecord(
    val date: String,
    val id: UUID = UUID.randomUUID()
)

// --- THIS IS THE UPDATED PART ---
@Entity(tableName = "dog_profiles") // Tell Room this is a database table
data class DogProfile(
    @PrimaryKey(autoGenerate = true) // Add an auto-generating ID for the database
    val id: Long = 0,

    val dogName: String = "",
    val breed: String = "",
    val gender: DogGender = DogGender.None,
    val primaryColor: DogColor = DogColor.NONE,
    val secondaryColor: DogColor = DogColor.NONE,
    val ageInMonths: Int = 0,
    val dogType: DogType = DogType.PET,
    val ownerName: String? = null,
    val adoptionDate: String? = null,
    val vaccinations: List<VaccinationRecord> = emptyList(),
    val microchipNumber: String? = null,

    // Add the field to store the "fingerprint"
    val embedding: FloatArray? = null
) {
    // Add equals/hashCode for FloatArray, which is good practice for Room entities
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DogProfile

        if (id != other.id) return false
        if (dogName != other.dogName) return false
        if (breed != other.breed) return false
        if (gender != other.gender) return false
        if (primaryColor != other.primaryColor) return false
        if (secondaryColor != other.secondaryColor) return false
        if (ageInMonths != other.ageInMonths) return false
        if (dogType != other.dogType) return false
        if (ownerName != other.ownerName) return false
        if (adoptionDate != other.adoptionDate) return false
        if (vaccinations != other.vaccinations) return false
        if (microchipNumber != other.microchipNumber) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dogName.hashCode()
        result = 31 * result + breed.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + primaryColor.hashCode()
        result = 31 * result + secondaryColor.hashCode()
        result = 31 * result + ageInMonths
        result = 31 * result + dogType.hashCode()
        result = 31 * result + (ownerName?.hashCode() ?: 0)
        result = 31 * result + (adoptionDate?.hashCode() ?: 0)
        result = 31 * result + vaccinations.hashCode()
        result = 31 * result + (microchipNumber?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}