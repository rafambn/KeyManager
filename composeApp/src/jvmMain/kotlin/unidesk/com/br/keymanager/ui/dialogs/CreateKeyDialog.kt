package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        title = { Text("Create New Key Pair") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = alias, 
                    onValueChange = { alias = it }, 
                    label = { Text("Alias") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = validity, 
                    onValueChange = { if (it.all { char -> char.isDigit() }) validity = it }, 
                    label = { Text("Validity (Days)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Distinguished Name (DN)", style = MaterialTheme.typography.titleSmall)
                Row {
                    TextField(value = cn, onValueChange = { cn = it }, label = { Text("CN (Common Name)") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    TextField(value = c, onValueChange = { c = it }, label = { Text("C (Country)") }, modifier = Modifier.weight(0.5f))
                }
                Row {
                    TextField(value = ou, onValueChange = { ou = it }, label = { Text("OU") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    TextField(value = o, onValueChange = { o = it }, label = { Text("O (Organization)") }, modifier = Modifier.weight(1f))
                }
                Row {
                    TextField(value = l, onValueChange = { l = it }, label = { Text("L (Locality)") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    TextField(value = st, onValueChange = { st = it }, label = { Text("ST (State)") }, modifier = Modifier.weight(1f))
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
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
