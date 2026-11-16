package com.jonesl7l.carinfg.feature.incident.form

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import com.jonesl7l.carinfg.core.domain.validation.IncidentValidator
import com.jonesl7l.carinfg.core.ui.permissions.PermissionDialogs
import com.jonesl7l.carinfg.core.ui.permissions.rememberPermissionState
import com.jonesl7l.carinfg.core.ui.photo.imageModel
import com.jonesl7l.carinfg.feature.incident.CoverBadge

private val tileSize = 80.dp
private val tileShape = RoundedCornerShape(size = 12.dp)
private val tileSpacing = 10.dp
private val tileBorderWidth = 1.5.dp
private val tileIconSize = 24.dp
private val tileLabelSpacing = 4.dp
private const val DISABLED_ALPHA = 0.38f
private const val MIN_MULTI_PICK = 2
private val imageOnly = PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)

@Composable
fun PhotosSection(
    photos: List<Photo>,
    photosLeft: Int,
    onCameraTarget: () -> Uri,
    onPhotoTake: (Boolean) -> Unit,
    onPick: (List<Uri>) -> Unit,
    onMakeCover: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = onPhotoTake,
    )
    val camera = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        onGrant = { takePicture.launch(onCameraTarget()) },
    )
    val pickMany = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxOf(photosLeft, MIN_MULTI_PICK)),
        onResult = onPick,
    )
    val pickOne = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPick(listOf(it)) }
    }
    val canAdd = photosLeft > 0
    PermissionDialogs(
        state = camera,
        title = stringResource(id = R.string.camera_permission_title),
        rationale = stringResource(id = R.string.camera_permission_rationale),
        settingsRationale = stringResource(id = R.string.camera_permission_settings),
    )
    FormSection(
        title = R.string.section_photos,
        hint = stringResource(
            id = R.string.photos_count,
            photos.size,
            IncidentValidator.MAX_PHOTOS,
        ),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = tileSpacing),
            modifier = Modifier.horizontalScroll(state = rememberScrollState()),
        ) {
            AddPhotoTile(
                icon = R.drawable.ic_camera,
                label = R.string.photos_camera,
                isPrimary = true,
                enabled = canAdd,
                onClick = camera.request,
            )
            AddPhotoTile(
                icon = R.drawable.ic_image,
                label = R.string.photos_gallery,
                isPrimary = false,
                enabled = canAdd,
                onClick = { if (photosLeft > 1) pickMany.launch(imageOnly) else pickOne.launch(imageOnly) },
            )
            photos.forEachIndexed { index, photo ->
                key(photo.id) {
                    PhotoTile(
                        photo = photo,
                        index = index,
                        count = photos.size,
                        onMakeCover = { onMakeCover(index) },
                        onRemove = { onRemove(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPhotoTile(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = tileShape,
        color = if (isPrimary) colors.primaryContainer else colors.surface,
        contentColor = if (isPrimary) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = BorderStroke(
            width = tileBorderWidth,
            color = if (isPrimary) colors.primary else colors.outline,
        ),
        modifier =
            Modifier
                .size(size = tileSize)
                .alpha(alpha = if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = tileLabelSpacing,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(size = tileIconSize),
            )
            Text(
                text = stringResource(id = label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PhotoTile(
    photo: Photo,
    index: Int,
    count: Int,
    onMakeCover: () -> Unit,
    onRemove: () -> Unit,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val isCover = index == 0
    val description =
        stringResource(
            id = if (isCover) R.string.photo_cover_description else R.string.photo_description,
            index + 1,
            count,
        )
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(size = tileSize)
                    .clip(shape = tileShape)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClickLabel = description) { isMenuOpen = true },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size = tileIconSize),
            )
            AsyncImage(
                model = photo.imageModel(),
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCover) CoverBadge(modifier = Modifier.align(alignment = Alignment.BottomStart))
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            if (!isCover) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.photo_make_cover)) },
                    onClick = {
                        isMenuOpen = false
                        onMakeCover()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.photo_remove)) },
                onClick = {
                    isMenuOpen = false
                    onRemove()
                },
            )
        }
    }
}
