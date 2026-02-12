package com.rafambn.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateKeyScreen(
    onCreateKey: (String, String, Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var validity by remember { mutableStateOf("365") }

    var cn by remember { mutableStateOf("") }
    var ou by remember { mutableStateOf("") }
    var o by remember { mutableStateOf("") }
    var l by remember { mutableStateOf("") }
    var st by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onNavigateBack) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 700.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.create_key_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(24.dp))

                TextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(stringResource(Res.string.alias_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = validity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) validity = it },
                    label = { Text(stringResource(Res.string.validity_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(Res.string.dn_section_title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row {
                    TextField(
                        value = cn,
                        onValueChange = { cn = it },
                        label = { Text(stringResource(Res.string.cn_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    TextField(
                        value = c,
                        onValueChange = { c = it },
                        label = { Text(stringResource(Res.string.country_label)) },
                        modifier = Modifier.weight(0.5f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextField(
                        value = ou,
                        onValueChange = { ou = it },
                        label = { Text(stringResource(Res.string.ou_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    TextField(
                        value = o,
                        onValueChange = { o = it },
                        label = { Text(stringResource(Res.string.org_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextField(
                        value = l,
                        onValueChange = { l = it },
                        label = { Text(stringResource(Res.string.locality_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    TextField(
                        value = st,
                        onValueChange = { st = it },
                        label = { Text(stringResource(Res.string.state_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dn = buildString {
                                if (cn.isNotBlank()) append("CN=$cn,")
                                if (ou.isNotBlank()) append("OU=$ou,")
                                if (o.isNotBlank()) append("O=$o,")
                                if (l.isNotBlank()) append("L=$l,")
                                if (st.isNotBlank()) append("ST=$st,")
                                if (c.isNotBlank()) append("C=$c,")
                            }.removeSuffix(",")

                            if (alias.isNotBlank() && dn.isNotBlank()) {
                                onCreateKey(alias, dn, validity.toIntOrNull() ?: 365)
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.create_button))
                    }
                }
            }
        }
    }
}