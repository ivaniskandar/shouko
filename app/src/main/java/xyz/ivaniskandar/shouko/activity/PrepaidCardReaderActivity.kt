package xyz.ivaniskandar.shouko.activity

import android.app.PendingIntent
import android.content.ClipData
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import xyz.ivaniskandar.shouko.ui.theme.green
import java.nio.ByteBuffer
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class PrepaidCardReaderActivity : ComponentActivity() {

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }

    private val viewModel by viewModels<PrepaidCardReaderViewModel>()

    private var currentCard: Card?
        inline get() = viewModel.currentCard
        inline set(value) {
            viewModel.currentCard = value
        }

    private var isLoading: Boolean
        inline get() = viewModel.isLoading
        inline set(value) {
            viewModel.isLoading = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ShoukoM3Theme {
                val behavior = TopAppBarDefaults.pinnedScrollBehavior()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                if (currentCard != null) {
                                    Text(text = currentCard!!.name)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = ::finish) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            },
                            scrollBehavior = behavior,
                        )
                    },
                ) { innerPadding ->
                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(behavior.nestedScrollConnection),
                        contentPadding = innerPadding,
                    ) {
                        if (isLoading) {
                            item {
                                CircularProgressIndicator()
                            }
                            item {
                                Text(
                                    text = stringResource(R.string.prepaid_card_reader_loading),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            val c = currentCard
                            if (c != null) {
                                item {
                                    CardInfo(
                                        balance = c.balance,
                                        cardNo = c.cardNo,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                                if (c.transactionHistory.isNotEmpty()) {
                                    item {
                                        TransactionHistoryHeader()
                                    }
                                    c.transactionHistory
                                        .sortedByDescending { it.timeMillis }
                                        .take(10)
                                        .groupBy {
                                            Instant.ofEpochMilli(it.timeMillis)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                        }
                                        .forEach { (date, list) ->
                                            item {
                                                val formattedDate = remember(date) {
                                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
                                                }
                                                TransactionGroupHeader(
                                                    title = formattedDate,
                                                    modifier = Modifier,
                                                )
                                            }
                                            items(list) {
                                                TransactionHistoryItem(
                                                    type = it.type,
                                                    amount = it.amount,
                                                    newBalance = it.newBalance,
                                                    timeMillis = it.timeMillis,
                                                )
                                            }
                                            item {
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                }
                            } else {
                                item {
                                    Text(
                                        text = stringResource(R.string.prepaid_card_reader_hint),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val pendingIntent = PendingIntent
            .getActivity(this, 0, Intent(this, this::class.java), PendingIntent.FLAG_MUTABLE)
        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java) ?: return
        handleTag(tag)
    }

    private fun handleTag(tag: Tag) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                IsoDep.get(tag)?.use { isoDep ->
                    currentCard = null
                    isLoading = true
                    val cardResult = runCatching {
                        isoDep.connect()
                        getCard(isoDep)!!
                    }
                    cardResult
                        .onSuccess { currentCard = it }
                        .onFailure { it.printStackTrace() }
                }
                isLoading = false
            }
        }
    }
}

private class PrepaidCardReaderViewModel : ViewModel() {
    var currentCard: Card? by mutableStateOf(null)
    var isLoading: Boolean by mutableStateOf(false)
}

private interface Card {
    val name: String
    val cardNo: String
    val balance: Int
    val transactionHistory: List<TransactionHistory>
}

private enum class TransactionType {
    IN,
    OUT,
}

private data class TransactionHistory(
    val type: TransactionType,
    val amount: Int,
    val newBalance: Int?,
    val timeMillis: Long,
)

@OptIn(ExperimentalStdlibApi::class)
private class EMoneyCard(private val isoDep: IsoDep) : Card {
    override val name: String = "e-money"
    override val cardNo: String
    override val balance: Int
    override val transactionHistory: List<TransactionHistory>

    private val cardInfoCommand = byteArrayOf(0, -77, 0, 0, 63)
    private val balanceCommand = byteArrayOf(0, -75, 0, 0, 10)
    private val txHistoryCommand = byteArrayOf(0, -78, 0, 0, 40)

    init {
        var response = isoDep.transceive(cardInfoCommand)
        val info = if (response.validate()) {
            response.toHexString()
        } else {
            null
        }
        cardNo = info?.substring(0, 16) ?: "0000000000000000"

        response = isoDep.transceive(balanceCommand)
        balance = if (response.validate()) {
            response.extractValue(0)
        } else {
            -1
        }

        transactionHistory = buildList {
            if (info?.substring(36, 38)?.toInt(16) == 3) {
                addAll(historyGen1())
            }
        }
    }

    private fun historyGen1(): List<TransactionHistory> {
        val list = mutableListOf<TransactionHistory>()
        val clone = txHistoryCommand.clone()
        var i = 1
        var validate: Boolean
        do {
            clone[2] = i.toByte()
            val response = isoDep.transceive(clone)
            validate = response.validate()
            if (!validate) continue
            val txTime = response.sliceArray(0 until 6).toHexString()
                .let { SimpleDateFormat("ddMMyyHHmmss", Locale.US).parse(it)!!.time }
            val type = if (response[15].toInt() == 80) TransactionType.IN else TransactionType.OUT
            val amount = response.extractValue(16)
            val newBalance = response.extractValue(20)
            list.add(TransactionHistory(type, amount, newBalance, txTime))
            i++
        } while (validate)
        return list
    }

    private fun ByteArray.extractValue(startIndex: Int): Int {
        return (0..2).sumOf {
            get(startIndex + it).toInt()
                .and(255)
                .shl(it.times(8))
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class FlazzCard(private val isoDep: IsoDep) : Card {
    override val name: String = "Flazz"
    override val cardNo: String
    override val balance: Int
    override val transactionHistory: List<TransactionHistory>

    private val cardInfoCommand = byteArrayOf(0, -80, -127, 0, -114)
    private val cardGenCheckCommand = byteArrayOf(0, -80, -127, 0, 0)
    private val balanceCommand = byteArrayOf(-128, 50, 0, 3, 4, 0, 0, 0, 0)
    private val historyTypeACommands = listOf(
        byteArrayOf(0, -80, -123, 0, 120),
        byteArrayOf(0, -80, -124, 0, -16),
    )
    private val historyTypeBCommands = listOf(
        byteArrayOf(0, -80, -119, 0, 64),
        byteArrayOf(-112, 50, 3, 0, 1, 0, 32),
    )

    init {
        var response = isoDep.transceive(cardInfoCommand)
        val info = if (response.validate()) {
            String(response)
        } else {
            null
        }
        cardNo = info?.substring(info.indexOf(';').inc(), info.indexOf('=')) ?: "0000000000000000"

        response = isoDep.transceive(balanceCommand)
        balance = if (response.validate()) {
            ByteBuffer.wrap(response).getInt()
        } else {
            -1
        }

        transactionHistory = buildList {
            if (isoDep.transceive(cardGenCheckCommand).validate()) {
                addAll(historyGen2())
            }
        }.distinctBy { it.timeMillis }
    }

    private fun historyGen2(): List<TransactionHistory> {
        val list = mutableListOf<TransactionHistory>()
        val strA = StringBuilder()
        historyTypeACommands.forEach { bytes ->
            for (i in 0..4) {
                bytes[3] = (i * 60).toByte()
                val response = isoDep.transceive(bytes)
                if (!response.validate()) {
                    break
                }
                strA.append(response.getRawTransactionData())
            }
        }
        list += TxHistoryProcessor(strA.toString(), 120).getTransactionHistory()

        val strB = StringBuilder()
        historyTypeBCommands.forEach { bytes ->
            val replaceIndex = bytes.indexOfLast { it.toInt() == 0 }
            for (i in 0..255) {
                bytes[replaceIndex] = i.toByte()
                val response = isoDep.transceive(bytes)
                if (!response.validate()) {
                    break
                }
                strB.append(response.getRawTransactionData())
            }
        }
        list += TxHistoryProcessor(strB.toString(), 64).getTransactionHistory()

        return list
    }

    private fun ByteArray.getRawTransactionData(): String {
        return this
            .slice(0 until lastIndex.dec())
            .joinToString("") { it.toHexString() }
    }

    private class TxHistoryProcessor(stringData: String, chunkSize: Int) {
        private val chunks = mutableMapOf<Int, String>()

        private val chunkType = when (chunkSize) {
            120 -> ChunkType.BIG
            64 -> ChunkType.SMALL
            else -> throw IllegalArgumentException("Invalid chunk size")
        }

        private enum class ChunkType {
            BIG,
            SMALL,
        }

        init {
            val length = stringData.length / chunkSize
            for (i in 0 until length) {
                val startIndex = i * chunkSize
                val endIndex = startIndex + chunkSize
                val substring = stringData.substring(startIndex, endIndex)
                val time = when (chunkType) {
                    ChunkType.BIG -> substring.substring(76, 84)
                    ChunkType.SMALL -> substring.substring(8, 18)
                }
                if (!chunks.containsKey(i) && time.toLong(16) > 0) {
                    chunks[i] = substring
                }
            }
        }

        private fun getStringChunk(index: Int): String {
            return chunks[index]!!
        }

        private fun getType(index: Int): TransactionType {
            val value = getStringChunk(index)
            val isOut = when (chunkType) {
                ChunkType.BIG -> value.substring(0, 4).toInt(16) == 1024
                ChunkType.SMALL -> value.substring(0, 2).toInt(16) == 4
            }
            return if (isOut) TransactionType.OUT else TransactionType.IN
        }

        private fun getAmount(index: Int): Int {
            val value = getStringChunk(index)
            return when (chunkType) {
                ChunkType.BIG -> {
                    value.substring(12, 18).toInt(16)
                }
                ChunkType.SMALL -> {
                    val s = value.substring(2, 8).toInt(16)
                    if (getType(index) == TransactionType.OUT) {
                        16777216 - s
                    } else {
                        s
                    }
                }
            }
        }

        private fun getNewBalance(index: Int): Int {
            val value = getStringChunk(index)
            return when (chunkType) {
                ChunkType.BIG -> value.substring(4, 10).toInt(16)
                ChunkType.SMALL -> {
                    val s = value.substring(44, 50).toInt(16)
                    if (getType(index) == TransactionType.OUT) {
                        s - 9000
                    } else {
                        s + getAmount(index)
                    }
                }
            }
        }

        private fun getTimeMillis(index: Int): Long {
            val substring = getStringChunk(index)
            val key = when (chunkType) {
                ChunkType.BIG -> substring.substring(76, 84).toLong(16)
                ChunkType.SMALL -> substring.substring(8, 16).toLong(16)
            }
            return LocalDateTime.now()
                .truncatedTo(ChronoUnit.DAYS)
                .withDayOfYear(1)
                .withYear(1980)
                .plusSeconds(key)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        fun getTransactionHistory(): List<TransactionHistory> {
            return chunks.keys.map {
                TransactionHistory(
                    type = getType(it),
                    amount = getAmount(it),
                    newBalance = getNewBalance(it),
                    timeMillis = getTimeMillis(it),
                )
            }
        }
    }
}

private fun getCard(isoDep: IsoDep): Card? {
    var response = isoDep.transceive(eMoneyIdCommand)
    if (response.validate()) {
        return EMoneyCard(isoDep)
    }

    response = isoDep.transceive(flazzIdCommand)
    if (response.validate()) {
        return FlazzCard(isoDep)
    }

    return null
}

private fun ByteArray.validate(): Boolean {
    return get(lastIndex - 1).toInt() == -112 && get(lastIndex).toInt() == 0
}

private val eMoneyIdCommand = byteArrayOf(0, -92, 4, 0, 8, 0, 0, 0, 0, 0, 0, 0, 1)
private val flazzIdCommand = byteArrayOf(0, -92, 4, 0, 11, -96, 0, 0, 0, 24, 15, 0, 0, 1, -128, 1, 0)

private val numberFormat = NumberFormat.getNumberInstance(Locale.GERMAN)
private const val currencyRupiah = "Rp"

@Composable
private fun CardInfo(
    balance: Int,
    cardNo: String,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Card(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val currencyStyle = MaterialTheme.typography.titleLarge
            val currencyColor = MaterialTheme.colorScheme.onSurfaceVariant
            val formattedBalance = remember(balance) {
                buildAnnotatedString {
                    withStyle(style = currencyStyle.toSpanStyle().copy(color = currencyColor)) {
                        append(currencyRupiah)
                    }
                    append(numberFormat.format(balance))
                }
            }
            val formattedCardNo = remember(cardNo) { cardNo.chunked(4).joinToString(" ") }
            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                modifier = Modifier.padding(16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedCardNo,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(cardNo, cardNo)))
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CardInfoPreview() {
    ShoukoM3Theme {
        Surface {
            CardInfo(
                25000,
                "1234567890123456",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun TransactionHistoryHeader(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.prepaid_card_history_title),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun TransactionGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun TransactionHistoryItem(
    type: TransactionType,
    amount: Int,
    newBalance: Int?,
    timeMillis: Long,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        leadingContent = {
            Surface(
                color = when (type) {
                    TransactionType.IN -> MaterialTheme.colorScheme.primary
                    TransactionType.OUT -> MaterialTheme.colorScheme.error
                },
                shape = CircleShape,
                modifier = Modifier.size(36.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = currencyRupiah)
                }
            }
        },
        headlineContent = {
            Row {
                Text(
                    text = when (type) {
                        TransactionType.IN -> stringResource(R.string.prepaid_card_topup)
                        TransactionType.OUT -> stringResource(R.string.prepaid_card_payment)
                    },
                    modifier = Modifier.weight(1f),
                )
                val formattedNumber = remember(amount) { numberFormat.format(amount) }
                Text(
                    text = when (type) {
                        TransactionType.IN -> "+ $currencyRupiah$formattedNumber"
                        TransactionType.OUT -> "$currencyRupiah$formattedNumber"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    color = when (type) {
                        TransactionType.IN -> MaterialTheme.colorScheme.green
                        TransactionType.OUT -> LocalContentColor.current
                    },
                )
            }
        },
        supportingContent = {
            val formattedTime = remember(timeMillis) {
                SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(timeMillis)
            }
            Row {
                Text(
                    text = formattedTime,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (newBalance != null) {
                    val formattedNewBalance = remember(newBalance) { numberFormat.format(newBalance) }
                    Text(
                        text = "$currencyRupiah$formattedNewBalance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun TransactionHistoryPreview() {
    ShoukoM3Theme {
        Surface {
            Column {
                TransactionHistoryHeader()
                TransactionGroupHeader(title = "17 Aug 1945")
                TransactionHistoryItem(
                    type = TransactionType.IN,
                    amount = 10000,
                    newBalance = 15000,
                    timeMillis = System.currentTimeMillis(),
                )
                TransactionHistoryItem(
                    type = TransactionType.OUT,
                    amount = 5000,
                    newBalance = 10000,
                    timeMillis = System.currentTimeMillis(),
                )
                TransactionGroupHeader(title = "11 Jan 2011")
                TransactionHistoryItem(
                    type = TransactionType.IN,
                    amount = 20000,
                    newBalance = null,
                    timeMillis = System.currentTimeMillis(),
                )
            }
        }
    }
}
