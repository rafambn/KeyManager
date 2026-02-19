package com.rafambn.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rafambn.keymanager.keytool.enums.ECCurve
import com.rafambn.keymanager.keytool.enums.KeyAlgorithm
import com.rafambn.keymanager.keytool.enums.SignatureAlgorithm
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateKeyScreen(
    onCreateKey: (String, String, Int, KeyAlgorithm, Int?, SignatureAlgorithm?, ECCurve?) -> Unit,
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

    // Crypto settings state
    var selectedKeyAlgorithm by remember { mutableStateOf(KeyAlgorithm.RSA) }
    var keyAlgorithmExpanded by remember { mutableStateOf(false) }
    var selectedKeySize by remember { mutableStateOf<Int?>(null) }
    var keySizeExpanded by remember { mutableStateOf(false) }
    var selectedSignatureAlgorithm by remember { mutableStateOf<SignatureAlgorithm?>(null) }
    var sigAlgorithmExpanded by remember { mutableStateOf(false) }
    var selectedEcCurve by remember { mutableStateOf<ECCurve?>(null) }
    var ecCurveExpanded by remember { mutableStateOf(false) }

    val availableKeyAlgorithms = remember {
        KeyAlgorithm.signatureAlgorithms().filter { it.isUsable() && !it.deprecated }
    }

    val availableKeySizes = remember(selectedKeyAlgorithm) {
        when (selectedKeyAlgorithm) {
            KeyAlgorithm.RSA, KeyAlgorithm.RSA_PSS -> listOf(2048, 3072, 4096, 8192)
            KeyAlgorithm.DSA -> listOf(1024, 2048, 3072)
            KeyAlgorithm.EC -> listOf(256, 384, 521)
            else -> emptyList()
        }
    }

    val availableSignatureAlgorithms = remember(selectedKeyAlgorithm) {
        SignatureAlgorithm.compatibleWith(selectedKeyAlgorithm).filter { it.isUsable() && !it.deprecated }
    }

    val availableEcCurves = remember { ECCurve.recommendedCurves() }

    val showKeySizeSelector = remember(selectedKeyAlgorithm, selectedEcCurve) {
        selectedKeyAlgorithm.defaultKeySize != -1 &&
            selectedKeyAlgorithm.supportedKeySizes.first != selectedKeyAlgorithm.supportedKeySizes.last &&
            !(selectedKeyAlgorithm == KeyAlgorithm.EC && selectedEcCurve != null)
    }

    val showEcCurveSelector = selectedKeyAlgorithm == KeyAlgorithm.EC

    // Reset dependent fields on algorithm change
    LaunchedEffect(selectedKeyAlgorithm) {
        selectedKeySize = null
        selectedSignatureAlgorithm = null
        selectedEcCurve = null
    }

    Dialog(onDismissRequest = onNavigateBack) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 850.dp),
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

                // Cryptographic Settings section
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(Res.string.crypto_section_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))

                // Key Algorithm dropdown
                ExposedDropdownMenuBox(
                    expanded = keyAlgorithmExpanded,
                    onExpandedChange = { keyAlgorithmExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedKeyAlgorithm.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.key_algorithm_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyAlgorithmExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = keyAlgorithmExpanded,
                        onDismissRequest = { keyAlgorithmExpanded = false }
                    ) {
                        availableKeyAlgorithms.forEach { algo ->
                            DropdownMenuItem(
                                text = { Text(algo.displayName) },
                                onClick = {
                                    selectedKeyAlgorithm = algo
                                    keyAlgorithmExpanded = false
                                }
                            )
                        }
                    }
                }

                // EC Curve dropdown (only for EC)
                if (showEcCurveSelector) {
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = ecCurveExpanded,
                        onExpandedChange = { ecCurveExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEcCurve?.displayName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.ec_curve_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ecCurveExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = ecCurveExpanded,
                            onDismissRequest = { ecCurveExpanded = false }
                        ) {
                            availableEcCurves.forEach { curve ->
                                DropdownMenuItem(
                                    text = { Text(curve.displayName) },
                                    onClick = {
                                        selectedEcCurve = curve
                                        ecCurveExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Key Size dropdown (hidden for fixed-size algos or when EC curve is set)
                if (showKeySizeSelector && availableKeySizes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = keySizeExpanded,
                        onExpandedChange = { keySizeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedKeySize?.toString() ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.key_size_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keySizeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = keySizeExpanded,
                            onDismissRequest = { keySizeExpanded = false }
                        ) {
                            availableKeySizes.forEach { size ->
                                DropdownMenuItem(
                                    text = { Text("$size") },
                                    onClick = {
                                        selectedKeySize = size
                                        keySizeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Signature Algorithm dropdown
                if (availableSignatureAlgorithms.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = sigAlgorithmExpanded,
                        onExpandedChange = { sigAlgorithmExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSignatureAlgorithm?.displayName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.signature_algorithm_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sigAlgorithmExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = sigAlgorithmExpanded,
                            onDismissRequest = { sigAlgorithmExpanded = false }
                        ) {
                            availableSignatureAlgorithms.forEach { sigAlg ->
                                DropdownMenuItem(
                                    text = { Text(sigAlg.displayName) },
                                    onClick = {
                                        selectedSignatureAlgorithm = sigAlg
                                        sigAlgorithmExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // DN Section
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
                                onCreateKey(
                                    alias, dn, validity.toIntOrNull() ?: 365,
                                    selectedKeyAlgorithm, selectedKeySize,
                                    selectedSignatureAlgorithm, selectedEcCurve
                                )
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
