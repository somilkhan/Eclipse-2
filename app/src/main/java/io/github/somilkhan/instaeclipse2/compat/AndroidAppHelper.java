package io.github.somilkhan.instaeclipse2.compat;
import android.app.Application;
public final class AndroidAppHelper { private AndroidAppHelper(){} public static Application currentApplication(){ return io.github.somilkhan.instaeclipse2.Xposed.Module.currentApplication(); } }
