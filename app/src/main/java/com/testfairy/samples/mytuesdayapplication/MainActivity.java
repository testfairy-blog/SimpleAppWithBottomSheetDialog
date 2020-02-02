package com.testfairy.samples.mytuesdayapplication;

import android.os.Bundle;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.view.Gravity;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.testfairy.TestFairy;
import com.testfairy.TestFairyFeedbackOverlay;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

	private AppBarConfiguration mAppBarConfiguration;

	static private final String TESTFAIRY_APP_TOKEN = "SDK-aaaaaaaaaa";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		FloatingActionButton fab = findViewById(R.id.fab);

		DrawerLayout drawer = findViewById(R.id.drawer_layout);
		NavigationView navigationView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_tools, R.id.nav_share, R.id.nav_send).setDrawerLayout(drawer).build();
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
		NavigationUI.setupWithNavController(navigationView, navController);

		// point to your single-tenant instance if needed
		TestFairy.setServerEndpoint("subdomain.testfairy.com");

		// set up testfairy
		TestFairy.setUserId("jack@example.com");
		TestFairy.setAttribute("paying", "true");
		TestFairy.setAttribute("fullName", "Jack Black");

		// option 1: start recording a video immediately
		// TestFairy.begin(this, TESTFAIRY_APP_TOKEN);

		// option 2: install button overlay
		// TestFairyFeedbackOverlay.installOverlay(this, TESTFAIRY_APP_TOKEN, TestFairyFeedbackOverlay.OverlayPurpose.SCREENSHOT);

		// option 3: show a button, that when clicked, will show bottom sheet dialog with options
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showBottomSheetDialog();
			}
		});

		// if there are unsent feedbacks, try sending them now
		TestFairy.sendPendingFeedbacks(this, TESTFAIRY_APP_TOKEN);
	}

	private void showBottomSheetDialog() {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		Button takeScreenshotButton = new Button(this);
		takeScreenshotButton.setText("Take a screenshot for feedback");
		takeScreenshotButton.setGravity(Gravity.CENTER);
		layout.addView(takeScreenshotButton);

		Button recordVideoButton = new Button(this);
		recordVideoButton.setText("Record a video for bug report");
		recordVideoButton.setGravity(Gravity.CENTER);
		layout.addView(recordVideoButton);

		Button cancelButton = new Button(this);
		cancelButton.setText("Cancel");
		cancelButton.setGravity(Gravity.CENTER);
		layout.addView(cancelButton);

		final BottomSheetDialog dialog = new BottomSheetDialog(this);
		dialog.setContentView(layout);
		dialog.show();

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
			}
		});

		takeScreenshotButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
				TestFairyFeedbackOverlay.installOverlay(MainActivity.this, TESTFAIRY_APP_TOKEN, TestFairyFeedbackOverlay.OverlayPurpose.SCREENSHOT);
			}
		});

		recordVideoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
				TestFairyFeedbackOverlay.installOverlay(MainActivity.this, TESTFAIRY_APP_TOKEN, TestFairyFeedbackOverlay.OverlayPurpose.VIDEO);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onSupportNavigateUp() {
		NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
		return NavigationUI.navigateUp(navController, mAppBarConfiguration)
			|| super.onSupportNavigateUp();
	}
}
