package haivo.us.screenmirror;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;

class TextPref extends EditTextPreference {
    private final int defaultValue;
    private final int value;

    public TextPref(Context context, int i, int i2) {
        super(context);
        this.defaultValue = i;
        this.value = i2;
        m398a();
    }

    private void m398a() {
        getEditText().setFilters(new InputFilter[] {
            new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source,
                                           int start,
                                           int end,
                                           Spanned dest,
                                           int dstart,
                                           int dend) {
                    CharSequence charSequence2 = source.subSequence(start, end).toString();
                    return TextUtils.isDigitsOnly(new StringBuilder().append(dest.subSequence(0, dstart).toString())
                                                                     .append(charSequence2)
                                                                     .append(dest.subSequence(dend, dest.length())
                                                                                 .toString())
                                                                     .toString()) ? charSequence2 : "";
                }
            }
        });
    }

    protected String getPersistedString(String str) {
        return String.valueOf(getPersistedInt(-1));
    }

    protected boolean persistString(String str) {
        try {
            return persistInt(Math.max(Math.min(this.value, Integer.valueOf(str).intValue()), this.defaultValue));
        } catch (NumberFormatException e) {
            return persistInt(this.value);
        }
    }
}
