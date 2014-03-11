/*
* This file is part of the GamePad
*
* Copyright (C) 2013 Mundo Reader S.L.
* 
* Date: February 2014
* Author: Estefanía Sarasola Elvira <estefania.sarasola@bq.com>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/

package com.bq.robotic.robopad_plusplus.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.bq.robotic.robopad_plusplus.RoboPadConstants;
import com.bq.robotic.robopad_plusplus.listeners.RobotListener;


/**
 * Base fragment for all the robot fragments
 * 
 * @author Estefanía Sarasola Elvira
 *
 */

public abstract class RobotFragment extends Fragment {

	// Debugging
	private static final String LOG_TAG = "RobotFragment";

	protected boolean mIsClick;
	protected boolean mIsConnected = false;

	protected RobotListener listener;

	/**
	 * Set the listeners to the UI views
	 * @param containerLayout
	 */
	protected abstract void setUiListeners (View containerLayout);


	/**
	 * Send the message depending on the button pressed
	 * @param viewId the id of the view pressed
	 */
	protected abstract void controlButtonActionDown(int viewId);

	
	/**
	 * Callback method called from the activity when the Bluetooth change its status to connected
	 */
	public void onBluetoothConnected() {}
	
	
	/**
	 * Callback method called from the activity when the Bluetooth change its status to disconnected
	 */
	public void onBluetoothDisconnected() {}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Retain this fragment across configuration changes.
		setRetainInstance(true);
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Check the listener is the correct one: the fragment activity container
		// implements that listener
		if (activity instanceof RobotListener) {
			this.listener = (RobotListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement robotListener");
		}
	}


	/**
	 * Listener for the touch events. When action_down, the user is pressing the button
	 * so we send the message to the arduino, and when action_up it is send a message to the arduino
	 * in order to stop it.
	 */
	protected OnTouchListener buttonOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			final View view = v;

			Thread sendActionThread;

			switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:

				if(listener != null && !listener.onCheckIsConnected()) {
					mIsConnected = false;
					break;
				} else {
					mIsConnected = true;
				}

				mIsClick = false;					
				sendActionThread = createSendActionThread(view.getId());										
				sendActionThread.start();

				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:

				if(!mIsConnected) {
					break;
				}

				mIsClick = true;
				if (listener != null) {
					listener.onSendMessage(RoboPadConstants.STOP_COMMAND);
				}

				break;

			}		

			return false;
		}

	};



	/**
	 * Thread to send the command but waits and send the stop command with a 130 delay
	 * in case it was only a click and the arduino app didn't process the stop command 
	 * because of itself delays
	 * 
	 * @param actionId the id of the view touched
	 * @return Thread The thread that send the commands when pressed the corresponding buttons
	 */
	private Thread createSendActionThread(final int actionId) {

		Thread sendActionThread = new Thread() {

			@Override
			public void run() {
				try {

					if(!mIsClick) {
						controlButtonActionDown(actionId);
					}

					sleep(RoboPadConstants.CLICK_SLEEP_TIME);

					if(mIsClick && listener != null) {
						Log.e(LOG_TAG, "stop command in thread send");
						listener.onSendMessage(RoboPadConstants.STOP_COMMAND);
					}

				} catch (InterruptedException e) {
					Log.e(LOG_TAG, "error in sendActionThread: )" + e);
				}

			}

		};

		return sendActionThread;
	}

}