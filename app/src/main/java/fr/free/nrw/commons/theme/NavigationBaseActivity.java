package fr.free.nrw.commons.theme;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.bookmarks.BookmarksActivity;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.explore.categories.ExploreActivity;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.CommonsLogSender;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import timber.log.Timber;

public abstract class NavigationBaseActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private boolean isRestoredToTop;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;
    @Inject
    CommonsLogSender commonsLogSender;


    private ActionBarDrawerToggle toggle;

    public void initDrawer() {
        setUserName();
    }

    public void changeDrawerIconToBackButton() {
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        toggle.setToolbarNavigationClickListener(view -> onBackPressed());
    }

    public void changeDrawerIconToDefault() {
        if (toggle != null) {
            toggle.setDrawerIndicatorEnabled(true);
        }
    }

    /**
     * Set the username in navigationHeader.
     */
    private void setUserName() {
    }

    public void initBackButton() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        toggle.setDrawerIndicatorEnabled(backStackEntryCount == 0);
        toggle.setToolbarNavigationClickListener(v -> onBackPressed());
    }

    /**
     * This method changes the toolbar icon to back regardless of any conditions that
     * there is any fragment in the backStack or not
     */
    public void forceInitBackButton() {
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setToolbarNavigationClickListener(v -> onBackPressed());
    }

    public void initBack() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_login:
                startActivityWithFlags(
                        this, LoginActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                applicationKvStore.putBoolean("login_skipped", false);
                finish();
                return true;
            case R.id.action_home:
                startActivityWithFlags(
                        this, MainActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return true;
            case R.id.action_about:
                startActivityWithFlags(this, AboutActivity.class, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return true;
            case R.id.action_settings:
                startActivityWithFlags(this, SettingsActivity.class, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return true;
            case R.id.action_introduction:
                WelcomeActivity.startYourself(this);
                return true;
            case R.id.action_feedback:

                String technicalInfo = commonsLogSender.getExtraInfo();

                Intent feedbackIntent = new Intent(Intent.ACTION_SENDTO);
                feedbackIntent.setType("message/rfc822");
                feedbackIntent.setData(Uri.parse("mailto:"));
                feedbackIntent.putExtra(Intent.EXTRA_EMAIL,
                        new String[]{CommonsApplication.FEEDBACK_EMAIL});
                feedbackIntent.putExtra(Intent.EXTRA_SUBJECT,
                        CommonsApplication.FEEDBACK_EMAIL_SUBJECT);
                feedbackIntent.putExtra(Intent.EXTRA_TEXT, String.format(
                        "\n\n%s\n%s", CommonsApplication.FEEDBACK_EMAIL_TEMPLATE_HEADER, technicalInfo));
                try {
                    startActivity(feedbackIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_email_client, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_logout:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.logout_verification)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            BaseLogoutListener logoutListener = new BaseLogoutListener();
                            CommonsApplication app = (CommonsApplication) getApplication();
                            app.clearApplicationData(this, logoutListener);
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                        .show();
                return true;
            case R.id.action_explore:
                ExploreActivity.startYourself(this);
                return true;
            case R.id.action_bookmarks:
                BookmarksActivity.startYourself(this);
                return true;

            case R.id.action_review:
                ReviewActivity.startYourself(this, getString(R.string.title_activity_review));
                return true;
            default:
                Timber.e("Unknown option [%s] selected from the navigation menu", itemId);
                return false;
        }
    }

    private class BaseLogoutListener implements CommonsApplication.LogoutListener {
        @Override
        public void onLogoutComplete() {
            Timber.d("Logout complete callback received.");
            Intent nearbyIntent = new Intent(
                    NavigationBaseActivity.this, LoginActivity.class);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(nearbyIntent);
            finish();
        }
    }

    public static <T> void startActivityWithFlags(Context context, Class<T> cls, int... flags) {
        Intent intent = new Intent(context, cls);
        for (int flag : flags) {
            intent.addFlags(flag);
        }
        context.startActivity(intent);
    }

    /* This is a workaround for a known Android bug which is present in some API levels.
       https://issuetracker.google.com/issues/36986021
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ((intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) > 0) {
            isRestoredToTop = true;
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (Build.VERSION.SDK_INT == 19 || Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25
                && !isTaskRoot() && isRestoredToTop) {
            // Issue with FLAG_ACTIVITY_REORDER_TO_FRONT,
            // Reordered activity back press will go to home unexpectly,
            // Workaround: move reordered activity current task to front when it's finished.
            ActivityManager tasksManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            tasksManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }


    /**
     * Handles visibility of navigation base toolbar
     *
     * @param show : Used to handle visibility of toolbar
     */
    public void setNavigationBaseToolbarVisibility(boolean show) {
        if (show) {
            toolbar.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
