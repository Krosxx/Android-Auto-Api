/*
 * Copyright (C) 2015 The Android Open Source Project
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
 */

package cn.vove7.accessibility.uiauto;

import android.app.UiAutomation;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.vove7.auto.core.utils.AutoGestureDescription;
import timber.log.Timber;

/**
 * Injects MotionEvents to permit {@code AccessibilityService}s to touch the screen on behalf of
 * users.
 * <p>
 * All methods except {@code injectEvents} must be called only from the main thread.
 */
public class MotionInjector implements Handler.Callback {
    private static final String LOG_TAG = "MotionInjector";
    private static final int MESSAGE_SEND_MOTION_EVENT = 1;
    private static final int MESSAGE_INJECT_EVENTS = 2;

    /**
     * Constants used to initialize all MotionEvents
     */
    private static final int EVENT_META_STATE = 0;
    private static final int EVENT_BUTTON_STATE = 0;
    private static final int EVENT_EDGE_FLAGS = 0;
    private static final int EVENT_SOURCE = InputDevice.SOURCE_TOUCHSCREEN;
    private static final int EVENT_FLAGS = 0;
    private static final float EVENT_X_PRECISION = 1;
    private static final float EVENT_Y_PRECISION = 1;

    private static MotionEvent.PointerCoords[] sPointerCoords;
    private static MotionEvent.PointerProperties[] sPointerProps;

    private final Handler mHandler;
    private final SparseArray<Boolean> mOpenGesturesInProgress = new SparseArray<>();

    private GestureCallback mServiceInterfaceForCurrentGesture;
    private List<Integer> mSequencesInProgress = new ArrayList<>(5);
    private boolean mIsDestroyed = false;
    private AutoGestureDescription.TouchPoint[] mLastTouchPoints;
    private int mNumLastTouchPoints;
    private long mDownTime;
    private long mLastScheduledEventTime;
    private SparseIntArray mStrokeIdToPointerId = new SparseIntArray(5);

    UiAutomation uiAutomation;

    /**
     * @param looper A looper on the main thread to use for dispatching new events
     */
    public MotionInjector(Looper looper, UiAutomation uiAutomation) {
        mHandler = new Handler(looper, this);
        this.uiAutomation = uiAutomation;
    }

    /**
     * @param handler A handler to post messages. Exposes internal state for testing only.
     */
    public MotionInjector(Handler handler, UiAutomation uiAutomation) {
        mHandler = handler;
        this.uiAutomation = uiAutomation;
    }

    /**
     * Schedule a gesture for injection. The gesture is defined by a set of {@code GestureStep}s,
     * from which {@code MotionEvent}s will be derived. All gestures currently in progress will be
     * cancelled.
     *
     * @param gestureSteps The gesture steps to inject.
     *                     either complete or cancelled.
     */
    public void injectEvents(
            List<AutoGestureDescription.GestureStep> gestureSteps,
            int sequence, int displayId,
            GestureCallback callback
    ) {
        Object[] args = new Object[]{
                gestureSteps,
                callback,
                sequence,
                displayId
        };
        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_INJECT_EVENTS, args));
    }

    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        // MotionEventInjector would cancel any injected gesture when any MotionEvent arrives.
        // For user using an external device to control the pointer movement, it's almost
        // impossible to perform the gestures. Any slightly unintended movement results in the
        // cancellation of the gesture.
        if ((event.isFromSource(InputDevice.SOURCE_MOUSE)
                && event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE)
                && mOpenGesturesInProgress.get(EVENT_SOURCE, false)) {
            return;
        }
        cancelAnyPendingInjectedEvents();
        // Indicate that the input event is injected from accessibility, to let applications
        // distinguish it from events injected by other means.
        policyFlags |= FLAG_INJECTED_FROM_ACCESSIBILITY;
        sendMotionEventToNext(event, rawEvent, policyFlags);
    }

    public void clearEvents(int inputSource) {
        /*
         * Reset state for motion events passing through so we won't send a cancel event for
         * them.
         */
        if (!mHandler.hasMessages(MESSAGE_SEND_MOTION_EVENT)) {
            mOpenGesturesInProgress.put(inputSource, false);
        }
    }

    public void onDestroy() {
        cancelAnyPendingInjectedEvents();
        mIsDestroyed = true;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message.what == MESSAGE_INJECT_EVENTS) {
            Object[] args = (Object[]) message.obj;
            injectEventsMainThread((List<AutoGestureDescription.GestureStep>) args[0],
                    (GestureCallback) args[1], (int) args[2], (int) args[3]);
            return true;
        }
        if (message.what != MESSAGE_SEND_MOTION_EVENT) {
            Timber.tag(LOG_TAG).e("Unknown message: %s", message.what);
            return false;
        }
        MotionEvent motionEvent = (MotionEvent) message.obj;
        sendMotionEventToNext(motionEvent, motionEvent,
                FLAG_PASS_TO_USER
                        | FLAG_INJECTED_FROM_ACCESSIBILITY);
        boolean isEndOfSequence = message.arg1 != 0;
        if (isEndOfSequence) {
            notifyService(mServiceInterfaceForCurrentGesture, mSequencesInProgress.get(0), true);
            mSequencesInProgress.remove(0);
        }
        return true;
    }

    private Object getNext() {
        return this;
    }

    private void injectEventsMainThread(List<AutoGestureDescription.GestureStep> gestureSteps,
                                        GestureCallback serviceInterface,
                                        int sequence, int displayId) {
        if (mIsDestroyed) {
            serviceInterface.onPerformGestureResult(sequence, false);
            return;
        }

        if (getNext() == null) {
            notifyService(serviceInterface, sequence, false);
            return;
        }

        boolean continuingGesture = newGestureTriesToContinueOldOne(gestureSteps);

        if (continuingGesture) {
            if (/*(serviceInterface != mServiceInterfaceForCurrentGesture)
                    || */!prepareToContinueOldGesture(gestureSteps)) {
                cancelAnyPendingInjectedEvents();
                notifyService(serviceInterface, sequence, false);
                return;
            }
        }
        if (!continuingGesture) {
            cancelAnyPendingInjectedEvents();
            // Injected gestures have been canceled, but real gestures still need cancelling
            cancelAnyGestureInProgress(EVENT_SOURCE);
        }
        mServiceInterfaceForCurrentGesture = serviceInterface;

        long currentTime = SystemClock.uptimeMillis();
        List<MotionEvent> events = getMotionEventsFromGestureSteps(gestureSteps,
                (mSequencesInProgress.size() == 0) ? currentTime : mLastScheduledEventTime);
        if (events.isEmpty()) {
            notifyService(serviceInterface, sequence, false);
            return;
        }
        mSequencesInProgress.add(sequence);

        for (int i = 0; i < events.size(); i++) {
            MotionEvent event = events.get(i);
            // event.setDisplayId(displayId);
            int isEndOfSequence = (i == events.size() - 1) ? 1 : 0;
            Message message = mHandler.obtainMessage(
                    MESSAGE_SEND_MOTION_EVENT, isEndOfSequence, 0, event);
            mLastScheduledEventTime = event.getEventTime();
            mHandler.sendMessageDelayed(message, Math.max(0, event.getEventTime() - currentTime));
        }
    }

    private boolean newGestureTriesToContinueOldOne(List<AutoGestureDescription.GestureStep> gestureSteps) {
        if (gestureSteps.isEmpty()) {
            return false;
        }
        AutoGestureDescription.GestureStep firstStep = gestureSteps.get(0);
        for (int i = 0; i < firstStep.numTouchPoints; i++) {
            if (!firstStep.touchPoints[i].mIsStartOfPath) {
                return true;
            }
        }
        return false;
    }

    /**
     * A gesture can only continue a gesture if it contains intermediate points that continue
     * each continued stroke of the last gesture, and no extra points.
     *
     * @param gestureSteps The steps of the new gesture
     * @return {@code true} if the new gesture could continue the last one dispatched. {@code false}
     * otherwise.
     */
    private boolean prepareToContinueOldGesture(List<AutoGestureDescription.GestureStep> gestureSteps) {
        if (gestureSteps.isEmpty() || (mLastTouchPoints == null) || (mNumLastTouchPoints == 0)) {
            return false;
        }
        AutoGestureDescription.GestureStep firstStep = gestureSteps.get(0);
        // Make sure all of the continuing paths match up
        int numContinuedStrokes = 0;
        for (int i = 0; i < firstStep.numTouchPoints; i++) {
            AutoGestureDescription.TouchPoint touchPoint = firstStep.touchPoints[i];
            if (!touchPoint.mIsStartOfPath) {
                int continuedPointerId = mStrokeIdToPointerId
                        .get(touchPoint.mContinuedStrokeId, -1);
                if (continuedPointerId == -1) {
                    Timber.tag(LOG_TAG).w("Can't continue gesture due to unknown continued stroke id in %s", touchPoint);
                    return false;
                }
                mStrokeIdToPointerId.put(touchPoint.mStrokeId, continuedPointerId);
                int lastPointIndex = findPointByStrokeId(
                        mLastTouchPoints, mNumLastTouchPoints, touchPoint.mContinuedStrokeId);
                if (lastPointIndex < 0) {
                    Timber.tag(LOG_TAG).w("Can't continue gesture due continued gesture id of "
                            + touchPoint + " not matching any previous strokes in "
                            + Arrays.asList(mLastTouchPoints));
                    return false;
                }
                if (mLastTouchPoints[lastPointIndex].mIsEndOfPath
                        || (mLastTouchPoints[lastPointIndex].mX != touchPoint.mX)
                        || (mLastTouchPoints[lastPointIndex].mY != touchPoint.mY)) {
                    Timber.tag(LOG_TAG).w("Can't continue gesture due to points mismatch between "
                            + mLastTouchPoints[lastPointIndex] + " and " + touchPoint);
                    return false;
                }
                // Update the last touch point to match the continuation, so the gestures will
                // line up
                mLastTouchPoints[lastPointIndex].mStrokeId = touchPoint.mStrokeId;
            }
            numContinuedStrokes++;
        }
        // Make sure we didn't miss any paths
        for (int i = 0; i < mNumLastTouchPoints; i++) {
            if (!mLastTouchPoints[i].mIsEndOfPath) {
                numContinuedStrokes--;
            }
        }
        return numContinuedStrokes == 0;
    }

    private void sendMotionEventToNext(MotionEvent event, MotionEvent rawEvent,
                                       int policyFlags) {
        if (getNext() != null) {
            // super.onMotionEvent(event, rawEvent, policyFlags);
            uiAutomation.injectInputEvent(event, false);
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mOpenGesturesInProgress.put(event.getSource(), true);
            }
            if ((event.getActionMasked() == MotionEvent.ACTION_UP)
                    || (event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
                mOpenGesturesInProgress.put(event.getSource(), false);
            }
        }
    }

    int FLAG_PASS_TO_USER = 0x40000000;

    public static final int POLICY_FLAG_INJECTED_FROM_ACCESSIBILITY = 131072;
    int FLAG_INJECTED_FROM_ACCESSIBILITY = POLICY_FLAG_INJECTED_FROM_ACCESSIBILITY;

    private void cancelAnyGestureInProgress(int source) {
        if ((getNext() != null) && mOpenGesturesInProgress.get(source, false)) {
            long now = SystemClock.uptimeMillis();
            MotionEvent cancelEvent =
                    obtainMotionEvent(now, now, MotionEvent.ACTION_CANCEL, getLastTouchPoints(), 1);
            sendMotionEventToNext(cancelEvent, cancelEvent,
                    FLAG_PASS_TO_USER
                            | FLAG_INJECTED_FROM_ACCESSIBILITY);
            mOpenGesturesInProgress.put(source, false);
        }
    }

    private void cancelAnyPendingInjectedEvents() {
        if (mHandler.hasMessages(MESSAGE_SEND_MOTION_EVENT)) {
            mHandler.removeMessages(MESSAGE_SEND_MOTION_EVENT);
            cancelAnyGestureInProgress(EVENT_SOURCE);
            for (int i = mSequencesInProgress.size() - 1; i >= 0; i--) {
                notifyService(mServiceInterfaceForCurrentGesture,
                        mSequencesInProgress.get(i), false);
                mSequencesInProgress.remove(i);
            }
        } else if (mNumLastTouchPoints != 0) {
            // An injected gesture is in progress and waiting for a continuation. Cancel it.
            cancelAnyGestureInProgress(EVENT_SOURCE);
        }
        mNumLastTouchPoints = 0;
        mStrokeIdToPointerId.clear();
    }

    private void notifyService(GestureCallback callback, int sequence, boolean success) {
        callback.onPerformGestureResult(sequence, success);
    }

    private List<MotionEvent> getMotionEventsFromGestureSteps(
            List<AutoGestureDescription.GestureStep> steps, long startTime) {
        final List<MotionEvent> motionEvents = new ArrayList<>();

        AutoGestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();

        for (int i = 0; i < steps.size(); i++) {
            AutoGestureDescription.GestureStep step = steps.get(i);
            int currentTouchPointSize = step.numTouchPoints;
            if (currentTouchPointSize > lastTouchPoints.length) {
                mNumLastTouchPoints = 0;
                motionEvents.clear();
                return motionEvents;
            }

            appendMoveEventIfNeeded(motionEvents, step.touchPoints, currentTouchPointSize,
                    startTime + step.timeSinceGestureStart);
            appendUpEvents(motionEvents, step.touchPoints, currentTouchPointSize,
                    startTime + step.timeSinceGestureStart);
            appendDownEvents(motionEvents, step.touchPoints, currentTouchPointSize,
                    startTime + step.timeSinceGestureStart);
        }
        return motionEvents;
    }

    private AutoGestureDescription.TouchPoint[] getLastTouchPoints() {
        if (mLastTouchPoints == null) {
            int capacity = 20;// GestureDescription.getMaxStrokeCount();
            mLastTouchPoints = new AutoGestureDescription.TouchPoint[capacity];
            for (int i = 0; i < capacity; i++) {
                mLastTouchPoints[i] = new AutoGestureDescription.TouchPoint();
            }
        }
        return mLastTouchPoints;
    }

    private void appendMoveEventIfNeeded(List<MotionEvent> motionEvents,
                                         AutoGestureDescription.TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
        /* Look for pointers that have moved */
        boolean moveFound = false;
        AutoGestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();
        for (int i = 0; i < currentTouchPointsSize; i++) {
            int lastPointsIndex = findPointByStrokeId(lastTouchPoints, mNumLastTouchPoints,
                    currentTouchPoints[i].mStrokeId);
            if (lastPointsIndex >= 0) {
                moveFound |= (lastTouchPoints[lastPointsIndex].mX != currentTouchPoints[i].mX)
                        || (lastTouchPoints[lastPointsIndex].mY != currentTouchPoints[i].mY);
                lastTouchPoints[lastPointsIndex].copyFrom(currentTouchPoints[i]);
            }
        }

        if (moveFound) {
            motionEvents.add(obtainMotionEvent(mDownTime, currentTime, MotionEvent.ACTION_MOVE,
                    lastTouchPoints, mNumLastTouchPoints));
        }
    }

    private void appendUpEvents(List<MotionEvent> motionEvents,
                                AutoGestureDescription.TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
        /* Look for a pointer at the end of its path */
        AutoGestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();
        for (int i = 0; i < currentTouchPointsSize; i++) {
            if (currentTouchPoints[i].mIsEndOfPath) {
                int indexOfUpEvent = findPointByStrokeId(lastTouchPoints, mNumLastTouchPoints,
                        currentTouchPoints[i].mStrokeId);
                if (indexOfUpEvent < 0) {
                    continue; // Should not happen
                }
                int action = (mNumLastTouchPoints == 1) ? MotionEvent.ACTION_UP
                        : MotionEvent.ACTION_POINTER_UP;
                action |= indexOfUpEvent << MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                motionEvents.add(obtainMotionEvent(mDownTime, currentTime, action,
                        lastTouchPoints, mNumLastTouchPoints));
                /* Remove this point from lastTouchPoints */
                for (int j = indexOfUpEvent; j < mNumLastTouchPoints - 1; j++) {
                    lastTouchPoints[j].copyFrom(mLastTouchPoints[j + 1]);
                }
                mNumLastTouchPoints--;
                if (mNumLastTouchPoints == 0) {
                    mStrokeIdToPointerId.clear();
                }
            }
        }
    }

    private void appendDownEvents(List<MotionEvent> motionEvents,
                                  AutoGestureDescription.TouchPoint[] currentTouchPoints, int currentTouchPointsSize, long currentTime) {
        /* Look for a pointer that is just starting */
        AutoGestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();
        for (int i = 0; i < currentTouchPointsSize; i++) {
            if (currentTouchPoints[i].mIsStartOfPath) {
                /* Add the point to last coords and use the new array to generate the event */
                lastTouchPoints[mNumLastTouchPoints++].copyFrom(currentTouchPoints[i]);
                int action = (mNumLastTouchPoints == 1) ? MotionEvent.ACTION_DOWN
                        : MotionEvent.ACTION_POINTER_DOWN;
                if (action == MotionEvent.ACTION_DOWN) {
                    mDownTime = currentTime;
                }
                action |= i << MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                motionEvents.add(obtainMotionEvent(mDownTime, currentTime, action,
                        lastTouchPoints, mNumLastTouchPoints));
            }
        }
    }

    private MotionEvent obtainMotionEvent(long downTime, long eventTime, int action,
                                          AutoGestureDescription.TouchPoint[] touchPoints, int touchPointsSize) {
        if ((sPointerCoords == null) || (sPointerCoords.length < touchPointsSize)) {
            sPointerCoords = new MotionEvent.PointerCoords[touchPointsSize];
            for (int i = 0; i < touchPointsSize; i++) {
                sPointerCoords[i] = new MotionEvent.PointerCoords();
            }
        }
        if ((sPointerProps == null) || (sPointerProps.length < touchPointsSize)) {
            sPointerProps = new MotionEvent.PointerProperties[touchPointsSize];
            for (int i = 0; i < touchPointsSize; i++) {
                sPointerProps[i] = new MotionEvent.PointerProperties();
            }
        }
        for (int i = 0; i < touchPointsSize; i++) {
            int pointerId = mStrokeIdToPointerId.get(touchPoints[i].mStrokeId, -1);
            if (pointerId == -1) {
                pointerId = getUnusedPointerId();
                mStrokeIdToPointerId.put(touchPoints[i].mStrokeId, pointerId);
            }
            sPointerProps[i].id = pointerId;
            sPointerProps[i].toolType = MotionEvent.TOOL_TYPE_UNKNOWN;
            sPointerCoords[i].clear();
            sPointerCoords[i].pressure = 1.0f;
            sPointerCoords[i].size = 1.0f;
            sPointerCoords[i].x = touchPoints[i].mX;
            sPointerCoords[i].y = touchPoints[i].mY;
        }
        return MotionEvent.obtain(downTime, eventTime, action, touchPointsSize,
                sPointerProps, sPointerCoords, EVENT_META_STATE, EVENT_BUTTON_STATE,
                EVENT_X_PRECISION, EVENT_Y_PRECISION, KeyCharacterMap.VIRTUAL_KEYBOARD,
                EVENT_EDGE_FLAGS, EVENT_SOURCE, EVENT_FLAGS);
    }

    private static int findPointByStrokeId(AutoGestureDescription.TouchPoint[] touchPoints, int touchPointsSize,
                                           int strokeId) {
        for (int i = 0; i < touchPointsSize; i++) {
            if (touchPoints[i].mStrokeId == strokeId) {
                return i;
            }
        }
        return -1;
    }

    private int getUnusedPointerId() {
        int MAX_POINTER_ID = 10;
        int pointerId = 0;
        while (mStrokeIdToPointerId.indexOfValue(pointerId) >= 0) {
            pointerId++;
            if (pointerId >= MAX_POINTER_ID) {
                return MAX_POINTER_ID;
            }
        }
        return pointerId;
    }
}