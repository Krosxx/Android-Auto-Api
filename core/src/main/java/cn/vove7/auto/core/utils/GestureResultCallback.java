package cn.vove7.auto.core.utils;

public abstract class GestureResultCallback {
    /**
     * Called when the gesture has completed successfully
     *
     * @param gestureDescription The description of the gesture that completed.
     */
    public void onCompleted(AutoGestureDescription gestureDescription) {
    }

    /**
     * Called when the gesture was cancelled
     *
     * @param gestureDescription The description of the gesture that was cancelled.
     */
    public void onCancelled(AutoGestureDescription gestureDescription) {
    }
}