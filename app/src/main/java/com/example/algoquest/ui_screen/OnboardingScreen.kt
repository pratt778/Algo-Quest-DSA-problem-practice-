// OnboardingScreen.kt
package com.example.algoquest

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algoquest.R
import com.example.algoquest.model.OnboardingPage
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            imageResId = R.drawable.ic_launcher_foreground, // ← Use custom if available, or keep ic_launcher_foreground
            title = stringResource(R.string.onboarding_title_1),
            description = stringResource(R.string.onboarding_desc_1)
        ),
        OnboardingPage(
            imageResId = R.drawable.ic_launcher_foreground,
            title = stringResource(R.string.onboarding_title_2),
            description = stringResource(R.string.onboarding_desc_2)
        ),
        OnboardingPage(
            imageResId = R.drawable.ic_launcher_foreground,
            title = stringResource(R.string.onboarding_title_3),
            description = stringResource(R.string.onboarding_desc_3)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Dynamic accent based on page (optional but nice)
    val currentAccent = when (pagerState.currentPage) {
        0 -> Color(0xFF4F46E5) // indigo-600
        1 -> Color(0xFFEC4899) // pink-500
        else -> Color(0xFF10B981) // emerald-500
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo or App Name (optional)
            Text(
                text = "AlgoQuest",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = currentAccent,
                modifier = Modifier.alpha(0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 24.dp)
            ) { page ->
                val item = pages[page]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Illustration
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        currentAccent.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(160.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Title
                    Text(
                        text = item.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 34.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    Text(
                        text = item.description,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            // Page Indicator (Dots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) currentAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onFinish() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .alpha(0.8f)
                ) {
                    Text(
                        "Skip",
                        color = currentAccent,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage == pages.lastIndex) {
                            onFinish()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .width(140.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (pagerState.currentPage == pages.lastIndex) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}