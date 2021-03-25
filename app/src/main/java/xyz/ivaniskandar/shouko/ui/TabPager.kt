package xyz.ivaniskandar.shouko.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalPagerApi::class)
@Composable
fun TabPager(
    pageTitles: List<String>,
    content: @Composable ColumnScope.(page: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = pageTitles.size)

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.navigationBarsPadding(bottom = false),
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(Modifier.pagerTabIndicatorOffset(pagerState, tabPositions))
            }
        ) {
            pageTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        // Animate to the selected page when clicked
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title.toUpperCase(Locale.getDefault())) },
                    unselectedContentColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            content = { page -> content.invoke(this@Column, page) }
        )
    }
}

/**
 * From Accompanist's sample
 */
@OptIn(ExperimentalPagerApi::class)
fun Modifier.pagerTabIndicatorOffset(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
): Modifier = composed {
    val targetIndicatorOffset: Dp
    val indicatorWidth: Dp

    val currentTab = tabPositions[pagerState.currentPage]
    val nextTab = tabPositions.getOrNull(pagerState.currentPage + 1)
    if (nextTab != null) {
        // If we have a next tab, lerp between the size and offset
        targetIndicatorOffset = lerp(currentTab.left, nextTab.left, pagerState.currentPageOffset)
        indicatorWidth = lerp(currentTab.width, nextTab.width, pagerState.currentPageOffset)
    } else {
        // Otherwise we just use the current tab/page
        targetIndicatorOffset = currentTab.left
        indicatorWidth = currentTab.width
    }

    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = targetIndicatorOffset)
        .width(indicatorWidth)
}