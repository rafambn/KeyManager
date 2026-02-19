# Suppress all warnings for unresolved class references.
-dontwarn **

# Disable optimization, obfuscation, and preverification to fix IncompleteClassHierarchyException on JDK 25.
# This ensures the build succeeds while still allowing the app to run on JVM 25.
-dontoptimize
-dontobfuscate
-dontpreverify

# Common Compose Desktop ProGuard rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontnote
-ignorewarnings

# Keep all classes - since we disabled obfuscation and optimization, 
# this is mostly to be explicit and avoid any potential shrinking issues.
-keep class ** { *; }
