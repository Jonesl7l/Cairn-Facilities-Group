package com.jonesl7l.carinfg.core.network

import com.jonesl7l.carinfg.core.di.NetworkModule
import com.jonesl7l.carinfg.core.network.dto.incident.DeleteIncidentRequest
import com.jonesl7l.carinfg.core.network.dto.incident.GpsLocationDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentWriteRequest
import com.jonesl7l.carinfg.core.network.dto.person.InjuryDto
import com.jonesl7l.carinfg.core.network.dto.person.PersonDto
import com.jonesl7l.carinfg.core.network.dto.photo.PhotoDto
import com.jonesl7l.carinfg.core.network.dto.problem.ProblemDetailsDto
import com.jonesl7l.carinfg.core.network.dto.problem.ProblemFieldErrorDto
import com.jonesl7l.carinfg.core.network.dto.reference.AssetDto
import com.jonesl7l.carinfg.core.network.dto.reference.BuildingDto
import com.jonesl7l.carinfg.core.network.dto.reference.FloorDto
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.dto.reference.RoomDto
import com.jonesl7l.carinfg.core.network.dto.reference.SiteDto
import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.MediaTypes
import com.jonesl7l.carinfg.core.network.result.ApiCallHandler
import com.jonesl7l.carinfg.core.network.result.ProblemParser
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiTestHelper {
    val now: Instant = Instant.parse("2026-11-13T12:00:00Z")
    const val REPORTER_ID = "tech1"
    const val INCIDENT_ID = "incident-1"
    const val DATABASE_ID = "db-1"

    val json: Json = NetworkModule.provideJson()

    private val problemJsonMediaType = MediaTypes.PROBLEM_JSON.toMediaType()

    fun retrofit(server: MockWebServer): Retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(OkHttpClient())
        .addConverterFactory(json.asConverterFactory(MediaTypes.JSON.toMediaType()))
        .build()

    fun apiCallHandler(): ApiCallHandler = ApiCallHandler(problemParser = ProblemParser(json = json))

    fun jsonResponse(
        code: Int = 200,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): MockResponse = MockResponse.Builder()
        .code(code)
        .addHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.JSON)
        .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
        .body(body)
        .build()

    fun errorResponse(
        status: Int,
        body: String = "",
        headers: Map<String, String> = emptyMap(),
    ): Response<Unit> {
        val raw = okhttp3.Response.Builder()
            .code(status)
            .message("Error")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/").build())
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        return Response.error(body.toResponseBody(problemJsonMediaType), raw)
    }

    fun problemJson(problem: ProblemDetailsDto): String = json.encodeToString(problem)

    fun userDto(
        id: String = "u1",
        username: String = REPORTER_ID,
        displayName: String = "Tech One",
        role: String = "TECHNICIAN",
    ) = UserDto(
        id = id,
        username = username,
        displayName = displayName,
        role = role,
    )

    fun problemDetailsDto(
        status: Int,
        type: String? = "about:blank",
        title: String? = "Problem",
        detail: String? = null,
        code: String? = null,
        deletedAt: Instant? = null,
        deletedBy: String? = null,
        referenceNumber: String? = null,
        currentStatus: String? = null,
        current: IncidentDto? = null,
        errors: List<ProblemFieldErrorDto> = emptyList(),
    ) = ProblemDetailsDto(
        type = type,
        title = title,
        status = status,
        detail = detail,
        code = code,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        referenceNumber = referenceNumber,
        currentStatus = currentStatus,
        current = current,
        errors = errors,
    )

    fun incidentDto(
        id: String = INCIDENT_ID,
        referenceNumber: String = "INC-2026-00001",
        reporterId: String = REPORTER_ID,
        reportedAt: Instant = now,
        gpsLocation: GpsLocationDto? = gpsLocationDto(),
        title: String = "Wet floor in lobby",
        description: String = "Water pooling by the main entrance.",
        occurredAt: Instant = now - 1.hours,
        occurredTimeZone: String = "Europe/London",
        incidentType: String = "SLIP_TRIP_FALL",
        incidentTypeOther: String? = null,
        severity: String = "MEDIUM",
        siteId: String = "site-1",
        buildingId: String? = "building-1",
        floorId: String? = "floor-1",
        roomId: String? = "room-1",
        unlistedLocation: String? = null,
        exactSpot: String? = "By the revolving door",
        assetId: String? = "asset-1",
        equipmentOutOfService: Boolean = true,
        injuryOccurred: Boolean = true,
        hazardStillPresent: Boolean = false,
        immediateActions: List<String> = listOf("FIRST_AID_GIVEN", "OTHER"),
        immediateActionsOther: String? = "Mopped the floor",
        persons: List<PersonDto> = listOf(personDto()),
        photos: List<PhotoDto> = listOf(photoDto()),
        additionalNotes: String? = "Cleaner informed",
        status: String = "OPEN",
        assignedTo: String? = "operator1",
        dueDate: LocalDate? = LocalDate(
            year = 2026,
            month = 11,
            day = 20,
        ),
        rootCause: String? = "Leaking roof",
        correctiveActions: String? = "Roof repair booked",
        regulatoryReportable: Boolean? = false,
        supervisorAcknowledged: Boolean? = true,
        createdAt: Instant = now,
        updatedAt: Instant = now,
        version: Int = 1,
        changeSeq: Long = 1,
        deletedAt: Instant? = null,
        deletedBy: String? = null,
    ) = IncidentDto(
        id = id,
        referenceNumber = referenceNumber,
        reporterId = reporterId,
        reportedAt = reportedAt,
        gpsLocation = gpsLocation,
        title = title,
        description = description,
        occurredAt = occurredAt,
        occurredTimeZone = occurredTimeZone,
        incidentType = incidentType,
        incidentTypeOther = incidentTypeOther,
        severity = severity,
        siteId = siteId,
        buildingId = buildingId,
        floorId = floorId,
        roomId = roomId,
        unlistedLocation = unlistedLocation,
        exactSpot = exactSpot,
        assetId = assetId,
        equipmentOutOfService = equipmentOutOfService,
        injuryOccurred = injuryOccurred,
        hazardStillPresent = hazardStillPresent,
        immediateActions = immediateActions,
        immediateActionsOther = immediateActionsOther,
        persons = persons,
        photos = photos,
        additionalNotes = additionalNotes,
        status = status,
        assignedTo = assignedTo,
        dueDate = dueDate,
        rootCause = rootCause,
        correctiveActions = correctiveActions,
        regulatoryReportable = regulatoryReportable,
        supervisorAcknowledged = supervisorAcknowledged,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        changeSeq = changeSeq,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
    )

    fun problemFieldErrorDto(
        field: String = "title",
        code: String? = "TOO_SHORT",
        message: String? = "Title must be at least 3 characters",
    ) = ProblemFieldErrorDto(
        field = field,
        code = code,
        message = message,
    )

    fun incidentWriteRequest(
        id: String = INCIDENT_ID,
        reporterId: String = REPORTER_ID,
        reportedAt: Instant = now,
        gpsLocation: GpsLocationDto? = gpsLocationDto(),
        title: String = "Wet floor in lobby",
        description: String = "Water pooling by the main entrance.",
        occurredAt: Instant = now - 1.hours,
        occurredTimeZone: String = "Europe/London",
        incidentType: String = "SLIP_TRIP_FALL",
        incidentTypeOther: String? = null,
        severity: String = "MEDIUM",
        siteId: String = "site-1",
        buildingId: String? = "building-1",
        floorId: String? = "floor-1",
        roomId: String? = "room-1",
        unlistedLocation: String? = null,
        exactSpot: String? = "By the revolving door",
        assetId: String? = "asset-1",
        equipmentOutOfService: Boolean = true,
        injuryOccurred: Boolean = true,
        hazardStillPresent: Boolean = false,
        immediateActions: List<String> = listOf("FIRST_AID_GIVEN", "OTHER"),
        immediateActionsOther: String? = "Mopped the floor",
        persons: List<PersonDto> = listOf(personDto()),
        photos: List<PhotoDto> = listOf(photoDto()),
        additionalNotes: String? = "Cleaner informed",
    ) = IncidentWriteRequest(
        id = id,
        reporterId = reporterId,
        reportedAt = reportedAt,
        gpsLocation = gpsLocation,
        title = title,
        description = description,
        occurredAt = occurredAt,
        occurredTimeZone = occurredTimeZone,
        incidentType = incidentType,
        incidentTypeOther = incidentTypeOther,
        severity = severity,
        siteId = siteId,
        buildingId = buildingId,
        floorId = floorId,
        roomId = roomId,
        unlistedLocation = unlistedLocation,
        exactSpot = exactSpot,
        assetId = assetId,
        equipmentOutOfService = equipmentOutOfService,
        injuryOccurred = injuryOccurred,
        hazardStillPresent = hazardStillPresent,
        immediateActions = immediateActions,
        immediateActionsOther = immediateActionsOther,
        persons = persons,
        photos = photos,
        additionalNotes = additionalNotes,
    )

    fun deleteIncidentRequest(
        deletedAt: Instant = now,
        deletedBy: String = REPORTER_ID,
    ) = DeleteIncidentRequest(
        deletedAt = deletedAt,
        deletedBy = deletedBy,
    )

    fun gpsLocationDto(
        lat: Double = 51.5074,
        lng: Double = -0.1278,
        accuracyMeters: Float = 12f,
    ) = GpsLocationDto(
        lat = lat,
        lng = lng,
        accuracyMeters = accuracyMeters,
    )

    fun personDto(
        id: String = "person-1",
        name: String = "Alex Smith",
        personType: String = "EMPLOYEE",
        role: String = "INJURED",
        department: String? = "Facilities",
        position: String? = "Cleaner",
        company: String? = null,
        contactNumber: String? = "07700900000",
        injury: InjuryDto? = injuryDto(),
    ) = PersonDto(
        id = id,
        name = name,
        personType = personType,
        role = role,
        department = department,
        position = position,
        company = company,
        contactNumber = contactNumber,
        injury = injury,
    )

    fun injuryDto(
        nature: String = "Sprain",
        bodyPart: String = "Left ankle",
        treatment: String = "FIRST_AID",
        notes: String? = null,
    ) = InjuryDto(
        nature = nature,
        bodyPart = bodyPart,
        treatment = treatment,
        notes = notes,
    )

    fun photoDto(
        id: String = "photo-0",
        incidentId: String = INCIDENT_ID,
        url: String = "uploads/incidents/$incidentId/$id.jpg",
    ) = PhotoDto(
        id = id,
        url = url,
    )

    fun referenceDataDto(
        sites: List<SiteDto> = listOf(siteDto()),
        buildings: List<BuildingDto> = listOf(buildingDto()),
        floors: List<FloorDto> = listOf(floorDto()),
        rooms: List<RoomDto> = listOf(roomDto()),
        assets: List<AssetDto> = listOf(assetDto()),
        technicians: List<UserDto> = listOf(userDto()),
    ) = ReferenceDataDto(
        sites = sites,
        buildings = buildings,
        floors = floors,
        rooms = rooms,
        assets = assets,
        technicians = technicians,
    )

    fun siteDto(
        id: String = "site-1",
        name: String = "Head Office",
    ) = SiteDto(
        id = id,
        name = name,
    )

    fun buildingDto(
        id: String = "building-1",
        siteId: String = "site-1",
        name: String = "North Block",
    ) = BuildingDto(
        id = id,
        siteId = siteId,
        name = name,
    )

    fun floorDto(
        id: String = "floor-1",
        buildingId: String = "building-1",
        name: String = "Ground Floor",
    ) = FloorDto(
        id = id,
        buildingId = buildingId,
        name = name,
    )

    fun roomDto(
        id: String = "room-1",
        floorId: String = "floor-1",
        name: String = "Lobby",
    ) = RoomDto(
        id = id,
        floorId = floorId,
        name = name,
    )

    fun assetDto(
        id: String = "asset-1",
        roomId: String = "room-1",
        name: String = "Revolving Door",
        code: String = "DOOR-001",
    ) = AssetDto(
        id = id,
        roomId = roomId,
        name = name,
        code = code,
    )
}
