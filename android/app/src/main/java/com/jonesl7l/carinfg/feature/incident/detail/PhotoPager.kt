package com.jonesl7l.carinfg.feature.incident.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jonesl7l.carinfg.R
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import com.jonesl7l.carinfg.core.ui.photo.imageModel

private val pagerHeight = 220.dp
private val placeholderIconSize = 40.dp
private val overlayPadding = 12.dp
private val pillShape = RoundedCornerShape(size = 13.dp)
private val pillHorizontalPadding = 10.dp
private val pillVerticalPadding = 4.dp
private val dotSize = 8.dp
private val dotSpacing = 6.dp
private val dotsBottomPadding = 14.dp
private const val PILL_ALPHA = 0.75f

@Composable
fun PhotoPager(
    photos: List<Photo>,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = pagerHeight)
                .background(color = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            PhotoPage(
                photo = photos[page],
                description = stringResource(
                    id = if (page == 0) R.string.photo_cover_description else R.string.photo_description,
                    page + 1,
                    photos.size,
                ),
            )
        }
        if (photos.size > 1) {
            PageDots(
                count = photos.size,
                current = pagerState.currentPage,
                modifier =
                    Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .padding(bottom = dotsBottomPadding),
            )
            Text(
                text = stringResource(
                    id = R.string.photo_page,
                    pagerState.currentPage + 1,
                    photos.size,
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier =
                    Modifier
                        .align(alignment = Alignment.BottomEnd)
                        .padding(all = overlayPadding)
                        .background(
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = PILL_ALPHA),
                            shape = pillShape,
                        )
                        .padding(
                            horizontal = pillHorizontalPadding,
                            vertical = pillVerticalPadding,
                        ),
            )
        }
    }
}

@Composable
private fun PhotoPage(
    photo: Photo,
    description: String,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_image),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size = placeholderIconSize),
        )
        AsyncImage(
            model = photo.imageModel(),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PageDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = dotSpacing),
        modifier = modifier,
    ) {
        repeat(times = count) { index ->
            Box(
                modifier =
                    Modifier
                        .size(size = dotSize)
                        .background(
                            color = if (index == current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        ),
            )
        }
    }
}
