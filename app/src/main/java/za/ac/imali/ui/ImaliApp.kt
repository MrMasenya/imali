package za.ac.imali.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import za.ac.imali.R
import za.ac.imali.util.Finance
import za.ac.imali.util.BudgetColor
import za.ac.imali.vm.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImaliApp(
    state: UiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onLogout: () -> Unit,
    onClearError: () -> Unit,
    onRefresh: () -> Unit,
    onAddTransaction: (String, String, String, String) -> Unit,
    onDeleteTransaction: (String?) -> Unit,
    onAddBudget: (String, String, String) -> Unit,
    onCreateStokvel: (String, String) -> Unit,
    onContributeStokvel: (String, String, String) -> Unit,
    onSaveSettings: (String, String, String, Boolean, Boolean) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            onClearError()
        }
    }

    if (!state.isLoggedIn) {
        AuthScreen(onLogin, onRegister, onGoogleSignIn, state.loading)
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (state.loading) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.dashboard)) })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.PieChart, null) }, label = { Text(stringResource(R.string.reports)) })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Star, null) }, label = { Text(stringResource(R.string.stokvel)) })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                androidx.compose.material3.FloatingActionButton(onClick = { tab = 4 }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_transaction))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (tab) {
                0 -> DashboardScreen(state, onRefresh, onDeleteTransaction)
                1 -> ReportsScreen(state)
                2 -> StokvelScreen(state, onCreateStokvel, onContributeStokvel)
                3 -> SettingsScreen(state, onSaveSettings, onLogout, onAddBudget)
                4 -> AddTransactionScreen(onAddTransaction) { tab = 0 }
            }
        }
    }
}

@Composable
private fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    loading: Boolean,
) {
    var registerMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        if (registerMode) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.password)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (registerMode) onRegister(name, email, password) else onLogin(email, password)
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (registerMode) stringResource(R.string.register) else stringResource(R.string.login))
        }
        OutlinedButton(onClick = { registerMode = !registerMode }, modifier = Modifier.fillMaxWidth()) {
            Text(if (registerMode) stringResource(R.string.login) else stringResource(R.string.register))
        }
        OutlinedButton(onClick = onGoogleSignIn, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.google_sign_in))
        }
    }
}

@Composable
private fun DashboardScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onDelete: (String?) -> Unit,
) {
    LaunchedEffect(Unit) { onRefresh() }
    Text("${stringResource(R.string.balance)}: ${state.currency} ${"%.2f".format(state.balance)}", style = MaterialTheme.typography.headlineSmall)
    if (!state.isOnline) {
        Text(stringResource(R.string.sync_pending), color = MaterialTheme.colorScheme.error)
    }
    if (state.pendingSyncCount > 0) {
        Text(stringResource(R.string.sync_pending), color = MaterialTheme.colorScheme.secondary)
    }
    Text("${stringResource(R.string.day_streak)}: ${state.streakDays}", modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleMedium)
    LazyColumn {
        items(state.transactions.take(20)) { tx ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${tx.category} (${tx.type})")
                        Text(tx.date, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        Text("${if (tx.type == "expense") "-" else "+"}${state.currency} ${tx.amount}")
                        if (!tx.id.isNullOrBlank() && !tx.id.startsWith("local-")) {
                            OutlinedButton(onClick = { onDelete(tx.id) }) { Text("X") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTransactionScreen(onAdd: (String, String, String, String) -> Unit, onDone: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var category by remember { mutableStateOf("Food") }
    var note by remember { mutableStateOf("") }
    val categories = listOf("Food", "Transport", "Rent", "Entertainment", "Salary", "Other")
    Column {
        Text(stringResource(R.string.add_transaction), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount)) }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == "expense", { type = "expense" }, { Text(stringResource(R.string.expense)) })
            FilterChip(type == "income", { type = "income" }, { Text(stringResource(R.string.income)) })
        }
        categories.forEach { cat ->
            FilterChip(category == cat, { category = cat }, { Text(cat) })
        }
        OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note)) }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onAdd(amount, type, category, note); onDone() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun ReportsScreen(state: UiState) {
    val totals = Finance.categoryTotals(state.transactions)
    val budgetStatuses = Finance.enrichBudgets(state.budgets, state.transactions)
    Text(stringResource(R.string.spending_by_category), style = MaterialTheme.typography.titleLarge)
    Canvas(Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp)) {
        val max = totals.values.maxOrNull()?.toFloat() ?: 1f
        var x = 0f
        val barWidth = size.width / (totals.size.coerceAtLeast(1) * 1.5f)
        val canvasHeight = size.height
        totals.forEach { (_, value) ->
            val h = ((value / max) * canvasHeight).toFloat()
            val top = canvasHeight - h
            drawRect(
                color = Color(0xFF059669),
                topLeft = Offset(x, top),
                size = Size(barWidth, h),
            )
            x += barWidth * 1.5f
        }
    }
    totals.forEach { (cat, total) -> Text("$cat: ${state.currency} ${"%.2f".format(total)}") }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.budgets), style = MaterialTheme.typography.titleMedium)
    budgetStatuses.forEach { status ->
        val color = when (status.color) {
            BudgetColor.GREEN -> Color(0xFF059669)
            BudgetColor.AMBER -> Color(0xFFD97706)
            BudgetColor.RED -> Color(0xFFDC2626)
        }
        Text("${status.budget.category} (${status.budget.month})")
        LinearProgressIndicator(progress = { status.progress }, color = color, modifier = Modifier.fillMaxWidth())
        Text("Spent ${"%.2f".format(status.spent)} / ${status.budget.limit}")
    }
}

@Composable
private fun StokvelScreen(
    state: UiState,
    onCreate: (String, String) -> Unit,
    onContribute: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var members by remember { mutableStateOf("") }
    var member by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.create_stokvel)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(members, { members = it }, label = { Text(stringResource(R.string.members_hint)) }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { onCreate(name, members); name = ""; members = "" }) { Text(stringResource(R.string.save)) }
    LazyColumn {
        items(state.stokvels) { s ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium)
                    Text("Members: ${s.members.joinToString()}")
                    s.contributions.forEach { c -> Text("${c.member}: R${c.amount} (${c.date})") }
                    OutlinedButton(onClick = { selectedId = s.id }) { Text(stringResource(R.string.contribute)) }
                }
            }
        }
    }
    if (selectedId != null) {
        OutlinedTextField(member, { member = it }, label = { Text(stringResource(R.string.name)) })
        OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount)) })
        Button(onClick = {
            onContribute(selectedId!!, member, amount)
            member = ""
            amount = ""
            selectedId = null
        }) { Text(stringResource(R.string.contribute)) }
    }
}

@Composable
private fun SettingsScreen(
    state: UiState,
    onSave: (String, String, String, Boolean, Boolean) -> Unit,
    onLogout: () -> Unit,
    onAddBudget: (String, String, String) -> Unit,
) {
    var name by remember(state.userName) { mutableStateOf(state.userName) }
    var currency by remember(state.currency) { mutableStateOf(state.currency) }
    var language by remember(state.language) { mutableStateOf(state.language) }
    var dark by remember(state.darkTheme) { mutableStateOf(state.darkTheme) }
    var notify by remember(state.notifications) { mutableStateOf(state.notifications) }
    var budgetCat by remember { mutableStateOf("Food") }
    var budgetLimit by remember { mutableStateOf("") }
    val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(currency, { currency = it }, label = { Text(stringResource(R.string.currency)) }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(language == "en", { language = "en" }, { Text("English") })
        FilterChip(language == "zu", { language = "zu" }, { Text("isiZulu") })
        FilterChip(language == "af", { language = "af" }, { Text("Afrikaans") })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.dark_theme))
        Switch(dark, { dark = it })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.notifications))
        Switch(notify, { notify = it })
    }
    Button(onClick = { onSave(name, currency, language, dark, notify) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.save))
    }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.budgets), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(budgetCat, { budgetCat = it }, label = { Text(stringResource(R.string.category)) })
    OutlinedTextField(budgetLimit, { budgetLimit = it }, label = { Text(stringResource(R.string.limit)) })
    Button(onClick = { onAddBudget(budgetCat, month, budgetLimit) }) { Text(stringResource(R.string.save)) }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.gamification), style = MaterialTheme.typography.titleMedium)
    Text("${stringResource(R.string.day_streak)}: ${state.streakDays}")
    if (state.badges.isEmpty()) Text(stringResource(R.string.no_badges))
    else state.badges.forEach { Text("🏅 $it") }
    OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.logout))
    }
}
