/*
 * Copyright (C) 2019 TestFairy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Icons are available under Apache License, Version 2.0 (the "License")
 * and provided by material.io project.
 */

package com.testfairy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class TestFairyFeedbackOverlay {

	public enum OverlayPurpose {
		SCREENSHOT, VIDEO
	}

	/***************** Public Interface *****************/

	/**
	 * Call this before everything else to install a human friendly overlay UI
	 * for sending feedbacks to TestFairy.
	 *
	 * @param activity The Activity installing the overlay, must not be null.
	 * @param appToken Your TestFairy app token.
	 */
	static public void installOverlay(final Activity activity, final String appToken, final OverlayPurpose overlayPurpose) {
		if (activity == null) {
			throw new AssertionError("Activity cannot be null.");
		}

		if (appToken == null) {
			throw new AssertionError("TestFairy app token cannot be null.");
		}

		Runnable install = new Runnable() {
			@Override
			public void run() {

				final Application a = (Application) activity.getApplicationContext();
				a.registerActivityLifecycleCallbacks(lifecycle);

				token = appToken;
				purpose = overlayPurpose;

				uninstall = new Runnable() {
					@Override
					public void run() {
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								uninstall = null;
								a.unregisterActivityLifecycleCallbacks(lifecycle);

								synchronized (instance) {
									instance.removeOverlay(true);
									beginCalled = false;
								}
							}
						});
					}
				};

				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						onResume(activity);
					}
				});
			}
		};

		uninstallOverlay();
		if (overlayPurpose == OverlayPurpose.VIDEO) {
			showRecordDialog(activity, install);
		} else {
			install.run();
		}
	}

	/**
	 * Call this if you no longer need the overlay during your app's lifetime.
	 */
	static public void uninstallOverlay() {
		if (instance != null && uninstall != null) {
			uninstall.run();
		}
	}

	/***************** SDK Constants *****************/

	static private String TAG = "TFFeedbackOverlay";

	/***************** Overlay UI State *****************/

	private LinearLayout container;

	/***************** Singleton State *****************/

	static private TestFairyFeedbackOverlay instance;
	static private String token;
	static private OverlayPurpose purpose = null;
	static private boolean beginCalled = false;
	static private Runnable uninstall = null;

	/***************** Lifecycle *****************/

	static private final Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
		@Override
		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		}

		@Override
		public void onActivityStarted(Activity activity) {

		}

		@Override
		public void onActivityResumed(Activity activity) {
			onResume(activity);
		}

		@Override
		public void onActivityPaused(Activity activity) {
			onPause(activity);
		}

		@Override
		public void onActivityStopped(Activity activity) {
		}

		@Override
		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

		}

		@Override
		public void onActivityDestroyed(Activity activity) {

		}
	};

	private TestFairyFeedbackOverlay() { }

	private synchronized void attachOverlay(FrameLayout root) {
		Log.d(TAG, "Attaching overlay");

		final Context context = root.getContext();
		container = new LinearLayout(context);
		int padding = (int) dpToPx(context, OVERLAY_PADDING);

		int containerWidth = (int) dpToPx(context, ICON_SIZE_IN_DP) + padding * 2;
		final FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(
			containerWidth,
			FrameLayout.LayoutParams.WRAP_CONTENT,
			OVERLAY_GRAVITY
		);
		containerLayoutParams.leftMargin = 0;
		containerLayoutParams.rightMargin = 0;

		int imageWidthHeight = (int) dpToPx(context, ICON_SIZE_IN_DP);
		final ImageView roundedImageView = new ImageView(context);

		LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(
			imageWidthHeight,
			imageWidthHeight
		);
		imageLayoutParams.setMargins(0,0,0, padding);

		roundedImageView.setPadding(padding * 2, padding * 2, padding * 2, padding * 2);
		roundedImageView.setLayoutParams(imageLayoutParams);

		if (purpose == OverlayPurpose.VIDEO) {
			final View containerDuringSetup = container;

			if (!beginCalled) {
				TestFairy.begin(context, token);
				beginCalled = true;
			}

			TestFairy.addEvent(VIDEO_OVERLAY_EVENT);

			roundedImageView.setImageDrawable(new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(RECORDING_ICON, 0, RECORDING_ICON.length)));
			roundedImageView.setOnClickListener(null);
			roundedImageView.post(new ShowHideAnimationRunnable(containerDuringSetup, roundedImageView));

			container.addView(roundedImageView);
		} else if (purpose == OverlayPurpose.SCREENSHOT) {
			roundedImageView.setImageDrawable(new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(SCREENSHOT_ICON, 0, SCREENSHOT_ICON.length)));
			roundedImageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					uninstallOverlay();
					TestFairy.showFeedbackForm(context, token, true);
				}
			});

			container.addView(roundedImageView);
		} else {
			uninstallOverlay();
			return;
		}

		container.setPadding(padding, padding, padding, padding);
		container.setLayoutParams(containerLayoutParams);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
		container.setDividerPadding(0);
		container.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
		root.addView(container);

		if (purpose == OverlayPurpose.SCREENSHOT) {
			float widthHeight = dpToPx(container.getContext(), ICON_SIZE_IN_DP);
			TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, widthHeight * 5, 0);
			translateAnimation.setDuration(500);
			container.startAnimation(translateAnimation);
		}
	}

	private synchronized void removeOverlay(boolean postToContainer) {
		Log.d(TAG, "Removing overlay");

		if (container != null) {
			Runnable remove = new Runnable() {
				@Override
				public void run() {
					container.removeAllViews();
					ViewParent parent = container.getParent();
					if (parent instanceof ViewGroup) {
						ViewGroup castedParent = (ViewGroup) parent;
						castedParent.removeView(container);
					}

					container = null;
				}
			};

			if (postToContainer) {
				container.post(remove);
			} else {
				remove.run();
			}
		}
	}

	static private void onResume(Activity activity) {
		Log.d(TAG, "Overlay is attaching to " + activity.getClass().getSimpleName());

		if (instance == null) {
			instance = new TestFairyFeedbackOverlay();
		}

		synchronized (instance) {
			final View root = activity.getWindow().getDecorView().findViewById(android.R.id.content);
			if (root != null && root instanceof FrameLayout) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						instance.removeOverlay(false);
						instance.attachOverlay((FrameLayout) root);
					}
				});
			} else {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						instance.removeOverlay(false);
					}
				});
				Log.e(TAG, "There is no view to attach TestFairy feedback overlay");
			}
		}
	}

	static private void onPause(Activity activity) {
		Log.d(TAG, "TestFairyFeedbackOverlay is detaching from " + activity.getClass().getSimpleName());

		if (instance == null) {
			instance = new TestFairyFeedbackOverlay();
		}

		synchronized (instance) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					instance.removeOverlay(false);
				}
			});
		}
	}

	static private float dpToPx(Context context, float dp) {
		Resources r = context.getResources();
		return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP,
			dp,
			r.getDisplayMetrics()
		);
	}

	static private void showRecordDialog(final Context context, final Runnable onSuccess) {
		createDialogBuilder(context)
			.setTitle(RECORD_DIALOG_TITLE)
			.setMessage(RECORD_DIALOG_MESSAGE)
			.setCancelable(true)
			.setIcon(android.R.drawable.ic_popup_sync)
			.setPositiveButton(RECORD_DIALOG_OK_BUTTON, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onSuccess.run();
				}
			})
			.setNegativeButton(RECORD_DIALOG_CANCEL_BUTTON, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					safeDismiss(dialog);
				}
			}).create().show();
	}

	static private void safeDismiss(DialogInterface dialog) {
		try {
			if (dialog != null) {
				dialog.dismiss();
			}
		} catch (Throwable e) {
			// ignored
		}
	}

	static private AlertDialog.Builder createDialogBuilder(Context context) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH ) {
			return new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Dialog));

		}

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
			return new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Dialog));

		}

		return new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Dialog));
	}

	static private class ShowHideAnimationRunnable implements Runnable {
		private final View view;
		private final View container;
		private boolean showing = false;

		public ShowHideAnimationRunnable(View container, View view) {
			this.view = view;
			this.container = container;
		}

		@Override
		public void run() {
			if (
				view != null &&
					container != null &&
					TestFairyFeedbackOverlay.instance != null &&
					container == TestFairyFeedbackOverlay.instance.container
			) {
				if (showing) {
					view.setVisibility(View.INVISIBLE);
				} else {
					view.setVisibility(View.VISIBLE);
				}

				view.postDelayed(this, RECORD_ANIMATION_FREQUENCY);
				showing = !showing;
			}
		}
	}

	/***************** UI Setup *****************/

	static private final byte[] SCREENSHOT_ICON = Base64.decode("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", 0);
	static private final byte[] RECORDING_ICON = Base64.decode("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", 0);
	static private final int ICON_SIZE_IN_DP = 70;
	static private final int OVERLAY_PADDING = 5;
	static private final int OVERLAY_GRAVITY = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
	static private final long RECORD_ANIMATION_FREQUENCY = 1000;
	static private final String RECORD_DIALOG_TITLE = "Session recording";
	static private final String RECORD_DIALOG_MESSAGE = "By clicking \"start now\" you agree to start recording your session for quality assurance purposes. We are going to take your house, and sell it on ebay. and your car, and your phone. OK?";
	static private final String RECORD_DIALOG_OK_BUTTON = "Start Now";
	static private final String RECORD_DIALOG_CANCEL_BUTTON = "Cancel";
	static private final String VIDEO_OVERLAY_EVENT = "User requested video";
}