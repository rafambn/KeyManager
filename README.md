# KeyManager

<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="composeApp/src/jvmMain/composeResources/drawable/app_icon.xml">
    <img alt="KeyManager Icon" src="composeApp/src/jvmMain/composeResources/drawable/app_icon.xml" width="120">
  </picture>
</div>

**Modern Desktop Application for Java Keystore Management**

[![Build Status](https://img.shields.io/github/actions/workflow/status/<username>/KeyManager/build.yml?branch=master)](https://github.com/<username>/KeyManager/actions)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-blue)](https://github.com/<username>/KeyManager/releases)
[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/<username>/KeyManager/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple)](https://kotlinlang.org)

## Overview

KeyManager is a Kotlin Multiplatform Desktop application that provides a modern, user-friendly interface for managing Java keystores (.jks, .pkcs12, .jceks files). It wraps the Java `keytool` CLI with an intuitive Compose Multiplatform UI, making keystore operations accessible to developers and system administrators.

## Features

### Keystore Management
- **Open multiple keystores** simultaneously with easy switching
- **Format support**: PKCS12, JKS, and JCEKS keystores
- **Recent keystores** tracking for quick access
- **Password-protected** keystore operations

### Key Operations
- **Generate cryptographic keys** with support for:
  - RSA (1024, 2048, 3072, 4096 bits)
  - EC (P-256, P-384, P-521, secp256k1)
  - DSA (1024, 2048 bits)
  - EdDSA (Ed25519, Ed448)
  - Quantum-resistant algorithms (ML-DSA, ML-KEM)
- **Import/Export certificates** in multiple formats
- **Certificate signing requests** (CSR) generation
- **Certificate operations**: View details, validate expiry, chain management
- **Bulk operations**: Move multiple keys between keystores

## Installation

Download the installer for your architecture from the [Releases](https://github.com/<username>/KeyManager/releases) page:

- **Windows**: `keymanager-windows-x64.msi` (requires Windows 10 or later)
- **macOS**:
  - `keymanager-macos-x64.dmg` (Intel processors)
  - `keymanager-macos-arm64.dmg` (Apple Silicon)
- **Linux**:
  - `keymanager-linux-x64.deb` (x86-64)
  - `keymanager-linux-arm64.deb` (ARM64)

## Contributing

We welcome contributions! Whether you want to report bugs, suggest features, or submit code improvements:

1. **Report Issues**: Open a [GitHub Issue](https://github.com/<username>/KeyManager/issues) to report bugs or suggest features
2. **Submit Pull Requests**: Fork the repository, create a feature branch, and submit a PR with your improvements
3. **Code Guidelines**: Follow the existing code style and patterns
4. **Testing**: Ensure all tests pass before submitting (`./gradlew :composeApp:test`)

Contributions are welcome! Feel free to open issues, submit pull requests, or suggest new features. Let's make KeyManager better together! 🚀

## License

This project is licensed under the **MIT License** - see the [LICENSE](./LICENSE) file for details.

**Important Note**: KeyManager calls the Java `keytool` CLI from OpenJDK as an external process. KeyManager itself is licensed under the MIT License, which is compatible with calling GPL-licensed tools like OpenJDK's keytool.