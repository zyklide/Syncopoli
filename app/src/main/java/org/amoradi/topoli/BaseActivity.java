package org.amoradi.topoli;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "BaseActivity";


    public Toolbar mToolbar;
    public DrawerLayout mDrawer;
    public ActionBarDrawerToggle mDrawerToggle;
    public NavigationView mNavigationView;

    protected Handler mHandler;

    public String rsyncPath;
    public String sshPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        overridePendingTransition(0, 0);
        setExecutables();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        View mainContent = findViewById(R.id.activity_container);
        mainContent.setAlpha(0);
        mainContent.animate().alpha(1).setDuration(250);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID)
    {
        super.setContentView(layoutResID);
        onCreateDrawer();
    }

    protected void onCreateDrawer() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_movies) {
            runActivity(MoviesActivity.class);
        } else if (id == R.id.nav_tvshows) {
            runActivity(TvShowsActivity.class);
        } else if (id == R.id.nav_sync) {
            runActivity(SyncActivity.class);
        } else if (id == R.id.nav_backup) {
            runActivity(BackupActivity.class);
        } else if (id == R.id.nav_settings) {
            runActivity(SettingsActivity.class);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void runActivity(final Class cls) {
        // launch the target Activity after a short delay, to allow the close animation to play
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(getApplicationContext(), cls);
                    startActivity(intent);
                    finish();
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }, 250);

        View mainContent = findViewById(R.id.activity_container);
        mainContent.setAlpha(1);
        mainContent.animate().alpha(0).setDuration(150);
    }

    protected void setNavDrawerSelected(int itemId) {
        Menu m = mNavigationView.getMenu();
        for (int i = 0; i < m.size(); i++) {
            MenuItem item = m.getItem(i);
            item.setChecked(item.getItemId() == itemId);
        }
    }

    protected void setExecutables() {
        rsyncPath = copyRsyncExecutable();
        sshPath = copySshExecutable();
    }

    public String copyRsyncExecutable() {
        return copyExecutable("rsync");
    }

    public String copySshExecutable(){
        return copyExecutable("ssh");
    }

    public String copyExecutable(String filename) {
        try {
            File file = getFileStreamPath(filename);

            InputStream ins = getAssets().open("arm/" + filename);
            FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath());

            byte[] buffer = new byte[ins.available()];
            int bytes = ins.read(buffer);
            outputStream.write(buffer, 0, bytes);

            outputStream.close();
            ins.close();

            File f = new File(file.getAbsolutePath());
            f.setExecutable(true);

            return f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
