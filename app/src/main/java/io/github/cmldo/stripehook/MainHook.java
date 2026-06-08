package io.github.cmldo.stripehook;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed module that forces {@code CustomerSheet.Configuration.Builder.googlePayEnabled()}
 * to always behave as if {@code false} was passed, regardless of what the host app provides.
 *
 * <p>Stripe SDK target: {@code com.stripe.android:stripe-android} 20.x+</p>
 *
 * @author cmldo
 * @see <a href="https://github.com/cmldo/stripe-googlepay-disabler">GitHub</a>
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "StripeGPDisabler";

    /**
     * Fully-qualified name of the Stripe Builder class.
     *
     * <p>Kotlin nested classes are compiled with {@code $} separators:</p>
     * {@code CustomerSheet.Configuration.Builder}
     * → {@code CustomerSheet$Configuration$Builder}
     */
    private static final String BUILDER_CLASS =
            "com.stripe.android.customersheet.CustomerSheet$Configuration$Builder";

    // -------------------------------------------------------------------------
    // IXposedHookLoadPackage
    // -------------------------------------------------------------------------

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Skip the system server and this module's own process
        if (lpparam.packageName.equals("android")
                || lpparam.packageName.equals("io.github.cmldo.stripehook")) {
            return;
        }

        hookBuilder(lpparam.classLoader, lpparam.packageName);
    }

    // -------------------------------------------------------------------------
    // Hook wiring
    // -------------------------------------------------------------------------

    /**
     * Installs hooks on every available overload of {@code googlePayEnabled}.
     *
     * <p>Stripe ships two overloads depending on SDK version:</p>
     * <ul>
     *   <li>{@code googlePayEnabled(Boolean)} — Kotlin nullable (20.x+)</li>
     *   <li>{@code googlePayEnabled(boolean)} — primitive (older)</li>
     * </ul>
     */
    private void hookBuilder(ClassLoader classLoader, String packageName) {
        Class<?> builderClass;
        try {
            builderClass = XposedHelpers.findClass(BUILDER_CLASS, classLoader);
        } catch (XposedHelpers.ClassNotFoundError e) {
            // Stripe SDK not present in this process — expected for most apps
            Log.v(TAG, packageName + " does not load Stripe CustomerSheet, skipping.");
            return;
        }

        int hooksInstalled = 0;

        // Kotlin nullable Boolean overload (primary target)
        hooksInstalled += tryHook(builderClass, "googlePayEnabled", Boolean.class);

        // Primitive overload (older SDK versions / Java callers)
        hooksInstalled += tryHook(builderClass, "googlePayEnabled", boolean.class);

        if (hooksInstalled > 0) {
            XposedBridge.log(TAG + ": " + hooksInstalled + " hook(s) installed in " + packageName);
        } else {
            XposedBridge.log(TAG + ": " + BUILDER_CLASS
                    + " found but no googlePayEnabled overload matched in " + packageName);
        }
    }

    /**
     * Attempts to hook a single method overload.
     *
     * @return 1 if the hook was installed, 0 otherwise.
     */
    private int tryHook(Class<?> clazz, String methodName, Class<?> paramType) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, paramType, new ForceFalseHook());
            Log.d(TAG, "Hooked " + methodName + "(" + paramType.getSimpleName() + ")");
            return 1;
        } catch (NoSuchMethodError e) {
            Log.v(TAG, methodName + "(" + paramType.getSimpleName() + ") not found — skipped.");
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Hook implementation
    // -------------------------------------------------------------------------

    /**
     * Replaces the caller-supplied value with {@code false} before the original
     * method executes, so the Builder's internal state is set to disabled.
     *
     * <p>Using {@code beforeHookedMethod} (rather than replacing the method entirely)
     * means the original Builder logic still runs — return value, null-checks, etc.
     * are all preserved.</p>
     */
    private static class ForceFalseHook extends XC_MethodHook {

        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            Object original = param.args[0];

            // Only log when we're actually changing something
            if (!Boolean.FALSE.equals(original)) {
                XposedBridge.log(TAG + ": googlePayEnabled(" + original
                        + ") intercepted → forcing false");
                param.args[0] = Boolean.FALSE;
            }
        }
    }
}
