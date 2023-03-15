package eu.depau.etchdroid.ui.utils

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ModalBottomSheet has a bug and it doesn't call onDismissRequest when it's dismissed by
 * dragging the sheet down by the handle. This sheet state wrapper fixes that by dismissing the
 * sheet after a delay when a change to the hidden state is requested.
 */

@Composable
@ExperimentalMaterial3Api
fun rememberPorkedAroundSheetState(
    onDismissRequest: () -> Unit,
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
): SheetState {
    val scope = rememberCoroutineScope()
    return rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded, confirmValueChange = { value ->
        val upstreamResult = confirmValueChange(value)
        if (upstreamResult && value == SheetValue.Hidden) {
            scope.launch {
                delay(100)
                onDismissRequest()
            }
        }
        upstreamResult
    })
}