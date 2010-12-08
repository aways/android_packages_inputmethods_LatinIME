/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.inputmethod.keyboard;

import com.android.inputmethod.keyboard.KeyboardParser.ParseException;
import com.android.inputmethod.keyboard.KeyStyles.KeyStyle;
import com.android.inputmethod.latin.R;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Xml;

/**
 * Class for describing the position and characteristics of a single key in the keyboard.
 */
public class Key {
    /**
     * All the key codes (unicode or custom code) that this key could generate, zero'th
     * being the most important.
     */
    public final int[] mCodes;
    /** The unicode that this key generates in manual temporary upper case mode. */
    public final int mManualTemporaryUpperCaseCode;

    /** Label to display */
    public final CharSequence mLabel;
    /** Option of the label */
    public final int mLabelOption;

    /** Icon to display instead of a label. Icon takes precedence over a label */
    private Drawable mIcon;
    /** Preview version of the icon, for the preview popup */
    private Drawable mPreviewIcon;
    /** Hint icon to display on the key in conjunction with the label */
    public final Drawable mHintIcon;
    /**
     * The hint icon to display on the key when keyboard is in manual temporary upper case
     * mode.
     */
    public final Drawable mManualTemporaryUpperCaseHintIcon;

    /** Width of the key, not including the gap */
    public final int mWidth;
    /** Height of the key, not including the gap */
    public final int mHeight;
    /** The horizontal gap before this key */
    public final int mGap;
    /** Whether this key is sticky, i.e., a toggle key */
    public final boolean mSticky;
    /** X coordinate of the key in the keyboard layout */
    public final int mX;
    /** Y coordinate of the key in the keyboard layout */
    public final int mY;
    /** Text to output when pressed. This can be multiple characters, like ".com" */
    public final CharSequence mOutputText;
    /** Popup characters */
    public final CharSequence mPopupCharacters;
    /**
     * If this key pops up a mini keyboard, this is the resource id for the XML layout for that
     * keyboard.
     */
    public final int mPopupResId;

    /**
     * Flags that specify the anchoring to edges of the keyboard for detecting touch events
     * that are just out of the boundary of the key. This is a bit mask of
     * {@link Keyboard#EDGE_LEFT}, {@link Keyboard#EDGE_RIGHT},
     * {@link Keyboard#EDGE_TOP} and {@link Keyboard#EDGE_BOTTOM}.
     */
    public final int mEdgeFlags;
    /** Whether this is a modifier key, such as Shift or Alt */
    public final boolean mModifier;
    /** Whether this key repeats itself when held down */
    public final boolean mRepeatable;

    /** The Keyboard that this key belongs to */
    private final Keyboard mKeyboard;

    /** The current pressed state of this key */
    public boolean mPressed;
    /** If this is a sticky key, is it on? */
    public boolean mOn;

    private final static int[] KEY_STATE_NORMAL_ON = {
        android.R.attr.state_checkable,
        android.R.attr.state_checked
    };

    private final static int[] KEY_STATE_PRESSED_ON = {
        android.R.attr.state_pressed,
        android.R.attr.state_checkable,
        android.R.attr.state_checked
    };

    private final static int[] KEY_STATE_NORMAL_OFF = {
        android.R.attr.state_checkable
    };

    private final static int[] KEY_STATE_PRESSED_OFF = {
        android.R.attr.state_pressed,
        android.R.attr.state_checkable
    };

    private final static int[] KEY_STATE_NORMAL = {
    };

    private final static int[] KEY_STATE_PRESSED = {
        android.R.attr.state_pressed
    };

    // functional normal state (with properties)
    private static final int[] KEY_STATE_FUNCTIONAL_NORMAL = {
            android.R.attr.state_single
    };

    // functional pressed state (with properties)
    private static final int[] KEY_STATE_FUNCTIONAL_PRESSED = {
            android.R.attr.state_single,
            android.R.attr.state_pressed
    };

    /** Create an empty key with no attributes. */
    public Key(Row row, char letter, int x, int y) {
        mKeyboard = row.getKeyboard();
        mHeight = row.mDefaultHeight;
        mGap = row.mDefaultHorizontalGap;
        mWidth = row.mDefaultWidth - mGap;
        mEdgeFlags = row.mRowEdgeFlags;
        mHintIcon = null;
        mManualTemporaryUpperCaseHintIcon = null;
        mManualTemporaryUpperCaseCode = 0;
        mLabelOption = 0;
        mModifier = false;
        mSticky = false;
        mRepeatable = false;
        mOutputText = null;
        mPopupCharacters = null;
        mPopupResId = 0;
        mLabel = String.valueOf(letter);
        mCodes = new int[] { letter };
        // Horizontal gap is divided equally to both sides of the key.
        mX = x + mGap / 2;
        mY = y;
    }

    /** Create a key with the given top-left coordinate and extract its attributes from
     * the XML parser.
     * @param res resources associated with the caller's context
     * @param row the row that this key belongs to. The row must already be attached to
     * a {@link Keyboard}.
     * @param x the x coordinate of the top-left
     * @param y the y coordinate of the top-left
     * @param parser the XML parser containing the attributes for this key
     */
    public Key(Resources res, Row row, int x, int y, XmlResourceParser parser,
            KeyStyles keyStyles) {
        mKeyboard = row.getKeyboard();

        TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
                R.styleable.Keyboard);
        mHeight = KeyboardParser.getDimensionOrFraction(a,
                R.styleable.Keyboard_keyHeight,
                mKeyboard.getKeyboardHeight(), row.mDefaultHeight);
        mGap = KeyboardParser.getDimensionOrFraction(a,
                R.styleable.Keyboard_horizontalGap,
                mKeyboard.getKeyboardWidth(), row.mDefaultHorizontalGap);
        mWidth = KeyboardParser.getDimensionOrFraction(a,
                R.styleable.Keyboard_keyWidth,
                mKeyboard.getKeyboardWidth(), row.mDefaultWidth) - mGap;
        a.recycle();

        a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key);

        final KeyStyle style;
        if (a.hasValue(R.styleable.Keyboard_Key_keyStyle)) {
            String styleName = a.getString(R.styleable.Keyboard_Key_keyStyle);
            style = keyStyles.getKeyStyle(styleName);
            if (style == null)
                throw new ParseException("Unknown key style: " + styleName, parser);
        } else {
            style = keyStyles.getEmptyKeyStyle();
        }

        // Horizontal gap is divided equally to both sides of the key.
        this.mX = x + mGap / 2;
        this.mY = y;

        mPreviewIcon = style.getDrawable(a, R.styleable.Keyboard_Key_iconPreview);
        Keyboard.setDefaultBounds(mPreviewIcon);
        final CharSequence popupCharacters = style.getText(a,
                R.styleable.Keyboard_Key_popupCharacters);
        final int popupResId = style.getResourceId(a, R.styleable.Keyboard_Key_popupKeyboard, 0);
        // Set popup keyboard resource and characters only when both are specified.
        if (popupResId != 0 && !TextUtils.isEmpty(popupCharacters)) {
            mPopupResId = popupResId;
            mPopupCharacters = popupCharacters;
        } else {
            mPopupResId = 0;
            mPopupCharacters = null;
        }
        mRepeatable = style.getBoolean(a, R.styleable.Keyboard_Key_isRepeatable, false);
        mModifier = style.getBoolean(a, R.styleable.Keyboard_Key_isModifier, false);
        mSticky = style.getBoolean(a, R.styleable.Keyboard_Key_isSticky, false);
        mEdgeFlags = style.getFlag(a, R.styleable.Keyboard_Key_keyEdgeFlags, 0)
                | row.mRowEdgeFlags;

        mIcon = style.getDrawable(a, R.styleable.Keyboard_Key_keyIcon);
        Keyboard.setDefaultBounds(mIcon);
        mHintIcon = style.getDrawable(a, R.styleable.Keyboard_Key_keyHintIcon);
        Keyboard.setDefaultBounds(mHintIcon);
        mManualTemporaryUpperCaseHintIcon = style.getDrawable(a,
                R.styleable.Keyboard_Key_manualTemporaryUpperCaseHintIcon);
        Keyboard.setDefaultBounds(mManualTemporaryUpperCaseHintIcon);

        mLabel = style.getText(a, R.styleable.Keyboard_Key_keyLabel);
        mLabelOption = style.getFlag(a, R.styleable.Keyboard_Key_keyLabelOption, 0);
        mManualTemporaryUpperCaseCode = style.getInt(a,
                R.styleable.Keyboard_Key_manualTemporaryUpperCaseCode, 0);
        mOutputText = style.getText(a, R.styleable.Keyboard_Key_keyOutputText);
        // Choose the first letter of the label as primary code if not specified.
        final int[] codes = style.getIntArray(a, R.styleable.Keyboard_Key_codes);
        if (codes == null && !TextUtils.isEmpty(mLabel)) {
            mCodes = new int[] { mLabel.charAt(0) };
        } else {
            mCodes = codes;
        }

        final Drawable shiftedIcon = style.getDrawable(a,
                R.styleable.Keyboard_Key_shiftedIcon);
        if (shiftedIcon != null)
            mKeyboard.getShiftedIcons().put(this, shiftedIcon);

        a.recycle();
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public Drawable getPreviewIcon() {
        return mPreviewIcon;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public void setPreviewIcon(Drawable icon) {
        mPreviewIcon = icon;
    }

    /**
     * Informs the key that it has been pressed, in case it needs to change its appearance or
     * state.
     * @see #onReleased(boolean)
     */
    public void onPressed() {
        mPressed = !mPressed;
    }

    /**
     * Changes the pressed state of the key. If it is a sticky key, it will also change the
     * toggled state of the key if the finger was release inside.
     * @param inside whether the finger was released inside the key
     * @see #onPressed()
     */
    public void onReleased(boolean inside) {
        mPressed = !mPressed;
        if (mSticky && !mKeyboard.isShiftLockEnabled(this))
            mOn = !mOn;
    }

    public boolean isInside(int x, int y) {
        return mKeyboard.isInside(this, x, y);
    }

    /**
     * Detects if a point falls on this key.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return whether or not the point falls on the key. If the key is attached to an edge, it will
     * assume that all points between the key and the edge are considered to be on the key.
     */
    public boolean isOnKey(int x, int y) {
        final int flags = mEdgeFlags;
        final boolean leftEdge = (flags & Keyboard.EDGE_LEFT) != 0;
        final boolean rightEdge = (flags & Keyboard.EDGE_RIGHT) != 0;
        final boolean topEdge = (flags & Keyboard.EDGE_TOP) != 0;
        final boolean bottomEdge = (flags & Keyboard.EDGE_BOTTOM) != 0;
        final int left = this.mX;
        final int right = left + this.mWidth;
        final int top = this.mY;
        final int bottom = top + this.mHeight;
        return (x >= left || leftEdge) && (x < right || rightEdge)
                && (y >= top || topEdge) && (y < bottom || bottomEdge);
    }

    /**
     * Returns the square of the distance to the nearest edge of the key and the given point.
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the square of the distance of the point from the nearest edge of the key
     */
    public int squaredDistanceToEdge(int x, int y) {
        final int left = this.mX;
        final int right = left + this.mWidth;
        final int top = this.mY;
        final int bottom = top + this.mHeight;
        final int edgeX = x < left ? left : (x > right ? right : x);
        final int edgeY = y < top ? top : (y > bottom ? bottom : y);
        final int dx = x - edgeX;
        final int dy = y - edgeY;
        return dx * dx + dy * dy;
    }

    // sticky is used for shift key.  If a key is not sticky and is modifier,
    // the key will be treated as functional.
    private boolean isFunctionalKey() {
        return !mSticky && mModifier;
    }

    /**
     * Returns the drawable state for the key, based on the current state and type of the key.
     * @return the drawable state of the key.
     * @see android.graphics.drawable.StateListDrawable#setState(int[])
     */
    public int[] getCurrentDrawableState() {
        if (isFunctionalKey()) {
            if (mPressed) {
                return KEY_STATE_FUNCTIONAL_PRESSED;
            } else {
                return KEY_STATE_FUNCTIONAL_NORMAL;
            }
        }

        int[] states = KEY_STATE_NORMAL;

        if (mOn) {
            if (mPressed) {
                states = KEY_STATE_PRESSED_ON;
            } else {
                states = KEY_STATE_NORMAL_ON;
            }
        } else {
            if (mSticky) {
                if (mPressed) {
                    states = KEY_STATE_PRESSED_OFF;
                } else {
                    states = KEY_STATE_NORMAL_OFF;
                }
            } else {
                if (mPressed) {
                    states = KEY_STATE_PRESSED;
                }
            }
        }
        return states;
    }
}