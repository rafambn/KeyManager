<h1 align="center">KeyManager</h1>

<p align="center">A Compose Multiplatform desktop app for Java keystore management</p>

<p align="center">
  <img src="app_icon.svg" alt="KeyManager Icon" width="200" height="200">
</p>

<p align="center">
  <a href="https://github.com/rafambn/KeyManager/releases/latest">
    <img alt="Release" src="https://img.shields.io/github/v/release/rafambn/KeyManager?label=Release">
  </a>
  <a href="./LICENSE">
    <img alt="License" src="https://img.shields.io/badge/license-MIT-blue.svg">
  </a>
  <a href="https://github.com/rafambn/KeyManager/releases">
    <img alt="Platform" src="https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-0A7EA4">
  </a>
</p>

<p align="center">
  KeyManager is a Kotlin Multiplatform desktop application that wraps the Java <code>keytool</code> CLI with an intuitive Compose UI. Manage <code>.jks</code>, <code>.pkcs12</code>, and <code>.jceks</code> keystores faster, with a clear workflow for everyday certificate and key operations.
</p>

<table align="center">
  <tr>
    <td align="center">
      <a href="https://github.com/rafambn/KeyManager/releases"><strong>Download Latest Release</strong></a>
    </td>
  </tr>
</table>

### Key Features

- **Multi-keystore workflow**: Open multiple keystores and switch quickly between them.
- **Keystore format support**: PKCS12, JKS, and JCEKS.
- **Advanced key generation**: RSA, EC, DSA, EdDSA, and post-quantum (ML-DSA, ML-KEM) options.
- **Certificate tooling**: Import/export certificates, create CSRs, inspect chains, and validate expiration.
- **Bulk operations**: Move multiple entries between keystores.
- **Recent files and secure prompts**: Faster reopen flow with password-protected operations.

### Instalation

Download the installer for your architecture from the [Releases](https://github.com/rafambn/KeyManager/releases) page:

- **Windows**: `keymanager-windows-x64.msi` (Windows 10+)
- **macOS**: `keymanager-macos-x64.dmg` (Intel) or `keymanager-macos-arm64.dmg` (Apple Silicon)
- **Linux**: `keymanager-linux-x64.deb` (x86-64) or `keymanager-linux-arm64.deb` (ARM64)

KeyManager is licensed under the [MIT License](./LICENSE). It invokes OpenJDK's `keytool` as an external process.
