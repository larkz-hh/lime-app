# Lime  R8 规则
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Gson 泛型 TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Gson 反射模型包
-keep class xyz.larkzhh.lime.data.network.model.** { *; }
-keep class xyz.larkzhh.lime.domain.model.** { *; }

# SSE 事件 DTO
-keep class xyz.larkzhh.lime.data.network.AiSseEventDto { *; }

# 腾讯 IM SDK  libImSDK.so 的 JNI_OnLoad 按类名查找 Java 类
-keep class com.tencent.imsdk.** { *; }

# -keep class me.leolin.shortcutbadger.** { *; } # ShortcutBadger
# -keep class com.yalantis.ucrop.** { *; } # uCrop 裁剪