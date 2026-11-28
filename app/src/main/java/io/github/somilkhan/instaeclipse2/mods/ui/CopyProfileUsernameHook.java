package io.github.somilkhan.instaeclipse2.mods.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.somilkhan.instaeclipse2.compat.XC_MethodHook;
import io.github.somilkhan.instaeclipse2.compat.XposedBridge;
import io.github.somilkhan.instaeclipse2.Xposed.Module;
import io.github.somilkhan.instaeclipse2.utils.core.IgModels;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/** Adds an explicit Copy username action to the profile handle's long-press menu. */
public final class CopyProfileUsernameHook {
    private static final String TAG = "(IE|CopyUsername) ";
    private static final String FRAGMENT = "com.instagram.profile.fragment.UserDetailFragment";
    private static final String VIEW_MODEL = "com.instagram.profile.fragment.UserDetailViewModel";
    private static final int COPY_USERNAME_ITEM = 0x49454355;
    private final AtomicBoolean loggedMissingHandle = new AtomicBoolean();
    private final AtomicBoolean loggedAttached = new AtomicBoolean();

    public void install(ClassLoader cl) {
        try {
            Class<?> fragment = cl.loadClass("androidx.fragment.app.Fragment");
            XposedBridge.hookAllMethods(fragment, "onResume", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Object frag = param.thisObject;
                    if (!isProfileFragment(frag)) return;
                    View root;
                    try { root = (View) fragment.getMethod("getView").invoke(frag); }
                    catch (Throwable ignored) { return; }
                    if (root == null) return;
                    root.postDelayed(() -> attach(frag, root), 500L);
                }
            });
            FeatureStatusTracker.setHooked("CopyProfileUsername");
            ModuleLog.line(TAG + "profile resume hook installed");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ install failed: " + t);
            FeatureStatusTracker.setBroken("CopyProfileUsername", t.toString());
        }
    }

    private static boolean isProfileFragment(Object fragment) {
        if (fragment == null) return false;
        for (Class<?> c = fragment.getClass(); c != null && c != Object.class; c = c.getSuperclass())
            if (FRAGMENT.equals(c.getName())) return true;
        return false;
    }

    private void attach(Object fragment, View root) {
        if (!FeatureFlags.copyProfileUsername) return;
        String username = profileUsername(fragment);
        TextView handle = findHandle(root, username);
        if (handle == null) {
            if (loggedMissingHandle.compareAndSet(false, true)) ModuleLog.line(TAG + "profile handle view not found");
            return;
        }
        handle.setOnLongClickListener(v -> {
            if (!FeatureFlags.copyProfileUsername) return false;
            String value = usernameFromView((TextView) v, profileUsername(fragment));
            if (value == null) return false;
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            MenuItem item = menu.getMenu().add(0, COPY_USERNAME_ITEM, 0, "Copy username");
            item.setOnMenuItemClickListener(ignored -> copy(v.getContext(), value));
            menu.show();
            return true;
        });
        if (loggedAttached.compareAndSet(false, true)) {
            ModuleLog.line(TAG + "profile copy action attached");
            FeatureStatusTracker.setHooked("CopyProfileUsername");
        }
    }

    private static TextView findHandle(View root, String username) {
        if (username == null) return null;
        Context context = root.getContext();
        try {
            int id = context.getResources().getIdentifier("profile_header_username", "id", context.getPackageName());
            View byId = id != 0 ? root.findViewById(id) : null;
            if (byId instanceof TextView tv && matches(tv.getText(), username)) return tv;
        } catch (Throwable ignored) { }
        return findMatchingText(root, username);
    }

    private static TextView findMatchingText(View view, String username) {
        if (view instanceof TextView tv && matches(tv.getText(), username)) return tv;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findMatchingText(group.getChildAt(i), username);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean matches(CharSequence text, String username) {
        String shown = normalize(text == null ? null : text.toString());
        return shown != null && username != null && username.equalsIgnoreCase(shown);
    }

    private static String usernameFromView(TextView view, String modelUsername) {
        String shown = normalize(view.getText() == null ? null : view.getText().toString());
        if (modelUsername != null && shown != null && modelUsername.equalsIgnoreCase(shown)) return modelUsername;
        if (modelUsername != null) return modelUsername;
        return shown != null && shown.matches("[A-Za-z0-9._]{1,30}") ? shown : null;
    }

    private static String profileUsername(Object fragment) {
        try {
            Object vm = fieldOfType(fragment, VIEW_MODEL);
            if (vm == null) return null;
            String type = IgModels.userClassName();
            Object user = type == null ? null : fieldOfType(vm, type);
            Object raw = user == null ? null : IgModels.read(user, "username");
            return normalize(raw instanceof String ? (String) raw : raw == null ? null : String.valueOf(raw));
        } catch (Throwable ignored) { return null; }
    }

    private static Object fieldOfType(Object target, String type) throws IllegalAccessException {
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass())
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !f.getType().getName().equals(type)) continue;
                f.setAccessible(true);
                Object value = f.get(target);
                if (value != null) return value;
            }
        return null;
    }

    static String normalize(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (s.startsWith("@")) s = s.substring(1);
        if (!s.matches("[A-Za-z0-9._]{1,30}")) return null;
        return s;
    }

    private static boolean copy(Context context, String username) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                FeatureStatusTracker.setBroken("CopyProfileUsername", "Android clipboard service is unavailable.");
                return false;
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("Instagram username", username));
            Toast.makeText(context, "Username copied", Toast.LENGTH_SHORT).show();
            FeatureStatusTracker.setRuntimeObserved("CopyProfileUsername");
            return true;
        } catch (Throwable t) {
            ModuleLog.line(TAG + "copy failed: " + t);
            FeatureStatusTracker.setBroken("CopyProfileUsername", t.toString());
            return false;
        }
    }
}
