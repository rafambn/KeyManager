# Suppress all warnings for unresolved class references.
-dontwarn **

# Disable optimization and obfuscation to avoid IncompleteClassHierarchyException on JDK 25
-dontoptimize
-dontobfuscate

# Disable preverification as it's causing IncompleteClassHierarchyException on JDK 25 
# even with lib/modules. We'll handle the VerifyError differently.
-dontpreverify

# Provide the JDK runtime modules for hierarchy resolution.
-libraryjars <java.home>/lib/modules(!**.jar;!module-info.class)

# Common Compose Desktop ProGuard rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontnote
-ignorewarnings

# Keep everything to ensure it works, then we can try to narrow it down
-keep class ** { *; }
-keep interface ** { *; }
-keep enum ** { *; }
