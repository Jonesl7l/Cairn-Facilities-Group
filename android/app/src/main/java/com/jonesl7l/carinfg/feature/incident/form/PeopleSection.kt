package com.jonesl7l.carinfg.feature.incident.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.person.InjuryDetails
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.person.PersonRole
import com.jonesl7l.carinfg.core.domain.model.person.PersonType
import com.jonesl7l.carinfg.core.domain.model.person.Treatment
import com.jonesl7l.carinfg.core.domain.validation.FieldError
import com.jonesl7l.carinfg.core.domain.validation.IncidentField
import com.jonesl7l.carinfg.core.domain.validation.PersonField
import com.jonesl7l.carinfg.feature.incident.personTypeLabel
import com.jonesl7l.carinfg.feature.incident.roleLabel
import com.jonesl7l.carinfg.feature.incident.treatmentLabel

private val cardPadding = 16.dp
private val cardSpacing = 14.dp
private val touchTargetHeight = 56.dp
private val iconSize = 20.dp
private val personTypes =
    listOf(
        PersonType.Employee,
        PersonType.Contractor,
        PersonType.Visitor,
        PersonType.MemberOfPublic,
    )
private val roles =
    listOf(
        PersonRole.Injured,
        PersonRole.Witness,
        PersonRole.Involved,
    )
private val treatments =
    listOf(
        Treatment.None,
        Treatment.FirstAid,
        Treatment.MedicalTreatment,
        Treatment.Hospital,
    )

@Composable
fun PeopleSection(
    fields: IncidentFormFields,
    errors: List<FieldError>,
    remainingErrors: List<FieldError>,
    canAddPerson: Boolean,
    onChange: ((IncidentFormFields) -> IncidentFormFields) -> Unit,
    onAddPerson: () -> Unit,
    onRemovePerson: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormSection(
        title = R.string.section_people,
        modifier = modifier,
        errorCount = errors.count { it.field is IncidentField.Person },
        isComplete = fields.persons.isNotEmpty() && remainingErrors.none { it.field is IncidentField.Person },
    ) {
        fields.persons.forEachIndexed { index, person ->
            key(person.id) {
                PersonCard(
                    index = index,
                    person = person,
                    errors = errors,
                    onPersonChange = { change ->
                        onChange {
                            it.copy(
                                persons = it.persons.mapIndexed { personIndex, current ->
                                    if (personIndex ==
                                        index
                                    ) {
                                        change(current)
                                    } else {
                                        current
                                    }
                                },
                            )
                        }
                    },
                    onRemove = { onRemovePerson(index) },
                )
            }
        }
        if (canAddPerson) {
            OutlinedButton(
                onClick = onAddPerson,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = touchTargetHeight),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(size = iconSize),
                )
                Text(text = stringResource(id = R.string.add_person))
            }
        }
    }
}

@Composable
private fun PersonCard(
    index: Int,
    person: Person,
    errors: List<FieldError>,
    onPersonChange: ((Person) -> Person) -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(all = cardPadding),
            verticalArrangement = Arrangement.spacedBy(space = cardSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.person_title, index + 1),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(weight = 1f),
                )
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.heightIn(min = touchTargetHeight),
                ) {
                    Text(text = stringResource(id = R.string.remove_person))
                }
            }
            FormTextField(
                label = R.string.label_person_name,
                value = person.name,
                onValueChange = { name -> onPersonChange { it.copy(name = name) } },
                error =
                    errors.personReason(
                        index = index,
                        field = PersonField.NAME,
                    ),
            )
            FormDropdown(
                label = R.string.label_person_type,
                selectedText = person.type?.let { personTypeLabel(type = it) },
                options =
                    personTypes.map { type ->
                        FormOption(
                            label = personTypeLabel(type = type),
                            onSelect = { onPersonChange { it.copy(type = type) } },
                        )
                    },
                error =
                    errors.personReason(
                        index = index,
                        field = PersonField.TYPE,
                    ),
            )
            when (person.type) {
                PersonType.Employee -> {
                    FormTextField(
                        label = R.string.label_department,
                        value = person.department.orEmpty(),
                        onValueChange = { text -> onPersonChange { it.copy(department = text.ifEmpty { null }) } },
                    )
                    FormTextField(
                        label = R.string.label_position,
                        value = person.position.orEmpty(),
                        onValueChange = { text -> onPersonChange { it.copy(position = text.ifEmpty { null }) } },
                    )
                }

                PersonType.Contractor -> FormTextField(
                    label = R.string.label_company,
                    value = person.company.orEmpty(),
                    onValueChange = { text -> onPersonChange { it.copy(company = text.ifEmpty { null }) } },
                )

                else -> Unit
            }
            FormChoices(
                label = R.string.label_person_role,
                choices = roles,
                selected = person.role,
                choiceLabel = { roleLabel(role = it) },
                onSelect = { role -> onPersonChange { it.copy(role = role) } },
                error =
                    errors.personReason(
                        index = index,
                        field = PersonField.ROLE,
                    ),
            )
            FormTextField(
                label = R.string.label_contact_number,
                value = person.contactNumber.orEmpty(),
                onValueChange = { text -> onPersonChange { it.copy(contactNumber = text.ifEmpty { null }) } },
                keyboardType = KeyboardType.Phone,
            )
            person.injury?.let { injury ->
                InjuryFields(
                    index = index,
                    injury = injury,
                    errors = errors,
                    onInjuryChange = { change -> onPersonChange { it.copy(injury = it.injury?.let(change)) } },
                )
            }
        }
    }
}

@Composable
private fun InjuryFields(
    index: Int,
    injury: InjuryDetails,
    errors: List<FieldError>,
    onInjuryChange: ((InjuryDetails) -> InjuryDetails) -> Unit,
) {
    FormTextField(
        label = R.string.label_injury_nature,
        value = injury.nature,
        onValueChange = { text -> onInjuryChange { it.copy(nature = text) } },
        error =
            errors.personReason(
                index = index,
                field = PersonField.INJURY_NATURE,
            ),
    )
    FormTextField(
        label = R.string.label_body_part,
        value = injury.bodyPart,
        onValueChange = { text -> onInjuryChange { it.copy(bodyPart = text) } },
        error =
            errors.personReason(
                index = index,
                field = PersonField.INJURY_BODY_PART,
            ),
    )
    FormDropdown(
        label = R.string.label_treatment,
        selectedText = injury.treatment?.let { treatmentLabel(treatment = it) },
        options =
            treatments.map { treatment ->
                FormOption(
                    label = treatmentLabel(treatment = treatment),
                    onSelect = { onInjuryChange { it.copy(treatment = treatment) } },
                )
            },
        error =
            errors.personReason(
                index = index,
                field = PersonField.INJURY_TREATMENT,
            ),
    )
    FormTextField(
        label = R.string.label_injury_notes,
        value = injury.notes.orEmpty(),
        onValueChange = { text -> onInjuryChange { it.copy(notes = text.ifEmpty { null }) } },
        singleLine = false,
    )
}

private fun List<FieldError>.personReason(
    index: Int,
    field: PersonField,
) = reasonFor(
    field =
        IncidentField.Person(
            index = index,
            field = field,
        ),
)
