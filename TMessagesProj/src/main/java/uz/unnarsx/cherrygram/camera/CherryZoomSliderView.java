/**
 * This is the source code of Cherrygram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Please, be respectful and credit the original author if you use this code.
 *
 * Copyright github.com/arsLan4k1390, 2022-2026.
 */

package uz.unnarsx.cherrygram.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProvider;

@SuppressLint("ViewConstructor")
public class CherryZoomSliderView extends View {

    public interface OnPresetSelectedListener {
        void onPresetSelected(float zoom);
    }

    public interface ZoomController {
        boolean isCameraReady();

        float getZoomRatio();

        float getMinZoomRatio();

        float getMaxZoomRatio();

        void setZoomRatio(float ratio);

        boolean isFrontface();
    }

    public CherryZoomSliderView(Context context, Theme.ResourcesProvider resourcesProvider, boolean greyColor) {
        super(context);
    }

    /**
     * 🚀✨ XXL: в опубликованном Арсланом дереве этот слайдер — оболочка без тел, а сток
     *InstantCameraView/InstantCameraView2 зовут setOnPresetSelectedListener. Держим слушателя
     * и зовём его сами, чтобы подсказка про зум не превращалась в `cannot find symbol`.
     */
    private OnPresetSelectedListener presetSelectedListener;
    private float currentZoomRatio = 1f;
    private ZoomController boundController;

    public void setOnPresetSelectedListener(OnPresetSelectedListener listener) {
        presetSelectedListener = listener;
    }

    public OnPresetSelectedListener getOnPresetSelectedListener() {
        return presetSelectedListener;
    }

    private void notifyPresetSelected() {
        final OnPresetSelectedListener listener = presetSelectedListener;
        if (listener == null) {
            return;
        }
        try {
            listener.onPresetSelected(currentZoomRatio);
        } catch (Throwable ignored) {
        }
    }

    public void setLiquidGlassBackground(
            BlurredBackgroundDrawableViewFactory factory,
            BlurredBackgroundColorProvider colorProvider
    ) {

    }

    public void setZoom(float newZoom, boolean notify) {
        currentZoomRatio = newZoom;
        if (notify) {
            notifyPresetSelected();
        }
    }

    public float getZoom() {
        return currentZoomRatio;
    }

    public void scaleZoom(float factor) {
        if (factor <= 0f || Float.isNaN(factor)) {
            return;
        }
        currentZoomRatio *= factor;
        notifyPresetSelected();
    }

    public void collapse() {

    }

    public void bindCamera(ZoomController controller) {
        boundController = controller;
    }

    public void unbindCamera() {
        boundController = null;
    }

    public ZoomController getBoundCamera() {
        return boundController;
    }

    public void prepareForCameraChange() {

    }

    public void hideImmediately() {

    }

    public void setExpanded(boolean value, boolean animated) {

    }

}