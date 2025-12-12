package com.app.eroum.ethernettestapp.ui.ethernet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.eroum.ethernettestapp.MainViewModel
import com.app.eroum.ethernettestapp.ui.theme.EthernetTestAppTheme
import kotlinx.coroutines.launch

/*
 이더넷 테스트 화면
 */
@Composable
fun EthernetTestScreen(
    viewModel: MainViewModel,
) {
    /** Field */
    val localIp = viewModel.localIp
    val isPinging = viewModel.isPinging
    val logs = viewModel.pingLogs

    var targetIp by remember { mutableStateOf("192.168.10.50") }

    val outerScrollState = rememberScrollState()
    val logListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 새 로그 생기면 로그 영역 안에서 맨 아래로 스크롤
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            coroutineScope.launch {
                logListState.animateScrollToItem(logs.lastIndex)
            }
        }
    }

    /** UI */
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 상단 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Ethernet Test App",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 본문
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(outerScrollState)
        ) {
            // 현재 IP 영역
            Text(
                text = "Current Ethernet IP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localIp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { viewModel.loadEthernetIp() }) {
                    Text("Refresh")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Ping Test",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Target IP 입력
            OutlinedTextField(
                value = targetIp,
                onValueChange = { targetIp = it },
                label = { Text("Target IP for ping") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.onPingButtonClick(targetIp) }
                ) {
                    Text(if (isPinging) "Stop Ping" else "Start Ping")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { viewModel.pingLogs.clear() }) {
                    Text("Clear Log")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Ping Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))

            // 로그 출력 영역 (고정 높이 + 내부 스크롤 + 스크롤바)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)   // 로그창 높이 고정
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(4.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No logs yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 실제 로그 스크롤 영역
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 6.dp),  // 스크롤바 자리 조금 띄우기
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            itemsIndexed(logs) { index, line ->
                                Text(
                                    text = line.text,
                                    color = line.color.takeOrElse { MaterialTheme.colorScheme.onSurface },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (index != logs.lastIndex) {
                                    Spacer(Modifier.height(2.dp))
                                }
                            }
                        }

                        // 스크롤바 그리기
                        LogScrollbar(
                            listState = logListState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
    }
}




/**
 * 오른쪽에 간단한 수직 스크롤바
 */
@Composable
private fun LogScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 4.dp
) {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems == 0) return

    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    val firstVisibleIndex = visibleItems.first().index
    val visibleCount = visibleItems.size.coerceAtLeast(1)

    // 아주 간단한 비율 기반 계산 (아이템 높이가 다 달라도 대충 감 잡는 용도)
    val thumbHeightFraction = (visibleCount.toFloat() / totalItems.toFloat())
        .coerceIn(0.1f, 1f) // 너무 작아지지 않게 최소값 0.1
    val scrollProgress = (firstVisibleIndex.toFloat() / (totalItems - visibleCount).coerceAtLeast(1))
        .coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(trackWidth)
    ) {
        val trackHeight = maxHeight
        val thumbHeight = trackHeight * thumbHeightFraction
        val thumbOffset = (trackHeight - thumbHeight) * scrollProgress

        // 연한 트랙
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray.copy(alpha = 0.2f))
        )

        // 실제 thumb
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(thumbHeight)
                .offset(y = thumbOffset)
                .background(Color.Gray, RoundedCornerShape(2.dp))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraPreview() {
    EthernetTestScreen(viewModel = MainViewModel())
}