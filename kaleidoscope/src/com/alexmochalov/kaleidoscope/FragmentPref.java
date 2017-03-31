package com.alexmochalov.kaleidoscope;

import com.alexmochalov.kaleidoscope.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class FragmentPref extends PreferenceFragment {
	
	public void onCreate(Bundle savedInstanceState) {
		
		//getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.pref1);
	  }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    View view = super.onCreateView(inflater, container, savedInstanceState);
	    view.setBackgroundColor(getResources().getColor(R.color.gray_light));
	    return view;
	}	
}
