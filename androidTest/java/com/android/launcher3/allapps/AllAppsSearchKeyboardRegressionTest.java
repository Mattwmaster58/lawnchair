package com.android.launcher3.allapps;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AllAppsSearchKeyboardRegressionTest {

    private static final long TIMEOUT_MS = 10000;
    private static final String TARGET_PACKAGE = "app.lawnchair.debug";
    private static final String APPS_VIEW_RES = "apps_view";
    private static final String SEARCH_RESULTS_RES = "search_results_list_view";
    private static final String SEARCH_INPUT_RES = "input";
    private static final String ACTION_BUTTON_RES = "action_btn";

    private final UiDevice mDevice = UiDevice.getInstance(getInstrumentation());

    @Before
    public void setUp() throws Exception {
        Context context = getInstrumentation().getContext();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(TARGET_PACKAGE);
        assertNotNull("Launch intent is null", launchIntent);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);

        waitForLauncher();
        mDevice.pressHome();
        waitForLauncher();
    }

    @Test
    public void testDeletingLastCharacterKeepsKeyboardOpen() throws Exception {
        openAllApps();
        waitForLauncherState(LauncherState.ALL_APPS, "Launcher did not enter All Apps");

        getInstrumentation().runOnMainSync(() -> {
            Launcher launcher = Launcher.ACTIVITY_TRACKER.getCreatedContext();
            assertNotNull("Launcher instance is null", launcher);
            ExtendedEditText editText = launcher.getAppsView().getSearchUiManager().getEditText();
            assertNotNull("All Apps search edit text is null", editText);
            editText.showKeyboard();
        });

        waitForLauncherCondition("Search edit text did not gain focus", () -> {
            Launcher launcher = Launcher.ACTIVITY_TRACKER.getCreatedContext();
            if (launcher == null) {
                return false;
            }
            ExtendedEditText editText = launcher.getAppsView().getSearchUiManager().getEditText();
            return editText != null && editText.isFocused();
        });

        UiObject2 searchInput = mDevice.wait(
                Until.findObject(By.res(TARGET_PACKAGE, SEARCH_INPUT_RES)),
                TIMEOUT_MS);
        assertNotNull("Search input not found", searchInput);

        mDevice.pressKeyCode(KeyEvent.KEYCODE_C);

        UiObject2 searchResults = mDevice.wait(
                Until.findObject(By.res(TARGET_PACKAGE, SEARCH_RESULTS_RES)),
                TIMEOUT_MS);
        assertNotNull("Search results container did not appear after typing", searchResults);

        mDevice.pressKeyCode(KeyEvent.KEYCODE_DEL);

        waitForLauncherState(LauncherState.ALL_APPS,
                "Launcher unexpectedly exited All Apps after delete");
        waitForLauncherCondition("Search edit text lost focus after delete", () -> {
            Launcher launcher = Launcher.ACTIVITY_TRACKER.getCreatedContext();
            if (launcher == null) {
                return false;
            }
            ExtendedEditText editText = launcher.getAppsView().getSearchUiManager().getEditText();
            return editText != null && editText.isFocused();
        });

        UiObject2 appsView = mDevice.wait(
                Until.findObject(By.res(TARGET_PACKAGE, APPS_VIEW_RES)),
                TIMEOUT_MS);
        assertNotNull("All Apps container disappeared after delete", appsView);

        searchInput = mDevice.wait(
                Until.findObject(By.res(TARGET_PACKAGE, SEARCH_INPUT_RES)),
                TIMEOUT_MS);
        assertNotNull("Search input disappeared after delete", searchInput);
        assertTrue("Search input is no longer focused after delete", searchInput.isFocused());

        UiObject2 resultsAfterDelete = mDevice.wait(
                Until.findObject(By.res(TARGET_PACKAGE, SEARCH_RESULTS_RES)),
                TIMEOUT_MS);
        assertNotNull("Search results container disappeared after delete", resultsAfterDelete);

        UiObject2 clearButton = mDevice.findObject(By.res(TARGET_PACKAGE, ACTION_BUTTON_RES));
        assertTrue("Clear button should be hidden after deleting final character",
                clearButton == null);
    }

    private void openAllApps() throws Exception {
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        int startX = width / 2;
        int startY = (height * 3) / 4;
        int endY = height / 5;

        for (int i = 0; i < 3; i++) {
            mDevice.swipe(startX, startY, startX, endY, 12);
            mDevice.waitForIdle();
            Launcher launcher = Launcher.ACTIVITY_TRACKER.getCreatedContext();
            if (launcher != null && launcher.isInState(LauncherState.ALL_APPS)) {
                return;
            }
            SystemClock.sleep(500);
        }
    }

    private void waitForLauncher() {
        UiObject2 appsView = mDevice.wait(Until.findObject(By.pkg(TARGET_PACKAGE)), TIMEOUT_MS);
        assertNotNull("Launcher app did not appear", appsView);
    }

    private void waitForLauncherState(LauncherState state, String message) {
        waitForLauncherCondition(message, () -> {
            Launcher launcher = Launcher.ACTIVITY_TRACKER.getCreatedContext();
            return launcher != null && launcher.isInState(state);
        });
    }

    private void waitForLauncherCondition(String message, Condition condition) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            getInstrumentation().waitForIdleSync();
            if (condition.isTrue()) {
                return;
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError(message);
    }

    private interface Condition {
        boolean isTrue();
    }
}
