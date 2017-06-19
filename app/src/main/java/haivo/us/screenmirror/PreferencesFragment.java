package haivo.us.screenmirror;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

public class PreferencesFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    private void setUpStringPref(String str) {
        Preference preferenceCategory = new PreferenceCategory(getActivity());
        preferenceCategory.setTitle(str);
        getPreferenceScreen().addPreference(preferenceCategory);
    }

    private void setUpVersionPref(String str, String str2) {
        Preference preference = new Preference(getActivity());
        preference.setTitle(str);
        preference.setSummary(str2);
        getPreferenceScreen().addPreference(preference);
    }

    private void setUpPortPref(String str, String str2, int i, int i2, int i3) {
        Preference portNumberPref = new TextPref(getActivity(), i, i2);
        portNumberPref.setDefaultValue(String.valueOf(i3));
        portNumberPref.setKey(str);
        portNumberPref.setTitle(str2);
        portNumberPref.setSummary(String.valueOf(getPreferenceManager().getSharedPreferences().getInt(str, i3)));
        getPreferenceScreen().addPreference(portNumberPref);
    }

    private void setUpQualityDialogPref(String str,
                                        String str2,
                                        CharSequence[] charSequenceArr,
                                        CharSequence[] charSequenceArr2,
                                        CharSequence charSequence) {
        ListPreference listPreference = new ListPreference(getActivity());
        listPreference.setKey(str);
        listPreference.setTitle(str2);
        listPreference.setDialogTitle(str2);
        listPreference.setEntries(charSequenceArr);
        listPreference.setEntryValues(charSequenceArr2);
        listPreference.setDefaultValue(charSequenceArr2[0]);
        listPreference.setSummary(charSequenceArr[listPreference.findIndexOfValue(getPreferenceManager().getSharedPreferences()
                                                                                                        .getString(str,
                                                                                                                   charSequence
                                                                                                                       .toString()))]);
        getPreferenceScreen().addPreference(listPreference);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
        setUpStringPref(getActivity().getString(R.string.pref_category_network));
        setUpPortPref(getString(R.string.pref_http_port_key),
                      getString(R.string.pref_http_port_title),
                      1024,
                      65534,
                      8080);
        setUpStringPref(getActivity().getString(R.string.pref_category_encoding));
        CharSequence[] stringArray = getResources().getStringArray(R.array.pref_quality_entries);
        CharSequence[] stringArray2 = getResources().getStringArray(R.array.pref_quality_entryValues);
        setUpQualityDialogPref(getString(R.string.pref_quality_key),
                               getString(R.string.pref_quality_title),
                               stringArray,
                               stringArray2,
                               stringArray2[1]);
        setUpStringPref(getActivity().getString(R.string.pref_category_about));
        setUpVersionPref(getActivity().getString(R.string.pref_version_title), "0.0.1");
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        Preference findPreference = findPreference(str);
        if (findPreference == null) {
            return;
        }
        if (findPreference instanceof TextPref) {
            TextPref textPref = (TextPref) findPreference;
            String valueOf = String.valueOf(sharedPreferences.getInt(str, 0));
            textPref.setSummary(valueOf);
            textPref.setText(valueOf);
        } else if (findPreference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) findPreference;
            listPreference.setSummary(listPreference.getEntry());
        }
    }
}
