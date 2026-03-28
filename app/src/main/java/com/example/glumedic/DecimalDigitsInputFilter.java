package com.example.glumedic;
import android.text.InputFilter;
import android.text.Spanned;

public class DecimalDigitsInputFilter implements InputFilter {
    private final int digitsBeforeZero;
    private final int digitsAfterZero;

    public DecimalDigitsInputFilter(int digitsBeforeZero, int digitsAfterZero) {
        this.digitsBeforeZero = digitsBeforeZero;
        this.digitsAfterZero = digitsAfterZero;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        StringBuilder builder = new StringBuilder(dest);
        builder.replace(dstart, dend, source.subSequence(start, end).toString());
        String newText = builder.toString();

        int dotIndex = newText.indexOf('.');
        if (dotIndex == -1) {
            String integerPart = newText.replaceAll("[^0-9]", "");
            if (integerPart.length() > digitsBeforeZero) {
                return "";
            }
            return null;
        }

        String integerPart = newText.substring(0, dotIndex).replaceAll("[^0-9]", "");
        String fractionalPart = newText.substring(dotIndex + 1).replaceAll("[^0-9]", "");

        if (integerPart.length() > digitsBeforeZero) {
            return "";
        }
        if (fractionalPart.length() > digitsAfterZero) {
            return "";
        }

        return null;
    }
}