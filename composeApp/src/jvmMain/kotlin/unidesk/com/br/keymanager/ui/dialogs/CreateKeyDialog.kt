package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.*

@Composable
fun CreateKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (alias: String, dn: String, validity: Int) -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var validity by remember { mutableStateOf("365") }
    
    // DN Fields
    var cn by remember { mutableStateOf("") }
    var ou by remember { mutableStateOf("") }
    var o by remember { mutableStateOf("") }
    var l by remember { mutableStateOf("") }
    var st by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_key_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = alias, 
                    onValueChange = { alias = it }, 
                    label = { Text(stringResource(Res.string.alias_label)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = validity, 
                    onValueChange = { if (it.all { char -> char.isDigit() }) validity = it }, 
                    label = { Text(stringResource(Res.string.validity_label)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.dn_section_title), style = MaterialTheme.typography.titleSmall)
                Row {
                    TextField(value = cn, onValueChange = { cn = it }, label = { Text(stringResource(Res.string.cn_label)) }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    TextField(value = c, onValueChange = { c = it }, label = { Text(stringResource(Res.string.country_label)) }, modifier = Modifier.weight(0.5f))
                }
                Row {
                    TextField(value = ou, onValueChange = { ou = it }, label = { Text(stringResource(Res.string.ou_label)) }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    TextField(value = o, onValueChange = { o = it }, label = { Text(stringResource(Res.string.org_label)) }, modifier = Modifier.weight(1f))
                }
                Row {
                    TextField(value = l, onValueChange = { l = it }, label = { Text(stringResource(Res.string.locality_label)) }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    TextField(value = st, onValueChange = { st = it }, label = { Text(stringResource(Res.string.state_label)) }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
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
                         onConfirm(alias, dn, validity.toIntOrNull() ?: 365)
                    }
                }
            ) {
                Text(stringResource(Res.string.create_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
