# Java 25 module system
# Declare jmod files as library jars so ProGuard can resolve java.lang.*, java.util.*, etc.
-libraryjars <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.datatransfer.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.desktop.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.logging.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.management.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.naming.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.net.http.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.prefs.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.xml.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/jdk.unsupported.jmod(!**.jar;!module-info.class)

# Suppress any remaining warnings for references that exist at runtime
# (safe for native distributions because the JDK is bundled with the app)
-dontwarn **
