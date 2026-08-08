package com.yandex.school.casheye.core.designsystem.component.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.R

@Composable
fun EditorOptionSelectionContent(
    title: String,
    options: List<EditorOption>,
    selectedId: Int?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (EditorOption) -> Unit,
    modifier: Modifier = Modifier,
    emptyContent: @Composable () -> Unit = {},
) {
    val visibleOptions = remember(options, query) { options.filter { it.label.contains(query, ignoreCase = true) } }
    val listState = rememberLazyListState()
    val gestureCoordinator = rememberSheetListGestureCoordinator(listState)
    Column(modifier = modifier.fillMaxWidth()) {
        EditorSheetTitle(title)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.finance_editor_find_category)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_editor_search),
                    contentDescription = null,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 20.dp, end = 20.dp),
        )
        if (visibleOptions.isEmpty()) {
            emptyContent()
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .nestedScroll(gestureCoordinator),
                state = listState,
                flingBehavior = gestureCoordinator,
                overscrollEffect = null,
            ) {
                itemsIndexed(visibleOptions, key = { _, option -> option.id }) { index, option ->
                    EditorSelectionRow(
                        emoji = option.emoji,
                        title = option.label,
                        subtitle = null,
                        selected = option.id == selectedId,
                        isLast = index == visibleOptions.lastIndex,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}
