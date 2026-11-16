package com.jonesl7l.carinfg.feature.incident.form

import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetails
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentLocation
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
data class IncidentFormFields(
    val details: IncidentDetails,
    val location: IncidentLocation = IncidentLocation(),
    val safety: IncidentSafety = IncidentSafety(),
    val persons: List<Person> = emptyList(),
    val photos: List<Photo> = emptyList(),
    val additionalNotes: String? = null,
)

fun Incident.toFormFields() = IncidentFormFields(
    details = details,
    location = location,
    safety = safety,
    persons = persons,
    photos = photos,
    additionalNotes = additionalNotes,
)

fun IncidentFormFields.applyTo(incident: Incident) = incident.copy(
    details = details,
    location = location,
    safety = safety,
    persons = persons,
    photos = photos,
    additionalNotes = additionalNotes,
)
