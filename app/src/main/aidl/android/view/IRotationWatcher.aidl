// IRotationWatcher.aidl
package android.view;

// Declare any non-default types here with import statements

/**
 * {@hide}
 */
interface IRotationWatcher {
    void onRotationChanged(int rotation);
}